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

package uk.gov.hmrc.formpproxy.sdlt.controllers

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{ControllerComponents, PlayBodyParsers, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.formpproxy.sdlt.controllers.returns.TaxCalculationReturnsController
import uk.gov.hmrc.formpproxy.sdlt.models.taxCalculation.*
import uk.gov.hmrc.formpproxy.sdlt.services.ReturnService

import scala.concurrent.{ExecutionContext, Future}

class TaxCalculationReturnsControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "TaxCalculationReturnsController updateTaxCalculation" - {

    "returns 200 with updated true when service succeeds" in new Setup {
      val request: UpdateTaxCalculationRequest = UpdateTaxCalculationRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        amountPaid = Some("2000"),
        includesPenalty = Some("YES"),
        taxDue = Some("8000"),
        calcPenaltyDue = Some("500"),
        calcTaxDue = Some("8000"),
        calcTaxRate1 = Some("3"),
        calcTaxRate2 = Some("7"),
        calcTotalTaxPenaltyDue = Some("8500"),
        calcTotalNpvTax = Some("1000"),
        calcTotalPremiumTax = Some("7500"),
        taxDuePremium = Some("7500"),
        taxDueNpv = Some("1000"),
        honestyDeclaration = Some("YES")
      )

      val expectedResponse: UpdateTaxCalculationReturn = UpdateTaxCalculationReturn(updated = true)

      when(mockService.updateTaxCalculation(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateTaxCalculation()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateTaxCalculation(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with minimal update details" in new Setup {
      val request: UpdateTaxCalculationRequest = UpdateTaxCalculationRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002"
      )

      val expectedResponse: UpdateTaxCalculationReturn = UpdateTaxCalculationReturn(updated = true)

      when(mockService.updateTaxCalculation(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateTaxCalculation()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true
    }

    "returns 200 with partial tax calculation details" in new Setup {
      val request: UpdateTaxCalculationRequest = UpdateTaxCalculationRequest(
        stornId = "STORN77777",
        returnResourceRef = "100003",
        amountPaid = Some("3000"),
        taxDue = Some("3000"),
        calcTaxDue = Some("3000"),
        honestyDeclaration = Some("YES")
      )

      val expectedResponse: UpdateTaxCalculationReturn = UpdateTaxCalculationReturn(updated = true)

      when(mockService.updateTaxCalculation(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateTaxCalculation()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateTaxCalculation()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateTaxCalculation()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId" -> "STORN12345"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateTaxCalculation()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdateTaxCalculationRequest = UpdateTaxCalculationRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001"
      )

      when(mockService.updateTaxCalculation(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database timeout")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateTaxCalculation()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateTaxCalculation(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService: ReturnService = mock[ReturnService]
    val controller                 = new TaxCalculationReturnsController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/tax-calculation")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)
  }
}
