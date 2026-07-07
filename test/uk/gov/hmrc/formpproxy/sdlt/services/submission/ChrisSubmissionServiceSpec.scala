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

package uk.gov.hmrc.formpproxy.sdlt.services.submission

import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.formpproxy.sdlt.models.submission.*
import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository

import scala.concurrent.Future

class ChrisSubmissionServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with ScalaFutures {

  private def newService(repo: SdltFormpRepository) = new ChrisSubmissionService(repo)

  private val lockReturnRequest =
    LockReturnRequest(storn = "STORN12345", returnResourceRef = "100001", version = 1)

  private val createSubmissionRequest =
    CreateSubmissionRequest(storn = "STORN12345", returnResourceRef = "100001", email = "agent@example.com")

  private val updateSubmissionRequest =
    UpdateSubmissionRequest(
      storn = "STORN12345",
      returnResourceRef = "100001",
      submission = SubmissionUpdate(
        IRMarkRecieved = Some("IRMARK-RCV"),
        utrn = Some("UTRN123"),
        email = Some("agent@example.com"),
        submissionRequestDate = Some("2025-01-15"),
        acceptedDate = Some("2025-01-16"),
        submittableStatus = Some("ACCEPTED"),
        govTalkErrorCode = None,
        govTalkErrorType = None,
        govTalkErrorMessage = None,
        IRMarkSent = Some("IRMARK-SNT")
      )
    )

  private val createSubmissionErrorDetailRequest =
    CreateSubmissionErrorDetailRequest(
      storn = "STORN12345",
      returnResourceRef = "100001",
      submissionErrorDetails = SubmissionErrorDetail(position = "12", errorMessage = "Invalid postcode format")
    )

  private val deleteSubmissionErrorDetailRequest =
    DeleteSubmissionErrorDetailRequest(storn = "STORN12345", returnResourceRef = "100001")

  private val insertInitialGovTalkStatusRequest =
    InsertInitialGovTalkStatusRequest(
      userIdentifier = "STORN12345",
      formResultId = "SUB123",
      correlationId = "CORR-XYZ",
      govTalkStatus = GovTalkStatusInitial(
        formLock = "N",
        createTimestamp = "2025-01-15 10:30:00",
        endStateTimestamp = Some("2025-01-15 11:00:00"),
        lastMessageTimestamp = "2025-01-15 10:45:00",
        numberOfPolls = "3",
        pollInterval = "60",
        protocolStatus = "submitted",
        gatewayUrl = "https://gateway.example.com/submit"
      )
    )

  private val resetGovTalkStatusRequest =
    ResetGovTalkStatusRequest(
      userIdentifier = "STORN12345",
      formResultId = "SUB123",
      correlationId = "CORR-XYZ",
      govTalkStatus = GovTalkStatusReset(
        formLock = "N",
        createTimestamp = "2025-01-15 10:30:00",
        endStateTimestamp = Some("2025-01-15 11:00:00"),
        lastMessageTimestamp = "2025-01-15 10:45:00",
        numberOfPolls = "5",
        pollInterval = "60",
        protocolStatusOld = "error",
        protocolStatusNew = "submitted",
        gatewayUrl = "https://gateway.example.com/submit"
      )
    )

  private val updateGovTalkStatusRequest =
    UpdateGovTalkStatusRequest(
      userIdentifier = "STORN12345",
      formResultId = "SUB123",
      endStateTimestamp = "2025-01-15 12:00:00",
      protocolStatus = "response"
    )

  private val updateGovTalkStatusCorrelationIdRequest =
    UpdateGovTalkStatusCorrelationIdRequest(
      userIdentifier = "STORN12345",
      formResultId = "SUB123",
      correlationId = "CORR-NEW",
      endStateTimestamp = "2025-01-15 12:30:00",
      protocolStatus = "acknowledgement"
    )

  private val updateGovTalkStatusLockRequest =
    UpdateGovTalkStatusLockRequest(
      userIdentifier = "STORN12345",
      formResultId = "SUB123",
      govTalkStatus = GovTalkStatusLock(
        formLockOld = "N",
        formLockNew = "Y",
        pollInterval = "90",
        gatewayUrl = "https://gateway.example.com/submit"
      )
    )

