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
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import uk.gov.hmrc.formpproxy.sdlt.models.agents.*

class AgentModelsSpec extends AnyFreeSpec with Matchers {

  "CreateReturnAgentRequest" - {

    "must serialize to JSON correctly with all fields populated" in {
      val request = CreateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR",
        name = "Smith & Partners LLP",
        houseNumber = Some("10"),
        addressLine1 = "Legal District",
        addressLine2 = Some("Business Quarter"),
        addressLine3 = Some("Manchester"),
        addressLine4 = Some("Greater Manchester"),
        postcode = "M1 1AA",
        phoneNumber = Some("0161234567"),
        email = Some("agent@smithpartners.com"),
        agentReference = Some("AGT123456"),
        isAuthorised = Some("Y")
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "agentType").as[String] mustBe "SOLICITOR"
      (json \ "name").as[String] mustBe "Smith & Partners LLP"
      (json \ "houseNumber").as[String] mustBe "10"
      (json \ "addressLine1").as[String] mustBe "Legal District"
      (json \ "addressLine2").as[String] mustBe "Business Quarter"
      (json \ "addressLine3").as[String] mustBe "Manchester"
      (json \ "addressLine4").as[String] mustBe "Greater Manchester"
      (json \ "postcode").as[String] mustBe "M1 1AA"
      (json \ "phoneNumber").as[String] mustBe "0161234567"
      (json \ "email").as[String] mustBe "agent@smithpartners.com"
      (json \ "agentReference").as[String] mustBe "AGT123456"
      (json \ "isAuthorised").as[String] mustBe "Y"
    }

