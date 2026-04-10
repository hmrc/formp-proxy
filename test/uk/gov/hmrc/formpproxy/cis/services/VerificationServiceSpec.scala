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
import org.mockito.Mockito._
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.response.GetNewestVerificationBatchResponse
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
        scheme = Seq.empty,
        subcontractors = Seq.empty,
        verificationBatch = Seq.empty,
        verifications = Seq.empty,
        submission = Seq.empty,
        monthlyReturn = Seq.empty,
        mrSubmission = Seq.empty
      )

      when(repo.getNewestVerificationBatch(eqTo(instanceId)))
        .thenReturn(Future.successful(response))

      service.getNewestVerificationBatch(instanceId).futureValue mustBe response

      verify(repo).getNewestVerificationBatch(eqTo(instanceId))
      verifyNoMoreInteractions(repo)
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
}
