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

package uk.gov.hmrc.formpproxy.sdlt.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.formpproxy.sdlt.models.organisation.GetSdltOrgByStornRequest

class GetSdltOrgByStornRequestSpec extends AnyFreeSpec with Matchers {

  "GetSdltOrgByStornRequest" - {

    "must serialize to JSON correctly" in {
      val request = GetSdltOrgByStornRequest(
        storn = "STORN12345"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn" -> "STORN12345"
      )

      val result = json.validate[GetSdltOrgByStornRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj()

      val result = json.validate[GetSdltOrgByStornRequest]

      result.isError mustBe true
    }
  }
}
