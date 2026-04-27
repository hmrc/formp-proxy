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

package uk.gov.hmrc.formpproxy.sdlt.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.formpproxy.sdlt.models.transaction.*

class TransactionModelsSpec extends AnyFreeSpec with Matchers {

  "TransactionPayload" - {

    "must serialize to JSON correctly with all fields populated" in {
      val payload = TransactionPayload(
        claimingRelief = Some("Y"),
        reliefAmount = Some("5000"),
        reliefReason = Some("CHARITY"),
        reliefSchemeNumber = Some("CIS123456"),
        isLinked = Some("N"),
        totalConsiderLinked = Some("500000"),
        totalConsider = Some("250000"),
        considerBuild = Some("N"),
        considerCash = Some("Y"),
        considerContingent = Some("N"),
        considerDebt = Some("N"),
        considerEmploy = Some("N"),
        considerOther = Some("N"),
        considerLand = Some("N"),
        considerServices = Some("N"),
        considerSharesQtd = Some("N"),
        considerSharesUnqtd = Some("N"),
        considerVat = Some("N"),
        includesChattel = Some("N"),
        includesGoodwill = Some("N"),
        includesOther = Some("N"),
        includesStock = Some("N"),
        usedAsFactory = Some("N"),
        usedAsHotel = Some("N"),
        usedAsIndustrial = Some("N"),
        usedAsOffice = Some("N"),
        usedAsOther = Some("N"),
        usedAsShop = Some("N"),
        usedAsWarehouse = Some("N"),
        contractDate = Some("2025-01-15"),
        isDependOnFutureEvent = Some("N"),
        transactionDescription = Some("RESIDENTIAL"),
        newTransactionDescription = Some("RESIDENTIAL"),
        effectiveDate = Some("2025-02-01"),
        isLandExchanged = Some("N"),
        exLandHouseNumber = Some("10"),
        exLandAddress1 = Some("Exchange Street"),
        exLandAddress2 = Some("Exchange Town"),
        exLandAddress3 = Some("Exchange City"),
        exLandAddress4 = Some("Exchange County"),
        exLandPostcode = Some("EX1 1CH"),
        agreedDeferPay = Some("N"),
        postTransactionRulingApplied = Some("N"),
        isPursuantToPreviousOption = Some("N"),
        restAffectInt = Some("N"),
        restDetails = Some("Some restriction details"),
        postTransactionRulingFollowed = Some("N"),
        isPartOfSaleOfBusiness = Some("N"),
        totalConsiderationOfBusiness = Some("500000")
      )

      val json = Json.toJson(payload)

      (json \ "claimingRelief").as[String] mustBe "Y"
      (json \ "reliefAmount").as[String] mustBe "5000"
      (json \ "reliefReason").as[String] mustBe "CHARITY"
      (json \ "reliefSchemeNumber").as[String] mustBe "CIS123456"
      (json \ "isLinked").as[String] mustBe "N"
      (json \ "totalConsiderLinked").as[String] mustBe "500000"
      (json \ "totalConsider").as[String] mustBe "250000"
      (json \ "considerCash").as[String] mustBe "Y"
      (json \ "contractDate").as[String] mustBe "2025-01-15"
      (json \ "effectiveDate").as[String] mustBe "2025-02-01"
      (json \ "transactionDescription").as[String] mustBe "RESIDENTIAL"
      (json \ "newTransactionDescription").as[String] mustBe "RESIDENTIAL"
      (json \ "isLandExchanged").as[String] mustBe "N"
      (json \ "exLandAddress1").as[String] mustBe "Exchange Street"
      (json \ "exLandPostcode").as[String] mustBe "EX1 1CH"
      (json \ "restDetails").as[String] mustBe "Some restriction details"
      (json \ "isPartOfSaleOfBusiness").as[String] mustBe "N"
      (json \ "totalConsiderationOfBusiness").as[String] mustBe "500000"
    }

    "must serialize to JSON correctly with all fields None" in {
      val payload = TransactionPayload()

      val json = Json.toJson(payload)

      (json \ "claimingRelief").toOption mustBe None
      (json \ "reliefAmount").toOption mustBe None
      (json \ "totalConsider").toOption mustBe None
      (json \ "contractDate").toOption mustBe None
      (json \ "effectiveDate").toOption mustBe None
      (json \ "isLandExchanged").toOption mustBe None
      (json \ "isPartOfSaleOfBusiness").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "claimingRelief"                -> "N",
        "isLinked"                      -> "N",
        "totalConsider"                 -> "250000",
        "considerCash"                  -> "Y",
        "includesChattel"               -> "N",
        "contractDate"                  -> "2025-01-15",
        "isDependOnFutureEvent"         -> "N",
        "transactionDescription"        -> "RESIDENTIAL",
        "newTransactionDescription"     -> "RESIDENTIAL",
        "effectiveDate"                 -> "2025-02-01",
        "isLandExchanged"               -> "N",
        "agreedDeferPay"                -> "N",
        "postTransactionRulingApplied"  -> "N",
        "isPursuantToPreviousOption"    -> "N",
        "restAffectInt"                 -> "N",
        "postTransactionRulingFollowed" -> "N",
        "isPartOfSaleOfBusiness"        -> "N"
      )

      val result = json.validate[TransactionPayload]

      result mustBe a[JsSuccess[_]]
      val payload = result.get

      payload.claimingRelief mustBe Some("N")
      payload.isLinked mustBe Some("N")
      payload.totalConsider mustBe Some("250000")
      payload.considerCash mustBe Some("Y")
      payload.contractDate mustBe Some("2025-01-15")
      payload.effectiveDate mustBe Some("2025-02-01")
      payload.transactionDescription mustBe Some("RESIDENTIAL")
      payload.isLandExchanged mustBe Some("N")
      payload.isPartOfSaleOfBusiness mustBe Some("N")
    }

