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

import play.api.libs.json.{Format, Json, Reads, Writes}
import uk.gov.hmrc.formpproxy.cis.models.SubcontractorType

final case class CreateAndUpdateSubcontractorRequest(
  cisId: String,
  subcontractorType: SubcontractorType,
  utr: Option[String],
  partnerUtr: Option[String],
  crn: Option[String],
  firstName: Option[String],
  secondName: Option[String],
  surname: Option[String],
  nino: Option[String],
  partnershipTradingName: Option[String],
  tradingName: Option[String],
  addressLine1: Option[String],
  addressLine2: Option[String],
  city: Option[String],
  county: Option[String],
  country: Option[String],
  postcode: Option[String],
  emailAddress: Option[String],
  phoneNumber: Option[String],
  mobilePhoneNumber: Option[String],
  worksReferenceNumber: Option[String]
)

object CreateAndUpdateSubcontractorRequest {
  given Format[CreateAndUpdateSubcontractorRequest] = Json.format[CreateAndUpdateSubcontractorRequest]
}
