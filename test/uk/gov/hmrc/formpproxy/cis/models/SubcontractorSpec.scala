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

import play.api.libs.json.Json
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDateTime

class SubcontractorSpec extends AnyWordSpec with Matchers {

  private val baseSubcontractor =
    Subcontractor(
      subcontractorId = 1L,
      utr = Some("1234567890"),
      pageVisited = None,
      partnerUtr = None,
      crn = None,
      firstName = None,
      nino = None,
      secondName = None,
      surname = None,
      partnershipTradingName = None,
      tradingName = None,
      subcontractorType = None,
      addressLine1 = Some("1 Test Street"),
      addressLine2 = None,
      addressLine3 = None,
      addressLine4 = None,
      country = Some("UK"),
      postcode = Some("AA1 1AA"),
      emailAddress = None,
      phoneNumber = None,
      mobilePhoneNumber = None,
      worksReferenceNumber = None,
      createDate = Some(LocalDateTime.parse("2025-01-01T10:00:00")),
      lastUpdate = None,
      subbieResourceRef = None,
      matched = None,
      autoVerified = None,
      verified = None,
      verificationNumber = None,
      taxTreatment = None,
      verificationDate = None,
      version = None,
      updatedTaxTreatment = None,
      lastMonthlyReturnDate = None,
      pendingVerifications = None
    )

  "Subcontractor JSON format" should {

    "serialize and deserialize correctly" in {

      val model =
        baseSubcontractor.copy(
          firstName = Some("First"),
          surname = Some("Last"),
          subcontractorType = Some("soletrader")
        )

      val json = Json.toJson(model)

      json.as[Subcontractor] mustBe model
    }
  }

  "Subcontractor.displayName" should {

    "return surname comma firstname for a sole trader" in {

      val subcontractor =
        baseSubcontractor.copy(
          subcontractorType = Some("soletrader"),
          firstName = Some("Alan"),
          surname = Some("Grant")
        )

      subcontractor.displayName mustBe "Grant, Alan"
    }

    "return surname for a sole trader when first name is missing" in {

      val subcontractor =
        baseSubcontractor.copy(
          subcontractorType = Some("soletrader"),
          firstName = None,
          surname = Some("Grant")
        )

      subcontractor.displayName mustBe "Grant"
    }

    "fall back to trading name for a sole trader" in {

      val subcontractor =
        baseSubcontractor.copy(
          subcontractorType = Some("soletrader"),
          firstName = None,
          surname = None,
          tradingName = Some("Grant Builders")
        )

      subcontractor.displayName mustBe "Grant Builders"
    }

    "return trading name for a company" in {

      val subcontractor =
        baseSubcontractor.copy(
          subcontractorType = Some("company"),
          tradingName = Some("ACME Ltd")
        )

      subcontractor.displayName mustBe "ACME Ltd"
    }

    "return trading name for a trust" in {

      val subcontractor =
        baseSubcontractor.copy(
          subcontractorType = Some("trust"),
          tradingName = Some("Family Trust")
        )

      subcontractor.displayName mustBe "Family Trust"
    }

    "return partnership trading name when present for a partnership" in {

      val subcontractor =
        baseSubcontractor.copy(
          subcontractorType = Some("partnership"),
          partnershipTradingName = Some("ABC Partnership"),
          tradingName = Some("Fallback Name")
        )

      subcontractor.displayName mustBe "ABC Partnership"
    }

    "fall back to trading name for a partnership when partnership trading name is missing" in {

      val subcontractor =
        baseSubcontractor.copy(
          subcontractorType = Some("partnership"),
          partnershipTradingName = None,
          tradingName = Some("Fallback Name")
        )

      subcontractor.displayName mustBe "Fallback Name"
    }

    "return default text when no display name can be determined" in {

      val subcontractor =
        baseSubcontractor.copy(
          subcontractorType = None,
          firstName = None,
          surname = None,
          tradingName = None,
          partnershipTradingName = None
        )

      subcontractor.displayName mustBe "No name provided"
    }

    "return default text for a sole trader with no name fields and no trading name" in {
      val subcontractor =
        baseSubcontractor.copy(
          subcontractorType = Some("soletrader"),
          firstName = None,
          surname = None,
          tradingName = None
        )
      subcontractor.displayName mustBe "No name provided"
    }

    "return default text when subcontractor type is unrecognised" in {
      val subcontractor =
        baseSubcontractor.copy(
          subcontractorType = Some("unknown"),
          firstName = Some("Alan"),
          surname = Some("Grant"),
          tradingName = Some("Grant Builders")
        )
      subcontractor.displayName mustBe "No name provided"
    }
  }
}
