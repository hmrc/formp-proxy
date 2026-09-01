/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.formpproxy.cis.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, PlayBodyParsers, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.formpproxy.actions.AuthOrInternalAuthAction
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.requests.CreateAmendedMonthlyReturnRequest
import uk.gov.hmrc.formpproxy.cis.services.AmendMonthlyReturnService
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client.*

import scala.concurrent.Future

class AmendMonthlyReturnControllerSpec extends SpecBase {

  "AmendMonthlyReturnController createAmendedMonthlyReturn" - {

    "returns 201 Created when service succeeds" in new Setup {
      val requestBody = CreateAmendedMonthlyReturnRequest(
        instanceId = "1",
        taxYear = 2025,
        taxMonth = 1,
        version = 0
      )

      when(mockService.createAmendedMonthlyReturn(any[CreateAmendedMonthlyReturnRequest]))
        .thenReturn(Future.successful(()))

      val req: FakeRequest[CreateAmendedMonthlyReturnRequest] =
        FakeRequest(POST, "/formp-proxy/cis/amend-monthly-return/create")
          .withHeaders(AUTHORIZATION -> "Token internal-auth")
          .withBody(requestBody)

      val res: Future[Result] =
        controller.createAmendedMonthlyReturn(req)

      status(res) mustBe CREATED

      verify(mockService).createAmendedMonthlyReturn(any[CreateAmendedMonthlyReturnRequest])
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 with generic message when service fails" in new Setup {
      val requestBody = CreateAmendedMonthlyReturnRequest(
        instanceId = "1",
        taxYear = 2025,
        taxMonth = 1,
        version = 0
      )

      when(mockService.createAmendedMonthlyReturn(any[CreateAmendedMonthlyReturnRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req: FakeRequest[CreateAmendedMonthlyReturnRequest] =
        FakeRequest(POST, "/formp-proxy/cis/amend-monthly-return/create")
          .withHeaders(AUTHORIZATION -> "Token internal-auth")
          .withBody(requestBody)

      val res: Future[Result] =
        controller.createAmendedMonthlyReturn(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(res) mustBe Json.obj("message" -> "Unexpected error")

      verify(mockService).createAmendedMonthlyReturn(any[CreateAmendedMonthlyReturnRequest])
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    private val parsers: PlayBodyParsers = cc.parsers

    val mockService: AmendMonthlyReturnService = mock[AmendMonthlyReturnService]

    private val resource                             = Resource.from("formp-proxy", "formp-proxy/cis/monthly-returns")
    val writeMonthlyReturns: Predicate.Permission    = Predicate.Permission(resource, IAAction("WRITE"))
    val internalAuth: StubBehaviour                  = mock[StubBehaviour]
    val backendAuth: BackendAuthComponents           = BackendAuthComponentsStub(internalAuth)(cc, ec)
    val mockAuthConnector: AuthConnector             = mock[AuthConnector]
    val authOrInternalAuth: AuthOrInternalAuthAction =
      new AuthOrInternalAuthAction(mockAuthConnector, backendAuth, new BodyParsers.Default(parsers))

    when(internalAuth.stubAuth(Some(writeMonthlyReturns), Retrieval.EmptyRetrieval)).thenReturn(Future.unit)

    val controller = new AmendMonthlyReturnController(authOrInternalAuth, mockService, cc)
  }
}
