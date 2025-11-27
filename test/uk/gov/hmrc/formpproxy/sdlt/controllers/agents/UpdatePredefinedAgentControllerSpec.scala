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

package uk.gov.hmrc.formpproxy.sdlt.controllers.agents

import org.mockito.Mockito.{verify, verifyNoInteractions, verifyNoMoreInteractions, when}
import org.mockito.ArgumentMatchers.eq as eqTo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{ControllerComponents, PlayBodyParsers, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.formpproxy.sdlt.models.agents.{UpdatePredefinedAgentRequest, UpdatePredefinedAgentResponse}
import uk.gov.hmrc.formpproxy.sdlt.services.agents.UpdatePredefinedAgentService
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.{ExecutionContext, Future}

class UpdatePredefinedAgentControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  val storn                = "STN001"
  val agentReferenceNumber = "ARN001"

  val request: UpdatePredefinedAgentRequest = UpdatePredefinedAgentRequest(
    storn,
    agentId = None,
    name = "Smith & Co Solicitors",
    houseNumber = None,
    address1 = "12 High Street",
    address2 = Some("London"),
    address3 = Some("Greater London"),
    address4 = None,
    postcode = Some("SW1A 1AA"),
    phone = "02071234567",
    email = "info@smithco.co.uk",
    dxAddress = None,
    agentResourceReference = agentReferenceNumber
  )

  "AgentController updateSDLTAgent" - {

    "returns 200 with updated flag when service succeeds" in new Setup {

      val expectedResponse: UpdatePredefinedAgentResponse = UpdatePredefinedAgentResponse(
        updated = true
      )

      when(mockService.updatePredefinedAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updatePredefinedAgent()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "updated").as[Boolean] mustBe true

      verify(mockService).updatePredefinedAgent(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updatePredefinedAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "agentReferenceNumber" -> agentReferenceNumber
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updatePredefinedAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when agentReferenceNumber is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn" -> storn
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updatePredefinedAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns status code BAD_GATEWAY when Upstream error is returned" in new Setup {
      val err: UpstreamErrorResponse = UpstreamErrorResponse("Unexpected error", BAD_GATEWAY, BAD_GATEWAY)

      when(mockService.updatePredefinedAgent(eqTo(request)))
        .thenReturn(Future.failed(err))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updatePredefinedAgent()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }

    "returns 500 with generic message on unexpected exception" in new Setup {

      when(mockService.updatePredefinedAgent(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updatePredefinedAgent()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updatePredefinedAgent(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers

    private def fakeAuth: AuthAction = new FakeAuthAction(parsers)

    val mockService: UpdatePredefinedAgentService = mock[UpdatePredefinedAgentService]
    val controller                                = new UpdatePredefinedAgentController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/return-agent")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)
  }
}
