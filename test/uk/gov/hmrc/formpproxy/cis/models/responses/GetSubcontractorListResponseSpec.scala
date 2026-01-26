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

package uk.gov.hmrc.formpproxy.cis.models.responses

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.formpproxy.cis.models.response.{Subcontractor, GetSubcontractorListResponse}

import java.time.LocalDateTime

final class GetSubcontractorListResponseSpec extends AnyWordSpec with Matchers {

  "GetSubcontractorListResponse Json format" should {

    "read FormP response JSON and parse subcontractors (including missing utr)" in {
      val json = Json.parse(
        """
          |{
          |  "subcontractors": [
          |    {
          |      "subcontractorId": 10101,
          |      "subbieResourceRef": 1,
          |      "type": "soletrader",
          |      "utr": "1111111111"
          |    },
          |    {
          |      "subcontractorId": 20202,
          |      "subbieResourceRef": 2,
          |      "type": "soletrader",
          |      "utr": "2222222222"
          |    },
          |    {
          |      "subcontractorId": 30303,
          |      "subbieResourceRef": 3,
          |      "type": "soletrader"
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      val result = json.validate[GetSubcontractorListResponse]
      result mustBe a[JsSuccess[?]]

      val out = result.get
      out.subcontractors must have size 3

      out.subcontractors.head.subcontractorId mustBe 10101L
      out.subcontractors.head.subbieResourceRef mustBe 1
      out.subcontractors.head.`type` mustBe "soletrader"
      out.subcontractors.head.utr mustBe Some("1111111111")

      out.subcontractors(2).subcontractorId mustBe 30303L
      out.subcontractors(2).utr mustBe None
    }

    "write a response to JSON" in {
      val model = GetSubcontractorListResponse(
        subcontractors = List(
          Subcontractor(
            subcontractorId = 1L,
            subbieResourceRef = 10,
            `type` = "soletrader",
            utr = Some("1234567890"),
            pageVisited = Some(1),
            partnerUtr = None,
            crn = None,
            firstName = None,
            nino = None,
            secondName = None,
            surname = None,
            partnershipTradingName = None,
            tradingName = None,
            addressLine1 = None,
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            country = None,
            postcode = None,
            emailAddress = None,
            phoneNumber = None,
            mobilePhoneNumber = None,
            worksReferenceNumber = None,
            version = Some(2),
            taxTreatment = None,
            updatedTaxTreatment = None,
            verificationNumber = None,
            createDate = Some(LocalDateTime.of(2026, 1, 23, 10, 0, 0)),
            lastUpdate = None,
            matched = None,
            verified = None,
            autoVerified = None,
            verificationDate = None,
            lastMonthlyReturnDate = None,
            pendingVerifications = None
          )
        )
      )

      val json = Json.toJson(model)

      (json \ "subcontractors").as[List[play.api.libs.json.JsValue]] must have size 1
      ((json \ "subcontractors")(0) \ "subcontractorId").as[Long] mustBe 1L
      ((json \ "subcontractors")(0) \ "subbieResourceRef").as[Int] mustBe 10
      ((json \ "subcontractors")(0) \ "type").as[String] mustBe "soletrader"
      ((json \ "subcontractors")(0) \ "utr").as[String] mustBe "1234567890"
      ((json \ "subcontractors")(0) \ "pageVisited").as[Int] mustBe 1
      ((json \ "subcontractors")(0) \ "version").as[Int] mustBe 2
    }

    "round-trip (model -> json -> model) without losing data" in {
      val model = GetSubcontractorListResponse(
        subcontractors = List(
          Subcontractor(
            subcontractorId = 999L,
            subbieResourceRef = 123,
            `type` = "company",
            utr = None,
            pageVisited = None,
            partnerUtr = Some("9999999999"),
            crn = Some("CRN123"),
            firstName = None,
            nino = None,
            secondName = None,
            surname = None,
            partnershipTradingName = None,
            tradingName = Some("ACME LTD"),
            addressLine1 = Some("1 Main St"),
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            country = Some("GB"),
            postcode = Some("AA1 1AA"),
            emailAddress = Some("a@b.com"),
            phoneNumber = None,
            mobilePhoneNumber = None,
            worksReferenceNumber = None,
            version = None,
            taxTreatment = None,
            updatedTaxTreatment = None,
            verificationNumber = None,
            createDate = None,
            lastUpdate = None,
            matched = None,
            verified = None,
            autoVerified = None,
            verificationDate = None,
            lastMonthlyReturnDate = None,
            pendingVerifications = None
          )
        )
      )

      val json = Json.toJson(model)
      json.validate[GetSubcontractorListResponse] mustBe JsSuccess(model)
    }
  }
}
