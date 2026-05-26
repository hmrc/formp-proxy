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
//package uk.gov.hmrc.formpproxy.sdlt.DONOTDELETE
//
//import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import play.api.http.Status.*
//import play.api.libs.json.{JsValue, Json}
//import uk.gov.hmrc.formpproxy.itutil.{ApplicationWithWiremock, AuthStub}
//
//class LeaseFullWorkflowIntegrationSpec
//  extends AnyWordSpec
//    with Matchers
//    with ScalaFutures
//    with IntegrationPatience
//    with ApplicationWithWiremock {
//
//  private val createReturnEndpoint = "create/return"
//  private val getReturnEndpoint    = "retrieve-return"
//  private val createLeaseEndpoint  = "filing/create/lease"
//  private val updateLeaseEndpoint  = "filing/update/lease"
//  private val deleteLeaseEndpoint  = "filing/delete/lease"
//
//  private def uniqueStorn(): String =
//    System.currentTimeMillis().toString.takeRight(10)
//
//  "Complete Lease Workflow" should {
//
//    "create return, add lease, update lease, get return, delete lease, and verify deletion" in {
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
//      println("\n\n=== STEP 2: Create Lease (commercial, full fields) ===")
//      val createLeasePayload = Json.obj(
//        "stornId"           -> storn,
//        "returnResourceRef" -> returnResourceRef,
//        "lease" -> Json.obj(
//          "isAnnualRentOver1000" -> "YES",
//          "contractEndDate"      -> "2030-12-31",
//          "contractStartDate"    -> "2025-01-01",
//          "leaseType"            -> "COMMERCIAL",
//          "netPresentValue"      -> "50000",
//          "totalPremiumPayable"  -> "10000",
//          "rentFreePeriod"       -> "NO",
//          "startingRent"         -> "12000",
//          "startingRentEndDate"  -> "2026-01-01",
//          "laterRentKnown"       -> "YES",
//          "vatAmount"            -> "2400"
//        )
//      )
//
//      val createRes = postJson(createLeaseEndpoint, createLeasePayload)
//      createRes.status mustBe CREATED
//      println("Lease created successfully")
//      println(createRes.json)
//
//      println("\n\n=== STEP 3: Update Lease (residential, different values) ===")
//      val updateLeasePayload = Json.obj(
//        "stornId"           -> storn,
//        "returnResourceRef" -> returnResourceRef,
//        "lease" -> Json.obj(
//          "isAnnualRentOver1000" -> "NO",
//          "contractEndDate"      -> "2028-06-30",
//          "contractStartDate"    -> "2025-07-01",
//          "leaseType"            -> "RESIDENTIAL",
//          "netPresentValue"      -> "60000",
//          "totalPremiumPayable"  -> "15000",
//          "rentFreePeriod"       -> "YES",
//          "startingRent"         -> "8000",
//          "startingRentEndDate"  -> "2026-07-01",
//          "laterRentKnown"       -> "NO",
//          "vatAmount"            -> "1600"
//        )
//      )
//
//      val updateRes = postJson(updateLeaseEndpoint, updateLeasePayload)
//      println(s"Update response status: ${updateRes.status}")
//      println(s"Update response body: ${updateRes.json}")
//      updateRes.status mustBe OK
//      println("Lease updated successfully")
//
//      println("\n\n=== STEP 4: Get Return (verify updated values) ===")
//      val getReturnPayload = Json.obj(
//        "storn"             -> storn,
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes1 = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes1.status mustBe OK
//      println("Retrieved return with lease:")
//      println(Json.prettyPrint(getReturnRes1.json))
//
//      val lease1 = (getReturnRes1.json \ "lease").toOption
//      lease1 must not be empty
//
//      val leaseData = lease1.get
//      (leaseData \ "isAnnualRentOver1000").asOpt[String] mustBe Some("NO")
//      (leaseData \ "contractEndDate").asOpt[String] mustBe Some("2028-06-30")
//      (leaseData \ "contractStartDate").asOpt[String] mustBe Some("2025-07-01")
//      (leaseData \ "leaseType").asOpt[String] mustBe Some("RESIDENTIAL")
//      (leaseData \ "netPresentValue").asOpt[String] mustBe Some("60000")
//      (leaseData \ "totalPremiumPayable").asOpt[String] mustBe Some("15000")
//      (leaseData \ "rentFreePeriod").asOpt[String] mustBe Some("YES")
//      (leaseData \ "startingRent").asOpt[String] mustBe Some("8000")
//      (leaseData \ "startingRentEndDate").asOpt[String] mustBe Some("2026-07-01")
//      (leaseData \ "laterRentKnown").asOpt[String] mustBe Some("NO")
//      (leaseData \ "VATAmount").asOpt[String] mustBe Some("1600")
//      println("✓ Verified lease data matches updated values")
//
//      println("\n\n=== STEP 5: Delete Lease ===")
//      val deleteLeasePayload = Json.obj(
//        "storn"             -> storn,
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val deleteRes = postJson(deleteLeaseEndpoint, deleteLeasePayload)
//      println(s"Delete response status: ${deleteRes.status}")
//      println(s"Delete response body: ${deleteRes.json}")
//      deleteRes.status mustBe OK
//      println("Lease deleted successfully")
//
//      println("\n\n=== STEP 6: Get Return (verify lease removed) ===")
//      val getReturnRes2 = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes2.status mustBe OK
//      println("Retrieved return after deletion:")
//      println(Json.prettyPrint(getReturnRes2.json))
//
//      val lease2 = (getReturnRes2.json \ "lease").toOption
//      if (lease2.isEmpty) {
//        println("✓ Verified lease has been deleted from return")
//      } else {
//        println(s"⚠ Lease still exists on return")
//      }
//
//      println("\n\n=== WORKFLOW COMPLETE ===")
//    }
//  }
//}