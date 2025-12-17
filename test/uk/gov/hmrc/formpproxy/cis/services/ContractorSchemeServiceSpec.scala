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
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.requests.ApplyPrepopulationRequest
import uk.gov.hmrc.formpproxy.cis.models.{Company, ContractorScheme, CreateContractorSchemeParams, Partnership, SoleTrader, Trust, UpdateContractorSchemeParams}
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource

import java.time.Instant
import scala.concurrent.Future

final class ContractorSchemeServiceSpec extends SpecBase {

  case class Ctx() {
    val repo    = mock[CisMonthlyReturnSource]
    val service = new ContractorSchemeService(repo)
  }

  private def mkScheme(
    schemeId: Int,
    instanceId: String,
    accountsOfficeReference: String = "111111111",
    taxOfficeNumber: String = "0001"
  ): ContractorScheme =
    ContractorScheme(
      schemeId = schemeId,
      instanceId = instanceId,
      accountsOfficeReference = accountsOfficeReference,
      taxOfficeNumber = taxOfficeNumber,
      taxOfficeReference = "AB12345",
      utr = Some("1234567890"),
      name = Some("Test Contractor"),
      emailAddress = Some("test@example.com"),
      displayWelcomePage = Some("Y"),
      prePopCount = Some(5),
      prePopSuccessful = Some("Y"),
      subcontractorCounter = Some(10),
      verificationBatchCounter = Some(2),
      lastUpdate = Some(Instant.parse("2025-12-04T10:15:30Z")),
      version = Some(1)
    )

  private def mkApplyReq: ApplyPrepopulationRequest =
    ApplyPrepopulationRequest(
      schemeId = 789,
      instanceId = "abc-123",
      accountsOfficeReference = "111111111",
      taxOfficeNumber = "123",
      taxOfficeReference = "AB456",
      utr = Some("9876543210"),
      name = "Test Contractor",
      emailAddress = Some("test@test.com"),
      displayWelcomePage = Some("N"),
      prePopCount = 5,
      prePopSuccessful = "Y",
      version = 1,
      subcontractorTypes = Seq(SoleTrader, Company)
    )

  "ContractorSchemeService getScheme" - {

    "returns Some(ContractorScheme) when repository finds a scheme (happy path)" in {
      val c      = Ctx()
      val scheme = mkScheme(schemeId = 123, instanceId = "abc-123")

      when(c.repo.getScheme(eqTo("abc-123")))
        .thenReturn(Future.successful(Some(scheme)))

      val out = c.service.getScheme("abc-123").futureValue
      out mustBe Some(scheme)

      verify(c.repo).getScheme(eqTo("abc-123"))
      verifyNoMoreInteractions(c.repo)
    }

    "returns None when repository does not find a scheme" in {
      val c = Ctx()

      when(c.repo.getScheme(eqTo("unknown-123")))
        .thenReturn(Future.successful(None))

      val out = c.service.getScheme("unknown-123").futureValue
      out mustBe None

      verify(c.repo).getScheme(eqTo("unknown-123"))
      verifyNoMoreInteractions(c.repo)
    }

    "propagates failures from the repository" in {
      val c    = Ctx()
      val boom = new RuntimeException("database failed")

      when(c.repo.getScheme(eqTo("abc-123")))
        .thenReturn(Future.failed(boom))

      val ex = c.service.getScheme("abc-123").failed.futureValue
      ex mustBe boom

      verify(c.repo).getScheme(eqTo("abc-123"))
      verifyNoMoreInteractions(c.repo)
    }

    "returns scheme with all fields populated" in {
      val c      = Ctx()
      val scheme = ContractorScheme(
        schemeId = 999,
        instanceId = "full-123",
        accountsOfficeReference = "123456789",
        taxOfficeNumber = "0099",
        taxOfficeReference = "ZZ99999",
        utr = Some("9876543210"),
        name = Some("Full Contractor"),
        emailAddress = Some("full@example.com"),
        displayWelcomePage = Some("Y"),
        prePopCount = Some(100),
        prePopSuccessful = Some("Y"),
        subcontractorCounter = Some(50),
        verificationBatchCounter = Some(5),
        lastUpdate = Some(Instant.parse("2025-01-01T00:00:00Z")),
        version = Some(10)
      )

      when(c.repo.getScheme(eqTo("full-123")))
        .thenReturn(Future.successful(Some(scheme)))

      val out = c.service.getScheme("full-123").futureValue
      out mustBe Some(scheme)
      out.get.schemeId mustBe 999
      out.get.utr mustBe Some("9876543210")
      out.get.version mustBe Some(10)
    }

    "returns scheme with optional fields as None" in {
      val c      = Ctx()
      val scheme = ContractorScheme(
        schemeId = 777,
        instanceId = "sparse-123",
        accountsOfficeReference = "987654321",
        taxOfficeNumber = "0077",
        taxOfficeReference = "CC77777",
        utr = None,
        name = None,
        emailAddress = None,
        displayWelcomePage = None,
        prePopCount = None,
        prePopSuccessful = None,
        subcontractorCounter = None,
        verificationBatchCounter = None,
        lastUpdate = None,
        version = None
      )

      when(c.repo.getScheme(eqTo("sparse-123")))
        .thenReturn(Future.successful(Some(scheme)))

      val out = c.service.getScheme("sparse-123").futureValue
      out mustBe Some(scheme)
      out.get.utr mustBe None
      out.get.name mustBe None
      out.get.version mustBe None
    }
  }

