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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.formpproxy.cis.models.{Company, SoleTrader}

class ApplyPrepopulationRequestSpec extends AnyFreeSpec with Matchers {

  "ApplyPrepopulationRequest.format" - {

    "writes then reads back (round-trip) with all fields" in {
      val model = ApplyPrepopulationRequest(
        schemeId = 789,
        instanceId = "abc-123",
        accountsOfficeReference = "111111111",
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        utr = Some("9876543210"),
        name = "Test Contractor",
        emailAddress = Some("test@test.com"),
        displayWelcomePage = Some("Y"),
        prePopCount = 5,
        prePopSuccessful = "Y",
        version = 1,
        subcontractorTypes = Seq(SoleTrader, Company)
      )

      val json = Json.toJson(model)
      json.validate[ApplyPrepopulationRequest] mustBe JsSuccess(model)
    }

    "reads when optional fields are missing" in {
      val json = Json.obj(
        "schemeId"                -> 789,
        "instanceId"              -> "abc-123",
        "accountsOfficeReference" -> "111111111",
        "taxOfficeNumber"         -> "123",
        "taxOfficeReference"      -> "AB456",
        "name"                    -> "Test Contractor",
        "prePopCount"             -> 5,
        "prePopSuccessful"        -> "Y",
        "version"                 -> 1,
        "subcontractorTypes"      -> Json.arr("soletrader", "company")
      )

      val expected = ApplyPrepopulationRequest(
        schemeId = 789,
        instanceId = "abc-123",
        accountsOfficeReference = "111111111",
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        utr = None,
        name = "Test Contractor",
        emailAddress = None,
        displayWelcomePage = None,
        prePopCount = 5,
        prePopSuccessful = "Y",
        version = 1,
        subcontractorTypes = Seq(SoleTrader, Company)
      )

      json.validate[ApplyPrepopulationRequest] mustBe JsSuccess(expected)
    }
  }
}
