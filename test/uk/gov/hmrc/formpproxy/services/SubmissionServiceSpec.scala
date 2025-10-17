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

package uk.gov.hmrc.formpproxy.services

import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.models.requests.{CreateAndTrackSubmissionRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.formpproxy.repositories.CisMonthlyReturnSource

import scala.concurrent.Future

class SubmissionServiceSpec extends SpecBase {

  trait Setup {
    val repo: CisMonthlyReturnSource = mock[CisMonthlyReturnSource]
    lazy val service = new SubmissionService(repo)
  }
  def setup: Setup = new Setup {}

  "SubmissionService createAndTrackSubmission" - {

    "delegates to repo and returns submissionId" in {
      val s = setup; import s._

      val req = CreateAndTrackSubmissionRequest("123", 2024, 4)
      when(repo.createAndTrackSubmission(any[CreateAndTrackSubmissionRequest]))
        .thenReturn(Future.successful("sub-123"))

      val out = service.createAndTrackSubmission(req).futureValue
      out mustBe "sub-123"

      verify(repo).createAndTrackSubmission(eqTo(req))
    }

    "propagates failure from repo" in {
      val s = setup; import s._

      val req = CreateAndTrackSubmissionRequest("123", 2024, 4)
      when(repo.createAndTrackSubmission(any[CreateAndTrackSubmissionRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val fut = service.createAndTrackSubmission(req)
      whenReady(fut.failed) { ex =>
        ex.getMessage mustBe "boom"
      }

      verify(repo).createAndTrackSubmission(eqTo(req))
    }
  }

  "SubmissionService#updateSubmission" - {

    "delegates to repo and returns Unit" in {
      val s = setup; import s._

      val req = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
        submittableStatus = "ACCEPTED"
      )
      when(repo.updateMonthlyReturnSubmission(any[UpdateSubmissionRequest]))
        .thenReturn(Future.successful(()))

      service.updateSubmission(req).futureValue mustBe ((): Unit)

      verify(repo).updateMonthlyReturnSubmission(eqTo(req))
    }

    "propagates failure from repo" in {
      val s = setup; import s._

      val req = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
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
  }
}