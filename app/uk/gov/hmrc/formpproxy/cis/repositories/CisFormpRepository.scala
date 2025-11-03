/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.formpproxy.cis.repositories

import oracle.jdbc.OracleTypes
import play.api.Logging
import play.api.db.{Database, NamedDatabase}
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateNilMonthlyReturnRequest, CreateSubmissionRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.formpproxy.cis.models.response.CreateNilMonthlyReturnResponse
import uk.gov.hmrc.formpproxy.cis.models.{MonthlyReturn, UserMonthlyReturns}

import java.lang.Long
import java.sql.{Connection, ResultSet, Timestamp, Types}
import javax.inject.{Inject, Singleton}
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

trait CisMonthlyReturnSource {
  def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns]
  def createSubmission(request: CreateSubmissionRequest): Future[String]
  def updateMonthlyReturnSubmission(request: UpdateSubmissionRequest): Future[Unit]
  def createNilMonthlyReturn(request: CreateNilMonthlyReturnRequest): Future[CreateNilMonthlyReturnResponse]
  def getSchemeEmail(instanceId: String): Future[Option[String]]
}

private final case class SchemeRow(schemeId: Long, version: Option[Int], email: Option[String])

@Singleton
class CisFormpRepository @Inject() (@NamedDatabase("cis") db: Database)(implicit ec: ExecutionContext)
    extends CisMonthlyReturnSource
    with Logging {

  override def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns] = {
    logger.info(s"[CIS] getMonthlyReturns(instanceId=$instanceId)")
    Future {
      db.withConnection { conn =>
        val cs = conn.prepareCall(CallGetAllMonthlyReturns)
        try {
          cs.setString(1, instanceId)
          cs.registerOutParameter(2, OracleTypes.CURSOR)
          cs.registerOutParameter(3, OracleTypes.CURSOR)
          cs.execute()

          val rsScheme = cs.getObject(2, classOf[ResultSet])
          try ()
          finally if (rsScheme != null) rsScheme.close()

          val monthlyReturns = cs.getObject(3, classOf[ResultSet])
          val returns        =
            try collectMonthlyReturns(monthlyReturns)
            finally if (monthlyReturns != null) monthlyReturns.close()

          UserMonthlyReturns(returns)
        } finally cs.close()
      }
    }
  }

  @tailrec
  private def collectMonthlyReturns(rs: ResultSet, acc: Seq[MonthlyReturn] = Nil): Seq[MonthlyReturn] =
    if (!rs.next()) acc
    else {
      val mr = MonthlyReturn(
        monthlyReturnId = rs.getLong("monthly_return_id"),
        taxYear = rs.getInt("tax_year"),
        taxMonth = rs.getInt("tax_month"),
        nilReturnIndicator = Option(rs.getString("nil_return_indicator")),
        decEmpStatusConsidered = Option(rs.getString("dec_emp_status_considered")),
        decAllSubsVerified = Option(rs.getString("dec_all_subs_verified")),
        decInformationCorrect = Option(rs.getString("dec_information_correct")),
        decNoMoreSubPayments = Option(rs.getString("dec_no_more_sub_payments")),
        decNilReturnNoPayments = Option(rs.getString("dec_nil_return_no_payments")),
        status = Option(rs.getString("status")),
        lastUpdate = Option(rs.getTimestamp("last_update")).map(_.toLocalDateTime),
        amendment = Option(rs.getString("amendment")),
        supersededBy = { val v = rs.getLong("superseded_by"); if (rs.wasNull()) None else Some(v) }
      )
      collectMonthlyReturns(rs, acc :+ mr)
    }

  override def createSubmission(request: CreateSubmissionRequest): Future[String] = Future {
    db.withTransaction { conn =>
      val monthlyReturnId = getMonthlyReturnId(conn, request.instanceId, request.taxYear, request.taxMonth)

      val submissionId = callCreateSubmission(
        conn,
        instanceId = request.instanceId,
        submissionType = "MONTHLY_RETURN",
        activeObjectId = monthlyReturnId,
        hmrcMarkGenerated = request.hmrcMarkGenerated.orNull,
        hmrcMarkGgis = null,
        emailRecipient = request.emailRecipient.orNull,
        agentId = request.agentId.orNull,
        submittableStatus = "PENDING"
      )

      submissionId.toString
    }
  }

  override def updateMonthlyReturnSubmission(request: UpdateSubmissionRequest): Future[Unit] = Future {
    db.withConnection { conn =>
      val monthlyReturnId = getMonthlyReturnId(conn, request.instanceId, request.taxYear, request.taxMonth)
      val amendValue      = request.amendment.getOrElse("N")

      callUpdateMonthlyReturnSubmission(
        conn,
        submissionType = "MONTHLY_RETURN",
        activeObjectId = monthlyReturnId,
        hmrcMarkGenerated = request.hmrcMarkGenerated,
        hmrcMarkGgis = request.hmrcMarkGgis.orNull,
        emailRecipient = request.emailRecipient.orNull,
        submissionRequestDate = request.submissionRequestDate.map(Timestamp.from).orNull,
        acceptedTime = request.acceptedTime.orNull,
        agentId = request.agentId.orNull,
        submittableStatus = request.submittableStatus,
        govtalkErrorCode = request.govtalkErrorCode.orNull,
        govtalkErrorType = request.govtalkErrorType.orNull,
        govtalkErrorMessage = request.govtalkErrorMessage.orNull,
        instanceId = request.instanceId,
        taxYear = request.taxYear,
        taxMonth = request.taxMonth,
        amendment = amendValue
      )
      ()
    }
  }

  private def callCreateSubmission(
    conn: Connection,
    instanceId: String,
    submissionType: String,
    activeObjectId: Long,
    hmrcMarkGenerated: String,
    hmrcMarkGgis: String,
    emailRecipient: String,
    agentId: String,
    submittableStatus: String
  ): Long = {
    val cs = conn.prepareCall("{ call SUBMISSION_PROCS.Create_Submission(?, ?, ?, ?, ?, ?, ?, ?, ?) }")
    try {
      cs.setString(1, instanceId)
      cs.setString(2, submissionType)
      cs.setLong(3, activeObjectId)
      cs.setString(4, hmrcMarkGenerated)
      cs.setString(5, hmrcMarkGgis)
      cs.setString(6, emailRecipient)
      cs.setString(7, agentId)
      cs.setString(8, submittableStatus)
      cs.registerOutParameter(9, Types.NUMERIC)
      cs.execute()
      cs.getLong(9)
    } finally cs.close()
  }

  private def callUpdateMonthlyReturnSubmission(
    conn: Connection,
    submissionType: String,
    activeObjectId: Long,
    hmrcMarkGenerated: String,
    hmrcMarkGgis: String,
    emailRecipient: String,
    submissionRequestDate: Timestamp,
    acceptedTime: String,
    agentId: String,
    submittableStatus: String,
    govtalkErrorCode: String,
    govtalkErrorType: String,
    govtalkErrorMessage: String,
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    amendment: String
  ): Unit = {
    val cs = conn.prepareCall(
      "{ call SUBMISSION_PROCS_2016.UPDATE_MR_SUBMISSION(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
    )
    try {
      cs.setString(1, submissionType)
      cs.setLong(2, activeObjectId)
      cs.setString(3, hmrcMarkGenerated)
      cs.setString(4, hmrcMarkGgis)
      cs.setString(5, emailRecipient)
      cs.setTimestamp(6, submissionRequestDate)
      cs.setString(7, acceptedTime)
      cs.setString(8, agentId)
      cs.setString(9, submittableStatus)
      cs.setString(10, govtalkErrorCode)
      cs.setString(11, govtalkErrorType)
      cs.setString(12, govtalkErrorMessage)
      cs.setString(13, instanceId)
      cs.setInt(14, taxYear)
      cs.setInt(15, taxMonth)
      cs.setString(16, amendment)
      cs.execute()
    } finally cs.close()
  }

  override def createNilMonthlyReturn(
    request: CreateNilMonthlyReturnRequest
  ): Future[CreateNilMonthlyReturnResponse] = {
    logger.info(
      s"[CIS] createNilMonthlyReturn(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth})"
    )
    Future {
      db.withTransaction { conn =>
        val schemeVersionBefore = getSchemeVersion(conn, request.instanceId)

        callCreateMonthlyReturn(conn, request)
        callUpdateSchemeVersion(conn, request.instanceId, schemeVersionBefore)
        callUpdateMonthlyReturn(conn, request)

        CreateNilMonthlyReturnResponse(status = "STARTED")
      }
    }
  }

  private val CallCreateMonthlyReturn  = "{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }"
  private val CallUpdateSchemeVersion  = "{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"
  private val CallUpdateMonthlyReturn  =
    "{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  private val CallGetScheme            = "{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"
  private val CallGetAllMonthlyReturns = "{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }"

  private def callCreateMonthlyReturn(conn: Connection, req: CreateNilMonthlyReturnRequest): Unit = {
    val cs = conn.prepareCall(CallCreateMonthlyReturn)
    try {
      cs.setString(1, req.instanceId)
      cs.setInt(2, req.taxYear)
      cs.setInt(3, req.taxMonth)
      cs.setString(4, "Y")
      cs.execute()
    } finally cs.close()
  }

  private def callUpdateSchemeVersion(conn: Connection, instanceId: String, currentVersion: Int): Int = {
    val cs = conn.prepareCall(CallUpdateSchemeVersion)
    try {
      cs.setString(1, instanceId)
      cs.setInt(2, currentVersion)
      cs.registerOutParameter(2, Types.INTEGER)
      cs.execute()
      cs.getInt(2)
    } finally cs.close()
  }

  private def callUpdateMonthlyReturn(conn: Connection, req: CreateNilMonthlyReturnRequest): Unit = {
    val cs = conn.prepareCall(CallUpdateMonthlyReturn)
    try {
      cs.setString(1, req.instanceId)
      cs.setInt(2, req.taxYear)
      cs.setInt(3, req.taxMonth)
      cs.setString(4, "N")
      cs.setNull(5, Types.VARCHAR)
      cs.setNull(6, Types.VARCHAR)
      cs.setString(7, req.decInformationCorrect)
      cs.setNull(8, Types.CHAR)
      cs.setString(9, req.decNilReturnNoPayments)
      cs.setString(10, "Y")
      cs.setString(11, "STARTED")
      cs.setInt(12, 0)
      cs.registerOutParameter(12, Types.INTEGER)
      cs.execute()
    } finally cs.close()
  }

  private def readSchemeRow(rs: ResultSet): SchemeRow = {
    val id      = rs.getLong("scheme_id")
    val v       = rs.getInt("version")
    val version = if (rs.wasNull()) None else Some(v)
    val email   = Option(rs.getString("email_address")).map(_.trim).filter(_.nonEmpty)
    SchemeRow(id, version, email)
  }

  private def loadScheme(conn: Connection, instanceId: String): SchemeRow =
    Using.resource(conn.prepareCall(CallGetScheme)) { cs =>
      cs.setString(1, instanceId)
      cs.registerOutParameter(2, Types.REF_CURSOR)
      cs.execute()

      val rs = cs.getObject(2, classOf[ResultSet])
      if (rs == null)
        throw new RuntimeException(s"int_Get_Scheme returned null cursor for instance_id=$instanceId")

      Using.resource(rs) { r =>
        if (r != null && r.next()) readSchemeRow(r)
        else throw new RuntimeException(s"No SCHEME row for instance_id=$instanceId")
      }
    }

  private def getSchemeVersion(conn: Connection, instanceId: String): Int =
    loadScheme(conn, instanceId).version.getOrElse(0)

  override def getSchemeEmail(instanceId: String): Future[Option[String]] = Future {
    db.withConnection { conn =>
      loadScheme(conn, instanceId).email
    }
  }

  private def getSchemeId(conn: Connection, instanceId: String): Long =
    loadScheme(conn, instanceId).schemeId

  private def getMonthlyReturnId(
    conn: Connection,
    instanceId: String,
    taxYear: Int,
    taxMonth: Int
  ): Long =
    Using.resource(conn.prepareCall(CallGetAllMonthlyReturns)) { cs =>
      cs.setString(1, instanceId)
      cs.registerOutParameter(2, OracleTypes.CURSOR)
      cs.registerOutParameter(3, OracleTypes.CURSOR)
      cs.execute()

      val monthlyReturns = cs.getObject(3, classOf[ResultSet])
      if (monthlyReturns == null)
        throw new RuntimeException("Get_All_Monthly_Returns returned null monthly cursor")

      Using.resource(monthlyReturns) { rs =>
        var found: Long = null
        while (found == null && rs.next()) {
          val year  = rs.getInt("tax_year")
          val month = rs.getInt("tax_month")
          if (year == taxYear && month == taxMonth) {
            found = rs.getLong("monthly_return_id")
          }
        }

        if (found != null) found.longValue()
        else
          throw new RuntimeException(
            s"No MONTHLY_RETURN for instance_id=$instanceId year=$taxYear month=$taxMonth"
          )
      }
    }

}
