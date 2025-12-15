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

package uk.gov.hmrc.formpproxy.charities.services

import scala.concurrent.Future
import uk.gov.hmrc.formpproxy.charities.models.SaveUnregulatedDonationRequest
import uk.gov.hmrc.formpproxy.charities.repositories.CharitiesFormpRepository
import javax.inject.Inject
import javax.inject.Singleton
import com.google.inject.ImplementedBy

@ImplementedBy(classOf[UnregulatedDonationsServiceImpl])
trait UnregulatedDonationsService {

  /**
    * Sum of all the unregulated donations for the given charity reference
    *
    * @param charityReference
    * @return The total of all the unregulated donations for the given charity reference
    */
  def getTotalUnregulatedDonations(charityReference: String): Future[Option[BigDecimal]]

  /**
    * Save a unregulated donation for the given charity reference
    *
    * @param request - The request containing the charity reference, claim id and amount
    * @return nothing
    */
  def saveUnregulatedDonation(charityReference: String, request: SaveUnregulatedDonationRequest): Future[Unit]
}

@Singleton
class UnregulatedDonationsServiceImpl @Inject() (repo: CharitiesFormpRepository) extends UnregulatedDonationsService {
  
  override def getTotalUnregulatedDonations(charityReference: String): Future[Option[BigDecimal]] =
    repo.getTotalUnregulatedDonations(charityReference)
  
  override def saveUnregulatedDonation(charityReference: String, request: SaveUnregulatedDonationRequest): Future[Unit] =
    repo.saveUnregulatedDonation(charityReference, request)
}

