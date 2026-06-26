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

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.formpproxy.base.SpecBase

class GetSubmittedVerificationsRequestSpec extends SpecBase {

  "GetSubmittedVerificationsRequest" - {

    val request = GetSubmittedVerificationsRequest(
      instanceId = "abc-123"
    )

    "serialize to JSON" in {
      Json.toJson(request) mustBe Json.parse(
        """
          |{
          |  "instanceId": "abc-123"
          |}
          |""".stripMargin
      )
    }

    "deserialize from JSON" in {
      val json = Json.parse(
        """
          |{
          |  "instanceId": "abc-123"
          |}
          |""".stripMargin
      )

      json.validate[GetSubmittedVerificationsRequest] mustBe JsSuccess(request)
    }

    "fail to deserialize when instanceId is missing" in {
      Json
        .obj()
        .validate[GetSubmittedVerificationsRequest]
        .isError mustBe true
    }
  }
}
