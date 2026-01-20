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

class UnsubmittedMonthlyReturnsSpec extends AnyWordSpec with Matchers {

  "UnsubmittedMonthlyReturns" should {

    "serialise and deserialise correctly" in {
      val scheme = ContractorScheme(
        schemeId = 1,
        instanceId = "abc-123",
        accountsOfficeReference = "123PA123456789",
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456"
      )

      val monthlyReturn = MonthlyReturn(
        monthlyReturnId = 1,
        taxYear = 2025,
        taxMonth = 4,
        nilReturnIndicator = Some("Y"),
        decEmpStatusConsidered = None,
        decAllSubsVerified = None,
        decInformationCorrect = None,
        decNoMoreSubPayments = None,
        decNilReturnNoPayments = None,
        status = Some("STARTED"),
        lastUpdate = Some(LocalDateTime.parse("2024-06-30T10:15:30")),
        amendment = None,
        supersededBy = None
      )

      val model = UnsubmittedMonthlyReturns(
        scheme = scheme,
        monthlyReturn = Seq(monthlyReturn)
      )

      val json   = Json.toJson(model)
      val parsed = json.validate[UnsubmittedMonthlyReturns].get
      parsed mustBe model
    }
  }
}
