/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.formpproxy.cis.models.requests

import play.api.libs.json.{JsError, JsSuccess, Json, OFormat, Reads}

import java.time.{LocalDateTime, OffsetDateTime}
import java.time.format.DateTimeParseException

final case class ProcessVerificationResponseFromChrisRequest(
  instanceId: String,
  verificationBatchResourceRef: Long,
  acceptedTime: String,
  submissionStatus: String,
  irMarkReceived: Option[String],
  verificationResults: Seq[VerificationResult]
)

object ProcessVerificationResponseFromChrisRequest {
  implicit val format: OFormat[ProcessVerificationResponseFromChrisRequest] =
    Json.format[ProcessVerificationResponseFromChrisRequest]
}

final case class VerificationResult(
  resourceRef: Long,
  matched: Option[String],
  verified: Option[String],
  verificationNumber: Option[String],
  taxTreatment: String,
  verifiedDate: LocalDateTime
)

object VerificationResult {
  private implicit val localDateTimeReads: Reads[LocalDateTime] =
    Reads[LocalDateTime] { json =>
      json.validate[String].flatMap { s =>
        try JsSuccess(LocalDateTime.parse(s))
        catch {
          case _: DateTimeParseException =>
            try JsSuccess(OffsetDateTime.parse(s).toLocalDateTime)
            catch { case _: Exception => JsError(s"Cannot parse '$s' as a LocalDateTime") }
        }
      }
    }

  implicit val format: OFormat[VerificationResult] = Json.format[VerificationResult]
}
