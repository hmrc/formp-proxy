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

package uk.gov.hmrc.formpproxy.cis.services

import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.formpproxy.cis.models.response.*
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource

import scala.concurrent.Future

class BatchPollServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "BatchPollService getBatchPollSubmissions" - {

    "returns submissions from repository" in new Setup {
      when(mockRepo.getBatchPollSubmissions())
        .thenReturn(Future.successful(response))

      service.getBatchPollSubmissions().futureValue mustBe response

      verify(mockRepo).getBatchPollSubmissions()
      verifyNoMoreInteractions(mockRepo)
    }

    "propagates repository failure" in new Setup {
      val exception = new RuntimeException("boom")

      when(mockRepo.getBatchPollSubmissions())
        .thenReturn(Future.failed(exception))

      service.getBatchPollSubmissions().failed.futureValue mustBe exception

      verify(mockRepo).getBatchPollSubmissions()
      verifyNoMoreInteractions(mockRepo)
    }
  }

  private trait Setup {
    val mockRepo: CisMonthlyReturnSource = mock[CisMonthlyReturnSource]

    val service = new BatchPollService(mockRepo)

    val response: GetBatchPollSubmissionsResponse =
      GetBatchPollSubmissionsResponse(
        verificationSubmissions = Seq(
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
        ),
        monthlyReturnSubmissions = Seq(
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
        )
      )
  }
}
