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

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.time.LocalDateTime

case class UpdateGovTalkStatusStatisticsRequest(
  userIdentifier: String,
  formResultID: String,
  lastMessageDate: LocalDateTime,
  numPolls: Int,
  pollInterval: Int,
  gatewayURL: String
)

object UpdateGovTalkStatusStatisticsRequest {
  private val nonEmptyStringReads: Reads[String] = Reads.StringReads.filter(
    JsonValidationError("String must not be empty")
  )(_.nonEmpty)

  given reads: Reads[UpdateGovTalkStatusStatisticsRequest] = (
    (__ \ "userIdentifier").read[String](nonEmptyStringReads) and
      (__ \ "formResultID").read[String](nonEmptyStringReads) and
      (__ \ "lastMessageDate").read[LocalDateTime] and
      (__ \ "numPolls").read[Int] and
      (__ \ "pollInterval").read[Int] and
      (__ \ "gatewayURL").read[String](nonEmptyStringReads)
  )(UpdateGovTalkStatusStatisticsRequest.apply _)

  given writes: OWrites[UpdateGovTalkStatusStatisticsRequest] = Json.writes[UpdateGovTalkStatusStatisticsRequest]

  given format: OFormat[UpdateGovTalkStatusStatisticsRequest] = OFormat(reads, writes)
}
