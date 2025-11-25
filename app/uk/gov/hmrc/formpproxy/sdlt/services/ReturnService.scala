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

package uk.gov.hmrc.formpproxy.sdlt.services

import uk.gov.hmrc.formpproxy.sdlt.models.*
import uk.gov.hmrc.formpproxy.sdlt.models.returnAgent.*
import uk.gov.hmrc.formpproxy.sdlt.models.vendor.*
import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository

import javax.inject.Inject
import scala.concurrent.Future

class ReturnService @Inject() (repo: SdltFormpRepository) {

  def createSDLTReturn(req: CreateReturnRequest): Future[String] =
    repo.sdltCreateReturn(req)

  def getSDLTReturn(returnResourceRef: String, storn: String): Future[GetReturnRequest] =
    repo.sdltGetReturn(returnResourceRef = returnResourceRef, storn = storn)

  def createVendor(req: CreateVendorRequest): Future[CreateVendorReturn] =
    repo.sdltCreateVendor(req)

  def updateVendor(req: UpdateVendorRequest): Future[UpdateVendorReturn] =
    repo.sdltUpdateVendor(req)

  def deleteVendor(req: DeleteVendorRequest): Future[DeleteVendorReturn] =
    repo.sdltDeleteVendor(req)

  def createReturnAgent(req: CreateReturnAgentRequest): Future[CreateReturnAgentReturn] =
    repo.sdltCreateReturnAgent(req)

  def updateReturnAgent(req: UpdateReturnAgentRequest): Future[UpdateReturnAgentReturn] =
    repo.sdltUpdateReturnAgent(req)

  def deleteReturnAgent(req: DeleteReturnAgentRequest): Future[DeleteReturnAgentReturn] =
    repo.sdltDeleteReturnAgent(req)

  def updateReturnVersion(req: ReturnVersionUpdateRequest): Future[ReturnVersionUpdateReturn] =
    repo.sdltUpdateReturnVersion(req)

}
