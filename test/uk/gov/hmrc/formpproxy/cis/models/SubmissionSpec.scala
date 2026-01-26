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

class SubmissionSpec extends AnyWordSpec with Matchers {

  "Submission JSON format" should {

    "serialize and deserialize correctly" in {
      val model = Submission(
        submissionId = 1L,
        submissionType = "MONTHLY_RETURN",
        activeObjectId = None,
        status = Some("ACCEPTED"),
        hmrcMarkGenerated = None,
        hmrcMarkGgis = None,
        emailRecipient = None,
        acceptedTime = None,
        createDate = Some(LocalDateTime.parse("2025-01-01T09:00:00")),
        lastUpdate = None,
        schemeId = 10L,
        agentId = None,
        l_Migrated = None,
        submissionRequestDate = None,
        govTalkErrorCode = None,
        govTalkErrorType = None,
        govTalkErrorMessage = None
      )

      val json = Json.toJson(model)
      json.as[Submission] mustBe model
    }
  }
}
