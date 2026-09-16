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

package uk.gov.hmrc.formpproxy.sdlt.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.formpproxy.sdlt.models.residency.*

class ResidencyModelsSpec extends AnyFreeSpec with Matchers {

  "ResidencyPayload" - {

    "must serialize to JSON correctly" in {
      val payload = ResidencyPayload(
        isNonUkResidents = "yes",
        isCompany = Some("no"),
        isCrownRelief = Some("yes")
      )

      val json = Json.toJson(payload)

      (json \ "isNonUkResidents").as[String] mustBe "yes"
      (json \ "isCompany").as[String] mustBe "no"
      (json \ "isCrownRelief").as[String] mustBe "yes"
    }

    "must serialize to JSON correctly with all NO values" in {
      val payload = ResidencyPayload(
        isNonUkResidents = "no",
        isCompany = Some("no"),
        isCrownRelief = Some("no")
      )

      val json = Json.toJson(payload)

      (json \ "isNonUkResidents").as[String] mustBe "no"
      (json \ "isCompany").as[String] mustBe "no"
      (json \ "isCrownRelief").as[String] mustBe "no"
    }

    "must serialize to JSON correctly with all YES values" in {
      val payload = ResidencyPayload(
        isNonUkResidents = "yes",
        isCompany = Some("yes"),
        isCrownRelief = Some("yes")
      )

      val json = Json.toJson(payload)

      (json \ "isNonUkResidents").as[String] mustBe "yes"
      (json \ "isCompany").as[String] mustBe "yes"
      (json \ "isCrownRelief").as[String] mustBe "yes"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "isNonUkResidents" -> "yes",
        "isCompany"        -> "no",
        "isCrownRelief"    -> "yes"
      )

      val result = json.validate[ResidencyPayload]

      result mustBe a[JsSuccess[_]]
      val payload = result.get

      payload.isNonUkResidents mustBe "yes"
      payload.isCompany mustBe Some("no")
      payload.isCrownRelief mustBe Some("yes")
    }

    "must fail to deserialize when isNonUkResidents is missing" in {
      val json = Json.obj(
        "isCompany"     -> "no",
        "isCrownRelief" -> "no"
      )

      val result = json.validate[ResidencyPayload]

      result.isError mustBe true
    }

    "must deserialize when isCompany is missing" in {
      val json = Json.obj(
        "isNonUkResidents" -> "no",
        "isCrownRelief"    -> "no"
      )

      val result = json.validate[ResidencyPayload]

      result.isError mustBe false
    }

