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

import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.requests.CreateAmendedMonthlyReturnRequest
import uk.gov.hmrc.formpproxy.cis.services.AmendMonthlyReturnService

import scala.concurrent.{ExecutionContext, Future}

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
        FakeRequest(POST, "/formp-proxy/cis/amend-monthly-return/create").withBody(requestBody)

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
        FakeRequest(POST, "/formp-proxy/cis/amend-monthly-return/create").withBody(requestBody)

      val res: Future[Result] =
        controller.createAmendedMonthlyReturn(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(res) mustBe Json.obj("message" -> "Unexpected error")

      verify(mockService).createAmendedMonthlyReturn(any[CreateAmendedMonthlyReturnRequest])
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    private val cc: ControllerComponents = stubControllerComponents()
    private val fakeAuth: AuthAction     = new FakeAuthAction(cc.parsers)

    val mockService: AmendMonthlyReturnService = mock[AmendMonthlyReturnService]

    val controller =
      new AmendMonthlyReturnController(fakeAuth, mockService, cc)
  }
}
