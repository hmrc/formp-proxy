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
import uk.gov.hmrc.formpproxy.sdlt.controllers.returns.ReturnAgentReturnsController
import uk.gov.hmrc.formpproxy.sdlt.models.agent.*
import uk.gov.hmrc.formpproxy.sdlt.services.ReturnService

import scala.concurrent.{ExecutionContext, Future}

class ReturnAgentReturnsControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "ReturnAgentReturnsController createReturnAgent" - {

    "returns 201 with returnAgentID when service succeeds" in new Setup {
      val request: CreateReturnAgentRequest = CreateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR",
        name = "Smith & Partners LLP",
        houseNumber = Some("10"),
        addressLine1 = "Legal District",
        addressLine2 = Some("Business Quarter"),
        addressLine3 = Some("Manchester"),
        addressLine4 = Some("Greater Manchester"),
        postcode = "M1 1AA",
        phoneNumber = Some("0161234567"),
        email = Some("agent@smithpartners.com"),
        agentReference = Some("AGT123456"),
        isAuthorised = Some("Y")
      )

      val expectedResponse: CreateReturnAgentReturn = CreateReturnAgentReturn(
        returnAgentID = "RA100001"
      )

      when(mockService.createReturnAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createReturnAgent()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "returnAgentID").as[String] mustBe "RA100001"

      verify(mockService).createReturnAgent(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 201 with minimal agent details" in new Setup {
      val request: CreateReturnAgentRequest = CreateReturnAgentRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        agentType = "ACCOUNTANT",
        name = "Quick Accounting Services",
        houseNumber = None,
        addressLine1 = "High Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = "EC1A 1BB",
        phoneNumber = None,
        email = None,
        agentReference = None,
        isAuthorised = None
      )

      val expectedResponse: CreateReturnAgentReturn = CreateReturnAgentReturn(
        returnAgentID = "RA100002"
      )

      when(mockService.createReturnAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createReturnAgent()(req)

      status(res) mustBe CREATED
      (contentAsJson(res) \ "returnAgentID").as[String] mustBe "RA100002"
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.createReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "name"              -> "Smith & Partners LLP",
        "addressLine1"      -> "Legal District",
        "postcode"          -> "M1 1AA"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"      -> "STORN12345",
        "agentType"    -> "SOLICITOR",
        "name"         -> "Smith & Partners LLP",
        "addressLine1" -> "Legal District",
        "postcode"     -> "M1 1AA"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when agentType is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "name"              -> "Smith & Partners LLP",
        "addressLine1"      -> "Legal District",
        "postcode"          -> "M1 1AA"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when name is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "addressLine1"      -> "Legal District",
        "postcode"          -> "M1 1AA"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when addressLine1 is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "name"              -> "Smith & Partners LLP",
        "postcode"          -> "M1 1AA"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when postcode is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "name"              -> "Smith & Partners LLP",
        "addressLine1"      -> "Legal District"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: CreateReturnAgentRequest = CreateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR",
        name = "Smith & Partners LLP",
        houseNumber = None,
        addressLine1 = "Legal District",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = "M1 1AA",
        phoneNumber = None,
        email = None,
        agentReference = None,
        isAuthorised = None
      )

      when(mockService.createReturnAgent(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createReturnAgent()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).createReturnAgent(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ReturnAgentReturnsController updateReturnAgent" - {

    "returns 200 with updated flag when service succeeds" in new Setup {
      val request: UpdateReturnAgentRequest = UpdateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR",
        name = "Updated Smith & Partners LLP",
        houseNumber = Some("20"),
        addressLine1 = "New Legal District",
        addressLine2 = Some("Updated Quarter"),
        addressLine3 = Some("Manchester"),
        addressLine4 = Some("Greater Manchester"),
        postcode = "M2 2BB",
        phoneNumber = Some("0161999888"),
        email = Some("updated@smithpartners.com"),
        agentReference = Some("AGT999999"),
        isAuthorised = Some("Y")
      )

      val expectedResponse: UpdateReturnAgentReturn = UpdateReturnAgentReturn(
        updated = true
      )

      when(mockService.updateReturnAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateReturnAgent()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updateReturnAgent(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with minimal update details" in new Setup {
      val request: UpdateReturnAgentRequest = UpdateReturnAgentRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        agentType = "ACCOUNTANT",
        name = "Quick Accounting Updated",
        houseNumber = None,
        addressLine1 = "New High Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = "EC2A 2BB",
        phoneNumber = None,
        email = None,
        agentReference = None,
        isAuthorised = None
      )

      val expectedResponse: UpdateReturnAgentReturn = UpdateReturnAgentReturn(
        updated = true
      )

      when(mockService.updateReturnAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateReturnAgent()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when stornId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "name"              -> "Smith & Partners LLP",
        "addressLine1"      -> "Legal District",
        "postcode"          -> "M1 1AA"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"      -> "STORN12345",
        "agentType"    -> "SOLICITOR",
        "name"         -> "Smith & Partners LLP",
        "addressLine1" -> "Legal District",
        "postcode"     -> "M1 1AA"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when agentType is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "name"              -> "Smith & Partners LLP",
        "addressLine1"      -> "Legal District",
        "postcode"          -> "M1 1AA"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when name is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "addressLine1"      -> "Legal District",
        "postcode"          -> "M1 1AA"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when addressLine1 is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "name"              -> "Smith & Partners LLP",
        "postcode"          -> "M1 1AA"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when postcode is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR",
        "name"              -> "Smith & Partners LLP",
        "addressLine1"      -> "Legal District"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdateReturnAgentRequest = UpdateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR",
        name = "Smith & Partners LLP",
        houseNumber = None,
        addressLine1 = "Legal District",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = "M1 1AA",
        phoneNumber = None,
        email = None,
        agentReference = None,
        isAuthorised = None
      )

      when(mockService.updateReturnAgent(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database timeout")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateReturnAgent()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateReturnAgent(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ReturnAgentReturnsController deleteReturnAgent" - {

    "returns 200 with deleted flag when service succeeds" in new Setup {
      val request: DeleteReturnAgentRequest = DeleteReturnAgentRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR"
      )

      val expectedResponse: DeleteReturnAgentReturn = DeleteReturnAgentReturn(
        deleted = true
      )

      when(mockService.deleteReturnAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteReturnAgent()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "deleted").as[Boolean] mustBe true

      verify(mockService).deleteReturnAgent(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 for different agent type" in new Setup {
      val request: DeleteReturnAgentRequest = DeleteReturnAgentRequest(
        storn = "STORN99999",
        returnResourceRef = "100002",
        agentType = "ACCOUNTANT"
      )

      val expectedResponse: DeleteReturnAgentReturn = DeleteReturnAgentReturn(
        deleted = true
      )

      when(mockService.deleteReturnAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteReturnAgent()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "deleted").as[Boolean] mustBe true
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.deleteReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "agentType"         -> "SOLICITOR"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"     -> "STORN12345",
        "agentType" -> "SOLICITOR"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when agentType is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteReturnAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: DeleteReturnAgentRequest = DeleteReturnAgentRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR"
      )

      when(mockService.deleteReturnAgent(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteReturnAgent()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).deleteReturnAgent(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService: ReturnService = mock[ReturnService]
    val controller                 = new ReturnAgentReturnsController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/return-agent")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)
  }
}
