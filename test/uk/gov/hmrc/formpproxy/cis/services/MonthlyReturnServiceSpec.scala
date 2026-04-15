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
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.models.response.*
import uk.gov.hmrc.formpproxy.cis.models.{ContractorScheme, MonthlyReturn, UnsubmittedMonthlyReturns, UserMonthlyReturns}
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource

import java.time.LocalDateTime
import scala.concurrent.Future

final class MonthlyReturnServiceSpec extends SpecBase {

  case class Ctx() {
    val repo    = mock[CisMonthlyReturnSource]
    val service = new MonthlyReturnService(repo)
    val id      = "123"
  }

  private def mkReturn(id: Long, month: Int, year: Int = 2025): MonthlyReturn =
    MonthlyReturn(
      monthlyReturnId = id,
      taxYear = year,
      taxMonth = month,
      nilReturnIndicator = Some("N"),
      decEmpStatusConsidered = Some("Y"),
      decAllSubsVerified = Some("Y"),
      decInformationCorrect = Some("Y"),
      decNoMoreSubPayments = Some("N"),
      decNilReturnNoPayments = Some("N"),
      status = Some("SUBMITTED"),
      lastUpdate = Some(LocalDateTime.parse("2025-01-01T00:00:00")),
      amendment = Some("N"),
      supersededBy = None
    )

