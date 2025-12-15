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

class UpdatePredefinedAgentModelsSpec extends AnyFreeSpec with Matchers {

  "UpdatePredefinedAgentRequest" - {

    "must serialize to JSON correctly" in {
      val request = UpdatePredefinedAgentRequest(
        agentResourceReference = "ARN001",
        storn = "STN001",
        agentName = "Smith & Co Solicitors",
        houseNumber = None,
        addressLine1 = Some("12 High Street"),
        addressLine2 = Some("London"),
        addressLine3 = Some("Greater London"),
        addressLine4 = None,
        postcode = Some("SW1A 1AA"),
        phone = Some("02071234567"),
        email = Some("info@smithco.co.uk"),
        dxAddress = None
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STN001"
      (json \ "agentName").as[String] mustBe "Smith & Co Solicitors"
      (json \ "agentResourceReference").as[String] mustBe "ARN001"
      (json \ "addressLine1").as[String] mustBe "12 High Street"
      (json \ "phone").as[String] mustBe "02071234567"
      (json \ "email").as[String] mustBe "info@smithco.co.uk"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn"                  -> "STN001",
        "agentName"              -> "Smith & Co Solicitors",
        "agentResourceReference" -> "ARN001",
        "addressLine1"           -> "12 High Street",
        "phone"                  -> "02071234567",
        "email"                  -> "info@smithco.co.uk"
      )

      val result = json.validate[UpdatePredefinedAgentRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STN001"
      request.agentResourceReference mustBe "ARN001"
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "agentResourceReference" -> "ARN001"
      )

      val result = json.validate[UpdatePredefinedAgentRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when agentReferenceNumber is missing" in {
      val json = Json.obj(
        "storn" -> "STN001"
      )

      val result = json.validate[UpdatePredefinedAgentRequest]

      result.isError mustBe true
    }
  }

  "UpdatePredefinedAgentResponse" - {

    "must serialize to JSON correctly when Updated is true" in {
      val response = UpdatePredefinedAgentResponse(updated = true)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when updated is false" in {
      val response = UpdatePredefinedAgentResponse(updated = false)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when updated is true" in {
      val json = Json.obj("updated" -> true)

      val result = json.validate[UpdatePredefinedAgentResponse]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe true
    }

    "must deserialize from JSON correctly when updated is false" in {
      val json = Json.obj("updated" -> false)

      val result = json.validate[UpdatePredefinedAgentResponse]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe false
    }

    "must fail to deserialize when updated field is missing" in {
      val json = Json.obj()

      val result = json.validate[UpdatePredefinedAgentResponse]

      result.isError mustBe true
    }
  }
}
