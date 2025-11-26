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
import uk.gov.hmrc.formpproxy.sdlt.models.agents.{DeletePredefinedAgentRequest, DeletePredefinedAgentReturn}
import uk.gov.hmrc.formpproxy.sdlt.services.agents.DeletePredefinedAgentService

import scala.concurrent.{ExecutionContext, Future}

class DeletePredefinedAgentControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  val storn                = "STN001"
  val agentReferenceNumber = "ARN001"

  val request: DeletePredefinedAgentRequest = DeletePredefinedAgentRequest(
    storn,
    agentReferenceNumber
  )

  "AgentController deleteSDLTAgent" - {

    "returns 200 with deleted flag when service succeeds" in new Setup {

      val expectedResponse: DeletePredefinedAgentReturn = DeletePredefinedAgentReturn(
        deleted = true
      )

      when(mockService.deletePredefinedAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deletePredefinedAgent()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "deleted").as[Boolean] mustBe true

      verify(mockService).deletePredefinedAgent(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.deletePredefinedAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "agentReferenceNumber" -> agentReferenceNumber
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deletePredefinedAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "returns 400 when agentReferenceNumber is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn" -> storn
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deletePredefinedAgent()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {

      when(mockService.deletePredefinedAgent(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deletePredefinedAgent()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).deletePredefinedAgent(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers

    private def fakeAuth: AuthAction = new FakeAuthAction(parsers)

    val mockService: DeletePredefinedAgentService = mock[DeletePredefinedAgentService]
    val controller                                = new DeletePredefinedAgentController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/return-agent")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)
  }
}
