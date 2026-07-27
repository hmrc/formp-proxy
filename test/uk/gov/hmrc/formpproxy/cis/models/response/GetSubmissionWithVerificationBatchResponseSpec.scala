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

package uk.gov.hmrc.formpproxy.cis.models.response

import java.time.LocalDateTime
import play.api.libs.json.Json
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.{ContractorScheme, Subcontractor, Submission, Verification, VerificationBatch}

class GetSubmissionWithVerificationBatchResponseSpec extends SpecBase {

  "GetSubmissionWithVerificationBatchResponse" - {

    "support JSON round-trip with populated records" in {
      val dateTime = LocalDateTime.parse("2026-07-14T10:55:00")

      val model =
        GetSubmissionWithVerificationBatchResponse(
          scheme = Some(
            ContractorScheme(
              schemeId = 123,
              instanceId = "abc-123",
              accountsOfficeReference = "123PA00123456",
              taxOfficeNumber = "163",
              taxOfficeReference = "AB0063",
              name = Some("ABC Construction Ltd")
            )
          ),
          submission = Some(
            Submission(
              submissionId = 555L,
              submissionType = "VERIFICATIONS",
              activeObjectId = Some(99L),
              status = Some("SUBMITTED"),
              hmrcMarkGenerated = Some("generated-mark"),
              hmrcMarkGgis = None,
              emailRecipient = None,
              acceptedTime = None,
              createDate = None,
              lastUpdate = None,
              schemeId = 123L,
              agentId = None,
              l_Migrated = None,
              submissionRequestDate = Some(dateTime),
              govTalkErrorCode = None,
              govTalkErrorType = None,
              govTalkErrorMessage = None
            )
          ),
          verificationBatch = Some(
            VerificationBatch(
              verificationBatchId = 99L,
              schemeId = 123L,
              verificationsCounter = Some(1L),
              verifBatchResourceRef = Some(77L),
              proceedSession = Some("Y"),
              confirmArrangement = None,
              confirmCorrect = None,
              status = Some("SUBMITTED"),
              verificationNumber = None,
              createDate = None,
              lastUpdate = None,
              version = Some(1)
            )
          ),
          verifications = Seq(
            Verification(
              verificationId = 1001L,
              matched = None,
              verificationNumber = None,
              taxTreatment = None,
              actionIndicator = Some("VERIFY"),
              verificationBatchId = Some(99L),
              schemeId = Some(123L),
              subcontractorId = Some(1L),
              subcontractorName = Some("Smith, John"),
              verificationResourceRef = Some(10L),
              proceed = Some("Y"),
              createDate = None,
              lastUpdate = None,
              version = None
            )
          ),
          subcontractors = Seq(
            Subcontractor(
              subcontractorId = 1L,
              utr = Some("8786438047"),
              pageVisited = None,
              partnerUtr = None,
              crn = None,
              firstName = Some("John"),
              nino = None,
              secondName = None,
              surname = Some("Smith"),
              partnershipTradingName = None,
              tradingName = Some("Smith Construction"),
              subcontractorType = Some("soletrader"),
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
              createDate = None,
              lastUpdate = None,
              subbieResourceRef = Some(10L),
              matched = None,
              autoVerified = None,
              verified = None,
              verificationNumber = None,
              taxTreatment = None,
              verificationDate = None,
              version = None,
              updatedTaxTreatment = None,
              lastMonthlyReturnDate = None,
              pendingVerifications = None
            )
          )
        )

      val json = Json.toJson(model)

      json.as[GetSubmissionWithVerificationBatchResponse] mustBe model
      (json \ "subcontractors" \ 0 \ "displayName").as[String] mustBe "Smith, John"
    }
  }
}
