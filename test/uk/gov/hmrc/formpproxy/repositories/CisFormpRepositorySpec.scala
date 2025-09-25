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

package uk.gov.hmrc.formpproxy.repositories

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any => anyArg, eq => eqTo}
import play.api.db.Database
import java.sql.{CallableStatement, ResultSet, Timestamp}
import oracle.jdbc.OracleTypes
import scala.concurrent.ExecutionContext.Implicits.global

final class CisFormpRepositorySpec
  extends AnyWordSpec with Matchers with ScalaFutures {

  "getAllMonthlyReturns" should {

    "parse one monthly row and close resources" in {
      val db = mock(classOf[Database])
      val conn = mock(classOf[java.sql.Connection])
      val cs = mock(classOf[CallableStatement])
      val rsScheme = mock(classOf[ResultSet])
      val rsMonthly = mock(classOf[ResultSet])

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsMonthly)

      when(rsMonthly.next()).thenReturn(true, false)
      when(rsMonthly.getLong("monthly_return_id")).thenReturn(66666L)
      when(rsMonthly.getInt("tax_year")).thenReturn(2025)
      when(rsMonthly.getInt("tax_month")).thenReturn(1)
      when(rsMonthly.getString("status")).thenReturn("Submitted")
      when(rsMonthly.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2025-01-31 12:34:56"))
      when(rsMonthly.getLong("superseded_by")).thenReturn(0L)
      when(rsMonthly.wasNull()).thenReturn(true)

      val repo = new CisFormpRepository(db)
      val out = repo.getAllMonthlyReturns("abc-123").futureValue

      out.monthlyReturnList must have size 1
      val mr = out.monthlyReturnList.head
      mr.monthlyReturnId mustBe 66666L
      mr.taxYear mustBe 2025
      mr.taxMonth mustBe 1
      mr.status mustBe Some("Submitted")
      mr.lastUpdate.isDefined mustBe true
      mr.supersededBy mustBe None

      verify(conn).prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }")
      verify(cs).setString(1, "abc-123")
      verify(cs).registerOutParameter(2, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(3, OracleTypes.CURSOR)
      verify(cs).execute()
      verify(rsScheme).close()
      verify(rsMonthly).close()
      verify(cs).close()
    }

    "return empty when monthly cursor has no rows" in {
      val db = mock(classOf[Database])
      val conn = mock(classOf[java.sql.Connection])
      val cs = mock(classOf[CallableStatement])
      val rsMonthly = mock(classOf[ResultSet])

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall(anyArg[String])).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(null) // scheme cursor can be null
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsMonthly)
      when(rsMonthly.next()).thenReturn(false)

      val repo = new CisFormpRepository(db)
      val out = repo.getAllMonthlyReturns("abc-123").futureValue

      out.monthlyReturnList mustBe empty
      verify(rsMonthly).close()
      verify(cs).close()
    }
  }
}