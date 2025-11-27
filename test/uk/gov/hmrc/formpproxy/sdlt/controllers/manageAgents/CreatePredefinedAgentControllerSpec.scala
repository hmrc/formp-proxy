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

package uk.gov.hmrc.formpproxy.sdlt.controllers.manageAgents

import org.mockito.Mockito.{verify, verifyNoInteractions, verifyNoMoreInteractions, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{ControllerComponents, PlayBodyParsers, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{ACCEPT, BAD_GATEWAY, CONTENT_TYPE, JSON, contentAsJson, contentType, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.formpproxy.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.formpproxy.sdlt.models.agent.manageAgents.{CreatePredefinedAgentRequest, CreatePredefinedAgentResponse}
import uk.gov.hmrc.formpproxy.sdlt.services.manageAgents.CreatePredefinedAgentService
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.{ExecutionContext, Future}

class CreatePredefinedAgentControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "CreatePredefinedAgentController createPredefinedAgent" - {
    "invalid payload" - {

      "must return BadRequest when `agentName` is missing" in new Setup {
        val req = makeRequest(createPredefinedAgentRequestWithoutAgentNameData)
        val res = controller.createPredefinedAgent()(req)

        status(res) mustBe BAD_REQUEST
        (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
        verifyNoInteractions(mockService)
      }

      "must return BadRequest when `storn` is missing" in new Setup {
        val req = makeRequest(createPredefinedAgentRequestWithoutStornData)
        val res = controller.createPredefinedAgent()(req)

        status(res) mustBe BAD_REQUEST
        (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
        verifyNoInteractions(mockService)
      }

      "must return BadRequest for empty `agentName` and `storn` are missing" in new Setup {
        val req = makeRequest(createPredefinedAgentRequestWithoutStornAndAgentNameData)
        val res = controller.createPredefinedAgent()(req)

        status(res) mustBe BAD_REQUEST
        (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
        verifyNoInteractions(mockService)
      }

      "must return BadRequest for invalid json " in new Setup {
        val req = makeRequest(Json.obj("foo" -> "bar"))
        val res = controller.createPredefinedAgent()(req)

        status(res) mustBe BAD_REQUEST
        (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
        verifyNoInteractions(mockService)
      }

      "must return BadRequest for empty json " in new Setup {
        val req                 = makeRequest(Json.obj())
        val res: Future[Result] = controller.createPredefinedAgent()(req)

        status(res) mustBe BAD_REQUEST
        (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
        verifyNoInteractions(mockService)
      }

    }

    "valid payload" - {
      val fullCreatePredefinedAgentData: CreatePredefinedAgentRequest  =
        CreatePredefinedAgentRequest(
          storn = "STN12345",
          agentName = "John",
          houseNumber = Some("10"),
          addressLine1 = Some("Downing Street"),
          addressLine2 = Some("Westminster"),
          addressLine3 = Some("London"),
          addressLine4 = None,
          postcode = Some("SW1A 2AA"),
          phone = Some("02071234567"),
          email = Some("info@smithco.co.uk"),
          dxAddress = None
        )
      val createPredefinedAgentResponse: CreatePredefinedAgentResponse =
        CreatePredefinedAgentResponse(
          agentResourceRef = Some("1234"),
          agentId = Some("value")
        )
      "must return Ok with CreatePredefinedAgentResponse when createPredefined operation succeeds" in new Setup {

        val req = makeRequest(Json.toJson(fullCreatePredefinedAgentData))

        when(mockService.createPredefinedAgent(fullCreatePredefinedAgentData))
          .thenReturn(Future.successful(createPredefinedAgentResponse))

        val res: Future[Result] = controller.createPredefinedAgent()(req)

        status(res) mustBe OK
        contentType(res) mustBe Some(JSON)

        val json = contentAsJson(res)
        (json \ "agentResourceRef").asOpt[String] mustBe Some("1234")
        (json \ "agentId").asOpt[String] mustBe Some("value")

        verify(mockService).createPredefinedAgent(fullCreatePredefinedAgentData)
        verifyNoMoreInteractions(mockService)

      }

      "must propagate Upstream Error Response (status and message)" in new Setup {

        val req = makeRequest(Json.toJson(fullCreatePredefinedAgentData))

        val err = UpstreamErrorResponse("formp service failed", BAD_GATEWAY, BAD_GATEWAY)

        when(mockService.createPredefinedAgent(fullCreatePredefinedAgentData))
          .thenReturn(Future.failed(err))

        val res: Future[Result] = controller.createPredefinedAgent()(req)

        status(res) mustBe BAD_GATEWAY
        (contentAsJson(res) \ "message").as[String] must include("formp service failed")

        verify(mockService).createPredefinedAgent(fullCreatePredefinedAgentData)
        verifyNoMoreInteractions(mockService)

      }

      "must return 500 with generic message on unexpected exception" in new Setup {
        val req = makeRequest(Json.toJson(fullCreatePredefinedAgentData))

        when(mockService.createPredefinedAgent(fullCreatePredefinedAgentData))
          .thenReturn(Future.failed(new RuntimeException("Database timeout")))

        val res: Future[Result] = controller.createPredefinedAgent()(req)

        status(res) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(res) \ "message").as[String] must include("Unexpected error")

        verify(mockService).createPredefinedAgent(fullCreatePredefinedAgentData)
        verifyNoMoreInteractions(mockService)
      }
    }

  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService = mock[CreatePredefinedAgentService]
    val controller  = new CreatePredefinedAgentController(fakeAuth, mockService, cc)

    def makeRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest("POST", "formp-proxy/create/predefined-agent")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)

    val validCreatePredefinedAgentRequestData: JsObject                    = Json.obj(
      "storn"        -> "STN12345",
      "agentName"    -> "Michael",
      "houseNumber"  -> Some("10"),
      "addressLine1" -> Some("Downing Street"),
      "addressLine2" -> Some("Westminster"),
      "addressLine3" -> Some("London"),
      "addressLine4" -> None,
      "postcode"     -> Some("SW1A 2AA"),
      "phone"        -> Some("02071234567"),
      "email"        -> Some("info@smithco.co.uk"),
      "dxAddress"    -> None
    )
    val createPredefinedAgentRequestWithoutAgentNameData: JsObject         = Json.obj(
      "storn"        -> "STN12345",
      "houseNumber"  -> Some("10"),
      "addressLine1" -> Some("Downing Street"),
      "addressLine2" -> Some("Westminster"),
      "addressLine3" -> Some("London"),
      "addressLine4" -> None,
      "postcode"     -> Some("SW1A 2AA"),
      "phone"        -> Some("02071234567"),
      "email"        -> Some("info@smithco.co.uk"),
      "dxAddress"    -> None
    )
    val createPredefinedAgentRequestWithoutStornData: JsObject             = Json.obj(
      "agentName"    -> "John",
      "houseNumber"  -> Some("10"),
      "addressLine1" -> Some("Downing Street"),
      "addressLine2" -> Some("Westminster"),
      "addressLine3" -> Some("London"),
      "addressLine4" -> None,
      "postcode"     -> Some("SW1A 2AA"),
      "phone"        -> Some("02071234567"),
      "email"        -> Some("info@smithco.co.uk"),
      "dxAddress"    -> None
    )
    val createPredefinedAgentRequestWithoutStornAndAgentNameData: JsObject = Json.obj(
      "houseNumber"  -> Some("10"),
      "addressLine1" -> Some("Downing Street"),
      "addressLine2" -> Some("Westminster"),
      "addressLine3" -> Some("London"),
      "addressLine4" -> None,
      "postcode"     -> Some("SW1A 2AA"),
      "phone"        -> Some("02071234567"),
      "email"        -> Some("info@smithco.co.uk"),
      "dxAddress"    -> None
    )
  }

}
