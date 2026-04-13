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
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDateTime

class SubmittedMonthlyReturnsSpec extends AnyWordSpec with Matchers {

  "SubmittedMonthlyReturns" should {

    "should serialise and deserialise correctly" in {
      val scheme = ContractorScheme(
        schemeId = 123,
        instanceId = "abc-123",
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        accountsOfficeReference = "123PA12345678"
      )

      val monthlyReturn = SubmittedMonthlyReturn(
        monthlyReturnId = 1L,
        taxYear = 2025,
        taxMonth = 1,
        nilReturnIndicator = Some("N"),
        decEmpStatusConsidered = Some("Y"),
        decAllSubsVerified = Some("Y"),
        decInformationCorrect = Some("Y"),
        decNoMoreSubPayments = Some("N"),
        decNilReturnNoPayments = Some("N"),
        status = Some("SUBMITTED"),
        lastUpdate = Some(LocalDateTime.of(2025, 1, 1, 0, 0)),
        amendment = Some("N"),
        supersededBy = None,
        amendmentStatus = None,
        monthlyReturnItems = None
      )

      val submission = Submission(
        submissionId = 123L,
        submissionType = "MONTHLY_RETURN",
        activeObjectId = None,
        status = Some("ACCEPTED"),
        hmrcMarkGenerated = None,
        hmrcMarkGgis = None,
        emailRecipient = None,
        acceptedTime = None,
        createDate = Some(LocalDateTime.of(2025, 1, 1, 12, 0)),
        lastUpdate = None,
        schemeId = 123L,
        agentId = None,
        l_Migrated = None,
        submissionRequestDate = None,
        govTalkErrorCode = None,
        govTalkErrorType = None,
        govTalkErrorMessage = None
      )

      val model = SubmittedMonthlyReturns(
        scheme = scheme,
        monthlyReturn = Seq(monthlyReturn),
        submissions = Seq(submission)
      )

      val json = Json.toJson(model)

      (json \ "scheme" \ "instanceId").as[String] mustBe "abc-123"
      (json \ "monthlyReturn").as[Seq[JsValue]] must have size 1
      (json \ "submission").as[Seq[JsValue]]    must have size 1

      json.as[SubmittedMonthlyReturns] mustBe model
    }
  }
}
