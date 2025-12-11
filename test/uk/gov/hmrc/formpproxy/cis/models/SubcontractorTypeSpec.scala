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

package uk.gov.hmrc.formpproxy.cis.models

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}

class SubcontractorTypeSpec extends AnyWordSpec with Matchers {

  "SubcontractorType" should {

    "have correct toString for each type" in {
      SoleTrader.toString mustBe "soletrader"
      Company.toString mustBe "company"
      Partnership.toString mustBe "partnership"
      Trust.toString mustBe "trust"
    }

    "serialize to JSON correctly" in {
      Json.toJson(SoleTrader)(SubcontractorType.given_Writes_SubcontractorType) mustBe JsString("soletrader")
      Json.toJson(Company)(SubcontractorType.given_Writes_SubcontractorType) mustBe JsString("company")
      Json.toJson(Partnership)(SubcontractorType.given_Writes_SubcontractorType) mustBe JsString("partnership")
      Json.toJson(Trust)(SubcontractorType.given_Writes_SubcontractorType) mustBe JsString("trust")
    }

    "deserialize from JSON correctly" in {
      JsString("soletrader").validate[SubcontractorType] mustBe JsSuccess(SoleTrader)
      JsString("company").validate[SubcontractorType] mustBe JsSuccess(Company)
      JsString("partnership").validate[SubcontractorType] mustBe JsSuccess(Partnership)
      JsString("trust").validate[SubcontractorType] mustBe JsSuccess(Trust)
    }

    "deserialize from JSON with uppercase correctly" in {
      JsString("SOLETRADER").validate[SubcontractorType] mustBe JsSuccess(SoleTrader)
      JsString("COMPANY").validate[SubcontractorType] mustBe JsSuccess(Company)
      JsString("PARTNERSHIP").validate[SubcontractorType] mustBe JsSuccess(Partnership)
      JsString("TRUST").validate[SubcontractorType] mustBe JsSuccess(Trust)
    }

    "deserialize from JSON with mixed case correctly" in {
      JsString("SoleTrader").validate[SubcontractorType] mustBe JsSuccess(SoleTrader)
      JsString("Company").validate[SubcontractorType] mustBe JsSuccess(Company)
      JsString("Partnership").validate[SubcontractorType] mustBe JsSuccess(Partnership)
      JsString("Trust").validate[SubcontractorType] mustBe JsSuccess(Trust)
    }

    "fail to deserialize invalid subcontractor type from JSON" in {
      val result = JsString("invalid").validate[SubcontractorType]
      result.isError mustBe true
      result mustBe a[JsError]
    }

    "roundtrip through JSON correctly for all types" in {
      val types = List(SoleTrader, Company, Partnership, Trust)
      types.foreach { subType =>
        val json   = Json.toJson(subType)(using SubcontractorType.given_Writes_SubcontractorType)
        val parsed = json.validate[SubcontractorType](using SubcontractorType.given_Reads_SubcontractorType)
        parsed mustBe JsSuccess(subType)
      }
    }
  }
}
