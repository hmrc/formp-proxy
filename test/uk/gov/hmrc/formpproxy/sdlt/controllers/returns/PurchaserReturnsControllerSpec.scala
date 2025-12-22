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
import uk.gov.hmrc.formpproxy.sdlt.models.purchaser.*
import uk.gov.hmrc.formpproxy.sdlt.models.vendor.*
import uk.gov.hmrc.formpproxy.sdlt.services.ReturnService

import scala.concurrent.{ExecutionContext, Future}

class PurchaserReturnsControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "PurchaserReturnsController createPurchaser" - {

    "returns 201 with purchaser response when service succeeds" in new Setup {
      val request: CreatePurchaserRequest = CreatePurchaserRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        isCompany = "N",
        isTrustee = "N",
        isConnectedToVendor = "N",
        isRepresentedByAgent = "N",
        title = Some("Mr"),
        surname = Some("Smith"),
        forename1 = Some("John"),
        forename2 = Some("James"),
        companyName = None,
        houseNumber = Some("123"),
        address1 = "Main Street",
        address2 = Some("Apartment 4B"),
        address3 = Some("City Center"),
        address4 = Some("Greater London"),
        postcode = Some("SW1A 1AA"),
        phone = Some("07777123456"),
        nino = Some("AB123456C"),
        isUkCompany = None,
        hasNino = Some("Y"),
        dateOfBirth = Some("1980-01-15"),
        registrationNumber = None,
        placeOfRegistration = None
      )

      val expectedResponse: CreatePurchaserReturn = CreatePurchaserReturn(
        purchaserResourceRef = "P100001",
        purchaserId = "123"
      )

