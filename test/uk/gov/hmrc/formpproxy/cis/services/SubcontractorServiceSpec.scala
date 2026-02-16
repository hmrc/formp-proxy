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
import uk.gov.hmrc.formpproxy.cis.models.SoleTrader
import uk.gov.hmrc.formpproxy.cis.models.requests.CreateAndUpdateSubcontractorRequest
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource
import uk.gov.hmrc.formpproxy.cis.models.GetSubcontractorList
import uk.gov.hmrc.formpproxy.cis.models.response.GetSubcontractorListResponse

import scala.concurrent.Future

class SubcontractorServiceSpec extends SpecBase {

  case class Ctx() {
    val repo: CisMonthlyReturnSource = mock[CisMonthlyReturnSource]
    lazy val service                 = new SubcontractorService(repo)
  }
  "SubcontractorService#createAndUpdateSubcontractor" - {

    "delegates to repo and returns Unit" in {
      val c = Ctx(); import c.*

      val req = CreateAndUpdateSubcontractorRequest(
        cisId = "123",
        subcontractorType = SoleTrader,
        utr = Some("1234567890"),
        partnerUtr = Some("9999999999"),
        crn = Some("CRN123"),
        nino = Some("AA123456A"),
        partnershipTradingName = Some("My Partnership"),
        tradingName = Some("ACME"),
        addressLine1 = Some("1 Main Street"),
        addressLine2 = None,
        city = Some("London"),
        county = Some("Greater London"),
        postcode = Some("AA1 1AA"),
        emailAddress = None,
        phoneNumber = None,
        mobilePhoneNumber = Some("07123456789"),
        worksReferenceNumber = Some("34567")
      )

      when(repo.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest]))
        .thenReturn(Future.successful(()))

      service.createAndUpdateSubcontractor(req).futureValue mustBe ((): Unit)

      verify(repo).createAndUpdateSubcontractor(eqTo(req))
    }

    "propagates failure from repo" in {
      val c = Ctx(); import c.*

      val req = CreateAndUpdateSubcontractorRequest(
        cisId = "123",
        subcontractorType = SoleTrader,
        utr = Some("1234567890"),
        partnerUtr = Some("9999999999"),
        crn = Some("CRN123"),
        nino = Some("AA123456A"),
        partnershipTradingName = Some("My Partnership"),
        tradingName = Some("ACME"),
        addressLine1 = Some("1 Main Street"),
        addressLine2 = None,
        city = Some("London"),
        county = Some("Greater London"),
        postcode = Some("AA1 1AA"),
        emailAddress = None,
        phoneNumber = None,
        mobilePhoneNumber = Some("07123456789"),
        worksReferenceNumber = Some("34567")
      )

      when(repo.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val fut = service.createAndUpdateSubcontractor(req)
      whenReady(fut.failed) { ex =>
        ex.getMessage mustBe "boom"
      }

      verify(repo).createAndUpdateSubcontractor(eqTo(req))
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
