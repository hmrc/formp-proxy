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

case class FormSummary(
  formId: Long,
  userId: String,
  formType: String,
  versionId: Long,
  creationTimestamp: String,
  formStatus: String
)

object FormSummary {
  implicit val format: OFormat[FormSummary] = Json.format[FormSummary]
}

case class FormsResponse(
  forms: Seq[FormSummary]
)

object FormsResponse {
  implicit val format: OFormat[FormsResponse] = Json.format[FormsResponse]
}

case class FormDetail(
  formId: Long,
  userId: String,
  formType: String,
  versionId: Long,
  creationTimestamp: String,
  formStatus: String,
  submissionDatetime: Option[String],
  submissionReference: Option[String]
)

object FormDetail {
  implicit val format: OFormat[FormDetail] = Json.format[FormDetail]
}

case class StoreFormResponse(
  formId: Long,
  versionId: Long
)

object StoreFormResponse {
  implicit val format: OFormat[StoreFormResponse] = Json.format[StoreFormResponse]
}
