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

class ReturnVersioningSpec extends AnyFreeSpec with Matchers {

  "ReturnVersionUpdateRequest" - {

    "must serialize to JSON correctly with version 1" in {
      val request = ReturnVersionUpdateRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        currentVersion = "1"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "currentVersion").as[String] mustBe "1"
    }

    "must serialize to JSON correctly with version 0" in {
      val request = ReturnVersionUpdateRequest(
        storn = "STORN11111",
        returnResourceRef = "100003",
        currentVersion = "0"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN11111"
      (json \ "returnResourceRef").as[String] mustBe "100003"
      (json \ "currentVersion").as[String] mustBe "0"
    }

    "must serialize to JSON correctly with higher version number" in {
      val request = ReturnVersionUpdateRequest(
        storn = "STORN99999",
        returnResourceRef = "100002",
        currentVersion = "5"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "currentVersion").as[String] mustBe "5"
    }

    "must serialize to JSON correctly with very high version number" in {
      val request = ReturnVersionUpdateRequest(
        storn = "STORN77777",
        returnResourceRef = "999999",
        currentVersion = "10"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN77777"
      (json \ "returnResourceRef").as[String] mustBe "999999"
      (json \ "currentVersion").as[String] mustBe "10"
    }

    "must deserialize from JSON correctly with version 1" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001",
        "currentVersion"    -> "1"
      )

      val result = json.validate[ReturnVersionUpdateRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.currentVersion mustBe "1"
    }

    "must deserialize from JSON correctly with version 0" in {
      val json = Json.obj(
        "storn"             -> "STORN11111",
        "returnResourceRef" -> "100003",
        "currentVersion"    -> "0"
      )

      val result = json.validate[ReturnVersionUpdateRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN11111"
      request.returnResourceRef mustBe "100003"
      request.currentVersion mustBe "0"
    }

    "must deserialize from JSON correctly with higher version number" in {
      val json = Json.obj(
        "storn"             -> "STORN99999",
        "returnResourceRef" -> "100002",
        "currentVersion"    -> "5"
      )

      val result = json.validate[ReturnVersionUpdateRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN99999"
      request.returnResourceRef mustBe "100002"
      request.currentVersion mustBe "5"
    }

    "must deserialize from JSON correctly with very high version number" in {
      val json = Json.obj(
        "storn"             -> "STORN77777",
        "returnResourceRef" -> "999999",
        "currentVersion"    -> "10"
      )

      val result = json.validate[ReturnVersionUpdateRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN77777"
      request.returnResourceRef mustBe "999999"
      request.currentVersion mustBe "10"
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "currentVersion"    -> "1"
      )

      val result = json.validate[ReturnVersionUpdateRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj(
        "storn"          -> "STORN12345",
        "currentVersion" -> "1"
      )

      val result = json.validate[ReturnVersionUpdateRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when currentVersion is missing" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[ReturnVersionUpdateRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when all fields are missing" in {
      val json = Json.obj()

      val result = json.validate[ReturnVersionUpdateRequest]

      result.isError mustBe true
    }

    "must handle currentVersion as string for numeric values" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001",
        "currentVersion"    -> "123"
      )

      val result = json.validate[ReturnVersionUpdateRequest]

      result mustBe a[JsSuccess[_]]
      result.get.currentVersion mustBe "123"
    }
  }

  "ReturnVersionUpdateReturn" - {

    "must serialize to JSON correctly with version 1" in {
      val response = ReturnVersionUpdateReturn(newVersion = 1)

      val json = Json.toJson(response)

      (json \ "newVersion").as[Int] mustBe 1
    }

    "must serialize to JSON correctly with version 2" in {
      val response = ReturnVersionUpdateReturn(newVersion = 2)

      val json = Json.toJson(response)

      (json \ "newVersion").as[Int] mustBe 2
    }

    "must serialize to JSON correctly with version 6" in {
      val response = ReturnVersionUpdateReturn(newVersion = 6)

      val json = Json.toJson(response)

      (json \ "newVersion").as[Int] mustBe 6
    }

    "must serialize to JSON correctly with version 11" in {
      val response = ReturnVersionUpdateReturn(newVersion = 11)

      val json = Json.toJson(response)

      (json \ "newVersion").as[Int] mustBe 11
    }

    "must serialize to JSON correctly with version 0" in {
      val response = ReturnVersionUpdateReturn(newVersion = 0)

      val json = Json.toJson(response)

      (json \ "newVersion").as[Int] mustBe 0
    }

    "must serialize to JSON correctly with high version number" in {
      val response = ReturnVersionUpdateReturn(newVersion = 999)

      val json = Json.toJson(response)

      (json \ "newVersion").as[Int] mustBe 999
    }

    "must deserialize from JSON correctly with version 1" in {
      val json = Json.obj("newVersion" -> 1)

      val result = json.validate[ReturnVersionUpdateReturn]

      result mustBe a[JsSuccess[_]]
      result.get.newVersion mustBe 1
    }

    "must deserialize from JSON correctly with version 2" in {
      val json = Json.obj("newVersion" -> 2)

      val result = json.validate[ReturnVersionUpdateReturn]

      result mustBe a[JsSuccess[_]]
      result.get.newVersion mustBe 2
    }

    "must deserialize from JSON correctly with version 6" in {
      val json = Json.obj("newVersion" -> 6)

      val result = json.validate[ReturnVersionUpdateReturn]

      result mustBe a[JsSuccess[_]]
      result.get.newVersion mustBe 6
    }

    "must deserialize from JSON correctly with version 11" in {
      val json = Json.obj("newVersion" -> 11)

      val result = json.validate[ReturnVersionUpdateReturn]

      result mustBe a[JsSuccess[_]]
      result.get.newVersion mustBe 11
    }

    "must deserialize from JSON correctly with version 0" in {
      val json = Json.obj("newVersion" -> 0)

      val result = json.validate[ReturnVersionUpdateReturn]

      result mustBe a[JsSuccess[_]]
      result.get.newVersion mustBe 0
    }

    "must deserialize from JSON correctly with high version number" in {
      val json = Json.obj("newVersion" -> 999)

      val result = json.validate[ReturnVersionUpdateReturn]

      result mustBe a[JsSuccess[_]]
      result.get.newVersion mustBe 999
    }

    "must fail to deserialize when newVersion is missing" in {
      val json = Json.obj()

      val result = json.validate[ReturnVersionUpdateReturn]

      result.isError mustBe true
    }

    "must fail to deserialize when newVersion is not an integer" in {
      val json = Json.obj("newVersion" -> "not-a-number")

      val result = json.validate[ReturnVersionUpdateReturn]

      result.isError mustBe true
    }

    "must fail to deserialize when newVersion is a decimal" in {
      val json = Json.obj("newVersion" -> 1.5)

      val result = json.validate[ReturnVersionUpdateReturn]

      result.isError mustBe true
    }

    "must deserialize negative version numbers" in {
      val json = Json.obj("newVersion" -> -1)

      val result = json.validate[ReturnVersionUpdateReturn]

      result mustBe a[JsSuccess[_]]
      result.get.newVersion mustBe -1
    }
  }

  "ReturnVersionUpdateRequest and ReturnVersionUpdateReturn" - {

    "must round-trip serialize and deserialize correctly" in {
      val request = ReturnVersionUpdateRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        currentVersion = "1"
      )

      val requestJson   = Json.toJson(request)
      val requestResult = requestJson.validate[ReturnVersionUpdateRequest]

      requestResult mustBe a[JsSuccess[_]]
      requestResult.get mustBe request

      val response = ReturnVersionUpdateReturn(newVersion = 2)

      val responseJson   = Json.toJson(response)
      val responseResult = responseJson.validate[ReturnVersionUpdateReturn]

      responseResult mustBe a[JsSuccess[_]]
      responseResult.get mustBe response
    }

    "must handle typical version increment scenario" in {
      val request = ReturnVersionUpdateRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        currentVersion = "5"
      )

      val requestJson = Json.toJson(request)
      (requestJson \ "currentVersion").as[String] mustBe "5"

      val response = ReturnVersionUpdateReturn(newVersion = 6)

      val responseJson = Json.toJson(response)
      (responseJson \ "newVersion").as[Int] mustBe 6
    }

    "must handle version 0 to version 1 scenario" in {
      val request = ReturnVersionUpdateRequest(
        storn = "STORN11111",
        returnResourceRef = "100003",
        currentVersion = "0"
      )

      val requestJson = Json.toJson(request)
      (requestJson \ "currentVersion").as[String] mustBe "0"

      val response = ReturnVersionUpdateReturn(newVersion = 1)

      val responseJson = Json.toJson(response)
      (responseJson \ "newVersion").as[Int] mustBe 1
    }
  }
}
