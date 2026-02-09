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
//import play.api.libs.json.{JsValue, Json, Writes}
//import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import play.api.http.ContentTypes.JSON
//import play.api.http.Status.*
//import play.api.libs.json.{JsValue, Json}
//import play.api.mvc.Result
//import play.api.test.{FakeRequest, Helpers}
//import play.api.test.Helpers.{ACCEPT, AUTHORIZATION, CONTENT_TYPE, POST, contentAsJson, status}
//import uk.gov.hmrc.formpproxy.itutil.{ApplicationWithWiremock, AuthStub}
//
//import scala.concurrent.Future
//
//class LandFullWorkflowIntegrationSpec
//  extends AnyWordSpec
//    with Matchers
//    with ScalaFutures
//    with IntegrationPatience
//    with ApplicationWithWiremock {
//  
//  private val createReturnEndpoint = "create/return"
//  private val getReturnEndpoint = "retrieve-return"
//  private val createLandEndpoint = "filing/create/land"
//  private val updateLandEndpoint = "filing/update/land"
//  private val deleteLandEndpoint = "filing/delete/land"
//  
//  "Complete Land Workflow" should {
//
//    "create return, add land, update land, get return, delete land, and verify deletion" in {
//      AuthStub.authorised()
//
//      println("\n\n=== STEP 1: Create Return ===")
//      val createReturnPayload = Json.obj(
//        "stornId" -> "LAND123456",
//        "purchaserIsCompany" -> "N",
//        "surNameOrCompanyName" -> "PropertyBuyer",
//        "houseNumber" -> 50,
//        "addressLine1" -> "Estate Road",
//        "addressLine2" -> "Residential Area",
//        "postcode" -> "LD1 2AB",
//        "transactionType" -> "RESIDENTIAL"
//      )
//
//      val returnRes = postJson(createReturnEndpoint, createReturnPayload)
//      returnRes.status mustBe CREATED
//      val returnResourceRef = (returnRes.json \ "returnResourceRef").as[String]
//      println(s"Created return with ID: $returnResourceRef")
//      println(returnRes.json)
//
//      println("\n\n=== STEP 2: Create Land ===")
//      val createLandPayload = Json.obj(
//        "stornId" -> "LAND123456",
//        "returnResourceRef" -> returnResourceRef,
//        "propertyType" -> "RESIDENTIAL",
//        "interestTransferredCreated" -> "FREEHOLD",
//        "houseNumber" -> "123",
//        "addressLine1" -> "Original Land Street",
//        "addressLine2" -> "Original Town",
//        "addressLine3" -> "Original County",
//        "postcode" -> "SW1A 1AA",
//        "landArea" -> "500",
//        "areaUnit" -> "SQUARE_METERS",
//        "localAuthorityNumber" -> "LA12345",
//        "mineralRights" -> "YES",
//        "nlpgUprn" -> "100012345678",
//        "willSendPlansByPost" -> "NO",
//        "titleNumber" -> "TN123456"
//      )
//
//      val landRes = postJson(createLandEndpoint, createLandPayload)
//      landRes.status mustBe CREATED
//      val landResourceRef = (landRes.json \ "landResourceRef").as[String]
//      val landId = (landRes.json \ "landId").as[String]
//      println(s"Created land with landResourceRef: $landResourceRef, landId: $landId")
//      println(landRes.json)
//
//      println("\n\n=== STEP 3: Update Land ===")
//      val updateLandPayload = Json.obj(
//        "stornId" -> "LAND123456",
//        "returnResourceRef" -> returnResourceRef,
//        "landResourceRef" -> landResourceRef,
//        "propertyType" -> "RESIDENTIAL",
//        "interestTransferredCreated" -> "FREEHOLD",
//        "houseNumber" -> "456",
//        "addressLine1" -> "Updated Land Street",
//        "addressLine2" -> "Updated Town",
//        "addressLine3" -> "Updated County",
//        "addressLine4" -> "Updated Region",
//        "postcode" -> "W1A 1AA",
//        "landArea" -> "750",
//        "areaUnit" -> "SQUARE_METERS",
//        "localAuthorityNumber" -> "LA54321",
//        "mineralRights" -> "NO",
//        "nlpgUprn" -> "100087654321",
//        "willSendPlansByPost" -> "YES",
//        "titleNumber" -> "TN654321"
//      )
//
//      val updateRes = postJson(updateLandEndpoint, updateLandPayload)
//      updateRes.status mustBe OK
//      println("Land updated successfully")
//      println(updateRes.json)
//
//      println("\n\n=== STEP 4: Get Return (with land) ===")
//      val getReturnPayload1 = Json.obj(
//        "storn" -> "LAND123456",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes1 = postJson(getReturnEndpoint, getReturnPayload1)
//      getReturnRes1.status mustBe OK
//      println("Retrieved return with land:")
//      println(Json.prettyPrint(getReturnRes1.json))
//
//      val lands1 = (getReturnRes1.json \ "land").asOpt[Seq[JsValue]]
//      lands1 must not be empty
//      val ourLand = lands1.get.find(l =>
//        (l \ "landResourceRef").asOpt[String].contains(landResourceRef)
//      )
//      ourLand must not be empty
//
//      val land = ourLand.get
//      // FIXED: Database returns address1, address2, etc. (not addressLine1, addressLine2)
//      (land \ "landResourceRef").asOpt[String] mustBe Some(landResourceRef)
//      (land \ "houseNumber").asOpt[String] mustBe Some("456")
//      (land \ "address1").asOpt[String] mustBe Some("Updated Land Street") // FIXED: address1
//      (land \ "address2").asOpt[String] mustBe Some("Updated Town") // FIXED: address2
//      (land \ "postcode").asOpt[String] mustBe Some("W1A 1AA")
//      (land \ "landArea").asOpt[String] mustBe Some("750")
//      (land \ "areaUnit").asOpt[String] mustBe Some("SQUARE_METERS")
//      (land \ "mineralRights").asOpt[String] mustBe Some("NO")
//      (land \ "titleNumber").asOpt[String] mustBe Some("TN654321")
//      println("✓ Verified land data matches updated values")
//
//      println("\n\n=== STEP 5: Delete Land ===")
//      val deleteLandPayload = Json.obj(
//        "storn" -> "LAND123456",
//        "returnResourceRef" -> returnResourceRef,
//        "landResourceRef" -> landResourceRef
//      )
//
//      val deleteRes = postJson(deleteLandEndpoint, deleteLandPayload)
//      println(s"Delete response status: ${deleteRes.status}")
//      println(s"Delete response body: ${deleteRes.json}")
//
//      if (deleteRes.status == BAD_REQUEST) {
//        println(s"Delete failed with: ${deleteRes.json}")
//      }
//
//      println(s"Attempted to delete land with landResourceRef: $landResourceRef")
//
//      println("\n\n=== STEP 6: Get Return (verify land status) ===")
//      val getReturnPayload2 = Json.obj(
//        "storn" -> "LAND123456",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes2 = postJson(getReturnEndpoint, getReturnPayload2)
//      getReturnRes2.status mustBe OK
//      println("Retrieved return after deletion attempt:")
//      println(Json.prettyPrint(getReturnRes2.json))
//
//      // Check land status
//      val lands2 = (getReturnRes2.json \ "land").asOpt[Seq[JsValue]]
//      if (lands2.isEmpty) {
//        println("✓ Verified land has been deleted from return")
//      } else {
//        val deletedLandExists = lands2.get.exists(l =>
//          (l \ "landResourceRef").asOpt[String].contains(landResourceRef)
//        )
//        if (!deletedLandExists) {
//          println("✓ Verified deleted land is no longer in return")
//        } else {
//          println(s"⚠ Land still exists in return")
//        }
//      }
//
//      println("\n\n=== WORKFLOW COMPLETE ===")
//    }
//    "create return with multiple lands and delete only one" in {
//      AuthStub.authorised()
//
//      println("\n\n=== Multiple Lands Test ===")
//
//      // Step 1: Create return
//      val createReturnPayload = Json.obj(
//        "stornId" -> "LAND123457",
//        "purchaserIsCompany" -> "N",
//        "surNameOrCompanyName" -> "MultiLandBuyer",
//        "addressLine1" -> "Multi Property Street",
//        "transactionType" -> "RESIDENTIAL"
//      )
//
//      val returnRes = postJson(createReturnEndpoint, createReturnPayload)
//      returnRes.status mustBe CREATED
//      val returnResourceRef = (returnRes.json \ "returnResourceRef").as[String]
//      println(s"Created return: $returnResourceRef")
//
//      // Step 2: Create first land (residential)
//      val createLand1Payload = Json.obj(
//        "stornId" -> "LAND123457",
//        "returnResourceRef" -> returnResourceRef,
//        "propertyType" -> "RESIDENTIAL",
//        "interestTransferredCreated" -> "FREEHOLD",
//        "houseNumber" -> "10",
//        "addressLine1" -> "First Land Street",
//        "addressLine2" -> "Residential Area",
//        "postcode" -> "RES1 1AA",
//        "landArea" -> "300",
//        "areaUnit" -> "SQUARE_METERS"
//      )
//
//      val land1Res = postJson(createLandEndpoint, createLand1Payload)
//      land1Res.status mustBe CREATED
//      val land1ResourceRef = (land1Res.json \ "landResourceRef").as[String]
//      println(s"Created land 1: $land1ResourceRef")
//
//      // Step 3: Create second land (non-residential)
//      val createLand2Payload = Json.obj(
//        "stornId" -> "LAND123457",
//        "returnResourceRef" -> returnResourceRef,
//        "propertyType" -> "NON_RESIDENTIAL",
//        "interestTransferredCreated" -> "LEASEHOLD",
//        "addressLine1" -> "Second Land Street",
//        "addressLine2" -> "Commercial Area",
//        "postcode" -> "COM2 2BB",
//        "landArea" -> "1000",
//        "areaUnit" -> "SQUARE_FEET"
//      )
//
//      val land2Res = postJson(createLandEndpoint, createLand2Payload)
//      land2Res.status mustBe CREATED
//      val land2ResourceRef = (land2Res.json \ "landResourceRef").as[String]
//      println(s"Created land 2: $land2ResourceRef")
//
//      // Step 4: Get return - verify both lands exist
//      val getReturn1Payload = Json.obj(
//        "storn" -> "LAND123457",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturn1Res = postJson(getReturnEndpoint, getReturn1Payload)
//      getReturn1Res.status mustBe OK
//      val lands1 = (getReturn1Res.json \ "land").asOpt[Seq[JsValue]]
//      lands1 must not be empty
//
//      // Verify both our lands exist
//      val hasLand1Before = lands1.get.exists(l =>
//        (l \ "landResourceRef").asOpt[String].contains(land1ResourceRef)
//      )
//      val hasLand2Before = lands1.get.exists(l =>
//        (l \ "landResourceRef").asOpt[String].contains(land2ResourceRef)
//      )
//      hasLand1Before mustBe true
//      hasLand2Before mustBe true
//      val originalCount = lands1.get.size
//      println(s"✓ Verified both lands exist (total: $originalCount lands)")
//
//      // Step 5: Delete first land
//      val deleteLand1Payload = Json.obj(
//        "storn" -> "LAND123457",
//        "returnResourceRef" -> returnResourceRef,
//        "landResourceRef" -> land1ResourceRef
//      )
//
//      val deleteRes = postJson(deleteLandEndpoint, deleteLand1Payload)
//      println(s"Delete response: status=${deleteRes.status}, body=${deleteRes.json}")
//
//      if (deleteRes.status != NO_CONTENT && deleteRes.status != OK) {
//        println(s"⚠ Delete returned ${deleteRes.status} instead of 204 or 200")
//      }
//
//      // Step 6: Get return - verify land 1 deleted, land 2 remains
//      val getReturn2Payload = Json.obj(
//        "storn" -> "LAND123457",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturn2Res = postJson(getReturnEndpoint, getReturn2Payload)
//      getReturn2Res.status mustBe OK
//      println("Retrieved return after deleting one land:")
//      println(Json.prettyPrint(getReturn2Res.json))
//
//      val lands2 = (getReturn2Res.json \ "land").asOpt[Seq[JsValue]]
//      lands2 must not be empty
//
//      // Verify count decreased by 1
//      lands2.get.size mustBe (originalCount - 1)
//      println(s"✓ Verified land count decreased from $originalCount to ${lands2.get.size}")
//
//      // Verify the specific land we deleted is gone
//      val hasLand1After = lands2.get.exists(l =>
//        (l \ "landResourceRef").asOpt[String].contains(land1ResourceRef)
//      )
//      hasLand1After mustBe false
//      println("✓ Verified deleted land (First Land) is gone")
//
//      // Verify the other land still exists
//      val hasLand2After = lands2.get.exists(l =>
//        (l \ "landResourceRef").asOpt[String].contains(land2ResourceRef)
//      )
//      hasLand2After mustBe true
//      println("✓ Verified remaining land (Second Land) still exists")
//
//      println("\n\n=== Multiple Lands Test Complete ===")
//    }
//
//    "handle different property types correctly" in {
//      AuthStub.authorised()
//
//      println("\n\n=== Different Property Types Test ===")
//
//      // Create return
//      val createReturnPayload = Json.obj(
//        "stornId" -> "LAND123458",
//        "purchaserIsCompany" -> "N",
//        "surNameOrCompanyName" -> "PropertyTypeBuyer",
//        "addressLine1" -> "Type Street",
//        "transactionType" -> "MIXED"
//      )
//
//      val returnRes = postJson(createReturnEndpoint, createReturnPayload)
//      returnRes.status mustBe CREATED
//      val returnResourceRef = (returnRes.json \ "returnResourceRef").as[String]
//      println(s"Created return: $returnResourceRef")
//
//      // Create residential land - FIXED: use interestTransferredCreated for INPUT
//      val createResidentialPayload = Json.obj(
//        "stornId" -> "LAND123458",
//        "returnResourceRef" -> returnResourceRef,
//        "propertyType" -> "RESIDENTIAL",
//        "interestTransferredCreated" -> "FREEHOLD", // <-- FIXED field name
//        "houseNumber" -> "1",
//        "addressLine1" -> "Residential Property",
//        "postcode" -> "RES1 1AA",
//        "landArea" -> "200",
//        "areaUnit" -> "SQUARE_METERS"
//      )
//
//      val residentialRes = postJson(createLandEndpoint, createResidentialPayload)
//      residentialRes.status mustBe CREATED
//      val residentialRef = (residentialRes.json \ "landResourceRef").as[String]
//      println(s"Created residential land: $residentialRef")
//
//      // Create non-residential land - FIXED: use interestTransferredCreated for INPUT
//      val createNonResidentialPayload = Json.obj(
//        "stornId" -> "LAND123458",
//        "returnResourceRef" -> returnResourceRef,
//        "propertyType" -> "NON_RESIDENTIAL",
//        "interestTransferredCreated" -> "LEASEHOLD", // <-- FIXED field name
//        "addressLine1" -> "Commercial Property",
//        "postcode" -> "COM1 1BB",
//        "landArea" -> "2000",
//        "areaUnit" -> "SQUARE_FEET"
//      )
//
//      val nonResidentialRes = postJson(createLandEndpoint, createNonResidentialPayload)
//      nonResidentialRes.status mustBe CREATED
//      val nonResidentialRef = (nonResidentialRes.json \ "landResourceRef").as[String]
//      println(s"Created non-residential land: $nonResidentialRef")
//
//      // Create mixed land - FIXED: use interestTransferredCreated for INPUT
//      val createMixedPayload = Json.obj(
//        "stornId" -> "LAND123458",
//        "returnResourceRef" -> returnResourceRef,
//        "propertyType" -> "MIXED",
//        "interestTransferredCreated" -> "FREEHOLD", // <-- FIXED field name
//        "houseNumber" -> "99",
//        "addressLine1" -> "Mixed Use Property",
//        "postcode" -> "MIX1 1CC",
//        "landArea" -> "1500",
//        "areaUnit" -> "SQUARE_METERS"
//      )
//
//      val mixedRes = postJson(createLandEndpoint, createMixedPayload)
//      mixedRes.status mustBe CREATED
//      val mixedRef = (mixedRes.json \ "landResourceRef").as[String]
//      println(s"Created mixed land: $mixedRef")
//
//      // Verify all property types are stored correctly
//      val getReturnPayload = Json.obj(
//        "storn" -> "LAND123458",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes.status mustBe OK
//      println("Retrieved return with all property types:")
//      println(Json.prettyPrint(getReturnRes.json))
//
//      val lands = (getReturnRes.json \ "land").asOpt[Seq[JsValue]]
//      lands must not be empty
//
//      // Verify residential land - use interestCreatedTransferred for OUTPUT
//      val residentialLand = lands.get.find(l =>
//        (l \ "landResourceRef").asOpt[String].contains(residentialRef)
//      )
//      residentialLand must not be empty
//      (residentialLand.get \ "propertyType").asOpt[String] mustBe Some("RESIDENTIAL")
//      (residentialLand.get \ "interestCreatedTransferred").asOpt[String] mustBe Some("FREEHOLD") // <-- For OUTPUT
//      println("✓ Verified residential land stored correctly")
//
//      // Verify non-residential land - use interestCreatedTransferred for OUTPUT
//      val nonResidentialLand = lands.get.find(l =>
//        (l \ "landResourceRef").asOpt[String].contains(nonResidentialRef)
//      )
//      nonResidentialLand must not be empty
//      (nonResidentialLand.get \ "propertyType").asOpt[String] mustBe Some("NON_RESIDENTIAL")
//      (nonResidentialLand.get \ "interestCreatedTransferred").asOpt[String] mustBe Some("LEASEHOLD") // <-- For OUTPUT
//      println("✓ Verified non-residential land stored correctly")
//
//      // Verify mixed land
//      val mixedLand = lands.get.find(l =>
//        (l \ "landResourceRef").asOpt[String].contains(mixedRef)
//      )
//      mixedLand must not be empty
//      (mixedLand.get \ "propertyType").asOpt[String] mustBe Some("MIXED")
//      (mixedLand.get \ "houseNumber").asOpt[String] mustBe Some("99")
//      println("✓ Verified mixed land stored correctly")
//
//      println("\n\n=== Different Property Types Test Complete ===")
//    }
//
//    "handle land with minimal and maximal optional fields" in {
//      AuthStub.authorised()
//
//      println("\n\n=== Minimal/Maximal Fields Test ===")
//
//      // Create return
//      val createReturnPayload = Json.obj(
//        "stornId" -> "LAND123459",
//        "purchaserIsCompany" -> "N",
//        "surNameOrCompanyName" -> "FieldsBuyer",
//        "addressLine1" -> "Fields Street",
//        "transactionType" -> "RESIDENTIAL"
//      )
//
//      val returnRes = postJson(createReturnEndpoint, createReturnPayload)
//      returnRes.status mustBe CREATED
//      val returnResourceRef = (returnRes.json \ "returnResourceRef").as[String]
//
//      // Create land with minimal fields
//      val createMinimalPayload = Json.obj(
//        "stornId" -> "LAND123459",
//        "returnResourceRef" -> returnResourceRef,
//        "propertyType" -> "RESIDENTIAL",
//        "interestTransferredCreated" -> "FREEHOLD",
//        "addressLine1" -> "Minimal Land Street"
//      )
//
//      val minimalRes = postJson(createLandEndpoint, createMinimalPayload)
//      minimalRes.status mustBe CREATED
//      val minimalRef = (minimalRes.json \ "landResourceRef").as[String]
//      println(s"Created minimal land: $minimalRef")
//
//      // Create land with all optional fields
//      val createMaximalPayload = Json.obj(
//        "stornId" -> "LAND123459",
//        "returnResourceRef" -> returnResourceRef,
//        "propertyType" -> "NON_RESIDENTIAL",
//        "interestTransferredCreated" -> "LEASEHOLD",
//        "houseNumber" -> "999",
//        "addressLine1" -> "Maximal Land Street",
//        "addressLine2" -> "Line 2",
//        "addressLine3" -> "Line 3",
//        "addressLine4" -> "Line 4",
//        "postcode" -> "MAX9 9ZZ",
//        "landArea" -> "9999",
//        "areaUnit" -> "SQUARE_FEET",
//        "localAuthorityNumber" -> "LA99999",
//        "mineralRights" -> "YES",
//        "nlpgUprn" -> "999999999999",
//        "willSendPlansByPost" -> "YES",
//        "titleNumber" -> "TN999999"
//      )
//
//      val maximalRes = postJson(createLandEndpoint, createMaximalPayload)
//      maximalRes.status mustBe CREATED
//      val maximalRef = (maximalRes.json \ "landResourceRef").as[String]
//      println(s"Created maximal land: $maximalRef")
//
//      // Verify both lands stored correctly
//      val getReturnPayload = Json.obj(
//        "storn" -> "LAND123459",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes.status mustBe OK
//
//      val lands = (getReturnRes.json \ "land").asOpt[Seq[JsValue]]
//      lands must not be empty
//
//      // Verify minimal land - FIXED: use address1 instead of addressLine1
//      val minimalLand = lands.get.find(l =>
//        (l \ "landResourceRef").asOpt[String].contains(minimalRef)
//      )
//      minimalLand must not be empty
//      (minimalLand.get \ "address1").asOpt[String] mustBe Some("Minimal Land Street")  // FIXED: address1
//      (minimalLand.get \ "houseNumber").asOpt[String] mustBe None
//      (minimalLand.get \ "landArea").asOpt[String] mustBe None
//      println("✓ Verified minimal land has only required fields")
//
//      // Verify maximal land - FIXED: use address2, address3, address4
//      val maximalLand = lands.get.find(l =>
//        (l \ "landResourceRef").asOpt[String].contains(maximalRef)
//      )
//      maximalLand must not be empty
//      (maximalLand.get \ "houseNumber").asOpt[String] mustBe Some("999")
//      (maximalLand.get \ "address2").asOpt[String] mustBe Some("Line 2")  // FIXED: address2
//      (maximalLand.get \ "address3").asOpt[String] mustBe Some("Line 3")  // FIXED: address3
//      (maximalLand.get \ "address4").asOpt[String] mustBe Some("Line 4")  // FIXED: address4
//      (maximalLand.get \ "postcode").asOpt[String] mustBe Some("MAX9 9ZZ")
//      (maximalLand.get \ "landArea").asOpt[String] mustBe Some("9999")
//      (maximalLand.get \ "areaUnit").asOpt[String] mustBe Some("SQUARE_FEET")
//      (maximalLand.get \ "titleNumber").asOpt[String] mustBe Some("TN999999")
//      println("✓ Verified maximal land has all optional fields")
//
//      println("\n\n=== Minimal/Maximal Fields Test Complete ===")
//    }
//  }
//}