  "ContractorSchemeService updateScheme" - {

    "returns version when repository successfully updates a scheme (happy path)" in {
      val c            = Ctx()
      val updateParams = UpdateContractorSchemeParams(
        schemeId = 123,
        instanceId = "abc-123",
        accountsOfficeReference = "111111111",
        taxOfficeNumber = "0001",
        taxOfficeReference = "AB12345",
        utr = Some("1234567890"),
        name = Some("Updated Contractor"),
        emailAddress = Some("updated@example.com"),
        displayWelcomePage = Some("Y"),
        prePopCount = Some(10),
        prePopSuccessful = Some("Y"),
        version = Some(1)
      )

      when(c.repo.updateScheme(eqTo(updateParams)))
        .thenReturn(Future.successful(2))

      val out = c.service.updateScheme(updateParams).futureValue
      out mustBe 2

      verify(c.repo).updateScheme(eqTo(updateParams))
      verifyNoMoreInteractions(c.repo)
    }

    "updates scheme with no optional fields" in {
      val c            = Ctx()
      val updateParams = UpdateContractorSchemeParams(
        schemeId = 456,
        instanceId = "sparse-123",
        accountsOfficeReference = "555555555",
        taxOfficeNumber = "0055",
        taxOfficeReference = "YY55555",
        version = Some(2)
      )

      when(c.repo.updateScheme(eqTo(updateParams)))
        .thenReturn(Future.successful(3))

      val out = c.service.updateScheme(updateParams).futureValue
      out mustBe 3

      verify(c.repo).updateScheme(eqTo(updateParams))
      verifyNoMoreInteractions(c.repo)
    }

    "propagates failures from the repository" in {
      val c            = Ctx()
      val boom         = new RuntimeException("database failed")
      val updateParams = UpdateContractorSchemeParams(
        schemeId = 123,
        instanceId = "abc-123",
        accountsOfficeReference = "111111111",
        taxOfficeNumber = "0001",
        taxOfficeReference = "AB12345",
        version = Some(1)
      )

      when(c.repo.updateScheme(eqTo(updateParams)))
        .thenReturn(Future.failed(boom))

      val ex = c.service.updateScheme(updateParams).failed.futureValue
      ex mustBe boom

      verify(c.repo).updateScheme(eqTo(updateParams))
      verifyNoMoreInteractions(c.repo)
    }
  }

