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

import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.mockito.Mockito.*
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.FakeAuthAction
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.requests.CreateAndUpdateSubcontractorRequest
import uk.gov.hmrc.formpproxy.cis.models.SoleTrader
import uk.gov.hmrc.formpproxy.cis.services.SubcontractorService
import uk.gov.hmrc.formpproxy.cis.models.GetSubcontractorList
import uk.gov.hmrc.formpproxy.cis.models.response.GetSubcontractorListResponse
import uk.gov.hmrc.formpproxy.cis.models.Subcontractor

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
          utr = Some("1234567890"),
          partnerUtr = Some("9999999999"),
          crn = Some("CRN123"),
          firstName = Some("John"),
          secondName = Some("Q"),
          surname = Some("Smith"),
          nino = Some("AA123456A"),
          partnershipTradingName = Some("My Partnership"),
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
          utr = Some("1234567890"),
          partnerUtr = Some("9999999999"),
          crn = Some("CRN123"),
          firstName = Some("John"),
          secondName = Some("Q"),
          surname = Some("Smith"),
          nino = Some("AA123456A"),
          partnershipTradingName = Some("My Partnership"),
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
      )

      val result = controller
        .createAndUpdateSubcontractor()
        .apply(postJson("/subcontractor/create-and-update", json))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")
    }
  }

  "GET /cis/subcontractors/:cisId (getSubcontractorList)" - {

    "returns 200 OK with subcontractor list on success" in {
      val s = setup;
      import s.*

      val cisId = "cis-123"

      val response = GetSubcontractorListResponse(
        subcontractors = List(
          Subcontractor(
            subcontractorId = 1L,
            subbieResourceRef = Some(10),
            subcontractorType = Some("soletrader"),
            utr = Some("1234567890"),
            pageVisited = Some(2),
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
            country = Some("United Kingdom"),
            postCode = Some("AA1 1AA"),
            emailAddress = None,
            phoneNumber = None,
            mobilePhoneNumber = None,
            worksReferenceNumber = None,
            version = Some(1),
            taxTreatment = None,
            updatedTaxTreatment = None,
            verificationNumber = None,
            createDate = None,
            lastUpdate = None,
            matched = None,
            verified = None,
            autoVerified = None,
            verificationDate = None,
            lastMonthlyReturnDate = None,
            pendingVerifications = Some(0)
          )
        )
      )

      when(mockService.getSubcontractorList(eqTo(GetSubcontractorList(cisId))))
        .thenReturn(Future.successful(response))

      val req    = FakeRequest(GET, s"/cis/subcontractors/$cisId")
      val result = controller.getSubcontractorList(cisId).apply(req)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(response)

      verify(mockService).getSubcontractorList(eqTo(GetSubcontractorList(cisId)))
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 when service fails" in {
      val s = setup;
      import s.*

      val cisId = "cis-123"

      when(mockService.getSubcontractorList(eqTo(GetSubcontractorList(cisId))))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req    = FakeRequest(GET, s"/cis/subcontractors/$cisId")
      val result = controller.getSubcontractorList(cisId).apply(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")

      verify(mockService).getSubcontractorList(eqTo(GetSubcontractorList(cisId)))
      verifyNoMoreInteractions(mockService)
    }
  }

}
