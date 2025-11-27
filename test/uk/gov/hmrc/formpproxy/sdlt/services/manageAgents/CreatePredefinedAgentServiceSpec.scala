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

package uk.gov.hmrc.formpproxy.sdlt.services.manageAgents

import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.formpproxy.sdlt.models.agent.manageAgents.{CreatePredefinedAgentRequest, CreatePredefinedAgentResponse}
import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository

import scala.concurrent.{ExecutionContext, Future}

class CreatePredefinedAgentServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "CreatePredefinedAgentService.createPredefinedAgent" - {

    "must delegate to repository and return its result " in new Setup {

      when(mockRepo.sdltCreatePredefinedAgent(fullCreatePredefinedAgentData))
        .thenReturn(Future.successful(createPredefinedAgentResponse))

      val res: CreatePredefinedAgentResponse = service.createPredefinedAgent(fullCreatePredefinedAgentData).futureValue

      res mustBe createPredefinedAgentResponse
      res.agentResourceRef mustBe Some("1234")
      res.agentId mustBe Some("value")

      verify(mockRepo).sdltCreatePredefinedAgent(fullCreatePredefinedAgentData)
      verifyNoMoreInteractions(mockRepo)
    }
  }

}

trait Setup {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val mockRepo: SdltFormpRepository         = mock[SdltFormpRepository]
  val service: CreatePredefinedAgentService = new CreatePredefinedAgentService(mockRepo)

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
}
