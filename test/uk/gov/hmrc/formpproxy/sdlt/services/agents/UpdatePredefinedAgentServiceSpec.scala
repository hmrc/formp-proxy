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
import uk.gov.hmrc.formpproxy.sdlt.models.agents.{UpdatePredefinedAgentRequest, UpdatePredefinedAgentResponse}
import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository

import scala.concurrent.Future

class UpdatePredefinedAgentServiceSpec extends SpecBase {

  "UpdatePredefinedAgentService.updatePredefinedAgent" - {

    "must delegate to repository and return true when update successful" in new Setup {
      val expectedResponse: UpdatePredefinedAgentResponse = UpdatePredefinedAgentResponse(updated = true)

      when(repo.sdltUpdatePredefinedAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: UpdatePredefinedAgentResponse = service.updatePredefinedAgent(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltUpdatePredefinedAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must return false when update fails" in new Setup {
      val expectedResponse: UpdatePredefinedAgentResponse = UpdatePredefinedAgentResponse(updated = false)

      when(repo.sdltUpdatePredefinedAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: UpdatePredefinedAgentResponse = service.updatePredefinedAgent(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltUpdatePredefinedAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in new Setup {
      val boom = new RuntimeException("DB error")

      when(repo.sdltUpdatePredefinedAgent(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updatePredefinedAgent(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltUpdatePredefinedAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  private trait Setup {
    val repo                                  = mock[SdltFormpRepository]
    val service                               = new UpdatePredefinedAgentService(repo)
    val request: UpdatePredefinedAgentRequest = UpdatePredefinedAgentRequest(
      storn = "STN001",
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
      agentResourceReference = "ARN001"
    )
  }
}