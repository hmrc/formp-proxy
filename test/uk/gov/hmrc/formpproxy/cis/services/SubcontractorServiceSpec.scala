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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.{Company, Partnership, SoleTrader, Trust}
import uk.gov.hmrc.formpproxy.cis.models.GetSubcontractorList
import uk.gov.hmrc.formpproxy.cis.models.CreateAndUpdateSubcontractorDatabaseRecord
import uk.gov.hmrc.formpproxy.cis.models.requests.CreateAndUpdateSubcontractorRequest
import uk.gov.hmrc.formpproxy.cis.models.response.GetSubcontractorListResponse
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource

import scala.concurrent.Future

class SubcontractorServiceSpec extends SpecBase {

  case class Ctx() {
    val repo: CisMonthlyReturnSource = mock[CisMonthlyReturnSource]
    lazy val service                 = new SubcontractorService(repo)
  }

  "SubcontractorService#createAndUpdateSubcontractor" - {

    "maps SoleTraderRequest -> CreateAndUpdateSubcontractorDatabaseRecord and delegates to repo" in {
      val c = Ctx(); import c.*

      val req: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.SoleTraderRequest(
          cisId = "123",
          utr = Some("1234567890"),
          nino = Some("AA123456A"),
          firstName = Some("John"),
          secondName = Some("Q"),
          surname = Some("Smith"),
          tradingName = Some("ACME"),
          addressLine1 = Some("1 Main Street"),
          city = Some("London"),
          county = Some("Greater London"),
          country = Some("United Kingdom"),
          postcode = Some("AA1 1AA"),
          mobilePhoneNumber = Some("07123456789"),
          worksReferenceNumber = Some("34567")
        )

      when(repo.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorDatabaseRecord]))
        .thenReturn(Future.successful(()))

      service.createAndUpdateSubcontractor(req).futureValue mustBe ((): Unit)

      val captor = ArgumentCaptor.forClass(classOf[CreateAndUpdateSubcontractorDatabaseRecord])
      verify(repo).createAndUpdateSubcontractor(captor.capture())

