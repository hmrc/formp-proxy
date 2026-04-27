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
import uk.gov.hmrc.formpproxy.sdlt.controllers.returns.ResidencyReturnsController
import uk.gov.hmrc.formpproxy.sdlt.models.residency.*
import uk.gov.hmrc.formpproxy.sdlt.services.ReturnService

import scala.concurrent.{ExecutionContext, Future}

class ResidencyReturnsControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "ResidencyReturnsController createResidency" - {

    "returns 201 with created=true when service succeeds" in new Setup {
      when(mockService.createResidency(eqTo(testCreateRequest)))
        .thenReturn(Future.successful(CreateResidencyReturn(created = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(testCreateRequest))
      val res: Future[Result]       = controller.createResidency()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "created").as[Boolean] mustBe true

      verify(mockService).createResidency(eqTo(testCreateRequest))
      verifyNoMoreInteractions(mockService)
    }

    "returns 201 with all YES flags" in new Setup {
      val allYesRequest: CreateResidencyRequest = testCreateRequest.copy(
        residency = ResidencyPayload(
          isNonUkResidents = "YES",
          isCompany = "YES",
          isCrownRelief = "YES"
        )
      )

      when(mockService.createResidency(eqTo(allYesRequest)))
        .thenReturn(Future.successful(CreateResidencyReturn(created = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(allYesRequest))
      val res: Future[Result]       = controller.createResidency()(req)

      status(res) mustBe CREATED
      (contentAsJson(res) \ "created").as[Boolean] mustBe true

      verify(mockService).createResidency(eqTo(allYesRequest))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.createResidency()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "residency"         -> Json.obj(
          "isNonUkResidents" -> "NO",
          "isCompany"        -> "NO",
          "isCrownRelief"    -> "NO"
        )
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createResidency()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"   -> "STORN12345",
        "residency" -> Json.obj(
          "isNonUkResidents" -> "NO",
          "isCompany"        -> "NO",
          "isCrownRelief"    -> "NO"
        )
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createResidency()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when residency payload is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createResidency()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.createResidency(eqTo(testCreateRequest)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(testCreateRequest))
      val res: Future[Result]       = controller.createResidency()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).createResidency(eqTo(testCreateRequest))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ResidencyReturnsController updateResidency" - {

    "returns 200 with updated=true when service succeeds" in new Setup {
      when(mockService.updateResidency(eqTo(testUpdateRequest)))
        .thenReturn(Future.successful(UpdateResidencyReturn(updated = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(testUpdateRequest))
      val res: Future[Result]       = controller.updateResidency()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateResidency(eqTo(testUpdateRequest))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with all YES flags" in new Setup {
      val allYesRequest: UpdateResidencyRequest = testUpdateRequest.copy(
        residency = ResidencyPayload(
          isNonUkResidents = "YES",
          isCompany = "YES",
          isCrownRelief = "YES"
        )
      )

      when(mockService.updateResidency(eqTo(allYesRequest)))
        .thenReturn(Future.successful(UpdateResidencyReturn(updated = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(allYesRequest))
      val res: Future[Result]       = controller.updateResidency()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateResidency(eqTo(allYesRequest))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateResidency()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "residency"         -> Json.obj(
          "isNonUkResidents" -> "NO",
          "isCompany"        -> "NO",
          "isCrownRelief"    -> "NO"
        )
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateResidency()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when residency payload is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateResidency()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.updateResidency(eqTo(testUpdateRequest)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(testUpdateRequest))
      val res: Future[Result]       = controller.updateResidency()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateResidency(eqTo(testUpdateRequest))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ResidencyReturnsController deleteResidency" - {

    "returns 200 with deleted=true when service succeeds" in new Setup {
      when(mockService.deleteResidency(eqTo(testDeleteRequest)))
        .thenReturn(Future.successful(DeleteResidencyReturn(deleted = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(testDeleteRequest))
      val res: Future[Result]       = controller.deleteResidency()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "deleted").as[Boolean] mustBe true

      verify(mockService).deleteResidency(eqTo(testDeleteRequest))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.deleteResidency()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteResidency()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn" -> "STORN12345"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteResidency()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.deleteResidency(eqTo(testDeleteRequest)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(testDeleteRequest))
      val res: Future[Result]       = controller.deleteResidency()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).deleteResidency(eqTo(testDeleteRequest))
      verifyNoMoreInteractions(mockService)
    }

    "handles different storn formats" in new Setup {
      val request1 = testDeleteRequest.copy(storn = "STORN99999")
      val request2 = testDeleteRequest.copy(storn = "STORN-ABC-123")

      when(mockService.deleteResidency(eqTo(request1)))
        .thenReturn(Future.successful(DeleteResidencyReturn(deleted = true)))
      when(mockService.deleteResidency(eqTo(request2)))
        .thenReturn(Future.successful(DeleteResidencyReturn(deleted = true)))

      val res1 = controller.deleteResidency()(makeJsonRequest(Json.toJson(request1)))
      val res2 = controller.deleteResidency()(makeJsonRequest(Json.toJson(request2)))

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
    val controller                 = new ResidencyReturnsController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/residency")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)

    val testResidencyPayload: ResidencyPayload = ResidencyPayload(
      isNonUkResidents = "NO",
      isCompany = "NO",
      isCrownRelief = "NO"
    )

    val testCreateRequest: CreateResidencyRequest = CreateResidencyRequest(
      stornId = "STORN12345",
      returnResourceRef = "100001",
      residency = testResidencyPayload
    )

    val testUpdateRequest: UpdateResidencyRequest = UpdateResidencyRequest(
      stornId = "STORN12345",
      returnResourceRef = "100001",
      residency = testResidencyPayload
    )

    val testDeleteRequest: DeleteResidencyRequest = DeleteResidencyRequest(
      storn = "STORN12345",
      returnResourceRef = "100001"
    )
  }
}
