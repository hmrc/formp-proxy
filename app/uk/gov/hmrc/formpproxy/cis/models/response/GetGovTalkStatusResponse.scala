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

package uk.gov.hmrc.formpproxy.cis.models.response

import play.api.libs.json.{Json, OFormat}
import java.time.LocalDateTime

final case class GovtTalkKStatusRecord(
  userIdentifier: String,
  formResultID: String,
  correlationID: String,
  formLock: String,
  createDate: Option[LocalDateTime],
  endStateDate: Option[LocalDateTime],
  lastMessageDate: Option[LocalDateTime],
  numPolls: Int,
  pollInterval: Int,
  protocolStatus: String,
  gatewayURL: String
)

object GovtTalkKStatusRecord {
  implicit val format: OFormat[GovtTalkKStatusRecord] = Json.format[GovtTalkKStatusRecord]
}

final case class GovtTalkStatusResponse(
  govtallk_status: Seq[GovtTalkKStatusRecord]
)

object GovtTalkStatusResponse {
  implicit val format: OFormat[GovtTalkStatusResponse] = Json.format[GovtTalkStatusResponse]
}
