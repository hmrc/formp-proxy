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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class UpdateSubcontractorRequestSpec extends AnyWordSpec with Matchers {

  "UpdateSubcontractorRequest (JSON)" should {

    "read and write with required fields only" in {
      val json = Json.parse("""
                              |{
                              |  "utr": "1234567890",
                              |  "pageVisited": 1,
                              |  "schemeId": 999,
                              |  "subbieResourceRef": 123
                              |}
        """.stripMargin)

      val model = json.as[UpdateSubcontractorRequest]
      Json.toJson(model) mustBe json
    }

    "read and write with all fields populated" in {
      val json = Json.parse("""
                              |{
                              |  "utr": "1234567890",
                              |  "pageVisited": 2,
                              |  "partnerUtr": "2222222222",
                              |  "crn": "CRN-1",
                              |  "firstName": "John",
                              |  "nino": "AA123456A",
                              |  "secondName": "Q",
                              |  "surname": "Smith",
                              |  "partnershipTradingName": "PartnerTrade",
                              |  "tradingName": "ACME",
                              |  "addressLine1": "1 Main Street",
                              |  "addressLine2": "Line 2",
                              |  "addressLine3": "Line 3",
                              |  "addressLine4": "Line 4",
                              |  "country": "GB",
                              |  "postcode": "AA1 1AA",
                              |  "emailAddress": "test@test.com",
                              |  "phoneNumber": "01234567890",
                              |  "mobilePhoneNumber": "07123456789",
                              |  "worksReferenceNumber": "WRN-123",
                              |  "schemeId": 999,
                              |  "subbieResourceRef": 123,
                              |  "matched": "Y",
                              |  "autoVerified": "Y",
                              |  "verified": "Y",
                              |  "verificationNumber": "V123",
                              |  "taxTreatment": "GROSS",
                              |  "verificationDate": "2026-01-12T10:15:30",
                              |  "updatedTaxTreatment": "NET"
                              |}
        """.stripMargin)

      val model = json.as[UpdateSubcontractorRequest]

      Json.toJson(model) mustBe json
    }

    "fail to read missing utr" in {
      val json = Json.parse("""
                              |{
                              |  "pageVisited": 1,
                              |  "schemeId": 999,
                              |  "subbieResourceRef": 123
                              |}
        """.stripMargin)

      val result = json.validate[UpdateSubcontractorRequest]
      result.isError mustBe true
    }

    "fail to read missing pageVisited" in {
      val json = Json.parse("""
                              |{
                              |  "utr": "1234567890",
                              |  "schemeId": 999,
                              |  "subbieResourceRef": 123
                              |}
        """.stripMargin)

      val result = json.validate[UpdateSubcontractorRequest]
      result.isError mustBe true
    }

    "fail to read missing schemeId" in {
      val json = Json.parse("""
                              |{
                              |  "utr": "1234567890",
                              |  "pageVisited": 1,
                              |  "subbieResourceRef": 123
                              |}
        """.stripMargin)

      val result = json.validate[UpdateSubcontractorRequest]
      result.isError mustBe true
    }

    "fail to read missing subbieResourceRef" in {
      val json = Json.parse("""
                              |{
                              |  "utr": "1234567890",
                              |  "pageVisited": 1,
                              |  "schemeId": 999
                              |}
        """.stripMargin)

      val result = json.validate[UpdateSubcontractorRequest]
      result.isError mustBe true
    }

    "fail to read pageVisited with wrong type" in {
      val json = Json.parse("""
                              |{
                              |  "utr": "1234567890",
                              |  "pageVisited": "nope",
                              |  "schemeId": 999,
                              |  "subbieResourceRef": 123
                              |}
        """.stripMargin)

      val result = json.validate[UpdateSubcontractorRequest]
      result.isError mustBe true
    }

    "fail to read schemeId with wrong type" in {
      val json = Json.parse("""
                              |{
                              |  "utr": "1234567890",
                              |  "pageVisited": 1,
                              |  "schemeId": "nope",
                              |  "subbieResourceRef": 123
                              |}
        """.stripMargin)

      val result = json.validate[UpdateSubcontractorRequest]
      result.isError mustBe true
    }

    "fail to read subbieResourceRef with wrong type" in {
      val json = Json.parse("""
                              |{
                              |  "utr": "1234567890",
                              |  "pageVisited": 1,
                              |  "schemeId": 999,
                              |  "subbieResourceRef": "nope"
                              |}
        """.stripMargin)

      val result = json.validate[UpdateSubcontractorRequest]
      result.isError mustBe true
    }

    "fail to read verificationDate with wrong type" in {
      val json = Json.parse("""
                              |{
                              |  "utr": "1234567890",
                              |  "pageVisited": 1,
                              |  "schemeId": 999,
                              |  "subbieResourceRef": 123,
                              |  "verificationDate": "not-a-date"
                              |}
        """.stripMargin)

      val result = json.validate[UpdateSubcontractorRequest]
      result.isError mustBe true
    }
  }
}

