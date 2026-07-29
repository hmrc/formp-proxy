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

package uk.gov.hmrc.formpproxy.sdlt.repositories

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.formpproxy.sdlt.models.{GetReturnRecordsRequest, GetReturnsForPurgeRequest}
import uk.gov.hmrc.formpproxy.sdlt.models.returns.{ReturnForPurge, ReturnSummary, ReturnsForPurgeResponse, SdltReturnRecordResponse, SubmissionForPolling, SubmissionsForPollingResponse}

import java.time.LocalDate

trait SdltFormpRepoDataHelper {
  val expectedReturnsSummary: List[ReturnSummary]      = List(
    ReturnSummary(
      returnReference = "REF01",
      utrn = Some("UTR001"),
      status = Some("SUBMITTED"),
      dateSubmitted = Some(LocalDate.parse("2025-01-01")),
      purchaserName = "purchaserName1",
      address = "Address 11",
      agentReference = Some("Agent 11")
    ),
    ReturnSummary(
      returnReference = "REF02",
      utrn = Some("UTR003"),
      status = Some("SUBMITTED"),
      dateSubmitted = Some(LocalDate.parse("2025-02-03")),
      purchaserName = "purchaserName2",
      address = "Address 22",
      agentReference = Some("Agent 22")
    ),
    ReturnSummary(
      returnReference = "REF03",
      utrn = Some("UTR004"),
      dateSubmitted = Some(LocalDate.parse("2025-02-04")),
      purchaserName = "purchaserName3",
      address = "Address 23",
      agentReference = Some("Agent 23")
    )
  )
  val expectedReturnsSummaryEmpty: List[ReturnSummary] = List.empty
  val expectedResponse: SdltReturnRecordResponse       = SdltReturnRecordResponse(
    returnSummaryCount = Some(expectedReturnsSummary.length),
    returnSummaryList = expectedReturnsSummary
  )
  val actualResponse: SdltReturnRecordResponse         = SdltReturnRecordResponse(
    returnSummaryCount = Some(expectedReturnsSummary.length),
    returnSummaryList = expectedReturnsSummary
  )

  val requestReturns: GetReturnRecordsRequest = GetReturnRecordsRequest(
    storn = "STORN12345",
    status = None,
    deletionFlag = false,
    pageType = Some("SUBMITTED"),
    pageNumber = None
  )

  case class InvalidRecordsRequest(
    statusA: Option[String],
    pageTypeA: Option[String],
    pageNumberA: Option[String] = None
  )

  object InvalidRecordsRequest {
    implicit val format: OFormat[InvalidRecordsRequest] = Json.format[InvalidRecordsRequest]
  }

  val requestReturnsInvalid: InvalidRecordsRequest = InvalidRecordsRequest(
    statusA = Some("ACTIVE"),
    pageTypeA = None,
    pageNumberA = None
  )

  val requestReturnsOther: GetReturnRecordsRequest = GetReturnRecordsRequest(
    storn = "STORN12347",
    status = None,
    deletionFlag = false,
    pageType = None,
    pageNumber = None
  )

  val expectedReturnsForPurge: List[ReturnForPurge]      = List(
    ReturnForPurge(storn = "STORN12345", returnResourceRef = "REF01", status = "SUBMITTED"),
    ReturnForPurge(storn = "STORN12345", returnResourceRef = "REF02", status = "STARTED")
  )
  val expectedReturnsForPurgeEmpty: List[ReturnForPurge] = List.empty
  val returnsForPurgeResponse: ReturnsForPurgeResponse   = ReturnsForPurgeResponse(
    returnsForPurge = expectedReturnsForPurge
  )

  val requestReturnsForPurge: GetReturnsForPurgeRequest = GetReturnsForPurgeRequest(
    purgeDate = LocalDate.parse("2026-06-29")
  )

  val expectedSubmissionsForPolling: List[SubmissionForPolling]      = List(
    SubmissionForPolling(
      submissionId = "9001",
      storn = "STORN12345",
      returnResourceRef = "REF01",
      submissionStatus = "ACCEPTED"
    ),
    SubmissionForPolling(
      submissionId = "9002",
      storn = "STORN12345",
      returnResourceRef = "REF02",
      submissionStatus = "ACCEPTED"
    )
  )
  val expectedSubmissionsForPollingEmpty: List[SubmissionForPolling] = List.empty
  val submissionsForPollingResponse: SubmissionsForPollingResponse   = SubmissionsForPollingResponse(
    submissions = expectedSubmissionsForPolling
  )
}
