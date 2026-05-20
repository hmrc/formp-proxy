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

class UpdateSubmissionRequestSpec extends SpecBase {

  "UpdateSubmissionRequest" - {

    "round-trip through JSON" in {
      val request = UpdateSubmissionRequest(
        instanceId = "1",
        taxYear = 2026,
        taxMonth = 7,
        hmrcMarkGenerated = "hmrc-mark",
        submittableStatus = "SUBMITTED",
        amendment = "Y"
      )

      Json.toJson(request).as[UpdateSubmissionRequest] mustBe request
    }

    "fail to deserialize when amendment is missing" in {
      val json = Json.obj(
        "instanceId"        -> "1",
        "taxYear"           -> 2026,
        "taxMonth"          -> 7,
        "hmrcMarkGenerated" -> "hmrc-mark",
        "submittableStatus" -> "SUBMITTED"
      )

      json.validate[UpdateSubmissionRequest].isError mustBe true
    }
  }
}
