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
import uk.gov.hmrc.formpproxy.sdlt.models.organisation._

class GetSdltOrgRequestSpec extends AnyFreeSpec with Matchers {

  "GetSdltOrgRequest" - {

    "must serialize to JSON correctly with all fields populated and agents present" in {
      val request = GetSdltOrgRequest(
        storn = Some("STORN12345"),
        version = Some("1"),
        isReturnUser = Some("Y"),
        doNotDisplayWelcomePage = Some("N"),
        agents = Seq(
          Agent(
            agentId = Some("AGT001"),
            storn = Some("STORN12345"),
            name = Some("Smith & Co Solicitors"),
            houseNumber = Some("10"),
            address1 = Some("Downing Street"),
            address2 = Some("Westminster"),
            address3 = Some("London"),
            address4 = None,
            postcode = Some("SW1A 2AA"),
            phone = Some("02071234567"),
            email = Some("info@smithco.co.uk"),
            dxAddress = Some("DX 12345 London"),
            agentResourceReference = Some("AGT-RES-001")
          )
        )
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "version").as[String] mustBe "1"
      (json \ "isReturnUser").as[String] mustBe "Y"
      (json \ "doNotDisplayWelcomePage").as[String] mustBe "N"

      val agentsJson = (json \ "agents").as[Seq[play.api.libs.json.JsValue]]
      agentsJson must have size 1

      (agentsJson.head \ "agentId").as[String] mustBe "AGT001"
      (agentsJson.head \ "storn").as[String] mustBe "STORN12345"
      (agentsJson.head \ "name").as[String] mustBe "Smith & Co Solicitors"
      (agentsJson.head \ "houseNumber").as[String] mustBe "10"
      (agentsJson.head \ "address1").as[String] mustBe "Downing Street"
      (agentsJson.head \ "address2").as[String] mustBe "Westminster"
      (agentsJson.head \ "address3").as[String] mustBe "London"
      (agentsJson.head \ "postcode").as[String] mustBe "SW1A 2AA"
      (agentsJson.head \ "phone").as[String] mustBe "02071234567"
      (agentsJson.head \ "email").as[String] mustBe "info@smithco.co.uk"
      (agentsJson.head \ "dxAddress").as[String] mustBe "DX 12345 London"
      (agentsJson.head \ "agentResourceReference").as[String] mustBe "AGT-RES-001"
    }

    "must serialize to JSON correctly with minimal fields and empty agents list" in {
      val request = GetSdltOrgRequest(
        storn = None,
        version = None,
        isReturnUser = None,
        doNotDisplayWelcomePage = None,
        agents = Seq.empty
      )

      val json = Json.toJson(request)

      (json \ "storn").toOption mustBe None
      (json \ "version").toOption mustBe None
      (json \ "isReturnUser").toOption mustBe None
      (json \ "doNotDisplayWelcomePage").toOption mustBe None
      (json \ "agents").as[Seq[play.api.libs.json.JsValue]] mustBe empty
    }

    "must deserialize from JSON correctly with all fields populated and agents present" in {
      val json = Json.obj(
        "storn"                   -> "STORN12345",
        "version"                 -> "1",
        "isReturnUser"            -> "Y",
        "doNotDisplayWelcomePage" -> "N",
        "agents"                  -> Json.arr(
          Json.obj(
            "agentId"                -> "AGT001",
            "storn"                  -> "STORN12345",
            "name"                   -> "Smith & Co Solicitors",
            "houseNumber"            -> "10",
            "address1"               -> "Downing Street",
            "address2"               -> "Westminster",
            "address3"               -> "London",
            "postcode"               -> "SW1A 2AA",
            "phone"                  -> "02071234567",
            "email"                  -> "info@smithco.co.uk",
            "dxAddress"              -> "DX 12345 London",
            "agentResourceReference" -> "AGT-RES-001"
          )
        )
      )

      val result = json.validate[GetSdltOrgRequest]

      result mustBe a[JsSuccess[_]]
      val org = result.get

      org.storn mustBe Some("STORN12345")
      org.version mustBe Some("1")
      org.isReturnUser mustBe Some("Y")
      org.doNotDisplayWelcomePage mustBe Some("N")

      org.agents must have size 1
      val agent = org.agents.head

      agent.agentId mustBe Some("AGT001")
      agent.storn mustBe Some("STORN12345")
      agent.name mustBe Some("Smith & Co Solicitors")
      agent.houseNumber mustBe Some("10")
      agent.address1 mustBe Some("Downing Street")
      agent.address2 mustBe Some("Westminster")
      agent.address3 mustBe Some("London")
      agent.postcode mustBe Some("SW1A 2AA")
      agent.phone mustBe Some("02071234567")
      agent.email mustBe Some("info@smithco.co.uk")
      agent.dxAddress mustBe Some("DX 12345 London")
      agent.agentResourceReference mustBe Some("AGT-RES-001")
    }

    "must deserialize from JSON correctly with only agents present and other fields missing" in {
      val json = Json.obj(
        "agents" -> Json.arr(
          Json.obj(
            "agentId"  -> "AGT001",
            "storn"    -> "STORN00000",
            "name"     -> "Minimal Agent",
            "address1" -> "Some Street",
            "postcode" -> "AB1 2CD"
          )
        )
      )

      val result = json.validate[GetSdltOrgRequest]

      result mustBe a[JsSuccess[_]]
      val org = result.get

      org.storn mustBe None
      org.version mustBe None
      org.isReturnUser mustBe None
      org.doNotDisplayWelcomePage mustBe None

      org.agents must have size 1
      val agent = org.agents.head

      agent.agentId mustBe Some("AGT001")
      agent.storn mustBe Some("STORN00000")
      agent.name mustBe Some("Minimal Agent")
      agent.address1 mustBe Some("Some Street")
      agent.postcode mustBe Some("AB1 2CD")
    }

    "must fail to deserialize when agents field is missing" in {
      val json = Json.obj(
        "storn"        -> "STORN12345",
        "version"      -> "1",
        "isReturnUser" -> "Y"
      )

      val result = json.validate[GetSdltOrgRequest]

      result.isError mustBe true
    }
  }
}
