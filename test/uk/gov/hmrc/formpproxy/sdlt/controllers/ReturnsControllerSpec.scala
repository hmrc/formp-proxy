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
import uk.gov.hmrc.formpproxy.sdlt.controllers.returns.ReturnsController
import uk.gov.hmrc.formpproxy.sdlt.models.*
import uk.gov.hmrc.formpproxy.sdlt.models.purchaser.{UpdateReturnRequest, UpdateReturnReturn}
import uk.gov.hmrc.formpproxy.sdlt.models.returns.ReturnSummary
import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepoDataHelper
import uk.gov.hmrc.formpproxy.sdlt.services.ReturnService
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.{ExecutionContext, Future}

class ReturnsControllerSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with SdltFormpRepoDataHelper {

  "ReturnsController createSDLTReturn" - {

    "returns 201 with returnResourceRef when service succeeds" in new Setup {
      val request: CreateReturnRequest = CreateReturnRequest(
        stornId = "STORN12345",
        purchaserIsCompany = "N",
        surNameOrCompanyName = "Smith",
        houseNumber = Some(42),
        addressLine1 = "High Street",
        addressLine2 = Some("Kensington"),
        addressLine3 = Some("London"),
        addressLine4 = None,
        postcode = Some("SW1A 1AA"),
        transactionType = "RESIDENTIAL"
      )

      when(mockService.createSDLTReturn(eqTo(request)))
        .thenReturn(Future.successful("100001"))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createSDLTReturn()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "returnResourceRef").as[String] mustBe "100001"

      verify(mockService).createSDLTReturn(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 201 for company purchaser" in new Setup {
      val request: CreateReturnRequest = CreateReturnRequest(
        stornId = "STORN99999",
        purchaserIsCompany = "YES",
        surNameOrCompanyName = "ABC Property Ltd",
        houseNumber = Some(100),
        addressLine1 = "Business Park",
        addressLine2 = Some("Westminster"),
        addressLine3 = Some("London"),
        addressLine4 = None,
        postcode = Some("W1B 2EL"),
        transactionType = "NON_RESIDENTIAL"
      )

      when(mockService.createSDLTReturn(eqTo(request)))
        .thenReturn(Future.successful("100002"))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createSDLTReturn()(req)

      status(res) mustBe CREATED
      (contentAsJson(res) \ "returnResourceRef").as[String] mustBe "100002"
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.createSDLTReturn()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "purchaserIsCompany"   -> "N",
        "surNameOrCompanyName" -> "Smith",
        "addressLine1"         -> "High Street",
        "transactionType"      -> "RESIDENTIAL"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createSDLTReturn()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when purchaserIsCompany is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"              -> "STORN12345",
        "surNameOrCompanyName" -> "Smith",
        "addressLine1"         -> "High Street",
        "transactionType"      -> "RESIDENTIAL"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createSDLTReturn()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when surNameOrCompanyName is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"            -> "STORN12345",
        "purchaserIsCompany" -> "N",
        "addressLine1"       -> "High Street",
        "transactionType"    -> "RESIDENTIAL"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createSDLTReturn()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when addressLine1 is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"              -> "STORN12345",
        "purchaserIsCompany"   -> "N",
        "surNameOrCompanyName" -> "Smith",
        "transactionType"      -> "RESIDENTIAL"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createSDLTReturn()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when transactionType is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"              -> "STORN12345",
        "purchaserIsCompany"   -> "N",
        "surNameOrCompanyName" -> "Smith",
        "addressLine1"         -> "High Street"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createSDLTReturn()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: CreateReturnRequest = CreateReturnRequest(
        stornId = "STORN12345",
        purchaserIsCompany = "N",
        surNameOrCompanyName = "Smith",
        houseNumber = Some(42),
        addressLine1 = "High Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        transactionType = "RESIDENTIAL"
      )

      when(mockService.createSDLTReturn(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createSDLTReturn()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).createSDLTReturn(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ReturnsController getSDLTReturn" - {

    "returns 200 with full return data when service succeeds" in new Setup {
      val getRequest: GetReturnByRefRequest = GetReturnByRefRequest(
        returnResourceRef = "100001",
        storn = "STORN12345"
      )

      when(mockService.getSDLTReturn(eqTo("100001"), eqTo("STORN12345")))
        .thenReturn(Future.successful(fullReturnData))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(getRequest))
      val res: Future[Result]       = controller.getSDLTReturn()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)

      val json: JsValue = contentAsJson(res)
      (json \ "returnResourceRef").asOpt[String] mustBe Some("100001")
      (json \ "stornId").asOpt[String] mustBe Some("STORN12345")
      (json \ "sdltOrganisation").asOpt[JsValue] must not be empty
      (json \ "returnInfo").asOpt[JsValue]       must not be empty
      (json \ "purchaser").asOpt[Seq[JsValue]]   must not be empty

      verify(mockService).getSDLTReturn(eqTo("100001"), eqTo("STORN12345"))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with minimal return data" in new Setup {
      val getRequest: GetReturnByRefRequest = GetReturnByRefRequest(
        returnResourceRef = "100002",
        storn = "STORN99999"
      )

      when(mockService.getSDLTReturn(eqTo("100002"), eqTo("STORN99999")))
        .thenReturn(Future.successful(minimalReturnData))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(getRequest))
      val res: Future[Result]       = controller.getSDLTReturn()(req)

      status(res) mustBe OK
      val json: JsValue = contentAsJson(res)
      (json \ "returnResourceRef").asOpt[String] mustBe Some("100002")
      (json \ "stornId").asOpt[String] mustBe Some("STORN99999")
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.getSDLTReturn()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj("storn" -> "STORN12345")

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.getSDLTReturn()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj("returnResourceRef" -> "100001")

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.getSDLTReturn()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val getRequest: GetReturnByRefRequest = GetReturnByRefRequest(
        returnResourceRef = "100001",
        storn = "STORN12345"
      )
      val err: UpstreamErrorResponse        = UpstreamErrorResponse("FORMP service unavailable", BAD_GATEWAY, BAD_GATEWAY)

      when(mockService.getSDLTReturn(eqTo("100001"), eqTo("STORN12345")))
        .thenReturn(Future.failed(err))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(getRequest))
      val res: Future[Result]       = controller.getSDLTReturn()(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("FORMP service unavailable")

      verify(mockService).getSDLTReturn(eqTo("100001"), eqTo("STORN12345"))
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val getRequest: GetReturnByRefRequest = GetReturnByRefRequest(
        returnResourceRef = "100001",
        storn = "STORN12345"
      )

      when(mockService.getSDLTReturn(eqTo("100001"), eqTo("STORN12345")))
        .thenReturn(Future.failed(new RuntimeException("Database timeout")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(getRequest))
      val res: Future[Result]       = controller.getSDLTReturn()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).getSDLTReturn(eqTo("100001"), eqTo("STORN12345"))
      verifyNoMoreInteractions(mockService)
    }

    "handles service returning different stornId" in new Setup {
      val getRequest: GetReturnByRefRequest = GetReturnByRefRequest(
        returnResourceRef = "100001",
        storn = "STORN12345"
      )

      val differentStornData: GetReturnRequest = fullReturnData.copy(stornId = Some("STORN99999"))

      when(mockService.getSDLTReturn(eqTo("100001"), eqTo("STORN12345")))
        .thenReturn(Future.successful(differentStornData))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(getRequest))
      val res: Future[Result]       = controller.getSDLTReturn()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "stornId").asOpt[String] mustBe Some("STORN99999")
    }
  }

  "ReturnsController getSDLTReturns" - {

    "return status code: OK" in new Setup {
      when(mockService.getSDLTReturns(eqTo(requestReturns)))
        .thenReturn(Future.successful(actualResponse))
      val req: FakeRequest[JsValue] = makeJsonReturnsRequest(Json.toJson(requestReturns))
      val res: Future[Result]       = controller.getSDLTReturns()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)

      val json: JsValue = contentAsJson(res)
      (json \ "returnSummaryCount").asOpt[Int] mustBe Some(2)
      (json \ "returnSummaryList").as[List[ReturnSummary]] mustBe expectedReturnsSummary

      verify(mockService).getSDLTReturns(eqTo(requestReturns))
      verifyNoMoreInteractions(mockService)
    }

    "return status code: BAD_REQUEST / invalid json in request" in new Setup {

      when(mockService.getSDLTReturns(eqTo(requestReturns)))
        .thenReturn(Future.successful(actualResponse))

      val req: FakeRequest[JsValue] = makeJsonReturnsRequest(Json.toJson(requestReturnsInvalid))
      val res: Future[Result]       = controller.getSDLTReturns()(req)

      status(res) mustBe BAD_REQUEST

      verify(mockService, times(0)).getSDLTReturns(eqTo(requestReturns))
      verifyNoMoreInteractions(mockService)
    }

    "return status code: BAD_GATEWAY :: Upstream error" in new Setup {

      val req: FakeRequest[JsValue] = makeJsonReturnsRequest(Json.toJson(requestReturns))

      val err: UpstreamErrorResponse = UpstreamErrorResponse("FORMP service unavailable", BAD_GATEWAY, BAD_GATEWAY)

      when(mockService.getSDLTReturns(eqTo(requestReturns)))
        .thenReturn(Future.failed(err))

      val res: Future[Result] = controller.getSDLTReturns()(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("FORMP service unavailable")
    }

    "return status code: INTERNAL_ERROR :: Unexpected error" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonReturnsRequest(Json.toJson(requestReturns))

      when(mockService.getSDLTReturns(eqTo(requestReturns)))
        .thenReturn(Future.failed(new RuntimeException("Database timeout")))

      val res: Future[Result] = controller.getSDLTReturns()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error::getSDLTReturns"
    }
  }

  "ReturnsController updateReturnVersion" - {

    "returns 200 with new version when service succeeds" in new Setup {
      val request: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        currentVersion = "1"
      )

      val expectedResponse: ReturnVersionUpdateReturn = ReturnVersionUpdateReturn(
        newVersion = 2
      )

      when(mockService.updateReturnVersion(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateReturnVersion()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "newVersion").as[Int] mustBe 2

      verify(mockService).updateReturnVersion(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with incremented version for higher version numbers" in new Setup {
      val request: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
        storn = "STORN99999",
        returnResourceRef = "100002",
        currentVersion = "5"
      )

      val expectedResponse: ReturnVersionUpdateReturn = ReturnVersionUpdateReturn(
        newVersion = 6
      )

      when(mockService.updateReturnVersion(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateReturnVersion()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "newVersion").as[Int] mustBe 6
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateReturnVersion()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "currentVersion"    -> "1"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnVersion()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"          -> "STORN12345",
        "currentVersion" -> "1"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnVersion()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when currentVersion is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnVersion()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        currentVersion = "1"
      )

      when(mockService.updateReturnVersion(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateReturnVersion()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateReturnVersion(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "handles version update from version 0" in new Setup {
      val request: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
        storn = "STORN11111",
        returnResourceRef = "100003",
        currentVersion = "0"
      )

      val expectedResponse: ReturnVersionUpdateReturn = ReturnVersionUpdateReturn(
        newVersion = 1
      )

      when(mockService.updateReturnVersion(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateReturnVersion()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "newVersion").as[Int] mustBe 1
    }

    "handles version update with different return" in new Setup {
      val request: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
        storn = "STORN77777",
        returnResourceRef = "999999",
        currentVersion = "10"
      )

      val expectedResponse: ReturnVersionUpdateReturn = ReturnVersionUpdateReturn(
        newVersion = 11
      )

      when(mockService.updateReturnVersion(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateReturnVersion()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "newVersion").as[Int] mustBe 11

      verify(mockService).updateReturnVersion(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ReturnsController updateReturnInfo" - {

    "returns 200 with updated=true when service succeeds" in new Setup {
      val request: UpdateReturnRequest = UpdateReturnRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        mainPurchaserId = "1",
        mainVendorId = "1",
        mainLandId = "1",
        irmarkGenerated = "IRMark123456",
        landCertForEachProp = "YES",
        declaration = "YES"
      )

      val expectedResponse: UpdateReturnReturn = UpdateReturnReturn(
        updated = true
      )

      when(mockService.updateReturn(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateReturn(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with different valid values" in new Setup {
      val request: UpdateReturnRequest = UpdateReturnRequest(
        storn = "STORN99999",
        returnResourceRef = "100002",
        mainPurchaserId = "5",
        mainVendorId = "3",
        mainLandId = "7",
        irmarkGenerated = "IRMark999999",
        landCertForEachProp = "N",
        declaration = "YES"
      )

      val expectedResponse: UpdateReturnReturn = UpdateReturnReturn(
        updated = true
      )

      when(mockService.updateReturn(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef"   -> "100001",
        "mainPurchaserId"     -> "1",
        "mainVendorId"        -> "1",
        "mainLandId"          -> "1",
        "irmarkGenerated"     -> "IRMark123456",
        "landCertForEachProp" -> "YES",
        "declaration"         -> "YES"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"               -> "STORN12345",
        "mainPurchaserId"     -> "1",
        "mainVendorId"        -> "1",
        "mainLandId"          -> "1",
        "irmarkGenerated"     -> "IRMark123456",
        "landCertForEachProp" -> "YES",
        "declaration"         -> "YES"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when mainPurchaserId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"               -> "STORN12345",
        "returnResourceRef"   -> "100001",
        "mainVendorId"        -> "1",
        "mainLandId"          -> "1",
        "irmarkGenerated"     -> "IRMark123456",
        "landCertForEachProp" -> "YES",
        "declaration"         -> "YES"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when mainVendorId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"               -> "STORN12345",
        "returnResourceRef"   -> "100001",
        "mainPurchaserId"     -> "1",
        "mainLandId"          -> "1",
        "irmarkGenerated"     -> "IRMark123456",
        "landCertForEachProp" -> "YES",
        "declaration"         -> "YES"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when mainLandId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"               -> "STORN12345",
        "returnResourceRef"   -> "100001",
        "mainPurchaserId"     -> "1",
        "mainVendorId"        -> "1",
        "irmarkGenerated"     -> "IRMark123456",
        "landCertForEachProp" -> "YES",
        "declaration"         -> "YES"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when irmarkGenerated is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"               -> "STORN12345",
        "returnResourceRef"   -> "100001",
        "mainPurchaserId"     -> "1",
        "mainVendorId"        -> "1",
        "mainLandId"          -> "1",
        "landCertForEachProp" -> "YES",
        "declaration"         -> "YES"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when landCertForEachProp is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001",
        "mainPurchaserId"   -> "1",
        "mainVendorId"      -> "1",
        "mainLandId"        -> "1",
        "irmarkGenerated"   -> "IRMark123456",
        "declaration"       -> "YES"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when declaration is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"               -> "STORN12345",
        "returnResourceRef"   -> "100001",
        "mainPurchaserId"     -> "1",
        "mainVendorId"        -> "1",
        "mainLandId"          -> "1",
        "irmarkGenerated"     -> "IRMark123456",
        "landCertForEachProp" -> "YES"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdateReturnRequest = UpdateReturnRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        mainPurchaserId = "1",
        mainVendorId = "1",
        mainLandId = "1",
        irmarkGenerated = "IRMark123456",
        landCertForEachProp = "YES",
        declaration = "YES"
      )

      when(mockService.updateReturn(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateReturn(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "handles update with N values for boolean fields" in new Setup {
      val request: UpdateReturnRequest = UpdateReturnRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        mainPurchaserId = "1",
        mainVendorId = "1",
        mainLandId = "1",
        irmarkGenerated = "IRMark123456",
        landCertForEachProp = "N",
        declaration = "N"
      )

      val expectedResponse: UpdateReturnReturn = UpdateReturnReturn(
        updated = true
      )

      when(mockService.updateReturn(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateReturnInfo()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateReturn(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService: ReturnService = mock[ReturnService]
    val controller                 = new ReturnsController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/return")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)

    def makeJsonReturnsRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/returns")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)

    val fullReturnData: GetReturnRequest = GetReturnRequest(
      stornId = Some("STORN12345"),
      returnResourceRef = Some("100001"),
      sdltOrganisation = Some(
        SdltOrganisation(
          isReturnUser = Some("YES"),
          doNotDisplayWelcomePage = Some("N"),
          storn = Some("STORN12345"),
          version = Some("1")
        )
      ),
      returnInfo = Some(
        ReturnInfo(
          returnID = Some("100001"),
          storn = Some("STORN12345"),
          purchaserCounter = Some("1"),
          vendorCounter = Some("1"),
          landCounter = Some("1"),
          purgeDate = None,
          version = Some("1"),
          mainPurchaserID = Some("1"),
          mainVendorID = Some("1"),
          mainLandID = Some("1"),
          IRMarkGenerated = Some("N"),
          landCertForEachProp = Some("N"),
          returnResourceRef = Some("100001"),
          declaration = Some("YES"),
          status = Some("STARTED")
        )
      ),
      purchaser = Some(
        Seq(
          Purchaser(
            purchaserID = Some("1"),
            returnID = Some("100001"),
            isCompany = Some("N"),
            isTrustee = Some("N"),
            isConnectedToVendor = Some("N"),
            isRepresentedByAgent = Some("N"),
            title = Some("Mr"),
            surname = Some("Smith"),
            forename1 = Some("John"),
            forename2 = None,
            companyName = None,
            houseNumber = Some("42"),
            address1 = Some("High Street"),
            address2 = Some("Kensington"),
            address3 = Some("London"),
            address4 = None,
            postcode = Some("SW1A 1AA"),
            phone = Some("01234567890"),
            nino = Some("AB123456C"),
            purchaserResourceRef = Some("1"),
            nextPurchaserID = None,
            lMigrated = Some("N"),
            createDate = Some("2025-01-01"),
            lastUpdateDate = Some("2025-01-01"),
            isUkCompany = Some("YES"),
            hasNino = Some("YES"),
            dateOfBirth = Some("1980-01-01"),
            registrationNumber = None,
            placeOfRegistration = None
          )
        )
      ),
      companyDetails = None,
      vendor = Some(
        Seq(
          Vendor(
            vendorID = Some("1"),
            returnID = Some("100001"),
            title = Some("Mrs"),
            forename1 = Some("Jane"),
            forename2 = None,
            name = Some("Vendor"),
            houseNumber = Some("456"),
            address1 = Some("Vendor Street"),
            address2 = Some("Vendor Town"),
            address3 = None,
            address4 = None,
            postcode = Some("VE1 2ND"),
            isRepresentedByAgent = Some("N"),
            vendorResourceRef = Some("1"),
            nextVendorID = None
          )
        )
      ),
      land = Some(
        Seq(
          Land(
            landID = Some("1"),
            returnID = Some("100001"),
            propertyType = Some("RESIDENTIAL"),
            interestCreatedTransferred = Some("FREEHOLD"),
            houseNumber = Some("42"),
            address1 = Some("High Street"),
            address2 = Some("Kensington"),
            address3 = Some("London"),
            address4 = None,
            postcode = Some("SW1A 1AA"),
            landArea = Some("100"),
            areaUnit = Some("SQMETRE"),
            localAuthorityNumber = Some("1234"),
            mineralRights = Some("N"),
            NLPGUPRN = Some("123456789012"),
            willSendPlanByPost = Some("N"),
            titleNumber = Some("TN123456"),
            landResourceRef = Some("1"),
            nextLandID = None,
            DARPostcode = None
          )
        )
      ),
      transaction = Some(
        Transaction(
          transactionID = Some("1"),
          returnID = Some("100001"),
          claimingRelief = Some("N"),
          reliefAmount = None,
          reliefReason = None,
          reliefSchemeNumber = None,
          isLinked = Some("N"),
          totalConsiderationLinked = None,
          totalConsideration = Some(BigDecimal("250000.00")),
          considerationBuild = None,
          considerationCash = Some(BigDecimal("250000.00")),
          considerationContingent = None,
          considerationDebt = None,
          considerationEmploy = None,
          considerationOther = None,
          considerationLand = None,
          considerationServices = None,
          considerationSharesQTD = None,
          considerationSharesUNQTD = None,
          considerationVAT = None,
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
          isDependantOnFutureEvent = Some("N"),
          transactionDescription = Some("RESIDENTIAL"),
          newTransactionDescription = None,
          effectiveDate = Some("2025-02-01"),
          isLandExchanged = Some("N"),
          exchangedLandHouseNumber = None,
          exchangedLandAddress1 = None,
          exchangedLandAddress2 = None,
          exchangedLandAddress3 = None,
          exchangedLandAddress4 = None,
          exchangedLandPostcode = None,
          agreedToDeferPayment = Some("N"),
          postTransRulingApplied = Some("N"),
          isPursuantToPreviousOption = Some("N"),
          restrictionsAffectInterest = Some("N"),
          restrictionDetails = None,
          postTransRulingFollowed = Some("N"),
          isPartOfSaleOfBusiness = Some("N"),
          totalConsiderationBusiness = None
        )
      ),
      returnAgent = None,
      agent = None,
      lease = None,
      taxCalculation = Some(
        TaxCalculation(
          taxCalculationID = Some("1"),
          returnID = Some("100001"),
          amountPaid = Some("0.00"),
          includesPenalty = Some("N"),
          taxDue = Some("2500.00"),
          taxDuePremium = None,
          taxDueNPV = None,
          calcPenaltyDue = Some("0.00"),
          calcTaxDue = Some("2500.00"),
          calcTaxRate1 = None,
          calcTaxRate2 = None,
          calcTotalTaxPenaltyDue = None,
          calcTotalNPVTax = None,
          calcTotalPremiumTax = None,
          honestyDeclaration = None
        )
      ),
      submission = Some(
        Submission(
          submissionID = Some("1"),
          returnID = Some("100001"),
          storn = Some("STORN12345"),
          submissionStatus = Some("STARTED"),
          govtalkMessageClass = None,
          UTRN = None,
          irmarkReceived = None,
          submissionReceipt = None,
          govtalkErrorCode = None,
          govtalkErrorType = None,
          govtalkErrorMessage = None,
          numPolls = Some("0"),
          createDate = Some("2025-01-01T10:00:00"),
          lastUpdateDate = Some("2025-01-01T10:00:00"),
          acceptedDate = None,
          submittedDate = None,
          email = Some("test@example.com"),
          submissionRequestDate = None,
          irmarkSent = None
        )
      ),
      submissionErrorDetails = None,
      residency = Some(
        Residency(
          residencyID = Some("1"),
          isNonUkResidents = Some("N"),
          isCloseCompany = Some("N"),
          isCrownRelief = Some("N")
        )
      )
    )

    val minimalReturnData: GetReturnRequest = GetReturnRequest(
      stornId = Some("STORN99999"),
      returnResourceRef = Some("100002"),
      sdltOrganisation = None,
      returnInfo = None,
      purchaser = None,
      companyDetails = None,
      vendor = None,
      land = None,
      transaction = None,
      returnAgent = None,
      agent = None,
      lease = None,
      taxCalculation = None,
      submission = None,
      submissionErrorDetails = None,
      residency = None
    )
  }
}
