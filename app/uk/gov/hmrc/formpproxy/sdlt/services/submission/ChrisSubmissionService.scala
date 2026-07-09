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

import uk.gov.hmrc.formpproxy.sdlt.models.submission.*
import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository

import javax.inject.Inject
import scala.concurrent.Future

class ChrisSubmissionService @Inject() (repo: SdltFormpRepository) {

  def lockReturn(req: LockReturnRequest): Future[LockReturnResponse] =
    repo.sdltLockReturn(req)

  def createSubmission(req: CreateSubmissionRequest): Future[CreateSubmissionReturn] =
    repo.sdltCreateSubmission(req)

  def updateSubmission(req: UpdateSubmissionRequest): Future[UpdateSubmissionReturn] =
    repo.sdltUpdateSubmission(req)

  def createSubmissionErrorDetail(req: CreateSubmissionErrorDetailRequest): Future[CreateSubmissionErrorDetailReturn] =
    repo.sdltCreateSubmissionErrorDetail(req)

  def deleteSubmissionErrorDetail(req: DeleteSubmissionErrorDetailRequest): Future[DeleteSubmissionErrorDetailReturn] =
    repo.sdltDeleteSubmissionErrorDetail(req)

  def insertInitialGovTalkStatus(req: InsertInitialGovTalkStatusRequest): Future[GovTalkStatusReturn] =
    repo.sdltInsertInitialGovTalkStatus(req)

  def resetGovTalkStatus(req: ResetGovTalkStatusRequest): Future[GovTalkStatusReturn] =
    repo.sdltResetGovTalkStatus(req)

  def updateGovTalkStatus(req: UpdateGovTalkStatusRequest): Future[GovTalkStatusReturn] =
    repo.sdltUpdateGovTalkStatus(req)

  def updateGovTalkStatusCorrelationId(req: UpdateGovTalkStatusCorrelationIdRequest): Future[GovTalkStatusReturn] =
    repo.sdltUpdateGovTalkStatusCorrelationId(req)

  def updateGovTalkStatusLock(req: UpdateGovTalkStatusLockRequest): Future[GovTalkStatusReturn] =
    repo.sdltUpdateGovTalkStatusLock(req)

  def updateGovTalkStatistics(req: UpdateGovTalkStatisticsRequest): Future[GovTalkStatusReturn] =
    repo.sdltUpdateGovTalkStatistics(req)

  def deleteGovTalkStatus(req: DeleteGovTalkStatusRequest): Future[GovTalkStatusReturn] =
    repo.sdltDeleteGovTalkStatus(req)

  def selectGovTalkStatus(req: SelectGovTalkStatusRequest): Future[SelectGovTalkStatusResponse] =
    repo.sdltSelectGovTalkStatus(req)

  def selectGovTalkFormResultId(req: SelectGovTalkFormResultIdRequest): Future[SelectGovTalkFormResultIdResponse] =
    repo.sdltSelectGovTalkFormResultId(req)

}
