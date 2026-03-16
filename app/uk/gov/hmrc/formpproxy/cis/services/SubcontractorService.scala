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

import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.models.GetSubcontractorList
import uk.gov.hmrc.formpproxy.cis.models.response.GetSubcontractorListResponse
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource
import uk.gov.hmrc.formpproxy.cis.models.CreateAndUpdateSubcontractorDatabaseRecord

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SubcontractorService @Inject() (repo: CisMonthlyReturnSource) {

  def createAndUpdateSubcontractor(req: CreateAndUpdateSubcontractorRequest): Future[Unit] =
    repo.createAndUpdateSubcontractor(toDbRecord(req))

  def getSubcontractorList(req: GetSubcontractorList): Future[GetSubcontractorListResponse] =
    repo.getSubcontractorList(req.cisId)

  private def toDbRecord(req: CreateAndUpdateSubcontractorRequest): CreateAndUpdateSubcontractorDatabaseRecord =
    req match {
      case s: CreateAndUpdateSubcontractorRequest.SoleTraderRequest =>
        CreateAndUpdateSubcontractorDatabaseRecord(
          cisId = s.cisId,
          subcontractorType = s.subcontractorType,
          utr = s.utr,
          partnerUtr = None,
          crn = None,
          firstName = s.firstName,
          secondName = s.secondName,
          surname = s.surname,
          nino = s.nino,
          partnershipTradingName = None,
          tradingName = s.tradingName,
          addressLine1 = s.addressLine1,
          addressLine2 = s.addressLine2,
          city = s.city,
          county = s.county,
          country = s.country,
          postcode = s.postcode,
          emailAddress = s.emailAddress,
          phoneNumber = s.phoneNumber,
          mobilePhoneNumber = s.mobilePhoneNumber,
          worksReferenceNumber = s.worksReferenceNumber
        )

      case c: CreateAndUpdateSubcontractorRequest.CompanyRequest =>
        CreateAndUpdateSubcontractorDatabaseRecord(
          cisId = c.cisId,
          subcontractorType = c.subcontractorType,
          utr = c.utr,
          partnerUtr = None,
          crn = c.crn,
          firstName = None,
          secondName = None,
          surname = None,
          nino = None,
          partnershipTradingName = None,
          tradingName = c.tradingName,
          addressLine1 = c.addressLine1,
          addressLine2 = c.addressLine2,
          city = c.city,
          county = c.county,
          country = c.country,
          postcode = c.postcode,
          emailAddress = c.emailAddress,
          phoneNumber = c.phoneNumber,
          mobilePhoneNumber = c.mobilePhoneNumber,
          worksReferenceNumber = c.worksReferenceNumber
        )

      case p: CreateAndUpdateSubcontractorRequest.PartnershipRequest =>
        CreateAndUpdateSubcontractorDatabaseRecord(
          cisId = p.cisId,
          subcontractorType = p.subcontractorType,
          utr = p.utr,
          partnerUtr = p.partnerUtr,
          crn = p.crn,
          firstName = None,
          secondName = None,
          surname = None,
          nino = p.nino,
          partnershipTradingName = p.partnershipTradingName,
          tradingName = p.tradingName,
          addressLine1 = p.addressLine1,
          addressLine2 = p.addressLine2,
          city = p.city,
          county = p.county,
          country = p.country,
          postcode = p.postcode,
          emailAddress = p.emailAddress,
          phoneNumber = p.phoneNumber,
          mobilePhoneNumber = p.mobilePhoneNumber,
          worksReferenceNumber = p.worksReferenceNumber
        )
    }
}