  "ContractorSchemeService createScheme" - {

    "returns schemeId when repository successfully creates a scheme (happy path)" in {
      val c            = Ctx()
      val createParams = CreateContractorSchemeParams(
        instanceId = "abc-123",
        accountsOfficeReference = "111111111",
        taxOfficeNumber = "0001",
        taxOfficeReference = "AB12345",
        utr = Some("1234567890"),
        name = Some("Test Contractor"),
        emailAddress = Some("test@example.com"),
        displayWelcomePage = Some("Y"),
        prePopCount = Some(5),
        prePopSuccessful = Some("Y")
      )

      when(c.repo.createScheme(eqTo(createParams)))
        .thenReturn(Future.successful(123))

      val out = c.service.createScheme(createParams).futureValue
      out mustBe 123

      verify(c.repo).createScheme(eqTo(createParams))
      verifyNoMoreInteractions(c.repo)
    }

    "creates scheme with no optional fields" in {
      val c            = Ctx()
      val createParams = CreateContractorSchemeParams(
        instanceId = "sparse-123",
        accountsOfficeReference = "555555555",
        taxOfficeNumber = "0055",
        taxOfficeReference = "YY55555"
      )

      when(c.repo.createScheme(eqTo(createParams)))
        .thenReturn(Future.successful(777))

      val out = c.service.createScheme(createParams).futureValue
      out mustBe 777

      verify(c.repo).createScheme(eqTo(createParams))
      verifyNoMoreInteractions(c.repo)
    }

    "propagates failures from the repository" in {
      val c            = Ctx()
      val boom         = new RuntimeException("database failed")
      val createParams = CreateContractorSchemeParams(
        instanceId = "abc-123",
        accountsOfficeReference = "111111111",
        taxOfficeNumber = "0001",
        taxOfficeReference = "AB12345"
      )

      when(c.repo.createScheme(eqTo(createParams)))
        .thenReturn(Future.failed(boom))

      val ex = c.service.createScheme(createParams).failed.futureValue
      ex mustBe boom

      verify(c.repo).createScheme(eqTo(createParams))
      verifyNoMoreInteractions(c.repo)
    }
  }

  "ContractorSchemeService updateSchemeVersion" - {

    "returns version when repository successfully updates scheme version (happy path)" in {
      val c = Ctx()

      when(c.repo.updateSchemeVersion(eqTo("abc-123"), eqTo(1)))
        .thenReturn(Future.successful(2))

      val out = c.service.updateSchemeVersion("abc-123", 1).futureValue
      out mustBe 2

      verify(c.repo).updateSchemeVersion(eqTo("abc-123"), eqTo(1))
      verifyNoMoreInteractions(c.repo)
    }

    "propagates failures from the repository" in {
      val c    = Ctx()
      val boom = new RuntimeException("database failed")

      when(c.repo.updateSchemeVersion(eqTo("abc-123"), eqTo(1)))
        .thenReturn(Future.failed(boom))

      val ex = c.service.updateSchemeVersion("abc-123", 1).failed.futureValue
      ex mustBe boom

      verify(c.repo).updateSchemeVersion(eqTo("abc-123"), eqTo(1))
      verifyNoMoreInteractions(c.repo)
    }
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

  "ContractorSchemeService applyPrepopulation" - {

    "returns version when repository successfully applies prepopulation (happy path)" in {
      val c   = Ctx()
      val req = mkApplyReq

      when(c.repo.applyPrepopulation(eqTo(req)))
        .thenReturn(Future.successful(2))

      val out = c.service.applyPrepopulation(req).futureValue
      out mustBe 2

      verify(c.repo).applyPrepopulation(eqTo(req))
      verifyNoMoreInteractions(c.repo)
    }

    "propagates failures from the repository" in {
      val c    = Ctx()
      val req  = mkApplyReq
      val boom = new RuntimeException("database failed")

      when(c.repo.applyPrepopulation(eqTo(req)))
        .thenReturn(Future.failed(boom))

      val ex = c.service.applyPrepopulation(req).failed.futureValue
      ex mustBe boom

      verify(c.repo).applyPrepopulation(eqTo(req))
      verifyNoMoreInteractions(c.repo)
    }
  }
}
