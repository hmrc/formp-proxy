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

package uk.gov.hmrc.formpproxy.nova.models

import play.api.libs.json.{Json, OFormat}

case class NotificationRefsRequest(
  requestRefNumCount: Int,
  userCredentials: String,
  versionId: Long
)

object NotificationRefsRequest {
  implicit val format: OFormat[NotificationRefsRequest] = Json.format[NotificationRefsRequest]
}

case class NotificationRefsResponse(
  taxYear: Int,
  minReference: Long,
  maxReference: Long,
  version: Long
)

object NotificationRefsResponse {
  implicit val format: OFormat[NotificationRefsResponse] = Json.format[NotificationRefsResponse]
}