    "must deserialize from empty JSON correctly (all None)" in {
      val json = Json.obj()

      val result = json.validate[TransactionPayload]

      result mustBe a[JsSuccess[_]]
      val payload = result.get

      payload.claimingRelief mustBe None
      payload.reliefAmount mustBe None
      payload.totalConsider mustBe None
      payload.contractDate mustBe None
      payload.effectiveDate mustBe None
      payload.isLandExchanged mustBe None
      payload.isPartOfSaleOfBusiness mustBe None
    }

    "must deserialize with relief fields" in {
      val json = Json.obj(
        "claimingRelief"     -> "Y",
        "reliefAmount"       -> "5000",
        "reliefReason"       -> "CHARITY",
        "reliefSchemeNumber" -> "CIS123456"
      )

      val result = json.validate[TransactionPayload]

      result mustBe a[JsSuccess[_]]
      val payload = result.get

      payload.claimingRelief mustBe Some("Y")
      payload.reliefAmount mustBe Some("5000")
      payload.reliefReason mustBe Some("CHARITY")
      payload.reliefSchemeNumber mustBe Some("CIS123456")
    }

    "must deserialize with land exchange fields" in {
      val json = Json.obj(
        "isLandExchanged"   -> "Y",
        "exLandHouseNumber" -> "10",
        "exLandAddress1"    -> "Exchange Street",
        "exLandAddress2"    -> "Exchange Town",
        "exLandAddress3"    -> "Exchange City",
        "exLandAddress4"    -> "Exchange County",
        "exLandPostcode"    -> "EX1 1CH"
      )

      val result = json.validate[TransactionPayload]

      result mustBe a[JsSuccess[_]]
      val payload = result.get

      payload.isLandExchanged mustBe Some("Y")
      payload.exLandHouseNumber mustBe Some("10")
      payload.exLandAddress1 mustBe Some("Exchange Street")
      payload.exLandAddress2 mustBe Some("Exchange Town")
      payload.exLandAddress3 mustBe Some("Exchange City")
      payload.exLandAddress4 mustBe Some("Exchange County")
      payload.exLandPostcode mustBe Some("EX1 1CH")
    }

