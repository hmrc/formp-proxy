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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import java.time.LocalDateTime

class SubcontractorSpec extends AnyWordSpec with Matchers {

  "Subcontractor JSON format" should {

    "serialize and deserialize correctly" in {
      val model = Subcontractor(
        subcontractorId = 1L,
        utr = Some("1234567890"),
        pageVisited = None,
        partnerUtr = None,
        crn = None,
        firstName = Some("First"),
        nino = None,
        secondName = None,
        surname = Some("Last"),
        partnershipTradingName = None,
        tradingName = None,
        subcontractorType = Some("Individual"),
        addressLine1 = Some("1 Test Street"),
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        country = Some("UK"),
        postCode = Some("AA1 1AA"),
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

      val json = Json.toJson(model)
      json.as[Subcontractor] mustBe model
    }
  }
}
