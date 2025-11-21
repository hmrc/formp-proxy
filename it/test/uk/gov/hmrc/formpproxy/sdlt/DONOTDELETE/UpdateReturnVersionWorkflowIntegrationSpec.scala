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

///*
// * Copyright 2025 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.formpproxy.sdlt.DONOTDELETE
//
//import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import play.api.http.Status.*
//import play.api.libs.json.Json
//import uk.gov.hmrc.formpproxy.itutil.{ApplicationWithWiremock, AuthStub}
//
//class UpdateReturnVersionWorkflowIntegrationSpec
//  extends AnyWordSpec
//    with Matchers
//    with ScalaFutures
//    with IntegrationPatience
//    with ApplicationWithWiremock {
//
//  private val createReturnEndpoint = "create/return"
//  private val getReturnEndpoint = "retrieve-return"
//  private val updateVersionEndpoint = "filing/update/return-version"
//
//  "Update Return Version Workflow" should {
//
//    "create return, check initial version, update version, and verify change" in {
//      AuthStub.authorised()
//
//      // STEP 1: Create Return
//      println("\n=== STEP 1: Create Return ===")
//      val createReturnPayload = Json.obj(
//        "stornId" -> "TEST123459",
//        "purchaserIsCompany" -> "N",
//        "surNameOrCompanyName" -> "Jones",
//        "houseNumber" -> 50,
//        "addressLine1" -> "Main Road",
//        "postcode" -> "EC1A 1BB",
//        "transactionType" -> "RESIDENTIAL"
//      )
//
//      println(s"Creating return with payload: ${Json.prettyPrint(createReturnPayload)}")
//
//      val returnRes = postJson(createReturnEndpoint, createReturnPayload)
//      println(s"Create return response status: ${returnRes.status}")
//      println(s"Create return response body: ${Json.prettyPrint(returnRes.json)}")
//
//      returnRes.status mustBe CREATED
//      val returnResourceRef = (returnRes.json \ "returnResourceRef").as[String]
//      println(s"✓ Created return with ID: $returnResourceRef")
//
//      // STEP 2: Get Return (check initial version)
//      println("\n=== STEP 2: Get Return (check initial version) ===")
//      val getReturnPayload = Json.obj(
//        "storn" -> "TEST123459",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      println(s"Getting return with payload: ${Json.prettyPrint(getReturnPayload)}")
//
//      val getReturnRes1 = postJson(getReturnEndpoint, getReturnPayload)
//      println(s"Get return response status: ${getReturnRes1.status}")
//      println(s"Return info: ${Json.prettyPrint((getReturnRes1.json \ "returnInfo").get)}")
//
//      getReturnRes1.status mustBe OK
//
//      val initialVersion = (getReturnRes1.json \ "returnInfo" \ "version").asOpt[String]
//      println(s"Initial version: ${initialVersion.getOrElse("not set")}")
//
//      initialVersion mustBe Some("0")
//      println("✓ Verified initial version is 0")
//
//      // STEP 3: Update Return Version
//      println("\n=== STEP 3: Update Return Version ===")
//      val updateVersionPayload = Json.obj(
//        "storn" -> "TEST123459",
//        "returnResourceRef" -> returnResourceRef,
//        "currentVersion" -> initialVersion
//      )
//
//      println(s"Updating version with payload: ${Json.prettyPrint(updateVersionPayload)}")
//
//      val updateVersionRes = postJson(updateVersionEndpoint, updateVersionPayload)
//      println(s"Update version response status: ${updateVersionRes.status}")
//      println(s"Update version response body: ${Json.prettyPrint(updateVersionRes.json)}")
//
//      updateVersionRes.status mustBe OK
//      val updateResult = (updateVersionRes.json \ "newVersion").as[Int]
//      updateResult mustBe initialVersion.get.toLong + 1
//      
//      println("✓ Version updated successfully")
//
//      // STEP 4: Get Return (verify version changed)
//      println("\n=== STEP 4: Get Return (verify version changed) ===")
//      val getReturnRes2 = postJson(getReturnEndpoint, getReturnPayload)
//      println(s"Get return response status: ${getReturnRes2.status}")
//      println(s"Return info after update: ${Json.prettyPrint((getReturnRes2.json \ "returnInfo").get)}")
//
//      getReturnRes2.status mustBe OK
//
//      val updatedVersion = (getReturnRes2.json \ "returnInfo" \ "version").asOpt[String]
//      println(s"Updated version: ${updatedVersion.getOrElse("not set")}")
//
//      println("\n=== STEP 5: Get Return (verify second version change) ===")
//      val getReturnRes3 = postJson(getReturnEndpoint, getReturnPayload)
//      println(s"Get return response status: ${getReturnRes3.status}")
//
//      getReturnRes3.status mustBe OK
//
//      val finalVersion = (getReturnRes3.json \ "returnInfo" \ "version").asOpt[String]
//      println(s"Final version: ${finalVersion.getOrElse("not set")}")
//
//      finalVersion mustBe Some("1")
//
//      println("=== VERSION UPDATE WORKFLOW COMPLETE ===")
//    }
//  }
//}