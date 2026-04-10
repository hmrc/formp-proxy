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

package uk.gov.hmrc.formpproxy.nova.repositories

import oracle.jdbc.OracleTypes
import play.api.Logging
import play.api.db.Database
import play.api.db.NamedDatabase
import uk.gov.hmrc.formpproxy.nova.models.*

import java.sql.{ResultSet, SQLException, Types}
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

trait NovaSource {
  def getForms(userId: String, formType: String, formStatus: String): Future[FormsResponse]
  def getForm(formId: Long): Future[Option[FormDetail]]
  def createForm(userId: String): Future[StoreFormResponse]
  def updateForm(formId: Long, request: UpdateFormRequest): Future[StoreFormResponse]
  def updateFormStatus(formId: Long, request: UpdateFormStatusRequest): Future[StoreFormResponse]
  def deleteForm(formId: Long): Future[Unit]
}

@Singleton
class NovaFormpRepository @Inject() (@NamedDatabase("nova") db: Database)(implicit ec: ExecutionContext)
    extends NovaSource
    with Logging {

  override def getForms(userId: String, formType: String, formStatus: String): Future[FormsResponse] = {
    logger.info("[NOVA] getForms")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallGetForms)) { cs =>
          cs.setString(1, userId)
          cs.setString(2, formType)
          cs.setString(3, formStatus)
          cs.registerOutParameter(4, OracleTypes.CURSOR)
          cs.execute()

          val rs    = cs.getObject(4).asInstanceOf[ResultSet]
          val forms = Iterator
            .continually(rs)
            .takeWhile(r => r != null && r.next())
            .map(NovaRowMappers.readFormSummary)
            .toSeq

          FormsResponse(forms)
        }
      }
    }
  }

  override def getForm(formId: Long): Future[Option[FormDetail]] = {
    logger.info("[NOVA] getForm")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallGetForm)) { cs =>
          cs.setLong(1, formId)
          cs.registerOutParameter(2, OracleTypes.CURSOR)
          cs.execute()

          val rs = cs.getObject(2).asInstanceOf[ResultSet]
          if (rs != null && rs.next()) Some(NovaRowMappers.readFormDetail(rs))
          else None
        }
      }
    }
  }

  override def createForm(userId: String): Future[StoreFormResponse] = {
    logger.info("[NOVA] createForm")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallStoreForm)) { cs =>
          cs.setLong(1, 0L)
          cs.registerOutParameter(1, OracleTypes.NUMBER)
          cs.setString(2, userId)
          cs.setString(3, "NOVA")
          cs.setString(4, "UNSUBMITTED")
          cs.setLong(5, 0L)
          cs.registerOutParameter(5, OracleTypes.NUMBER)
          cs.execute()

          StoreFormResponse(
            formId = cs.getLong(1),
            versionId = cs.getLong(5)
          )
        }
      }
    }
  }

  override def updateForm(formId: Long, request: UpdateFormRequest): Future[StoreFormResponse] = {
    logger.info("[NOVA] updateForm")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallStoreForm)) { cs =>
          cs.setLong(1, formId)
          cs.registerOutParameter(1, OracleTypes.NUMBER)
          cs.setString(2, request.userId)
          cs.setString(3, "NOVA")
          cs.setString(4, request.formStatus)
          cs.setLong(5, request.versionId)
          cs.registerOutParameter(5, OracleTypes.NUMBER)
          cs.execute()

          StoreFormResponse(
            formId = cs.getLong(1),
            versionId = cs.getLong(5)
          )
        }
      }
    }.recoverWith { case e: SQLException => Future.failed(mapOracleError(e)) }
  }

  override def updateFormStatus(formId: Long, request: UpdateFormStatusRequest): Future[StoreFormResponse] = {
    logger.info("[NOVA] updateFormStatus")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallUpdateFormStatus)) { cs =>
          cs.setLong(1, formId)
          cs.setString(2, request.formStatus)
          request.submissionDatetime match {
            case Some(dt) => cs.setTimestamp(3, java.sql.Timestamp.from(java.time.Instant.parse(dt)))
            case None     => cs.setNull(3, Types.TIMESTAMP)
          }
          request.submissionReference match {
            case Some(ref) => cs.setString(4, ref)
            case None      => cs.setNull(4, Types.VARCHAR)
          }
          cs.setLong(5, request.versionId)
          cs.registerOutParameter(5, OracleTypes.NUMBER)
          cs.execute()

          StoreFormResponse(
            formId = formId,
            versionId = cs.getLong(5)
          )
        }
      }
    }.recoverWith { case e: SQLException => Future.failed(mapOracleError(e)) }
  }

  override def deleteForm(formId: Long): Future[Unit] = {
    logger.info("[NOVA] deleteForm")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallDeleteForm)) { cs =>
          cs.setLong(1, formId)
          cs.execute()
          ()
        }
      }
    }.recoverWith { case e: SQLException => Future.failed(mapOracleError(e)) }
  }

  private def mapOracleError(e: SQLException): Throwable =
    e.getErrorCode match {
      case 20001 => FormNotFoundException(e.getMessage)
      case 20000 => FormAlreadyUpdatedException(e.getMessage)
      case _     => e
    }
}

case class FormNotFoundException(message: String) extends RuntimeException(message)
case class FormAlreadyUpdatedException(message: String) extends RuntimeException(message)
