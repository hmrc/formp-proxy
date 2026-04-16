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

case class FormSection(
  formDataId: String,
  formData: Option[String],
  formDataStatus: Option[String]
)

object FormSection {
  implicit val format: OFormat[FormSection] = Json.format[FormSection]
}

case class FormDataResponse(
  formId: Long,
  userId: String,
  formType: String,
  versionId: Long,
  creationTimestamp: String,
  formStatus: String,
  sections: Seq[FormSection]
)

object FormDataResponse {
  implicit val format: OFormat[FormDataResponse] = Json.format[FormDataResponse]
}

case class StoreFormDataRequest(
  formData: String,
  versionId: Long
)

object StoreFormDataRequest {
  implicit val format: OFormat[StoreFormDataRequest] = Json.format[StoreFormDataRequest]
}

case class StoreFormDataResponse(
  formId: Long,
  formDataId: String,
  versionId: Long
)

object StoreFormDataResponse {
  implicit val format: OFormat[StoreFormDataResponse] = Json.format[StoreFormDataResponse]
}
