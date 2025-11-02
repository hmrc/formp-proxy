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

package uk.gov.hmrc.formpproxy.cis.utils

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.*
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.MonthlyReturn
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateNilMonthlyReturnRequest, CreateSubmissionRequest, UpdateSubmissionRequest}

import java.time.LocalDateTime

class CisFormpStubSpec extends SpecBase {

  trait Setup {
    val utils: StubUtils = mock[StubUtils]
    lazy val stub = new CisFormpStub(utils)

    def createMonthlyReturn(month: Int): MonthlyReturn =
      MonthlyReturn(
        monthlyReturnId = month.toLong,
        taxYear = 2025,
        taxMonth = month,
        nilReturnIndicator = Some("Y"),
        decEmpStatusConsidered = None,
        decAllSubsVerified = None,
        decInformationCorrect = Some("Y"),
        decNoMoreSubPayments = None,
        decNilReturnNoPayments = Some("Y"),
        status = Some("Submitted"),
        lastUpdate = Some(LocalDateTime.parse("2025-01-01T00:00:00")),
        amendment = Some("N"),
        supersededBy = None
      )
  }
  def setup: Setup = new Setup {}
  
  "getAllMonthlyReturns" - {
    "returns Jan/Feb/Mar via StubUtils" in {
      val s = setup; import s.*

      when(utils.generateMonthlyReturns(eqTo(1))).thenReturn(createMonthlyReturn(1))
      when(utils.generateMonthlyReturns(eqTo(2))).thenReturn(createMonthlyReturn(2))
      when(utils.generateMonthlyReturns(eqTo(3))).thenReturn(createMonthlyReturn(3))

      val out = stub.getAllMonthlyReturns("123").futureValue

      out.monthlyReturnList.map(_.taxMonth) mustBe Seq(1, 2, 3)

      verify(utils).generateMonthlyReturns(1)
      verify(utils).generateMonthlyReturns(2)
      verify(utils).generateMonthlyReturns(3)
    }
  }
  
  "createNilMonthlyReturn" - {
    "stores a STARTED nil return and bumps scheme version" in {
      val s = setup; import s.*

      val req = CreateNilMonthlyReturnRequest(
        instanceId = "123",
        taxYear = 2025,
        taxMonth = 2,
        decInformationCorrect = "Y",
        decNilReturnNoPayments = "Y"
      )

      val res = stub.createNilMonthlyReturn(req).futureValue
      res.status mustBe "STARTED"

      stub.getSchemeVersion("123") mustBe 1L

      val stored = stub.getStoredReturn("123", 2025, 2).value
      stored.monthlyReturnId mustBe 1000L         
      stored.nilReturnIndicator mustBe Some("Y")
      stored.decInformationCorrect mustBe Some("Y")
      stored.decNilReturnNoPayments mustBe Some("Y")
      stored.status mustBe Some("STARTED")
      stored.amendment mustBe Some("N")
      stored.lastUpdate.isDefined mustBe true
      stored.supersededBy mustBe None
    }

    "increments id and version on subsequent calls" in {
      val s = setup; import s.*

      val base = CreateNilMonthlyReturnRequest("123", 2025, 1, "Y", "Y")
      stub.createNilMonthlyReturn(base).futureValue
      stub.createNilMonthlyReturn(base.copy(taxMonth = 3)).futureValue

      stub.getSchemeVersion("123") mustBe 2L
      stub.getStoredReturn("123", 2025, 3).value.monthlyReturnId mustBe 1001L 
    }

    "reset clears state and sets id sequence to 1,000,000" in {
      val s = setup; import s.*

      val req = CreateNilMonthlyReturnRequest("123", 2025, 2, "Y", "Y")
      stub.createNilMonthlyReturn(req).futureValue

      stub.reset()

      stub.getSchemeVersion("123") mustBe 0L
      stub.getStoredReturn("123", 2025, 2) mustBe None

      stub.createNilMonthlyReturn(req.copy(taxMonth = 4)).futureValue
      stub.getStoredReturn("123", 2025, 4).value.monthlyReturnId mustBe 1_000_000L
    }
  }
  
  "createSubmission" - {
    "returns constant submission id '90001'" in {
      val s = setup; import s.*

      val req = CreateSubmissionRequest("123", 2024, 4)
      stub.createSubmission(req).futureValue mustBe "90001"
    }
  }

  "updateMonthlyReturnSubmission" - {
    "completes successfully" in {
      val s = setup; import s.*

      val req = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
        submittableStatus = "ACCEPTED"
      )
      stub.updateMonthlyReturnSubmission(req).futureValue mustBe ((): Unit)
    }
  }
}
