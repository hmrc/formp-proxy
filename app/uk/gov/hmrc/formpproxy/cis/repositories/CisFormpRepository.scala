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
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.models.response.*
import uk.gov.hmrc.formpproxy.cis.models.*
import uk.gov.hmrc.formpproxy.shared.utils.CallableStatementUtils.*
import uk.gov.hmrc.formpproxy.shared.utils.ResultSetUtils.*
import uk.gov.hmrc.formpproxy.cis.repositories.CisStoredProcedures.*
import uk.gov.hmrc.formpproxy.cis.repositories.CisRowMappers.*
import java.lang.Long
import java.sql.{CallableStatement, Connection, ResultSet, Timestamp, Types}
import java.time.{Instant, LocalDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

trait CisMonthlyReturnSource {
  def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns]
  def getUnsubmittedMonthlyReturns(instanceId: String): Future[UnsubmittedMonthlyReturns]
  def createSubmission(request: CreateSubmissionRequest): Future[String]
  def updateMonthlyReturnSubmission(request: UpdateSubmissionRequest): Future[Unit]
  def createNilMonthlyReturn(request: CreateNilMonthlyReturnRequest): Future[CreateNilMonthlyReturnResponse]
  def updateNilMonthlyReturn(request: CreateNilMonthlyReturnRequest): Future[Unit]
  def createMonthlyReturn(request: CreateMonthlyReturnRequest): Future[Unit]
  def getSchemeEmail(instanceId: String): Future[Option[String]]
  def getScheme(instanceId: String): Future[Option[ContractorScheme]]
  def createScheme(contractorScheme: CreateContractorSchemeParams): Future[Int]
  def updateScheme(contractorScheme: UpdateContractorSchemeParams): Future[Int]
  def updateSchemeVersion(instanceId: String, version: Int): Future[Int]
  def applyPrepopulation(req: ApplyPrepopulationRequest): Future[Int]
  def createAndUpdateSubcontractor(request: CreateAndUpdateSubcontractorRequest): Future[Unit]
  def getSubcontractorList(cisId: String): Future[GetSubcontractorListResponse]
  def getMonthlyReturnForEdit(instanceId: String, taxYear: Int, taxMonth: Int): Future[GetMonthlyReturnForEditResponse]
  def createMonthlyReturnItem(request: CreateMonthlyReturnItemRequest): Future[Unit]
  def deleteMonthlyReturnItem(request: DeleteMonthlyReturnItemRequest): Future[Unit]
  def syncMonthlyReturnItems(request: SyncMonthlyReturnItemsRequest): Future[Unit]
  def getGovTalkStatus(req: GetGovTalkStatusRequest): Future[GetGovTalkStatusResponse]
  def resetGovTalkStatus(req: ResetGovTalkStatusRequest): Future[Unit]
  def createGovTalkStatusRecord(req: CreateGovTalkStatusRecordRequest): Future[Unit]
}

private final case class SchemeRow(schemeId: Long, version: Option[Int], email: Option[String])