  private val updateGovTalkStatisticsRequest =
    UpdateGovTalkStatisticsRequest(
      userIdentifier = "STORN12345",
      formResultId = "SUB123",
      govTalkStatus = GovTalkStatusStatistics(
        lastMessageTimestamp = "2025-01-15 13:00:00",
        numberOfPolls = "7",
        pollInterval = "120",
        gatewayUrl = "https://gateway.example.com/submit"
      )
    )

  private val deleteGovTalkStatusRequest =
    DeleteGovTalkStatusRequest(resultId = "SUB123")

  private val selectGovTalkStatusRequest =
    SelectGovTalkStatusRequest(userIdentifier = "STORN12345", formResultId = "SUB123")

  private val selectGovTalkFormResultIdRequest =
    SelectGovTalkFormResultIdRequest(userIdentifier = "STORN12345")

  private val lockReturnResponse                = LockReturnResponse(success = true)
  private val createSubmissionReturn            = CreateSubmissionReturn(success = true)
  private val updateSubmissionReturn            = UpdateSubmissionReturn(success = true)
  private val createSubmissionErrorDetailReturn = CreateSubmissionErrorDetailReturn(success = true)
  private val deleteSubmissionErrorDetailReturn = DeleteSubmissionErrorDetailReturn(success = true)
  private val govTalkStatusReturn               = GovTalkStatusReturn(success = true)

  private val selectGovTalkStatusResponse =
    SelectGovTalkStatusResponse(
      userIdentifier = Some("STORN12345"),
      formResultId = Some("SUB123"),
      correlationId = Some("CORR-XYZ"),
      formLock = Some("N"),
      createTimestamp = Some("2025-01-15 10:30:00"),
      endStateTimestamp = Some("2025-01-15 11:00:00"),
      lastMessageTimestamp = Some("2025-01-15 10:45:00"),
      numberOfPolls = Some("3"),
      pollInterval = Some("60"),
      protocolStatus = Some("submitted"),
      gatewayUrl = Some("https://gateway.example.com/submit")
    )

  private val selectGovTalkFormResultIdResponse =
    SelectGovTalkFormResultIdResponse(formResultId = Some("SUB123"))

  // ---- tests -------------------------------------------------------------

  "ChrisSubmissionService" - {

    "lockReturn delegates to repo.sdltLockReturn and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltLockReturn(lockReturnRequest)).thenReturn(Future.successful(lockReturnResponse))

      newService(repo).lockReturn(lockReturnRequest).futureValue mustBe lockReturnResponse

      verify(repo).sdltLockReturn(lockReturnRequest)
    }

    "createSubmission delegates to repo.sdltCreateSubmission and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltCreateSubmission(createSubmissionRequest)).thenReturn(Future.successful(createSubmissionReturn))

      newService(repo).createSubmission(createSubmissionRequest).futureValue mustBe createSubmissionReturn

