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

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.*
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.FakeAuthAction
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.{ContractorScheme, MonthlyReturn, Subcontractor, Submission, Verification, VerificationBatch}
import uk.gov.hmrc.formpproxy.cis.models.response.GetNewestVerificationBatchResponse
import uk.gov.hmrc.formpproxy.cis.models.requests.CreateVerificationBatchAndVerificationsRequest
import uk.gov.hmrc.formpproxy.cis.models.response.CreateVerificationBatchAndVerificationsResponse
import uk.gov.hmrc.formpproxy.cis.services.VerificationService
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.Helpers.{CONTENT_TYPE, POST}

import scala.concurrent.Future

class VerificationControllerSpec extends SpecBase {

  trait Setup {
    val mockService: VerificationService = mock[VerificationService]
    val auth                             = new FakeAuthAction(cc.parsers)
    lazy val controller                  = new VerificationController(auth, mockService, cc)(
      scala.concurrent.ExecutionContext.Implicits.global
    )
  }

  private def setup: Setup = new Setup {}

  "GET /cis/verification-batch/newest/:instanceId (getNewestVerificationBatch)" - {

    "returns 200 OK with JSON body when service succeeds" in {
      val s = setup; import s.*

      val instanceId = "abc-123"
      val response   = GetNewestVerificationBatchResponse(
        scheme = Seq.empty,
        subcontractors = Seq.empty,
        verificationBatch = Seq.empty,
        verifications = Seq.empty,
        submission = Seq.empty,
        monthlyReturn = Seq.empty,
        monthlyReturnSubmission = Seq.empty
      )

      when(mockService.getNewestVerificationBatch(eqTo(instanceId)))
        .thenReturn(Future.successful(response))

      val req    = FakeRequest(GET, s"/cis/verification-batch/newest/$instanceId")
      val result = controller.getNewestVerificationBatch(instanceId).apply(req)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(response)

      verify(mockService).getNewestVerificationBatch(eqTo(instanceId))
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 InternalServerError with error body when service fails" in {
      val s = setup; import s.*

      val instanceId = "abc-123"

      when(mockService.getNewestVerificationBatch(eqTo(instanceId)))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req    = FakeRequest(GET, s"/cis/verification-batch/newest/$instanceId")
      val result = controller.getNewestVerificationBatch(instanceId).apply(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")

      verify(mockService).getNewestVerificationBatch(eqTo(instanceId))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 OK with JSON body when service succeeds (all fields has values)" in {
      val s = setup;
      import s.*

      val instId = "abc-123"

      val response = GetNewestVerificationBatchResponse(
        scheme = Seq(
          ContractorScheme(
            schemeId = 123,
            instanceId = instId,
            accountsOfficeReference = "123PA00123456",
            taxOfficeNumber = "163",
            taxOfficeReference = "AB0063",
            utr = Some("1111111111"),
            name = Some("ABC Construction Ltd"),
            emailAddress = Some("ops@example.com"),
            displayWelcomePage = Some("Y"),
            prePopCount = Some(5),
            prePopSuccessful = Some("Y"),
            subcontractorCounter = Some(10),
            verificationBatchCounter = Some(2),
            lastUpdate = None,
            version = Some(1)
          )
        ),
        subcontractors = Seq(
          Subcontractor(
            subcontractorId = 1L,
            utr = Some("1111111111"),
            pageVisited = Some(2),
            partnerUtr = None,
            crn = None,
            firstName = Some("John"),
            nino = Some("AA123456A"),
            secondName = None,
            surname = Some("Smith"),
            partnershipTradingName = None,
            tradingName = Some("ACME"),
            subcontractorType = Some("soletrader"),
            addressLine1 = Some("1 Main Street"),
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            country = Some("United Kingdom"),
            postcode = Some("AA1 1AA"),
            emailAddress = Some("test@test.com"),
            phoneNumber = Some("01234567890"),
            mobilePhoneNumber = Some("07123456789"),
            worksReferenceNumber = Some("WRN-123"),
            createDate = None,
            lastUpdate = None,
            subbieResourceRef = Some(10L),
            matched = Some("Y"),
            autoVerified = Some("N"),
            verified = Some("Y"),
            verificationNumber = Some("V0000000001"),
            taxTreatment = Some("0"),
            verificationDate = None,
            version = Some(1),
            updatedTaxTreatment = None,
            lastMonthlyReturnDate = None,
            pendingVerifications = Some(0)
          )
        ),
        verificationBatch = Seq(
          VerificationBatch(
            verificationBatchId = 99L,
            schemeId = 123L,
            verificationsCounter = Some(1),
            verifBatchResourceRef = Some(7L),
            proceedSession = Some("Y"),
            confirmArrangement = Some("Y"),
            confirmCorrect = Some("Y"),
            status = Some("STARTED"),
            verificationNumber = Some("V0000000001"),
            createDate = None,
            lastUpdate = None,
            version = Some(1)
          )
        ),
        verifications = Seq(
          Verification(
            verificationId = 1001L,
            matched = Some("Y"),
            verificationNumber = Some("V0000000001"),
            taxTreatment = Some("0"),
            actionIndicator = Some("A"),
            verificationBatchId = Some(99L),
            schemeId = Some(123L),
            subcontractorId = Some(1L),
            subcontractorName = Some("ACME"),
            verificationResourceRef = Some(1L),
            proceed = Some("Y"),
            createDate = None,
            lastUpdate = None,
            version = Some(1)
          )
        ),
        submission = Seq(
          Submission(
            submissionId = 555L,
            submissionType = "VERIFICATIONS",
            activeObjectId = Some(99L),
            status = Some("ACCEPTED"),
            hmrcMarkGenerated = Some("mark-gen"),
            hmrcMarkGgis = None,
            emailRecipient = Some("ops@example.com"),
            acceptedTime = Some("12:00:00"),
            createDate = None,
            lastUpdate = None,
            schemeId = 123L,
            agentId = None,
            l_Migrated = None,
            submissionRequestDate = None,
            govTalkErrorCode = None,
            govTalkErrorType = None,
            govTalkErrorMessage = None
          )
        ),
        monthlyReturn = Seq(
          MonthlyReturn(
            monthlyReturnId = 777L,
            taxYear = 2025,
            taxMonth = 1,
            nilReturnIndicator = Some("N"),
            decEmpStatusConsidered = Some("Y"),
            decAllSubsVerified = Some("Y"),
            decInformationCorrect = Some("Y"),
            decNoMoreSubPayments = Some("N"),
            decNilReturnNoPayments = Some("N"),
            status = Some("SUBMITTED"),
            lastUpdate = None,
            amendment = Some("N"),
            supersededBy = None
          )
        ),
        monthlyReturnSubmission = Seq(
          Submission(
            submissionId = 556L,
            submissionType = "MONTHLY_RETURN",
            activeObjectId = Some(777L),
            status = Some("ACCEPTED"),
            hmrcMarkGenerated = Some("mark-gen-mr"),
            hmrcMarkGgis = None,
            emailRecipient = Some("ops@example.com"),
            acceptedTime = Some("12:01:00"),
            createDate = None,
            lastUpdate = None,
            schemeId = 123L,
            agentId = None,
            l_Migrated = None,
            submissionRequestDate = None,
            govTalkErrorCode = None,
            govTalkErrorType = None,
            govTalkErrorMessage = None
          )
        )
      )

      when(mockService.getNewestVerificationBatch(eqTo(instId)))
        .thenReturn(Future.successful(response))

      val req    = FakeRequest(GET, s"/cis/verification-batch/newest/$instId")
      val result = controller.getNewestVerificationBatch(instId).apply(req)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(response)

      verify(mockService).getNewestVerificationBatch(eqTo(instId))
      verifyNoMoreInteractions(mockService)
    }
  }

  // add this new block to VerificationControllerSpec.scala (e.g. after the GET tests)

  "POST /cis/verification-batch/create (createVerificationBatchAndVerifications)" - {

    val url = "/cis/verification-batch/create"

    "returns 200 OK with JSON body when service succeeds" in {
      val s = setup;
      import s.*

      val requestModel = CreateVerificationBatchAndVerificationsRequest(
        instanceId = "abc-123",
        verificationResourceReferences = Seq(1L, 2L, 3L),
        actionIndicator = Some("A")
      )

      val requestJson = Json.toJson(requestModel)

      val responseModel = CreateVerificationBatchAndVerificationsResponse(
        verificationBatchResourceReference = 99L
      )

      when(mockService.createVerificationBatchAndVerifications(eqTo(requestModel)))
        .thenReturn(Future.successful(responseModel))

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(requestJson)

      val result = controller.createVerificationBatchAndVerifications().apply(req)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(responseModel)

      verify(mockService).createVerificationBatchAndVerifications(eqTo(requestModel))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 BadRequest with error payload when JSON is invalid" in {
      val s = setup;
      import s.*

      val badJson = Json.obj(
        "instanceId" -> "abc-123"
        // missing verificationResourceReferences etc
      )

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(badJson)

      val result = controller.createVerificationBatchAndVerifications().apply(req)

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some(JSON)

      val body = contentAsJson(result)
      (body \ "message").as[String] mustBe "Invalid payload"
      (body \ "errors").isDefined mustBe true

      verifyNoInteractions(mockService)
    }

    "returns 500 InternalServerError with error body when service fails" in {
      val s = setup;
      import s.*

      val requestModel = CreateVerificationBatchAndVerificationsRequest(
        instanceId = "abc-123",
        verificationResourceReferences = Seq(1L),
        actionIndicator = None
      )

      val requestJson = Json.toJson(requestModel)

      when(mockService.createVerificationBatchAndVerifications(eqTo(requestModel)))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(requestJson)

      val result = controller.createVerificationBatchAndVerifications().apply(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")

      verify(mockService).createVerificationBatchAndVerifications(eqTo(requestModel))
      verifyNoMoreInteractions(mockService)
    }
  }
}
