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

package uk.gov.hmrc.formpproxy.cis

import play.api.Logging
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateNilMonthlyReturnRequest, CreateSubmissionRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.formpproxy.cis.models.response.CreateNilMonthlyReturnResponse
import uk.gov.hmrc.formpproxy.cis.models.{ContractorScheme, MonthlyReturn, UserMonthlyReturns}
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource
import uk.gov.hmrc.formpproxy.cis.utils.StubUtils

import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong
import javax.inject.{Inject, Singleton}
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

@Singleton
class CisFormpStub @Inject() (stubUtils: StubUtils) extends CisMonthlyReturnSource with Logging {

  private[this] val stubData = stubUtils
  private val idSeq          = new AtomicLong(1000L)
  private val schemeVersions = TrieMap.empty[String, Long]
  private val storedReturns  = TrieMap.empty[(String, Int, Int), MonthlyReturn]

  override def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns] = {
    logger.info(s"[Stub] getAllMonthlyReturns(instanceId=$instanceId) -> returning Jan, Feb, Mar 2025")
    val monthlyReturns: Seq[MonthlyReturn] = Seq(1, 2, 3).map(stubData.generateMonthlyReturns)
    Future.successful(UserMonthlyReturns(monthlyReturns))
  }

  override def createNilMonthlyReturn(
    request: CreateNilMonthlyReturnRequest
  ): Future[CreateNilMonthlyReturnResponse] = {
    val monthlyReturnId = idSeq.getAndIncrement()

    schemeVersions.updateWith(request.instanceId)(v => Some(v.getOrElse(0L) + 1L))

    val now           = LocalDateTime.now()
    val monthlyReturn = MonthlyReturn(
      monthlyReturnId = monthlyReturnId,
      taxYear = request.taxYear,
      taxMonth = request.taxMonth,
      nilReturnIndicator = Some("Y"),
      decEmpStatusConsidered = None,
      decAllSubsVerified = None,
      decInformationCorrect = Some(request.decInformationCorrect),
      decNoMoreSubPayments = None,
      decNilReturnNoPayments = Some(request.decNilReturnNoPayments),
      status = Some("STARTED"),
      lastUpdate = Some(now),
      amendment = Some("N"),
      supersededBy = None
    )
    storedReturns.put((request.instanceId, request.taxYear, request.taxMonth), monthlyReturn)

    Future.successful(CreateNilMonthlyReturnResponse(status = "STARTED"))
  }

  def getSchemeVersion(instanceId: String): Long =
    schemeVersions.getOrElse(instanceId, 0L)

  def getStoredReturn(instanceId: String, taxYear: Int, taxMonth: Int): Option[MonthlyReturn] =
    storedReturns.get((instanceId, taxYear, taxMonth))

  override def getSchemeEmail(instanceId: String): Future[Option[String]] =
    Future.successful(Some("test@test.com"))

  def reset(): Unit = {
    idSeq.set(1_000_000L)
    schemeVersions.clear()
    storedReturns.clear()
  }

  override def createSubmission(req: CreateSubmissionRequest): Future[String] = {
    logger.info(s"[Stub] createAndTrackSubmission(${req.instanceId}, ${req.taxYear}, ${req.taxMonth})")
    Future.successful("90001")
  }

  override def updateMonthlyReturnSubmission(req: UpdateSubmissionRequest): Future[Unit] = {
    logger.info(
      s"[Stub] updateMrSubmission(${req.instanceId}, ${req.taxYear}, ${req.taxMonth}, status=${req.submittableStatus})"
    )
    Future.successful(())
  }

  override def getScheme(instanceId: String): Future[Option[ContractorScheme]] = Future.successful(Some(ContractorScheme(
    schemeId = 1,
    instanceId = "111",
    accountsOfficeReference = "222",
    taxOfficeNumber = "123",
    taxOfficeReference = "AB123456"
  )))
}
