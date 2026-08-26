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

package uk.gov.hmrc.formpproxy.cis.models.responses

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.formpproxy.cis.models.response.DeleteVerificationResponse

class DeleteVerificationResponseSpec extends AnyWordSpec with Matchers {

  "DeleteVerificationResponse JSON format" should {

    "serialise to JSON" in {
      Json.toJson(DeleteVerificationResponse(Some(2L))) mustBe
        Json.obj("verificationsCounter" -> 2L)
    }

    "deserialise from JSON" in {
      Json.obj("verificationsCounter" -> 2L).as[DeleteVerificationResponse] mustBe
        DeleteVerificationResponse(Some(2L))
    }

    "deserialise when verificationsCounter is null" in {
      Json.obj("verificationsCounter" -> Json.parse("null")).as[DeleteVerificationResponse] mustBe
        DeleteVerificationResponse(None)
    }
  }
}
