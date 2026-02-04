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

package uk.gov.hmrc.formpproxy.sdlt.controllers.returns

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
import uk.gov.hmrc.formpproxy.sdlt.models.land.*
import uk.gov.hmrc.formpproxy.sdlt.services.ReturnService

import scala.concurrent.{ExecutionContext, Future}

class LandReturnsControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "LandReturnsController createLand" - {

    "returns 201 with land response when service succeeds" in new Setup {
      val request: CreateLandRequest = CreateLandRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        propertyType = "RESIDENTIAL",
        interestTransferredCreated = "FREEHOLD",
        houseNumber = Some("123"),
        addressLine1 = "Main Street",
        addressLine2 = Some("Apartment 4B"),
        addressLine3 = Some("City Center"),
        addressLine4 = Some("Greater London"),
        postcode = Some("SW1A 1AA"),
        landArea = Some("500"),
        areaUnit = Some("SQUARE_METERS"),
        localAuthorityNumber = Some("LA12345"),
        mineralRights = Some("YES"),
        nlpgUprn = Some("100012345678"),
        willSendPlansByPost = Some("NO"),
        titleNumber = Some("TN123456")
      )

      val expectedResponse: CreateLandReturn = CreateLandReturn(
        landResourceRef = "L100001",
        landId = "123"
      )

