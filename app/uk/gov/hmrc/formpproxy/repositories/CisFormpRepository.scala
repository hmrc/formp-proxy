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

package uk.gov.hmrc.formpproxy.repositories

import oracle.jdbc.OracleTypes

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.db.{Database, NamedDatabase}
import uk.gov.hmrc.formpproxy.models.requests.{CreateAndTrackSubmissionRequest, UpdateSubmissionRequest, CreateNilMonthlyReturnRequest}
import uk.gov.hmrc.formpproxy.models.response.CreateNilMonthlyReturnResponse

import scala.concurrent.{ExecutionContext, Future}
import java.sql.{Connection, ResultSet, Timestamp, Types}
import scala.annotation.tailrec
import uk.gov.hmrc.formpproxy.models.{MonthlyReturn, UserMonthlyReturns}

trait CisMonthlyReturnSource {
  def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns]
  def createAndTrackSubmission(request: CreateAndTrackSubmissionRequest): Future[String]
  def updateMonthlyReturnSubmission(request: UpdateSubmissionRequest): Future[Unit]
  def createNilMonthlyReturn(request: CreateNilMonthlyReturnRequest): Future[CreateNilMonthlyReturnResponse]
  def getSchemeEmail(instanceId: String): Future[Option[String]]
}

@Singleton
class CisFormpRepository @Inject()(@NamedDatabase("cis") db: Database)(implicit ec: ExecutionContext)
  extends CisMonthlyReturnSource with Logging {
  
  override def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns] = {
    logger.info(s"[CIS] getMonthlyReturns(instanceId=$instanceId)")
    Future {
      db.withConnection { conn =>
        val cs = conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }")
        try {
          cs.setString(1, instanceId)
          cs.registerOutParameter(2, OracleTypes.CURSOR)
          cs.registerOutParameter(3, OracleTypes.CURSOR)
          cs.execute()

          val rsScheme = cs.getObject(2, classOf[ResultSet])
          try () finally if (rsScheme != null) rsScheme.close()

          val rsMonthly = cs.getObject(3, classOf[ResultSet])
          val returns =
            try collectMonthlyReturns(rsMonthly)
            finally if (rsMonthly != null) rsMonthly.close()

          UserMonthlyReturns(returns)
      } finally {
        cs.close()
        }
      }
    }
  }

  @tailrec
  private def collectMonthlyReturns(rs: ResultSet, acc: Seq[MonthlyReturn] = Nil): Seq[MonthlyReturn] =
    if (!rs.next()) acc
    else {
      val mr = MonthlyReturn(
        monthlyReturnId = rs.getLong("monthly_return_id"),
        taxYear         = rs.getInt("tax_year"),
        taxMonth        = rs.getInt("tax_month"),
        nilReturnIndicator     = Option(rs.getString("nil_return_indicator")),
        decEmpStatusConsidered = Option(rs.getString("dec_emp_status_considered")),
        decAllSubsVerified     = Option(rs.getString("dec_all_subs_verified")),
        decInformationCorrect  = Option(rs.getString("dec_information_correct")),
        decNoMoreSubPayments   = Option(rs.getString("dec_no_more_sub_payments")),
        decNilReturnNoPayments = Option(rs.getString("dec_nil_return_no_payments")),
        status                 = Option(rs.getString("status")),
        lastUpdate             = Option(rs.getTimestamp("last_update")).map(_.toLocalDateTime),
        amendment              = Option(rs.getString("amendment")),
        supersededBy           = { val v = rs.getLong("superseded_by"); if (rs.wasNull()) None else Some(v) }
      )
      collectMonthlyReturns(rs, acc :+ mr)
    }


  override def createAndTrackSubmission(request: CreateAndTrackSubmissionRequest): Future[String] = Future {
    db.withTransaction { conn =>
      val schemeId = getSchemeId(conn, request.instanceId)
      val monthlyReturnId = getMonthlyReturnId(conn, schemeId, request.taxYear, request.taxMonth)

      val submissionId = callCreateSubmission(
        conn,
        instanceId = request.instanceId,
        submissionType = "MONTHLY_RETURN",
        activeObjectId = monthlyReturnId,
        hmrcMarkGenerated = request.hmrcMarkGenerated.orNull,
        hmrcMarkGgis = null,
        emailRecipient = request.emailRecipient.orNull,
        agentId = request.agentId.orNull,
        submittableStatus = "PENDING" // to be discussed
      )

      callTrackSubmission(
        conn,
        schemeId = schemeId,
        taxYear = request.taxYear,
        taxMonth = request.taxMonth,
        subcontractorCount = request.subcontractorCount.getOrElse(0),
        totalPaymentsMade = request.totalPaymentsMade.getOrElse(BigDecimal(0)),
        totalTaxDeducted = request.totalTaxDeducted.getOrElse(BigDecimal(0))
      )

      submissionId.toString
    }
  }

  override def updateMonthlyReturnSubmission(request: UpdateSubmissionRequest): Future[Unit] = Future {
    db.withConnection { conn =>
      val schemeId = getSchemeId(conn, request.instanceId)
      val monthlyReturnId = getMonthlyReturnId(conn, schemeId, request.taxYear, request.taxMonth)
      val amendValue = request.amendment.getOrElse("N")

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

  private val SqlGetSchemeId =
    "select scheme_id from scheme where instance_id = ?"

  private def getSchemeId(conn: Connection, instanceId: String): Long = {
    val statement = conn.prepareStatement(SqlGetSchemeId)
    try {
      statement.setString(1, instanceId)
      val rs = statement.executeQuery()
      try {
        if (!rs.next()) throw new RuntimeException(s"No SCHEME row for instance_id=$instanceId")
        rs.getLong(1)
      } finally rs.close()
    } finally statement.close()
  }

  private val SqlGetMonthlyReturnId =
    "select monthly_return_id from monthly_return " +
    "where scheme_id = ? and tax_year = ? and tax_month = ?"

  private def getMonthlyReturnId(conn: Connection, schemeId: Long, taxYear: Int, taxMonth: Int): Long = {
    val statement = conn.prepareStatement(SqlGetMonthlyReturnId)
    try {
      statement.setLong(1, schemeId)
      statement.setInt(2, taxYear)
      statement.setInt(3, taxMonth)
      val rs = statement.executeQuery()
      try {
        if (!rs.next()) throw new RuntimeException(s"No MONTHLY_RETURN for scheme=$schemeId year=$taxYear, month=$taxMonth")
        rs.getLong(1)
      } finally rs.close()
    } finally statement.close()
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

  private def callTrackSubmission(
    conn: Connection,
    schemeId: Long,
    taxYear: Int,
    taxMonth: Int,
    subcontractorCount: Int,
    totalPaymentsMade: BigDecimal,
    totalTaxDeducted: BigDecimal
  ): Unit = {
    val cs = conn.prepareCall("{ call HONESTY_DECLARATION_PROCS.TRACK_SUBMISSIONS(?, ?, ?, ?, ?, ?) }")
    try {
      cs.setLong(1, schemeId)
      cs.setInt(2,  taxYear)
      cs.setInt(3,  taxMonth)
      cs.setInt(4,  subcontractorCount)
      cs.setBigDecimal(5, totalPaymentsMade.bigDecimal)
      cs.setBigDecimal(6, totalTaxDeducted.bigDecimal)
      cs.execute()
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
    val cs = conn.prepareCall("{ call SUBMISSION_PROCS_2016.UPDATE_MR_SUBMISSION(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
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


  override def createNilMonthlyReturn(request: CreateNilMonthlyReturnRequest): Future[CreateNilMonthlyReturnResponse] = {
    logger.info(s"[CIS] createNilMonthlyReturn(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth})")
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

  private val CallCreateMonthlyReturn = "{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }"
  private val CallUpdateSchemeVersion = "{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"
  private val CallUpdateMonthlyReturn = "{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"

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


  private val SqlGetSchemeVersion =
    "select version from scheme where instance_id = ?"

  private val CallGetSchemeEmail =
    "{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"

  private def getSchemeVersion(conn: Connection, instanceId: String): Int = {
    val statement = conn.prepareStatement(SqlGetSchemeVersion)
    try {
      statement.setString(1, instanceId)
      val rs = statement.executeQuery()
      try {
        if (!rs.next())
          throw new RuntimeException(s"No SCHEME row for instance_id=$instanceId")
        rs.getInt(1)
      } finally rs.close()
    } finally statement.close()
  }

  override def getSchemeEmail(instanceId: String): Future[Option[String]] = {
    logger.info(s"[CIS] getSchemeEmail(instanceId=$instanceId)")
    Future {
      db.withConnection { conn =>
        val cs = conn.prepareCall(CallGetSchemeEmail)
        try {
          cs.setString(1, instanceId)
          cs.registerOutParameter(2, java.sql.Types.REF_CURSOR)
          cs.execute()

          val cursor = cs.getObject(2).asInstanceOf[java.sql.ResultSet]
          try {
            if (cursor.next()) {
              val email = cursor.getString("email_address")
              Option(email).map(_.trim).filter(_.nonEmpty)
            } else {
              None
            }
          } finally cursor.close()
        } finally cs.close()
      }
    }
  }


}