    "must deserialize with sale of business fields" in {
      val json = Json.obj(
        "isPartOfSaleOfBusiness"       -> "Y",
        "totalConsiderationOfBusiness" -> "500000"
      )

      val result = json.validate[TransactionPayload]

      result mustBe a[JsSuccess[_]]
      val payload = result.get

      payload.isPartOfSaleOfBusiness mustBe Some("Y")
      payload.totalConsiderationOfBusiness mustBe Some("500000")
    }
  }

  "UpdateTransactionRequest" - {

    "must serialize to JSON correctly" in {
      val request = UpdateTransactionRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        transaction = TransactionPayload(
          claimingRelief = Some("N"),
          isLinked = Some("N"),
          totalConsider = Some("250000"),
          considerCash = Some("Y"),
          contractDate = Some("2025-01-15"),
          effectiveDate = Some("2025-02-01"),
          transactionDescription = Some("RESIDENTIAL")
        )
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "transaction" \ "claimingRelief").as[String] mustBe "N"
      (json \ "transaction" \ "totalConsider").as[String] mustBe "250000"
      (json \ "transaction" \ "considerCash").as[String] mustBe "Y"
      (json \ "transaction" \ "contractDate").as[String] mustBe "2025-01-15"
      (json \ "transaction" \ "effectiveDate").as[String] mustBe "2025-02-01"
    }

    "must serialize to JSON correctly with empty transaction payload" in {
      val request = UpdateTransactionRequest(
        storn = "STORN99999",
        returnResourceRef = "100002",
        transaction = TransactionPayload()
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "transaction" \ "claimingRelief").toOption mustBe None
      (json \ "transaction" \ "totalConsider").toOption mustBe None
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001",
        "transaction"       -> Json.obj(
          "claimingRelief"            -> "N",
          "isLinked"                  -> "N",
          "totalConsider"             -> "250000",
          "considerCash"              -> "Y",
          "contractDate"              -> "2025-01-15",
          "effectiveDate"             -> "2025-02-01",
          "transactionDescription"    -> "RESIDENTIAL",
          "newTransactionDescription" -> "RESIDENTIAL"
        )
      )

      val result = json.validate[UpdateTransactionRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.transaction.claimingRelief mustBe Some("N")
      request.transaction.isLinked mustBe Some("N")
      request.transaction.totalConsider mustBe Some("250000")
      request.transaction.considerCash mustBe Some("Y")
      request.transaction.contractDate mustBe Some("2025-01-15")
      request.transaction.effectiveDate mustBe Some("2025-02-01")
      request.transaction.transactionDescription mustBe Some("RESIDENTIAL")
    }

    "must deserialize from JSON correctly with empty transaction" in {
      val json = Json.obj(
        "storn"             -> "STORN99999",
        "returnResourceRef" -> "100002",
        "transaction"       -> Json.obj()
      )

      val result = json.validate[UpdateTransactionRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN99999"
      request.returnResourceRef mustBe "100002"
      request.transaction.claimingRelief mustBe None
      request.transaction.totalConsider mustBe None
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "transaction"       -> Json.obj()
      )

      val result = json.validate[UpdateTransactionRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj(
        "storn"       -> "STORN12345",
        "transaction" -> Json.obj()
      )

      val result = json.validate[UpdateTransactionRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when transaction is missing" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[UpdateTransactionRequest]

      result.isError mustBe true
    }
  }

  "UpdateTransactionReturn" - {

    "must serialize to JSON correctly when updated is true" in {
      val response = UpdateTransactionReturn(updated = true)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when updated is false" in {
      val response = UpdateTransactionReturn(updated = false)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when updated is true" in {
      val json = Json.obj("updated" -> true)

      val result = json.validate[UpdateTransactionReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe true
    }

    "must deserialize from JSON correctly when updated is false" in {
      val json = Json.obj("updated" -> false)

      val result = json.validate[UpdateTransactionReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe false
    }

    "must fail to deserialize when updated field is missing" in {
      val json = Json.obj()

      val result = json.validate[UpdateTransactionReturn]

      result.isError mustBe true
    }
  }
}
