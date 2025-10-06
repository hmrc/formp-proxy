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

package uk.gov.hmrc.formpproxy.services

import uk.gov.hmrc.formpproxy.repositories.CisMonthlyReturnSource
import uk.gov.hmrc.formpproxy.models.UserMonthlyReturns
import uk.gov.hmrc.formpproxy.models.UserMonthlyReturns

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class MonthlyReturnService @Inject()(repo: CisMonthlyReturnSource)() {

  def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns] =
    repo.getAllMonthlyReturns(instanceId)

  def createMonthlyReturn(instanceId: String, taxYear: Int, taxMonth: Int, nilReturnIndicator: String): Future[Unit] =
    repo.createMonthlyReturn(instanceId, taxYear, taxMonth, nilReturnIndicator)

  def updateSchemeVersion(instanceId: String, version: Int): Future[Int] =
    repo.updateSchemeVersion(instanceId, version)

  def updateMonthlyReturn(
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    amendment: String,
    decEmpStatusConsidered: Option[String],
    decAllSubsVerified: Option[String],
    decInformationCorrect: Option[String],
    decNoMoreSubPayments: Option[String],
    decNilReturnNoPayments: Option[String],
    nilReturnIndicator: String,
    status: String,
    version: Int
  ): Future[Int] =
    repo.updateMonthlyReturn(
      instanceId,
      taxYear,
      taxMonth,
      amendment,
      decEmpStatusConsidered,
      decAllSubsVerified,
      decInformationCorrect,
      decNoMoreSubPayments,
      decNilReturnNoPayments,
      nilReturnIndicator,
      status,
      version
    )
}