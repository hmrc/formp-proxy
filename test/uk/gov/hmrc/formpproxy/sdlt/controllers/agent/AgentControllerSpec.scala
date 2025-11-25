package uk.gov.hmrc.formpproxy.sdlt.controllers.agent

import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.mockito.ArgumentMatchers.eq as eqTo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{ControllerComponents, PlayBodyParsers, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.formpproxy.sdlt.models.agent.{DeleteAgentRequest, DeleteAgentReturn}
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.formpproxy.sdlt.services.agent.AgentService

import scala.concurrent.{ExecutionContext, Future}

class AgentControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "AgentController deleteSDLTAgent" - {

    "returns 200 with deleted flag when service succeeds" in new Setup {
      val request: DeleteAgentRequest = DeleteAgentRequest(
        storn = "STN001",
        agentReferenceNumber = "ARN001"
      )

      val expectedResponse: DeleteAgentReturn = DeleteAgentReturn(
        deleted = true
      )

      when(mockService.deleteAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result] = controller.deleteAgent()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "deleted").as[Boolean] mustBe true

      verify(mockService).deleteAgent(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers

    private def fakeAuth: AuthAction = new FakeAuthAction(parsers)

    val mockService: AgentService = mock[AgentService]
    val controller = new AgentController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/return-agent")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)
  }
}