      when(mockService.createLand(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createLand()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "landResourceRef").as[String] mustBe "L100001"
      (contentAsJson(res) \ "landId").as[String] mustBe "123"

      verify(mockService).createLand(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 201 with non-residential land details" in new Setup {
      val request: CreateLandRequest = CreateLandRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        propertyType = "NON_RESIDENTIAL",
        interestTransferredCreated = "LEASEHOLD",
        houseNumber = None,
        addressLine1 = "Business Park",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = Some("EC1A 1BB"),
        landArea = Some("1000"),
        areaUnit = Some("SQUARE_FEET"),
        localAuthorityNumber = Some("LA99999"),
        mineralRights = Some("NO"),
        nlpgUprn = None,
        willSendPlansByPost = Some("YES"),
        titleNumber = Some("TN999888")
      )

      val expectedResponse: CreateLandReturn = CreateLandReturn(
        landResourceRef = "L100002",
        landId = "456"
      )

      when(mockService.createLand(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createLand()(req)

      status(res) mustBe CREATED
      (contentAsJson(res) \ "landResourceRef").as[String] mustBe "L100002"
    }

    "returns 201 with mixed property details" in new Setup {
      val request: CreateLandRequest = CreateLandRequest(
        stornId = "STORN88888",
        returnResourceRef = "100003",
        propertyType = "MIXED",
        interestTransferredCreated = "FREEHOLD",
        houseNumber = Some("99"),
        addressLine1 = "High Street",
        addressLine2 = Some("Town Centre"),
        addressLine3 = Some("Manchester"),
        addressLine4 = None,
        postcode = Some("M1 1AA"),
        landArea = Some("1000"),
        areaUnit = Some("SQUARE_FEET"),
        localAuthorityNumber = Some("LA99999"),
        mineralRights = Some("NO"),
        nlpgUprn = Some("100099887766"),
        willSendPlansByPost = Some("YES"),
        titleNumber = Some("TN999888")
      )

      val expectedResponse: CreateLandReturn = CreateLandReturn(
        landResourceRef = "L100003",
        landId = "789"
      )

      when(mockService.createLand(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createLand()(req)

      status(res) mustBe CREATED
      (contentAsJson(res) \ "landResourceRef").as[String] mustBe "L100003"
      (contentAsJson(res) \ "landId").as[String] mustBe "789"
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.createLand()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef"          -> "100001",
        "propertyType"               -> "RESIDENTIAL",
        "interestTransferredCreated" -> "FREEHOLD",
        "addressLine1"               -> "Main Street"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createLand()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"                    -> "STORN12345",
        "propertyType"               -> "RESIDENTIAL",
        "interestTransferredCreated" -> "FREEHOLD",
        "addressLine1"               -> "Main Street"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createLand()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when addressLine1 is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"                    -> "STORN12345",
        "returnResourceRef"          -> "100001",
        "propertyType"               -> "RESIDENTIAL",
        "interestTransferredCreated" -> "FREEHOLD"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createLand()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when propertyType is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"                    -> "STORN12345",
        "returnResourceRef"          -> "100001",
        "interestTransferredCreated" -> "FREEHOLD",
        "addressLine1"               -> "Main Street"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createLand()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when interestTransferredCreated is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "propertyType"      -> "RESIDENTIAL",
        "addressLine1"      -> "Main Street"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createLand()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: CreateLandRequest = CreateLandRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        propertyType = "RESIDENTIAL",
        interestTransferredCreated = "FREEHOLD",
        houseNumber = Some("123"),
        addressLine1 = "Main Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        landArea = None,
        areaUnit = None,
        localAuthorityNumber = None,
        mineralRights = None,
        nlpgUprn = None,
        willSendPlansByPost = None,
        titleNumber = None
      )

      when(mockService.createLand(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createLand()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).createLand(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "LandReturnsController updateLand" - {

    "returns 200 with land response when service succeeds" in new Setup {
      val request: UpdateLandRequest = UpdateLandRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        landResourceRef = "L100001",
        propertyType = "RESIDENTIAL",
        interestTransferredCreated = "FREEHOLD",
        houseNumber = Some("456"),
        addressLine1 = "Oak Avenue",
        addressLine2 = Some("Suite 10"),
        addressLine3 = Some("Updated City"),
        addressLine4 = None,
        postcode = Some("W1A 1AA"),
        landArea = Some("750"),
        areaUnit = Some("SQUARE_METERS"),
        localAuthorityNumber = Some("LA54321"),
        mineralRights = Some("NO"),
        nlpgUprn = Some("100087654321"),
        willSendPlansByPost = Some("YES"),
        titleNumber = Some("TN654321"),
        nextLandId = Some("100002")
      )

      val expectedResponse: UpdateLandReturn = UpdateLandReturn(
        updated = true
      )

      when(mockService.updateLand(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateLand()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateLand(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with minimal update details" in new Setup {
      val request: UpdateLandRequest = UpdateLandRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        landResourceRef = "L100002",
        propertyType = "NON_RESIDENTIAL",
        interestTransferredCreated = "LEASEHOLD",
        houseNumber = None,
        addressLine1 = "Updated Business Park",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = Some("EC2A 2BB"),
        landArea = None,
        areaUnit = None,
        localAuthorityNumber = None,
        mineralRights = None,
        nlpgUprn = None,
        willSendPlansByPost = None,
        titleNumber = None,
        nextLandId = None
      )

      val expectedResponse: UpdateLandReturn = UpdateLandReturn(
        updated = true
      )

      when(mockService.updateLand(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateLand()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true
    }

    "returns 200 with only required fields changed" in new Setup {
      val request: UpdateLandRequest = UpdateLandRequest(
        stornId = "STORN77777",
        returnResourceRef = "100003",
        landResourceRef = "L100003",
        propertyType = "MIXED",
        interestTransferredCreated = "FREEHOLD",
        houseNumber = Some("1"),
        addressLine1 = "New Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = Some("NE1 1AA"),
        landArea = None,
        areaUnit = None,
        localAuthorityNumber = None,
        mineralRights = None,
        nlpgUprn = None,
        willSendPlansByPost = None,
        titleNumber = None,
        nextLandId = None
      )

      val expectedResponse: UpdateLandReturn = UpdateLandReturn(
        updated = true
      )

      when(mockService.updateLand(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateLand()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateLand()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when landResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"                    -> "STORN12345",
        "returnResourceRef"          -> "100001",
        "propertyType"               -> "RESIDENTIAL",
        "interestTransferredCreated" -> "FREEHOLD",
        "addressLine1"               -> "Main Street"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateLand()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdateLandRequest = UpdateLandRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        landResourceRef = "L100001",
        propertyType = "RESIDENTIAL",
        interestTransferredCreated = "FREEHOLD",
        houseNumber = Some("123"),
        addressLine1 = "Main Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        landArea = None,
        areaUnit = None,
        localAuthorityNumber = None,
        mineralRights = None,
        nlpgUprn = None,
        willSendPlansByPost = None,
        titleNumber = None,
        nextLandId = None
      )

      when(mockService.updateLand(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database timeout")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateLand()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateLand(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "LandReturnsController deleteLand" - {

    "returns 200 with land response when service succeeds" in new Setup {
      val request: DeleteLandRequest = DeleteLandRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        landResourceRef = "L100001"
      )

      val expectedResponse: DeleteLandReturn = DeleteLandReturn(
        deleted = true
      )

      when(mockService.deleteLand(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteLand()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "deleted").as[Boolean] mustBe true

      verify(mockService).deleteLand(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.deleteLand()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "landResourceRef"   -> "L100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteLand()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when landResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteLand()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"           -> "STORN12345",
        "landResourceRef" -> "L100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteLand()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: DeleteLandRequest = DeleteLandRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        landResourceRef = "L100001"
      )

      when(mockService.deleteLand(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteLand()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).deleteLand(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService: ReturnService = mock[ReturnService]
    val controller                 = new LandReturnsController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/land")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)
  }
}
