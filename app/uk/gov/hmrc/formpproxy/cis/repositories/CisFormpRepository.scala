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
import uk.gov.hmrc.formpproxy.cis.models.requests.{ApplyPrepopulationRequest, CreateMonthlyReturnRequest, CreateNilMonthlyReturnRequest, CreateSubmissionRequest, UpdateSubcontractorRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.formpproxy.cis.models.response.CreateNilMonthlyReturnResponse
import uk.gov.hmrc.formpproxy.cis.models.{ContractorScheme, CreateContractorSchemeParams, MonthlyReturn, SubcontractorType, UpdateContractorSchemeParams, UserMonthlyReturns}
import uk.gov.hmrc.formpproxy.shared.utils.CallableStatementUtils.setOptionalInt
import uk.gov.hmrc.formpproxy.shared.utils.ResultSetUtils.*

import java.lang.Long
import java.sql.{Connection, ResultSet, Timestamp, Types}
import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Using}

trait CisMonthlyReturnSource {
  def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns]
  def createSubmission(request: CreateSubmissionRequest): Future[String]
  def updateMonthlyReturnSubmission(request: UpdateSubmissionRequest): Future[Unit]
  def createNilMonthlyReturn(request: CreateNilMonthlyReturnRequest): Future[CreateNilMonthlyReturnResponse]
  def createMonthlyReturn(request: CreateMonthlyReturnRequest): Future[Unit]
  def getSchemeEmail(instanceId: String): Future[Option[String]]
  def getScheme(instanceId: String): Future[Option[ContractorScheme]]
  def createScheme(contractorScheme: CreateContractorSchemeParams): Future[Int]
  def updateScheme(contractorScheme: UpdateContractorSchemeParams): Future[Int]
  def updateSchemeVersion(instanceId: String, version: Int): Future[Int]
  def createSubcontractor(schemeId: Int, subcontractorType: SubcontractorType, version: Int): Future[Int]
  def applyPrepopulation(req: ApplyPrepopulationRequest): Future[Int]
  def updateSubcontractor(result: UpdateSubcontractorRequest): Future[Unit]
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

  def getScheme(instanceId: String): Future[Option[ContractorScheme]] = Future {
    db.withConnection { conn =>
      Using.resource(conn.prepareCall(CallGetScheme)) { cs =>
        cs.setString(1, instanceId)
        cs.registerOutParameter(2, Types.REF_CURSOR)
        cs.execute()

        val resultSet = cs.getObject(2, classOf[ResultSet])
        if (resultSet == null)
          throw new RuntimeException(s"int_Get_Scheme returned null cursor for instance_id=$instanceId")

        Using.resource(resultSet) { resultSet =>
          if (resultSet != null && resultSet.next()) {
            Some(
              ContractorScheme(
                resultSet.getInt("scheme_id"),
                resultSet.getString("instance_id"),
                resultSet.getString("aoref"),
                resultSet.getString("tax_office_number"),
                resultSet.getString("tax_office_reference"),
                Option(resultSet.getString("utr")),
                Option(resultSet.getString("name")),
                Option(resultSet.getString("email_address")),
                Option(resultSet.getString("display_welcome_page")),
                resultSet.getOptionalInt("pre_pop_count"),
                Option(resultSet.getString("pre_pop_successful")),
                resultSet.getOptionalInt("subcontractor_counter"),
                resultSet.getOptionalInt("verif_batch_counter"),
                Option(resultSet.getString("last_update")).flatMap(x => Try(Instant.parse(x)).toOption),
                resultSet.getOptionalInt("version")
              )
            )
          } else None
        }
      }
    }
  }

  def createScheme(contractorScheme: CreateContractorSchemeParams): Future[Int] = Future {
    db.withConnection { conn =>
      Using.resource(conn.prepareCall(CallCreateScheme)) { cs =>
        cs.setString(1, contractorScheme.instanceId)
        cs.setString(2, contractorScheme.accountsOfficeReference)
        cs.setString(3, contractorScheme.taxOfficeNumber)
        cs.setString(4, contractorScheme.taxOfficeReference)
        cs.setString(5, contractorScheme.utr.orNull)
        cs.setString(6, contractorScheme.name.orNull)
        cs.setString(7, contractorScheme.emailAddress.orNull)
        cs.setString(8, contractorScheme.displayWelcomePage.orNull)
        cs.setOptionalInt(9, contractorScheme.prePopCount)
        cs.setString(10, contractorScheme.prePopSuccessful.orNull)
        cs.registerOutParameter(11, OracleTypes.INTEGER)

        cs.execute()

        val schemeId = cs.getInt(11)

        schemeId
      }
    }
  }

  def updateScheme(contractorScheme: UpdateContractorSchemeParams): Future[Int] = Future {
    db.withConnection { conn =>
      Using.resource(conn.prepareCall(CallUpdateScheme)) { cs =>
        cs.setInt(1, contractorScheme.schemeId)
        cs.setString(2, contractorScheme.instanceId)
        cs.setString(3, contractorScheme.accountsOfficeReference)
        cs.setString(4, contractorScheme.taxOfficeNumber)
        cs.setString(5, contractorScheme.taxOfficeReference)
        cs.setString(6, contractorScheme.utr.orNull)
        cs.setString(7, contractorScheme.name.orNull)
        cs.setString(8, contractorScheme.emailAddress.orNull)
        cs.setString(9, contractorScheme.displayWelcomePage.orNull)
        cs.setOptionalInt(10, contractorScheme.prePopCount)
        cs.setString(11, contractorScheme.prePopSuccessful.orNull)
        cs.setOptionalInt(12, contractorScheme.version)
        cs.registerOutParameter(12, OracleTypes.INTEGER)

        cs.execute()

        val version = cs.getInt(12)

        version
      }
    }
  }

  def updateSchemeVersion(instanceId: String, version: Int): Future[Int] = Future {
    logger.info(s"[CIS] updateSchemeVersion(instanceId=$instanceId, version=$version)")
    db.withConnection { conn =>
      callUpdateSchemeVersion(conn, instanceId, version)
    }
  }

  def createSubcontractor(schemeId: Int, subcontractorType: SubcontractorType, version: Int): Future[Int] = Future {
    db.withConnection { conn =>
      Using.resource(conn.prepareCall(CallCreateSubcontractor)) { cs =>
        cs.setInt(1, schemeId)
        cs.setInt(2, version)
        cs.setString(3, subcontractorType.toString)
        cs.registerOutParameter(4, OracleTypes.INTEGER)

        cs.execute()

        cs.getInt(4)
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
        supersededBy = {
          val v = rs.getLong("superseded_by"); if (rs.wasNull()) None else Some(v)
        }
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

  override def createMonthlyReturn(request: CreateMonthlyReturnRequest): Future[Unit] = {
    logger.info(
      s"[CIS] createMonthlyReturn(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth})"
    )
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(CallCreateMonthlyReturn)) { cs =>
          cs.setString(1, request.instanceId)
          cs.setInt(2, request.taxYear)
          cs.setInt(3, request.taxMonth)
          cs.setString(4, "N")
          cs.execute()
        }
      }
    }
  }

  override def applyPrepopulation(req: ApplyPrepopulationRequest): Future[Int] = Future {
    logger.info(
      s"[CIS] applyPrepopulation(schemeId=${req.schemeId}, instanceId=${req.instanceId}, version=${req.version}, subs=${req.subcontractorTypes.size})"
    )

    db.withTransaction { conn =>
      // 1) Update_Scheme – set name/UTR/prePopCount/prePopSuccessful (but NOT version)
      Using.resource(conn.prepareCall(CallUpdateScheme)) { cs =>
        cs.setInt(1, req.schemeId)
        cs.setString(2, req.instanceId)
        cs.setString(3, req.accountsOfficeReference)
        cs.setString(4, req.taxOfficeNumber)
        cs.setString(5, req.taxOfficeReference)
        cs.setString(6, req.utr.orNull)
        cs.setString(7, req.name)
        cs.setString(8, req.emailAddress.orNull)
        cs.setString(9, req.displayWelcomePage.orNull)
        cs.setInt(10, req.prePopCount)
        cs.setString(11, req.prePopSuccessful)
        cs.setInt(12, req.version)
        cs.registerOutParameter(12, OracleTypes.INTEGER)

        cs.execute()
      }

      // 2) Create_Subcontractor for each subcontractorType
      req.subcontractorTypes.foreach { subcontractorType =>
        Using.resource(conn.prepareCall(CallCreateSubcontractor)) { cs =>
          cs.setInt(1, req.schemeId)
          cs.setInt(2, req.version)
          cs.setString(3, subcontractorType.toString)
          cs.registerOutParameter(4, OracleTypes.INTEGER)

          cs.execute()
        }
      }

      // 3) Update_Version_Number – increment version atomically in same transaction
      val newVersion = callUpdateSchemeVersion(conn, req.instanceId, req.version)
      newVersion
    }
  }

  private val CallCreateMonthlyReturn  = "{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }"
  private val CallUpdateSchemeVersion  = "{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"
  private val CallUpdateMonthlyReturn  =
    "{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  private val CallGetScheme            = "{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"
  private val CallCreateScheme         = "{ call SCHEME_PROCS.Create_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  private val CallUpdateScheme         = "{ call SCHEME_PROCS.Update_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  private val CallCreateSubcontractor  = "{ call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR(?, ?, ?, ?) }"
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

  private val CallUpdateSubcontractor =
    "{ call SUBCONTRACTOR_PROCS.Update_Subcontractor(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"

  override def updateSubcontractor(request: UpdateSubcontractorRequest): Future[Unit] = Future {
    db.withConnection { conn =>
      Using.resource(conn.prepareCall(CallUpdateSubcontractor)) { cs =>

        def setOptString(i: Int, v: Option[String]): Unit =
          v match {
            case Some(x) => cs.setString(i, x)
            case None    => cs.setNull(i, Types.VARCHAR)
          }

        def setOptTimestamp(i: Int, v: Option[java.time.LocalDateTime]): Unit =
          v match {
            case Some(dt) => cs.setTimestamp(i, Timestamp.valueOf(dt))
            case None     => cs.setNull(i, Types.TIMESTAMP)
          }

        def setOptInt(index: Int, value: Option[Int]): Unit =
          value match {
            case Some(v) => cs.setInt(index, v)
            case None    => cs.setNull(index, Types.NUMERIC)
          }

        cs.setInt(1, request.schemeId)

        cs.setInt(2, request.subbieResourceRef)
        setOptString(3, request.utr)
        setOptInt(4, request.pageVisited)
        setOptString(5, request.partnerUtr)
        setOptString(6, request.crn)
        setOptString(7, request.firstName)
        setOptString(8, request.nino)
        setOptString(9, request.secondName)
        setOptString(10, request.surname)

        setOptString(11, request.partnershipTradingName)
        setOptString(12, request.tradingName)
        setOptString(13, request.addressLine1)
        setOptString(14, request.addressLine2)
        setOptString(15, request.addressLine3)
        setOptString(16, request.addressLine4)
        setOptString(17, request.country)
        setOptString(18, request.postcode)
        setOptString(19, request.emailAddress)
        setOptString(20, request.phoneNumber)
        setOptString(21, request.mobilePhoneNumber)
        setOptString(22, request.worksReferenceNumber)

        setOptString(23, request.matched)
        setOptString(24, request.autoVerified)
        setOptString(25, request.verified)
        setOptString(26, request.verificationNumber)
        setOptString(27, request.taxTreatment)
        setOptString(28, request.updatedTaxTreatment)
        setOptTimestamp(29, request.verificationDate)

        cs.setNull(30, Types.INTEGER)
        cs.registerOutParameter(30, Types.INTEGER)

        cs.execute()

      }
    }
  }

}
