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

import play.api.libs.json.*
import uk.gov.hmrc.formpproxy.base.SpecBase

class UpdateVerificationSubmissionRequestSpec extends SpecBase {

  "UpdateVerificationSubmissionRequest" - {

    "round-trip through JSON with all fields" in {
      val request = UpdateVerificationSubmissionRequest(
        instanceId = "abc-123",
        verificationBatchId = 99L,
        verificationBatchResourceRef = 77L,
        submittableStatus = "FATAL_ERROR",
        govtalkErrorCode = Some("500"),
        govtalkErrorType = Some("timeOut"),
        govtalkErrorMessage = Some("timeOut")
      )

      Json.toJson(request).as[UpdateVerificationSubmissionRequest] mustBe request
    }

    "round-trip through JSON with optional fields absent" in {
      val request = UpdateVerificationSubmissionRequest(
        instanceId = "abc-123",
        verificationBatchId = 99L,
        verificationBatchResourceRef = 77L,
        submittableStatus = "ACCEPTED"
      )

      Json.toJson(request).as[UpdateVerificationSubmissionRequest] mustBe request
    }

    "fail to deserialize when submittableStatus is missing" in {
      val json = Json.obj(
        "instanceId"                   -> "abc-123",
        "verificationBatchId"          -> 99L,
        "verificationBatchResourceRef" -> 77L
      )

      json.validate[UpdateVerificationSubmissionRequest].isError mustBe true
    }
  }
}