  "MonthlyReturnService getAllMonthlyReturns" - {

    "returns wrapper when repository returns rows (happy path)" in {
      val c       = Ctx()
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
      val c    = Ctx()
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
        instanceId = id,
        taxYear = 2025,
        taxMonth = 2,
        decInformationCorrect = "Y",
        decNilReturnNoPayments = "Y"
      )
      val res     = CreateNilMonthlyReturnResponse(status = "STARTED")

      when(repo.createNilMonthlyReturn(eqTo(request))).thenReturn(Future.successful(res))

      val out = service.createNilMonthlyReturn(request).futureValue
      out mustBe res

      verify(repo).createNilMonthlyReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "propagates failures from the repository" in new Ctx {
      val request = CreateNilMonthlyReturnRequest(
        instanceId = id,
        taxYear = 2025,
        taxMonth = 2,
        decInformationCorrect = "Y",
        decNilReturnNoPayments = "Y"
      )
      val boom    = new RuntimeException("db failed")

      when(repo.createNilMonthlyReturn(eqTo(request))).thenReturn(Future.failed(boom))

      val ex = service.createNilMonthlyReturn(request).failed.futureValue
      ex mustBe boom

      verify(repo).createNilMonthlyReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "MonthlyReturnService updateNilMonthlyReturn" - {

    "delegates to repo (happy path)" in new Ctx {
      val request = UpdateMonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 2,
        amendment = "N",
        decInformationCorrect = Some("Y"),
        decNilReturnNoPayments = Some("Y"),
        nilReturnIndicator = "Y",
        status = "STARTED",
        version = Some(1L)
      )

      when(repo.updateMonthlyReturn(eqTo(request)))
        .thenReturn(Future.successful(()))

      service.updateMonthlyReturn(request).futureValue mustBe ()

      verify(repo).updateMonthlyReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "propagates failures from the repository" in new Ctx {
      val request = UpdateMonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 2,
        amendment = "N",
        decInformationCorrect = Some("Y"),
        decNilReturnNoPayments = Some("Y"),
        nilReturnIndicator = "Y",
        status = "STARTED",
        version = Some(1L)
      )
      val boom    = new RuntimeException("db failed")

      when(repo.updateMonthlyReturn(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex = service.updateMonthlyReturn(request).failed.futureValue
      ex mustBe boom

      verify(repo).updateMonthlyReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "MonthlyReturnService updateMonthlyReturnItem" - {

    "delegates to repo (happy path)" in new Ctx {
      val request = UpdateMonthlyReturnItemRequest(
        instanceId = "1",
        taxYear = 2015,
        taxMonth = 5,
        amendment = "N",
        itemResourceReference = 999L,
        totalPayments = "1000.00",
        costOfMaterials = "200.00",
        totalDeducted = "80.00",
        subcontractorName = "ABC Ltd",
        verificationNumber = Some("V123456")
      )

      when(repo.updateMonthlyReturnItem(eqTo(request)))
        .thenReturn(Future.successful(()))

      service.updateMonthlyReturnItem(request).futureValue mustBe ((): Unit)

      verify(repo).updateMonthlyReturnItem(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "propagates failures from the repository" in new Ctx {
      val request = UpdateMonthlyReturnItemRequest(
        instanceId = "1",
        taxYear = 2015,
        taxMonth = 5,
        amendment = "N",
        itemResourceReference = 999L,
        totalPayments = "1000.00",
        costOfMaterials = "200.00",
        totalDeducted = "80.00",
        subcontractorName = "ABC Ltd",
        verificationNumber = Some("V123456")
      )

      val boom = new RuntimeException("db failed")

      when(repo.updateMonthlyReturnItem(eqTo(request)))
        .thenReturn(Future.failed(boom))

      service.updateMonthlyReturnItem(request).failed.futureValue mustBe boom

      verify(repo).updateMonthlyReturnItem(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "MonthlyReturnService createMonthlyReturn" - {

    "delegates to repo and create return (happy path)" in new Ctx {
      val request = CreateMonthlyReturnRequest(
        instanceId = id,
        taxYear = 2025,
        taxMonth = 2
      )

      when(repo.createMonthlyReturn(eqTo(request))).thenReturn(Future.successful(()))

      service.createMonthlyReturn(request).futureValue

      verify(repo).createMonthlyReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "propagates failures from the repository" in new Ctx {
      val request = CreateMonthlyReturnRequest(
        instanceId = id,
        taxYear = 2025,
        taxMonth = 2
      )
      val boom    = new RuntimeException("db failed")

      when(repo.createMonthlyReturn(eqTo(request))).thenReturn(Future.failed(boom))

      val ex: Throwable = service.createMonthlyReturn(request).failed.futureValue
      ex mustBe boom

      verify(repo).createMonthlyReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "MonthlyReturnService getSchemeEmail" - {

    "delegates to repo and returns email (happy path)" in new Ctx {
      val payload = Some("test@test.com")

      when(repo.getSchemeEmail(eqTo(id)))
        .thenReturn(Future.successful(payload))

      val out = service.getSchemeEmail(id).futureValue

      out mustBe payload

      verify(repo).getSchemeEmail(eqTo(id))
      verifyNoMoreInteractions(repo)
    }

    "propagates failures from the repository" in new Ctx {
      val payload = Some("test@test.com")
      val boom    = new RuntimeException("db failed")

      when(repo.getSchemeEmail(eqTo(id))).thenReturn(Future.failed(boom))

      val ex: Throwable = service.getSchemeEmail(id).failed.futureValue
      ex mustBe boom

      verify(repo).getSchemeEmail(eqTo(id))
      verifyNoMoreInteractions(repo)
    }
  }

  "MonthlyReturnService deleteMonthlyReturnItem" - {

    "delegates to repo (happy path)" in new Ctx {
      val request = DeleteMonthlyReturnItemRequest(
        instanceId = id,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        resourceReference = 12345L
      )

      when(repo.deleteMonthlyReturnItem(eqTo(request)))
        .thenReturn(Future.successful(()))

      service.deleteMonthlyReturnItem(request).futureValue mustBe ()

      verify(repo).deleteMonthlyReturnItem(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "propagates failures from repo" in new Ctx {
      val request = DeleteMonthlyReturnItemRequest(
        instanceId = id,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        resourceReference = 12345L
      )

      val boom = new RuntimeException("db failed")
      when(repo.deleteMonthlyReturnItem(eqTo(request)))
        .thenReturn(Future.failed(boom))

      service.deleteMonthlyReturnItem(request).failed.futureValue mustBe boom

      verify(repo).deleteMonthlyReturnItem(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "MonthlyReturnService getUnsubmittedMonthlyReturns" - {

    "returns wrapper when repository returns UnsubmittedMonthlyReturns (happy path)" in {
      val c = Ctx()

      val payload = UnsubmittedMonthlyReturns(
        scheme = ContractorScheme(
          schemeId = 100,
          instanceId = "abc-123",
          accountsOfficeReference = "accountsOfficeReference",
          taxOfficeNumber = "taxOfficeNumber",
          taxOfficeReference = "taxOfficeReference"
        ),
        monthlyReturn = Seq.empty
      )

      when(c.repo.getUnsubmittedMonthlyReturns(eqTo(c.id)))
        .thenReturn(Future.successful(payload))

      val out = c.service.getUnsubmittedMonthlyReturns(c.id).futureValue
      out mustBe payload

      verify(c.repo).getUnsubmittedMonthlyReturns(eqTo(c.id))
      verifyNoMoreInteractions(c.repo)
    }

    "propagates failures from the repository" in {
      val c    = Ctx()
      val boom = new RuntimeException("formp failed")

      when(c.repo.getUnsubmittedMonthlyReturns(eqTo(c.id)))
        .thenReturn(Future.failed(boom))

      val ex = c.service.getUnsubmittedMonthlyReturns(c.id).failed.futureValue
      ex mustBe boom

      verify(c.repo).getUnsubmittedMonthlyReturns(eqTo(c.id))
      verifyNoMoreInteractions(c.repo)
    }
  }

  "MonthlyReturnService getSubmittedMonthlyReturns" - {

    "returns wrapper when repository returns SubmittedMonthlyReturns (happy path)" in {
      val c       = Ctx()
      val request = GetSubmittedMonthlyReturnsRequest("abc-123", 2025, 1, "Y")

      val payload = GetSubmittedMonthlyReturnsResponse(
        scheme = ContractorScheme(
          schemeId = 100,
          instanceId = "abc-123",
          accountsOfficeReference = "accountsOfficeReference",
          taxOfficeNumber = "taxOfficeNumber",
          taxOfficeReference = "taxOfficeReference"
        ),
        monthlyReturn = Seq.empty,
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      when(c.repo.getSubmittedMonthlyReturns(eqTo(request)))
        .thenReturn(Future.successful(payload))

      val out = c.service.getSubmittedMonthlyReturns(request).futureValue
      out mustBe payload

      verify(c.repo).getSubmittedMonthlyReturns(eqTo(request))
      verifyNoMoreInteractions(c.repo)
    }

    "propagates failures from the repository" in {
      val c    = Ctx()
      val boom = new RuntimeException("formp failed")

      val request = GetSubmittedMonthlyReturnsRequest("abc-123", 2025, 1, "Y")

      when(c.repo.getSubmittedMonthlyReturns(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex = c.service.getSubmittedMonthlyReturns(request).failed.futureValue
      ex mustBe boom

      verify(c.repo).getSubmittedMonthlyReturns(eqTo(request))
      verifyNoMoreInteractions(c.repo)
    }
  }
}
