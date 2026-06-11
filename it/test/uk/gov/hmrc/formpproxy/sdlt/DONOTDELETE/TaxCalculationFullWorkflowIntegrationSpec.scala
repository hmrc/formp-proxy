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
//class TaxCalculationFullWorkflowIntegrationSpec
//  extends AnyWordSpec
//    with Matchers
//    with ScalaFutures
//    with IntegrationPatience
//    with ApplicationWithWiremock {
//
//  private val createReturnEndpoint         = "create/return"
//  private val getReturnEndpoint            = "retrieve-return"
//  private val updateTaxCalculationEndpoint = "filing/update/tax-calculation"
//
//  private def uniqueStorn(): String =
//    System.currentTimeMillis().toString.takeRight(10)
//
//  "Complete Tax Calculation Workflow" should {
//
//    "create return (auto-seeds tax-calculation), update tax-calculation, get return, and verify updated values" in {
//      AuthStub.authorised()
//
//      val storn = uniqueStorn()
//
//      println("\n\n=== STEP 1: Create Return (auto-seeds tax-calculation placeholder) ===")
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
//      println("\n\n=== STEP 2: Update Tax Calculation (penalty included, full fields) ===")
//      val updateTaxCalculationPayload = Json.obj(
//        "stornId"                -> storn,
//        "returnResourceRef"      -> returnResourceRef,
//        "amountPaid"             -> "2000",
//        "includesPenalty"        -> "YES",
//        "taxDue"                 -> "8000",
//        "calcPenaltyDue"         -> "500",
//        "calcTaxDue"             -> "8000",
//        "calcTaxRate1"           -> "3",
//        "calcTaxRate2"           -> "7",
//        "calcTotalTaxPenaltyDue" -> "8500",
//        "calcTotalNpvTax"        -> "1000",
//        "calcTotalPremiumTax"    -> "7500",
//        "taxDuePremium"          -> "7500",
//        "taxDueNpv"              -> "1000",
//        "honestyDeclaration"     -> "YES"
//      )
//
//      val updateRes = postJson(updateTaxCalculationEndpoint, updateTaxCalculationPayload)
//      println(s"Update response status: ${updateRes.status}")
//      println(s"Update response body: ${updateRes.json}")
//      updateRes.status mustBe OK
//      println("Tax calculation updated successfully")
//
//      println("\n\n=== STEP 3: Get Return (verify updated values) ===")
//      val getReturnPayload = Json.obj(
//        "storn"             -> storn,
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes.status mustBe OK
//      println("Retrieved return with tax-calculation:")
//      println(Json.prettyPrint(getReturnRes.json))
//
//      val taxCalc = (getReturnRes.json \ "taxCalculation").toOption
//      taxCalc must not be empty
//
//      val taxCalcData = taxCalc.get
//      (taxCalcData \ "amountPaid").asOpt[String] mustBe Some("2000")
//      (taxCalcData \ "includesPenalty").asOpt[String] mustBe Some("YES")
//      (taxCalcData \ "taxDue").asOpt[String] mustBe Some("8000")
//      (taxCalcData \ "calcPenaltyDue").asOpt[String] mustBe Some("500")
//      (taxCalcData \ "calcTaxDue").asOpt[String] mustBe Some("8000")
//      println("✓ Verified tax-calculation data matches updated values")
//
//      println("\n\n=== WORKFLOW COMPLETE ===")
//    }
//  }
//}
