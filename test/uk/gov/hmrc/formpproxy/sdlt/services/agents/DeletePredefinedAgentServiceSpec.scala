package uk.gov.hmrc.formpproxy.sdlt.services.agents

import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.mockito.ArgumentMatchers.eq as eqTo
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.sdlt.models.agents.{DeletePredefinedAgentRequest, DeletePredefinedAgentReturn}
import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository

import scala.concurrent.Future

class DeletePredefinedAgentServiceSpec extends SpecBase {

  "DeletePredefinedAgentService.deletePredefinedAgent" - {

    "must delegate to repository and return true when delete successful" in new Setup {
      val expectedResponse: DeletePredefinedAgentReturn = DeletePredefinedAgentReturn(deleted = true)

      when(repo.sdltDeletePredefinedAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: DeletePredefinedAgentReturn = service.deletePredefinedAgent(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltDeletePredefinedAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must return false when delete fails" in new Setup {
      val expectedResponse: DeletePredefinedAgentReturn = DeletePredefinedAgentReturn(deleted = false)

      when(repo.sdltDeletePredefinedAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: DeletePredefinedAgentReturn = service.deletePredefinedAgent(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltDeletePredefinedAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in new Setup {
      val boom = new RuntimeException("DB error")

      when(repo.sdltDeletePredefinedAgent(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deletePredefinedAgent(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltDeletePredefinedAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  private trait Setup {
    val repo = mock[SdltFormpRepository]
    val service = new DeletePredefinedAgentService(repo)
    val request: DeletePredefinedAgentRequest = DeletePredefinedAgentRequest(
      storn = "STN001",
      agentReferenceNumber = "ARN001"
    )
  }
}
