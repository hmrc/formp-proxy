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
import uk.gov.hmrc.formpproxy.cis.models.{ContractorScheme, CreateVerifications, DeleteVerifications, MonthlyReturn, Subcontractor, Submission, Verification, VerificationBatch}
import uk.gov.hmrc.formpproxy.cis.models.response.{GetCurrentVerificationBatchResponse, GetNewestVerificationBatchResponse}
import uk.gov.hmrc.formpproxy.cis.models.requests._
import uk.gov.hmrc.formpproxy.cis.models.response.CreateVerificationBatchAndVerificationsResponse
import uk.gov.hmrc.formpproxy.cis.models.response.CreateSubmissionAndUpdateVerificationsResponse
import uk.gov.hmrc.formpproxy.cis.services.VerificationService
import java.time.LocalDateTime

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
        scheme = None,
        subcontractors = Seq.empty,
        verificationBatch = None,
        verifications = Seq.empty,
        submission = None,
        monthlyReturn = None,
        monthlyReturnSubmission = None
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
      val s = setup
      import s.*

      val instId = "abc-123"

      val response = GetNewestVerificationBatchResponse(
        scheme = Some(
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
        verificationBatch = Some(
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
        submission = Some(
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
        monthlyReturn = Some(
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
        monthlyReturnSubmission = Some(
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

  "GET /cis/verification-batch/current/:instanceId (getCurrentVerificationBatch)" - {

    "returns 200 OK with JSON body when service succeeds" in {
      val s = setup;
      import s.*

      val instanceId = "abc-123"
      val response   = GetCurrentVerificationBatchResponse(
        scheme = None,
        subcontractors = Seq.empty,
        verificationBatch = None,
        verifications = Seq.empty,
        submission = None
      )

      when(mockService.getCurrentVerificationBatch(eqTo(instanceId)))
        .thenReturn(Future.successful(response))

      val req    = FakeRequest(GET, s"/cis/verification-batch/current/$instanceId")
      val result = controller.getCurrentVerificationBatch(instanceId).apply(req)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(response)

      verify(mockService).getCurrentVerificationBatch(eqTo(instanceId))
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 InternalServerError with error body when service fails" in {
      val s = setup;
      import s.*

      val instanceId = "abc-123"

      when(mockService.getCurrentVerificationBatch(eqTo(instanceId)))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req    = FakeRequest(GET, s"/cis/verification-batch/current/$instanceId")
      val result = controller.getCurrentVerificationBatch(instanceId).apply(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")

      verify(mockService).getCurrentVerificationBatch(eqTo(instanceId))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 OK with JSON body when service succeeds (all fields has values)" in {
      val s = setup;
      import s.*

      val instId = "abc-123"

      val response = GetCurrentVerificationBatchResponse(
        scheme = Some(
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
        verificationBatch = Some(
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
        submission = Some(
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
        )
      )

      when(mockService.getCurrentVerificationBatch(eqTo(instId)))
        .thenReturn(Future.successful(response))

      val req    = FakeRequest(GET, s"/cis/verification-batch/current/$instId")
      val result = controller.getCurrentVerificationBatch(instId).apply(req)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(response)

      verify(mockService).getCurrentVerificationBatch(eqTo(instId))
      verifyNoMoreInteractions(mockService)
    }
  }

  "POST /cis/verification-batch/create (createVerificationBatchAndVerifications)" - {

    val url = "/cis/verification-batch/create"

    "returns 200 OK with JSON body when service succeeds" in {
      val s = setup
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
      val s = setup
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
      val s = setup
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

  "POST /cis/verification-batch/modify (modifyVerifications)" - {

    val url = "/cis/verification-batch/modify"

    "returns 204 OK with JSON body when service succeeds" in {
      val s = setup;
      import s.*

      val requestModel = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(1L, 2L, 3L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(4L, 5L, 6L),
            actionIndicator = Some("A")
          )
        )
      )

      val requestJson = Json.toJson(requestModel)

      when(mockService.modifyVerifications(eqTo(requestModel)))
        .thenReturn(Future.successful(()))

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(requestJson)

      val result = controller.modifyVerifications().apply(req)

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe ""

      verify(mockService).modifyVerifications(eqTo(requestModel))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 BadRequest with error payload when JSON is invalid" in {
      val s = setup;
      import s.*

      val badJson = Json.obj()

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(badJson)

      val result = controller.modifyVerifications().apply(req)

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

      val requestModel = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(1L, 2L, 3L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(4L, 5L, 6L),
            actionIndicator = Some("A")
          )
        )
      )

      val requestJson = Json.toJson(requestModel)

      when(mockService.modifyVerifications(eqTo(requestModel)))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(requestJson)

      val result = controller.modifyVerifications().apply(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")

      verify(mockService).modifyVerifications(eqTo(requestModel))
      verifyNoMoreInteractions(mockService)
    }
  }

  "POST /cis/verification/submission/create (createSubmissionForVerification)" - {

    val url = "/cis/verification/submission/create"

    "returns 200 OK with JSON body when service succeeds" in {
      val s = setup
      import s.*

      val requestModel = CreateSubmissionAndUpdateVerificationsRequest(
        instanceId = "abc-123",
        verificationBatchId = 999L,
        verificationBatchResourceRef = 77L,
        emailRecipient = "ops@example.com",
        irMarkGenerated = Some("IR_MARK"),
        verifications = Seq(
          VerificationToUpdate(
            subcontractorName = "ACME LTD",
            verificationResourceRef = 10L,
            proceedVerification = "Y"
          )
        ),
        agentId = None
      )

      val responseModel = CreateSubmissionAndUpdateVerificationsResponse(submissionId = 555L)

      when(mockService.createSubmissionAndUpdateVerifications(eqTo(requestModel)))
        .thenReturn(Future.successful(responseModel))

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(Json.toJson(requestModel))

      val result = controller.createSubmissionAndUpdateVerifications().apply(req)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(responseModel)

      verify(mockService).createSubmissionAndUpdateVerifications(eqTo(requestModel))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 BadRequest with error payload when JSON is invalid" in {
      val s = setup
      import s.*

      val badJson = Json.obj(
        "instanceId" -> "abc-123"
      )

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(badJson)

      val result = controller.createSubmissionAndUpdateVerifications().apply(req)

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some(JSON)

      val body = contentAsJson(result)
      (body \ "message").as[String] mustBe "Invalid payload"
      (body \ "errors").isDefined mustBe true

      verifyNoInteractions(mockService)
    }

    "returns 500 InternalServerError with error body when service fails" in {
      val s = setup
      import s.*

      val requestModel = CreateSubmissionAndUpdateVerificationsRequest(
        instanceId = "abc-123",
        verificationBatchId = 999L,
        verificationBatchResourceRef = 77L,
        emailRecipient = "ops@example.com",
        irMarkGenerated = Some("IR_MARK"),
        verifications = Seq(
          VerificationToUpdate(
            subcontractorName = "ACME LTD",
            verificationResourceRef = 10L,
            proceedVerification = "Y"
          )
        ),
        agentId = None
      )

      when(mockService.createSubmissionAndUpdateVerifications(eqTo(requestModel)))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(Json.toJson(requestModel))

      val result = controller.createSubmissionAndUpdateVerifications().apply(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")

      verify(mockService).createSubmissionAndUpdateVerifications(eqTo(requestModel))
      verifyNoMoreInteractions(mockService)
    }
  }

  "POST /cis/verification/submission/update (updateVerificationSubmission)" - {

    val url = "/cis/verification/submission/update"

    "returns 204 NoContent when service succeeds" in {
      val s = setup
      import s.*

      val requestModel = UpdateVerificationSubmissionRequest(
        instanceId = "abc-123",
        verificationBatchId = 99L,
        verificationBatchResourceRef = 77L,
        submittableStatus = "FATAL_ERROR",
        govtalkErrorCode = Some("500"),
        govtalkErrorType = Some("timeOut"),
        govtalkErrorMessage = Some("timeOut")
      )

      when(mockService.updateVerificationSubmission(eqTo(requestModel)))
        .thenReturn(Future.successful(()))

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(Json.toJson(requestModel))

      val result = controller.updateVerificationSubmission().apply(req)

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe ""

      verify(mockService).updateVerificationSubmission(eqTo(requestModel))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 BadRequest with error payload when JSON is invalid" in {
      val s = setup
      import s.*

      val badJson = Json.obj("instanceId" -> "abc-123")

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(badJson)

      val result = controller.updateVerificationSubmission().apply(req)

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some(JSON)

      val body = contentAsJson(result)
      (body \ "message").as[String] mustBe "Invalid payload"
      (body \ "errors").isDefined mustBe true

      verifyNoInteractions(mockService)
    }

    "returns 500 InternalServerError with error body when service fails" in {
      val s = setup
      import s.*

      val requestModel = UpdateVerificationSubmissionRequest(
        instanceId = "abc-123",
        verificationBatchId = 99L,
        verificationBatchResourceRef = 77L,
        submittableStatus = "DEPARTMENTAL_ERROR",
        govtalkErrorCode = Some("3001"),
        govtalkErrorType = Some("departmentalError"),
        govtalkErrorMessage = Some("some error text")
      )

      when(mockService.updateVerificationSubmission(eqTo(requestModel)))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(Json.toJson(requestModel))

      val result = controller.updateVerificationSubmission().apply(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")

      verify(mockService).updateVerificationSubmission(eqTo(requestModel))
      verifyNoMoreInteractions(mockService)
    }
  }

  "POST /cis/verification/response/process (processVerificationResponseFromChris)" - {

    val url = "/cis/verification/response/process"

    "returns 204 NoContent when service succeeds" in {
      val s = setup
      import s.*

      val requestModel = ProcessVerificationResponseFromChrisRequest(
        instanceId = "abc-123",
        verificationBatchResourceRef = 222L,
        acceptedTime = "2026-06-15T10:05:00Z",
        submissionStatus = "ACCEPTED",
        irMarkReceived = Some("IR_MARK"),
        verificationResults = Seq(
          VerificationResult(
            resourceRef = 456L,
            matched = Some("Y"),
            verified = Some("Y"),
            verificationNumber = Some("V123456"),
            taxTreatment = "NET",
            verifiedDate = LocalDateTime.parse("2026-06-15T10:05:00")
          )
        )
      )

      when(mockService.processVerificationResponseFromChris(eqTo(requestModel)))
        .thenReturn(Future.successful(()))

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(Json.toJson(requestModel))

      val result = controller.processVerificationResponseFromChris().apply(req)

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe ""

      verify(mockService).processVerificationResponseFromChris(eqTo(requestModel))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 BadRequest with error payload when JSON is invalid" in {
      val s = setup
      import s.*

      val badJson = Json.obj("instanceId" -> "abc-123")

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(badJson)

      val result = controller.processVerificationResponseFromChris().apply(req)

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some(JSON)

      val body = contentAsJson(result)
      (body \ "message").as[String] mustBe "Invalid payload"
      (body \ "errors").isDefined mustBe true

      verifyNoInteractions(mockService)
    }

    "returns 500 InternalServerError with error body when service fails" in {
      val s = setup
      import s.*

      val requestModel = ProcessVerificationResponseFromChrisRequest(
        instanceId = "abc-123",
        verificationBatchResourceRef = 222L,
        acceptedTime = "2026-06-15T10:05:00Z",
        submissionStatus = "FAILED",
        irMarkReceived = Some("IR_MARK"),
        verificationResults = Seq(
          VerificationResult(
            resourceRef = 456L,
            matched = None,
            verified = None,
            verificationNumber = Some("V123456"),
            taxTreatment = "NET",
            verifiedDate = LocalDateTime.parse("2026-06-15T10:05:00")
          )
        )
      )

      when(mockService.processVerificationResponseFromChris(eqTo(requestModel)))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(Json.toJson(requestModel))

      val result = controller.processVerificationResponseFromChris().apply(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")

      verify(mockService).processVerificationResponseFromChris(eqTo(requestModel))
      verifyNoMoreInteractions(mockService)
    }
  }
}
