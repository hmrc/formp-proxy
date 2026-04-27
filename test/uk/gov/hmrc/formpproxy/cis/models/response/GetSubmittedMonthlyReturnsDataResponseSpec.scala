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

package uk.gov.hmrc.formpproxy.cis.models.response

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.formpproxy.cis.models.ContractorScheme

class GetSubmittedMonthlyReturnsDataResponseSpec extends AnyWordSpec with Matchers {

  "GetSubmittedMonthlyReturnsDataResponse JSON format" should {

    "serialize and deserialize correctly" in {
      val model = GetSubmittedMonthlyReturnsDataResponse(
        scheme = ContractorScheme(
          schemeId = 100,
          instanceId = "abc-123",
          accountsOfficeReference = "accountsOfficeReference",
          taxOfficeNumber = "taxOfficeNumber",
          taxOfficeReference = "taxOfficeReference"
        ),
        monthlyReturn = Seq.empty,
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      val json = Json.toJson(model)
      json.as[GetSubmittedMonthlyReturnsDataResponse] mustBe model
    }
  }
}
