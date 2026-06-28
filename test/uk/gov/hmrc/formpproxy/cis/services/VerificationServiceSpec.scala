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
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.{CreateVerifications, DeleteVerifications}
import uk.gov.hmrc.formpproxy.cis.models.response.*
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource
import java.time.LocalDateTime

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
        monthlyReturnSubmission = None
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

  "MonthlyReturnService deleteMonthlyReturnItem" - {

    "delegates to repo (happy path) when all properties are present in JSON" in new Ctx {
      val request = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L, 222L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L, 444L),
            actionIndicator = None
          )
        )
      )

      when(repo.modifyVerifications(eqTo(request)))
        .thenReturn(Future.successful(()))

      service.modifyVerifications(request).futureValue mustBe ()

      verify(repo).modifyVerifications(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "propagates failures from repo" in new Ctx {
      val request = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L, 222L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L, 444L),
            actionIndicator = None
          )
        )
      )

      val boom = new RuntimeException("db failed")
      when(repo.modifyVerifications(eqTo(request)))
        .thenReturn(Future.failed(boom))

      service.modifyVerifications(request).failed.futureValue mustBe boom

      verify(repo).modifyVerifications(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "delegates to repo (happy path) when instanceId and deleteVerifications are present" in new Ctx {
      val request = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L, 222L)
          )
        ),
        createVerifications = None
      )

      when(repo.modifyVerifications(eqTo(request)))
        .thenReturn(Future.successful(()))

      service.modifyVerifications(request).futureValue mustBe ()

      verify(repo).modifyVerifications(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "delegates to repo (happy path) when instanceId and createVerifications are present" in new Ctx {
      val request = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = None,
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L, 444L),
            actionIndicator = None
          )
        )
      )

      when(repo.modifyVerifications(eqTo(request)))
        .thenReturn(Future.successful(()))

      service.modifyVerifications(request).futureValue mustBe ()

      verify(repo).modifyVerifications(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "fail when deleteVerifications is provided but verificationResourceReferences is empty" in new Ctx {
      val request = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq.empty
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L, 444L),
            actionIndicator = None
          )
        )
      )

      val exception: Throwable = service.modifyVerifications(request).failed.futureValue

      exception mustBe a[IllegalArgumentException]
      exception.getMessage mustBe "verificationResourceReferences must not be empty when deleteVerifications is provided"

      verify(repo, times(0))
        .modifyVerifications(any[ModifyVerificationsRequest])
    }

    "fail when createVerifications is provided but verificationResourceReferences is empty" in new Ctx {
      val request = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L, 222L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq.empty,
            actionIndicator = None
          )
        )
      )

      val exception: Throwable = service.modifyVerifications(request).failed.futureValue

      exception mustBe a[IllegalArgumentException]
      exception.getMessage mustBe "verificationResourceReferences must not be empty when createVerifications is provided"

      verify(repo, times(0))
        .modifyVerifications(any[ModifyVerificationsRequest])
    }

    "fail when deleteVerifications and createVerifications are both None" in new Ctx {
      val request = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = None,
        createVerifications = None
      )

      val exception: Throwable = service.modifyVerifications(request).failed.futureValue

      exception mustBe a[RuntimeException]
      exception.getMessage mustBe "deleteVerifications or createVerifications is required when instanceId is provided"

      verify(repo, times(0))
        .modifyVerifications(any[ModifyVerificationsRequest])
    }
  }
  "VerificationService#createSubmissionForVerification" - {

    "delegates to repository" in {
      val c = Ctx();
      import c.*

      val req = CreateSubmissionAndUpdateVerificationsRequest(
        instanceId = "abc-123",
        verificationBatchId = 99L,
        verificationBatchResourceRef = 10L,
        emailRecipient = "ops@example.com",
        irMarkGenerated = Some("IR_MARK"),
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

      val response = CreateSubmissionAndUpdateVerificationsResponse(submissionId = 555L)

      when(repo.createSubmissionAndUpdateVerifications(eqTo(req)))
        .thenReturn(Future.successful(response))

      service.createSubmissionAndUpdateVerifications(req).futureValue mustBe response

      verify(repo).createSubmissionAndUpdateVerifications(eqTo(req))
      verifyNoMoreInteractions(repo)
    }

    "propagates failure from repository" in {
      val c = Ctx();
      import c.*

      val req = CreateSubmissionAndUpdateVerificationsRequest(
        instanceId = "abc-123",
        verificationBatchId = 99L,
        verificationBatchResourceRef = 10L,
        emailRecipient = "ops@example.com",
        irMarkGenerated = Some("IR_MARK"),
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

      when(repo.createSubmissionAndUpdateVerifications(eqTo(req)))
        .thenReturn(Future.failed(boom))

      service.createSubmissionAndUpdateVerifications(req).failed.futureValue mustBe boom

      verify(repo).createSubmissionAndUpdateVerifications(eqTo(req))
      verifyNoMoreInteractions(repo)
    }
  }

  "VerificationService#processVerificationResponseFromChris" - {

    "delegates to repository" in {
      val c = Ctx()
      import c.*

      val request = ProcessVerificationResponseFromChrisRequest(
        instanceId = "abc-123",
        verificationBatchResourceRef = 77L,
        acceptedTime = "2026-06-15T10:05:00Z",
        submissionStatus = "ACCEPTED",
        irMarkReceived = Some("IR_MARK"),
        verificationResults = Seq(
          VerificationResult(
            resourceRef = 111L,
            matched = Some("Y"),
            verified = Some("Y"),
            verificationNumber = Some("V123456"),
            taxTreatment = "NET",
            verifiedDate = LocalDateTime.parse("2026-06-15T10:05:00")
          )
        )
      )

      when(repo.processVerificationResponseFromChris(eqTo(request)))
        .thenReturn(Future.successful(()))

      service.processVerificationResponseFromChris(request).futureValue mustBe ()

      verify(repo).processVerificationResponseFromChris(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "propagates failure from repository" in {
      val c = Ctx()
      import c.*

      val request = ProcessVerificationResponseFromChrisRequest(
        instanceId = "abc-123",
        verificationBatchResourceRef = 77L,
        acceptedTime = "2026-06-15T10:05:00Z",
        submissionStatus = "ACCEPTED",
        irMarkReceived = Some("IR_MARK"),
        verificationResults = Seq(
          VerificationResult(
            resourceRef = 111L,
            matched = Some("Y"),
            verified = Some("Y"),
            verificationNumber = Some("V123456"),
            taxTreatment = "NET",
            verifiedDate = LocalDateTime.parse("2026-06-15T10:05:00")
          )
        )
      )

      val boom = new RuntimeException("boom")

      when(repo.processVerificationResponseFromChris(eqTo(request)))
        .thenReturn(Future.failed(boom))

      service.processVerificationResponseFromChris(request).failed.futureValue mustBe boom

      verify(repo).processVerificationResponseFromChris(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "VerificationService#getSubmittedVerifications" - {

    "delegates to repository" in {
      val c = Ctx()
      import c.*

      val request  = GetSubmittedVerificationsRequest("abc-123")
      val response = GetSubmittedVerificationsResponse(
        scheme = Seq.empty,
        subcontractors = Seq.empty,
        verificationBatches = Seq.empty,
        verifications = Seq.empty,
        submissions = Seq.empty
      )

      when(repo.getSubmittedVerifications(eqTo(request)))
        .thenReturn(Future.successful(response))

      service.getSubmittedVerifications(request).futureValue mustBe response

      verify(repo).getSubmittedVerifications(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "propagates failure from repository" in {
      val c = Ctx()
      import c.*

      val request = GetSubmittedVerificationsRequest("abc-123")
      val boom    = new RuntimeException("boom")

      when(repo.getSubmittedVerifications(eqTo(request)))
        .thenReturn(Future.failed(boom))

      service.getSubmittedVerifications(request).failed.futureValue mustBe boom

      verify(repo).getSubmittedVerifications(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

}
