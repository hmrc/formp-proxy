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

import uk.gov.hmrc.formpproxy.models.requests.CreateNilMonthlyReturnRequest
import uk.gov.hmrc.formpproxy.models.response.CreateNilMonthlyReturnResponse
import uk.gov.hmrc.formpproxy.repositories.CisMonthlyReturnSource
import uk.gov.hmrc.formpproxy.models.UserMonthlyReturns

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class MonthlyReturnService @Inject()(repo: CisMonthlyReturnSource)() {

  def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns] =
    repo.getAllMonthlyReturns(instanceId)

  def createNilMonthlyReturn(request: CreateNilMonthlyReturnRequest): Future[CreateNilMonthlyReturnResponse] =
    repo.createNilMonthlyReturn(request)

  def getSchemeEmail(instanceId: String): Future[Option[String]] =
    repo.getSchemeEmail(instanceId)
}