    "must serialize to JSON correctly with only required fields" in {
      val request = CreateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "ACCOUNTANT",
        name = "Quick Accounting",
        houseNumber = None,
        addressLine1 = "High Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = "EC1A 1BB",
        phoneNumber = None,
        email = None,
        agentReference = None,
        isAuthorised = None
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "agentType").as[String] mustBe "ACCOUNTANT"
      (json \ "name").as[String] mustBe "Quick Accounting"
      (json \ "addressLine1").as[String] mustBe "High Street"
      (json \ "postcode").as[String] mustBe "EC1A 1BB"
      (json \ "houseNumber").toOption mustBe None
      (json \ "addressLine2").toOption mustBe None
      (json \ "addressLine3").toOption mustBe None
      (json \ "addressLine4").toOption mustBe None
      (json \ "phoneNumber").toOption mustBe None
      (json \ "email").toOption mustBe None
      (json \ "agentReference").toOption mustBe None
      (json \ "isAuthorised").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "name"              -> "Smith & Partners LLP",
        "houseNumber"       -> "10",
        "addressLine1"      -> "Legal District",
        "addressLine2"      -> "Business Quarter",
        "addressLine3"      -> "Manchester",
        "addressLine4"      -> "Greater Manchester",
        "postcode"          -> "M1 1AA",
        "phoneNumber"       -> "0161234567",
        "email"             -> "agent@smithpartners.com",
        "agentReference"    -> "AGT123456",
        "isAuthorised"      -> "Y"
      )

      val result = json.validate[CreateReturnAgentRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.agentType mustBe "SOLICITOR"
      request.name mustBe "Smith & Partners LLP"
      request.houseNumber mustBe Some("10")
      request.addressLine1 mustBe "Legal District"
      request.addressLine2 mustBe Some("Business Quarter")
      request.addressLine3 mustBe Some("Manchester")
      request.addressLine4 mustBe Some("Greater Manchester")
      request.postcode mustBe "M1 1AA"
      request.phoneNumber mustBe Some("0161234567")
      request.email mustBe Some("agent@smithpartners.com")
      request.agentReference mustBe Some("AGT123456")
      request.isAuthorised mustBe Some("Y")
    }

    "must deserialize from JSON correctly with only required fields" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "ACCOUNTANT",
        "name"              -> "Quick Accounting",
        "addressLine1"      -> "High Street",
        "postcode"          -> "EC1A 1BB"
      )

      val result = json.validate[CreateReturnAgentRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.agentType mustBe "ACCOUNTANT"
      request.name mustBe "Quick Accounting"
      request.addressLine1 mustBe "High Street"
      request.postcode mustBe "EC1A 1BB"
      request.houseNumber mustBe None
      request.addressLine2 mustBe None
      request.addressLine3 mustBe None
      request.addressLine4 mustBe None
      request.phoneNumber mustBe None
      request.email mustBe None
      request.agentReference mustBe None
      request.isAuthorised mustBe None
    }

    "must fail to deserialize when required field stornId is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "name"              -> "Smith & Partners LLP",
        "addressLine1"      -> "Legal District",
        "postcode"          -> "M1 1AA"
      )

      val result = json.validate[CreateReturnAgentRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field returnResourceRef is missing" in {
      val json = Json.obj(
        "stornId"      -> "STORN12345",
        "agentType"    -> "SOLICITOR",
        "name"         -> "Smith & Partners LLP",
        "addressLine1" -> "Legal District",
        "postcode"     -> "M1 1AA"
      )

      val result = json.validate[CreateReturnAgentRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field agentType is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "name"              -> "Smith & Partners LLP",
        "addressLine1"      -> "Legal District",
        "postcode"          -> "M1 1AA"
      )

      val result = json.validate[CreateReturnAgentRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field name is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "addressLine1"      -> "Legal District",
        "postcode"          -> "M1 1AA"
      )

      val result = json.validate[CreateReturnAgentRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field addressLine1 is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "name"              -> "Smith & Partners LLP",
        "postcode"          -> "M1 1AA"
      )

      val result = json.validate[CreateReturnAgentRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field postcode is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "name"              -> "Smith & Partners LLP",
        "addressLine1"      -> "Legal District"
      )

      val result = json.validate[CreateReturnAgentRequest]

      result.isError mustBe true
    }
  }

  "CreateReturnAgentReturn" - {

    "must serialize to JSON correctly" in {
      val response = CreateReturnAgentReturn(returnAgentID = "RA100001")

      val json = Json.toJson(response)

      (json \ "returnAgentID").as[String] mustBe "RA100001"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj("returnAgentID" -> "RA100001")

      val result = json.validate[CreateReturnAgentReturn]

      result mustBe a[JsSuccess[_]]
      result.get.returnAgentID mustBe "RA100001"
    }

    "must fail to deserialize when returnAgentID is missing" in {
      val json = Json.obj()

      val result = json.validate[CreateReturnAgentReturn]

      result.isError mustBe true
    }
  }

  "UpdateReturnAgentRequest" - {

    "must serialize to JSON correctly with all fields populated" in {
      val request = UpdateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR",
        name = "Updated Smith & Partners LLP",
        houseNumber = Some("20"),
        addressLine1 = "New Legal District",
        addressLine2 = Some("Updated Quarter"),
        addressLine3 = Some("Manchester"),
        addressLine4 = Some("Greater Manchester"),
        postcode = "M2 2BB",
        phoneNumber = Some("0161999888"),
        email = Some("updated@smithpartners.com"),
        agentReference = Some("AGT999999"),
        isAuthorised = Some("Y")
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "agentType").as[String] mustBe "SOLICITOR"
      (json \ "name").as[String] mustBe "Updated Smith & Partners LLP"
      (json \ "houseNumber").as[String] mustBe "20"
      (json \ "addressLine1").as[String] mustBe "New Legal District"
      (json \ "addressLine2").as[String] mustBe "Updated Quarter"
      (json \ "addressLine3").as[String] mustBe "Manchester"
      (json \ "addressLine4").as[String] mustBe "Greater Manchester"
      (json \ "postcode").as[String] mustBe "M2 2BB"
      (json \ "phoneNumber").as[String] mustBe "0161999888"
      (json \ "email").as[String] mustBe "updated@smithpartners.com"
      (json \ "agentReference").as[String] mustBe "AGT999999"
      (json \ "isAuthorised").as[String] mustBe "Y"
    }

    "must serialize to JSON correctly with only required fields" in {
      val request = UpdateReturnAgentRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        agentType = "ACCOUNTANT",
        name = "Updated Accounting",
        houseNumber = None,
        addressLine1 = "New High Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = "EC2A 2BB",
        phoneNumber = None,
        email = None,
        agentReference = None,
        isAuthorised = None
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "agentType").as[String] mustBe "ACCOUNTANT"
      (json \ "name").as[String] mustBe "Updated Accounting"
      (json \ "addressLine1").as[String] mustBe "New High Street"
      (json \ "postcode").as[String] mustBe "EC2A 2BB"
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "name"              -> "Updated Smith & Partners LLP",
        "houseNumber"       -> "20",
        "addressLine1"      -> "New Legal District",
        "addressLine2"      -> "Updated Quarter",
        "addressLine3"      -> "Manchester",
        "addressLine4"      -> "Greater Manchester",
        "postcode"          -> "M2 2BB",
        "phoneNumber"       -> "0161999888",
        "email"             -> "updated@smithpartners.com",
        "agentReference"    -> "AGT999999",
        "isAuthorised"      -> "Y"
      )

      val result = json.validate[UpdateReturnAgentRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.agentType mustBe "SOLICITOR"
      request.name mustBe "Updated Smith & Partners LLP"
      request.houseNumber mustBe Some("20")
      request.addressLine1 mustBe "New Legal District"
      request.addressLine2 mustBe Some("Updated Quarter")
      request.postcode mustBe "M2 2BB"
      request.phoneNumber mustBe Some("0161999888")
      request.email mustBe Some("updated@smithpartners.com")
      request.agentReference mustBe Some("AGT999999")
      request.isAuthorised mustBe Some("Y")
    }

    "must deserialize from JSON correctly with only required fields" in {
      val json = Json.obj(
        "stornId"           -> "STORN99999",
        "returnResourceRef" -> "100002",
        "agentType"         -> "ACCOUNTANT",
        "name"              -> "Updated Accounting",
        "addressLine1"      -> "New High Street",
        "postcode"          -> "EC2A 2BB"
      )

      val result = json.validate[UpdateReturnAgentRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN99999"
      request.returnResourceRef mustBe "100002"
      request.agentType mustBe "ACCOUNTANT"
      request.name mustBe "Updated Accounting"
      request.addressLine1 mustBe "New High Street"
      request.postcode mustBe "EC2A 2BB"
      request.houseNumber mustBe None
      request.addressLine2 mustBe None
    }

    "must fail to deserialize when required fields are missing" in {
      val json = Json.obj(
        "stornId" -> "STORN12345",
        "name"    -> "Smith & Partners LLP"
      )

      val result = json.validate[UpdateReturnAgentRequest]

      result.isError mustBe true
    }
  }

  "UpdateReturnAgentReturn" - {

    "must serialize to JSON correctly when updated is true" in {
      val response = UpdateReturnAgentReturn(updated = true)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when updated is false" in {
      val response = UpdateReturnAgentReturn(updated = false)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when updated is true" in {
      val json = Json.obj("updated" -> true)

      val result = json.validate[UpdateReturnAgentReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe true
    }

    "must deserialize from JSON correctly when updated is false" in {
      val json = Json.obj("updated" -> false)

      val result = json.validate[UpdateReturnAgentReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe false
    }

    "must fail to deserialize when updated field is missing" in {
      val json = Json.obj()

      val result = json.validate[UpdateReturnAgentReturn]

      result.isError mustBe true
    }
  }

  "DeleteReturnAgentRequest" - {

    "must serialize to JSON correctly" in {
      val request = DeleteReturnAgentRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "agentType").as[String] mustBe "SOLICITOR"
    }

    "must serialize to JSON correctly with different agent type" in {
      val request = DeleteReturnAgentRequest(
        storn = "STORN99999",
        returnResourceRef = "100002",
        agentType = "ACCOUNTANT"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "agentType").as[String] mustBe "ACCOUNTANT"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR"
      )

      val result = json.validate[DeleteReturnAgentRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.agentType mustBe "SOLICITOR"
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR"
      )

      val result = json.validate[DeleteReturnAgentRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj(
        "storn"     -> "STORN12345",
        "agentType" -> "SOLICITOR"
      )

      val result = json.validate[DeleteReturnAgentRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when agentType is missing" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[DeleteReturnAgentRequest]

      result.isError mustBe true
    }
  }

  "DeleteReturnAgentReturn" - {

    "must serialize to JSON correctly when deleted is true" in {
      val response = DeleteReturnAgentReturn(deleted = true)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when deleted is false" in {
      val response = DeleteReturnAgentReturn(deleted = false)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when deleted is true" in {
      val json = Json.obj("deleted" -> true)

      val result = json.validate[DeleteReturnAgentReturn]

      result mustBe a[JsSuccess[_]]
      result.get.deleted mustBe true
    }

    "must deserialize from JSON correctly when deleted is false" in {
      val json = Json.obj("deleted" -> false)

      val result = json.validate[DeleteReturnAgentReturn]

      result mustBe a[JsSuccess[_]]
      result.get.deleted mustBe false
    }

    "must fail to deserialize when deleted field is missing" in {
      val json = Json.obj()

      val result = json.validate[DeleteReturnAgentReturn]

      result.isError mustBe true
    }
  }
}
