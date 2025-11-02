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

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.*
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.requests.CreateNilMonthlyReturnRequest
import uk.gov.hmrc.formpproxy.cis.models.response.CreateNilMonthlyReturnResponse
import uk.gov.hmrc.formpproxy.cis.models.{MonthlyReturn, UserMonthlyReturns}
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource

import java.time.LocalDateTime
import scala.concurrent.Future

final class MonthlyReturnServiceSpec extends SpecBase{

  case class Ctx() {
    val repo    = mock[CisMonthlyReturnSource]
    val service = new MonthlyReturnService(repo)
    val id      = "123"
  }

  private def mkReturn(id: Long, month: Int, year: Int = 2025): MonthlyReturn =
    MonthlyReturn(
      monthlyReturnId        = id,
      taxYear                = year,
      taxMonth               = month,
      nilReturnIndicator     = Some("N"),
      decEmpStatusConsidered = Some("Y"),
      decAllSubsVerified     = Some("Y"),
      decInformationCorrect  = Some("Y"),
      decNoMoreSubPayments   = Some("N"),
      decNilReturnNoPayments = Some("N"),
      status                 = Some("SUBMITTED"),
      lastUpdate             = Some(LocalDateTime.parse("2025-01-01T00:00:00")),
      amendment              = Some("N"),
      supersededBy           = None
    )

  "MonthlyReturnService getAllMonthlyReturns" - {

    "returns wrapper when repository returns rows (happy path)" in {
      val c = Ctx()
      val payload = UserMonthlyReturns(Seq(mkReturn(66666L, 1), mkReturn(66667L, 7)))

      when(c.repo.getAllMonthlyReturns(eqTo(c.id)))
        .thenReturn(Future.successful(payload))

      val out = c.service.getAllMonthlyReturns(c.id).futureValue
      out mustBe payload

      verify(c.repo).getAllMonthlyReturns(eqTo(c.id))
      verifyNoMoreInteractions(c.repo)
    }

    "returns empty wrapper when repository returns empty" in {
      val c = Ctx()
      when(c.repo.getAllMonthlyReturns(eqTo(c.id)))
        .thenReturn(Future.successful(UserMonthlyReturns(Seq.empty)))

      val out = c.service.getAllMonthlyReturns(c.id).futureValue
      out.monthlyReturnList mustBe empty

      verify(c.repo).getAllMonthlyReturns(eqTo(c.id))
      verifyNoMoreInteractions(c.repo)
    }

    "propagates failures from the repository" in {
      val c = Ctx()
      val boom = new RuntimeException("formp failed")

      when(c.repo.getAllMonthlyReturns(eqTo(c.id)))
        .thenReturn(Future.failed(boom))

      val ex = c.service.getAllMonthlyReturns(c.id).failed.futureValue
      ex mustBe boom

      verify(c.repo).getAllMonthlyReturns(eqTo(c.id))
      verifyNoMoreInteractions(c.repo)
    }
  }

  "MonthlyReturnService createNilMonthlyReturn" - {

    "delegates to repo and returns status (happy path)" in new Ctx {
      val request = CreateNilMonthlyReturnRequest(
        instanceId             = id,
        taxYear                = 2025,
        taxMonth               = 2,
        decInformationCorrect  = "Y",
        decNilReturnNoPayments = "Y"
      )
      val res = CreateNilMonthlyReturnResponse(status = "STARTED")

      when(repo.createNilMonthlyReturn(eqTo(request))).thenReturn(Future.successful(res))

      val out = service.createNilMonthlyReturn(request).futureValue
      out mustBe res

      verify(repo).createNilMonthlyReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "propagates failures from the repository" in new Ctx {
      val request = CreateNilMonthlyReturnRequest(
        instanceId             = id,
        taxYear                = 2025,
        taxMonth               = 2,
        decInformationCorrect  = "Y",
        decNilReturnNoPayments = "Y"
      )
      val boom = new RuntimeException("db failed")

      when(repo.createNilMonthlyReturn(eqTo(request))).thenReturn(Future.failed(boom))

      val ex = service.createNilMonthlyReturn(request).failed.futureValue
      ex mustBe boom

      verify(repo).createNilMonthlyReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "MonthlyReturnService getSchemeEmail" - {

    "delegates to repo and returns Some(email)" in new Ctx {
      when(repo.getSchemeEmail(eqTo(id))).thenReturn(Future.successful(Some("a@b.com")))

      service.getSchemeEmail(id).futureValue mustBe Some("a@b.com")
      verify(repo).getSchemeEmail(eqTo(id))
      verifyNoMoreInteractions(repo)
    }

    "delegates to repo and returns None" in new Ctx {
      when(repo.getSchemeEmail(eqTo(id))).thenReturn(Future.successful(None))

      service.getSchemeEmail(id).futureValue mustBe None
      verify(repo).getSchemeEmail(eqTo(id))
      verifyNoMoreInteractions(repo)
    }

    "propagates failures from repo" in new Ctx {
      val boom = new RuntimeException("db boom")
      when(repo.getSchemeEmail(eqTo(id))).thenReturn(Future.failed(boom))

      service.getSchemeEmail(id).failed.futureValue mustBe boom
      verify(repo).getSchemeEmail(eqTo(id))
      verifyNoMoreInteractions(repo)
    }
  }
}
