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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.{Company, Partnership, SoleTrader, Trust}
import uk.gov.hmrc.formpproxy.cis.models.requests.UpdateSubcontractorRequest
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource
import uk.gov.hmrc.formpproxy.cis.models.GetSubcontractorList
import uk.gov.hmrc.formpproxy.cis.models.response.GetSubcontractorListResponse

import java.time.LocalDateTime
import scala.concurrent.Future

class SubcontractorServiceSpec extends SpecBase {

  case class Ctx() {
    val repo: CisMonthlyReturnSource = mock[CisMonthlyReturnSource]
    lazy val service                 = new SubcontractorService(repo)
  }
  "ContractorSchemeService createSubcontractor" - {

    "returns version when repository successfully creates subcontractor (happy path)" in {
      val c = Ctx()

      when(c.repo.createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1)))
        .thenReturn(Future.successful(2))

      val out = c.service.createSubcontractor(123, SoleTrader, 1).futureValue
      out mustBe 2

      verify(c.repo).createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1))
      verifyNoMoreInteractions(c.repo)
    }

    "creates subcontractor with Company type" in {
      val c = Ctx()

      when(c.repo.createSubcontractor(eqTo(456), eqTo(Company), eqTo(3)))
        .thenReturn(Future.successful(4))

      val out = c.service.createSubcontractor(456, Company, 3).futureValue
      out mustBe 4

      verify(c.repo).createSubcontractor(eqTo(456), eqTo(Company), eqTo(3))
      verifyNoMoreInteractions(c.repo)
    }

    "creates subcontractor with Partnership type" in {
      val c = Ctx()

      when(c.repo.createSubcontractor(eqTo(789), eqTo(Partnership), eqTo(5)))
        .thenReturn(Future.successful(6))

      val out = c.service.createSubcontractor(789, Partnership, 5).futureValue
      out mustBe 6

      verify(c.repo).createSubcontractor(eqTo(789), eqTo(Partnership), eqTo(5))
      verifyNoMoreInteractions(c.repo)
    }

    "creates subcontractor with Trust type" in {
      val c = Ctx()

      when(c.repo.createSubcontractor(eqTo(999), eqTo(Trust), eqTo(7)))
        .thenReturn(Future.successful(8))

      val out = c.service.createSubcontractor(999, Trust, 7).futureValue
      out mustBe 8

      verify(c.repo).createSubcontractor(eqTo(999), eqTo(Trust), eqTo(7))
      verifyNoMoreInteractions(c.repo)
    }

    "propagates failures from the repository" in {
      val c    = Ctx()
      val boom = new RuntimeException("database failed")

      when(c.repo.createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1)))
        .thenReturn(Future.failed(boom))

      val ex = c.service.createSubcontractor(123, SoleTrader, 1).failed.futureValue
      ex mustBe boom

      verify(c.repo).createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1))
      verifyNoMoreInteractions(c.repo)
    }
  }

  "SubcontractorService#updateSubcontractor" - {

    "delegates to repo and returns Unit" in {
      val c = Ctx(); import c.*

      val req = UpdateSubcontractorRequest(
        utr = Some("1234567890"),
        pageVisited = Some(1),
        partnerUtr = None,
        crn = None,
        firstName = Some("John"),
        nino = None,
        secondName = None,
        surname = Some("Smith"),
        partnershipTradingName = None,
        tradingName = None,
        addressLine1 = Some("1 Main Street"),
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        country = Some("GB"),
        postcode = Some("AA1 1AA"),
        emailAddress = None,
        phoneNumber = None,
        mobilePhoneNumber = None,
        worksReferenceNumber = None,
        schemeId = 999,
        subbieResourceRef = 123,
        matched = None,
        autoVerified = None,
        verified = None,
        verificationNumber = None,
        taxTreatment = None,
        verificationDate = Some(LocalDateTime.of(2026, 1, 12, 10, 15, 30)),
        updatedTaxTreatment = None
      )

      when(repo.updateSubcontractor(any[UpdateSubcontractorRequest]))
        .thenReturn(Future.successful(()))

      service.updateSubcontractor(req).futureValue mustBe ((): Unit)

      verify(repo).updateSubcontractor(eqTo(req))
    }

    "propagates failure from repo" in {
      val c = Ctx(); import c.*

      val req = UpdateSubcontractorRequest(
        utr = Some("1234567890"),
        pageVisited = Some(1),
        partnerUtr = None,
        crn = None,
        firstName = None,
        nino = None,
        secondName = None,
        surname = None,
        partnershipTradingName = None,
        tradingName = None,
        addressLine1 = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        country = None,
        postcode = None,
        emailAddress = None,
        phoneNumber = None,
        mobilePhoneNumber = None,
        worksReferenceNumber = None,
        schemeId = 999,
        subbieResourceRef = 123,
        matched = None,
        autoVerified = None,
        verified = None,
        verificationNumber = None,
        taxTreatment = None,
        verificationDate = None,
        updatedTaxTreatment = None
      )

      when(repo.updateSubcontractor(any[UpdateSubcontractorRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val fut = service.updateSubcontractor(req)
      whenReady(fut.failed) { ex =>
        ex.getMessage mustBe "boom"
      }

      verify(repo).updateSubcontractor(eqTo(req))
    }
  }

  "SubcontractorService#getSubcontractorList" - {

    "delegates to repo with cisId and returns response" in {
      val c = Ctx();
      import c.*

      val req  = GetSubcontractorList(cisId = "cis-123")
      val resp = GetSubcontractorListResponse(subcontractors = List.empty)

      when(repo.getSubcontractorList(eqTo("cis-123")))
        .thenReturn(Future.successful(resp))

      val out = service.getSubcontractorList(req).futureValue
      out mustBe resp

      verify(repo).getSubcontractorList(eqTo("cis-123"))
      verifyNoMoreInteractions(repo)
    }

    "propagates failure from repo" in {
      val c = Ctx();
      import c.*

      val req  = GetSubcontractorList(cisId = "cis-123")
      val boom = new RuntimeException("boom")

      when(repo.getSubcontractorList(eqTo("cis-123")))
        .thenReturn(Future.failed(boom))

      val ex = service.getSubcontractorList(req).failed.futureValue
      ex mustBe boom

      verify(repo).getSubcontractorList(eqTo("cis-123"))
      verifyNoMoreInteractions(repo)
    }
  }

}
