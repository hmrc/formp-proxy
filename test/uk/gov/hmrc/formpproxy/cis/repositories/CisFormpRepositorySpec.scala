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

package uk.gov.hmrc.formpproxy.cis.repositories

import org.mockito.ArgumentMatchers.{any as anyArg, eq as eqTo}
import org.mockito.Mockito.*
import play.api.db.Database
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateNilMonthlyReturnRequest, CreateSubmissionRequest, UpdateSubmissionRequest}

import java.sql.*

final class CisFormpRepositorySpec extends SpecBase {

  "getAllMonthlyReturns" - {

    "parse one monthly row and close resources" in {
      val db       = mock[Database]
      val conn     = mock[Connection]
      val cs       = mock[CallableStatement]
      val rsScheme = mock[ResultSet]
      val rsMonthly= mock[ResultSet]

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
      verify(cs).execute()
    }

    "return empty when monthly cursor has no rows" in {
      val db        = mock[Database]
      val conn      = mock[Connection]
      val cs        = mock[CallableStatement]
      val rsMonthly = mock[ResultSet]

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
      verify(conn).prepareCall(eqTo("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }"))
      verify(cs).execute()
    }
  }

  "createNilMonthlyReturn" - {

    "call underlying procedures with correct parameters and return STARTED" in {
      val db = mock[Database]
      val conn = mock[java.sql.Connection]
      val csCreate = mock[CallableStatement]
      val csVersion = mock[CallableStatement]
      val csUpdate = mock[CallableStatement]
      val csGetScheme = mock[CallableStatement]
      val rsScheme = mock[java.sql.ResultSet]

      when(db.withTransaction(org.mockito.ArgumentMatchers.any[java.sql.Connection => Any]))
        .thenAnswer { inv =>
          val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
        }


      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"))
        .thenReturn(csGetScheme)
      when(csGetScheme.getObject(eqTo(2), eqTo(classOf[ResultSet])))
        .thenReturn(rsScheme)
      when(rsScheme.next()).thenReturn(true, false)
      when(rsScheme.getInt("version")).thenReturn(0)

      when(conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }"))
        .thenReturn(csCreate)
      when(conn.prepareCall("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"))
        .thenReturn(csVersion)
      when(conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"))
        .thenReturn(csUpdate)

      val repo = new CisFormpRepository(db)

      val request = CreateNilMonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 2,
        decInformationCorrect = "Y",
        decNilReturnNoPayments = "Y"
      )

      val out = repo.createNilMonthlyReturn(request).futureValue
      out.status mustBe "STARTED"

      verify(conn).prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }")
      verify(csCreate).execute()

      verify(conn).prepareCall("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }")
      verify(csVersion).execute()

      verify(conn).prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
      verify(csUpdate).execute()
    }
  }

  "createSubmission" - {

    "look up ids, call SPs, return the submission id, and close resources" in {
      val db       = mock[Database]
      val conn     = mock[Connection]
      val rsMonthly= mock[ResultSet]
      val csAll     = mock[CallableStatement]
      val csCreate = mock[CallableStatement]


      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }"))).thenReturn(csAll)
      when(csAll.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsMonthly)
      when(rsMonthly.next()).thenReturn(true, false)
      when(rsMonthly.getInt("tax_year")).thenReturn(2024)
      when(rsMonthly.getInt("tax_month")).thenReturn(4)
      when(rsMonthly.getLong("monthly_return_id")).thenReturn(7777L)

      when(conn.prepareCall(eqTo("{ call SUBMISSION_PROCS.Create_Submission(?, ?, ?, ?, ?, ?, ?, ?, ?) }"))).thenReturn(csCreate)
      when(csCreate.getLong(9)).thenReturn(12345L)

      val repo = new CisFormpRepository(db)

      val req = CreateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        hmrcMarkGenerated = Some("Dj5TVJDyRYCn9zta5EdySeY4fyA="),
        emailRecipient = Some("test@test.com"),
        agentId = None,
        subcontractorCount = Some(2),
        totalPaymentsMade = Some(BigDecimal(1000)),
        totalTaxDeducted = Some(BigDecimal(200))
      )

      val out = repo.createSubmission(req).futureValue
      out mustBe "12345"

      verify(csCreate).execute()
    }
  }

  "updateMonthlyReturnSubmission" - {

    "look up ids, call update SP with correct params, and close resources" in {
      val db       = mock[Database]
      val conn     = mock[Connection]
      val rsScheme = mock[ResultSet]
      val rsMonthly = mock[ResultSet]
      val csScheme = mock[CallableStatement]
      val csAll = mock[CallableStatement]
      val csUpdate = mock[CallableStatement]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"))).thenReturn(csScheme)
      when(csScheme.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(rsScheme.next()).thenReturn(true)
      when(rsScheme.getLong("scheme_id")).thenReturn(9999L)

      when(conn.prepareCall(eqTo("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }"))).thenReturn(csAll)
      when(csAll.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsMonthly)
      when(rsMonthly.next()).thenReturn(true, false)
      when(rsMonthly.getInt("tax_year")).thenReturn(2024)
      when(rsMonthly.getInt("tax_month")).thenReturn(4)
      when(rsMonthly.getLong("monthly_return_id")).thenReturn(7777L)

      when(conn.prepareCall(eqTo("{ call SUBMISSION_PROCS_2016.UPDATE_MR_SUBMISSION(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(csUpdate)

      val repo = new CisFormpRepository(db)

      val req = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
        hmrcMarkGgis = None,
        emailRecipient = Some("test@test.com"),
        submissionRequestDate = None,
        acceptedTime = None,
        agentId = None,
        submittableStatus = "ACCEPTED",
        govtalkErrorCode = None,
        govtalkErrorType = None,
        govtalkErrorMessage = None,
        amendment = None
      )

      repo.updateMonthlyReturnSubmission(req).futureValue

      verify(csUpdate).execute()
      verify(conn).prepareCall(eqTo("{ call SUBMISSION_PROCS_2016.UPDATE_MR_SUBMISSION(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"))
    }
  }

  "loadScheme" - {

    "throw when int_Get_Scheme returns null cursor" in {
      val db  = mock[Database]
      val connection   = mock[Connection]
      val cs  = mock[CallableStatement]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any])
        f(connection)
      }
      when(connection.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(null)

      val repo = new CisFormpRepository(db)
      val method = classOf[CisFormpRepository].getDeclaredMethod("loadScheme", classOf[Connection], classOf[String])
      method.setAccessible(true)

      val thrown = intercept[java.lang.reflect.InvocationTargetException] {
        method.invoke(repo, connection, "abc-123")
      }
      val cause = thrown.getCause.asInstanceOf[RuntimeException]
      cause.getMessage must include("null cursor")
    }

    "throw when no SCHEME row for instance id" in {
      val db = mock[Database]
      val connection = mock[Connection]
      val cs = mock[CallableStatement]
      val rs = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any])
        f(connection)
      }
      when(connection.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(false)

      val repo = new CisFormpRepository(db)
      val method = classOf[CisFormpRepository].getDeclaredMethod("loadScheme", classOf[Connection], classOf[String])
      method.setAccessible(true)

      val thrown = intercept[java.lang.reflect.InvocationTargetException] {
        method.invoke(repo, connection, "abc-123")
      }
      val cause = thrown.getCause.asInstanceOf[RuntimeException]
      cause.getMessage must include("No SCHEME row")
    }
  }

  "getMonthlyReturnId" - {

    "throw when Get_All_Monthly_Returns returns null monthly cursor (outer null check)" in {
      val db = mock[Database]
      val connection  = mock[Connection]
      val cs = mock[CallableStatement]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any])
        f(connection)
      }
      when(connection.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(null) // ← null monthly cursor

      val repo = new CisFormpRepository(db)
      val method = classOf[CisFormpRepository].getDeclaredMethod(
        "getMonthlyReturnId", classOf[Connection], classOf[String], classOf[Int], classOf[Int]
      )
      method.setAccessible(true)
      val thrown = intercept[java.lang.reflect.InvocationTargetException] {
        method.invoke(repo, connection, "abc-123", Int.box(2025), Int.box(2))
      }
      val cause = thrown.getCause.asInstanceOf[RuntimeException]
      cause.getMessage must include("null monthly cursor")
    }

    "throw when no MONTHLY_RETURN matches the requested year/month" in {
      val db = mock[Database]
      val connection  = mock[Connection]
      val cs = mock[CallableStatement]
      val rs = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any])
        f(connection)
      }
      when(connection.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rs)

      when(rs.next()).thenReturn(true, true, false)
      when(rs.getInt("tax_year")).thenReturn(2024, 2025)
      when(rs.getInt("tax_month")).thenReturn(12, 1)

      val repo = new CisFormpRepository(db)
      val method = classOf[CisFormpRepository].getDeclaredMethod(
        "getMonthlyReturnId", classOf[Connection], classOf[String], classOf[Int], classOf[Int]
      )
      method.setAccessible(true)
      val thrown = intercept[java.lang.reflect.InvocationTargetException] {
        method.invoke(repo, connection, "abc-123", Int.box(2025), Int.box(2))
      }
      val cause = thrown.getCause.asInstanceOf[RuntimeException]
      cause.getMessage must include("No MONTHLY_RETURN for instance_id=abc-123 year=2025 month=2")
    }
  }

  "getSchemeEmail" - {

    "call SCHEME_PROCS.int_Get_Scheme and return Some(email) when email is found" in {
      val db = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs = mock[CallableStatement]
      val rs = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)
      when(rs.getString("email_address")).thenReturn("test@example.com")

      val repo = new CisFormpRepository(db)
      val result = repo.getSchemeEmail("abc-123").futureValue

      result mustBe Some("test@example.com")

      verify(cs).execute()
      verify(rs).close()
    }

    "call SCHEME_PROCS.int_Get_Scheme and return None when email is null" in {
      val db = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs = mock[CallableStatement]
      val rs = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)
      when(rs.getString("email_address")).thenReturn(null)

      val repo = new CisFormpRepository(db)
      val result = repo.getSchemeEmail("abc-123").futureValue

      result mustBe None

      verify(cs).execute()
      verify(rs).close()
    }

    "call SCHEME_PROCS.int_Get_Scheme and return None when email is empty string" in {
      val db = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs = mock[CallableStatement]
      val rs = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)
      when(rs.getString("email_address")).thenReturn("   ")

      val repo = new CisFormpRepository(db)
      val result = repo.getSchemeEmail("abc-123").futureValue

      result mustBe None

      verify(cs).execute()
      verify(rs).close()
    }

    "call SCHEME_PROCS.int_Get_Scheme and return Some(email) when email has whitespace that gets trimmed" in {
      val db = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs = mock[CallableStatement]
      val rs = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)
      when(rs.getString("email_address")).thenReturn("  test@example.com  ")

      val repo = new CisFormpRepository(db)
      val result = repo.getSchemeEmail("abc-123").futureValue

      result mustBe Some("test@example.com")

      verify(cs).execute()
      verify(rs).close()
    }
  }
}