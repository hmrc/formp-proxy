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

package uk.gov.hmrc.formpproxy.utils

import play.api.Logging
import uk.gov.hmrc.formpproxy.repositories.CisMonthlyReturnSource
import uk.gov.hmrc.formpproxy.models.{MonthlyReturn, UserMonthlyReturns}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class CisFormpStub @Inject()(stubUtils: StubUtils) extends CisMonthlyReturnSource with Logging {

  private[this] val stubData = stubUtils

  def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns] =
    val monthlyReturns: Seq[MonthlyReturn] = Seq(1, 2, 3).map(stubData.generateMonthlyReturns)
    Future.successful(UserMonthlyReturns(monthlyReturns))
}