      when(mockService.createPurchaser(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createPurchaser()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "purchaserResourceRef").as[String] mustBe "P100001"
      (contentAsJson(res) \ "purchaserId").as[String] mustBe "123"

      verify(mockService).createPurchaser(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 201 with company purchaser details" in new Setup {
      val request: CreatePurchaserRequest = CreatePurchaserRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        isCompany = "Y",
        isTrustee = "N",
        isConnectedToVendor = "N",
        isRepresentedByAgent = "N",
        title = None,
        surname = None,
        forename1 = None,
        forename2 = None,
        companyName = Some("Tech Corp Ltd"),
        houseNumber = None,
        address1 = "Business Park",
        address2 = None,
        address3 = None,
        address4 = None,
        postcode = Some("EC1A 1BB"),
        phone = Some("02012345678"),
        nino = None,
        isUkCompany = Some("Y"),
        hasNino = Some("N"),
        dateOfBirth = None,
        registrationNumber = Some("12345678"),
        placeOfRegistration = None
      )

      val expectedResponse: CreatePurchaserReturn = CreatePurchaserReturn(
        purchaserResourceRef = "P100002",
        purchaserId = "456"
      )

      when(mockService.createPurchaser(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createPurchaser()(req)

      status(res) mustBe CREATED
      (contentAsJson(res) \ "purchaserResourceRef").as[String] mustBe "P100002"
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.createPurchaser()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef"    -> "100001",
        "isCompany"            -> "N",
        "isTrustee"            -> "N",
        "isConnectedToVendor"  -> "N",
        "isRepresentedByAgent" -> "N",
        "address1"             -> "Main Street"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createPurchaser()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"              -> "STORN12345",
        "isCompany"            -> "N",
        "isTrustee"            -> "N",
        "isConnectedToVendor"  -> "N",
        "isRepresentedByAgent" -> "N",
        "address1"             -> "Main Street"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createPurchaser()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when address1 is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "isCompany"            -> "N",
        "isTrustee"            -> "N",
        "isConnectedToVendor"  -> "N",
        "isRepresentedByAgent" -> "N"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createPurchaser()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: CreatePurchaserRequest = CreatePurchaserRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        isCompany = "N",
        isTrustee = "N",
        isConnectedToVendor = "N",
        isRepresentedByAgent = "N",
        title = Some("Mr"),
        surname = Some("Smith"),
        forename1 = Some("John"),
        forename2 = None,
        companyName = None,
        houseNumber = Some("123"),
        address1 = "Main Street",
        address2 = None,
        address3 = None,
        address4 = None,
        postcode = None,
        phone = None,
        nino = None,
        isUkCompany = None,
        hasNino = Some("N"),
        dateOfBirth = None,
        registrationNumber = None,
        placeOfRegistration = None
      )

      when(mockService.createPurchaser(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createPurchaser()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).createPurchaser(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "PurchaserReturnsController updatePurchaser" - {

    "returns 200 with purchaser response when service succeeds" in new Setup {
      val request: UpdatePurchaserRequest = UpdatePurchaserRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        isCompany = "N",
        isTrustee = "N",
        isConnectedToVendor = "Y",
        isRepresentedByAgent = "Y",
        title = Some("Mrs"),
        surname = Some("Doe"),
        forename1 = Some("Jane"),
        forename2 = None,
        companyName = None,
        houseNumber = Some("456"),
        address1 = "Oak Avenue",
        address2 = Some("Suite 10"),
        address3 = Some("Downtown"),
        address4 = None,
        postcode = Some("W1A 1AA"),
        phone = Some("07777654321"),
        nino = Some("CD987654B"),
        nextPurchaserId = None,
        isUkCompany = None,
        hasNino = Some("Y"),
        dateOfBirth = Some("1985-05-20"),
        registrationNumber = None,
        placeOfRegistration = None
      )

      val expectedResponse: UpdatePurchaserReturn = UpdatePurchaserReturn(
        updated = true
      )

      when(mockService.updatePurchaser(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updatePurchaser()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updatePurchaser(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with minimal update details" in new Setup {
      val request: UpdatePurchaserRequest = UpdatePurchaserRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        purchaserResourceRef = "P100002",
        isCompany = "Y",
        isTrustee = "N",
        isConnectedToVendor = "N",
        isRepresentedByAgent = "N",
        title = None,
        surname = None,
        forename1 = None,
        forename2 = None,
        companyName = Some("Updated Corp Ltd"),
        houseNumber = None,
        address1 = "New Business Park",
        address2 = None,
        address3 = None,
        address4 = None,
        postcode = Some("EC2A 2BB"),
        phone = None,
        nino = None,
        nextPurchaserId = None,
        isUkCompany = Some("Y"),
        hasNino = Some("N"),
        dateOfBirth = None,
        registrationNumber = Some("87654321"),
        placeOfRegistration = None
      )

      val expectedResponse: UpdatePurchaserReturn = UpdatePurchaserReturn(
        updated = true
      )

      when(mockService.updatePurchaser(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updatePurchaser()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updatePurchaser()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when purchaserResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "isCompany"            -> "N",
        "isTrustee"            -> "N",
        "isConnectedToVendor"  -> "N",
        "isRepresentedByAgent" -> "N",
        "address1"             -> "Main Street"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updatePurchaser()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdatePurchaserRequest = UpdatePurchaserRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        isCompany = "N",
        isTrustee = "N",
        isConnectedToVendor = "N",
        isRepresentedByAgent = "N",
        title = Some("Mr"),
        surname = Some("Smith"),
        forename1 = Some("John"),
        forename2 = None,
        companyName = None,
        houseNumber = Some("123"),
        address1 = "Main Street",
        address2 = None,
        address3 = None,
        address4 = None,
        postcode = None,
        phone = None,
        nino = None,
        nextPurchaserId = None,
        isUkCompany = None,
        hasNino = Some("N"),
        dateOfBirth = None,
        registrationNumber = None,
        placeOfRegistration = None
      )

      when(mockService.updatePurchaser(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database timeout")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updatePurchaser()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updatePurchaser(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "PurchaserReturnsController deletePurchaser" - {

    "returns 200 with purchaser response when service succeeds" in new Setup {
      val request: DeletePurchaserRequest = DeletePurchaserRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001"
      )

      val expectedResponse: DeletePurchaserReturn = DeletePurchaserReturn(
        deleted = true
      )

      when(mockService.deletePurchaser(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deletePurchaser()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "deleted").as[Boolean] mustBe true

      verify(mockService).deletePurchaser(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.deletePurchaser()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef"    -> "100001",
        "purchaserResourceRef" -> "P100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deletePurchaser()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: DeletePurchaserRequest = DeletePurchaserRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001"
      )

      when(mockService.deletePurchaser(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deletePurchaser()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).deletePurchaser(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "PurchaserReturnsController createCompanyDetails" - {

    "returns 201 with company details response when service succeeds" in new Setup {
      val request: CreateCompanyDetailsRequest = CreateCompanyDetailsRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        utr = Some("1234567890"),
        vatReference = Some("GB123456789"),
        compTypeBank = Some("N"),
        compTypeBuilder = Some("N"),
        compTypeBuildsoc = Some("N"),
        compTypeCentgov = Some("N"),
        compTypeIndividual = Some("N"),
        compTypeInsurance = Some("N"),
        compTypeLocalauth = Some("N"),
        compTypeOcharity = Some("N"),
        compTypeOcompany = Some("Y"),
        compTypeOfinancial = Some("N"),
        compTypePartship = Some("N"),
        compTypeProperty = Some("N"),
        compTypePubliccorp = Some("N"),
        compTypeSoletrader = Some("N"),
        compTypePenfund = Some("N")
      )

      val expectedResponse: CreateCompanyDetailsReturn = CreateCompanyDetailsReturn(
        companyDetailsId = "789"
      )

      when(mockService.createCompanyDetails(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createCompanyDetails()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "companyDetailsId").as[String] mustBe "789"

      verify(mockService).createCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.createCompanyDetails()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: CreateCompanyDetailsRequest = CreateCompanyDetailsRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        utr = Some("1234567890"),
        vatReference = None,
        compTypeBank = None,
        compTypeBuilder = None,
        compTypeBuildsoc = None,
        compTypeCentgov = None,
        compTypeIndividual = None,
        compTypeInsurance = None,
        compTypeLocalauth = None,
        compTypeOcharity = None,
        compTypeOcompany = Some("Y"),
        compTypeOfinancial = None,
        compTypePartship = None,
        compTypeProperty = None,
        compTypePubliccorp = None,
        compTypeSoletrader = None,
        compTypePenfund = None
      )

      when(mockService.createCompanyDetails(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createCompanyDetails()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).createCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "PurchaserReturnsController updateCompanyDetails" - {

    "returns 200 with company details response when service succeeds" in new Setup {
      val request: UpdateCompanyDetailsRequest = UpdateCompanyDetailsRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        utr = Some("9876543210"),
        vatReference = Some("GB987654321"),
        compTypeBank = Some("N"),
        compTypeBuilder = Some("Y"),
        compTypeBuildsoc = Some("N"),
        compTypeCentgov = Some("N"),
        compTypeIndividual = Some("N"),
        compTypeInsurance = Some("N"),
        compTypeLocalauth = Some("N"),
        compTypeOcharity = Some("N"),
        compTypeOcompany = Some("Y"),
        compTypeOfinancial = Some("N"),
        compTypePartship = Some("N"),
        compTypeProperty = Some("Y"),
        compTypePubliccorp = Some("N"),
        compTypeSoletrader = Some("N"),
        compTypePenfund = Some("N")
      )

      val expectedResponse: UpdateCompanyDetailsReturn = UpdateCompanyDetailsReturn(
        updated = true
      )

      when(mockService.updateCompanyDetails(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateCompanyDetails()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateCompanyDetails()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdateCompanyDetailsRequest = UpdateCompanyDetailsRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        utr = Some("1234567890"),
        vatReference = None,
        compTypeBank = None,
        compTypeBuilder = None,
        compTypeBuildsoc = None,
        compTypeCentgov = None,
        compTypeIndividual = None,
        compTypeInsurance = None,
        compTypeLocalauth = None,
        compTypeOcharity = None,
        compTypeOcompany = Some("Y"),
        compTypeOfinancial = None,
        compTypePartship = None,
        compTypeProperty = None,
        compTypePubliccorp = None,
        compTypeSoletrader = None,
        compTypePenfund = None
      )

      when(mockService.updateCompanyDetails(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database timeout")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateCompanyDetails()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "PurchaserReturnsController deleteCompanyDetails" - {

    "returns 200 with company details response when service succeeds" in new Setup {
      val request: DeleteCompanyDetailsRequest = DeleteCompanyDetailsRequest(
        storn = "STORN12345",
        returnResourceRef = "100001"
      )

      val expectedResponse: DeleteCompanyDetailsReturn = DeleteCompanyDetailsReturn(
        deleted = true
      )

      when(mockService.deleteCompanyDetails(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteCompanyDetails()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "deleted").as[Boolean] mustBe true

      verify(mockService).deleteCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.deleteCompanyDetails()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: DeleteCompanyDetailsRequest = DeleteCompanyDetailsRequest(
        storn = "STORN12345",
        returnResourceRef = "100001"
      )

      when(mockService.deleteCompanyDetails(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteCompanyDetails()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).deleteCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService: ReturnService = mock[ReturnService]
    val controller                 = new PurchaserReturnsController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/purchaser")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)
  }
}
