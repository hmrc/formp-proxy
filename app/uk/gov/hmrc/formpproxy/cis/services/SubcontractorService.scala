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

package uk.gov.hmrc.formpproxy.cis.services

import uk.gov.hmrc.formpproxy.cis.models.SubcontractorType
import uk.gov.hmrc.formpproxy.cis.models.requests.UpdateSubcontractorRequest
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SubcontractorService @Inject() (repo: CisMonthlyReturnSource) {

  def createSubcontractor(schemeId: Int, subcontractorType: SubcontractorType, version: Int): Future[Int] =
    repo.createSubcontractor(schemeId, subcontractorType, version)

  def updateSubcontractor(req: UpdateSubcontractorRequest): Future[Unit] =
    repo.updateSubcontractor(req)

  def createAndUpdateSubcontractor(req: CreateAndUpdateSubcontractorRequest): Future[Unit] =
    repo.createAndUpdateSubcontractor(req)
}
