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
import uk.gov.hmrc.formpproxy.sdlt.controllers.returns.TransactionReturnsController
import uk.gov.hmrc.formpproxy.sdlt.services.ReturnService
import uk.gov.hmrc.formpproxy.sdlt.models.transaction.*

import scala.concurrent.{ExecutionContext, Future}

class TransactionReturnsControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "TransactionReturnsController updateTransaction" - {

    "returns 200 with updated=true when service succeeds" in new Setup {
      when(mockService.updateTransaction(eqTo(testUpdateRequest)))
        .thenReturn(Future.successful(UpdateTransactionReturn(updated = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(testUpdateRequest))
      val res: Future[Result]       = controller.updateTransaction()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateTransaction(eqTo(testUpdateRequest))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with all transaction fields populated" in new Setup {
      val fullRequest: UpdateTransactionRequest = testUpdateRequest.copy(
        transaction = testTransactionPayload.copy(
          claimingRelief = Some("Y"),
          reliefAmount = Some("5000"),
          reliefReason = Some("CHARITY"),
          reliefSchemeNumber = Some("CIS123456"),
          isLinked = Some("Y"),
          totalConsiderLinked = Some("500000"),
          totalConsider = Some("250000"),
          considerCash = Some("Y"),
          isLandExchanged = Some("Y"),
          exLandAddress1 = Some("Exchange Street"),
          exLandPostcode = Some("EX1 1CH"),
          isPartOfSaleOfBusiness = Some("Y"),
          totalConsiderationOfBusiness = Some("500000")
        )
      )

      when(mockService.updateTransaction(eqTo(fullRequest)))
        .thenReturn(Future.successful(UpdateTransactionReturn(updated = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(fullRequest))
      val res: Future[Result]       = controller.updateTransaction()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateTransaction(eqTo(fullRequest))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with minimal transaction payload (all None)" in new Setup {
      val minimalRequest: UpdateTransactionRequest = testUpdateRequest.copy(
        transaction = TransactionPayload()
      )

      when(mockService.updateTransaction(eqTo(minimalRequest)))
        .thenReturn(Future.successful(UpdateTransactionReturn(updated = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(minimalRequest))
      val res: Future[Result]       = controller.updateTransaction()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateTransaction(eqTo(minimalRequest))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateTransaction()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "transaction"       -> Json.obj()
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateTransaction()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"       -> "STORN12345",
        "transaction" -> Json.obj()
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateTransaction()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when transaction payload is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateTransaction()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.updateTransaction(eqTo(testUpdateRequest)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(testUpdateRequest))
      val res: Future[Result]       = controller.updateTransaction()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateTransaction(eqTo(testUpdateRequest))
      verifyNoMoreInteractions(mockService)
    }

    "handles different storn formats" in new Setup {
      val request1 = testUpdateRequest.copy(storn = "STORN99999")
      val request2 = testUpdateRequest.copy(storn = "STORN-ABC-123")

      when(mockService.updateTransaction(eqTo(request1)))
        .thenReturn(Future.successful(UpdateTransactionReturn(updated = true)))
      when(mockService.updateTransaction(eqTo(request2)))
        .thenReturn(Future.successful(UpdateTransactionReturn(updated = true)))

      val res1 = controller.updateTransaction()(makeJsonRequest(Json.toJson(request1)))
      val res2 = controller.updateTransaction()(makeJsonRequest(Json.toJson(request2)))

      status(res1) mustBe OK
      status(res2) mustBe OK
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService: ReturnService = mock[ReturnService]
    val controller                 = new TransactionReturnsController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/transaction")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)

    val testTransactionPayload: TransactionPayload = TransactionPayload(
      claimingRelief = Some("N"),
      isLinked = Some("N"),
      totalConsider = Some("250000"),
      considerCash = Some("Y"),
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
      agreedDeferPay = Some("N"),
      postTransactionRulingApplied = Some("N"),
      isPursuantToPreviousOption = Some("N"),
      restAffectInt = Some("N"),
      postTransactionRulingFollowed = Some("N"),
      isPartOfSaleOfBusiness = Some("N")
    )

    val testUpdateRequest: UpdateTransactionRequest = UpdateTransactionRequest(
      storn = "STORN12345",
      returnResourceRef = "100001",
      transaction = testTransactionPayload
    )
  }
}
