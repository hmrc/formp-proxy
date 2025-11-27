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

package uk.gov.hmrc.formpproxy.sdlt.services.agents

import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.mockito.ArgumentMatchers.eq as eqTo
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.sdlt.models.agents.{DeletePredefinedAgentRequest, DeletePredefinedAgentResponse}
import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository

import scala.concurrent.Future

class DeletePredefinedAgentServiceSpec extends SpecBase {

  "DeletePredefinedAgentService.deletePredefinedAgent" - {

    "must delegate to repository and return true when delete successful" in new Setup {
      val expectedResponse: DeletePredefinedAgentResponse = DeletePredefinedAgentResponse(deleted = true)

      when(repo.sdltDeletePredefinedAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: DeletePredefinedAgentResponse = service.deletePredefinedAgent(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltDeletePredefinedAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must return false when delete fails" in new Setup {
      val expectedResponse: DeletePredefinedAgentResponse = DeletePredefinedAgentResponse(deleted = false)

      when(repo.sdltDeletePredefinedAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: DeletePredefinedAgentResponse = service.deletePredefinedAgent(request).futureValue
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
    val repo                                  = mock[SdltFormpRepository]
    val service                               = new DeletePredefinedAgentService(repo)
    val request: DeletePredefinedAgentRequest = DeletePredefinedAgentRequest(
      storn = "STN001",
      agentReferenceNumber = "ARN001"
    )
  }
}
