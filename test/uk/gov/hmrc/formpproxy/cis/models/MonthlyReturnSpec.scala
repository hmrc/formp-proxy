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
import play.api.libs.json.Json

import java.time.LocalDateTime

class MonthlyReturnSpec extends AnyWordSpec with Matchers {
  private val tsStr = "2025-08-31T12:34:56"
  private val ts = LocalDateTime.parse(tsStr)

  "MonthlyReturn (JSON)" should {

    "read and write a fully-populated object" in {
      val json = Json.parse(
        s"""
           |{
           |  "monthlyReturnId": 999,
           |  "taxYear": 2025,
           |  "taxMonth": 4,
           |  "nilReturnIndicator": "Y",
           |  "decEmpStatusConsidered": "Y",
           |  "decAllSubsVerified": "Y",
           |  "decInformationCorrect": "Y",
           |  "decNoMoreSubPayments": "N",
           |  "decNilReturnNoPayments": "N",
           |  "status": "Open",
           |  "lastUpdate": "$tsStr",
           |  "amendment": "N",
           |  "supersededBy": 1001
           |}
        """.stripMargin
      )

      val model = json.as[MonthlyReturn]
      model.lastUpdate mustBe Some(ts)

      Json.toJson(model) mustBe json
    }
  }
}