@Singleton
class CisFormpRepository @Inject() (@NamedDatabase("cis") db: Database)(implicit ec: ExecutionContext)
    extends CisMonthlyReturnSource
    with Logging {

  // JDBC Helpers

  private def withCall[A](conn: Connection, sql: String)(f: CallableStatement => A): A =
    Using.resource(conn.prepareCall(sql))(f)

  private def withCursor[A](cs: CallableStatement, index: Int)(f: ResultSet => A): A = {
    val rs = cs.getObject(index, classOf[ResultSet])
    if (rs == null) throw new RuntimeException(s"SP returned null cursor at index $index")
    else Using.resource(rs)(f)
  }

  private def discardCursor(cs: CallableStatement, index: Int): Unit =
    withCursor(cs, index)(_ => ())

  // Monthly Returns

  override def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns] = {
    logger.info(s"[CIS] getMonthlyReturns(instanceId=$instanceId)")
    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetAllMonthlyReturns) { cs =>
          cs.setString(1, instanceId)
          cs.registerOutParameter(2, OracleTypes.CURSOR)
          cs.registerOutParameter(3, OracleTypes.CURSOR)
          cs.execute()

          discardCursor(cs, 2)

          val returns = withCursor(cs, 3)(collectMonthlyReturns)
          UserMonthlyReturns(returns)
        }
      }
    }
  }

  override def getUnsubmittedMonthlyReturns(instanceId: String): Future[UnsubmittedMonthlyReturns] = {
    logger.info(s"[CIS] getUnsubmittedMonthlyReturns(instanceId=$instanceId)")
    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetUnsubmittedMonthlyReturns) { cs =>
          cs.setString(1, instanceId)
          cs.registerOutParameter(2, OracleTypes.CURSOR)
          cs.registerOutParameter(3, OracleTypes.CURSOR)
          cs.execute()

          val scheme  = withCursor(cs, 2)(rs => readSingleSchemeRow(rs, instanceId))
          val returns = withCursor(cs, 3)(collectMonthlyReturns)

          UnsubmittedMonthlyReturns(scheme, returns)
        }
      }
    }
  }

  override def getMonthlyReturnForEdit(
    instanceId: String,
    taxYear: Int,
    taxMonth: Int
  ): Future[GetMonthlyReturnForEditResponse] = {
    logger.info(s"[CIS] getMonthlyReturnForEdit(instanceId=$instanceId, taxYear=$taxYear, taxMonth=$taxMonth)")
    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetMonthlyReturnForEdit) { cs =>
          cs.setString(1, instanceId)
          cs.setInt(2, taxYear)
          cs.setInt(3, taxMonth)
          cs.setString(4, "N")
          cs.registerOutParameter(5, OracleTypes.CURSOR)
          cs.registerOutParameter(6, OracleTypes.CURSOR)
          cs.registerOutParameter(7, OracleTypes.CURSOR)
          cs.registerOutParameter(8, OracleTypes.CURSOR)
          cs.registerOutParameter(9, OracleTypes.CURSOR)
          cs.execute()

          val scheme             = withCursor(cs, 5)(collectSchemes)
          val monthlyReturn      = withCursor(cs, 6)(collectMonthlyReturns)
          val monthlyReturnItems = withCursor(cs, 7)(collectMonthlyReturnItems)
          val subcontractors     = withCursor(cs, 8)(collectSubcontractors)
          val submission         = withCursor(cs, 9)(collectSubmissions)

          GetMonthlyReturnForEditResponse(
            scheme = scheme,
            monthlyReturn = monthlyReturn,
            monthlyReturnItems = monthlyReturnItems,
            subcontractors = subcontractors,
            submission = submission
          )
        }
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

  override def createMonthlyReturnItem(request: CreateMonthlyReturnItemRequest): Future[Unit] =
    Future {
      logger.info(
        s"[CIS] createMonthlyReturnItem(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth}, resourceReference=${request.resourceReference})"
      )
      db.withConnection { conn =>
        callCreateMonthlyReturnItem(
          conn,
          request.instanceId,
          request.taxYear,
          request.taxMonth,
          request.amendment,
          request.resourceReference
        )
      }
    }

  override def deleteMonthlyReturnItem(request: DeleteMonthlyReturnItemRequest): Future[Unit] =
    Future {
      logger.info(
        s"[CIS] deleteMonthlyReturnItem(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth}, resourceReference=${request.resourceReference})"
      )
      db.withConnection { conn =>
        callDeleteMonthlyReturnItem(
          conn,
          request.instanceId,
          request.taxYear,
          request.taxMonth,
          request.amendment,
          request.resourceReference
        )
      }
    }

  override def syncMonthlyReturnItems(request: SyncMonthlyReturnItemsRequest): Future[Unit] =
    Future {
      logger.info(
        s"[CIS] syncMonthlyReturnItems(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth}, creates=${request.createResourceReferences.size}, deletes=${request.deleteResourceReferences.size})"
      )

      db.withTransaction { conn =>
        val monthlyReturnsForEdit =
          getMonthlyReturnForEditInTransaction(conn, request.instanceId, request.taxYear, request.taxMonth)
        val status                = monthlyReturnsForEdit.monthlyReturn.headOption.flatMap(_.status).getOrElse("")

        if (status != "STARTED" && status != "VALIDATED") {
          throw new RuntimeException(s"Cannot sync monthly return items when status is $status")
        }

        val schemeVersionBefore = getSchemeVersion(conn, request.instanceId)

        request.deleteResourceReferences.distinct.foreach { ref =>
          callDeleteMonthlyReturnItem(
            conn,
            request.instanceId,
            request.taxYear,
            request.taxMonth,
            request.amendment,
            ref
          )
        }

        request.createResourceReferences.distinct.foreach { ref =>
          callCreateMonthlyReturnItem(
            conn,
            request.instanceId,
            request.taxYear,
            request.taxMonth,
            request.amendment,
            ref
          )
        }

        callUpdateSchemeVersion(conn, request.instanceId, schemeVersionBefore)
      }
    }

  // Scheme

  override def getScheme(instanceId: String): Future[Option[ContractorScheme]] =
    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetScheme) { cs =>
          cs.setString(1, instanceId)
          cs.registerOutParameter(2, Types.REF_CURSOR)
          cs.execute()

          withCursor(cs, 2) { rs =>
            if (rs != null && rs.next()) Some(readContractorScheme(rs))
            else None
          }
        }
      }
    }

  override def getSchemeEmail(instanceId: String): Future[Option[String]] =
    Future {
      db.withConnection { conn =>
        loadScheme(conn, instanceId).email
      }
    }

  override def createScheme(contractorScheme: CreateContractorSchemeParams): Future[Int] =
    Future {
      db.withConnection { conn =>
        callCreateScheme(conn, contractorScheme)
      }
    }

  override def updateScheme(contractorScheme: UpdateContractorSchemeParams): Future[Int] =
    Future {
      db.withConnection { conn =>
        callUpdateScheme(conn, contractorScheme)
      }
    }

  override def updateSchemeVersion(instanceId: String, version: Int): Future[Int] =
    Future {
      logger.info(s"[CIS] updateSchemeVersion(instanceId=$instanceId, version=$version)")
      db.withConnection { conn =>
        callUpdateSchemeVersion(conn, instanceId, version)
      }
    }

  // Subcontractor

  override def createAndUpdateSubcontractor(request: CreateAndUpdateSubcontractorRequest): Future[Unit] = {
    logger.info(
      s"[CIS] createAndUpdateSubcontractor(instanceId=${request.cisId})"
    )
    db.withTransaction { conn =>
      val scheme            = loadScheme(conn, request.cisId)
      val subbieResourceRef = callCreateSubcontractor(conn, scheme.schemeId, request.subcontractorType)
      callUpdateSchemeVersion(conn, request.cisId, scheme.version.getOrElse(0))
      callUpdateSubcontractor(conn, scheme.schemeId, subbieResourceRef, request)
    }
  }

  // Submission

  override def createSubmission(request: CreateSubmissionRequest): Future[String] =
    Future {
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

  override def updateMonthlyReturnSubmission(request: UpdateSubmissionRequest): Future[Unit] =
    Future {
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
      }
    }

  // Nil monthly return

  override def createNilMonthlyReturn(
    request: CreateNilMonthlyReturnRequest
  ): Future[CreateNilMonthlyReturnResponse] = {
    logger.info(
      s"[CIS] createNilMonthlyReturn(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth})"
    )
    Future {
      db.withConnection { conn =>
        callCreateMonthlyReturn(conn, request)

        CreateNilMonthlyReturnResponse(status = "STARTED")
      }
    }
  }

  override def updateNilMonthlyReturn(request: CreateNilMonthlyReturnRequest): Future[Unit] = {
    logger.info(
      s"[CIS] updateNilMonthlyReturn(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth})"
    )
    Future {
      db.withTransaction { conn =>
        val schemeVersionBefore = getSchemeVersion(conn, request.instanceId)

        callUpdateMonthlyReturn(conn, request)
        callUpdateSchemeVersion(conn, request.instanceId, schemeVersionBefore)
      }
    }
  }

  // Prepopulation

  override def applyPrepopulation(req: ApplyPrepopulationRequest): Future[Int] =
    Future {
      logger.info(
        s"[CIS] applyPrepopulation(schemeId=${req.schemeId}, instanceId=${req.instanceId}, version=${req.version}, subs=${req.subcontractorTypes.size})"
      )

      db.withTransaction { conn =>
        // 1) Update_Scheme – set name/UTR/prePopCount/prePopSuccessful (but NOT version)
        withCall(conn, CallUpdateScheme) { cs =>
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
          withCall(conn, CallCreateSubcontractor) { cs =>
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

  // govTalkStatus

  def getGovTalkStatus(req: GetGovTalkStatusRequest): Future[GetGovTalkStatusResponse] = {
    logger.info(s"[CIS] getGovTalkStatus(userIdentifier=${req.userIdentifier}, formResultID=${req.formResultID})")
    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetGovTalkStatus) { cs =>
          cs.setString(1, req.userIdentifier)
          cs.setString(2, req.formResultID)
          cs.registerOutParameter(3, OracleTypes.CURSOR)
          cs.execute()

          val statusRecords = withCursor(cs, 3)(collectGovtTalkStatusRecords)

          GetGovTalkStatusResponse(govtalk_status = statusRecords)
        }
      }
    }
  }

  def resetGovTalkStatus(req: ResetGovTalkStatusRequest): Future[Unit] = {
    logger.info(s"[CIS] resetGovTalkStatus(userIdentifier=${req.userIdentifier}, formResultID=${req.formResultID})")
    Future {
      db.withConnection { conn =>
        withCall(conn, CallResetGovTalkStatus) { cs =>
          cs.setString(1, req.userIdentifier)
          cs.setString(2, req.formResultID)
          cs.setString(3, "empty")
          cs.setString(4, "N")
          cs.setTimestamp(5, java.sql.Timestamp.valueOf(LocalDateTime.now()))
          cs.setNull(6, Types.TIMESTAMP)
          cs.setTimestamp(7, java.sql.Timestamp.valueOf(LocalDateTime.now()))
          cs.setInt(8, 0)
          cs.setInt(9, 0)
          cs.setString(10, req.oldProtocolStatus)
          cs.setString(11, "initial")
          cs.setString(12, req.gatewayURL)

          cs.execute()
        }
      }
    }
  }

  def createGovTalkStatusRecord(req: CreateGovTalkStatusRecordRequest): Future[Unit] = {
    logger.info(
      s"[CIS] createGovTalkStatusRecord(userIdentifier=${req.userIdentifier}, formResultID=${req.formResultID})"
    )
    Future {
      db.withConnection { conn =>
        withCall(conn, CallCreateGovTalkStatus) { cs =>
          cs.setString(1, req.userIdentifier)
          cs.setString(2, req.formResultID)
          cs.setString(3, req.correlationID)
          cs.setString(4, "N")
          cs.setTimestamp(5, java.sql.Timestamp.valueOf(LocalDateTime.now()))
          cs.setNull(6, Types.TIMESTAMP)
          cs.setTimestamp(7, java.sql.Timestamp.valueOf(LocalDateTime.now()))
          cs.setInt(8, 0)
          cs.setInt(9, 0)
          cs.setString(10, "initial")
          cs.setString(11, req.gatewayURL)

          cs.execute()
        }
      }
    }
  }

  // private helpers
  private def callCreateMonthlyReturn(conn: Connection, req: CreateNilMonthlyReturnRequest): Unit =
    withCall(conn, CallCreateMonthlyReturn) { cs =>
      cs.setString(1, req.instanceId)
      cs.setInt(2, req.taxYear)
      cs.setInt(3, req.taxMonth)
      cs.setString(4, "Y")
      cs.execute()
    }

  private def callUpdateSchemeVersion(conn: Connection, instanceId: String, currentVersion: Int): Int =
    withCall(conn, CallUpdateSchemeVersion) { cs =>
      cs.setString(1, instanceId)
      cs.setInt(2, currentVersion)
      cs.registerOutParameter(2, Types.INTEGER)
      cs.execute()
      cs.getInt(2)
    }

  private def callCreateScheme(conn: Connection, p: CreateContractorSchemeParams): Int =
    withCall(conn, CallCreateScheme) { cs =>
      cs.setString(1, p.instanceId)
      cs.setString(2, p.accountsOfficeReference)
      cs.setString(3, p.taxOfficeNumber)
      cs.setString(4, p.taxOfficeReference)
      cs.setString(5, p.utr.orNull)
      cs.setString(6, p.name.orNull)
      cs.setString(7, p.emailAddress.orNull)
      cs.setString(8, p.displayWelcomePage.orNull)
      cs.setOptionalInt(9, p.prePopCount)
      cs.setString(10, p.prePopSuccessful.orNull)
      cs.registerOutParameter(11, OracleTypes.INTEGER)

      cs.execute()
      cs.getInt(11)
    }

  private def callUpdateScheme(conn: Connection, p: UpdateContractorSchemeParams): Int =
    withCall(conn, CallUpdateScheme) { cs =>
      cs.setInt(1, p.schemeId)
      cs.setString(2, p.instanceId)
      cs.setString(3, p.accountsOfficeReference)
      cs.setString(4, p.taxOfficeNumber)
      cs.setString(5, p.taxOfficeReference)
      cs.setString(6, p.utr.orNull)
      cs.setString(7, p.name.orNull)
      cs.setString(8, p.emailAddress.orNull)
      cs.setString(9, p.displayWelcomePage.orNull)
      cs.setOptionalInt(10, p.prePopCount)
      cs.setString(11, p.prePopSuccessful.orNull)
      cs.setOptionalInt(12, p.version)
      cs.registerOutParameter(12, OracleTypes.INTEGER)

      cs.execute()
      cs.getInt(12)
    }

  private def callUpdateMonthlyReturn(conn: Connection, req: CreateNilMonthlyReturnRequest): Unit =
    withCall(conn, CallUpdateMonthlyReturn) { cs =>
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
  ): Long =
    withCall(conn, CallCreateSubmission) { cs =>
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
  ): Unit =
    withCall(conn, CallUpdateMonthlyReturnSubmission) { cs =>
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
    }

  private def callCreateMonthlyReturnItem(
    connection: Connection,
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    amendment: String,
    resourceReference: Long
  ): Unit =
    withCall(connection, CallCreateMonthlyReturnItem) { cs =>
      cs.setString(1, instanceId)
      cs.setInt(2, taxYear)
      cs.setInt(3, taxMonth)
      cs.setString(4, amendment)
      cs.setLong(5, resourceReference)
      cs.execute()
    }

  private def callDeleteMonthlyReturnItem(
    connection: Connection,
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    amendment: String,
    resourceReference: Long
  ): Unit =
    withCall(connection, CallDeleteMonthlyReturnItem) { cs =>
      cs.setString(1, instanceId)
      cs.setInt(2, taxYear)
      cs.setInt(3, taxMonth)
      cs.setString(4, amendment)
      cs.setLong(5, resourceReference)
      cs.execute()
    }

  private def getMonthlyReturnForEditInTransaction(
    connection: Connection,
    instanceId: String,
    taxYear: Int,
    taxMonth: Int
  ): GetMonthlyReturnForEditResponse =
    withCall(connection, CallGetMonthlyReturnForEdit) { cs =>
      cs.setString(1, instanceId)
      cs.setInt(2, taxYear)
      cs.setInt(3, taxMonth)
      cs.setString(4, "N")
      cs.registerOutParameter(5, OracleTypes.CURSOR)
      cs.registerOutParameter(6, OracleTypes.CURSOR)
      cs.registerOutParameter(7, OracleTypes.CURSOR)
      cs.registerOutParameter(8, OracleTypes.CURSOR)
      cs.registerOutParameter(9, OracleTypes.CURSOR)
      cs.execute()

      val scheme             = withCursor(cs, 5)(collectSchemes)
      val monthlyReturn      = withCursor(cs, 6)(collectMonthlyReturns)
      val monthlyReturnItems = withCursor(cs, 7)(collectMonthlyReturnItems)
      val subcontractors     = withCursor(cs, 8)(collectSubcontractors)
      val submission         = withCursor(cs, 9)(collectSubmissions)

      GetMonthlyReturnForEditResponse(
        scheme = scheme,
        monthlyReturn = monthlyReturn,
        monthlyReturnItems = monthlyReturnItems,
        subcontractors = subcontractors,
        submission = submission
      )
    }

  private def readSchemeRow(rs: ResultSet): SchemeRow = {
    val id      = rs.getLong("scheme_id")
    val version = rs.getOptionalInt("version")
    val email   = rs.getOptionalString("email_address").map(_.trim).filter(_.nonEmpty)
    SchemeRow(id, version, email)
  }

  private def loadScheme(conn: Connection, instanceId: String): SchemeRow =
    withCall(conn, CallGetScheme) { cs =>
      cs.setString(1, instanceId)
      cs.registerOutParameter(2, Types.REF_CURSOR)
      cs.execute()

      withCursor(cs, 2) { rs =>
        if (rs != null && rs.next()) readSchemeRow(rs)
        else throw new RuntimeException(s"No SCHEME row for instance_id=$instanceId")
      }
    }

  private def getSchemeVersion(conn: Connection, instanceId: String): Int =
    loadScheme(conn, instanceId).version.getOrElse(0)

  private def getMonthlyReturnId(conn: Connection, instanceId: String, taxYear: Int, taxMonth: Int): Long =
    withCall(conn, CallGetAllMonthlyReturns) { cs =>
      cs.setString(1, instanceId)
      cs.registerOutParameter(2, OracleTypes.CURSOR)
      cs.registerOutParameter(3, OracleTypes.CURSOR)
      cs.execute()

      discardCursor(cs, 2)

      withCursor(cs, 3) { rs =>
        var found: Option[Long] = None
        while (found.isEmpty && rs.next()) {
          val year  = rs.getInt("tax_year")
          val month = rs.getInt("tax_month")
          if (year == taxYear && month == taxMonth) found = Some(rs.getLong("monthly_return_id"))
        }

        found.getOrElse(
          throw new RuntimeException(s"No MONTHLY_RETURN for instance_id=$instanceId year=$taxYear month=$taxMonth")
        )
      }
    }

  private def readSingleSchemeRow(rs: ResultSet, instanceId: String): ContractorScheme =
    if (rs != null && rs.next()) readContractorScheme(rs)
    else throw new RuntimeException(s"No SCHEME row for instance_id=$instanceId")

  private def callCreateSubcontractor(conn: Connection, schemeId: Long, subcontractorType: SubcontractorType): Int = {
    val cs = conn.prepareCall(CallCreateSubcontractor)
    try {
      cs.setLong(1, schemeId)
      cs.setInt(2, 0) // initial version is 0
      cs.setString(3, subcontractorType.toString)
      cs.registerOutParameter(4, OracleTypes.INTEGER)

      cs.execute()

      cs.getInt(4)
    } finally cs.close()
  }

  private def callUpdateSubcontractor(
    conn: Connection,
    schemeId: Long,
    subbieResourceRef: Int,
    request: CreateAndUpdateSubcontractorRequest
  ): Future[Unit] =
    Future {
      val cs = conn.prepareCall(CallUpdateSubcontractor)
      try {
        cs.setLong(1, schemeId)

        cs.setInt(2, subbieResourceRef)
        cs.setOptionalString(3, request.utr)
        cs.setOptionalInt(4, None)
        cs.setOptionalString(5, None)
        cs.setOptionalString(6, None)
        cs.setOptionalString(7, request.firstName)
        cs.setOptionalString(8, request.nino)
        cs.setOptionalString(9, request.secondName)
        cs.setOptionalString(10, request.surname)

        cs.setOptionalString(11, None)
        cs.setOptionalString(12, request.tradingName)
        cs.setOptionalString(13, request.addressLine1)
        cs.setOptionalString(14, request.addressLine2)
        cs.setOptionalString(15, request.addressLine3)
        cs.setOptionalString(16, request.addressLine4)
        cs.setOptionalString(17, None)
        cs.setOptionalString(18, request.postcode)
        cs.setOptionalString(19, request.emailAddress)
        cs.setOptionalString(20, request.phoneNumber)
        cs.setOptionalString(21, None)
        cs.setOptionalString(22, request.worksReferenceNumber)

        cs.setOptionalString(23, None)
        cs.setOptionalString(24, None)
        cs.setOptionalString(25, None)
        cs.setOptionalString(26, None)
        cs.setOptionalString(27, None)
        cs.setOptionalString(28, None)

        cs.setOptionalTimestamp(29, None)
        cs.setOptionalInt(30, None)
        cs.registerOutParameter(30, Types.INTEGER)

        cs.execute()
      } finally cs.close()
    }

  override def getSubcontractorList(cisId: String): Future[GetSubcontractorListResponse] = Future {
    logger.info(s"[CIS] getSubcontractorList(cisId=$cisId)")

    db.withConnection { conn =>
      Using.resource(conn.prepareCall(CallGetSubcontractorList)) { cs =>
        cs.setString(1, cisId)
        cs.registerOutParameter(2, OracleTypes.CURSOR)
        cs.registerOutParameter(3, OracleTypes.CURSOR)
        cs.execute()

        val rsScheme = cs.getObject(2, classOf[ResultSet])
        try ()
        finally if (rsScheme != null) rsScheme.close()

        val rsSubs = cs.getObject(3, classOf[ResultSet])
        val subs   =
          try collectSubcontractorsResponse(rsSubs)
          finally if (rsSubs != null) rsSubs.close()

        GetSubcontractorListResponse(subcontractors = subs.toList)
      }
    }
  }

  private def collectSubcontractorsResponse(rs: ResultSet): Seq[Subcontractor] =
    if (rs == null) Seq.empty
    else {
      val b = Vector.newBuilder[Subcontractor]
      while (rs.next()) b += readSubcontractor(rs)
      b.result()
    }

}
