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

package uk.gov.hmrc.formpproxy.cis.models.requests

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.formpproxy.cis.models.{Company, Partnership, SoleTrader, Trust}

class CreateSubcontractorRequestSpec extends AnyWordSpec with Matchers {

  "CreateSubcontractorRequest (JSON)" should {

    "read and write with SoleTrader type" in {
      val json = Json.parse("""
          |{
          |  "subcontractorType": "soletrader",
          |  "version": 1
          |}
        """.stripMargin)

      val model = json.as[CreateSubcontractorRequest]
      model.subcontractorType mustBe SoleTrader
      model.version mustBe 1

      Json.toJson(model) mustBe json
    }

    "read and write with Company type" in {
      val json = Json.parse("""
          |{
          |  "subcontractorType": "company",
          |  "version": 2
          |}
        """.stripMargin)

      val model = json.as[CreateSubcontractorRequest]
      model.subcontractorType mustBe Company
      model.version mustBe 2

      Json.toJson(model) mustBe json
    }

    "read and write with Partnership type" in {
      val json = Json.parse("""
          |{
          |  "subcontractorType": "partnership",
          |  "version": 3
          |}
        """.stripMargin)

      val model = json.as[CreateSubcontractorRequest]
      model.subcontractorType mustBe Partnership
      model.version mustBe 3

      Json.toJson(model) mustBe json
    }

    "read and write with Trust type" in {
      val json = Json.parse("""
          |{
          |  "subcontractorType": "trust",
          |  "version": 4
          |}
        """.stripMargin)

      val model = json.as[CreateSubcontractorRequest]
      model.subcontractorType mustBe Trust
      model.version mustBe 4

      Json.toJson(model) mustBe json
    }

    "fail to read invalid subcontractor type" in {
      val json = Json.parse("""
          |{
          |  "subcontractorType": "invalid",
          |  "version": 1
          |}
        """.stripMargin)

      val result = json.validate[CreateSubcontractorRequest]
      result.isError mustBe true
    }

    "fail to read missing subcontractorType" in {
      val json = Json.parse("""
          |{
          |  "version": 1
          |}
        """.stripMargin)

      val result = json.validate[CreateSubcontractorRequest]
      result.isError mustBe true
    }

    "fail to read missing version" in {
      val json = Json.parse("""
          |{
          |  "subcontractorType": "soletrader"
          |}
        """.stripMargin)

      val result = json.validate[CreateSubcontractorRequest]
      result.isError mustBe true
    }
  }
}
