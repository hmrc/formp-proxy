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

import uk.gov.hmrc.formpproxy.sdlt.models.GetReturnRecordsRequest
import uk.gov.hmrc.formpproxy.sdlt.models.returns.{ReturnSummary, SdltReturnRecordResponse}

import java.time.LocalDate

trait SdltFormpRepoDataHelper {
  val expectedReturnsSummary: List[ReturnSummary]      = List(
    ReturnSummary(
      returnReference = "REF01",
      utrn = Some("UTR001"),
      status = "ACTIVE",
      dateSubmitted = Some(LocalDate.parse("2025-01-01")),
      purchaserName = "purchaserName1",
      address = "Address 11",
      agentReference = Some("Agent 11")
    ),
    ReturnSummary(
      returnReference = "REF02",
      utrn = Some("UTR003"),
      status = "SUBMITTED",
      dateSubmitted = Some(LocalDate.parse("2025-02-03")),
      purchaserName = "purchaserName2",
      address = "Address 22",
      agentReference = Some("Agent 22")
    )
  )
  val expectedReturnsSummaryEmpty: List[ReturnSummary] = List.empty

  val expectedResponse: SdltReturnRecordResponse = SdltReturnRecordResponse(
    returnSummaryCount = Some(1),
    returnSummaryList = expectedReturnsSummary
  )

  val actualResponse: SdltReturnRecordResponse = SdltReturnRecordResponse(
    returnSummaryCount = Some(1),
    returnSummaryList = expectedReturnsSummary
  )

  val requestReturns = GetReturnRecordsRequest(
    storn = "STORN12345",
    status = None,
    deletionFlag = false,
    pageType = None,
    pageNumber = None
  )

}
