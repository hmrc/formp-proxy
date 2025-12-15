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

import org.mockito.ArgumentMatchers.any as anyArg
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.*
import play.api.db.Database
import uk.gov.hmrc.formpproxy.base.SpecBase

import java.sql.*
import uk.gov.hmrc.formpproxy.charities.models.SaveUnregulatedDonationRequest

final class CharitiesFormpRepositorySpec extends SpecBase {

  "getTotalUnregulatedDonations" - {

    "parse one row and close resources" in {
      val db        = mock[Database]
      val conn      = mock[Connection]
      val cs        = mock[CallableStatement]
      val resultSet = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(resultSet)

      when(resultSet.next()).thenReturn(true, false)
      when(resultSet.getBigDecimal("p_total")).thenReturn(BigDecimal("1234.56").underlying())

      val repo = new CharitiesFormpRepository(db)
      val out  = repo.getTotalUnregulatedDonations("abc-123").futureValue

      out mustBe Some(BigDecimal("1234.56"))

      verify(conn).prepareCall("{ call UNREGULATED_DONATIONS_PK.getTotalUnregulatedDonations(?) }")
      verify(cs).execute()
    }

    "return empty when cursor is null" in {
      val db        = mock[Database]
      val conn      = mock[Connection]
      val cs        = mock[CallableStatement]
      val resultSet = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall(anyArg[String])).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(null)
      when(resultSet.next()).thenReturn(false)

      val repo = new CharitiesFormpRepository(db)
      val out  = repo.getTotalUnregulatedDonations("abc-123").futureValue

      out mustBe None
      verify(conn).prepareCall(eqTo("{ call UNREGULATED_DONATIONS_PK.getTotalUnregulatedDonations(?) }"))
      verify(cs).execute()
    }

    "return empty when cursor has no rows" in {
      val db        = mock[Database]
      val conn      = mock[Connection]
      val cs        = mock[CallableStatement]
      val resultSet = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(resultSet)

      when(resultSet.next()).thenReturn(false)

      val repo = new CharitiesFormpRepository(db)
      val out  = repo.getTotalUnregulatedDonations("abc-123").futureValue

      out mustBe None

      verify(conn).prepareCall("{ call UNREGULATED_DONATIONS_PK.getTotalUnregulatedDonations(?) }")
      verify(cs).execute()
    }
  }

  "saveUnregulatedDonation" - {
    "save a donation and close resources" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call UNREGULATED_DONATIONS_PK.createUnregulatedDonation(?, ?, ?) }")))
        .thenReturn(cs)

      val repo = new CharitiesFormpRepository(db)
      repo
        .saveUnregulatedDonation(
          charityReference = "abc-123",
          SaveUnregulatedDonationRequest(
            claimId = 123,
            amount = BigDecimal("1234.56")
          )
        )
        .futureValue

      verify(conn).prepareCall(eqTo("{ call UNREGULATED_DONATIONS_PK.createUnregulatedDonation(?, ?, ?) }"))
      verify(cs).execute()
    }
  }
}
