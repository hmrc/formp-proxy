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
import uk.gov.hmrc.formpproxy.cis.models.*
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.models.response.*
import uk.gov.hmrc.formpproxy.cis.repositories.CisRowMappers.*
import uk.gov.hmrc.formpproxy.cis.repositories.CisStoredProcedures.*
import uk.gov.hmrc.formpproxy.shared.utils.CallableStatementUtils.*
import uk.gov.hmrc.formpproxy.shared.utils.ResultSetUtils.*

import java.lang.Long
import java.sql.*
import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

trait CisMonthlyReturnSource {
  def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns]
  def getUnsubmittedMonthlyReturns(instanceId: String): Future[UnsubmittedMonthlyReturns]
  def getSubmittedMonthlyReturns(instanceId: String): Future[SubmittedMonthlyReturns]
  def createSubmission(request: CreateSubmissionRequest): Future[String]
  def updateMonthlyReturnSubmission(request: UpdateSubmissionRequest): Future[Unit]
  def createNilMonthlyReturn(request: CreateNilMonthlyReturnRequest): Future[CreateNilMonthlyReturnResponse]
  def updateMonthlyReturn(request: UpdateMonthlyReturnRequest): Future[Unit]
  def updateMonthlyReturnItem(request: UpdateMonthlyReturnItemRequest): Future[Unit]
  def createMonthlyReturn(request: CreateMonthlyReturnRequest): Future[Unit]
  def getSchemeEmail(instanceId: String): Future[Option[String]]
  def getScheme(instanceId: String): Future[Option[ContractorScheme]]
  def createScheme(contractorScheme: CreateContractorSchemeParams): Future[Int]
  def updateScheme(contractorScheme: UpdateContractorSchemeParams): Future[Int]
  def updateSchemeVersion(instanceId: String, version: Int): Future[Int]
  def applyPrepopulation(req: ApplyPrepopulationRequest): Future[Int]
  def createAndUpdateSubcontractor(record: CreateAndUpdateSubcontractorDatabaseRecord): Future[Unit]
  def getSubcontractorList(cisId: String): Future[GetSubcontractorListResponse]
  def getMonthlyReturnForEdit(
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    isAmendment: Boolean
  ): Future[GetMonthlyReturnForEditResponse]
  def createMonthlyReturnItem(request: CreateMonthlyReturnItemRequest): Future[Unit]
  def deleteMonthlyReturnItem(request: DeleteMonthlyReturnItemRequest): Future[Unit]
  def syncMonthlyReturnItems(request: SyncMonthlyReturnItemsRequest): Future[Unit]
  def getGovTalkStatus(req: GetGovTalkStatusRequest): Future[GetGovTalkStatusResponse]
  def updateGovTalkStatusCorrelationId(request: UpdateGovTalkStatusCorrelationIdRequest): Future[Unit]
  def resetGovTalkStatus(req: ResetGovTalkStatusRequest): Future[Unit]
  def updateGovTalkStatus(req: UpdateGovTalkStatusRequest): Future[Unit]
  def updateGovTalkStatusStatistics(req: UpdateGovTalkStatusStatisticsRequest): Future[Unit]
  def createGovTalkStatusRecord(req: CreateGovTalkStatusRecordRequest): Future[Unit]
  def getNewestVerificationBatch(instanceId: String): Future[GetNewestVerificationBatchResponse]
  def getCurrentVerificationBatch(instanceId: String): Future[GetCurrentVerificationBatchResponse]
  def deleteUnsubmittedMonthlyReturn(req: DeleteUnsubmittedMonthlyReturnRequest): Future[Unit]
  def getMonthlyReturnComplete(
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    amendment: String
  ): Future[GetMonthlyReturnCompleteResponse]
  def createSubmissionAndUpdateVerifications(
    req: CreateSubmissionAndUpdateVerificationsRequest
  ): Future[CreateSubmissionAndUpdateVerificationsResponse]
  def createVerificationBatchAndVerifications(
    req: CreateVerificationBatchAndVerificationsRequest
  ): Future[CreateVerificationBatchAndVerificationsResponse]
  def getSubmittedMonthlyReturnsData(
    request: GetSubmittedMonthlyReturnsDataRequest
  ): Future[GetSubmittedMonthlyReturnsDataResponse]
  def createAmendedMonthlyReturn(request: CreateAmendedMonthlyReturnRequest): Future[Unit]
  def modifyVerifications(req: ModifyVerificationsRequest): Future[Unit]
  def getBatchPollSubmissions(): Future[GetBatchPollSubmissionsResponse]
  def updateVerificationSubmission(req: UpdateVerificationSubmissionRequest): Future[Unit]
  def processVerificationResponseFromChris(req: ProcessVerificationResponseFromChrisRequest): Future[Unit]

  def getSubcontractorForDelete(cisId: String, subbieResourceRef: Long): Future[GetSubcontractorForDeleteResponse]

  def deleteSubcontractor(request: DeleteSubcontractorRequest): Future[Unit]

  def getSubmittedVerifications(req: GetSubmittedVerificationsRequest): Future[GetSubmittedVerificationsResponse]

  def getSubcontractor(cisId: String, subbieResourceRef: Long): Future[GetSubcontractorResponse]
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

  override def getSubmittedMonthlyReturns(instanceId: String): Future[SubmittedMonthlyReturns] = {
    logger.info(s"[CIS] getSubmittedMonthlyReturns(instanceId=$instanceId)")
    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetSubmittedMonthlyReturns) { cs =>
          cs.setString(1, instanceId)
          cs.registerOutParameter(2, OracleTypes.CURSOR)
          cs.registerOutParameter(3, OracleTypes.CURSOR)
          cs.registerOutParameter(4, OracleTypes.CURSOR)
          cs.execute()

          val scheme      = withCursor(cs, 2)(rs => readSingleSchemeRow(rs, instanceId))
          val returns     = withCursor(cs, 3)(collectSubmittedMonthlyReturns)
          val submissions = withCursor(cs, 4)(collectSubmissions)

          SubmittedMonthlyReturns(scheme, returns, submissions)
        }
      }
    }
  }

  override def getMonthlyReturnForEdit(
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    isAmendment: Boolean
  ): Future[GetMonthlyReturnForEditResponse] = {
    logger.info(s"[CIS] getMonthlyReturnForEdit(instanceId=$instanceId, taxYear=$taxYear, taxMonth=$taxMonth)")
    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetMonthlyReturnForEdit) { cs =>
          cs.setString(1, instanceId)
          cs.setInt(2, taxYear)
          cs.setInt(3, taxMonth)
          cs.setString(4, if (isAmendment) "Y" else "N")
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
        s"[CIS] syncMonthlyReturnItems(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth}, amendment=${request.amendment}, creates=${request.createResourceReferences.size}, deletes=${request.deleteResourceReferences.size})"
      )

      db.withTransaction { conn =>
        val monthlyReturnsForEdit =
          getMonthlyReturnForEditInTransaction(
            conn,
            request.instanceId,
            request.taxYear,
            request.taxMonth,
            request.amendment
          )
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

  override def getSubmittedMonthlyReturnsData(
    request: GetSubmittedMonthlyReturnsDataRequest
  ): Future[GetSubmittedMonthlyReturnsDataResponse] = {
    logger.info(
      s"[CIS] getSubmittedMonthlyReturns(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth})"
    )
    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetSubmittedMonthlyReturnsData) { cs =>
          cs.setString(1, request.instanceId)
          cs.setInt(2, request.taxYear)
          cs.setInt(3, request.taxMonth)
          cs.setString(4, request.amendment)
          cs.registerOutParameter(5, OracleTypes.CURSOR)
          cs.registerOutParameter(6, OracleTypes.CURSOR)
          cs.registerOutParameter(7, OracleTypes.CURSOR)
          cs.registerOutParameter(8, OracleTypes.CURSOR)
          cs.execute()

          val monthlyReturn      = withCursor(cs, 5)(collectMonthlyReturns)
          val monthlyReturnItems = withCursor(cs, 6)(collectMonthlyReturnItems)
          val scheme             = withCursor(cs, 7)(rs => readSingleSchemeRow(rs, request.instanceId))
          val submission         = withCursor(cs, 8)(collectSubmissions)

          GetSubmittedMonthlyReturnsDataResponse(scheme, monthlyReturn, monthlyReturnItems, submission)
        }
      }
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

  override def createAndUpdateSubcontractor(record: CreateAndUpdateSubcontractorDatabaseRecord): Future[Unit] = {
    logger.info(
      s"[CIS] createAndUpdateSubcontractor(instanceId=${record.cisId})"
    )
    db.withTransaction { conn =>
      val scheme            = loadScheme(conn, record.cisId)
      val subbieResourceRef = callCreateSubcontractor(conn, scheme.schemeId, record.subcontractorType)
      callUpdateSchemeVersion(conn, record.cisId, scheme.version.getOrElse(0))
      callUpdateSubcontractor(conn, scheme.schemeId, subbieResourceRef, record)
    }
  }

  // Submission

  override def createSubmission(request: CreateSubmissionRequest): Future[String] =
    Future {
      db.withTransaction { conn =>
        val monthlyReturnId =
          getMonthlyReturnId(conn, request.instanceId, request.taxYear, request.taxMonth, request.amendment)

        val submissionId = callCreateSubmission(
          conn,
          instanceId = request.instanceId,
          submissionType = "MONTHLY_RETURN",
          activeObjectId = monthlyReturnId,
          hmrcMarkGenerated = request.hmrcMarkGenerated.orNull,
          hmrcMarkGgis = null,
          emailRecipient = request.emailRecipient,
          agentId = request.agentId.orNull,
          submittableStatus = "STARTED"
        )

        submissionId.toString
      }
    }

  override def updateMonthlyReturnSubmission(request: UpdateSubmissionRequest): Future[Unit] =
    Future {
      db.withConnection { conn =>
        val monthlyReturnId =
          getMonthlyReturnId(conn, request.instanceId, request.taxYear, request.taxMonth, request.amendment)

        callUpdateMonthlyReturnSubmission(
          conn,
          submissionType = "MONTHLY_RETURN",
          activeObjectId = monthlyReturnId,
          hmrcMarkGenerated = request.hmrcMarkGenerated,
          hmrcMarkGgis = request.hmrcMarkGgis.orNull,
          emailRecipient = request.emailRecipient.orNull,
          submissionRequestDate = request.submissionRequestDate.map(Timestamp.valueOf).orNull,
          acceptedTime = request.acceptedTime.orNull,
          agentId = request.agentId.orNull,
          submittableStatus = request.submittableStatus,
          govtalkErrorCode = request.govtalkErrorCode.orNull,
          govtalkErrorType = request.govtalkErrorType.orNull,
          govtalkErrorMessage = request.govtalkErrorMessage.orNull,
          instanceId = request.instanceId,
          taxYear = request.taxYear,
          taxMonth = request.taxMonth,
          amendment = request.amendment
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

  override def updateMonthlyReturn(request: UpdateMonthlyReturnRequest): Future[Unit] = {
    logger.info(
      s"[CIS] updateMonthlyReturn(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth})"
    )
    Future {
      db.withTransaction { conn =>
        val schemeVersionBefore = getSchemeVersion(conn, request.instanceId)

        callUpdateMonthlyReturn(conn, request)
        callUpdateSchemeVersion(conn, request.instanceId, schemeVersionBefore)
      }
    }
  }

  override def updateMonthlyReturnItem(request: UpdateMonthlyReturnItemRequest): Future[Unit] = {
    logger.info(
      s"[CIS] updateMonthlyReturnItem(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth}, amendment=${request.amendment})"
    )
    Future {
      db.withTransaction { conn =>
        val schemeVersionBefore = getSchemeVersion(conn, request.instanceId)

        callUpdateMonthlyReturnItem(conn, request)
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

  override def updateGovTalkStatusCorrelationId(req: UpdateGovTalkStatusCorrelationIdRequest): Future[Unit] = {
    logger.info(
      s"[CIS] updateGovTalkStatusCorrelationId(userIdentifier=${req.userIdentifier}, formResultID=${req.formResultID}, correlationId=${req.correlationID}, pollInterval=${req.pollInterval}, gatewayUrl=${req.gatewayURL})"
    )
    Future {
      db.withConnection { conn =>
        withCall(conn, CallUpdateGetGovTalkStatusCorrelationId) { cs =>
          cs.setString(1, req.userIdentifier)
          cs.setString(2, req.formResultID)
          cs.setString(3, req.correlationID)
          cs.setInt(4, req.pollInterval)
          cs.setString(5, req.gatewayURL)
          cs.execute()
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

  def updateGovTalkStatus(req: UpdateGovTalkStatusRequest): Future[Unit] = {
    logger.info(s"[CIS] updateGovTalkStatus(userIdentifier=${req.userIdentifier}, formResultID=${req.formResultID})")
    Future {
      db.withConnection { conn =>
        withCall(conn, CallUpdateGovTalkStatus) { cs =>
          cs.setString(1, req.userIdentifier)
          cs.setString(2, req.formResultID)
          cs.setString(3, req.protocolStatus)
          cs.setOptionalTimestamp(4, req.endStateDate)
          cs.execute()
        }
      }
    }
  }

  def updateGovTalkStatusStatistics(req: UpdateGovTalkStatusStatisticsRequest): Future[Unit] = {
    logger.info(
      s"[CIS] updateGovTalkStatusStatistics(userIdentifier=${req.userIdentifier}, formResultID=${req.formResultID})"
    )
    Future {
      db.withConnection { conn =>
        withCall(conn, CallUpdateGovTalkStatusStatistics) { cs =>
          cs.setString(1, req.userIdentifier)
          cs.setString(2, req.formResultID)
          cs.setTimestamp(3, java.sql.Timestamp.valueOf(req.lastMessageDate))
          cs.setInt(4, req.numPolls)
          cs.setInt(5, req.pollInterval)
          cs.setString(6, req.gatewayURL)
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

  override def deleteUnsubmittedMonthlyReturn(request: DeleteUnsubmittedMonthlyReturnRequest): Future[Unit] =
    logger.info(
      s"[CIS] deleteUnsubmittedMonthlyReturn(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth})"
    )
    Future {
      db.withConnection { conn =>
        val schemeVersionBefore = getSchemeVersion(conn, request.instanceId)

        withCall(conn, CallUnsubmittedMonthlyReturn) { cs =>
          cs.setString(1, request.instanceId)
          cs.setInt(2, request.taxYear)
          cs.setInt(3, request.taxMonth)
          cs.setString(4, request.amendment)
          cs.execute()
        }

        callUpdateSchemeVersion(conn, request.instanceId, schemeVersionBefore)
      }
    }

  // Amend monthly return

  override def createAmendedMonthlyReturn(request: CreateAmendedMonthlyReturnRequest): Future[Unit] = {
    logger.info(
      s"[CIS] createAmendedMonthlyReturn(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth})"
    )
    Future {
      db.withConnection { conn =>
        withCall(conn, CallCreateAmendedMonthlyReturn) { cs =>
          cs.setString(1, request.instanceId)
          cs.setInt(2, request.taxYear)
          cs.setInt(3, request.taxMonth)
          cs.setInt(4, request.version)
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

  private def callUpdateMonthlyReturn(conn: Connection, req: UpdateMonthlyReturnRequest): Unit =
    withCall(conn, CallUpdateMonthlyReturn) { cs =>
      cs.setString(1, req.instanceId)
      cs.setInt(2, req.taxYear)
      cs.setInt(3, req.taxMonth)
      cs.setString(4, req.amendment)
      cs.setOptionalString(5, req.decEmpStatusConsidered)
      cs.setOptionalString(6, req.decAllSubsVerified)
      cs.setOptionalString(7, req.decInformationCorrect)
      cs.setOptionalString(8, req.decNoMoreSubPayments)
      cs.setOptionalString(9, req.decNilReturnNoPayments)
      cs.setString(10, req.nilReturnIndicator)
      cs.setString(11, req.status)
      cs.setOptionalLong(12, req.version)
      cs.registerOutParameter(12, Types.INTEGER)
      cs.execute()
    }

  private def callUpdateMonthlyReturnItem(conn: Connection, req: UpdateMonthlyReturnItemRequest): Unit =
    withCall(conn, CallUpdateMonthlyReturnItem) { cs =>
      cs.setString(1, req.instanceId)
      cs.setInt(2, req.taxYear)
      cs.setInt(3, req.taxMonth)
      cs.setString(4, req.amendment)
      cs.setLong(5, req.itemResourceReference)
      cs.setString(6, req.totalPayments)
      cs.setString(7, req.costOfMaterials)
      cs.setString(8, req.totalDeducted)
      cs.setString(9, req.subcontractorName)
      cs.setOptionalString(10, req.verificationNumber)
      cs.setNull(11, Types.INTEGER)
      cs.registerOutParameter(11, Types.INTEGER)
      cs.execute()
    }

  private def callCreateSubmission(
    conn: Connection,
    instanceId: String,
    submissionType: String,
    activeObjectId: Long,
    hmrcMarkGenerated: String,
    hmrcMarkGgis: String,
    emailRecipient: Option[String],
    agentId: String,
    submittableStatus: String
  ): Long =
    withCall(conn, CallCreateSubmission) { cs =>
      cs.setString(1, instanceId)
      cs.setString(2, submissionType)
      cs.setLong(3, activeObjectId)
      cs.setString(4, hmrcMarkGenerated)
      cs.setString(5, hmrcMarkGgis)
      cs.setOptionalString(6, emailRecipient)
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

  private def callUpdateSubmission(
    conn: Connection,
    instanceId: String,
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
    resourceRef: Long
  ): Unit =
    withCall(conn, CallUpdateSubmission) { cs =>
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
      cs.setLong(14, resourceRef)

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
    taxMonth: Int,
    amendment: String
  ): GetMonthlyReturnForEditResponse =
    withCall(connection, CallGetMonthlyReturnForEdit) { cs =>
      cs.setString(1, instanceId)
      cs.setInt(2, taxYear)
      cs.setInt(3, taxMonth)
      cs.setString(4, amendment)
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

  private def getMonthlyReturnId(
    conn: Connection,
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    amendment: String
  ): Long =
    withCall(conn, CallGetAllMonthlyReturns) { cs =>
      cs.setString(1, instanceId)
      cs.registerOutParameter(2, OracleTypes.CURSOR)
      cs.registerOutParameter(3, OracleTypes.CURSOR)
      cs.execute()

      discardCursor(cs, 2)

      withCursor(cs, 3) { rs =>
        var found: Option[Long] = None
        while (found.isEmpty && rs.next()) {
          val year         = rs.getInt("tax_year")
          val month        = rs.getInt("tax_month")
          val rowAmendment = rs.getString("amendment")
          if (year == taxYear && month == taxMonth && rowAmendment == amendment)
            found = Some(rs.getLong("monthly_return_id"))
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
    record: CreateAndUpdateSubcontractorDatabaseRecord
  ): Future[Unit] =
    Future {
      val cs = conn.prepareCall(CallUpdateSubcontractor)
      try {
        cs.setLong(1, schemeId)

        cs.setInt(2, subbieResourceRef)
        cs.setOptionalString(3, record.utr)
        cs.setOptionalInt(4, None)
        cs.setOptionalString(5, record.partnerUtr)
        cs.setOptionalString(6, record.crn)

        cs.setOptionalString(7, record.firstName)
        cs.setOptionalString(8, record.nino)
        cs.setOptionalString(9, record.secondName)
        cs.setOptionalString(10, record.surname)

        cs.setOptionalString(11, record.partnershipTradingName)
        cs.setOptionalString(12, record.tradingName)

        cs.setOptionalString(13, record.addressLine1)
        cs.setOptionalString(14, record.addressLine2)
        cs.setOptionalString(15, record.city)
        cs.setOptionalString(16, record.county)
        cs.setOptionalString(17, record.country)
        cs.setOptionalString(18, record.postcode)

        cs.setOptionalString(19, record.emailAddress)
        cs.setOptionalString(20, record.phoneNumber)
        cs.setOptionalString(21, record.mobilePhoneNumber)
        cs.setOptionalString(22, record.worksReferenceNumber)

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

  override def getMonthlyReturnComplete(
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    amendment: String
  ): Future[GetMonthlyReturnCompleteResponse] = {
    logger.info(
      s"[CIS] getMonthlyReturnComplete(instanceId=$instanceId, taxYear=$taxYear, taxMonth=$taxMonth, amendment=$amendment)"
    )
    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetMonthlyReturnComplete) { cs =>
          cs.setString(1, instanceId)
          cs.setInt(2, taxYear)
          cs.setInt(3, taxMonth)
          cs.setString(4, amendment)
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

          GetMonthlyReturnCompleteResponse(
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

  private def collectSubcontractorsResponse(rs: ResultSet): Seq[Subcontractor] =
    if (rs == null) Seq.empty
    else {
      val b = Vector.newBuilder[Subcontractor]
      while (rs.next()) b += readSubcontractor(rs)
      b.result()
    }

  override def getNewestVerificationBatch(instanceId: String): Future[GetNewestVerificationBatchResponse] = {
    logger.info(s"[CIS] getNewestVerificationBatch(instanceId=$instanceId)")
    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetNewestVerificationBatch) { cs =>
          cs.setString(1, instanceId)

          cs.registerOutParameter(2, OracleTypes.CURSOR) // scheme
          cs.registerOutParameter(3, OracleTypes.CURSOR) // subcontractors
          cs.registerOutParameter(4, OracleTypes.CURSOR) // verification_batch
          cs.registerOutParameter(5, OracleTypes.CURSOR) // verifications
          cs.registerOutParameter(6, OracleTypes.CURSOR) // ubmission
          cs.registerOutParameter(7, OracleTypes.CURSOR) // monthly_return
          cs.registerOutParameter(8, OracleTypes.CURSOR) // mr_submission

          cs.execute()

          val scheme            = withCursor(cs, 2)(collectSchemes).headOption
          val subcontractors    = withCursor(cs, 3)(collectSubcontractors)
          val verificationBatch = withCursor(cs, 4)(collectVerificationBatches).headOption
          val verifications     = withCursor(cs, 5)(collectVerifications)
          val submission        = withCursor(cs, 6)(collectSubmissionsForGetVerificationBatch).headOption
          val monthlyReturn     = withCursor(cs, 7)(collectMonthlyReturnsForGetVerificationBatch).headOption
          val mrSubmission      = withCursor(cs, 8)(collectSubmissionsForGetVerificationBatch).headOption

          GetNewestVerificationBatchResponse(
            scheme = scheme,
            subcontractors = subcontractors,
            verificationBatch = verificationBatch,
            verifications = verifications,
            submission = submission,
            monthlyReturn = monthlyReturn,
            monthlyReturnSubmission = mrSubmission
          )
        }
      }
    }
  }

  override def getCurrentVerificationBatch(instanceId: String): Future[GetCurrentVerificationBatchResponse] = {
    logger.info(s"[CIS] getCurrentVerificationBatch(instanceId=$instanceId)")
    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetCurrentVerificationBatch) { cs =>
          cs.setString(1, instanceId)

          cs.registerOutParameter(2, OracleTypes.CURSOR) // scheme
          cs.registerOutParameter(3, OracleTypes.CURSOR) // subcontractors
          cs.registerOutParameter(4, OracleTypes.CURSOR) // verification_batch
          cs.registerOutParameter(5, OracleTypes.CURSOR) // verifications
          cs.registerOutParameter(6, OracleTypes.CURSOR) // submission

          cs.execute()

          val scheme            = withCursor(cs, 2)(collectSchemes).headOption
          val subcontractors    = withCursor(cs, 3)(collectSubcontractors)
          val verificationBatch = withCursor(cs, 4)(collectVerificationBatches).headOption
          val verifications     = withCursor(cs, 5)(collectVerifications)
          val submission        = withCursor(cs, 6)(collectSubmissionsForGetVerificationBatch).headOption

          GetCurrentVerificationBatchResponse(
            scheme = scheme,
            subcontractors = subcontractors,
            verificationBatch = verificationBatch,
            verifications = verifications,
            submission = submission
          )
        }
      }
    }
  }

  override def createVerificationBatchAndVerifications(
    req: CreateVerificationBatchAndVerificationsRequest
  ): Future[CreateVerificationBatchAndVerificationsResponse] = {
    logger.info(
      s"[CIS] createVerificationBatchAndVerifications(instanceId=${req.instanceId}, subs=${req.verificationResourceReferences.size})"
    )

    Future {
      db.withTransaction { conn =>
        val batchRef: Long =
          callCreateVerificationBatch(conn, req.instanceId)

        req.verificationResourceReferences.distinct.foreach { verificationResourceRef =>
          callCreateVerification(
            conn = conn,
            instanceId = req.instanceId,
            verifBatchResourceRef = batchRef,
            verificationResourceRef = verificationResourceRef,
            actionIndicator = req.actionIndicator.orNull
          )
        }

        CreateVerificationBatchAndVerificationsResponse(batchRef)
      }
    }
  }

  private def callCreateVerificationBatch(conn: Connection, instanceId: String): Long =
    withCall(conn, CallCreateVerificationBatch) { cs =>
      cs.setString(1, instanceId)
      cs.registerOutParameter(2, Types.NUMERIC)
      cs.execute()
      cs.getLong(2)
    }

  private def callCreateVerification(
    conn: Connection,
    instanceId: String,
    verifBatchResourceRef: Long,
    verificationResourceRef: Long,
    actionIndicator: String
  ): Unit =
    withCall(conn, CallCreateVerification) { cs =>
      cs.setString(1, instanceId)
      cs.setLong(2, verifBatchResourceRef)
      cs.setLong(3, verificationResourceRef)
      cs.setString(4, actionIndicator)
      cs.execute()
    }

  override def createSubmissionAndUpdateVerifications(
    req: CreateSubmissionAndUpdateVerificationsRequest
  ): Future[CreateSubmissionAndUpdateVerificationsResponse] = {
    logger.info(
      s"[CIS] createSubmissionForVerification(instanceId=${req.instanceId}, verificationBatchId=${req.verificationBatchId}, verifs=${req.verifications.size})"
    )

    Future {
      db.withTransaction { conn =>
        val schemeRow = loadScheme(conn, req.instanceId)
        val schemeId  = schemeRow.schemeId

        val submissionId: Long =
          callCreateSubmission(
            conn,
            instanceId = req.instanceId,
            submissionType = "VERIFICATIONS",
            activeObjectId = req.verificationBatchId,
            hmrcMarkGenerated = req.irMarkGenerated.orNull,
            hmrcMarkGgis = null,
            emailRecipient = req.emailRecipient,
            agentId = req.agentId.orNull,
            submittableStatus = "STARTED"
          )

        callUpdateVerificationBatch(
          conn = conn,
          verificationBatchResourceRef = req.verificationBatchResourceRef,
          schemeId = schemeId,
          confirmArrangement = "Y",
          confirmCorrect = "Y",
          status = "STARTED"
        )

        req.verifications.foreach { v =>
          val actionIndicator =
            if (v.proceedVerification == "Y") "VERIFY" else "MATCH"

          callUpdateVerification(
            conn = conn,
            instanceId = req.instanceId,
            verificationBatchResourceRef = req.verificationBatchResourceRef,
            verificationResourceRef = v.verificationResourceRef,
            actionIndicator = actionIndicator,
            proceed = v.proceedVerification,
            subcontractorName = v.subcontractorName
          )
        }

        CreateSubmissionAndUpdateVerificationsResponse(submissionId)
      }
    }
  }

  private def callUpdateVerificationBatch(
    conn: Connection,
    verificationBatchResourceRef: Long,
    schemeId: Long,
    confirmArrangement: String,
    confirmCorrect: String,
    status: String
  ): Unit =
    withCall(conn, CallUpdateVerificationBatch) { cs =>
      cs.setLong(1, verificationBatchResourceRef)
      cs.setLong(2, schemeId)

      cs.setNull(3, Types.VARCHAR)
      cs.setString(4, confirmArrangement)
      cs.setString(5, confirmCorrect)
      cs.setString(6, status)
      cs.setNull(7, Types.VARCHAR)

      cs.setNull(8, Types.INTEGER)
      cs.registerOutParameter(8, Types.INTEGER)

      cs.execute()
    }

  private def callUpdateVerification(
    conn: Connection,
    instanceId: String,
    verificationBatchResourceRef: Long,
    verificationResourceRef: Long,
    actionIndicator: String,
    proceed: String,
    subcontractorName: String
  ): Unit =
    withCall(conn, CallUpdateVerification) { cs =>
      cs.setString(1, instanceId)
      cs.setLong(2, verificationBatchResourceRef)
      cs.setLong(3, verificationResourceRef)

      cs.setNull(4, Types.CHAR)
      cs.setNull(5, Types.VARCHAR)
      cs.setNull(6, Types.VARCHAR)

      cs.setString(7, actionIndicator)
      cs.setString(8, proceed)
      cs.setString(9, subcontractorName)

      cs.setNull(10, Types.INTEGER)
      cs.registerOutParameter(10, Types.INTEGER)

      cs.execute()
    }

  private def callDeleteVerifications(
    conn: Connection,
    instanceId: String,
    verificationResourceRef: Long
  ): Unit =
    withCall(conn, CallDeleteVerification) { cs =>
      cs.setString(1, instanceId)
      cs.setLong(2, verificationResourceRef)
      cs.execute()
    }

  override def modifyVerifications(req: ModifyVerificationsRequest): Future[Unit] = {
    logger.info(
      s"[CIS] modifyVerifications(instanceId=${req.instanceId}, " +
        s"noOfDeleteVerifications=${req.deleteVerifications.map(_.verificationResourceReferences.size).getOrElse(0)}, " +
        s"noOfCreateVerifications=${req.createVerifications.map(_.verificationResourceReferences.size).getOrElse(0)})"
    )

    Future {
      db.withTransaction { conn =>

        req.deleteVerifications.foreach { deleteVerifications =>
          deleteVerifications.verificationResourceReferences.distinct.foreach { verificationResourceRef =>
            callDeleteVerifications(
              conn = conn,
              instanceId = req.instanceId,
              verificationResourceRef = verificationResourceRef
            )
          }
        }

        req.createVerifications.foreach { createVerifications =>
          createVerifications.verificationResourceReferences.distinct.foreach { verificationResourceReference =>
            callCreateVerification(
              conn = conn,
              instanceId = req.instanceId,
              verifBatchResourceRef = createVerifications.verificationBatchResourceRef,
              verificationResourceRef = verificationResourceReference,
              actionIndicator = createVerifications.actionIndicator.orNull
            )
          }
        }
      }
    }
  }

  override def getBatchPollSubmissions(): Future[GetBatchPollSubmissionsResponse]                                     = {
    logger.info("[CIS][getBatchPollSubmissions] Calling GET_SUBMISSIONS_FOR_POLLING")

    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetBatchPollSubmissions) { cs =>
          cs.registerOutParameter(1, OracleTypes.CURSOR)
          cs.registerOutParameter(2, OracleTypes.CURSOR)

          cs.execute()

          val verificationSubmissions =
            withCursor(cs, 1)(collectVerificationSubmissionsToPoll)

          val monthlyReturnSubmissions =
            withCursor(cs, 2)(collectMonthlyReturnSubmissionsToPoll)

          GetBatchPollSubmissionsResponse(
            verificationSubmissions = verificationSubmissions,
            monthlyReturnSubmissions = monthlyReturnSubmissions
          )
        }
      }
    }
  }
  private def callGetSubmission(conn: Connection, instanceId: String, verificationBatchResourceRef: Long): Submission =
    withCall(conn, CallGetSubmission) { cs =>
      cs.setString(1, instanceId)
      cs.setLong(2, verificationBatchResourceRef)
      cs.registerOutParameter(3, OracleTypes.CURSOR)
      cs.execute()

      withCursor(cs, 3) { rs =>
        if (!rs.next())
          throw new RuntimeException(
            s"No submission found for instanceId=$instanceId, resourceRef=$verificationBatchResourceRef"
          )
        readSubmissionForGetVerificationBatch(rs)
      }
    }

  override def updateVerificationSubmission(req: UpdateVerificationSubmissionRequest): Future[Unit] = {
    logger.info(
      s"[CIS] updateVerificationSubmission(instanceId=${req.instanceId}, status=${req.submittableStatus})"
    )

    Future {
      db.withTransaction { conn =>
        val existing       = callGetSubmission(conn, req.instanceId, req.verificationBatchResourceRef)
        val activeObjectId = existing.activeObjectId.getOrElse(
          throw new RuntimeException(
            s"Submission activeObjectId missing for instanceId=${req.instanceId}, resourceRef=${req.verificationBatchResourceRef}"
          )
        )

        callUpdateSubmission(
          conn,
          instanceId = req.instanceId,
          submissionType = "VERIFICATIONS",
          activeObjectId = activeObjectId,
          hmrcMarkGenerated = req.hmrcMarkGenerated.orElse(existing.hmrcMarkGenerated).orNull,
          hmrcMarkGgis = existing.hmrcMarkGgis.orNull,
          emailRecipient = existing.emailRecipient.orNull,
          submissionRequestDate = req.submissionRequestDate
            .orElse(existing.submissionRequestDate)
            .map(Timestamp.valueOf)
            .orNull,
          acceptedTime = existing.acceptedTime.orNull,
          agentId = existing.agentId.orNull,
          submittableStatus = req.submittableStatus,
          govtalkErrorCode = req.govtalkErrorCode.orNull,
          govtalkErrorType = req.govtalkErrorType.orNull,
          govtalkErrorMessage = req.govtalkErrorMessage.orNull,
          resourceRef = req.verificationBatchResourceRef
        )
      }
    }
  }

  override def processVerificationResponseFromChris(req: ProcessVerificationResponseFromChrisRequest): Future[Unit] =
    Future {
      logger.info(
        s"[CIS] processVerificationResponseFromChris(instanceId=${req.instanceId}, verificationBatchResourceRef=${req.verificationBatchResourceRef})"
      )

      db.withTransaction { conn =>
        val existing = callGetSubmissionWithVerificationBatch(
          conn = conn,
          instanceId = req.instanceId,
          verificationBatchResourceRef = req.verificationBatchResourceRef
        )

        val scheme = existing.scheme.getOrElse(
          throw new RuntimeException(s"No scheme found for instanceId=${req.instanceId}")
        )

        val submission = existing.submission.getOrElse(
          throw new RuntimeException(
            s"No submission found for instanceId=${req.instanceId}, verificationBatchResourceRef=${req.verificationBatchResourceRef}"
          )
        )

        val verificationBatch = existing.verificationBatch.getOrElse(
          throw new RuntimeException(
            s"No verification batch found for instanceId=${req.instanceId}, verificationBatchResourceRef=${req.verificationBatchResourceRef}"
          )
        )

        req.verificationResults.foreach { result =>
          val verification = existing.verifications
            .find(_.verificationResourceRef.contains(result.resourceRef))
            .getOrElse(
              throw new RuntimeException(s"No verification found for resourceRef=${result.resourceRef}")
            )

          val subcontractor = existing.subcontractors
            .find(s => verification.subcontractorId.contains(s.subcontractorId))
            .getOrElse(
              throw new RuntimeException(s"No subcontractor found for resourceRef=${result.resourceRef}")
            )

          val subbieResourceRef = subcontractor.subbieResourceRef.getOrElse(
            throw new RuntimeException(
              s"No subbieResourceRef found for subcontractorId=${subcontractor.subcontractorId}, resourceRef=${result.resourceRef}"
            )
          )

          callUpdateSubcontractorFromChris(
            conn = conn,
            schemeId = scheme.schemeId.toLong,
            subbieResourceRef = subbieResourceRef,
            subcontractor = subcontractor,
            result = result
          )

          callUpdateVerificationFromChris(
            conn = conn,
            instanceId = req.instanceId,
            verificationBatchResourceRef = req.verificationBatchResourceRef,
            verification = verification,
            result = result
          )

          callUpdateVerificationBatchFromChris(
            conn = conn,
            verificationBatch = verificationBatch,
            submissionStatus = req.submissionStatus,
            result = result
          )
        }

        callUpdateSubmissionFromChris(
          conn = conn,
          submission = submission,
          req = req
        )
      }
    }

  private final case class SubmissionWithVerificationBatchRecord(
    scheme: Option[ContractorScheme],
    submission: Option[Submission],
    verificationBatch: Option[VerificationBatch],
    verifications: Seq[Verification],
    subcontractors: Seq[Subcontractor]
  )

  private def callGetSubmissionWithVerificationBatch(
    conn: Connection,
    instanceId: String,
    verificationBatchResourceRef: Long
  ): SubmissionWithVerificationBatchRecord =
    withCall(conn, CallGetSubmissionWithVerificationBatch) { cs =>
      cs.setString(1, instanceId)
      cs.setLong(2, verificationBatchResourceRef)

      cs.registerOutParameter(3, OracleTypes.CURSOR)
      cs.registerOutParameter(4, OracleTypes.CURSOR)
      cs.registerOutParameter(5, OracleTypes.CURSOR)
      cs.registerOutParameter(6, OracleTypes.CURSOR)
      cs.registerOutParameter(7, OracleTypes.CURSOR)

      cs.execute()

      SubmissionWithVerificationBatchRecord(
        submission = withCursor(cs, 3)(collectSubmissionsForGetVerificationBatch).headOption,
        verificationBatch = withCursor(cs, 4)(collectVerificationBatches).headOption,
        verifications = withCursor(cs, 5)(collectVerifications),
        subcontractors = withCursor(cs, 6)(collectSubcontractorsForGetVerificationBatchSubmission),
        scheme = withCursor(cs, 7)(collectSchemes).headOption
      )
    }

  private def callUpdateSubcontractorFromChris(
    conn: Connection,
    schemeId: Long,
    subbieResourceRef: Long,
    subcontractor: Subcontractor,
    result: VerificationResult
  ): Unit =
    withCall(conn, CallUpdateSubcontractor) { cs =>
      cs.setLong(1, schemeId)
      cs.setLong(2, subbieResourceRef)

      cs.setOptionalString(3, subcontractor.utr)
      cs.setOptionalInt(4, subcontractor.pageVisited)
      cs.setOptionalString(5, subcontractor.partnerUtr)
      cs.setOptionalString(6, subcontractor.crn)

      cs.setOptionalString(7, subcontractor.firstName)
      cs.setOptionalString(8, subcontractor.nino)
      cs.setOptionalString(9, subcontractor.secondName)
      cs.setOptionalString(10, subcontractor.surname)

      cs.setOptionalString(11, subcontractor.partnershipTradingName)
      cs.setOptionalString(12, subcontractor.tradingName)

      cs.setOptionalString(13, subcontractor.addressLine1)
      cs.setOptionalString(14, subcontractor.addressLine2)
      cs.setOptionalString(15, subcontractor.addressLine3)
      cs.setOptionalString(16, subcontractor.addressLine4)
      cs.setOptionalString(17, subcontractor.country)
      cs.setOptionalString(18, subcontractor.postcode)

      cs.setOptionalString(19, subcontractor.emailAddress)
      cs.setOptionalString(20, subcontractor.phoneNumber)
      cs.setOptionalString(21, subcontractor.mobilePhoneNumber)
      cs.setOptionalString(22, subcontractor.worksReferenceNumber)

      cs.setOptionalString(23, result.matched)
      cs.setOptionalString(24, subcontractor.autoVerified)
      cs.setOptionalString(25, result.verified)
      cs.setOptionalString(26, result.verificationNumber)
      cs.setString(27, result.taxTreatment)
      cs.setOptionalString(28, subcontractor.updatedTaxTreatment)
      cs.setOptionalTimestamp(29, result.verifiedDate)
      cs.setOptionalInt(30, subcontractor.version)
      cs.registerOutParameter(30, Types.INTEGER)

      cs.execute()
    }

  private def callUpdateVerificationFromChris(
    conn: Connection,
    instanceId: String,
    verificationBatchResourceRef: Long,
    verification: Verification,
    result: VerificationResult
  ): Unit =
    withCall(conn, CallUpdateVerification) { cs =>
      cs.setString(1, instanceId)
      cs.setLong(2, verificationBatchResourceRef)
      cs.setLong(3, result.resourceRef)

      cs.setOptionalString(4, result.matched)
      cs.setOptionalString(5, result.verificationNumber)
      cs.setString(6, result.taxTreatment)

      cs.setOptionalString(7, verification.actionIndicator)
      cs.setOptionalString(8, verification.proceed)
      cs.setOptionalString(9, verification.subcontractorName)

      cs.setOptionalInt(10, verification.version)
      cs.registerOutParameter(10, Types.INTEGER)

      cs.execute()
    }

  private def callUpdateVerificationBatchFromChris(
    conn: Connection,
    verificationBatch: VerificationBatch,
    submissionStatus: String,
    result: VerificationResult
  ): Unit =
    withCall(conn, CallUpdateVerificationBatch) { cs =>
      cs.setLong(
        1,
        verificationBatch.verifBatchResourceRef.getOrElse(
          throw new RuntimeException("Verification batch resource reference missing")
        )
      )
      cs.setLong(2, verificationBatch.schemeId)

      cs.setOptionalString(3, verificationBatch.proceedSession)
      cs.setOptionalString(4, verificationBatch.confirmArrangement)
      cs.setOptionalString(5, verificationBatch.confirmCorrect)
      cs.setString(6, submissionStatus)
      cs.setOptionalString(7, result.verificationNumber)

      cs.setOptionalInt(8, verificationBatch.version)
      cs.registerOutParameter(8, Types.INTEGER)

      cs.execute()
    }

  private def callUpdateSubmissionFromChris(
    conn: Connection,
    submission: Submission,
    req: ProcessVerificationResponseFromChrisRequest
  ): Unit =
    withCall(conn, CallUpdateSubmission) { cs =>
      cs.setString(1, submission.submissionType)
      cs.setLong(
        2,
        submission.activeObjectId.getOrElse(
          throw new RuntimeException("Submission activeObjectId missing")
        )
      )
      cs.setOptionalString(3, submission.hmrcMarkGenerated)
      cs.setOptionalString(4, req.irMarkReceived)
      cs.setString(5, submission.emailRecipient.orNull)
      cs.setTimestamp(6, submission.submissionRequestDate.map(Timestamp.valueOf).orNull)
      cs.setString(7, req.acceptedTime)
      cs.setString(8, submission.agentId.orNull)
      cs.setString(9, req.submissionStatus)
      cs.setString(10, submission.govTalkErrorCode.orNull)
      cs.setString(11, submission.govTalkErrorType.orNull)
      cs.setString(12, submission.govTalkErrorMessage.orNull)
      cs.setString(13, req.instanceId)
      cs.setLong(14, req.verificationBatchResourceRef)

      cs.execute()
    }

  override def deleteSubcontractor(request: DeleteSubcontractorRequest): Future[Unit] =
    Future {
      logger.info(
        s"[CIS] DeleteSubcontractorRequest(instanceId=${request.instanceId}, resourceReference=${request.subbieResourceRef})"
      )
      db.withConnection { conn =>
        callDeleteSubcontractor(
          conn,
          request.instanceId,
          request.subbieResourceRef
        )
      }
    }

  private def callDeleteSubcontractor(
    connection: Connection,
    instanceId: String,
    subbieResourceRef: Long
  ): Unit =
    withCall(connection, CallDeleteSubcontractor) { cs =>
      cs.setString(1, instanceId)
      cs.setLong(2, subbieResourceRef)
      cs.execute()
    }

  override def getSubcontractorForDelete(
    cisId: String,
    subbieResourceRef: Long
  ): Future[GetSubcontractorForDeleteResponse] = {
    logger.info(
      s"[CIS] getSubcontractorForDelete(cisId=$cisId, subbieResourceRef=$subbieResourceRef)"
    )

    Future {
      db.withConnection { conn =>
        withCall(conn, CallGetSubcontractorForDelete) { cs =>
          cs.setString(1, cisId)
          cs.setLong(2, subbieResourceRef)

          cs.registerOutParameter(3, OracleTypes.CURSOR)
          cs.registerOutParameter(4, OracleTypes.CURSOR)
          cs.registerOutParameter(5, OracleTypes.CURSOR)
          cs.registerOutParameter(6, Types.VARCHAR)

          cs.execute()

          discardCursor(cs, 3)
          discardCursor(cs, 5)

          val subcontractor             =
            withCursor(cs, 4)(collectSubcontractors) match {
              case Seq(subcontractor) =>
                subcontractor

              case Seq()    =>
                throw new IllegalStateException(
                  s"No subcontractor found for cisId=$cisId and subbieResourceRef=$subbieResourceRef"
                )
              case multiple =>
                throw new IllegalStateException(
                  s"Expected exactly one subcontractor for cisId=$cisId and subbieResourceRef=$subbieResourceRef, got ${multiple.size}"
                )
            }
          val subcontractorCanBeDeleted =
            Option(cs.getString(6)).map(_.trim.toLowerCase) match {
              case Some("true")  => true
              case Some("false") => false
              case other         =>
                logger.warn(
                  s"[getSubcontractorForDelete] unexpected flag value: '$other'"
                )
                false
            }

          GetSubcontractorForDeleteResponse(
            subcontractorName = subcontractor.displayName,
            subcontractorCanBeDeleted = subcontractorCanBeDeleted
          )
        }
      }
    }
  }

  override def getSubmittedVerifications(
    req: GetSubmittedVerificationsRequest
  ): Future[GetSubmittedVerificationsResponse] =
    Future {
      logger.info(s"[CIS] getSubmittedVerifications(instanceId=${req.instanceId})")

      db.withConnection { conn =>
        callGetSubmittedVerifications(conn, req.instanceId)
      }
    }

  private def callGetSubmittedVerifications(
    conn: Connection,
    instanceId: String
  ): GetSubmittedVerificationsResponse =
    withCall(conn, CallGetSubmittedVerifications) { cs =>
      cs.setString(1, instanceId)

      cs.registerOutParameter(2, OracleTypes.CURSOR) // scheme
      cs.registerOutParameter(3, OracleTypes.CURSOR) // subcontractors
      cs.registerOutParameter(4, OracleTypes.CURSOR) // verification batches
      cs.registerOutParameter(5, OracleTypes.CURSOR) // verifications
      cs.registerOutParameter(6, OracleTypes.CURSOR) // submissions

      cs.execute()

      GetSubmittedVerificationsResponse(
        scheme = withCursor(cs, 2)(collectSchemes),
        subcontractors = withCursor(cs, 3)(collectSubcontractors),
        verificationBatches = withCursor(cs, 4)(collectVerificationBatchesForGetSubmittedVerifications),
        verifications = withCursor(cs, 5)(collectVerifications),
        submissions = withCursor(cs, 6)(collectSubmissions)
      )
    }

  override def getSubcontractor(
    cisId: String,
    subbieResourceRef: Long
  ): Future[GetSubcontractorResponse] =
    Future {
      logger.info(
        s"[CIS] getSubcontractor(cisId=$cisId, subbieResourceRef=$subbieResourceRef)"
      )

      db.withConnection { conn =>
        withCall(conn, CallGetSubcontractor) { cs =>
          cs.setString(1, cisId)
          cs.setLong(2, subbieResourceRef)

          cs.registerOutParameter(3, OracleTypes.CURSOR)
          cs.registerOutParameter(4, OracleTypes.CURSOR)
          cs.registerOutParameter(5, OracleTypes.CURSOR)

          cs.execute()

          GetSubcontractorResponse(
            scheme = withCursor(cs, 3)(collectSchemes).headOption,
            subcontractor = withCursor(cs, 4)(collectSubcontractors).headOption,
            otherInfo = withCursor(cs, 5)(collectSubcontractorOtherInfo)
          )
        }
      }
    }

}
