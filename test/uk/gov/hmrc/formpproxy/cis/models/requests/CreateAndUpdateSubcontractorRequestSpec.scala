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
import uk.gov.hmrc.formpproxy.cis.models.SoleTrader

class CreateAndUpdateSubcontractorRequestSpec extends AnyWordSpec with Matchers {

  "CreateAndUpdateSubcontractorRequest (JSON)" should {

    "read and write with required fields only" in {
      val json = Json.parse("""
                              |{
                              |  "cisId": "1234567890",
                              |  "subcontractorType": "soletrader"
                              |}
        """.stripMargin)

      val model = json.as[CreateAndUpdateSubcontractorRequest]
      Json.toJson(model) mustBe json
    }

    "should read and write with all fields populated" in {
      val model = CreateAndUpdateSubcontractorRequest(
        cisId = "1234567890",
        subcontractorType = SoleTrader,
        utr = Some("1234567890"),
        partnerUtr = Some("9999999999"),
        crn = Some("CRN123"),
        nino = Some("AA123456A"),
        partnershipTradingName = Some("My Partnership"),
        tradingName = Some("ACME"),
        addressLine1 = Some("1 Main Street"),
        addressLine2 = Some("Line 2"),
        city = Some("London"),
        county = Some("Greater London"),
        postcode = Some("AA1 1AA"),
        emailAddress = Some("test@test.com"),
        phoneNumber = Some("01234567890"),
        mobilePhoneNumber = Some("07123456789"),
        worksReferenceNumber = Some("WRN-123")
      )

      val json = Json.parse(
        """{
          | "cisId":"1234567890",
          | "subcontractorType":"soletrader",
          | "utr":"1234567890",
          | "partnerUtr":"9999999999",
          | "crn":"CRN123",
          | "nino":"AA123456A",
          | "partnershipTradingName":"My Partnership",
          | "tradingName":"ACME",
          | "addressLine1":"1 Main Street",
          | "addressLine2":"Line 2",
          | "city":"London",
          | "county":"Greater London",
          | "postcode":"AA1 1AA",
          | "emailAddress":"test@test.com",
          | "phoneNumber":"01234567890",
          | "mobilePhoneNumber":"07123456789",
          | "worksReferenceNumber":"WRN-123"
          |}""".stripMargin
      )

      json.as[CreateAndUpdateSubcontractorRequest] mustBe model
      Json.toJson(model) mustBe json
    }

    "fail to read missing cisId" in {
      val json = Json.parse("""
                              |{
                              |  "subcontractorType": "soletrader"
                              |}
        """.stripMargin)

      val result = json.validate[CreateAndUpdateSubcontractorRequest]
      result.isError mustBe true
    }

    "fail to read missing subcontractorType" in {
      val json = Json.parse("""
                              |{
                              |  "cisId": "1234567890"
                              |}
        """.stripMargin)

      val result = json.validate[CreateAndUpdateSubcontractorRequest]
      result.isError mustBe true
    }

    "fail to read subcontractorType with wrong type" in {
      val json = Json.parse("""
                              |{
                              |  "cisId": "1234567890",
                              |  "subcontractorType": "invalid type"
                              |}
        """.stripMargin)

      val result = json.validate[CreateAndUpdateSubcontractorRequest]
      result.isError mustBe true
    }
  }
}