    "must deserialize when isCrownRelief is missing" in {
      val json = Json.obj(
        "isNonUkResidents" -> "no",
        "isCompany"        -> "no"
      )

      val result = json.validate[ResidencyPayload]

      result.isError mustBe false
    }
  }

  "CreateResidencyRequest" - {

    "must serialize to JSON correctly" in {
      val request = CreateResidencyRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        residency = ResidencyPayload(
          isNonUkResidents = "no",
          isCompany = Some("no"),
          isCrownRelief = Some("no")
        )
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "residency" \ "isNonUkResidents").as[String] mustBe "no"
      (json \ "residency" \ "isCompany").as[String] mustBe "no"
      (json \ "residency" \ "isCrownRelief").as[String] mustBe "no"
    }

    "must serialize to JSON correctly with all YES flags" in {
      val request = CreateResidencyRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        residency = ResidencyPayload(
          isNonUkResidents = "yes",
          isCompany = Some("yes"),
          isCrownRelief = Some("yes")
        )
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "residency" \ "isNonUkResidents").as[String] mustBe "yes"
      (json \ "residency" \ "isCompany").as[String] mustBe "yes"
      (json \ "residency" \ "isCrownRelief").as[String] mustBe "yes"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "residency"         -> Json.obj(
          "isNonUkResidents" -> "no",
          "isCompany"        -> "no",
          "isCrownRelief"    -> "no"
        )
      )

      val result = json.validate[CreateResidencyRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.residency.isNonUkResidents mustBe "no"
      request.residency.isCompany mustBe Some("no")
      request.residency.isCrownRelief mustBe Some("no")
    }

    "must fail to deserialize when stornId is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "residency"         -> Json.obj(
          "isNonUkResidents" -> "no",
          "isCompany"        -> "no",
          "isCrownRelief"    -> "no"
        )
      )

      val result = json.validate[CreateResidencyRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj(
        "stornId"   -> "STORN12345",
        "residency" -> Json.obj(
          "isNonUkResidents" -> "no",
          "isCompany"        -> "no",
          "isCrownRelief"    -> "no"
        )
      )

      val result = json.validate[CreateResidencyRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when residency payload is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[CreateResidencyRequest]

      result.isError mustBe true
    }
  }

  "CreateResidencyReturn" - {

    "must serialize to JSON correctly when created is true" in {
      val response = CreateResidencyReturn(created = true)

      val json = Json.toJson(response)

      (json \ "created").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when created is false" in {
      val response = CreateResidencyReturn(created = false)

      val json = Json.toJson(response)

      (json \ "created").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when created is true" in {
      val json = Json.obj("created" -> true)

      val result = json.validate[CreateResidencyReturn]

      result mustBe a[JsSuccess[_]]
      result.get.created mustBe true
    }

    "must deserialize from JSON correctly when created is false" in {
      val json = Json.obj("created" -> false)

      val result = json.validate[CreateResidencyReturn]

      result mustBe a[JsSuccess[_]]
      result.get.created mustBe false
    }

    "must fail to deserialize when created field is missing" in {
      val json = Json.obj()

      val result = json.validate[CreateResidencyReturn]

      result.isError mustBe true
    }
  }

  "UpdateResidencyRequest" - {

    "must serialize to JSON correctly" in {
      val request = UpdateResidencyRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        residency = ResidencyPayload(
          isNonUkResidents = "yes",
          isCompany = Some("yes"),
          isCrownRelief = Some("no")
        )
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "residency" \ "isNonUkResidents").as[String] mustBe "yes"
      (json \ "residency" \ "isCompany").as[String] mustBe "yes"
      (json \ "residency" \ "isCrownRelief").as[String] mustBe "no"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "residency"         -> Json.obj(
          "isNonUkResidents" -> "yes",
          "isCompany"        -> "yes",
          "isCrownRelief"    -> "no"
        )
      )

      val result = json.validate[UpdateResidencyRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.residency.isNonUkResidents mustBe "yes"
      request.residency.isCompany mustBe Some("yes")
      request.residency.isCrownRelief mustBe Some("no")
    }

    "must fail to deserialize when stornId is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "residency"         -> Json.obj(
          "isNonUkResidents" -> "no",
          "isCompany"        -> "no",
          "isCrownRelief"    -> "no"
        )
      )

      val result = json.validate[UpdateResidencyRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj(
        "stornId"   -> "STORN12345",
        "residency" -> Json.obj(
          "isNonUkResidents" -> "no",
          "isCompany"        -> "no",
          "isCrownRelief"    -> "no"
        )
      )

      val result = json.validate[UpdateResidencyRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when residency payload is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[UpdateResidencyRequest]

      result.isError mustBe true
    }
  }

  "UpdateResidencyReturn" - {

    "must serialize to JSON correctly when updated is true" in {
      val response = UpdateResidencyReturn(updated = true)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when updated is false" in {
      val response = UpdateResidencyReturn(updated = false)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when updated is true" in {
      val json = Json.obj("updated" -> true)

      val result = json.validate[UpdateResidencyReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe true
    }

    "must deserialize from JSON correctly when updated is false" in {
      val json = Json.obj("updated" -> false)

      val result = json.validate[UpdateResidencyReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe false
    }

    "must fail to deserialize when updated field is missing" in {
      val json = Json.obj()

      val result = json.validate[UpdateResidencyReturn]

      result.isError mustBe true
    }
  }

  "DeleteResidencyRequest" - {

    "must serialize to JSON correctly" in {
      val request = DeleteResidencyRequest(
        storn = "STORN12345",
        returnResourceRef = "100001"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
    }

    "must serialize to JSON correctly with different values" in {
      val request = DeleteResidencyRequest(
        storn = "STORN99999",
        returnResourceRef = "100002"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[DeleteResidencyRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj("returnResourceRef" -> "100001")

      val result = json.validate[DeleteResidencyRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj("storn" -> "STORN12345")

      val result = json.validate[DeleteResidencyRequest]

      result.isError mustBe true
    }
  }

  "DeleteResidencyReturn" - {

    "must serialize to JSON correctly when deleted is true" in {
      val response = DeleteResidencyReturn(deleted = true)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when deleted is false" in {
      val response = DeleteResidencyReturn(deleted = false)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when deleted is true" in {
      val json = Json.obj("deleted" -> true)

      val result = json.validate[DeleteResidencyReturn]

      result mustBe a[JsSuccess[_]]
      result.get.deleted mustBe true
    }

    "must deserialize from JSON correctly when deleted is false" in {
      val json = Json.obj("deleted" -> false)

      val result = json.validate[DeleteResidencyReturn]

      result mustBe a[JsSuccess[_]]
      result.get.deleted mustBe false
    }

    "must fail to deserialize when deleted field is missing" in {
      val json = Json.obj()

      val result = json.validate[DeleteResidencyReturn]

      result.isError mustBe true
    }
  }
}
