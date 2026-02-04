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

    "read and write with all fields populated" in {
      val json = Json.parse("""
                              |{
                              |  "cisId": "1234567890",
                              |  "subcontractorType": "soletrader",
                              |  "firstName": "John",
                              |  "secondName": "Q",
                              |  "surname": "Smith",
                              |  "tradingName": "ACME",
                              |  "addressLine1": "1 Main Street",
                              |  "addressLine2": "Line 2",
                              |  "addressLine3": "Line 3",
                              |  "addressLine4": "Line 4",
                              |  "postcode": "AA1 1AA",
                              |  "nino": "AA123456A",
                              |  "utr": "1234567890",
                              |  "worksReferenceNumber": "WRN-123",
                              |  "emailAddress": "test@test.com",
                              |  "phoneNumber": "01234567890"
                              |}
        """.stripMargin)

      val model = json.as[CreateAndUpdateSubcontractorRequest]

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
