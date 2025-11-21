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


package uk.gov.hmrc.formpproxy.sdlt.services.organistion

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.formpproxy.sdlt.models.Agent
import uk.gov.hmrc.formpproxy.sdlt.models.organisation.GetSdltOrgRequest
import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository
import uk.gov.hmrc.formpproxy.sdlt.services.organisation.SdltOrganisationService

import scala.concurrent.{ExecutionContext, Future}

class SdltOrganisationServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "SdltOrganisationService.getSDLTOrganisation" - {

    "must delegate to repository and return its result" in new Setup {
      val storn = "STORN12345"

      when(mockRepo.sdltGetOrganisation(eqTo(storn)))
        .thenReturn(Future.successful(expectedOrganisation))

      val result = service.getSDLTOrganisation(storn).futureValue

      result mustBe expectedOrganisation
      verify(mockRepo).sdltGetOrganisation(eqTo(storn))
      verifyNoMoreInteractions(mockRepo)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    val mockRepo: SdltFormpRepository = mock[SdltFormpRepository]
    val service: SdltOrganisationService = new SdltOrganisationService(mockRepo)

    val expectedOrganisation: GetSdltOrgRequest =
      GetSdltOrgRequest(
        storn = Some("STORN12345"),
        version = Some("1"),
        isReturnUser = Some("Y"),
        doNotDisplayWelcomePage = Some("N"),
        agents = Seq(
          Agent(
            agentId = Some("AGT001"),
            storn = Some("STORN12345"),
            name = Some("Smith & Co Solicitors"),
            houseNumber = Some("10"),
            address1 = Some("Downing Street"),
            address2 = Some("Westminster"),
            address3 = Some("London"),
            address4 = None,
            postcode = Some("SW1A 2AA"),
            phone = Some("02071234567"),
            email = Some("info@smithco.co.uk"),
            dxAddress = Some("DX 12345 London"),
            agentResourceReference = Some("AGT-RES-001")
          )
        )
      )
  }
}
