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

package uk.gov.hmrc.formpproxy.sdlt.controllers

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{ControllerComponents, PlayBodyParsers, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.{AuthAction, AuthOrApiKeyAction, FakeAuthAction, FakeAuthOrApiKeyAction}
import uk.gov.hmrc.formpproxy.sdlt.controllers.returns.ChrisSubmissionController
import uk.gov.hmrc.formpproxy.sdlt.models.submission.*
import uk.gov.hmrc.formpproxy.sdlt.services.submission.ChrisSubmissionService

import scala.concurrent.{ExecutionContext, Future}

class ChrisSubmissionControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private val submissionUpdate: SubmissionUpdate = SubmissionUpdate(
    IRMarkRecieved = Some("IRMARK-RECV-123"),
    utrn = Some("UTRN-987654"),
    email = Some("filer@example.test"),
    submissionRequestDate = Some("2026-01-01 10:00:00"),
    acceptedDate = Some("2026-01-02 11:30:00"),
    submittableStatus = Some("SUBMITTED"),
    govTalkErrorCode = None,
    govTalkErrorType = None,
    govTalkErrorMessage = None,
    IRMarkSent = Some("IRMARK-SENT-123")
  )

  private val submissionErrorDetail: SubmissionErrorDetail = SubmissionErrorDetail(
    position = "/GovTalkMessage/Body",
    errorMessage = "Schema validation failed"
  )

  private val govTalkStatusInitial: GovTalkStatusInitial = GovTalkStatusInitial(
    formLock = "0",
    createTimestamp = "2026-01-01 10:00:00",
    endStateTimestamp = None,
    lastMessageTimestamp = "2026-01-01 10:05:00",
    numberOfPolls = "0",
    pollInterval = "10",
    protocolStatus = "SUBMITTED",
    gatewayUrl = "https://transaction-engine.example/submission"
  )

  private val govTalkStatusReset: GovTalkStatusReset = GovTalkStatusReset(
    formLock = "0",
    createTimestamp = "2026-01-01 10:00:00",
    endStateTimestamp = None,
    lastMessageTimestamp = "2026-01-01 10:05:00",
    numberOfPolls = "3",
    pollInterval = "10",
    protocolStatusOld = "ACKNOWLEDGEMENT",
    protocolStatusNew = "SUBMITTED",
    gatewayUrl = "https://transaction-engine.example/submission"
  )

  private val govTalkStatusLock: GovTalkStatusLock = GovTalkStatusLock(
    formLockOld = "0",
    formLockNew = "1",
    pollInterval = "10",
    gatewayUrl = "https://transaction-engine.example/submission"
  )

  private val govTalkStatusStatistics: GovTalkStatusStatistics = GovTalkStatusStatistics(
    lastMessageTimestamp = "2026-01-01 10:05:00",
    numberOfPolls = "4",
    pollInterval = "10",
    gatewayUrl = "https://transaction-engine.example/submission"
  )

  "ChrisSubmissionController lockReturn" - {

    "returns 200 with the lock response when the return is successfully locked" in new Setup {
      val request: LockReturnRequest = LockReturnRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        version = 1
      )

      when(mockService.lockReturn(eqTo(request)))
        .thenReturn(Future.successful(LockReturnResponse(success = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.lockReturn()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "success").as[Boolean] mustBe true

      verify(mockService).lockReturn(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 409 Conflict when the lock cannot be acquired (version conflict)" in new Setup {
      val request: LockReturnRequest = LockReturnRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        version = 2
      )

      when(mockService.lockReturn(eqTo(request)))
        .thenReturn(Future.successful(LockReturnResponse(success = false)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.lockReturn()(req)

      status(res) mustBe CONFLICT
      (contentAsJson(res) \ "message").as[String] mustBe "Version conflict acquiring return lock"

      verify(mockService).lockReturn(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.lockReturn()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "returnResourceRef" -> "100001",
        "version"           -> 1
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.lockReturn()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: LockReturnRequest = LockReturnRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        version = 1
      )

      when(mockService.lockReturn(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.lockReturn()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).lockReturn(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController createSubmission" - {

    "returns 201 with submission response when service succeeds" in new Setup {
      val request: CreateSubmissionRequest = CreateSubmissionRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        email = Some("filer@example.test")
      )

      when(mockService.createSubmission(eqTo(request)))
        .thenReturn(Future.successful(CreateSubmissionReturn(success = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createSubmission()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "success").as[Boolean] mustBe true

      verify(mockService).createSubmission(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.createSubmission()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: CreateSubmissionRequest = CreateSubmissionRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        email = Some("filer@example.test")
      )

      when(mockService.createSubmission(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createSubmission()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).createSubmission(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController updateSubmission" - {

    "returns 200 with submission response when service succeeds" in new Setup {
      val request: UpdateSubmissionRequest = UpdateSubmissionRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        submission = submissionUpdate
      )

      when(mockService.updateSubmission(eqTo(request)))
        .thenReturn(Future.successful(UpdateSubmissionReturn(success = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateSubmission()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "success").as[Boolean] mustBe true

      verify(mockService).updateSubmission(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateSubmission()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when submission payload is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateSubmission()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdateSubmissionRequest = UpdateSubmissionRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        submission = submissionUpdate
      )

      when(mockService.updateSubmission(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database timeout")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateSubmission()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateSubmission(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController createSubmissionErrorDetail" - {

    "returns 201 with response when service succeeds" in new Setup {
      val request: CreateSubmissionErrorDetailRequest = CreateSubmissionErrorDetailRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        submissionErrorDetails = submissionErrorDetail
      )

      when(mockService.createSubmissionErrorDetail(eqTo(request)))
        .thenReturn(Future.successful(CreateSubmissionErrorDetailReturn(success = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createSubmissionErrorDetail()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "success").as[Boolean] mustBe true

      verify(mockService).createSubmissionErrorDetail(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.createSubmissionErrorDetail()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when submissionErrorDetails is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.createSubmissionErrorDetail()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: CreateSubmissionErrorDetailRequest = CreateSubmissionErrorDetailRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        submissionErrorDetails = submissionErrorDetail
      )

      when(mockService.createSubmissionErrorDetail(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.createSubmissionErrorDetail()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).createSubmissionErrorDetail(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController deleteSubmissionErrorDetail" - {

    "returns 200 with response when service succeeds" in new Setup {
      val request: DeleteSubmissionErrorDetailRequest = DeleteSubmissionErrorDetailRequest(
        storn = "STORN12345",
        returnResourceRef = "100001"
      )

      when(mockService.deleteSubmissionErrorDetail(eqTo(request)))
        .thenReturn(Future.successful(DeleteSubmissionErrorDetailReturn(success = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteSubmissionErrorDetail()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "success").as[Boolean] mustBe true

      verify(mockService).deleteSubmissionErrorDetail(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.deleteSubmissionErrorDetail()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when returnResourceRef is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "storn" -> "STORN12345"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteSubmissionErrorDetail()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: DeleteSubmissionErrorDetailRequest = DeleteSubmissionErrorDetailRequest(
        storn = "STORN12345",
        returnResourceRef = "100001"
      )

      when(mockService.deleteSubmissionErrorDetail(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteSubmissionErrorDetail()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).deleteSubmissionErrorDetail(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController insertInitialGovTalkStatus" - {

    "returns 201 with GovTalk status response when service succeeds" in new Setup {
      val request: InsertInitialGovTalkStatusRequest = InsertInitialGovTalkStatusRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1",
        correlationId = "CORR-1",
        govTalkStatus = govTalkStatusInitial
      )

      when(mockService.insertInitialGovTalkStatus(eqTo(request)))
        .thenReturn(Future.successful(GovTalkStatusReturn(success = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.insertInitialGovTalkStatus()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "success").as[Boolean] mustBe true

      verify(mockService).insertInitialGovTalkStatus(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.insertInitialGovTalkStatus()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when govTalkStatus is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "userIdentifier" -> "USER123",
        "formResultId"   -> "FRID-1",
        "correlationId"  -> "CORR-1"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.insertInitialGovTalkStatus()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: InsertInitialGovTalkStatusRequest = InsertInitialGovTalkStatusRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1",
        correlationId = "CORR-1",
        govTalkStatus = govTalkStatusInitial
      )

      when(mockService.insertInitialGovTalkStatus(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.insertInitialGovTalkStatus()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).insertInitialGovTalkStatus(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController resetGovTalkStatus" - {

    "returns 200 with GovTalk status response when service succeeds" in new Setup {
      val request: ResetGovTalkStatusRequest = ResetGovTalkStatusRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1",
        correlationId = "CORR-1",
        govTalkStatus = govTalkStatusReset
      )

      when(mockService.resetGovTalkStatus(eqTo(request)))
        .thenReturn(Future.successful(GovTalkStatusReturn(success = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.resetGovTalkStatus()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "success").as[Boolean] mustBe true

      verify(mockService).resetGovTalkStatus(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.resetGovTalkStatus()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when userIdentifier is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "formResultId"  -> "FRID-1",
        "correlationId" -> "CORR-1",
        "govTalkStatus" -> Json.toJson(govTalkStatusReset)
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.resetGovTalkStatus()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: ResetGovTalkStatusRequest = ResetGovTalkStatusRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1",
        correlationId = "CORR-1",
        govTalkStatus = govTalkStatusReset
      )

      when(mockService.resetGovTalkStatus(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.resetGovTalkStatus()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).resetGovTalkStatus(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController updateGovTalkStatus" - {

    "returns 200 with GovTalk status response when service succeeds" in new Setup {
      val request: UpdateGovTalkStatusRequest = UpdateGovTalkStatusRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1",
        endStateTimestamp = "2026-01-02 11:30:00",
        protocolStatus = "SUBMITTED"
      )

      when(mockService.updateGovTalkStatus(eqTo(request)))
        .thenReturn(Future.successful(GovTalkStatusReturn(success = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateGovTalkStatus()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "success").as[Boolean] mustBe true

      verify(mockService).updateGovTalkStatus(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateGovTalkStatus()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when protocolStatus is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "userIdentifier"    -> "USER123",
        "formResultId"      -> "FRID-1",
        "endStateTimestamp" -> "2026-01-02 11:30:00"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateGovTalkStatus()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdateGovTalkStatusRequest = UpdateGovTalkStatusRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1",
        endStateTimestamp = "2026-01-02 11:30:00",
        protocolStatus = "SUBMITTED"
      )

      when(mockService.updateGovTalkStatus(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateGovTalkStatus()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateGovTalkStatus(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController updateGovTalkStatusCorrelationId" - {

    "returns 200 with GovTalk status response when service succeeds" in new Setup {
      val request: UpdateGovTalkStatusCorrelationIdRequest = UpdateGovTalkStatusCorrelationIdRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1",
        correlationId = "CORR-1",
        endStateTimestamp = "2026-01-02 11:30:00",
        protocolStatus = "SUBMITTED"
      )

      when(mockService.updateGovTalkStatusCorrelationId(eqTo(request)))
        .thenReturn(Future.successful(GovTalkStatusReturn(success = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateGovTalkStatusCorrelationId()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "success").as[Boolean] mustBe true

      verify(mockService).updateGovTalkStatusCorrelationId(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateGovTalkStatusCorrelationId()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when correlationId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "userIdentifier"    -> "USER123",
        "formResultId"      -> "FRID-1",
        "endStateTimestamp" -> "2026-01-02 11:30:00",
        "protocolStatus"    -> "SUBMITTED"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateGovTalkStatusCorrelationId()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdateGovTalkStatusCorrelationIdRequest = UpdateGovTalkStatusCorrelationIdRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1",
        correlationId = "CORR-1",
        endStateTimestamp = "2026-01-02 11:30:00",
        protocolStatus = "SUBMITTED"
      )

      when(mockService.updateGovTalkStatusCorrelationId(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateGovTalkStatusCorrelationId()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateGovTalkStatusCorrelationId(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController updateGovTalkStatusLock" - {

    "returns 200 with GovTalk status response when service succeeds" in new Setup {
      val request: UpdateGovTalkStatusLockRequest = UpdateGovTalkStatusLockRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1",
        govTalkStatus = govTalkStatusLock
      )

      when(mockService.updateGovTalkStatusLock(eqTo(request)))
        .thenReturn(Future.successful(GovTalkStatusReturn(success = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateGovTalkStatusLock()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "success").as[Boolean] mustBe true

      verify(mockService).updateGovTalkStatusLock(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateGovTalkStatusLock()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when govTalkStatus is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "userIdentifier" -> "USER123",
        "formResultId"   -> "FRID-1"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateGovTalkStatusLock()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdateGovTalkStatusLockRequest = UpdateGovTalkStatusLockRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1",
        govTalkStatus = govTalkStatusLock
      )

      when(mockService.updateGovTalkStatusLock(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateGovTalkStatusLock()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateGovTalkStatusLock(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController updateGovTalkStatistics" - {

    "returns 200 with GovTalk status response when service succeeds" in new Setup {
      val request: UpdateGovTalkStatisticsRequest = UpdateGovTalkStatisticsRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1",
        govTalkStatus = govTalkStatusStatistics
      )

      when(mockService.updateGovTalkStatistics(eqTo(request)))
        .thenReturn(Future.successful(GovTalkStatusReturn(success = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateGovTalkStatistics()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "success").as[Boolean] mustBe true

      verify(mockService).updateGovTalkStatistics(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateGovTalkStatistics()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when govTalkStatus is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "userIdentifier" -> "USER123",
        "formResultId"   -> "FRID-1"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.updateGovTalkStatistics()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: UpdateGovTalkStatisticsRequest = UpdateGovTalkStatisticsRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1",
        govTalkStatus = govTalkStatusStatistics
      )

      when(mockService.updateGovTalkStatistics(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.updateGovTalkStatistics()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).updateGovTalkStatistics(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController deleteGovTalkStatus" - {

    "returns 200 with GovTalk status response when service succeeds" in new Setup {
      val request: DeleteGovTalkStatusRequest = DeleteGovTalkStatusRequest(
        resultId = "FRID-1"
      )

      when(mockService.deleteGovTalkStatus(eqTo(request)))
        .thenReturn(Future.successful(GovTalkStatusReturn(success = true)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteGovTalkStatus()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "success").as[Boolean] mustBe true

      verify(mockService).deleteGovTalkStatus(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.deleteGovTalkStatus()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when resultId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "notResultId" -> "FRID-1"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.deleteGovTalkStatus()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: DeleteGovTalkStatusRequest = DeleteGovTalkStatusRequest(
        resultId = "FRID-1"
      )

      when(mockService.deleteGovTalkStatus(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.deleteGovTalkStatus()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).deleteGovTalkStatus(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController selectGovTalkStatus" - {

    "returns 200 with the GovTalk status when service succeeds" in new Setup {
      val request: SelectGovTalkStatusRequest = SelectGovTalkStatusRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1"
      )

      val expectedResponse: SelectGovTalkStatusResponse = SelectGovTalkStatusResponse(
        userIdentifier = Some("USER123"),
        formResultId = Some("FRID-1"),
        correlationId = Some("CORR-1"),
        formLock = Some("0"),
        createTimestamp = Some("2026-01-01 10:00:00"),
        endStateTimestamp = None,
        lastMessageTimestamp = Some("2026-01-01 10:05:00"),
        numberOfPolls = Some("2"),
        pollInterval = Some("10"),
        protocolStatus = Some("SUBMITTED"),
        gatewayUrl = Some("https://transaction-engine.example/submission")
      )

      when(mockService.selectGovTalkStatus(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.selectGovTalkStatus()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "formResultId").as[String] mustBe "FRID-1"
      (contentAsJson(res) \ "protocolStatus").as[String] mustBe "SUBMITTED"

      verify(mockService).selectGovTalkStatus(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with an empty status when no row is found" in new Setup {
      val request: SelectGovTalkStatusRequest = SelectGovTalkStatusRequest(
        userIdentifier = "USER999",
        formResultId = "FRID-UNKNOWN"
      )

      val emptyResponse: SelectGovTalkStatusResponse = SelectGovTalkStatusResponse(
        userIdentifier = None,
        formResultId = None,
        correlationId = None,
        formLock = None,
        createTimestamp = None,
        endStateTimestamp = None,
        lastMessageTimestamp = None,
        numberOfPolls = None,
        pollInterval = None,
        protocolStatus = None,
        gatewayUrl = None
      )

      when(mockService.selectGovTalkStatus(eqTo(request)))
        .thenReturn(Future.successful(emptyResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.selectGovTalkStatus()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "formResultId").asOpt[String] mustBe None
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.selectGovTalkStatus()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when formResultId is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "userIdentifier" -> "USER123"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.selectGovTalkStatus()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: SelectGovTalkStatusRequest = SelectGovTalkStatusRequest(
        userIdentifier = "USER123",
        formResultId = "FRID-1"
      )

      when(mockService.selectGovTalkStatus(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.selectGovTalkStatus()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).selectGovTalkStatus(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ChrisSubmissionController selectGovTalkFormResultId" - {

    "returns 200 with the form result id when service succeeds" in new Setup {
      val request: SelectGovTalkFormResultIdRequest = SelectGovTalkFormResultIdRequest(
        userIdentifier = "USER123"
      )

      when(mockService.selectGovTalkFormResultId(eqTo(request)))
        .thenReturn(Future.successful(SelectGovTalkFormResultIdResponse(formResultId = Some("FRID-1"))))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.selectGovTalkFormResultId()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "formResultId").as[String] mustBe "FRID-1"

      verify(mockService).selectGovTalkFormResultId(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with no form result id when none is found" in new Setup {
      val request: SelectGovTalkFormResultIdRequest = SelectGovTalkFormResultIdRequest(
        userIdentifier = "USER999"
      )

      when(mockService.selectGovTalkFormResultId(eqTo(request)))
        .thenReturn(Future.successful(SelectGovTalkFormResultIdResponse(formResultId = None)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.selectGovTalkFormResultId()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "formResultId").asOpt[String] mustBe None
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.selectGovTalkFormResultId()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when userIdentifier is missing" in new Setup {
      val invalidJson: JsObject = Json.obj(
        "notUserIdentifier" -> "USER123"
      )

      val req: FakeRequest[JsValue] = makeJsonRequest(invalidJson)
      val res: Future[Result]       = controller.selectGovTalkFormResultId()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val request: SelectGovTalkFormResultIdRequest = SelectGovTalkFormResultIdRequest(
        userIdentifier = "USER123"
      )

      when(mockService.selectGovTalkFormResultId(eqTo(request)))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(request))
      val res: Future[Result]       = controller.selectGovTalkFormResultId()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).selectGovTalkFormResultId(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext                = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents             = stubControllerComponents()
    private val parsers: PlayBodyParsers             = cc.parsers
    private def fakeAuth: AuthAction                 = new FakeAuthAction(parsers)
    private def fakeAuthOrApiKey: AuthOrApiKeyAction = new FakeAuthOrApiKeyAction(parsers)

    val mockService: ChrisSubmissionService = mock[ChrisSubmissionService]
    val controller                          = new ChrisSubmissionController(fakeAuth, fakeAuthOrApiKey, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/submission")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)
  }
}
