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
import uk.gov.hmrc.formpproxy.sdlt.models.agents.*
import uk.gov.hmrc.formpproxy.sdlt.models.returns.SdltReturnRecordResponse
import uk.gov.hmrc.formpproxy.sdlt.models.vendor.*
import uk.gov.hmrc.formpproxy.sdlt.models.purchaser.*
import uk.gov.hmrc.formpproxy.sdlt.models.land.*
import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository

import javax.inject.Inject
import scala.concurrent.Future

class ReturnService @Inject() (repo: SdltFormpRepository) {

  def createSDLTReturn(req: CreateReturnRequest): Future[String] =
    repo.sdltCreateReturn(req)

  def getSDLTReturn(returnResourceRef: String, storn: String): Future[GetReturnRequest] =
    repo.sdltGetReturn(returnResourceRef = returnResourceRef, storn = storn)

  def getSDLTReturns(request: GetReturnRecordsRequest): Future[SdltReturnRecordResponse] =
    repo.sdltGetReturns(request)

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

  def createPurchaser(req: CreatePurchaserRequest): Future[CreatePurchaserReturn] =
    repo.sdltCreatePurchaser(req)

  def updatePurchaser(req: UpdatePurchaserRequest): Future[UpdatePurchaserReturn] =
    repo.sdltUpdatePurchaser(req)

  def deletePurchaser(req: DeletePurchaserRequest): Future[DeletePurchaserReturn] =
    repo.sdltDeletePurchaser(req)

  def createCompanyDetails(req: CreateCompanyDetailsRequest): Future[CreateCompanyDetailsReturn] =
    repo.sdltCreateCompanyDetails(req)

  def updateCompanyDetails(req: UpdateCompanyDetailsRequest): Future[UpdateCompanyDetailsReturn] =
    repo.sdltUpdateCompanyDetails(req)

  def deleteCompanyDetails(req: DeleteCompanyDetailsRequest): Future[DeleteCompanyDetailsReturn] =
    repo.sdltDeleteCompanyDetails(req)

  def createLand(req: CreateLandRequest): Future[CreateLandReturn] =
    repo.sdltCreateLand(req)

  def updateLand(req: UpdateLandRequest): Future[UpdateLandReturn] =
    repo.sdltUpdateLand(req)

  def deleteLand(req: DeleteLandRequest): Future[DeleteLandReturn] =
    repo.sdltDeleteLand(req)

  def updateReturn(request: UpdateReturnRequest): Future[UpdateReturnReturn] =
    repo.sdltUpdateReturn(request)

}
