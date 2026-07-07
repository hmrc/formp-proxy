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

package uk.gov.hmrc.formpproxy.cis.models

import play.api.libs.json.{JsString, Json, Reads, Writes}

import java.time.LocalDateTime

case class Subcontractor(
  subcontractorId: Long,
  utr: Option[String],
  pageVisited: Option[Int],
  partnerUtr: Option[String],
  crn: Option[String],
  firstName: Option[String],
  nino: Option[String],
  secondName: Option[String],
  surname: Option[String],
  partnershipTradingName: Option[String],
  tradingName: Option[String],
  subcontractorType: Option[String],
  addressLine1: Option[String],
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  country: Option[String],
  postcode: Option[String],
  emailAddress: Option[String],
  phoneNumber: Option[String],
  mobilePhoneNumber: Option[String],
  worksReferenceNumber: Option[String],
  createDate: Option[LocalDateTime],
  lastUpdate: Option[LocalDateTime],
  subbieResourceRef: Option[Long],
  matched: Option[String],
  autoVerified: Option[String],
  verified: Option[String],
  verificationNumber: Option[String],
  taxTreatment: Option[String],
  verificationDate: Option[LocalDateTime],
  version: Option[Int],
  updatedTaxTreatment: Option[String],
  lastMonthlyReturnDate: Option[LocalDateTime],
  pendingVerifications: Option[Int]
) {
  def displayName: String = {

    val first              = firstName.map(_.trim).filter(_.nonEmpty)
    val sur                = surname.map(_.trim).filter(_.nonEmpty)
    val trading            = tradingName.map(_.trim).filter(_.nonEmpty)
    val partnershipTrading = partnershipTradingName.map(_.trim).filter(_.nonEmpty)

    val individualName =
      sur.map { surname =>
        first match {
          case Some(firstName) => s"$surname, $firstName"
          case None            => surname
        }
      }

    subcontractorType
      .flatMap(SubcontractorType.fromString)
      .flatMap {
        case Partnership =>
          partnershipTrading.orElse(trading)

        case Company | Trust =>
          trading

        case SoleTrader =>
          individualName.orElse(trading)
      }
      .getOrElse("No name provided")
  }
}

object Subcontractor:
  given reads: Reads[Subcontractor]   = Json.reads[Subcontractor]
  given writes: Writes[Subcontractor] = s =>
    Json
      .writes[Subcontractor]
      .writes(s) + ("displayName", JsString(s.displayName))
