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

package uk.gov.hmrc.formpproxy.cis.services

import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateSubmissionRequest, GetSubmittedVerificationsRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource
import uk.gov.hmrc.formpproxy.cis.utils.GovTalkErrorMapper
import uk.gov.hmrc.formpproxy.cis.models.response.GetSubmittedVerificationsResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SubmissionService @Inject() (repo: CisMonthlyReturnSource) {

  def createSubmission(req: CreateSubmissionRequest): Future[String] =
    repo.createSubmission(req)

  def updateSubmission(req: UpdateSubmissionRequest): Future[Unit] = {
    val mappedReq = req.govTalkResponse match {
      case Some(status) =>
        val mapped = GovTalkErrorMapper(status)
        req.copy(
          govtalkErrorCode = mapped.errorCode,
          govtalkErrorType = mapped.errorType,
          govtalkErrorMessage = mapped.errorMessage
        )
      case None         =>
        req
    }
    repo.updateMonthlyReturnSubmission(mappedReq)
  }

  def getSubmittedVerifications(
    req: GetSubmittedVerificationsRequest
  ): Future[GetSubmittedVerificationsResponse] =
    repo.getSubmittedVerifications(req)
}
