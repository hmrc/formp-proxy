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

package uk.gov.hmrc.formpproxy.sdlt.DONOTDELETE


//
//import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import play.api.http.Status.*
//import play.api.libs.json.{JsValue, Json}
//import uk.gov.hmrc.formpproxy.itutil.{ApplicationWithWiremock, AuthStub}
//
//class ReturnAgentFullWorkflowIntegrationSpec
//  extends AnyWordSpec
//    with Matchers
//    with ScalaFutures
//    with IntegrationPatience
//    with ApplicationWithWiremock {
//
//  private val createReturnEndpoint = "create/return"
//  private val getReturnEndpoint = "retrieve-return"
//  private val createReturnAgentEndpoint = "filing/create/return-agent"
//  private val updateReturnAgentEndpoint = "filing/update/return-agent"
//  private val deleteReturnAgentEndpoint = "filing/delete/return-agent"
//
//  "Return Agent Workflow" should {
//
//    "create return, add agent, update agent, verify update, then delete agent" in {
//      AuthStub.authorised()
//
//      // STEP 1: Create Return
//      println("\n=== STEP 1: Create Return ===")
//      val createReturnPayload = Json.obj(
//        "stornId" -> "TEST123456",
//        "purchaserIsCompany" -> "N",
//        "surNameOrCompanyName" -> "Smith",
//        "houseNumber" -> 100,
//        "addressLine1" -> "High Street",
//        "postcode" -> "AB12 3CD",
//        "transactionType" -> "RESIDENTIAL"
//      )
//
//      val returnRes = postJson(createReturnEndpoint, createReturnPayload)
//      returnRes.status mustBe CREATED
//      val returnResourceRef = (returnRes.json \ "returnResourceRef").as[String]
//      println(s"✓ Created return: $returnResourceRef")
//
//      // STEP 2: Get Return (should have no agents)
//      println("\n=== STEP 2: Get Return (no agents yet) ===")
//      val getReturnPayload = Json.obj(
//        "storn" -> "TEST123456",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes1 = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes1.status mustBe OK
//      val returnAgents1 = (getReturnRes1.json \ "returnAgent").asOpt[Seq[JsValue]]
//      returnAgents1 mustBe None
//      println("✓ Verified no agents exist yet")
//
//      // STEP 3: Create Purchaser Agent
//      println("\n=== STEP 3: Create Purchaser Agent ===")
//      val createAgentPayload = Json.obj(
//        "stornId" -> "TEST123456",
//        "returnResourceRef" -> returnResourceRef,
//        "agentType" -> "PURCHASER",
//        "name" -> "Original Agent Name",
//        "houseNumber" -> "10",
//        "addressLine1" -> "Original Street",
//        "postcode" -> "SW1A 1AA",
//        "phoneNumber" -> "02071234567",
//        "email" -> "original@agent.com",
//        "agentReference" -> "REF123",
//        "isAuthorised" -> "N"
//      )
//
//      val agentRes = postJson(createReturnAgentEndpoint, createAgentPayload)
//      agentRes.status mustBe CREATED
//      val returnAgentID = (agentRes.json \ "returnAgentID").as[String]
//      println(s"✓ Created agent: $returnAgentID")
//
//      // STEP 4: Get Return (verify agent created)
//      println("\n=== STEP 4: Get Return (verify agent exists) ===")
//      val getReturnRes2 = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes2.status mustBe OK
//
//      val returnAgents2 = (getReturnRes2.json \ "returnAgent").asOpt[Seq[JsValue]]
//      returnAgents2 must not be empty
//      returnAgents2.get must have size 1
//
//      val originalAgent = returnAgents2.get.head
//      (originalAgent \ "returnAgentID").asOpt[String] mustBe Some(returnAgentID)
//      (originalAgent \ "name").asOpt[String] mustBe Some("Original Agent Name")
//      (originalAgent \ "address1").asOpt[String] mustBe Some("Original Street")
//      (originalAgent \ "phone").asOpt[String] mustBe Some("02071234567")
//      (originalAgent \ "email").asOpt[String] mustBe Some("original@agent.com")
//      (originalAgent \ "reference").asOpt[String] mustBe Some("REF123")
//      (originalAgent \ "isAuthorised").asOpt[String] mustBe Some("N")
//      println("✓ Verified agent details match original values")
//
//      // STEP 5: Update Agent
//      println("\n=== STEP 5: Update Purchaser Agent ===")
//      val updateAgentPayload = Json.obj(
//        "stornId" -> "TEST123456",
//        "returnResourceRef" -> returnResourceRef,
//        "agentType" -> "PURCHASER",
//        "name" -> "Updated Agent Name",
//        "houseNumber" -> "42",
//        "addressLine1" -> "Updated Street",
//        "addressLine2" -> "Updated Town",
//        "postcode" -> "W1A 1AA",
//        "phoneNumber" -> "02079876543",
//        "email" -> "updated@agent.com",
//        "agentReference" -> "UPDREF456",
//        "isAuthorised" -> "Y"
//      )
//
//      val updateRes = postJson(updateReturnAgentEndpoint, updateAgentPayload)
//      updateRes.status mustBe OK
//      println("✓ Agent updated successfully")
//
//      // STEP 6: Get Return (verify update)
//      println("\n=== STEP 6: Get Return (verify agent updated) ===")
//      val getReturnRes3 = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes3.status mustBe OK
//
//      val returnAgents3 = (getReturnRes3.json \ "returnAgent").asOpt[Seq[JsValue]]
//      returnAgents3 must not be empty
//      returnAgents3.get must have size 1
//
//      val updatedAgent = returnAgents3.get.head
//      (updatedAgent \ "returnAgentID").asOpt[String] mustBe Some(returnAgentID)
//      (updatedAgent \ "name").asOpt[String] mustBe Some("Updated Agent Name")
//      (updatedAgent \ "houseNumber").asOpt[String] mustBe Some("42")
//      (updatedAgent \ "address1").asOpt[String] mustBe Some("Updated Street")
//      (updatedAgent \ "address2").asOpt[String] mustBe Some("Updated Town")
//      (updatedAgent \ "postcode").asOpt[String] mustBe Some("W1A 1AA")
//      (updatedAgent \ "phone").asOpt[String] mustBe Some("02079876543")
//      (updatedAgent \ "email").asOpt[String] mustBe Some("updated@agent.com")
//      (updatedAgent \ "reference").asOpt[String] mustBe Some("UPDREF456")
//      (updatedAgent \ "isAuthorised").asOpt[String] mustBe Some("Y")
//      println("✓ Verified all agent fields updated correctly")
//
//      // STEP 7: Delete Agent
//      println("\n=== STEP 7: Delete Purchaser Agent ===")
//      val deleteAgentPayload = Json.obj(
//        "storn" -> "TEST123456",
//        "returnResourceRef" -> returnResourceRef,
//        "agentType" -> "PURCHASER"
//      )
//
//      println(s"Delete payload: ${Json.prettyPrint(deleteAgentPayload)}")
//
//      val deleteRes = postJson(deleteReturnAgentEndpoint, deleteAgentPayload)
//      println(s"Delete response status: ${deleteRes.status}")
//      println(s"Delete response body: ${Json.prettyPrint(deleteRes.json)}")
//
//      deleteRes.status mustBe OK
//      println("✓ Agent deleted successfully")
//
//
//      // STEP 8: Get Return (verify deletion)
//      println("\n=== STEP 8: Get Return (verify agent deleted) ===")
//      val getReturnRes4 = postJson(getReturnEndpoint, getReturnPayload)
//      println(s"Get return response status: ${getReturnRes4.status}")
//      println(s"Get return response:\n${Json.prettyPrint(getReturnRes4.json)}")
//
//      getReturnRes4.status mustBe OK
//
//      val returnAgents4 = (getReturnRes4.json \ "returnAgent").asOpt[Seq[JsValue]]
//      println(s"Number of agents after deletion: ${returnAgents4.map(_.size).getOrElse(0)}")
//
//      returnAgents4 mustBe None
//      println("✓ Verified agent has been deleted from return\n")
//
//      println("=== WORKFLOW COMPLETE ===")
//
//      println("=== WORKFLOW COMPLETE ===")
//    }
//  }
//}