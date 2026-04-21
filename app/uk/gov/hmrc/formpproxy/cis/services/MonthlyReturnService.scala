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

import uk.gov.hmrc.formpproxy.cis.models.{UnsubmittedMonthlyReturns, UserMonthlyReturns}
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.models.response.*
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MonthlyReturnService @Inject() (repo: CisMonthlyReturnSource)(implicit ec: ExecutionContext) {

  def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns] =
    repo.getAllMonthlyReturns(instanceId)

  def getUnsubmittedMonthlyReturns(instanceId: String): Future[UnsubmittedMonthlyReturns] =
    repo.getUnsubmittedMonthlyReturns(instanceId).map { unsubmitted =>
      unsubmitted.copy(
        monthlyReturn = unsubmitted.monthlyReturn.map { monthlyReturn =>
          monthlyReturn.copy(
            status = monthlyReturn.status.map(mapStatusForUnsubmittedReturns)
          )
        }
      )
    }

  private def mapStatusForUnsubmittedReturns(raw: String): String =
    raw.trim.toUpperCase match {
      case "STARTED"            => "STARTED"
      case "VALIDATED"          => "VALIDATED"
      case "PENDING"            => "PENDING"
      case "ACCEPTED"           => "PENDING"
      case "DEPARTMENTAL_ERROR" => "REJECTED"
      case "FATAL_ERROR"        => "REJECTED"
      case other                => other
    }

  def createNilMonthlyReturn(request: CreateNilMonthlyReturnRequest): Future[CreateNilMonthlyReturnResponse] =
    repo.createNilMonthlyReturn(request)

  def updateMonthlyReturn(request: UpdateMonthlyReturnRequest): Future[Unit] =
    repo.updateMonthlyReturn(request)

  def updateMonthlyReturnItem(request: UpdateMonthlyReturnItemRequest): Future[Unit] =
    repo.updateMonthlyReturnItem(request)

  def createMonthlyReturn(request: CreateMonthlyReturnRequest): Future[Unit] =
    repo.createMonthlyReturn(request)

  def getSchemeEmail(instanceId: String): Future[Option[String]] =
    repo.getSchemeEmail(instanceId)

  def getMonthlyReturnForEdit(request: GetMonthlyReturnForEditRequest): Future[GetMonthlyReturnForEditResponse] =
    repo.getMonthlyReturnForEdit(
      instanceId = request.instanceId,
      taxYear = request.taxYear,
      taxMonth = request.taxMonth
    )

  def syncMonthlyReturnItems(request: SyncMonthlyReturnItemsRequest): Future[Unit] =
    repo.syncMonthlyReturnItems(request)

  def deleteMonthlyReturnItem(request: DeleteMonthlyReturnItemRequest): Future[Unit] =
    repo.deleteMonthlyReturnItem(request)

  def deleteUnsubmittedMonthlyReturn(request: DeleteUnsubmittedMonthlyReturnRequest): Future[Unit] =
    repo.deleteUnsubmittedMonthlyReturn(request)
}
