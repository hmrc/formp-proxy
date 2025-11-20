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
import uk.gov.hmrc.formpproxy.sdlt.controllers.returns.VendorReturnsController
import uk.gov.hmrc.formpproxy.sdlt.models.vendor.*
import uk.gov.hmrc.formpproxy.sdlt.services.ReturnService
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.{ExecutionContext, Future}

class VendorReturnsControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "VendorReturnsController createVendor" - {

    "returns 201 with vendor response when service succeeds" in new Setup {
      val request: CreateVendorRequest = CreateVendorRequest(
        returnResourceRef = "100001",
        stornId = "STORN12345",
        title = Some("Mr"),
        forename1 = Some("John"),
        forename2 = Some("James"),
        name = "Smith",
        houseNumber = Some("123"),
        addressLine1 = "Main Street",
        addressLine2 = Some("Apartment 4B"),
        addressLine3 = Some("City Center"),
        addressLine4 = Some("Greater London"),
        postcode = Some("SW1A 1AA"),
        isRepresentedByAgent = "NO"
      )

      val expectedResponse: CreateVendorReturn = CreateVendorReturn(
        vendorResourceRef = "V100001",
        vendorId = "123"
      )

      when(mockService.createVendor(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createVendor()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "vendorResourceRef").as[String] mustBe "V100001"

      verify(mockService).createVendor(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 201 with minimal vendor details" in new Setup {
      val request: CreateVendorRequest = CreateVendorRequest(
        returnResourceRef = "100002",
        stornId = "STORN99999",
        title = None,
        forename1 = None,
        forename2 = None,
        name = "Company Vendor Ltd",
        houseNumber = None,
        addressLine1 = "Business Park",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = Some("EC1A 1BB"),
        isRepresentedByAgent = "Y"
      )

      val expectedResponse: CreateVendorReturn = CreateVendorReturn(
        vendorResourceRef = "V100002",
        vendorId = "123"
      )

      when(mockService.createVendor(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createVendor()(req)

      status(res) mustBe CREATED
      (contentAsJson(res) \ "vendorResourceRef").as[String] mustBe "V100002"
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.createVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"              -> "STORN12345",
        "name"                 -> "Smith",
        "address1"             -> "Main Street",
        "isRepresentedByAgent" -> "N"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef"    -> "100001",
        "name"                 -> "Smith",
        "address1"             -> "Main Street",
        "isRepresentedByAgent" -> "N"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when address1 is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef"    -> "100001",
        "stornId"              -> "STORN12345",
        "name"                 -> "Smith",
        "isRepresentedByAgent" -> "N"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when isRepresentedByAgent is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "stornId"           -> "STORN12345",
        "name"              -> "Smith",
        "address1"          -> "Main Street"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: CreateVendorRequest = CreateVendorRequest(
        returnResourceRef = "100001",
        stornId = "STORN12345",
        title = Some("Mr"),
        forename1 = Some("John"),
        forename2 = None,
        name = "Smith",
        houseNumber = Some("123"),
        addressLine1 = "Main Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        isRepresentedByAgent = "N"
      )

      when(mockService.createVendor(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createVendor()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).createVendor(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "VendorReturnsController updateVendor" - {

    "returns 200 with vendor response when service succeeds" in new Setup {
      val request: UpdateVendorRequest = UpdateVendorRequest(
        returnResourceRef = "100001",
        stornId = "STORN12345",
        vendorResourceRef = "V100001",
        title = Some("Mrs"),
        forename1 = Some("Jane"),
        forename2 = None,
        name = "Doe",
        houseNumber = Some("456"),
        addressLine1 = "Oak Avenue",
        addressLine2 = Some("Suite 10"),
        addressLine3 = Some("Downtown"),
        addressLine4 = None,
        postcode = Some("W1A 1AA"),
        isRepresentedByAgent = "Y"
      )

      val expectedResponse: UpdateVendorReturn = UpdateVendorReturn(
        updated = true
      )

      when(mockService.updateVendor(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateVendor()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateVendor(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with minimal update details" in new Setup {
      val request: UpdateVendorRequest = UpdateVendorRequest(
        returnResourceRef = "100002",
        stornId = "STORN99999",
        vendorResourceRef = "V100002",
        title = None,
        forename1 = None,
        forename2 = None,
        name = "Updated Vendor Ltd",
        houseNumber = None,
        addressLine1 = "New Business Park",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = Some("EC2A 2BB"),
        isRepresentedByAgent = "N"
      )

      val expectedResponse: UpdateVendorReturn = UpdateVendorReturn(
        updated = true
      )

      when(mockService.updateVendor(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateVendor()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"              -> "STORN12345",
        "vendorResourceRef"    -> "V100001",
        "name"                 -> "Smith",
        "address1"             -> "Main Street",
        "isRepresentedByAgent" -> "N"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef"    -> "100001",
        "vendorResourceRef"    -> "V100001",
        "name"                 -> "Smith",
        "address1"             -> "Main Street",
        "isRepresentedByAgent" -> "N"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when vendorResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef"    -> "100001",
        "stornId"              -> "STORN12345",
        "name"                 -> "Smith",
        "address1"             -> "Main Street",
        "isRepresentedByAgent" -> "N"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when address1 is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef"    -> "100001",
        "stornId"              -> "STORN12345",
        "vendorResourceRef"    -> "V100001",
        "name"                 -> "Smith",
        "isRepresentedByAgent" -> "N"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when isRepresentedByAgent is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "stornId"           -> "STORN12345",
        "vendorResourceRef" -> "V100001",
        "name"              -> "Smith",
        "address1"          -> "Main Street"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdateVendorRequest = UpdateVendorRequest(
        returnResourceRef = "100001",
        stornId = "STORN12345",
        vendorResourceRef = "V100001",
        title = Some("Mr"),
        forename1 = Some("John"),
        forename2 = None,
        name = "Smith",
        houseNumber = Some("123"),
        addressLine1 = "Main Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        isRepresentedByAgent = "N"
      )

      when(mockService.updateVendor(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database timeout")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateVendor()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateVendor(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "VendorReturnsController deleteVendor" - {

    "returns 200 with vendor response when service succeeds" in new Setup {
      val request: DeleteVendorRequest = DeleteVendorRequest(
        returnResourceRef = "100001",
        storn = "STORN12345",
        vendorResourceRef = "V100001"
      )

      val expectedResponse: DeleteVendorReturn = DeleteVendorReturn(
        deleted = true
      )

      when(mockService.deleteVendor(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteVendor()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "deleted").as[Boolean] mustBe true

      verify(mockService).deleteVendor(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 for different vendor" in new Setup {
      val request: DeleteVendorRequest = DeleteVendorRequest(
        returnResourceRef = "100002",
        storn = "STORN99999",
        vendorResourceRef = "V999999"
      )

      val expectedResponse: DeleteVendorReturn = DeleteVendorReturn(
        deleted = true
      )

      when(mockService.deleteVendor(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteVendor()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "deleted").as[Boolean] mustBe true
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.deleteVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "vendorResourceRef" -> "V100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "vendorResourceRef" -> "V100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when vendorResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "stornId"           -> "STORN12345"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteVendor()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: DeleteVendorRequest = DeleteVendorRequest(
        returnResourceRef = "100001",
        storn = "STORN12345",
        vendorResourceRef = "V100001"
      )

      when(mockService.deleteVendor(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteVendor()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).deleteVendor(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService: ReturnService = mock[ReturnService]
    val controller                 = new VendorReturnsController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/vendor")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)
  }
}
