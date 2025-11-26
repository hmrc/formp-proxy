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

package uk.gov.hmrc.formpproxy.sdlt.models.agents

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.formpproxy.sdlt.models.agents.{DeletePredefinedAgentRequest, DeletePredefinedAgentReturn}

class DeletePredefinedAgentModelsSpec extends AnyFreeSpec with Matchers {

  "DeletePredefinedAgentRequest" - {

    "must serialize to JSON correctly" in {
      val request = DeletePredefinedAgentRequest(
        storn = "STN001",
        agentReferenceNumber = "ARN001"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STN001"
      (json \ "agentReferenceNumber").as[String] mustBe "ARN001"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn"                -> "STN001",
        "agentReferenceNumber" -> "ARN001"
      )

      val result = json.validate[DeletePredefinedAgentRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STN001"
      request.agentReferenceNumber mustBe "ARN001"
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "agentReferenceNumber" -> "ARN001"
      )

      val result = json.validate[DeletePredefinedAgentRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when agentReferenceNumber is missing" in {
      val json = Json.obj(
        "storn" -> "STN001"
      )

      val result = json.validate[DeletePredefinedAgentRequest]

      result.isError mustBe true
    }
  }

  "DeletePredefinedReturnAgent" - {

    "must serialize to JSON correctly when deleted is true" in {
      val response = DeletePredefinedAgentReturn(deleted = true)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when deleted is false" in {
      val response = DeletePredefinedAgentReturn(deleted = false)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when deleted is true" in {
      val json = Json.obj("deleted" -> true)

      val result = json.validate[DeletePredefinedAgentReturn]

      result mustBe a[JsSuccess[_]]
      result.get.deleted mustBe true
    }

    "must deserialize from JSON correctly when deleted is false" in {
      val json = Json.obj("deleted" -> false)

      val result = json.validate[DeletePredefinedAgentReturn]

      result mustBe a[JsSuccess[_]]
      result.get.deleted mustBe false
    }

    "must fail to deserialize when deleted field is missing" in {
      val json = Json.obj()

      val result = json.validate[DeletePredefinedAgentReturn]

      result.isError mustBe true
    }
  }
}
