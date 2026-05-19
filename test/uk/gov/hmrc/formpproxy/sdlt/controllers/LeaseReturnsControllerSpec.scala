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
import uk.gov.hmrc.formpproxy.sdlt.controllers.returns.LeaseReturnsController
import uk.gov.hmrc.formpproxy.sdlt.models.lease.*
import uk.gov.hmrc.formpproxy.sdlt.services.ReturnService

import scala.concurrent.{ExecutionContext, Future}

class LeaseReturnsControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private val fullLeasePayload: LeasePayload = LeasePayload(
    isAnnualRentOver1000 = Some("YES"),
    contractEndDate = Some("2030-12-31"),
    contractStartDate = Some("2025-01-01"),
    leaseType = Some("COMMERCIAL"),
    netPresentValue = Some("50000"),
    totalPremiumPayable = Some("10000"),
    rentFreePeriod = Some("NO"),
    startingRent = Some("12000"),
    startingRentEndDate = Some("2026-01-01"),
    laterRentKnown = Some("YES"),
    vatAmount = Some("2400")
  )

  private val minimalLeasePayload: LeasePayload = LeasePayload(
    isAnnualRentOver1000 = None,
    contractEndDate = None,
    contractStartDate = None,
    leaseType = None,
    netPresentValue = None,
    totalPremiumPayable = None,
    rentFreePeriod = None,
    startingRent = None,
    startingRentEndDate = None,
    laterRentKnown = None,
    vatAmount = None
  )

  "LeaseReturnsController createLease" - {

    "returns 201 with lease response when service succeeds" in new Setup {
      val request: CreateLeaseRequest = CreateLeaseRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        lease = fullLeasePayload
      )

      val expectedResponse: CreateLeaseReturn = CreateLeaseReturn(created = true)

      when(mockService.createLease(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createLease()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "created").as[Boolean] mustBe true

      verify(mockService).createLease(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 201 with minimal lease details" in new Setup {
      val request: CreateLeaseRequest = CreateLeaseRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        lease = minimalLeasePayload
      )

      val expectedResponse: CreateLeaseReturn = CreateLeaseReturn(created = true)

      when(mockService.createLease(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createLease()(req)

      status(res) mustBe CREATED
      (contentAsJson(res) \ "created").as[Boolean] mustBe true
    }

    "returns 201 with residential lease details" in new Setup {
      val request: CreateLeaseRequest = CreateLeaseRequest(
        stornId = "STORN88888",
        returnResourceRef = "100003",
        lease = LeasePayload(
          isAnnualRentOver1000 = Some("NO"),
          contractEndDate = Some("2028-06-30"),
          contractStartDate = Some("2025-07-01"),
          leaseType = Some("RESIDENTIAL"),
          netPresentValue = Some("25000"),
          totalPremiumPayable = Some("5000"),
          rentFreePeriod = Some("YES"),
          startingRent = Some("8000"),
          startingRentEndDate = Some("2026-07-01"),
          laterRentKnown = Some("NO"),
          vatAmount = Some("1600")
        )
      )

      val expectedResponse: CreateLeaseReturn = CreateLeaseReturn(created = true)

      when(mockService.createLease(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createLease()(req)

      status(res) mustBe CREATED
      (contentAsJson(res) \ "created").as[Boolean] mustBe true
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.createLease()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "lease"             -> Json.toJson(fullLeasePayload)
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createLease()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId" -> "STORN12345",
        "lease"   -> Json.toJson(fullLeasePayload)
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createLease()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when lease payload is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createLease()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: CreateLeaseRequest = CreateLeaseRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        lease = fullLeasePayload
      )

      when(mockService.createLease(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createLease()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).createLease(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "LeaseReturnsController updateLease" - {

    "returns 200 with lease response when service succeeds" in new Setup {
      val request: UpdateLeaseRequest = UpdateLeaseRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        lease = fullLeasePayload
      )

      val expectedResponse: UpdateLeaseReturn = UpdateLeaseReturn(updated = true)

      when(mockService.updateLease(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateLease()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateLease(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with minimal update details" in new Setup {
      val request: UpdateLeaseRequest = UpdateLeaseRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        lease = minimalLeasePayload
      )

      val expectedResponse: UpdateLeaseReturn = UpdateLeaseReturn(updated = true)

      when(mockService.updateLease(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateLease()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true
    }

    "returns 200 with partial lease details" in new Setup {
      val request: UpdateLeaseRequest = UpdateLeaseRequest(
        stornId = "STORN77777",
        returnResourceRef = "100003",
        lease = LeasePayload(
          isAnnualRentOver1000 = Some("YES"),
          contractEndDate = Some("2029-12-31"),
          contractStartDate = Some("2025-01-01"),
          leaseType = Some("COMMERCIAL"),
          netPresentValue = None,
          totalPremiumPayable = None,
          rentFreePeriod = None,
          startingRent = Some("15000"),
          startingRentEndDate = None,
          laterRentKnown = None,
          vatAmount = None
        )
      )

      val expectedResponse: UpdateLeaseReturn = UpdateLeaseReturn(updated = true)

      when(mockService.updateLease(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateLease()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateLease()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "lease"             -> Json.toJson(fullLeasePayload)
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateLease()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when lease payload is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateLease()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdateLeaseRequest = UpdateLeaseRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        lease = fullLeasePayload
      )

      when(mockService.updateLease(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database timeout")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateLease()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateLease(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "LeaseReturnsController deleteLease" - {

    "returns 200 with lease response when service succeeds" in new Setup {
      val request: DeleteLeaseRequest = DeleteLeaseRequest(
        storn = "STORN12345",
        returnResourceRef = "100001"
      )

      val expectedResponse: DeleteLeaseReturn = DeleteLeaseReturn(deleted = true)

      when(mockService.deleteLease(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteLease()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "deleted").as[Boolean] mustBe true

      verify(mockService).deleteLease(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.deleteLease()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteLease()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn" -> "STORN12345"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteLease()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: DeleteLeaseRequest = DeleteLeaseRequest(
        storn = "STORN12345",
        returnResourceRef = "100001"
      )

      when(mockService.deleteLease(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteLease()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).deleteLease(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService: ReturnService = mock[ReturnService]
    val controller                 = new LeaseReturnsController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/lease")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)
  }
}
