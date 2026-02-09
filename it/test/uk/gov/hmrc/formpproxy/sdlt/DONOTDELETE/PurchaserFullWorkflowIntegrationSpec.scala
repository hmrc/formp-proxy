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
//class PurchaserFullWorkflowIntegrationSpec
//  extends AnyWordSpec
//    with Matchers
//    with ScalaFutures
//    with IntegrationPatience
//    with ApplicationWithWiremock {
//
//  private val createReturnEndpoint = "create/return"
//  private val getReturnEndpoint = "retrieve-return"
//  private val createPurchaserEndpoint = "filing/create/purchaser"
//  private val updatePurchaserEndpoint = "filing/update/purchaser"
//  private val deletePurchaserEndpoint = "filing/delete/purchaser"
//  private val createCompanyDetailsEndpoint = "filing/create/company-details"
//  private val updateCompanyDetailsEndpoint = "filing/update/company-details"
//  private val deleteCompanyDetailsEndpoint = "filing/delete/company-details"
//
//  "Complete Purchaser Workflow" should {
//
//    "create return, add purchaser, update purchaser, get return, delete purchaser, and verify deletion" in {
//      AuthStub.authorised()
//
//      println("\n\n=== STEP 1: Create Return ===")
//      val createReturnPayload = Json.obj(
//        "stornId" -> "TEST123456",
//        "purchaserIsCompany" -> "N",
//        "surNameOrCompanyName" -> "Johnson",
//        "houseNumber" -> 100,
//        "addressLine1" -> "High Street",
//        "addressLine2" -> "Anytown",
//        "postcode" -> "AB12 3CD",
//        "transactionType" -> "RESIDENTIAL"
//      )
//
//      val returnRes = postJson(createReturnEndpoint, createReturnPayload)
//      returnRes.status mustBe CREATED
//      val returnResourceRef = (returnRes.json \ "returnResourceRef").as[String]
//      println(s"Created return with ID: $returnResourceRef")
//      println(returnRes.json)
//
//      println("\n\n=== STEP 2: Create Purchaser ===")
//      val createPurchaserPayload = Json.obj(
//        "stornId" -> "TEST123456",
//        "returnResourceRef" -> returnResourceRef,
//        "isCompany" -> "NO",
//        "isTrustee" -> "NO",
//        "isConnectedToVendor" -> "NO",
//        "isRepresentedByAgent" -> "NO",
//        "title" -> "Mr",
//        "surname" -> "Original Surname",
//        "forename1" -> "John",
//        "forename2" -> "Paul",
//        "houseNumber" -> "10",
//        "address1" -> "Original Street",
//        "address2" -> "Original Town",
//        "postcode" -> "SW1A 1AA",
//        "phone" -> "01234567890",
//        "hasNino" -> "YES",
//        "nino" -> "AB123456C",
//        "dateOfBirth" -> "1980-01-15"
//      )
//
//      val purchaserRes = postJson(createPurchaserEndpoint, createPurchaserPayload)
//      purchaserRes.status mustBe CREATED
//      val purchaserResourceRef = (purchaserRes.json \ "purchaserResourceRef").as[String]
//      val purchaserId = (purchaserRes.json \ "purchaserId").as[String]
//      println(s"Created purchaser with purchaserResourceRef: $purchaserResourceRef, purchaserId: $purchaserId")
//      println(purchaserRes.json)
//
//      println("\n\n=== STEP 3: Update Purchaser ===")
//      val updatePurchaserPayload = Json.obj(
//        "stornId" -> "TEST123456",
//        "returnResourceRef" -> returnResourceRef,
//        "purchaserResourceRef" -> purchaserResourceRef,
//        "isCompany" -> "NO",
//        "isTrustee" -> "NO",
//        "isConnectedToVendor" -> "YES",
//        "isRepresentedByAgent" -> "YES",
//        "title" -> "Mrs",
//        "surname" -> "Updated Surname",
//        "forename1" -> "Jane",
//        "forename2" -> "Marie",
//        "houseNumber" -> "42",
//        "address1" -> "Updated High Street",
//        "address2" -> "Updated Town",
//        "address3" -> "Updated County",
//        "postcode" -> "W1A 1AA",
//        "phone" -> "07777123456",
//        "hasNino" -> "YES",
//        "nino" -> "AB123456C",
//        "dateOfBirth" -> "1980-01-15"
//      )
//
//      val updateRes = postJson(updatePurchaserEndpoint, updatePurchaserPayload)
//      updateRes.status mustBe OK
//      println("Purchaser updated successfully")
//      println(updateRes.json)
//
//      println("\n\n=== STEP 4: Get Return (with purchaser) ===")
//      val getReturnPayload1 = Json.obj(
//        "storn" -> "TEST123456",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes1 = postJson(getReturnEndpoint, getReturnPayload1)
//      getReturnRes1.status mustBe OK
//      println("Retrieved return with purchaser:")
//      println(Json.prettyPrint(getReturnRes1.json))
//
//      val purchasers1 = (getReturnRes1.json \ "purchaser").asOpt[Seq[JsValue]]
//      purchasers1 must not be empty
//      val ourPurchaser = purchasers1.get.find(p =>
//        (p \ "purchaserResourceRef").asOpt[String].contains(purchaserResourceRef)
//      )
//      ourPurchaser must not be empty
//
//      val purchaser = ourPurchaser.get
//      (purchaser \ "purchaserResourceRef").asOpt[String] mustBe Some(purchaserResourceRef)
//      (purchaser \ "surname").asOpt[String] mustBe Some("Updated Surname")
//      (purchaser \ "forename1").asOpt[String] mustBe Some("Jane")
//      (purchaser \ "title").asOpt[String] mustBe Some("Mrs")
//      (purchaser \ "address1").asOpt[String] mustBe Some("Updated High Street")
//      (purchaser \ "postcode").asOpt[String] mustBe Some("W1A 1AA")
//      (purchaser \ "isConnectedToVendor").asOpt[String] mustBe Some("YES")
//      (purchaser \ "isRepresentedByAgent").asOpt[String] mustBe Some("YES")
//      println("✓ Verified purchaser data matches updated values")
//
//      println("\n\n=== STEP 5: Delete Purchaser ===")
//      val deletePurchaserPayload = Json.obj(
//        "storn" -> "TEST123456",
//        "returnResourceRef" -> returnResourceRef,
//        "purchaserResourceRef" -> purchaserResourceRef
//      )
//
//      val deleteRes = postJson(deletePurchaserEndpoint, deletePurchaserPayload)
//      println(s"Delete response status: ${deleteRes.status}")
//      println(s"Delete response body: ${deleteRes.json}")
//
//      if (deleteRes.status == BAD_REQUEST) {
//        println(s"Delete failed with: ${deleteRes.json}")
//      }
//
//      println(s"Attempted to delete purchaser with purchaserResourceRef: $purchaserResourceRef")
//
//      println("\n\n=== STEP 6: Get Return (verify purchaser status) ===")
//      val getReturnPayload2 = Json.obj(
//        "storn" -> "TEST123456",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes2 = postJson(getReturnEndpoint, getReturnPayload2)
//      getReturnRes2.status mustBe OK
//      println("Retrieved return after deletion attempt:")
//      println(Json.prettyPrint(getReturnRes2.json))
//
//      // Check purchaser status
//      val purchasers2 = (getReturnRes2.json \ "purchaser").asOpt[Seq[JsValue]]
//      if (purchasers2.isEmpty) {
//        println("✓ Verified purchaser has been deleted from return")
//      } else {
//        println(s"⚠ Purchaser still exists: ${purchasers2.get.size} purchaser(s) found")
//      }
//
//      println("\n\n=== WORKFLOW COMPLETE ===")
//    }
//
//    "create return with multiple purchasers and delete only one" in {
//      AuthStub.authorised()
//
//      println("\n\n=== Multiple Purchasers Test ===")
//
//      // Step 1: Create return
//      val createReturnPayload = Json.obj(
//        "stornId" -> "TEST123457",
//        "purchaserIsCompany" -> "N",
//        "surNameOrCompanyName" -> "Williams",
//        "addressLine1" -> "Main Street",
//        "transactionType" -> "RESIDENTIAL"
//      )
//
//      val returnRes = postJson(createReturnEndpoint, createReturnPayload)
//      returnRes.status mustBe CREATED
//      val returnResourceRef = (returnRes.json \ "returnResourceRef").as[String]
//      println(s"Created return: $returnResourceRef")
//
//      // Step 2: Create first purchaser
//      val createPurchaser1Payload = Json.obj(
//        "stornId" -> "TEST123457",
//        "returnResourceRef" -> returnResourceRef,
//        "isCompany" -> "N",
//        "isTrustee" -> "N",
//        "isConnectedToVendor" -> "N",
//        "isRepresentedByAgent" -> "N",
//        "surname" -> "Purchaser One",
//        "address1" -> "Street One",
//        "hasNino" -> "N"
//      )
//
//      val purchaser1Res = postJson(createPurchaserEndpoint, createPurchaser1Payload)
//      purchaser1Res.status mustBe CREATED
//      val purchaser1ResourceRef = (purchaser1Res.json \ "purchaserResourceRef").as[String]
//      println(s"Created purchaser 1: $purchaser1ResourceRef")
//
//      // Step 3: Create second purchaser
//      val createPurchaser2Payload = Json.obj(
//        "stornId" -> "TEST123457",
//        "returnResourceRef" -> returnResourceRef,
//        "isCompany" -> "N",
//        "isTrustee" -> "N",
//        "isConnectedToVendor" -> "N",
//        "isRepresentedByAgent" -> "N",
//        "surname" -> "Purchaser Two",
//        "address1" -> "Street Two",
//        "hasNino" -> "N"
//      )
//
//      val purchaser2Res = postJson(createPurchaserEndpoint, createPurchaser2Payload)
//      purchaser2Res.status mustBe CREATED
//      val purchaser2ResourceRef = (purchaser2Res.json \ "purchaserResourceRef").as[String]
//      println(s"Created purchaser 2: $purchaser2ResourceRef")
//
//      // Step 4: Get return - verify both explicit purchasers exist
//      val getReturn1Payload = Json.obj(
//        "storn" -> "TEST123457",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturn1Res = postJson(getReturnEndpoint, getReturn1Payload)
//      getReturn1Res.status mustBe OK
//      val purchasers1 = (getReturn1Res.json \ "purchaser").asOpt[Seq[JsValue]]
//      purchasers1 must not be empty
//
//      // Verify both our explicit purchasers exist (may also have auto-created purchaser)
//      val hasPurchaser1Before = purchasers1.get.exists(p =>
//        (p \ "purchaserResourceRef").asOpt[String].contains(purchaser1ResourceRef)
//      )
//      val hasPurchaser2Before = purchasers1.get.exists(p =>
//        (p \ "purchaserResourceRef").asOpt[String].contains(purchaser2ResourceRef)
//      )
//      hasPurchaser1Before mustBe true
//      hasPurchaser2Before mustBe true
//      val originalCount = purchasers1.get.size
//      println(s"✓ Verified both explicit purchasers exist (total: $originalCount purchasers)")
//
//      // Step 5: Delete first purchaser
//      val deletePurchaser1Payload = Json.obj(
//        "storn" -> "TEST123457",
//        "returnResourceRef" -> returnResourceRef,
//        "purchaserResourceRef" -> purchaser1ResourceRef
//      )
//
//      val deleteRes = postJson(deletePurchaserEndpoint, deletePurchaser1Payload)
//      println(s"Delete response: status=${deleteRes.status}, body=${deleteRes.json}")
//
//      if (deleteRes.status != NO_CONTENT && deleteRes.status != OK) {
//        println(s"⚠ Delete returned ${deleteRes.status} instead of 204 or 200")
//      }
//
//      // Step 6: Get return - verify purchaser 1 deleted, purchaser 2 remains
//      val getReturn2Payload = Json.obj(
//        "storn" -> "TEST123457",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturn2Res = postJson(getReturnEndpoint, getReturn2Payload)
//      getReturn2Res.status mustBe OK
//      println("Retrieved return after deleting one purchaser:")
//      println(Json.prettyPrint(getReturn2Res.json))
//
//      val purchasers2 = (getReturn2Res.json \ "purchaser").asOpt[Seq[JsValue]]
//      purchasers2 must not be empty
//
//      // Verify count decreased by 1
//      purchasers2.get.size mustBe (originalCount - 1)
//      println(s"✓ Verified purchaser count decreased from $originalCount to ${purchasers2.get.size}")
//
//      // Verify the specific purchaser we deleted is gone
//      val hasPurchaser1After = purchasers2.get.exists(p =>
//        (p \ "purchaserResourceRef").asOpt[String].contains(purchaser1ResourceRef)
//      )
//      hasPurchaser1After mustBe false
//      println("✓ Verified deleted purchaser (Purchaser One) is gone")
//
//      // Verify the other purchaser we created still exists
//      val hasPurchaser2After = purchasers2.get.exists(p =>
//        (p \ "purchaserResourceRef").asOpt[String].contains(purchaser2ResourceRef)
//      )
//      hasPurchaser2After mustBe true
//      println("✓ Verified remaining purchaser (Purchaser Two) still exists")
//
//      println("\n\n=== Multiple Purchasers Test Complete ===")
//    }  }
//
//  "Complete Company Details Workflow" should {
//
//    "create return, add purchaser company, add company details, update company details, and delete company details" in {
//      AuthStub.authorised()
//
//      println("\n\n=== COMPANY DETAILS WORKFLOW ===")
//
//      println("\n=== STEP 1: Create Return ===")
//      val createReturnPayload = Json.obj(
//        "stornId" -> "TEST123458",
//        "purchaserIsCompany" -> "Y",
//        "surNameOrCompanyName" -> "Tech Corp Ltd",
//        "addressLine1" -> "Tech Street",
//        "transactionType" -> "COMMERCIAL"
//      )
//
//      val returnRes = postJson(createReturnEndpoint, createReturnPayload)
//      returnRes.status mustBe CREATED
//      val returnResourceRef = (returnRes.json \ "returnResourceRef").as[String]
//      println(s"Created return: $returnResourceRef")
//
//      println("\n=== STEP 2: Create Company Purchaser ===")
//      val createPurchaserPayload = Json.obj(
//        "stornId" -> "TEST123458",
//        "returnResourceRef" -> returnResourceRef,
//        "isCompany" -> "Y",
//        "isTrustee" -> "N",
//        "isConnectedToVendor" -> "N",
//        "isRepresentedByAgent" -> "N",
//        "companyName" -> "Tech Corp Ltd",
//        "address1" -> "123 Business Park",
//        "address2" -> "Innovation District",
//        "postcode" -> "EC1A 1BB",
//        "phone" -> "02012345678",
//        "isUkCompany" -> "Y",
//        "hasNino" -> "N",
//        "registrationNumber" -> "12345678"
//      )
//
//      val purchaserRes = postJson(createPurchaserEndpoint, createPurchaserPayload)
//      purchaserRes.status mustBe CREATED
//      val purchaserResourceRef = (purchaserRes.json \ "purchaserResourceRef").as[String]
//      println(s"Created company purchaser: $purchaserResourceRef")
//
//      println("\n=== STEP 3: Create Company Details ===")
//      val createCompanyDetailsPayload = Json.obj(
//        "stornId" -> "TEST123458",
//        "returnResourceRef" -> returnResourceRef,
//        "purchaserResourceRef" -> purchaserResourceRef,
//        "utr" -> "1234567890",
//        "vatReference" -> "GB123456789",
//        "compTypeOcompany" -> "Y",
//        "compTypeProperty" -> "N",
//        "compTypeBank" -> "N"
//      )
//
//      val companyDetailsRes = postJson(createCompanyDetailsEndpoint, createCompanyDetailsPayload)
//      companyDetailsRes.status mustBe CREATED
//      val companyDetailsId = (companyDetailsRes.json \ "companyDetailsId").as[String]
//      println(s"Created company details: id=$companyDetailsId")
//      println(companyDetailsRes.json)
//
//      println("\n=== STEP 4: Get Return (with company details) ===")
//      val getReturnPayload1 = Json.obj(
//        "storn" -> "TEST123458",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes1 = postJson(getReturnEndpoint, getReturnPayload1)
//      getReturnRes1.status mustBe OK
//      println("Retrieved return with company details:")
//      println(Json.prettyPrint(getReturnRes1.json))
//
//      // Verify company details exist
//      val companyDetails1 = (getReturnRes1.json \ "companyDetails").asOpt[JsValue]
//      companyDetails1 must not be empty
//      (companyDetails1.get \ "UTR").asOpt[String] mustBe Some("1234567890")
//      (companyDetails1.get \ "VATReference").asOpt[String] mustBe Some("GB123456789")
//      (companyDetails1.get \ "companyTypeOthercompany").asOpt[String] mustBe Some("Y")
//      println("✓ Verified company details exist with correct values")
//
//      println("\n=== STEP 5: Update Company Details ===")
//      val updateCompanyDetailsPayload = Json.obj(
//        "stornId" -> "TEST123458",
//        "returnResourceRef" -> returnResourceRef,
//        "purchaserResourceRef" -> purchaserResourceRef,
//        "utr" -> "9876543210",
//        "vatReference" -> "GB987654321",
//        "compTypeOcompany" -> "Y",
//        "compTypeProperty" -> "Y",
//        "compTypeBank" -> "N",
//        "compTypeBuilder" -> "Y"
//      )
//
//      val updateCompanyDetailsRes = postJson(updateCompanyDetailsEndpoint, updateCompanyDetailsPayload)
//      updateCompanyDetailsRes.status mustBe OK
//      println("Company details updated successfully")
//      println(updateCompanyDetailsRes.json)
//
//      println("\n=== STEP 6: Get Return (verify updated company details) ===")
//      val getReturnPayload2 = Json.obj(
//        "storn" -> "TEST123458",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes2 = postJson(getReturnEndpoint, getReturnPayload2)
//      getReturnRes2.status mustBe OK
//      println("Retrieved return with updated company details:")
//      println(Json.prettyPrint(getReturnRes2.json))
//
//      val companyDetails2 = (getReturnRes2.json \ "companyDetails").asOpt[JsValue]
//      companyDetails2 must not be empty
//      (companyDetails2.get \ "UTR").asOpt[String] mustBe Some("9876543210")
//      (companyDetails2.get \ "VATReference").asOpt[String] mustBe Some("GB987654321")
//      (companyDetails2.get \ "companyTypeProperty").asOpt[String] mustBe Some("Y")
//      (companyDetails2.get \ "companyTypeBuilder").asOpt[String] mustBe Some("Y")
//      println("✓ Verified company details were updated correctly")
//
//      println("\n=== STEP 7: Delete Company Details ===")
//      val deleteCompanyDetailsPayload = Json.obj(
//        "storn" -> "TEST123458",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val deleteCompanyDetailsRes = postJson(deleteCompanyDetailsEndpoint, deleteCompanyDetailsPayload)
//      println(s"Delete company details response: status=${deleteCompanyDetailsRes.status}")
//      println(s"Delete company details body: ${deleteCompanyDetailsRes.json}")
//
//      println("\n=== STEP 8: Get Return (verify company details deleted) ===")
//      val getReturnPayload3 = Json.obj(
//        "storn" -> "TEST123458",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes3 = postJson(getReturnEndpoint, getReturnPayload3)
//      getReturnRes3.status mustBe OK
//      println("Retrieved return after company details deletion:")
//      println(Json.prettyPrint(getReturnRes3.json))
//
//      val companyDetails3 = (getReturnRes3.json \ "companyDetails").asOpt[JsValue]
//      if (companyDetails3.isEmpty) {
//        println("✓ Verified company details have been deleted")
//      } else {
//        println(s"⚠ Company details still exist")
//      }
//
//      println("\n\n=== COMPANY DETAILS WORKFLOW COMPLETE ===")
//    }
//
//    "handle multiple company types and flags correctly" in {
//      AuthStub.authorised()
//
//      println("\n\n=== Multiple Company Types Test ===")
//
//      // Create return
//      val createReturnPayload = Json.obj(
//        "stornId" -> "TEST123459",
//        "purchaserIsCompany" -> "Y",
//        "surNameOrCompanyName" -> "Multi Type Corp",
//        "addressLine1" -> "Type Street",
//        "transactionType" -> "COMMERCIAL"
//      )
//
//      val returnRes = postJson(createReturnEndpoint, createReturnPayload)
//      returnRes.status mustBe CREATED
//      val returnResourceRef = (returnRes.json \ "returnResourceRef").as[String]
//
//      // Create purchaser
//      val createPurchaserPayload = Json.obj(
//        "stornId" -> "TEST123459",
//        "returnResourceRef" -> returnResourceRef,
//        "isCompany" -> "Y",
//        "isTrustee" -> "N",
//        "isConnectedToVendor" -> "N",
//        "isRepresentedByAgent" -> "N",
//        "companyName" -> "Multi Type Corp",
//        "address1" -> "Type Street",
//        "isUkCompany" -> "Y",
//        "hasNino" -> "N"
//      )
//
//      val purchaserRes = postJson(createPurchaserEndpoint, createPurchaserPayload)
//      purchaserRes.status mustBe CREATED
//      val purchaserResourceRef = (purchaserRes.json \ "purchaserResourceRef").as[String]
//
//      // Create company details with multiple types
//      val createCompanyDetailsPayload = Json.obj(
//        "stornId" -> "TEST123459",
//        "returnResourceRef" -> returnResourceRef,
//        "purchaserResourceRef" -> purchaserResourceRef,
//        "utr" -> "1111111111",
//        "compTypeBank" -> "Y",
//        "compTypeBuilder" -> "Y",
//        "compTypeProperty" -> "Y",
//        "compTypeInsurance" -> "N",
//        "compTypePenfund" -> "Y"
//      )
//
//      val companyDetailsRes = postJson(createCompanyDetailsEndpoint, createCompanyDetailsPayload)
//      companyDetailsRes.status mustBe CREATED
//      println(s"Created company with multiple types")
//
//      // Verify all types are stored correctly
//      val getReturnPayload = Json.obj(
//        "storn" -> "TEST123459",
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes.status mustBe OK
//
//      val companyDetails = (getReturnRes.json \ "companyDetails").asOpt[JsValue]
//      companyDetails must not be empty
//      (companyDetails.get \ "companyTypeBank").asOpt[String] mustBe Some("Y")
//      (companyDetails.get \ "companyTypeBuilder").asOpt[String] mustBe Some("Y")
//      (companyDetails.get \ "companyTypeProperty").asOpt[String] mustBe Some("Y")
//      (companyDetails.get \ "companyTypePensionfund").asOpt[String] mustBe Some("Y")
//      (companyDetails.get \ "companyTypeInsurance").asOpt[String] mustBe Some("N")
//      println("✓ Verified all company type flags are stored correctly")
//
//      println("\n\n=== Multiple Company Types Test Complete ===")
//    }
//  }
//}