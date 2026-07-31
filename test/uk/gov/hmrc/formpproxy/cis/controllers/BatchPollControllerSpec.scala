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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.formpproxy.actions.{CisAuthOrApiKeyAction, DefaultCisAuthOrApiKeyAction}
import uk.gov.hmrc.formpproxy.cis.models.response.*
import uk.gov.hmrc.formpproxy.cis.services.BatchPollService
import uk.gov.hmrc.formpproxy.config.AppConfig

import scala.concurrent.{ExecutionContext, Future}

class BatchPollControllerSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  private val expectedApiKey = "test-cis-api-key"

  "BatchPollController getBatchPollSubmissions" - {

    "must return 200 with submissions to poll" in new Setup {
      when(mockService.getBatchPollSubmissions())
        .thenReturn(Future.successful(nonEmptyResponse))

      val result =
        controller
          .getBatchPollSubmissions()
          .apply(
            FakeRequest(GET, "/cis/batchpoll-submissions")
              .withHeaders("X-API-Key" -> expectedApiKey)
          )

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(nonEmptyResponse)

      verify(mockService).getBatchPollSubmissions()
      verifyNoMoreInteractions(mockService)
      verifyNoInteractions(mockAuthConnector)
    }

    "must return 200 with empty lists when no submissions are returned" in new Setup {
      when(mockService.getBatchPollSubmissions())
        .thenReturn(Future.successful(emptyResponse))

      val result =
        controller
          .getBatchPollSubmissions()
          .apply(
            FakeRequest(GET, "/cis/batchpoll-submissions")
              .withHeaders("X-API-Key" -> expectedApiKey)
          )

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(emptyResponse)

      verify(mockService).getBatchPollSubmissions()
      verifyNoMoreInteractions(mockService)
      verifyNoInteractions(mockAuthConnector)
    }

    "must return 500 when service fails" in new Setup {
      when(mockService.getBatchPollSubmissions())
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result =
        controller
          .getBatchPollSubmissions()
          .apply(
            FakeRequest(GET, "/cis/batchpoll-submissions")
              .withHeaders("X-API-Key" -> expectedApiKey)
          )

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj(
        "message" -> "Unexpected error"
      )

      verify(mockService).getBatchPollSubmissions()
      verifyNoMoreInteractions(mockService)
      verifyNoInteractions(mockAuthConnector)
    }

    "must return 401 when the API key is missing" in new Setup {
      val result =
        controller
          .getBatchPollSubmissions()
          .apply(
            FakeRequest(GET, "/cis/batchpoll-submissions")
          )

      status(result) mustBe UNAUTHORIZED

      verifyNoInteractions(mockService)
      verifyNoInteractions(mockAuthConnector)
    }

    "must return 401 when the API key is incorrect" in new Setup {
      val result =
        controller
          .getBatchPollSubmissions()
          .apply(
            FakeRequest(GET, "/cis/batchpoll-submissions")
              .withHeaders("X-API-Key" -> "wrong-api-key")
          )

      status(result) mustBe UNAUTHORIZED

      verifyNoInteractions(mockService)
      verifyNoInteractions(mockAuthConnector)
    }
  }

  private trait Setup {

    implicit val ec: ExecutionContext =
      scala.concurrent.ExecutionContext.global

    private val cc: ControllerComponents =
      stubControllerComponents()

    val mockService: BatchPollService =
      mock[BatchPollService]

    val mockAuthConnector: AuthConnector =
      mock[AuthConnector]

    val mockAppConfig: AppConfig =
      mock[AppConfig]

    when(mockAppConfig.cisInternalServiceApiKey)
      .thenReturn(expectedApiKey)

    val cisAuthOrApiKeyAction: CisAuthOrApiKeyAction =
      new DefaultCisAuthOrApiKeyAction(
        authConnector = mockAuthConnector,
        appConfig = mockAppConfig,
        parser = new BodyParsers.Default(cc.parsers)
      )

    val controller =
      new BatchPollController(
        cisAuthOrApiKeyAction = cisAuthOrApiKeyAction,
        service = mockService,
        cc = cc
      )

    val verificationSubmission: VerificationSubmissionToPoll =
      VerificationSubmissionToPoll(
        submissionId = 90001L,
        submissionType = "CISVERIFY",
        agentId = Some("A123456"),
        taxOfficeNumber = "123",
        taxOfficeReference = "ABC123",
        instanceId = "instance-verification-001",
        status = "SUBMITTED",
        verificationBatchResourceRef = 70001L
      )

    val monthlyReturnSubmission: MonthlyReturnSubmissionToPoll =
      MonthlyReturnSubmissionToPoll(
        submissionId = 90002L,
        submissionType = "CIS300MR",
        status = "SUBMITTED",
        taxOfficeNumber = "123",
        taxOfficeReference = "456789",
        taxYear = 2025,
        taxMonth = 6,
        instanceId = "instance-monthly-return-001",
        agentId = Some("A123456")
      )

    val nonEmptyResponse: GetBatchPollSubmissionsResponse =
      GetBatchPollSubmissionsResponse(
        verificationSubmissions = Seq(verificationSubmission),
        monthlyReturnSubmissions = Seq(monthlyReturnSubmission)
      )

    val emptyResponse: GetBatchPollSubmissionsResponse =
      GetBatchPollSubmissionsResponse(
        verificationSubmissions = Seq.empty,
        monthlyReturnSubmissions = Seq.empty
      )
  }
}
