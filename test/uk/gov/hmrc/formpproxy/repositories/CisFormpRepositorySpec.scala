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
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(null)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsMonthly)
      when(rsMonthly.next()).thenReturn(false)

      val repo = new CisFormpRepository(db)
      val out = repo.getAllMonthlyReturns("abc-123").futureValue

      out.monthlyReturnList mustBe empty
      verify(rsMonthly).close()
      verify(cs).close()
    }
  }

  "createNilMonthlyReturn" should {

    "call underlying procedures with correct parameters and return STARTED" in {
      val db = mock(classOf[Database])
      val conn = mock(classOf[java.sql.Connection])
      val csCreate = mock(classOf[CallableStatement])
      val csVersion = mock(classOf[CallableStatement])
      val csUpdate = mock(classOf[CallableStatement])
      val csGetScheme = mock(classOf[CallableStatement])
      val rsScheme = mock(classOf[java.sql.ResultSet])

      when(db.withTransaction(org.mockito.ArgumentMatchers.any[java.sql.Connection => Any]))
        .thenAnswer { inv =>
          val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
        }


      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"))
        .thenReturn(csGetScheme)
      when(csGetScheme.getObject(2)).thenReturn(rsScheme)
      when(rsScheme.next()).thenReturn(true, false)
      when(rsScheme.getInt("version")).thenReturn(3)

      when(conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }"))
        .thenReturn(csCreate)
      when(conn.prepareCall("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"))
        .thenReturn(csVersion)
      when(conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"))
        .thenReturn(csUpdate)

      val repo = new CisFormpRepository(db)

      val request = uk.gov.hmrc.formpproxy.models.requests.CreateNilMonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 2,
        decInformationCorrect = "Y",
        decNilReturnNoPayments = "Y"
      )

      val out = repo.createNilMonthlyReturn(request).futureValue
      out.status mustBe "STARTED"


      verify(conn).prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")
      verify(csGetScheme).setString(1, "abc-123")
      verify(csGetScheme).registerOutParameter(2, java.sql.Types.REF_CURSOR)
      verify(csGetScheme).execute()
      verify(csGetScheme).getObject(2)
      verify(rsScheme).next()
      verify(rsScheme).getInt("version")
      verify(rsScheme).close()
      verify(csGetScheme).close()

      verify(conn).prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }")
      verify(csCreate).setString(1, "abc-123")
      verify(csCreate).setInt(2, 2025)
      verify(csCreate).setInt(3, 2)
      verify(csCreate).setString(4, "Y")
      verify(csCreate).execute()
      verify(csCreate).close()

      verify(conn).prepareCall("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }")
      verify(csVersion).setString(1, "abc-123")
      verify(csVersion).setInt(2, 3)
      verify(csVersion).registerOutParameter(2, java.sql.Types.INTEGER)
      verify(csVersion).execute()
      verify(csVersion).getInt(2)
      verify(csVersion).close()

      verify(conn).prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
      verify(csUpdate).setString(1, "abc-123")
      verify(csUpdate).setInt(2, 2025)
      verify(csUpdate).setInt(3, 2)
      verify(csUpdate).setString(4, "N")
      verify(csUpdate).setNull(5, java.sql.Types.VARCHAR)
      verify(csUpdate).setNull(6, java.sql.Types.VARCHAR)
      verify(csUpdate).setString(7, "Y")
      verify(csUpdate).setNull(8, java.sql.Types.CHAR)
      verify(csUpdate).setString(9, "Y")
      verify(csUpdate).setString(10, "Y")
      verify(csUpdate).setString(11, "STARTED")
      verify(csUpdate).setInt(12, 0)
      verify(csUpdate).registerOutParameter(12, java.sql.Types.INTEGER)
      verify(csUpdate).execute()
      verify(csUpdate).close()
    }
  }

  "getSchemeEmail" should {

    "call SCHEME_PROCS.int_Get_Scheme and return Some(email) when email is found" in {
      val db = mock(classOf[Database])
      val conn = mock(classOf[java.sql.Connection])
      val cs = mock(classOf[CallableStatement])
      val rs = mock(classOf[ResultSet])

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(2)).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)
      when(rs.getString("email_address")).thenReturn("test@example.com")

      val repo = new CisFormpRepository(db)
      val result = repo.getSchemeEmail("abc-123").futureValue

      result mustBe Some("test@example.com")

      verify(conn).prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")
      verify(cs).setString(1, "abc-123")
      verify(cs).registerOutParameter(2, java.sql.Types.REF_CURSOR)
      verify(cs).execute()
      verify(cs).getObject(2)
      verify(rs).next()
      verify(rs).getString("email_address")
      verify(rs).close()
      verify(cs).close()
    }

    "call SCHEME_PROCS.int_Get_Scheme and return None when email is null" in {
      val db = mock(classOf[Database])
      val conn = mock(classOf[java.sql.Connection])
      val cs = mock(classOf[CallableStatement])
      val rs = mock(classOf[ResultSet])

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(2)).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)
      when(rs.getString("email_address")).thenReturn(null)

      val repo = new CisFormpRepository(db)
      val result = repo.getSchemeEmail("abc-123").futureValue

      result mustBe None

      verify(conn).prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")
      verify(cs).setString(1, "abc-123")
      verify(cs).registerOutParameter(2, java.sql.Types.REF_CURSOR)
      verify(cs).execute()
      verify(cs).getObject(2)
      verify(rs).next()
      verify(rs).getString("email_address")
      verify(rs).close()
      verify(cs).close()
    }

    "call SCHEME_PROCS.int_Get_Scheme and return None when email is empty string" in {
      val db = mock(classOf[Database])
      val conn = mock(classOf[java.sql.Connection])
      val cs = mock(classOf[CallableStatement])
      val rs = mock(classOf[ResultSet])

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(2)).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)
      when(rs.getString("email_address")).thenReturn("   ")

      val repo = new CisFormpRepository(db)
      val result = repo.getSchemeEmail("abc-123").futureValue

      result mustBe None

      verify(conn).prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")
      verify(cs).setString(1, "abc-123")
      verify(cs).registerOutParameter(2, java.sql.Types.REF_CURSOR)
      verify(cs).execute()
      verify(cs).getObject(2)
      verify(rs).next()
      verify(rs).getString("email_address")
      verify(rs).close()
      verify(cs).close()
    }

    "call SCHEME_PROCS.int_Get_Scheme and return Some(email) when email has whitespace that gets trimmed" in {
      val db = mock(classOf[Database])
      val conn = mock(classOf[java.sql.Connection])
      val cs = mock(classOf[CallableStatement])
      val rs = mock(classOf[ResultSet])

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(2)).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)
      when(rs.getString("email_address")).thenReturn("  test@example.com  ")

      val repo = new CisFormpRepository(db)
      val result = repo.getSchemeEmail("abc-123").futureValue

      result mustBe Some("test@example.com")

      verify(conn).prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")
      verify(cs).setString(1, "abc-123")
      verify(cs).registerOutParameter(2, java.sql.Types.REF_CURSOR)
      verify(cs).execute()
      verify(cs).getObject(2)
      verify(rs).next()
      verify(rs).getString("email_address")
      verify(rs).close()
      verify(cs).close()
    }
  }
}