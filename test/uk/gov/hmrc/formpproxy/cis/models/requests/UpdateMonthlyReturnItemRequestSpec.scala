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

package uk.gov.hmrc.formpproxy.cis.models.requests

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class UpdateMonthlyReturnItemRequestSpec extends AnyWordSpec with Matchers {

  private val model = UpdateMonthlyReturnItemRequest(
    instanceId = "abc-123",
    taxYear = 2024,
    taxMonth = 5,
    amendment = "N",
    itemResourceReference = 9876543210L,
    totalPayments = "1000.00",
    costOfMaterials = "250.00",
    totalDeducted = "150.00",
    subcontractorName = "John Smith Ltd",
    verificationNumber = "V1234567"
  )

  private val json: JsValue = Json.parse("""
                                           |{
                                           |  "instanceId": "abc-123",
                                           |  "taxYear": 2024,
                                           |  "taxMonth": 5,
                                           |  "amendment": "N",
                                           |  "itemResourceReference": 9876543210,
                                           |  "totalPayments": "1000.00",
                                           |  "costOfMaterials": "250.00",
                                           |  "totalDeducted": "150.00",
                                           |  "subcontractorName": "John Smith Ltd",
                                           |  "verificationNumber": "V1234567"
                                           |}
                                           |""".stripMargin)

  "UpdateMonthlyReturnItemRequest JSON format" should {

    "serialize the model to JSON with all fields" in {
      Json.toJson(model) mustBe json
    }

    "deserialize JSON to the model with all fields" in {
      json.validate[UpdateMonthlyReturnItemRequest] mustBe JsSuccess(model)
    }

    "round-trip (model -> JSON -> model) consistently" in {
      Json.toJson(model).validate[UpdateMonthlyReturnItemRequest] mustBe JsSuccess(model)
    }

    "fail validation when required fields are missing" in {
      val missingRequired = Json.parse("""
                                         |{
                                         |  "instanceId": "abc-123",
                                         |  "taxYear": 2024
                                         |}
                                         |""".stripMargin)

      val result = missingRequired.validate[UpdateMonthlyReturnItemRequest]
      result.isError mustBe true

      result match {
        case JsError(errors) =>
          val paths = errors.map(_._1.toString).toSet
          paths must contain("/taxMonth")
          paths must contain("/amendment")
          paths must contain("/itemResourceReference")
          paths must contain("/totalPayments")
          paths must contain("/costOfMaterials")
          paths must contain("/totalDeducted")
          paths must contain("/subcontractorName")
          paths must contain("/verificationNumber")
        case _               =>
          fail("Expected JsError for missing fields")
      }
    }

    "fail validation when a field has the wrong type" in {
      val wrongTypeJson = Json.parse("""
                                       |{
                                       |  "instanceId": "abc-123",
                                       |  "taxYear": "2024",
                                       |  "taxMonth": 5,
                                       |  "amendment": "N",
                                       |  "itemResourceReference": 9876543210,
                                       |  "totalPayments": "1000.00",
                                       |  "costOfMaterials": "250.00",
                                       |  "totalDeducted": "150.00",
                                       |  "subcontractorName": "John Smith Ltd",
                                       |  "verificationNumber": "V1234567"
                                       |}
                                       |""".stripMargin)

      val result = wrongTypeJson.validate[UpdateMonthlyReturnItemRequest]
      result.isError mustBe true

      val paths = result.fold(
        invalid = errs => errs.map(_._1.toString),
        valid = _ => Seq.empty
      )

      paths must contain("/taxYear")
    }

    "support large Long values for itemResourceReference without loss" in {
      val bigRef          = 9007199254740992L
      val modelWithBigRef = model.copy(itemResourceReference = bigRef)

      Json
        .toJson(modelWithBigRef)
        .validate[UpdateMonthlyReturnItemRequest] mustBe JsSuccess(modelWithBigRef)
    }
  }
}
