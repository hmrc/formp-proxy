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

///*
// * Copyright 2026 HM Revenue & Customs
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
//import play.api.libs.json.{JsValue, Json}
//import uk.gov.hmrc.formpproxy.itutil.{ApplicationWithWiremock, AuthStub}
//
//class ResidencyFullWorkflowIntegrationSpec
//  extends AnyWordSpec
//    with Matchers
//    with ScalaFutures
//    with IntegrationPatience
//    with ApplicationWithWiremock {
//
//  private val createReturnEndpoint    = "create/return"
//  private val getReturnEndpoint       = "retrieve-return"
//  private val createResidencyEndpoint = "filing/create/residency"
//  private val updateResidencyEndpoint = "filing/update/residency"
//  private val deleteResidencyEndpoint = "filing/delete/residency"
//
//  private def uniqueStorn(): String =
//    System.currentTimeMillis().toString.takeRight(10)
//
//  "Complete Residency Workflow" should {
//
//    "create return, add residency, update residency, get return, delete residency, and verify deletion" in {
//      AuthStub.authorised()
//
//      val storn = uniqueStorn()
//
//      println("\n\n=== STEP 1: Create Return ===")
//      val createReturnPayload = Json.obj(
//        "stornId"              -> storn,
//        "purchaserIsCompany"   -> "N",
//        "surNameOrCompanyName" -> "Smith",
//        "houseNumber"          -> 100,
//        "addressLine1"         -> "High Street",
//        "addressLine2"         -> "Anytown",
//        "postcode"             -> "AB12 3CD",
//        "transactionType"      -> "RESIDENTIAL"
//      )
//
//      val returnRes = postJson(createReturnEndpoint, createReturnPayload)
//      returnRes.status mustBe CREATED
//      val returnResourceRef = (returnRes.json \ "returnResourceRef").as[String]
//      println(s"Created return with ID: $returnResourceRef for storn: $storn")
//
//      println("\n\n=== STEP 2: Delete auto-created residency row ===")
//      val deleteAutoResidencyPayload = Json.obj(
//        "storn"             -> storn,
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val deleteAutoRes = postJson(deleteResidencyEndpoint, deleteAutoResidencyPayload)
//      println(s"Delete auto-residency response status: ${deleteAutoRes.status}")
//      println(s"Delete auto-residency response body: ${deleteAutoRes.json}")
//      deleteAutoRes.status mustBe OK
//      println("Auto-created residency deleted successfully")
//
//      println("\n\n=== STEP 3: Create Residency (all NO) ===")
//      val createResidencyPayload = Json.obj(
//        "stornId"           -> storn,
//        "returnResourceRef" -> returnResourceRef,
//        "residency" -> Json.obj(
//          "isNonUkResidents" -> "NO",
//          "isCompany"        -> "NO",
//          "isCrownRelief"    -> "NO"
//        )
//      )
//
//      val createRes = postJson(createResidencyEndpoint, createResidencyPayload)
//      createRes.status mustBe CREATED
//      println("Residency created successfully")
//      println(createRes.json)
//
//      println("\n\n=== STEP 4: Update Residency (all YES) ===")
//      val updateResidencyPayload = Json.obj(
//        "stornId"           -> storn,
//        "returnResourceRef" -> returnResourceRef,
//        "residency" -> Json.obj(
//          "isNonUkResidents" -> "YES",
//          "isCompany"        -> "YES",
//          "isCrownRelief"    -> "YES"
//        )
//      )
//
//      val updateRes = postJson(updateResidencyEndpoint, updateResidencyPayload)
//      println(s"Update response status: ${updateRes.status}")
//      println(s"Update response body: ${updateRes.json}")
//      updateRes.status mustBe OK
//      println("Residency updated successfully")
//
//      println("\n\n=== STEP 5: Get Return (verify updated values) ===")
//      val getReturnPayload = Json.obj(
//        "storn"             -> storn,
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes1 = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes1.status mustBe OK
//      println("Retrieved return with residency:")
//      println(Json.prettyPrint(getReturnRes1.json))
//
//      val residency1 = (getReturnRes1.json \ "residency").toOption
//      residency1 must not be empty
//
//      val residencyData = residency1.get
//      (residencyData \ "isNonUkResidents").asOpt[String] mustBe Some("YES")
//      (residencyData \ "isCloseCompany").asOpt[String] mustBe Some("YES")
//      (residencyData \ "isCrownRelief").asOpt[String] mustBe Some("YES")
//      println("✓ Verified residency data matches updated values")
//
//      println("\n\n=== STEP 6: Delete Residency ===")
//      val deleteResidencyPayload = Json.obj(
//        "storn"             -> storn,
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val deleteRes = postJson(deleteResidencyEndpoint, deleteResidencyPayload)
//      println(s"Delete response status: ${deleteRes.status}")
//      println(s"Delete response body: ${deleteRes.json}")
//      deleteRes.status mustBe OK
//      println("Residency deleted successfully")
//
//      println("\n\n=== STEP 7: Get Return (verify residency removed) ===")
//      val getReturnRes2 = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes2.status mustBe OK
//      println("Retrieved return after deletion:")
//      println(Json.prettyPrint(getReturnRes2.json))
//
//      val residency2 = (getReturnRes2.json \ "residency").toOption
//      if (residency2.isEmpty) {
//        println("✓ Verified residency has been deleted from return")
//      } else {
//        println(s"⚠ Residency still exists on return")
//      }
//
//      println("\n\n=== WORKFLOW COMPLETE ===")
//    }
//  }
//}