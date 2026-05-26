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

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.*
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.response.*
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource

import scala.concurrent.Future

class VerificationServiceSpec extends SpecBase {

  case class Ctx() {
    val repo: CisMonthlyReturnSource = mock[CisMonthlyReturnSource]
    lazy val service                 = new VerificationService(repo)
  }

  "VerificationService#getNewestVerificationBatch" - {

    "return successful response from repo" in {
      val c = Ctx(); import c.*

      val instanceId = "abc-123"
      val response   = GetNewestVerificationBatchResponse(
        scheme = None,
        subcontractors = Seq.empty,
        verificationBatch = None,
        verifications = Seq.empty,
        submission = None,
        monthlyReturn = None,
        monthlyReturnSubmission = Seq.empty
      )

      when(repo.getNewestVerificationBatch(eqTo(instanceId)))
        .thenReturn(Future.successful(response))

      service.getNewestVerificationBatch(instanceId).futureValue mustBe response

      verify(repo).getNewestVerificationBatch(eqTo(instanceId))
      verifyNoMoreInteractions(repo)
    }

    "VerificationService.createVerificationBatchAndVerifications" - {
      "delegates to repository" in {
        val repo    = mock[CisMonthlyReturnSource]
        val service = new VerificationService(repo)

        val req = CreateVerificationBatchAndVerificationsRequest(
          instanceId = "abc-123",
          verificationResourceReferences = Seq(111L, 222L),
          actionIndicator = Some("A")
        )

        val response = CreateVerificationBatchAndVerificationsResponse(
          verificationBatchResourceReference = 10L
        )

        when(repo.createVerificationBatchAndVerifications(eqTo(req))).thenReturn(Future.successful(response))

        service.createVerificationBatchAndVerifications(req).futureValue mustBe response
        verify(repo).createVerificationBatchAndVerifications(eqTo(req))
      }
    }

    "propagates failure from repo" in {
      val c = Ctx(); import c.*

      val instanceId = "abc-123"
      val boom       = new RuntimeException("boom")

      when(repo.getNewestVerificationBatch(eqTo(instanceId)))
        .thenReturn(Future.failed(boom))

      val ex = service.getNewestVerificationBatch(instanceId).failed.futureValue
      ex mustBe boom

      verify(repo).getNewestVerificationBatch(eqTo(instanceId))
      verifyNoMoreInteractions(repo)
    }
  }

  "VerificationService#getCurrentVerificationBatch" - {

    "return successful response from repo" in {
      val c = Ctx();
      import c.*

      val instanceId = "abc-123"
      val response   = GetCurrentVerificationBatchResponse(
        scheme = None,
        subcontractors = Seq.empty,
        verificationBatch = None,
        verifications = Seq.empty,
        submission = None
      )

      when(repo.getCurrentVerificationBatch(eqTo(instanceId)))
        .thenReturn(Future.successful(response))

      service.getCurrentVerificationBatch(instanceId).futureValue mustBe response

      verify(repo).getCurrentVerificationBatch(eqTo(instanceId))
      verifyNoMoreInteractions(repo)
    }

    "propagates failure from repo" in {
      val c = Ctx();
      import c.*

      val instanceId = "abc-123"
      val boom       = new RuntimeException("boom")

      when(repo.getCurrentVerificationBatch(eqTo(instanceId)))
        .thenReturn(Future.failed(boom))

      val ex = service.getCurrentVerificationBatch(instanceId).failed.futureValue
      ex mustBe boom

      verify(repo).getCurrentVerificationBatch(eqTo(instanceId))
      verifyNoMoreInteractions(repo)
    }
  }

  "VerificationService#createSubmissionForVerification" - {

    "delegates to repository" in {
      val c = Ctx();
      import c.*

      val req = CreateSubmissionForVerificationRequest(
        instanceId = "abc-123",
        verificationBatchId = 99L,
        verificationBatchResourceRef = 10L,
        emailRecipient = "ops@example.com",
        irMarkGenerated = "IR_MARK",
        verifications = Seq(
          VerificationToUpdate(
            subcontractorName = "ACME LTD",
            verificationResourceRef = 111L,
            proceedVerification = "Y"
          ),
          VerificationToUpdate(
            subcontractorName = "BETA LTD",
            verificationResourceRef = 222L,
            proceedVerification = "N"
          )
        ),
        agentId = Some("agent-123")
      )

      val response = CreateSubmissionForVerificationResponse(submissionId = 555L)

      when(repo.createSubmissionForVerification(eqTo(req)))
        .thenReturn(Future.successful(response))

      service.createSubmissionForVerification(req).futureValue mustBe response

      verify(repo).createSubmissionForVerification(eqTo(req))
      verifyNoMoreInteractions(repo)
    }

    "propagates failure from repository" in {
      val c = Ctx();
      import c.*

      val req = CreateSubmissionForVerificationRequest(
        instanceId = "abc-123",
        verificationBatchId = 99L,
        verificationBatchResourceRef = 10L,
        emailRecipient = "ops@example.com",
        irMarkGenerated = "IR_MARK",
        verifications = Seq(
          VerificationToUpdate(
            subcontractorName = "ACME LTD",
            verificationResourceRef = 111L,
            proceedVerification = "Y"
          )
        ),
        agentId = None
      )

      val boom = new RuntimeException("boom")

      when(repo.createSubmissionForVerification(eqTo(req)))
        .thenReturn(Future.failed(boom))

      service.createSubmissionForVerification(req).failed.futureValue mustBe boom

      verify(repo).createSubmissionForVerification(eqTo(req))
      verifyNoMoreInteractions(repo)
    }
  }

}