      captor.getValue mustBe CreateAndUpdateSubcontractorDatabaseRecord(
        cisId = "123",
        subcontractorType = SoleTrader,
        utr = Some("1234567890"),
        partnerUtr = None,
        crn = None,
        firstName = Some("John"),
        secondName = Some("Q"),
        surname = Some("Smith"),
        nino = Some("AA123456A"),
        partnershipTradingName = None,
        tradingName = Some("ACME"),
        addressLine1 = Some("1 Main Street"),
        addressLine2 = None,
        city = Some("London"),
        county = Some("Greater London"),
        country = Some("United Kingdom"),
        postcode = Some("AA1 1AA"),
        emailAddress = None,
        phoneNumber = None,
        mobilePhoneNumber = Some("07123456789"),
        worksReferenceNumber = Some("34567")
      )
    }

    "maps CompanyRequest -> CreateAndUpdateSubcontractorDatabaseRecord and delegates to repo" in {
      val c = Ctx(); import c.*

      val req: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.CompanyRequest(
          cisId = "123",
          utr = Some("1234567890"),
          crn = Some("CRN123"),
          tradingName = Some("ABC Ltd"),
          addressLine1 = Some("10 Downing Street"),
          city = Some("London"),
          county = Some("Greater London"),
          country = Some("United Kingdom"),
          postcode = Some("SW1A 2AA"),
          emailAddress = Some("test@test.com"),
          phoneNumber = Some("01234567890"),
          mobilePhoneNumber = Some("07123456789"),
          worksReferenceNumber = Some("WRN-001")
        )

      when(repo.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorDatabaseRecord]))
        .thenReturn(Future.successful(()))

      service.createAndUpdateSubcontractor(req).futureValue mustBe ((): Unit)

      val captor = ArgumentCaptor.forClass(classOf[CreateAndUpdateSubcontractorDatabaseRecord])
      verify(repo).createAndUpdateSubcontractor(captor.capture())

      captor.getValue mustBe CreateAndUpdateSubcontractorDatabaseRecord(
        cisId = "123",
        subcontractorType = Company,
        utr = Some("1234567890"),
        partnerUtr = None,
        crn = Some("CRN123"),
        firstName = None,
        secondName = None,
        surname = None,
        nino = None,
        partnershipTradingName = None,
        tradingName = Some("ABC Ltd"),
        addressLine1 = Some("10 Downing Street"),
        addressLine2 = None,
        city = Some("London"),
        county = Some("Greater London"),
        country = Some("United Kingdom"),
        postcode = Some("SW1A 2AA"),
        emailAddress = Some("test@test.com"),
        phoneNumber = Some("01234567890"),
        mobilePhoneNumber = Some("07123456789"),
        worksReferenceNumber = Some("WRN-001")
      )
    }

    "maps PartnershipRequest -> CreateAndUpdateSubcontractorDatabaseRecord and delegates to repo" in {
      val c = Ctx();
      import c.*

      val req: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.PartnershipRequest(
          cisId = "123",
          utr = Some("1111111111"),
          partnerUtr = Some("2222222222"),
          partnerCrn = Some("CRN123"),
          partnerNino = Some("AA123456A"),
          partnershipTradingName = Some("My Partnership"),
          partnerTradingName = Some("Nominated Partner"),
          addressLine1 = Some("1 Main Street"),
          city = Some("London"),
          county = Some("Greater London"),
          country = Some("United Kingdom"),
          postcode = Some("AA1 1AA"),
          worksReferenceNumber = Some("WRN-123")
        )

      when(repo.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorDatabaseRecord]))
        .thenReturn(Future.successful(()))

      service.createAndUpdateSubcontractor(req).futureValue mustBe ((): Unit)

      val captor = ArgumentCaptor.forClass(classOf[CreateAndUpdateSubcontractorDatabaseRecord])
      verify(repo).createAndUpdateSubcontractor(captor.capture())

      captor.getValue mustBe CreateAndUpdateSubcontractorDatabaseRecord(
        cisId = "123",
        subcontractorType = Partnership,
        utr = Some("1111111111"),
        partnerUtr = Some("2222222222"),
        crn = Some("CRN123"),
        firstName = None,
        secondName = None,
        surname = None,
        nino = Some("AA123456A"),
        partnershipTradingName = Some("My Partnership"),
        tradingName = Some("Nominated Partner"),
        addressLine1 = Some("1 Main Street"),
        addressLine2 = None,
        city = Some("London"),
        county = Some("Greater London"),
        country = Some("United Kingdom"),
        postcode = Some("AA1 1AA"),
        emailAddress = None,
        phoneNumber = None,
        mobilePhoneNumber = None,
        worksReferenceNumber = Some("WRN-123")
      )
    }

    "maps TrustRequest -> CreateAndUpdateSubcontractorDatabaseRecord and delegates to repo" in {
      val c = Ctx();
      import c.*

      val req: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.TrustRequest(
          cisId = "123",
          utr = Some("1111111111"),
          trustTradingName = Some("The Big Trust"),
          addressLine1 = Some("1 Trust Street"),
          addressLine2 = Some("Line 2"),
          city = Some("London"),
          county = Some("Greater London"),
          country = Some("United Kingdom"),
          postcode = Some("AA1 1AA"),
          emailAddress = Some("trust@test.com"),
          phoneNumber = Some("02000000000"),
          mobilePhoneNumber = Some("07123456789"),
          worksReferenceNumber = Some("WRN-TRUST-1")
        )

      when(repo.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorDatabaseRecord]))
        .thenReturn(Future.successful(()))

      service.createAndUpdateSubcontractor(req).futureValue mustBe ((): Unit)

      val captor = ArgumentCaptor.forClass(classOf[CreateAndUpdateSubcontractorDatabaseRecord])
      verify(repo).createAndUpdateSubcontractor(captor.capture())

      captor.getValue mustBe CreateAndUpdateSubcontractorDatabaseRecord(
        cisId = "123",
        subcontractorType = Trust,
        utr = Some("1111111111"),
        partnerUtr = None,
        crn = None,
        firstName = None,
        secondName = None,
        surname = None,
        nino = None,
        partnershipTradingName = None,
        tradingName = Some("The Big Trust"),
        addressLine1 = Some("1 Trust Street"),
        addressLine2 = Some("Line 2"),
        city = Some("London"),
        county = Some("Greater London"),
        country = Some("United Kingdom"),
        postcode = Some("AA1 1AA"),
        emailAddress = Some("trust@test.com"),
        phoneNumber = Some("02000000000"),
        mobilePhoneNumber = Some("07123456789"),
        worksReferenceNumber = Some("WRN-TRUST-1")
      )
    }

    "propagates failure from repo" in {
      val c = Ctx(); import c.*

      val req: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.SoleTraderRequest(
          cisId = "123",
          utr = Some("1234567890")
        )

      when(repo.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorDatabaseRecord]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val fut = service.createAndUpdateSubcontractor(req)
      whenReady(fut.failed) { ex =>
        ex.getMessage mustBe "boom"
      }

      verify(repo).createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorDatabaseRecord])
    }
  }

  "SubcontractorService#getSubcontractorList" - {

    "delegates to repo with cisId and returns response" in {
      val c = Ctx(); import c.*

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
      val c = Ctx(); import c.*

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
