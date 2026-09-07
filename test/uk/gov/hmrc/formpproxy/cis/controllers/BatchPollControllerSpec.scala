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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, ControllerComponents, PlayBodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.formpproxy.actions.AuthOrInternalAuthAction
import uk.gov.hmrc.formpproxy.cis.models.response.*
import uk.gov.hmrc.formpproxy.cis.services.BatchPollService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, IAAction, Predicate, Resource, Retrieval}
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import scala.concurrent.{ExecutionContext, Future}

class BatchPollControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "BatchPollController getBatchPollSubmissions" - {

    "must return 200 with submissions to poll" in new Setup {
      when(internalAuth.stubAuth(Some(readBatchPoll), Retrieval.EmptyRetrieval)).thenReturn(Future.unit)
      when(mockService.getBatchPollSubmissions())
        .thenReturn(Future.successful(nonEmptyResponse))

      val result = controller
        .getBatchPollSubmissions()
        .apply(
          FakeRequest(GET, "/cis/batchpoll-submissions")
            .withHeaders(ACCEPT -> JSON, AUTHORIZATION -> "Token internal-auth")
        )

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(nonEmptyResponse)

      verify(mockService).getBatchPollSubmissions()
      verifyNoMoreInteractions(mockService)
    }

    "must return 200 with empty lists when no submissions are returned" in new Setup {
      when(internalAuth.stubAuth(Some(readBatchPoll), Retrieval.EmptyRetrieval)).thenReturn(Future.unit)
      when(mockService.getBatchPollSubmissions())
        .thenReturn(Future.successful(emptyResponse))

      val result = controller
        .getBatchPollSubmissions()
        .apply(
          FakeRequest(GET, "/cis/batchpoll-submissions")
            .withHeaders(ACCEPT -> JSON, AUTHORIZATION -> "Token internal-auth")
        )

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(emptyResponse)

      verify(mockService).getBatchPollSubmissions()
      verifyNoMoreInteractions(mockService)
    }

    "must return 500 when service fails" in new Setup {
      when(internalAuth.stubAuth(Some(readBatchPoll), Retrieval.EmptyRetrieval)).thenReturn(Future.unit)
      when(mockService.getBatchPollSubmissions())
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result = controller
        .getBatchPollSubmissions()
        .apply(
          FakeRequest(GET, "/cis/batchpoll-submissions")
            .withHeaders(ACCEPT -> JSON, AUTHORIZATION -> "Token internal-auth")
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")

      verify(mockService).getBatchPollSubmissions()
      verifyNoMoreInteractions(mockService)
    }

    "turns a caller away when internal-auth has not granted permission" in new Setup {
      when(internalAuth.stubAuth(Some(readBatchPoll), Retrieval.EmptyRetrieval))
        .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

      val rejection: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
        await(
          controller
            .getBatchPollSubmissions()
            .apply(
              FakeRequest(GET, "/cis/batchpoll-submissions")
                .withHeaders(ACCEPT -> JSON, AUTHORIZATION -> "Token internal-auth")
            )
        )
      }
      rejection.statusCode mustBe UNAUTHORIZED
      verifyNoInteractions(mockService)
    }
  }

  private trait Setup {

    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers

    val mockService: BatchPollService = mock[BatchPollService]

    private val resource                             = Resource.from("formp-proxy", "formp-proxy/cis/batchpoll")
    val readBatchPoll: Predicate.Permission          = Predicate.Permission(resource, IAAction("READ"))
    val internalAuth: StubBehaviour                  = mock[StubBehaviour]
    val backendAuth: BackendAuthComponents           = BackendAuthComponentsStub(internalAuth)(cc, ec)
    val mockAuthConnector: AuthConnector             = mock[AuthConnector]
    val authOrInternalAuth: AuthOrInternalAuthAction =
      new AuthOrInternalAuthAction(mockAuthConnector, backendAuth, new BodyParsers.Default(parsers))

    val controller = new BatchPollController(authOrInternalAuth, mockService, cc)

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
