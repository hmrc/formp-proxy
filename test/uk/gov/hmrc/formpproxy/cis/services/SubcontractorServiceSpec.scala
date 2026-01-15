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
import uk.gov.hmrc.formpproxy.cis.models.requests.UpdateSubcontractorRequest
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource

import java.time.LocalDateTime
import scala.concurrent.Future

class SubcontractorServiceSpec extends SpecBase {

  trait Setup {
    val repo: CisMonthlyReturnSource = mock[CisMonthlyReturnSource]
    lazy val service                 = new SubcontractorService(repo)
  }
  def setup: Setup = new Setup {}

  "SubcontractorService#updateSubcontractor" - {

    "delegates to repo and returns Unit" in {
      val s = setup; import s.*

      val req = UpdateSubcontractorRequest(
        utr = "1234567890",
        pageVisited = 1,
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
      val s = setup; import s.*

      val req = UpdateSubcontractorRequest(
        utr = "1234567890",
        pageVisited = 1,
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
}
