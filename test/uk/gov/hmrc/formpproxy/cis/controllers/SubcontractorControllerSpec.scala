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

package uk.gov.hmrc.formpproxy.cis.controllers

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.FakeAuthAction
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.requests.CreateAndUpdateSubcontractorRequest
import uk.gov.hmrc.formpproxy.cis.models.SoleTrader
import uk.gov.hmrc.formpproxy.cis.services.SubcontractorService
import scala.concurrent.Future

class SubcontractorControllerSpec extends SpecBase {

  trait Setup {
    val mockService: SubcontractorService = mock[SubcontractorService]
    val auth: FakeAuthAction              = new FakeAuthAction(cc.parsers)
    lazy val controller                   = new SubcontractorController(auth, mockService, cc)
  }

  def setup: Setup = new Setup {}

  "POST /subcontractor/update (createAndUpdateSubcontractor)" - {

    "returns 204 NoContent on valid payload" in {
      val s = setup; import s.*

      when(mockService.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest]))
        .thenReturn(Future.successful(()))

      val json = Json.toJson(
        CreateAndUpdateSubcontractorRequest(
          cisId = "123",
          subcontractorType = SoleTrader,
          firstName = Some("John"),
          secondName = None,
          surname = Some("Smith"),
          tradingName = Some("ACME"),
          addressLine1 = Some("1 Main Street"),
          addressLine2 = None,
          addressLine3 = None,
          addressLine4 = None,
          postcode = Some("AA1 1AA"),
          nino = Some("AA123456A"),
          utr = Some("1234567890"),
          worksReferenceNumber = Some("34567"),
          emailAddress = None,
          phoneNumber = None
        )
      )

      val result = controller
        .createAndUpdateSubcontractor()
        .apply(postJson("/subcontractor/create-and-update", json))

      status(result) mustBe NO_CONTENT
      verify(mockService).createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest])
    }

    "returns 400 BadRequest for invlid JSON" in {
      val s = setup; import s.*

      val bad = Json.obj("bad" -> "json")

      val result = controller
        .createAndUpdateSubcontractor()
        .apply(postJson("/subcontractor/create-and-update", bad))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      verify(mockService, never()).createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest])
    }

    "maps service failure to 500 with error body" in {
      val s = setup; import s.*

      when(mockService.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val json = Json.toJson(
        CreateAndUpdateSubcontractorRequest(
          cisId = "123",
          subcontractorType = SoleTrader,
          firstName = Some("John"),
          secondName = None,
          surname = Some("Smith"),
          tradingName = Some("ACME"),
          addressLine1 = Some("1 Main Street"),
          addressLine2 = None,
          addressLine3 = None,
          addressLine4 = None,
          postcode = Some("AA1 1AA"),
          nino = Some("AA123456A"),
          utr = Some("1234567890"),
          worksReferenceNumber = Some("34567"),
          emailAddress = None,
          phoneNumber = None
        )
      )

      val result = controller
        .createAndUpdateSubcontractor()
        .apply(postJson("/subcontractor/create-and-update", json))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")
    }
  }
}
