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
//import play.api.libs.json.{JsNull, JsValue, Json}
//import uk.gov.hmrc.formpproxy.itutil.{ApplicationWithWiremock, AuthStub}
//
//class TransactionFullWorkflowIntegrationSpec
//  extends AnyWordSpec
//    with Matchers
//    with ScalaFutures
//    with IntegrationPatience
//    with ApplicationWithWiremock {
//
//  private val createReturnEndpoint      = "create/return"
//  private val getReturnEndpoint         = "retrieve-return"
//  private val updateTransactionEndpoint = "filing/update/transaction"
//
//  private def uniqueStorn(): String =
//    System.currentTimeMillis().toString.takeRight(10)
//
//  "Complete Transaction Workflow" should {
//
//    "create return, update transaction, and verify updated values" in {
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
//      println(returnRes.json)
//
//      println("\n\n=== STEP 2: Update Transaction ===")
//      val updateTransactionPayload = Json.obj(
//        "storn"             -> storn,
//        "returnResourceRef" -> returnResourceRef,
//        "transaction" -> Json.obj(
//          "claimingRelief"               -> "N",
//          "reliefAmount"                 -> JsNull,
//          "reliefReason"                 -> JsNull,
//          "reliefSchemeNumber"           -> JsNull,
//          "isLinked"                     -> "N",
//          "totalConsiderLinked"          -> JsNull,
//          "totalConsider"                -> "250000",
//          "considerBuild"                -> "N",
//          "considerCash"                 -> "Y",
//          "considerContingent"           -> "N",
//          "considerDebt"                 -> "N",
//          "considerEmploy"               -> "N",
//          "considerOther"                -> "N",
//          "considerLand"                 -> "N",
//          "considerServices"             -> "N",
//          "considerSharesQtd"            -> "N",
//          "considerSharesUnqtd"          -> "N",
//          "considerVat"                  -> "N",
//          "includesChattel"              -> "N",
//          "includesGoodwill"             -> "N",
//          "includesOther"                -> "N",
//          "includesStock"                -> "N",
//          "usedAsFactory"                -> "N",
//          "usedAsHotel"                  -> "N",
//          "usedAsIndustrial"             -> "N",
//          "usedAsOffice"                 -> "N",
//          "usedAsOther"                  -> "N",
//          "usedAsShop"                   -> "N",
//          "usedAsWarehouse"              -> "N",
//          "contractDate"                 -> "2025-01-15",
//          "isDependOnFutureEvent"        -> "N",
//          "transactionDescription"       -> "RESIDENTIAL",
//          "newTransactionDescription"    -> "RESIDENTIAL",
//          "effectiveDate"                -> "2025-02-01",
//          "isLandExchanged"              -> "N",
//          "exLandHouseNumber"            -> JsNull,
//          "exLandAddress1"               -> JsNull,
//          "exLandAddress2"               -> JsNull,
//          "exLandAddress3"               -> JsNull,
//          "exLandAddress4"               -> JsNull,
//          "exLandPostcode"               -> JsNull,
//          "agreedDeferPay"               -> "N",
//          "postTransactionRulingApplied" -> "N",
//          "isPursuantToPreviousOption"   -> "N",
//          "restAffectInt"                -> "N",
//          "restDetails"                  -> JsNull,
//          "postTransactionRulingFollowed"-> "N",
//          "isPartOfSaleOfBusiness"       -> "N",
//          "totalConsiderationOfBusiness" -> JsNull
//        )
//      )
//
//      val updateRes = postJson(updateTransactionEndpoint, updateTransactionPayload)
//      println(s"Update response status: ${updateRes.status}")
//      println(s"Update response body: ${updateRes.json}")
//      updateRes.status mustBe OK
//      println("Transaction updated successfully")
//
//      println("\n\n=== STEP 3: Get Return (verify updated transaction) ===")
//      val getReturnPayload = Json.obj(
//        "storn"             -> storn,
//        "returnResourceRef" -> returnResourceRef
//      )
//
//      val getReturnRes = postJson(getReturnEndpoint, getReturnPayload)
//      getReturnRes.status mustBe OK
//      println("Retrieved return with transaction:")
//      println(Json.prettyPrint(getReturnRes.json))
//
//      val transaction = (getReturnRes.json \ "transaction").toOption
//      transaction must not be empty
//
//      val txData = transaction.get
//      (txData \ "totalConsideration").asOpt[BigDecimal] mustBe Some(BigDecimal("250000"))
//      (txData \ "contractDate").asOpt[String] mustBe Some("2025-01-15")
//      (txData \ "effectiveDate").asOpt[String] mustBe Some("2025-02-01")
//      (txData \ "transactionDescription").asOpt[String] mustBe Some("RESIDENTIAL")
//      (txData \ "claimingRelief").asOpt[String] mustBe Some("N")
//      (txData \ "isLinked").asOpt[String] mustBe Some("N")
//      println("✓ Verified transaction data matches updated values")
//
//      println("\n\n=== WORKFLOW COMPLETE ===")
//    }
//  }
//}