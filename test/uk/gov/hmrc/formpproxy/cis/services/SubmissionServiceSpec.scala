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

package uk.gov.hmrc.formpproxy.cis.services

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.GovTalkErrorStatus
import uk.gov.hmrc.formpproxy.cis.models.GovTalkErrorStatus.*
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateSubmissionRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource

import scala.concurrent.Future

class SubmissionServiceSpec extends SpecBase {

  trait Setup {
    val repo: CisMonthlyReturnSource = mock[CisMonthlyReturnSource]
    lazy val service                 = new SubmissionService(repo)
  }
  def setup: Setup = new Setup {}

  "SubmissionService createSubmission" - {

    "delegates to repo and returns submissionId" in {
      val s = setup; import s.*

      val req = CreateSubmissionRequest("123", 2024, 4, "N")
      when(repo.createSubmission(any[CreateSubmissionRequest]))
        .thenReturn(Future.successful("sub-123"))

      val out = service.createSubmission(req).futureValue
      out mustBe "sub-123"

      verify(repo).createSubmission(eqTo(req))
    }

    "propagates failure from repo" in {
      val s = setup; import s.*

      val req = CreateSubmissionRequest("123", 2024, 4, "N")
      when(repo.createSubmission(any[CreateSubmissionRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val fut = service.createSubmission(req)
      whenReady(fut.failed) { ex =>
        ex.getMessage mustBe "boom"
      }

      verify(repo).createSubmission(eqTo(req))
    }
  }

  "SubmissionService#updateSubmission" - {

    "delegates to repo and returns Unit" in {
      val s = setup; import s.*

      val req = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        amendment = "N",
        hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
        submittableStatus = "ACCEPTED"
      )
      when(repo.updateMonthlyReturnSubmission(any[UpdateSubmissionRequest]))
        .thenReturn(Future.successful(()))

      service.updateSubmission(req).futureValue mustBe ((): Unit)

      verify(repo).updateMonthlyReturnSubmission(eqTo(req))
    }

    "propagates failure from repo" in {
      val s = setup; import s.*

      val req = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        amendment = "N",
        hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
        submittableStatus = "ACCEPTED"
      )
      when(repo.updateMonthlyReturnSubmission(any[UpdateSubmissionRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val fut = service.updateSubmission(req)
      whenReady(fut.failed) { ex =>
        ex.getMessage mustBe "boom"
      }

      verify(repo).updateMonthlyReturnSubmission(eqTo(req))
    }

    "applies the F20b GovTalk error mapping when govTalkResponse is provided" - {

      def baseReq(status: GovTalkErrorStatus): UpdateSubmissionRequest =
        UpdateSubmissionRequest(
          instanceId = "123",
          taxYear = 2024,
          taxMonth = 4,
          hmrcMarkGenerated = "mark",
          submittableStatus = "FATAL_ERROR",
          amendment = "N",
          govTalkResponse = Some(status)
        )

      def captureRequest(s: Setup): UpdateSubmissionRequest = {
        val captor = ArgumentCaptor.forClass(classOf[UpdateSubmissionRequest])
        verify(s.repo).updateMonthlyReturnSubmission(captor.capture())
        captor.getValue
      }

      "RecoverableError → systemError with ChRIS code and text" in {
        val s = setup; import s.*
        when(repo.updateMonthlyReturnSubmission(any[UpdateSubmissionRequest])).thenReturn(Future.successful(()))

        service.updateSubmission(baseReq(RecoverableError("3000", "rec-text"))).futureValue

        val sent = captureRequest(s)
        sent.govtalkErrorCode mustBe Some("3000")
        sent.govtalkErrorType mustBe Some("systemError")
        sent.govtalkErrorMessage mustBe Some("rec-text")
      }

      "FatalError → systemError with ChRIS code and text" in {
        val s = setup; import s.*
        when(repo.updateMonthlyReturnSubmission(any[UpdateSubmissionRequest])).thenReturn(Future.successful(()))

        service.updateSubmission(baseReq(FatalError("9999", "fatal-text"))).futureValue

        val sent = captureRequest(s)
        sent.govtalkErrorCode mustBe Some("9999")
        sent.govtalkErrorType mustBe Some("systemError")
        sent.govtalkErrorMessage mustBe Some("fatal-text")
      }

      "DepartmentalError → 3001 / departmentalError / ChRIS text" in {
        val s = setup; import s.*
        when(repo.updateMonthlyReturnSubmission(any[UpdateSubmissionRequest])).thenReturn(Future.successful(()))

        service.updateSubmission(baseReq(DepartmentalError("dept-text"))).futureValue

        val sent = captureRequest(s)
        sent.govtalkErrorCode mustBe Some("3001")
        sent.govtalkErrorType mustBe Some("departmentalError")
        sent.govtalkErrorMessage mustBe Some("dept-text")
      }

      "ServerError(500..505) → http code / timeOut / timeOut" in {
        val s = setup; import s.*
        when(repo.updateMonthlyReturnSubmission(any[UpdateSubmissionRequest])).thenReturn(Future.successful(()))

        service.updateSubmission(baseReq(ServerError(503))).futureValue

        val sent = captureRequest(s)
        sent.govtalkErrorCode mustBe Some("503")
        sent.govtalkErrorType mustBe Some("timeOut")
        sent.govtalkErrorMessage mustBe Some("timeOut")
      }

      "NoResponse → 'xxxx' / timeOut / 'timed out'" in {
        val s = setup; import s.*
        when(repo.updateMonthlyReturnSubmission(any[UpdateSubmissionRequest])).thenReturn(Future.successful(()))

        service.updateSubmission(baseReq(NoResponse)).futureValue

        val sent = captureRequest(s)
        sent.govtalkErrorCode mustBe Some("xxxx")
        sent.govtalkErrorType mustBe Some("timeOut")
        sent.govtalkErrorMessage mustBe Some("timed out")
      }

      "OtherStatus → all three columns NULL even if caller supplied raw values" in {
        val s = setup; import s.*
        when(repo.updateMonthlyReturnSubmission(any[UpdateSubmissionRequest])).thenReturn(Future.successful(()))

        val req = baseReq(OtherStatus).copy(
          govtalkErrorCode = Some("ignored"),
          govtalkErrorType = Some("ignored"),
          govtalkErrorMessage = Some("ignored")
        )

        service.updateSubmission(req).futureValue

        val sent = captureRequest(s)
        sent.govtalkErrorCode mustBe None
        sent.govtalkErrorType mustBe None
        sent.govtalkErrorMessage mustBe None
      }
    }

    "preserves directly-supplied gov talk error fields when govTalkResponse is absent" in {
      val s = setup; import s.*
      when(repo.updateMonthlyReturnSubmission(any[UpdateSubmissionRequest])).thenReturn(Future.successful(()))

      val req = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        hmrcMarkGenerated = "mark",
        submittableStatus = "FATAL_ERROR",
        amendment = "N",
        govtalkErrorCode = Some("raw-code"),
        govtalkErrorType = Some("raw-type"),
        govtalkErrorMessage = Some("raw-msg")
      )

      service.updateSubmission(req).futureValue

      verify(repo).updateMonthlyReturnSubmission(eqTo(req))
    }
  }
}
