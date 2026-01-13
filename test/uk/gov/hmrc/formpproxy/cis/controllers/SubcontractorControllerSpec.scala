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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.formpproxy.actions.FakeAuthAction
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.requests.UpdateSubcontractorRequest
import uk.gov.hmrc.formpproxy.cis.models.response.UpdateSubcontractorResponse
import uk.gov.hmrc.formpproxy.cis.services.SubcontractorService

import java.time.LocalDateTime
import scala.concurrent.Future

class SubcontractorControllerSpec extends SpecBase {

  trait Setup {
    val service: SubcontractorService = mock[SubcontractorService]
    val auth: FakeAuthAction          = new FakeAuthAction(cc.parsers)
    lazy val controller               = new SubcontractorController(auth, service, cc)
  }

  def setup: Setup = new Setup {}

  "POST /subcontractor/update (updateSubcontractor)" - {

    "return 200 OK with newVersion on valid payload" in {
      val s = setup; import s.*

      when(service.updateSubcontractor(any[UpdateSubcontractorRequest]))
        .thenReturn(Future.successful(12))

      val json = Json.toJson(
        UpdateSubcontractorRequest(
          utr = "1234567890",
          pageVisited = 2,
          partnerUtr = None,
          crn = None,
          firstName = Some("John"),
          nino = Some("AA123456A"),
          secondName = None,
          surname = Some("Smith"),
          partnershipTradingName = None,
          tradingName = Some("ACME"),
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
          updatedTaxTreatment = None,
          currentVersion = 7
        )
      )

      val result = controller
        .updateSubcontractor()
        .apply(postJson("/subcontractor/update", json))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(UpdateSubcontractorResponse(newVersion = 12))
      verify(service).updateSubcontractor(any[UpdateSubcontractorRequest])
    }

    "returns 400 BadRequest for invlid JSON" in {
      val s = setup; import s.*

      val bad = Json.obj("bad" -> "json")

      val result = controller
        .updateSubcontractor()
        .apply(postJson("/subcontractor/update", bad))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      verify(service, never()).updateSubcontractor(any[UpdateSubcontractorRequest])
    }

    "maps service failure to 500 with error body" in {
      val s = setup; import s.*

      when(service.updateSubcontractor(any[UpdateSubcontractorRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val json = Json.toJson(
        UpdateSubcontractorRequest(
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
          subbieResourceRef = 1,
          matched = None,
          autoVerified = None,
          verified = None,
          verificationNumber = None,
          taxTreatment = None,
          verificationDate = None,
          updatedTaxTreatment = None,
          currentVersion = 0
        )
      )

      val result = controller
        .updateSubcontractor()
        .apply(postJson("/subcontractor/update", json))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")
    }
  }
}
