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

package uk.gov.hmrc.formpproxy.charities.repositories

import oracle.jdbc.OracleTypes
import play.api.Logging
import play.api.db.Database
import play.api.db.NamedDatabase
import uk.gov.hmrc.formpproxy.charities.models.*
import uk.gov.hmrc.formpproxy.shared.utils.CallableStatementUtils.*

import java.lang.Long
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Using

trait CharitiesSource {
  def getTotalUnregulatedDonations(charityReference: String): Future[Option[BigDecimal]]
  def saveUnregulatedDonation(charityReference: String, request: SaveUnregulatedDonationRequest): Future[Unit]
}

private final case class SchemeRow(schemeId: Long, version: Option[Int], email: Option[String])

@Singleton
class CharitiesFormpRepository @Inject() (@NamedDatabase("charities") db: Database)(implicit ec: ExecutionContext)
    extends CharitiesSource
    with Logging {

  override def getTotalUnregulatedDonations(charityReference: String): Future[Option[BigDecimal]] = {
    logger.info(s"[CHARITIES] getTotalUnregulatedDonations(charityReference=$charityReference)")
    Future {
      db.withConnection { conn =>
        val cs = conn.prepareCall(
          "{ call UNREGULATED_DONATIONS_PK.getTotalUnregulatedDonations(?, ?) }"
        )
        try {
          cs.setString(1, charityReference)
          cs.registerOutParameter(2, OracleTypes.NUMBER)
          cs.execute()

          val amount = cs.getBigDecimal(2)
          if amount == null
          then None
          else Some(amount)
        } finally cs.close()
      }
    }
  }

  override def saveUnregulatedDonation(
    charityReference: String,
    request: SaveUnregulatedDonationRequest
  ): Future[Unit] =
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall("{ call UNREGULATED_DONATIONS_PK.createUnregulatedDonation(?, ?, ?) }")) { cs =>
          cs.setString(1, charityReference)
          cs.setInt(2, request.claimId)
          cs.setBigDecimal(3, request.amount.underlying())
          cs.execute()
        }
      }
    }

}
