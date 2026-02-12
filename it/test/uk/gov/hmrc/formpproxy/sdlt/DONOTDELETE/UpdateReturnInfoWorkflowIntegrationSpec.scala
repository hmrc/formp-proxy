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
//import play.api.libs.json.Json
//import uk.gov.hmrc.formpproxy.itutil.{ApplicationWithWiremock, AuthStub}
//
//class UpdateReturnInfoWorkflowIntegrationSpec
//  extends AnyWordSpec
//    with Matchers
//    with ScalaFutures
//    with IntegrationPatience
//    with ApplicationWithWiremock {
//
//  private val createReturnEndpoint = "create/return"
//  private val getReturnEndpoint = "retrieve-return"
//  private val createPurchaserEndpoint = "filing/create/purchaser"
//  private val createVendorEndpoint = "filing/create/vendor"
//  private val createLandEndpoint = "filing/create/land"
//  private val updateReturnInfoEndpoint = "filing/update/return-info"
//
//  "Update Return Info Workflow" should {
//
//    "create return, add entities, update return info, and verify changes" in {
//      AuthStub.authorised()
//
//      // STEP 1: Create Return
//      println("\n=== STEP 1: Create Return ===")
//      val createReturnPayload = Json.obj(
//        "stornId" -> "TEST123460",
//        "purchaserIsCompany" -> "NO",
//        "surNameOrCompanyName" -> "UpdateTest",
//        "houseNumber" -> 100,
//        "addressLine1" -> "Update Street",
//        "postcode" -> "UT1 1UP",
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
//      // STEP 2: Create Purchaser
//      println("\n=== STEP 2: Create Purchaser ===")
//      val createPurchaserPayload = Json.obj(
//        "stornId" -> "TEST123460",
//        "returnResourceRef" -> returnResourceRef,
//        "isCompany" -> "NO",
//        "isTrustee" -> "NO",
//        "isConnectedToVendor" -> "NO",
//        "isRepresentedByAgent" -> "NO",
//        "title" -> "Mr",
//        "surname" -> "Smith",
//        "forename1" -> "John",
//        "houseNumber" -> "42",
//        "address1" -> "High Street",
//        "postcode" -> "SW1A 1AA",
//        "hasNino" -> "YES",
//        "dateOfBirth" -> "1980-01-01"
//      )
//
//      println(s"Creating purchaser with payload: ${Json.prettyPrint(createPurchaserPayload)}")
//
//      val purchaserRes = postJson(createPurchaserEndpoint, createPurchaserPayload)
//      println(s"Create purchaser response status: ${purchaserRes.status}")
//      println(s"Create purchaser response body: ${Json.prettyPrint(purchaserRes.json)}")
//
//      purchaserRes.status mustBe CREATED
//      val purchaserId = (purchaserRes.json \ "purchaserId").as[String]
//      println(s"✓ Created purchaser with ID: $purchaserId")
//
//      // STEP 3: Create Vendor
//      println("\n=== STEP 3: Create Vendor ===")
//      val createVendorPayload = Json.obj(
//        "stornId" -> "TEST123460",
//        "returnResourceRef" -> returnResourceRef,
//        "title" -> "Mrs",
//        "forename1" -> "Jane",
//        "name" -> "Vendor",
//        "houseNumber" -> "456",
//        "addressLine1" -> "Vendor Street",
//        "postcode" -> "VE1 2ND",
//        "isRepresentedByAgent" -> "NO"
//      )
//
//      println(s"Creating vendor with payload: ${Json.prettyPrint(createVendorPayload)}")
//
//      val vendorRes = postJson(createVendorEndpoint, createVendorPayload)
//      println(s"Create vendor response status: ${vendorRes.status}")
//      println(s"Create vendor response body: ${Json.prettyPrint(vendorRes.json)}")
//
//      vendorRes.status mustBe CREATED
//      val vendorId = (vendorRes.json \ "vendorId").as[String]
//      println(s"✓ Created vendor with ID: $vendorId")
//
//      // STEP 4: Create Land
//      println("\n=== STEP 4: Create Land ===")
//      val createLandPayload = Json.obj(
//        "stornId" -> "TEST123460",
//        "returnResourceRef" -> returnResourceRef,
//        "propertyType" -> "RESIDENTIAL",
//        "interestTransferredCreated" -> "FREEHOLD",
//        "houseNumber" -> "42",
//        "addressLine1" -> "High Street",
//        "postcode" -> "SW1A 1AA"
//      )
//
//      println(s"Creating land with payload: ${Json.prettyPrint(createLandPayload)}")
//
//      val landRes = postJson(createLandEndpoint, createLandPayload)
//      println(s"Create land response status: ${landRes.status}")
//      println(s"Create land response body: ${Json.prettyPrint(landRes.json)}")
//
//      landRes.status mustBe CREATED
//      val landId = (landRes.json \ "landId").as[String]
//      println(s"✓ Created land with ID: $landId")
//
//      // STEP 5: Get Return (check initial state)
//      println("\n=== STEP 5: Get Return (check initial state) ===")
//      val getReturnPayload = Json.obj(
//        "storn" -> "TEST123460",
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
//      val initialmainPurchaserID = (getReturnRes1.json \ "returnInfo" \ "mainPurchaserID").asOpt[String]
//      val initialmainVendorID = (getReturnRes1.json \ "returnInfo" \ "mainVendorID").asOpt[String]
//      val initialmainLandID = (getReturnRes1.json \ "returnInfo" \ "mainLandID").asOpt[String]
//      val initialIRMark = (getReturnRes1.json \ "returnInfo" \ "IRMarkGenerated").asOpt[String]
//      val initialLandCert = (getReturnRes1.json \ "returnInfo" \ "landCertForEachProp").asOpt[String]
//      val initialDeclaration = (getReturnRes1.json \ "returnInfo" \ "declaration").asOpt[String]
//
//      println(s"Initial mainPurchaserID: ${initialmainPurchaserID.getOrElse("not set")}")
//      println(s"Initial mainVendorID: ${initialmainVendorID.getOrElse("not set")}")
//      println(s"Initial mainLandID: ${initialmainLandID.getOrElse("not set")}")
//      println(s"Initial IRMarkGenerated: ${initialIRMark.getOrElse("not set")}")
//      println(s"Initial landCertForEachProp: ${initialLandCert.getOrElse("not set")}")
//      println(s"Initial declaration: ${initialDeclaration.getOrElse("not set")}")
//
//      // STEP 6: Update Return Info
//      println("\n=== STEP 6: Update Return Info ===")
//      val updateReturnInfoPayload = Json.obj(
//        "storn" -> "TEST123460",
//        "returnResourceRef" -> returnResourceRef,
//        "mainPurchaserID" -> purchaserId,
//        "mainVendorID" -> vendorId,
//        "mainLandID" -> landId,
//        "IRMarkGenerated" -> "IRMark123456789",
//        "landCertForEachProp" -> "YES",
//        "declaration" -> "YES"
//      )
//
//      println(s"Updating return info with payload: ${Json.prettyPrint(updateReturnInfoPayload)}")
//
//      val updateReturnInfoRes = postJson(updateReturnInfoEndpoint, updateReturnInfoPayload)
//      println(s"Update return info response status: ${updateReturnInfoRes.status}")
//      println(s"Update return info response body: ${Json.prettyPrint(updateReturnInfoRes.json)}")
//
//      updateReturnInfoRes.status mustBe OK
//      val updateResult = (updateReturnInfoRes.json \ "updated").as[Boolean]
//      updateResult mustBe true
//
//      println("✓ Return info updated successfully")
//
//      // STEP 7: Get Return (verify changes)
//      println("\n=== STEP 7: Get Return (verify changes) ===")
//      val getReturnRes2 = postJson(getReturnEndpoint, getReturnPayload)
//      println(s"Get return response status: ${getReturnRes2.status}")
//      println(s"Return info after update: ${Json.prettyPrint((getReturnRes2.json \ "returnInfo").get)}")
//
//      getReturnRes2.status mustBe OK
//
//      val updatedmainPurchaserID = (getReturnRes2.json \ "returnInfo" \ "mainPurchaserID").asOpt[String]
//      val updatedmainVendorID = (getReturnRes2.json \ "returnInfo" \ "mainVendorID").asOpt[String]
//      val updatedmainLandID = (getReturnRes2.json \ "returnInfo" \ "mainLandID").asOpt[String]
//      val updatedIRMark = (getReturnRes2.json \ "returnInfo" \ "IRMarkGenerated").asOpt[String]
//      val updatedLandCert = (getReturnRes2.json \ "returnInfo" \ "landCertForEachProp").asOpt[String]
//      val updatedDeclaration = (getReturnRes2.json \ "returnInfo" \ "declaration").asOpt[String]
//
//      println(s"Updated mainPurchaserID: ${updatedmainPurchaserID.getOrElse("not set")}")
//      println(s"Updated mainVendorID: ${updatedmainVendorID.getOrElse("not set")}")
//      println(s"Updated mainLandID: ${updatedmainLandID.getOrElse("not set")}")
//      println(s"Updated IRMarkGenerated: ${updatedIRMark.getOrElse("not set")}")
//      println(s"Updated landCertForEachProp: ${updatedLandCert.getOrElse("not set")}")
//      println(s"Updated declaration: ${updatedDeclaration.getOrElse("not set")}")
//
//      // Verify all fields were updated
//      updatedmainPurchaserID mustBe Some(purchaserId)
//      updatedmainVendorID mustBe Some(vendorId)
//      updatedmainLandID mustBe Some(landId)
//      updatedIRMark mustBe Some("IRMark123456789")
//      updatedLandCert mustBe Some("YES")
//      updatedDeclaration mustBe Some("YES")
//
//      println("✓ Verified all fields updated correctly")
//
//      println("\n=== UPDATE RETURN INFO WORKFLOW COMPLETE ===")
//    }
//
//    "update return info with N values for boolean fields" in {
//      AuthStub.authorised()
//
//      // STEP 1: Create Return
//      println("\n=== STEP 1: Create Return ===")
//      val createReturnPayload = Json.obj(
//        "stornId" -> "TEST123461",
//        "purchaserIsCompany" -> "NO",
//        "surNameOrCompanyName" -> "UpdateTestN",
//        "houseNumber" -> 200,
//        "addressLine1" -> "Test Street",
//        "postcode" -> "UT2 2UP",
//        "transactionType" -> "RESIDENTIAL"
//      )
//
//      val returnRes = postJson(createReturnEndpoint, createReturnPayload)
//      returnRes.status mustBe CREATED
//      val returnResourceRef = (returnRes.json \ "returnResourceRef").as[String]
//      println(s"✓ Created return with ID: $returnResourceRef")
//
//      // STEP 2: Create Purchaser
//      println("\n=== STEP 2: Create Purchaser ===")
//      val createPurchaserPayload = Json.obj(
//        "stornId" -> "TEST123461",
//        "returnResourceRef" -> returnResourceRef,
//        "isCompany" -> "NO",
//        "isTrustee" -> "NO",
//        "isConnectedToVendor" -> "NO",
//        "isRepresentedByAgent" -> "NO",
//        "surname" -> "Jones",
//        "address1" -> "Main Street",
//        "hasNino" -> "NO"
//      )
//
//      val purchaserRes = postJson(createPurchaserEndpoint, createPurchaserPayload)
//      purchaserRes.status mustBe CREATED
//      val purchaserId = (purchaserRes.json \ "purchaserId").as[String]
//      println(s"✓ Created purchaser with ID: $purchaserId")
//
//      // STEP 3: Create Vendor
//      println("\n=== STEP 3: Create Vendor ===")
//      val createVendorPayload = Json.obj(
//        "stornId" -> "TEST123461",
//        "returnResourceRef" -> returnResourceRef,
//        "name" -> "Vendor2",
//        "addressLine1" -> "Vendor Road",
//        "isRepresentedByAgent" -> "NO"
//      )
//
//      val vendorRes = postJson(createVendorEndpoint, createVendorPayload)
//      vendorRes.status mustBe CREATED
//      val vendorId = (vendorRes.json \ "vendorId").as[String]
//      println(s"✓ Created vendor with ID: $vendorId")
//
//      // STEP 4: Create Land
//      println("\n=== STEP 4: Create Land ===")
//      val createLandPayload = Json.obj(
//        "stornId" -> "TEST123461",
//        "returnResourceRef" -> returnResourceRef,
//        "propertyType" -> "NON_RESIDENTIAL",
//        "interestTransferredCreated" -> "LEASEHOLD",
//        "addressLine1" -> "Business Park"
//      )
//
//      val landRes = postJson(createLandEndpoint, createLandPayload)
//      landRes.status mustBe CREATED
//      val landId = (landRes.json \ "landId").as[String]
//      println(s"✓ Created land with ID: $landId")
//
//      // STEP 5: Update Return Info with N values
//      println("\n=== STEP 5: Update Return Info with N values ===")
//      val updateReturnInfoPayload = Json.obj(
//        "storn" -> "TEST123461",
//        "returnResourceRef" -> returnResourceRef,
//        "mainPurchaserID" -> purchaserId,
//        "mainVendorID" -> vendorId,
//        "mainLandID" -> landId,
//        "IRMarkGenerated" -> "IRMark999999999",
//        "landCertForEachProp" -> "NO",
//        "declaration" -> "NO"
//      )
//
//      println(s"Updating return info with payload: ${Json.prettyPrint(updateReturnInfoPayload)}")
//
//      val updateReturnInfoRes = postJson(updateReturnInfoEndpoint, updateReturnInfoPayload)
//      println(s"Update return info response status: ${updateReturnInfoRes.status}")
//
//      updateReturnInfoRes.status mustBe OK
//      val updateResult = (updateReturnInfoRes.json \ "updated").as[Boolean]
//      updateResult mustBe true
//
//      println("✓ Return info updated successfully with N values")
//
//      // STEP 6: Get Return (verify N values)
//      println("\n=== STEP 6: Get Return (verify N values) ===")
//      val getReturnPayload = Json.obj(
//        "storn" -> "TEST123461",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes.status mustBe OK
//
//      val updatedLandCert = (getReturnRes.json \ "returnInfo" \ "landCertForEachProp").asOpt[String]
//      val updatedDeclaration = (getReturnRes.json \ "returnInfo" \ "declaration").asOpt[String]
//
//      println(s"Updated landCertForEachProp: ${updatedLandCert.getOrElse("not set")}")
//      println(s"Updated declaration: ${updatedDeclaration.getOrElse("not set")}")
//
//      updatedLandCert mustBe Some("NO")
//      updatedDeclaration mustBe Some("NO")
//
//      println("✓ Verified N values updated correctly")
//
//      println("\n=== UPDATE RETURN INFO WITH N VALUES WORKFLOW COMPLETE ===")
//    }
//  }
//}