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
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.FakeAuthAction
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.{Company, Partnership, SoleTrader, Trust}
import uk.gov.hmrc.formpproxy.cis.models.requests.UpdateSubcontractorRequest
import uk.gov.hmrc.formpproxy.cis.services.SubcontractorService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.formpproxy.cis.models.GetSubcontractorList
import uk.gov.hmrc.formpproxy.cis.models.response.GetSubcontractorListResponse
import uk.gov.hmrc.formpproxy.cis.models.Subcontractor

import java.time.LocalDateTime
import scala.concurrent.Future

class SubcontractorControllerSpec extends SpecBase {

  trait Setup {
    val mockService: SubcontractorService = mock[SubcontractorService]
    val auth: FakeAuthAction              = new FakeAuthAction(cc.parsers)
    lazy val controller                   = new SubcontractorController(auth, mockService, cc)
  }

  def setup: Setup = new Setup {}

  "POST /subcontractor/create (createSubcontractor)" - {

    "returns 200 with version when service succeeds with SoleTrader (happy path)" in new Setup {
      when(mockService.createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1)))
        .thenReturn(Future.successful(2))

      val req                 =
        FakeRequest(POST, "/subcontractor/create").withBody(
          Json.obj("schemeId" -> 123, "subcontractorType" -> "soletrader", "version" -> 1)
        )
      val res: Future[Result] = controller.createSubcontractor(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "subbieResourceRef").as[Int] mustBe 2
      verify(mockService).createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with version when service succeeds with Company" in new Setup {
      when(mockService.createSubcontractor(eqTo(456), eqTo(Company), eqTo(3)))
        .thenReturn(Future.successful(4))

      val req                 =
        FakeRequest(POST, "/subcontractor/create").withBody(
          Json.obj("schemeId" -> 456, "subcontractorType" -> "company", "version" -> 3)
        )
      val res: Future[Result] = controller.createSubcontractor(req)

      status(res) mustBe CREATED

      (contentAsJson(res) \ "subbieResourceRef").as[Int] mustBe 4
      verify(mockService).createSubcontractor(eqTo(456), eqTo(Company), eqTo(3))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with version when service succeeds with Partnership" in new Setup {
      when(mockService.createSubcontractor(eqTo(789), eqTo(Partnership), eqTo(5)))
        .thenReturn(Future.successful(6))

      val req                 =
        FakeRequest(POST, "/subcontractor/create").withBody(
          Json.obj("schemeId" -> 789, "subcontractorType" -> "partnership", "version" -> 5)
        )
      val res: Future[Result] = controller.createSubcontractor(req)

      status(res) mustBe CREATED
      (contentAsJson(res) \ "subbieResourceRef").as[Int] mustBe 6
      verify(mockService).createSubcontractor(eqTo(789), eqTo(Partnership), eqTo(5))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with version when service succeeds with Trust" in new Setup {
      when(mockService.createSubcontractor(eqTo(999), eqTo(Trust), eqTo(7)))
        .thenReturn(Future.successful(8))

      val req                 =
        FakeRequest(POST, "/subcontractor/create").withBody(
          Json.obj("schemeId" -> 999, "subcontractorType" -> "trust", "version" -> 7)
        )
      val res: Future[Result] = controller.createSubcontractor(req)

      status(res) mustBe CREATED
      (contentAsJson(res) \ "subbieResourceRef").as[Int] mustBe 8
      verify(mockService).createSubcontractor(eqTo(999), eqTo(Trust), eqTo(7))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is invalid" in new Setup {
      val invalidJson         = Json.obj("subcontractorType" -> "soletrader")
      val req                 = FakeRequest(POST, "/subcontractor/create").withBody(invalidJson)
      val res: Future[Result] = controller.createSubcontractor(req)

      status(res) mustBe BAD_REQUEST
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "returns 400 when JSON body has wrong types" in new Setup {
      val invalidJson         = Json.obj("schemeId" -> 123, "subcontractorType" -> "soletrader", "version" -> "not-a-number")
      val req                 = FakeRequest(POST, "/subcontractor/create").withBody(invalidJson)
      val res: Future[Result] = controller.createSubcontractor(req)

      status(res) mustBe BAD_REQUEST
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val err = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)
      when(mockService.createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1)))
        .thenReturn(Future.failed(err))

      val req                 =
        FakeRequest(POST, "/subcontractor/create").withBody(
          Json.obj("schemeId" -> 123, "subcontractorType" -> "soletrader", "version" -> 1)
        )
      val res: Future[Result] = controller.createSubcontractor(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
      verify(mockService).createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1))
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1)))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req                 =
        FakeRequest(POST, "/subcontractor/create").withBody(
          Json.obj("schemeId" -> 123, "subcontractorType" -> "soletrader", "version" -> 1)
        )
      val res: Future[Result] = controller.createSubcontractor(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
      verify(mockService).createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1))
      verifyNoMoreInteractions(mockService)
    }
  }

  "POST /subcontractor/update (updateSubcontractor)" - {

    "returns 204 NoContent on valid payload" in {
      val s = setup; import s.*

      when(mockService.updateSubcontractor(any[UpdateSubcontractorRequest]))
        .thenReturn(Future.successful(()))

      val json = Json.toJson(
        UpdateSubcontractorRequest(
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
      )

      val result = controller
        .updateSubcontractor()
        .apply(postJson("/subcontractor/update", json))

      status(result) mustBe NO_CONTENT
      verify(mockService).updateSubcontractor(any[UpdateSubcontractorRequest])
    }

    "returns 400 BadRequest for invlid JSON" in {
      val s = setup; import s.*

      val bad = Json.obj("bad" -> "json")

      val result = controller
        .updateSubcontractor()
        .apply(postJson("/subcontractor/update", bad))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      verify(mockService, never()).updateSubcontractor(any[UpdateSubcontractorRequest])
    }

    "maps service failure to 500 with error body" in {
      val s = setup; import s.*

      when(mockService.updateSubcontractor(any[UpdateSubcontractorRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val json = Json.toJson(
        UpdateSubcontractorRequest(
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
          subbieResourceRef = 1,
          matched = None,
          autoVerified = None,
          verified = None,
          verificationNumber = None,
          taxTreatment = None,
          verificationDate = None,
          updatedTaxTreatment = None
        )
      )

      val result = controller
        .updateSubcontractor()
        .apply(postJson("/subcontractor/update", json))

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
            country = Some("GB"),
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