      verify(repo).sdltCreateSubmission(createSubmissionRequest)
    }

    "updateSubmission delegates to repo.sdltUpdateSubmission and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltUpdateSubmission(updateSubmissionRequest)).thenReturn(Future.successful(updateSubmissionReturn))

      newService(repo).updateSubmission(updateSubmissionRequest).futureValue mustBe updateSubmissionReturn

      verify(repo).sdltUpdateSubmission(updateSubmissionRequest)
    }

    "createSubmissionErrorDetail delegates to repo.sdltCreateSubmissionErrorDetail and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltCreateSubmissionErrorDetail(createSubmissionErrorDetailRequest))
        .thenReturn(Future.successful(createSubmissionErrorDetailReturn))

      newService(repo)
        .createSubmissionErrorDetail(createSubmissionErrorDetailRequest)
        .futureValue mustBe createSubmissionErrorDetailReturn

      verify(repo).sdltCreateSubmissionErrorDetail(createSubmissionErrorDetailRequest)
    }

    "deleteSubmissionErrorDetail delegates to repo.sdltDeleteSubmissionErrorDetail and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltDeleteSubmissionErrorDetail(deleteSubmissionErrorDetailRequest))
        .thenReturn(Future.successful(deleteSubmissionErrorDetailReturn))

      newService(repo)
        .deleteSubmissionErrorDetail(deleteSubmissionErrorDetailRequest)
        .futureValue mustBe deleteSubmissionErrorDetailReturn

      verify(repo).sdltDeleteSubmissionErrorDetail(deleteSubmissionErrorDetailRequest)
    }

    "insertInitialGovTalkStatus delegates to repo.sdltInsertInitialGovTalkStatus and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltInsertInitialGovTalkStatus(insertInitialGovTalkStatusRequest))
        .thenReturn(Future.successful(govTalkStatusReturn))

      newService(repo)
        .insertInitialGovTalkStatus(insertInitialGovTalkStatusRequest)
        .futureValue mustBe govTalkStatusReturn

      verify(repo).sdltInsertInitialGovTalkStatus(insertInitialGovTalkStatusRequest)
    }

    "resetGovTalkStatus delegates to repo.sdltResetGovTalkStatus and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltResetGovTalkStatus(resetGovTalkStatusRequest)).thenReturn(Future.successful(govTalkStatusReturn))

      newService(repo).resetGovTalkStatus(resetGovTalkStatusRequest).futureValue mustBe govTalkStatusReturn

      verify(repo).sdltResetGovTalkStatus(resetGovTalkStatusRequest)
    }

    "updateGovTalkStatus delegates to repo.sdltUpdateGovTalkStatus and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltUpdateGovTalkStatus(updateGovTalkStatusRequest)).thenReturn(Future.successful(govTalkStatusReturn))

      newService(repo).updateGovTalkStatus(updateGovTalkStatusRequest).futureValue mustBe govTalkStatusReturn

      verify(repo).sdltUpdateGovTalkStatus(updateGovTalkStatusRequest)
    }

    "updateGovTalkStatusCorrelationId delegates to repo.sdltUpdateGovTalkStatusCorrelationId and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltUpdateGovTalkStatusCorrelationId(updateGovTalkStatusCorrelationIdRequest))
        .thenReturn(Future.successful(govTalkStatusReturn))

      newService(repo)
        .updateGovTalkStatusCorrelationId(updateGovTalkStatusCorrelationIdRequest)
        .futureValue mustBe govTalkStatusReturn

      verify(repo).sdltUpdateGovTalkStatusCorrelationId(updateGovTalkStatusCorrelationIdRequest)
    }

    "updateGovTalkStatusLock delegates to repo.sdltUpdateGovTalkStatusLock and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltUpdateGovTalkStatusLock(updateGovTalkStatusLockRequest))
        .thenReturn(Future.successful(govTalkStatusReturn))

      newService(repo).updateGovTalkStatusLock(updateGovTalkStatusLockRequest).futureValue mustBe govTalkStatusReturn

      verify(repo).sdltUpdateGovTalkStatusLock(updateGovTalkStatusLockRequest)
    }

    "updateGovTalkStatistics delegates to repo.sdltUpdateGovTalkStatistics and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltUpdateGovTalkStatistics(updateGovTalkStatisticsRequest))
        .thenReturn(Future.successful(govTalkStatusReturn))

      newService(repo).updateGovTalkStatistics(updateGovTalkStatisticsRequest).futureValue mustBe govTalkStatusReturn

      verify(repo).sdltUpdateGovTalkStatistics(updateGovTalkStatisticsRequest)
    }

    "deleteGovTalkStatus delegates to repo.sdltDeleteGovTalkStatus and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltDeleteGovTalkStatus(deleteGovTalkStatusRequest)).thenReturn(Future.successful(govTalkStatusReturn))

      newService(repo).deleteGovTalkStatus(deleteGovTalkStatusRequest).futureValue mustBe govTalkStatusReturn

      verify(repo).sdltDeleteGovTalkStatus(deleteGovTalkStatusRequest)
    }

    "selectGovTalkStatus delegates to repo.sdltSelectGovTalkStatus and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltSelectGovTalkStatus(selectGovTalkStatusRequest))
        .thenReturn(Future.successful(selectGovTalkStatusResponse))

      newService(repo).selectGovTalkStatus(selectGovTalkStatusRequest).futureValue mustBe selectGovTalkStatusResponse

      verify(repo).sdltSelectGovTalkStatus(selectGovTalkStatusRequest)
    }

    "selectGovTalkFormResultId delegates to repo.sdltSelectGovTalkFormResultId and returns its result" in {
      val repo = mock[SdltFormpRepository]
      when(repo.sdltSelectGovTalkFormResultId(selectGovTalkFormResultIdRequest))
        .thenReturn(Future.successful(selectGovTalkFormResultIdResponse))

      newService(repo)
        .selectGovTalkFormResultId(selectGovTalkFormResultIdRequest)
        .futureValue mustBe selectGovTalkFormResultIdResponse

      verify(repo).sdltSelectGovTalkFormResultId(selectGovTalkFormResultIdRequest)
    }
  }
}
