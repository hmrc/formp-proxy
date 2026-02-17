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
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.GovTalkStatusRecord
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.models.response.*
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource

import java.time.LocalDateTime
import scala.concurrent.Future

final class GovTalkServiceSpec extends SpecBase {

  case class Ctx() {
    val repo: CisMonthlyReturnSource = mock[CisMonthlyReturnSource]
    val service                      = new GovTalkService(repo)
  }

  private def mkRecord(protocol: String, numPolls: Int, pollInterval: Int): GovTalkStatusRecord =
    GovTalkStatusRecord(
      userIdentifier = "1",
      formResultID = "12890",
      correlationID = "C742D5DEE7EB4D15B4F7EFD50B890525",
      formLock = "N",
      createDate = None,
      endStateDate = Some(LocalDateTime.parse("2026-02-05T00:00:00")),
      lastMessageDate = LocalDateTime.parse("2025-02-05T00:00:00"),
      numPolls = numPolls,
      pollInterval = pollInterval,
      protocolStatus = protocol,
      gatewayURL = "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/sync/VATDEC"
    )

  "GovTalkService getGovTalkStatus" - {

    val request = GetGovTalkStatusRequest("1", "1234")

    "returns multiple records when repository returns rows" in {
      val c       = Ctx()
      val payload = GetGovTalkStatusResponse(Seq(mkRecord("dataRequest", 0, 0), mkRecord("endState", 1, 1)))

      when(c.repo.getGovTalkStatus(request))
        .thenReturn(Future.successful(payload))

      val result = c.service.getGovTalkStatus(request).futureValue
      result mustBe payload

      verify(c.repo).getGovTalkStatus(request)
      verifyNoMoreInteractions(c.repo)
    }

    "returns empty response when repository returns empty list" in {
      val c = Ctx()
      when(c.repo.getGovTalkStatus(request))
        .thenReturn(Future.successful(GetGovTalkStatusResponse(Seq.empty)))

      val result = c.service.getGovTalkStatus(request).futureValue
      result.govtalk_status mustBe empty

      verify(c.repo).getGovTalkStatus(request)
      verifyNoMoreInteractions(c.repo)
    }

    "propagates failures from the repository" in {
      val c    = Ctx()
      val boom = new RuntimeException("formp failed")

      when(c.repo.getGovTalkStatus(request))
        .thenReturn(Future.failed(boom))

      val ex = c.service.getGovTalkStatus(request).failed.futureValue
      ex mustBe boom

      verify(c.repo).getGovTalkStatus(request)
      verifyNoMoreInteractions(c.repo)
    }
  }

  "GovTalkService updateGovTalkStatusCorrelationId" - {

    val request = UpdateGovTalkStatusCorrelationIdRequest(
      userIdentifier = "1",
      formResultID = "12890",
      correlationID = "C742D5DEE7EB4D15B4F7EFD50B890525",
      pollInterval = 1,
      gatewayURL = "http://example.com"
    )

    "returns Unit when repository succeeds" in {
      val c = Ctx()

      when(c.repo.updateGovTalkStatusCorrelationId(request))
        .thenReturn(Future.successful(()))

      val result = c.service.updateGovTalkStatusCorrelationId(request).futureValue
      result mustBe ()

      verify(c.repo).updateGovTalkStatusCorrelationId(request)
      verifyNoMoreInteractions(c.repo)
    }

    "propagates failures from the repository" in {
      val c    = Ctx()
      val boom = new RuntimeException("update failed")

      when(c.repo.updateGovTalkStatusCorrelationId(request))
        .thenReturn(Future.failed(boom))

      val ex = c.service.updateGovTalkStatusCorrelationId(request).failed.futureValue
      ex mustBe boom

      verify(c.repo).updateGovTalkStatusCorrelationId(request)
      verifyNoMoreInteractions(c.repo)
    }
  }

  "GovTalkService resetGovTalkStatus" - {

    val request = ResetGovTalkStatusRequest(
      userIdentifier = "1",
      formResultID = "12890",
      oldProtocolStatus = "dataRequest",
      gatewayURL = "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
    )

    "should succeed when records are reset" in {
      val c = Ctx()

      when(c.repo.resetGovTalkStatus(request))
        .thenReturn(Future.successful(()))

      c.service.resetGovTalkStatus(request).futureValue

      verify(c.repo).resetGovTalkStatus(request)
      verifyNoMoreInteractions(c.repo)
    }

    "propagates failures from the repository" in {
      val c    = Ctx()
      val boom = new RuntimeException("formp failed")

      when(c.repo.resetGovTalkStatus(request))
        .thenReturn(Future.failed(boom))

      val ex = c.service.resetGovTalkStatus(request).failed.futureValue
      ex mustBe boom

      verify(c.repo).resetGovTalkStatus(request)
      verifyNoMoreInteractions(c.repo)
    }
  }
}
