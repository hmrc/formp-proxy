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

import oracle.jdbc.OracleTypes
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any as anyArg, anyInt, eq as eqTo}
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import org.mockito.invocation.InvocationOnMock
import play.api.db.Database
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.*
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.models.response.GetSubcontractorForDeleteResponse
import uk.gov.hmrc.formpproxy.shared.utils.CallableStatementUtils.*

import java.sql.*
import java.time.{Instant, LocalDateTime}

final class CisFormpRepositorySpec extends SpecBase {

  "getAllMonthlyReturns" - {

    "parse one monthly row and close resources" in {
      val db        = mock[Database]
      val conn      = mock[Connection]
      val cs        = mock[CallableStatement]
      val rsScheme  = mock[ResultSet]
      val rsMonthly = mock[ResultSet]

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
      val out  = repo.getAllMonthlyReturns("abc-123").futureValue

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
      val rsIgnored = mock[ResultSet]
      val rsMonthly = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(2, classOf[ResultSet])).thenReturn(rsIgnored)
      when(rsIgnored.next()).thenReturn(false)

      when(cs.getObject(3, classOf[ResultSet])).thenReturn(rsMonthly)
      when(rsMonthly.next()).thenReturn(false)

      val repo = new CisFormpRepository(db)
      val out  = repo.getAllMonthlyReturns("abc-123").futureValue

      out.monthlyReturnList mustBe empty

      verify(conn).prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }")
      verify(cs).setString(1, "abc-123")
      verify(cs).registerOutParameter(2, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(3, OracleTypes.CURSOR)
      verify(cs).execute()
    }
  }

  "createNilMonthlyReturn" - {

    "call Create_Monthly_Return SP only and return STARTED" in {
      val db       = mock[Database]
      val conn     = mock[java.sql.Connection]
      val csCreate = mock[CallableStatement]

      when(db.withConnection(org.mockito.ArgumentMatchers.any[java.sql.Connection => Any]))
        .thenAnswer { inv =>
          val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
        }

      when(conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }"))
        .thenReturn(csCreate)

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

      verify(conn, never).prepareCall("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }")
      verify(conn, never).prepareCall(
        "{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
      )
    }
  }

  "updateMonthlyReturn" - {

    "call Update_Monthly_Return and Update_Scheme_Version SPs in transaction" in {
      val db          = mock[Database]
      val conn        = mock[Connection]
      val csUpdate    = mock[CallableStatement]
      val csVersion   = mock[CallableStatement]
      val csGetScheme = mock[CallableStatement]
      val rsScheme    = mock[ResultSet]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"))
        .thenReturn(csGetScheme)
      when(csGetScheme.getObject(eqTo(2), eqTo(classOf[ResultSet])))
        .thenReturn(rsScheme)
      when(rsScheme.next()).thenReturn(true, false)
      when(rsScheme.getInt("version")).thenReturn(1)

      val updateCall = "{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
      when(conn.prepareCall(eqTo(updateCall))).thenReturn(csUpdate)

      val versionCall = "{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"
      when(conn.prepareCall(eqTo(versionCall))).thenReturn(csVersion)

      val repo = new CisFormpRepository(db)
      val req  = UpdateMonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 2,
        amendment = "N",
        decInformationCorrect = Some("Y"),
        decNilReturnNoPayments = Some("Y"),
        nilReturnIndicator = "Y",
        status = "STARTED",
        version = Some(1L)
      )

      repo.updateMonthlyReturn(req).futureValue

      verify(conn).prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")
      verify(csGetScheme).execute()
      verify(conn).prepareCall(eqTo(updateCall))
      verify(csUpdate).execute()
      verify(conn).prepareCall(eqTo(versionCall))
      verify(csVersion).execute()
    }
  }

  "createSubmission" - {

    "look up ids, call SPs, return the submission id, and close resources" in {
      val db        = mock[Database]
      val conn      = mock[Connection]
      val rsIgnored = mock[ResultSet]
      val rsMonthly = mock[ResultSet]
      val csAll     = mock[CallableStatement]
      val csCreate  = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }"))
        .thenReturn(csAll)

      when(csAll.getObject(2, classOf[ResultSet])).thenReturn(rsIgnored)
      when(rsIgnored.next()).thenReturn(false)

      when(csAll.getObject(3, classOf[ResultSet])).thenReturn(rsMonthly)
      when(rsMonthly.next()).thenReturn(true, false)
      when(rsMonthly.getInt("tax_year")).thenReturn(2024)
      when(rsMonthly.getInt("tax_month")).thenReturn(4)
      when(rsMonthly.getLong("monthly_return_id")).thenReturn(7777L)
      when(rsMonthly.getString("amendment")).thenReturn("N")

      when(conn.prepareCall("{ call SUBMISSION_PROCS.Create_Submission(?, ?, ?, ?, ?, ?, ?, ?, ?) }"))
        .thenReturn(csCreate)
      when(csCreate.getLong(9)).thenReturn(12345L)

      val repo = new CisFormpRepository(db)

      val req = CreateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        amendment = "N",
        hmrcMarkGenerated = Some("Dj5TVJDyRYCn9zta5EdySeY4fyA="),
        emailRecipient = Some("test@test.com"),
        agentId = None,
        subcontractorCount = Some(2),
        totalPaymentsMade = Some(BigDecimal(1000)),
        totalTaxDeducted = Some(BigDecimal(200))
      )

      val out = repo.createSubmission(req).futureValue
      out mustBe "12345"

      verify(csCreate).setString(1, "123")
      verify(csCreate).setString(2, "MONTHLY_RETURN")
      verify(csCreate).setLong(3, 7777L)
      verify(csCreate).setString(4, "Dj5TVJDyRYCn9zta5EdySeY4fyA=")
      verify(csCreate).setString(5, null)
      verify(csCreate).setString(6, "test@test.com")
      verify(csCreate).setString(7, null)
      verify(csCreate).setString(8, "STARTED")
      verify(csCreate).execute()
    }
  }

  "updateMonthlyReturnSubmission" - {

    "look up ids, call update SP with correct params, and close resources" in {
      val db        = mock[Database]
      val conn      = mock[Connection]
      val rsScheme  = mock[ResultSet]
      val rsIgnored = mock[ResultSet]
      val rsMonthly = mock[ResultSet]
      val csScheme  = mock[CallableStatement]
      val csAll     = mock[CallableStatement]
      val csUpdate  = mock[CallableStatement]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }

      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(csScheme)
      when(csScheme.getObject(2, classOf[ResultSet])).thenReturn(rsScheme)
      when(rsScheme.next()).thenReturn(true)
      when(rsScheme.getLong("scheme_id")).thenReturn(9999L)

      when(conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }"))
        .thenReturn(csAll)

      when(csAll.getObject(2, classOf[ResultSet])).thenReturn(rsIgnored)
      when(rsIgnored.next()).thenReturn(false)

      when(csAll.getObject(3, classOf[ResultSet])).thenReturn(rsMonthly)
      when(rsMonthly.next()).thenReturn(true, false)
      when(rsMonthly.getInt("tax_year")).thenReturn(2024)
      when(rsMonthly.getInt("tax_month")).thenReturn(4)
      when(rsMonthly.getLong("monthly_return_id")).thenReturn(7777L)
      when(rsMonthly.getString("amendment")).thenReturn("N")

      when(
        conn.prepareCall(
          "{ call SUBMISSION_PROCS_2016.UPDATE_MR_SUBMISSION(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
        )
      ).thenReturn(csUpdate)

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
        amendment = "N"
      )

      repo.updateMonthlyReturnSubmission(req).futureValue

      verify(csUpdate).execute()
      verify(conn).prepareCall(
        "{ call SUBMISSION_PROCS_2016.UPDATE_MR_SUBMISSION(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
      )
    }
  }

  "loadScheme" - {

    "throw when int_Get_Scheme returns null cursor" in {
      val db         = mock[Database]
      val connection = mock[Connection]
      val cs         = mock[CallableStatement]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any])
        f(connection)
      }
      when(connection.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(null)

      val repo   = new CisFormpRepository(db)
      val method = classOf[CisFormpRepository].getDeclaredMethod("loadScheme", classOf[Connection], classOf[String])
      method.setAccessible(true)

      val thrown = intercept[java.lang.reflect.InvocationTargetException] {
        method.invoke(repo, connection, "abc-123")
      }
      val cause  = thrown.getCause.asInstanceOf[RuntimeException]
      cause.getMessage must include("null cursor")
    }

    "throw when no SCHEME row for instance id" in {
      val db         = mock[Database]
      val connection = mock[Connection]
      val cs         = mock[CallableStatement]
      val rs         = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any])
        f(connection)
      }
      when(connection.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(false)

      val repo   = new CisFormpRepository(db)
      val method = classOf[CisFormpRepository].getDeclaredMethod("loadScheme", classOf[Connection], classOf[String])
      method.setAccessible(true)

      val thrown = intercept[java.lang.reflect.InvocationTargetException] {
        method.invoke(repo, connection, "abc-123")
      }
      val cause  = thrown.getCause.asInstanceOf[RuntimeException]
      cause.getMessage must include("No SCHEME row")
    }
  }

  "getMonthlyReturnId" - {

    "throw when Get_All_Monthly_Returns returns null monthly cursor" in {
      val db         = mock[Database]
      val connection = mock[Connection]
      val cs         = mock[CallableStatement]
      val rsIgnored  = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any])
        f(connection)
      }

      when(connection.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }"))
        .thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsIgnored)
      when(rsIgnored.next()).thenReturn(false)

      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(null)

      val repo   = new CisFormpRepository(db)
      val method = classOf[CisFormpRepository].getDeclaredMethod(
        "getMonthlyReturnId",
        classOf[Connection],
        classOf[String],
        classOf[Int],
        classOf[Int],
        classOf[String]
      )
      method.setAccessible(true)

      val thrown = intercept[java.lang.reflect.InvocationTargetException] {
        method.invoke(repo, connection, "abc-123", Int.box(2025), Int.box(2), "N")
      }

      val cause = thrown.getCause.asInstanceOf[RuntimeException]
      cause.getMessage must include("SP returned null cursor at index 3")
    }

    "throw when no MONTHLY_RETURN matches the requested year/month" in {
      val db         = mock[Database]
      val connection = mock[Connection]
      val cs         = mock[CallableStatement]
      val rsIgnored  = mock[ResultSet]
      val rs         = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any])
        f(connection)
      }

      when(connection.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }"))
        .thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsIgnored)
      when(rsIgnored.next()).thenReturn(false)

      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rs)

      when(rs.next()).thenReturn(true, true, false)
      when(rs.getInt("tax_year")).thenReturn(2024, 2025)
      when(rs.getInt("tax_month")).thenReturn(12, 1)

      val repo   = new CisFormpRepository(db)
      val method = classOf[CisFormpRepository].getDeclaredMethod(
        "getMonthlyReturnId",
        classOf[Connection],
        classOf[String],
        classOf[Int],
        classOf[Int],
        classOf[String]
      )
      method.setAccessible(true)

      val thrown = intercept[java.lang.reflect.InvocationTargetException] {
        method.invoke(repo, connection, "abc-123", Int.box(2025), Int.box(2), "N")
      }

      val cause = thrown.getCause.asInstanceOf[RuntimeException]
      cause.getMessage must include("No MONTHLY_RETURN for instance_id=abc-123 year=2025 month=2")
    }
  }

  "getSchemeEmail" - {

    "call SCHEME_PROCS.int_Get_Scheme and return Some(email) when email is found" in {
      val db   = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs   = mock[CallableStatement]
      val rs   = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)
      when(rs.getString("email_address")).thenReturn("test@example.com")

      val repo   = new CisFormpRepository(db)
      val result = repo.getSchemeEmail("abc-123").futureValue

      result mustBe Some("test@example.com")

      verify(cs).execute()
      verify(rs).close()
    }

    "call SCHEME_PROCS.int_Get_Scheme and return None when email is null" in {
      val db   = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs   = mock[CallableStatement]
      val rs   = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)
      when(rs.getString("email_address")).thenReturn(null)

      val repo   = new CisFormpRepository(db)
      val result = repo.getSchemeEmail("abc-123").futureValue

      result mustBe None

      verify(cs).execute()
      verify(rs).close()
    }

    "call SCHEME_PROCS.int_Get_Scheme and return None when email is empty string" in {
      val db   = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs   = mock[CallableStatement]
      val rs   = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)
      when(rs.getString("email_address")).thenReturn("   ")

      val repo   = new CisFormpRepository(db)
      val result = repo.getSchemeEmail("abc-123").futureValue

      result mustBe None

      verify(cs).execute()
      verify(rs).close()
    }

    "call SCHEME_PROCS.int_Get_Scheme and return Some(email) when email has whitespace that gets trimmed" in {
      val db   = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs   = mock[CallableStatement]
      val rs   = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)
      when(rs.getString("email_address")).thenReturn("  test@example.com  ")

      val repo   = new CisFormpRepository(db)
      val result = repo.getSchemeEmail("abc-123").futureValue

      result mustBe Some("test@example.com")

      verify(cs).execute()
      verify(rs).close()
    }
  }

  "getScheme" - {

    "returns Some(ContractorScheme) with all fields when result set has data" in {
      val db   = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs   = mock[CallableStatement]
      val rs   = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)

      when(rs.getInt("scheme_id")).thenReturn(123)
      when(rs.getString("instance_id")).thenReturn("abc-123")
      when(rs.getString("aoref")).thenReturn("123456789")
      when(rs.getString("tax_office_number")).thenReturn("0001")
      when(rs.getString("tax_office_reference")).thenReturn("AB12345")
      when(rs.getString("utr")).thenReturn("1234567890")
      when(rs.getString("name")).thenReturn("Test Contractor")
      when(rs.getString("email_address")).thenReturn("test@example.com")
      when(rs.getString("display_welcome_page")).thenReturn("Y")
      when(rs.getInt("pre_pop_count")).thenReturn(5)
      when(rs.wasNull()).thenReturn(false)
      when(rs.getString("pre_pop_successful")).thenReturn("Y")
      when(rs.getInt("subcontractor_counter")).thenReturn(10)
      when(rs.wasNull()).thenReturn(false)
      when(rs.getInt("verif_batch_counter")).thenReturn(2)
      when(rs.wasNull()).thenReturn(false)
      when(rs.getTimestamp("last_update")).thenReturn(Timestamp.from(Instant.parse("2025-12-04T10:15:30Z")))
      when(rs.getInt("version")).thenReturn(1)
      when(rs.wasNull()).thenReturn(false)
      when(rs.getInt("version")).thenReturn(1)
      when(rs.wasNull()).thenReturn(false)

      val repo   = new CisFormpRepository(db)
      val result = repo.getScheme("abc-123").futureValue

      result must be(Symbol("defined"))
      val scheme = result.get
      scheme.schemeId mustBe 123
      scheme.instanceId mustBe "abc-123"
      scheme.accountsOfficeReference mustBe "123456789"
      scheme.taxOfficeNumber mustBe "0001"
      scheme.taxOfficeReference mustBe "AB12345"
      scheme.utr mustBe Some("1234567890")
      scheme.name mustBe Some("Test Contractor")
      scheme.emailAddress mustBe Some("test@example.com")
      scheme.displayWelcomePage mustBe Some("Y")
      scheme.prePopCount mustBe Some(5)
      scheme.prePopSuccessful mustBe Some("Y")
      scheme.subcontractorCounter mustBe Some(10)
      scheme.verificationBatchCounter mustBe Some(2)
      scheme.lastUpdate mustBe Some(Instant.parse("2025-12-04T10:15:30Z"))
      scheme.version mustBe Some(1)

      verify(cs).execute()
      verify(rs).close()
    }

    "returns None when result set is empty" in {
      val db   = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs   = mock[CallableStatement]
      val rs   = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(false)

      val repo   = new CisFormpRepository(db)
      val result = repo.getScheme("unknown-123").futureValue

      result mustBe None

      verify(cs).execute()
      verify(rs).close()
    }

    "handles null optional fields correctly" in {
      val db   = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs   = mock[CallableStatement]
      val rs   = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rs)
      when(rs.next()).thenReturn(true, false)

      when(rs.getInt("scheme_id")).thenReturn(456)
      when(rs.getString("instance_id")).thenReturn("def-456")
      when(rs.getString("aoref")).thenReturn("987654321")
      when(rs.getString("tax_office_number")).thenReturn("0002")
      when(rs.getString("tax_office_reference")).thenReturn("CD67890")
      when(rs.getString("utr")).thenReturn(null)
      when(rs.getString("name")).thenReturn(null)
      when(rs.getString("email_address")).thenReturn(null)
      when(rs.getString("display_welcome_page")).thenReturn(null)
      when(rs.getInt("pre_pop_count")).thenReturn(0)
      when(rs.wasNull()).thenReturn(true)
      when(rs.getString("pre_pop_successful")).thenReturn(null)
      when(rs.getInt("subcontractor_counter")).thenReturn(0)
      when(rs.wasNull()).thenReturn(true)
      when(rs.getInt("verif_batch_counter")).thenReturn(0)
      when(rs.wasNull()).thenReturn(true)
      when(rs.getString("last_update")).thenReturn(null)
      when(rs.getInt("version")).thenReturn(0)
      when(rs.wasNull()).thenReturn(true)

      val repo   = new CisFormpRepository(db)
      val result = repo.getScheme("def-456").futureValue

      result must be(Symbol("defined"))
      val scheme = result.get
      scheme.schemeId mustBe 456
      scheme.instanceId mustBe "def-456"
      scheme.utr mustBe None
      scheme.name mustBe None
      scheme.emailAddress mustBe None
      scheme.displayWelcomePage mustBe None
      scheme.prePopCount mustBe None
      scheme.prePopSuccessful mustBe None
      scheme.subcontractorCounter mustBe None
      scheme.verificationBatchCounter mustBe None
      scheme.lastUpdate mustBe None
      scheme.version mustBe None

      verify(cs).execute()
      verify(rs).close()
    }

    "throws RuntimeException when result set cursor is null" in {
      val db   = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(null)

      val repo = new CisFormpRepository(db)
      val ex   = repo.getScheme("null-cursor-123").failed.futureValue

      ex mustBe a[RuntimeException]
      ex.getMessage must include("SP returned null cursor at index 2")

      verify(cs).execute()
    }
  }

  "updateScheme" - {

    "call SCHEME_PROCS.Update_Scheme with correct parameters and return version" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(eqTo("{ call SCHEME_PROCS.Update_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(cs)
      when(cs.getInt(12)).thenReturn(2)

      val repo         = new CisFormpRepository(db)
      val updateParams = UpdateContractorSchemeParams(
        schemeId = 123,
        instanceId = "abc-123",
        accountsOfficeReference = "111111111",
        taxOfficeNumber = "0001",
        taxOfficeReference = "AB12345",
        utr = Some("1234567890"),
        name = Some("Updated Contractor"),
        emailAddress = Some("updated@example.com"),
        displayWelcomePage = Some("Y"),
        prePopCount = Some(10),
        prePopSuccessful = Some("Y"),
        version = Some(1)
      )

      val result = repo.updateScheme(updateParams).futureValue
      result mustBe 2

      verify(conn).prepareCall(eqTo("{ call SCHEME_PROCS.Update_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"))
      verify(cs).setInt(1, 123)
      verify(cs).setString(2, "abc-123")
      verify(cs).setString(3, "111111111")
      verify(cs).setString(4, "0001")
      verify(cs).setString(5, "AB12345")
      verify(cs).setString(6, "1234567890")
      verify(cs).setString(7, "Updated Contractor")
      verify(cs).setString(8, "updated@example.com")
      verify(cs).setString(9, "Y")
      verify(cs).setString(11, "Y")
      verify(cs).registerOutParameter(12, OracleTypes.INTEGER)
      verify(cs).execute()
      verify(cs).getInt(12)
      verify(cs).close()
    }

    "call SCHEME_PROCS.Update_Scheme with null optional fields" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(eqTo("{ call SCHEME_PROCS.Update_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(cs)
      when(cs.getInt(12)).thenReturn(3)

      val repo         = new CisFormpRepository(db)
      val updateParams = UpdateContractorSchemeParams(
        schemeId = 456,
        instanceId = "sparse-123",
        accountsOfficeReference = "555555555",
        taxOfficeNumber = "0055",
        taxOfficeReference = "YY55555",
        version = Some(2)
      )

      val result = repo.updateScheme(updateParams).futureValue
      result mustBe 3

      verify(conn).prepareCall(eqTo("{ call SCHEME_PROCS.Update_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"))
      verify(cs).setInt(1, 456)
      verify(cs).setString(2, "sparse-123")
      verify(cs).setString(3, "555555555")
      verify(cs).setString(4, "0055")
      verify(cs).setString(5, "YY55555")
      verify(cs).setString(6, null)
      verify(cs).setString(7, null)
      verify(cs).setString(8, null)
      verify(cs).setString(9, null)
      verify(cs).setString(11, null)
      verify(cs).registerOutParameter(12, OracleTypes.INTEGER)
      verify(cs).execute()
      verify(cs).getInt(12)
      verify(cs).close()
    }
  }

  "createScheme" - {

    "call SCHEME_PROCS.Create_Scheme with correct parameters and return schemeId" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(eqTo("{ call SCHEME_PROCS.Create_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(cs)
      when(cs.getInt(11)).thenReturn(123)

      val repo         = new CisFormpRepository(db)
      val createParams = CreateContractorSchemeParams(
        instanceId = "abc-123",
        accountsOfficeReference = "111111111",
        taxOfficeNumber = "0001",
        taxOfficeReference = "AB12345",
        utr = Some("1234567890"),
        name = Some("Test Contractor"),
        emailAddress = Some("test@example.com"),
        displayWelcomePage = Some("Y"),
        prePopCount = Some(5),
        prePopSuccessful = Some("Y")
      )

      val result = repo.createScheme(createParams).futureValue
      result mustBe 123

      verify(conn).prepareCall(eqTo("{ call SCHEME_PROCS.Create_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"))
      verify(cs).setString(1, "abc-123")
      verify(cs).setString(2, "111111111")
      verify(cs).setString(3, "0001")
      verify(cs).setString(4, "AB12345")
      verify(cs).setString(5, "1234567890")
      verify(cs).setString(6, "Test Contractor")
      verify(cs).setString(7, "test@example.com")
      verify(cs).setString(8, "Y")
      verify(cs).setString(10, "Y")
      verify(cs).registerOutParameter(11, OracleTypes.INTEGER)
      verify(cs).execute()
      verify(cs).getInt(11)
      verify(cs).close()
    }

    "call SCHEME_PROCS.Create_Scheme with null optional fields" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(eqTo("{ call SCHEME_PROCS.Create_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(cs)
      when(cs.getInt(11)).thenReturn(456)

      val repo         = new CisFormpRepository(db)
      val createParams = CreateContractorSchemeParams(
        instanceId = "sparse-123",
        accountsOfficeReference = "555555555",
        taxOfficeNumber = "0055",
        taxOfficeReference = "YY55555"
      )

      val result = repo.createScheme(createParams).futureValue
      result mustBe 456

      verify(conn).prepareCall(eqTo("{ call SCHEME_PROCS.Create_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"))
      verify(cs).setString(1, "sparse-123")
      verify(cs).setString(2, "555555555")
      verify(cs).setString(3, "0055")
      verify(cs).setString(4, "YY55555")
      verify(cs).setString(5, null)
      verify(cs).setString(6, null)
      verify(cs).setString(7, null)
      verify(cs).setString(8, null)
      verify(cs).setString(10, null)
      verify(cs).registerOutParameter(11, OracleTypes.INTEGER)
      verify(cs).execute()
      verify(cs).getInt(11)
      verify(cs).close()
    }

    "call SCHEME_PROCS.Create_Scheme with Some(prePopCount)" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(eqTo("{ call SCHEME_PROCS.Create_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(cs)
      when(cs.getInt(11)).thenReturn(789)

      val repo         = new CisFormpRepository(db)
      val createParams = CreateContractorSchemeParams(
        instanceId = "count-123",
        accountsOfficeReference = "999999999",
        taxOfficeNumber = "0099",
        taxOfficeReference = "ZZ99999",
        prePopCount = Some(100)
      )

      val result = repo.createScheme(createParams).futureValue
      result mustBe 789

      verify(conn).prepareCall(eqTo("{ call SCHEME_PROCS.Create_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"))
      verify(cs).execute()
      verify(cs).getInt(11)
      verify(cs).close()
    }
  }

  "updateSchemeVersion" - {

    "call SCHEME_PROCS.Update_Version_Number with correct parameters and return version" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(eqTo("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }")))
        .thenReturn(cs)
      when(cs.getInt(2)).thenReturn(2)

      val repo   = new CisFormpRepository(db)
      val result = repo.updateSchemeVersion("abc-123", 1).futureValue
      result mustBe 2

      verify(conn).prepareCall(eqTo("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"))
      verify(cs).setString(1, "abc-123")
      verify(cs).setInt(2, 1)
      verify(cs).registerOutParameter(2, Types.INTEGER)
      verify(cs).execute()
      verify(cs).getInt(2)
      verify(cs).close()
    }
  }

  "applyPrepopulation" - {

    "calls update scheme + creates subcontractors + updates version and returns new version" in {
      val db          = mock[Database]
      val conn        = mock[Connection]
      val csUpdate    = mock[CallableStatement]
      val csSub       = mock[CallableStatement]
      val csUpdateVer = mock[CallableStatement]

      when(db.withTransaction(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call SCHEME_PROCS.Update_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(csUpdate)

      when(conn.prepareCall(eqTo("{ call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR(?, ?, ?, ?) }")))
        .thenReturn(csSub)

      when(conn.prepareCall(eqTo("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }")))
        .thenReturn(csUpdateVer)

      when(csUpdateVer.getInt(2)).thenReturn(2)

      val repo = new CisFormpRepository(db)

      val req = ApplyPrepopulationRequest(
        schemeId = 789,
        instanceId = "abc-123",
        accountsOfficeReference = "111111111",
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        utr = Some("9876543210"),
        name = "Test Contractor",
        emailAddress = Some("test@test.com"),
        displayWelcomePage = Some("Y"),
        prePopCount = 5,
        prePopSuccessful = "Y",
        version = 1,
        subcontractorTypes = Seq(SoleTrader, Company)
      )

      val out = repo.applyPrepopulation(req).futureValue
      out mustBe 2

      verify(conn).prepareCall(eqTo("{ call SCHEME_PROCS.Update_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"))
      verify(conn, times(req.subcontractorTypes.size))
        .prepareCall(eqTo("{ call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR(?, ?, ?, ?) }"))
      verify(conn).prepareCall(eqTo("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"))

      verify(csUpdate).execute()
      verify(csSub, times(req.subcontractorTypes.size)).execute()
      verify(csUpdateVer).execute()
    }
  }

  "createMonthlyReturn" - {

    "call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return with correct parameters and execute" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(eqTo("{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }")))
        .thenReturn(cs)

      val repo = new CisFormpRepository(db)

      val req = CreateMonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 2
      )

      repo.createMonthlyReturn(req).futureValue mustBe ()

      verify(conn).prepareCall(eqTo("{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }"))
      verify(cs).setString(1, "abc-123")
      verify(cs).setInt(2, 2025)
      verify(cs).setInt(3, 2)
      verify(cs).setString(4, "N")
      verify(cs).execute()
      verify(cs).close()
    }
  }

  "getUnsubmittedMonthlyReturns" - {

    "calls MONTHLY_RETURN_PROCS_2016.Get_Unsubmitted_Monthly_Returns and returns list of instance IDs" in {
      val db        = mock[Database]
      val conn      = mock[java.sql.Connection]
      val cs        = mock[CallableStatement]
      val rsScheme  = mock[ResultSet]
      val rsMonthly = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_Monthly_Returns(?, ?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsMonthly)

      when(rsScheme.next()).thenReturn(true, false)
      when(rsScheme.getString("instance_id")).thenReturn("abc-123")
      when(rsScheme.getString("aoref")).thenReturn("123pa132456789")
      when(rsScheme.getString("tax_office_number")).thenReturn("123")
      when(rsScheme.getString("tax_office_reference")).thenReturn("AB456")

      when(rsMonthly.next()).thenReturn(true, false)
      when(rsMonthly.getString("tax_year")).thenReturn("2025")
      when(rsMonthly.getString("tax_month")).thenReturn("2")
      when(rsMonthly.getString("nil_return_indicator")).thenReturn("Y")
      when(rsMonthly.getString("status")).thenReturn("PENDING")
      when(rsMonthly.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2025-01-31 12:34:56"))

      val repo   = new CisFormpRepository(db)
      val result = repo.getUnsubmittedMonthlyReturns("abc-123").futureValue

      result.scheme.instanceId mustBe "abc-123"
      result.monthlyReturn must have size 1
      result.monthlyReturn.head.nilReturnIndicator mustBe Some("Y")
      result.monthlyReturn.head.status mustBe Some("PENDING")
    }
  }

  "getSubmittedMonthlyReturns" - {

    "calls MONTHLY_RETURN_PROCS_2016.Get_Unsubmitted_Monthly_Returns and returns list of instance IDs" in {
      val db           = mock[Database]
      val conn         = mock[java.sql.Connection]
      val cs           = mock[CallableStatement]
      val rsScheme     = mock[ResultSet]
      val rsMonthly    = mock[ResultSet]
      val rsSubmission = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.GET_SUBMITTED_RETURNS(?, ?, ?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsMonthly)
      when(cs.getObject(eqTo(4), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)

      when(rsScheme.next()).thenReturn(true, false)
      when(rsScheme.getString("instance_id")).thenReturn("abc-123")
      when(rsScheme.getString("aoref")).thenReturn("123pa132456789")
      when(rsScheme.getString("tax_office_number")).thenReturn("123")
      when(rsScheme.getString("tax_office_reference")).thenReturn("AB456")

      when(rsMonthly.next()).thenReturn(true, false)
      when(rsMonthly.getString("tax_year")).thenReturn("2025")
      when(rsMonthly.getString("tax_month")).thenReturn("2")
      when(rsMonthly.getString("nil_return_indicator")).thenReturn("Y")
      when(rsMonthly.getString("status")).thenReturn("PENDING")
      when(rsMonthly.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2025-01-31 12:34:56"))

      when(rsSubmission.next()).thenReturn(true, false)

      val repo   = new CisFormpRepository(db)
      val result = repo.getSubmittedMonthlyReturns("abc-123").futureValue

      result.scheme.instanceId mustBe "abc-123"
      result.monthlyReturns must have size 1
      result.monthlyReturns.head.nilReturnIndicator mustBe Some("Y")
      result.monthlyReturns.head.status mustBe Some("PENDING")
    }
  }

  "getMonthlyReturnForEdit" - {
    Seq(true -> "Y", false -> "N").foreach { case isAmendment -> amendmentFlag =>
      s"calls SP and returns empty response when all cursors are empty and isAmendment = $isAmendment" in {
        val db   = mock[Database]
        val conn = mock[Connection]
        val cs   = mock[CallableStatement]

        val rsScheme         = mock[ResultSet]
        val rsMonthlyReturn  = mock[ResultSet]
        val rsItems          = mock[ResultSet]
        val rsSubcontractors = mock[ResultSet]
        val rsSubmission     = mock[ResultSet]

        when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
          val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
        }

        when(
          conn.prepareCall(
            eqTo("{ call MONTHLY_RETURN_PROCS_2016.Get_Monthly_Return_For_Edit(?, ?, ?, ?, ?, ?, ?, ?, ?) }")
          )
        )
          .thenReturn(cs)

        when(cs.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
        when(cs.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsMonthlyReturn)
        when(cs.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsItems)
        when(cs.getObject(eqTo(8), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
        when(cs.getObject(eqTo(9), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)

        when(rsScheme.next()).thenReturn(false)
        when(rsMonthlyReturn.next()).thenReturn(false)
        when(rsItems.next()).thenReturn(false)
        when(rsSubcontractors.next()).thenReturn(false)
        when(rsSubmission.next()).thenReturn(false)

        val repo = new CisFormpRepository(db)

        val out = repo.getMonthlyReturnForEdit("abc-123", 2025, 1, isAmendment).futureValue

        out.scheme mustBe empty
        out.monthlyReturn mustBe empty
        out.monthlyReturnItems mustBe empty
        out.subcontractors mustBe empty
        out.submission mustBe empty

        verify(conn).prepareCall(
          eqTo("{ call MONTHLY_RETURN_PROCS_2016.Get_Monthly_Return_For_Edit(?, ?, ?, ?, ?, ?, ?, ?, ?) }")
        )
        verify(cs).setString(1, "abc-123")
        verify(cs).setInt(2, 2025)
        verify(cs).setInt(3, 1)
        verify(cs).setString(4, amendmentFlag)
        verify(cs).registerOutParameter(5, OracleTypes.CURSOR)
        verify(cs).registerOutParameter(6, OracleTypes.CURSOR)
        verify(cs).registerOutParameter(7, OracleTypes.CURSOR)
        verify(cs).registerOutParameter(8, OracleTypes.CURSOR)
        verify(cs).registerOutParameter(9, OracleTypes.CURSOR)
        verify(cs).execute()
      }
    }
  }

  "getMonthlyReturnComplete" - {

    "calls SP and returns empty response when all cursors are empty" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      val rsScheme         = mock[ResultSet]
      val rsMonthlyReturn  = mock[ResultSet]
      val rsItems          = mock[ResultSet]
      val rsSubcontractors = mock[ResultSet]
      val rsSubmission     = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }

      when(
        conn.prepareCall(
          eqTo("{ call MONTHLY_RETURN_PROCS_2016.Get_Monthly_Return_Complete(?, ?, ?, ?, ?, ?, ?, ?, ?) }")
        )
      )
        .thenReturn(cs)

      when(cs.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(cs.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsMonthlyReturn)
      when(cs.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsItems)
      when(cs.getObject(eqTo(8), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(cs.getObject(eqTo(9), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)

      when(rsScheme.next()).thenReturn(false)
      when(rsMonthlyReturn.next()).thenReturn(false)
      when(rsItems.next()).thenReturn(false)
      when(rsSubcontractors.next()).thenReturn(false)
      when(rsSubmission.next()).thenReturn(false)

      val repo = new CisFormpRepository(db)

      val out = repo.getMonthlyReturnComplete("abc-123", 2025, 1, "N").futureValue

      out.scheme mustBe empty
      out.monthlyReturn mustBe empty
      out.monthlyReturnItems mustBe empty
      out.subcontractors mustBe empty
      out.submission mustBe empty

      verify(conn).prepareCall(
        eqTo("{ call MONTHLY_RETURN_PROCS_2016.Get_Monthly_Return_Complete(?, ?, ?, ?, ?, ?, ?, ?, ?) }")
      )
      verify(cs).setString(1, "abc-123")
      verify(cs).setInt(2, 2025)
      verify(cs).setInt(3, 1)
      verify(cs).setString(4, "N")
      verify(cs).registerOutParameter(5, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(6, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(7, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(8, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(9, OracleTypes.CURSOR)
      verify(cs).execute()
    }
  }

  "createAndUpdateSubcontractor" - {

    def stubCommon(
      db: Database,
      conn: Connection,
      csGetScheme: CallableStatement,
      rsScheme: ResultSet,
      csCreate: CallableStatement,
      csVersion: CallableStatement,
      csUpdate: CallableStatement
    ): Unit = {
      when(db.withTransaction(org.mockito.ArgumentMatchers.any[java.sql.Connection => Any]))
        .thenAnswer { inv =>
          val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
        }

      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"))
        .thenReturn(csGetScheme)
      when(csGetScheme.getObject(eqTo(2), eqTo(classOf[ResultSet])))
        .thenReturn(rsScheme)
      when(rsScheme.next()).thenReturn(true, false)
      when(rsScheme.getLong("scheme_id")).thenReturn(123L)
      when(rsScheme.getInt("version")).thenReturn(1)
      when(rsScheme.wasNull()).thenReturn(false)

      when(conn.prepareCall("{ call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR(?, ?, ?, ?) }"))
        .thenReturn(csCreate)
      when(csCreate.getInt(4)).thenReturn(999)

      when(conn.prepareCall("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"))
        .thenReturn(csVersion)
      when(csVersion.getInt(2)).thenReturn(2)

      when(
        conn.prepareCall(
          "{ call SUBCONTRACTOR_PROCS.Update_Subcontractor(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
        )
      ).thenReturn(csUpdate)
    }

    "call underlying procedures with correct parameters for SoleTrader" in {
      val db          = mock[Database]
      val conn        = mock[Connection]
      val csGetScheme = mock[CallableStatement]
      val rsScheme    = mock[ResultSet]
      val csCreate    = mock[CallableStatement]
      val csVersion   = mock[CallableStatement]
      val csUpdate    = mock[CallableStatement]

      stubCommon(db, conn, csGetScheme, rsScheme, csCreate, csVersion, csUpdate)

      val repo = new CisFormpRepository(db)

      val request = CreateAndUpdateSubcontractorDatabaseRecord(
        cisId = "abc-123",
        subcontractorType = SoleTrader,
        utr = Some("1234567890"),
        partnerUtr = None,
        crn = None,
        firstName = Some("John"),
        secondName = Some("Q"),
        surname = Some("Smith"),
        nino = Some("AA123456A"),
        partnershipTradingName = None,
        tradingName = Some("ACME"),
        addressLine1 = Some("1 Main Street"),
        addressLine2 = Some("Flat 2"),
        city = Some("London"),
        county = Some("Greater London"),
        country = Some("United Kingdom"),
        postcode = Some("AA1 1AA"),
        emailAddress = Some("test@test.com"),
        phoneNumber = Some("01234567890"),
        mobilePhoneNumber = Some("07123456789"),
        worksReferenceNumber = Some("34567")
      )

      repo.createAndUpdateSubcontractor(request).futureValue

      verify(csCreate).setString(3, "soletrader")
      verify(csUpdate).setString(7, "John")
      verify(csUpdate).setString(9, "Q")
      verify(csUpdate).setString(10, "Smith")
      verify(csUpdate).setString(8, "AA123456A")
      verify(csUpdate).setString(17, "United Kingdom")
      verify(csUpdate).execute()
    }

    "call underlying procedures with correct parameters for Partnership" in {
      val db          = mock[Database]
      val conn        = mock[Connection]
      val csGetScheme = mock[CallableStatement]
      val rsScheme    = mock[ResultSet]
      val csCreate    = mock[CallableStatement]
      val csVersion   = mock[CallableStatement]
      val csUpdate    = mock[CallableStatement]

      stubCommon(db, conn, csGetScheme, rsScheme, csCreate, csVersion, csUpdate)

      val repo = new CisFormpRepository(db)

      val request = CreateAndUpdateSubcontractorDatabaseRecord(
        cisId = "abc-123",
        subcontractorType = Partnership,
        utr = Some("2234567890"),
        partnerUtr = None,
        crn = None,
        firstName = None,
        secondName = None,
        surname = None,
        nino = None,
        partnershipTradingName = Some("ABC Partnership"),
        tradingName = Some("Nominated Partner"),
        addressLine1 = Some("2 High Street"),
        addressLine2 = Some("Suite 5"),
        city = Some("Leeds"),
        county = Some("West Yorkshire"),
        country = Some("United Kingdom"),
        postcode = Some("LS1 1AA"),
        emailAddress = Some("partnership@test.com"),
        phoneNumber = Some("01131234567"),
        mobilePhoneNumber = Some("07999999999"),
        worksReferenceNumber = Some("PART-001")
      )

      repo.createAndUpdateSubcontractor(request).futureValue

      verify(csCreate).setString(3, "partnership")
      verify(csUpdate).setNull(eqTo(7), anyInt())
      verify(csUpdate).setNull(eqTo(9), anyInt())
      verify(csUpdate).setNull(eqTo(10), anyInt())
      verify(csUpdate).setNull(eqTo(8), anyInt())
      verify(csUpdate).setString(11, "ABC Partnership")
      verify(csUpdate).setString(12, "Nominated Partner")
      verify(csUpdate).setString(15, "Leeds")
      verify(csUpdate).setString(16, "West Yorkshire")
      verify(csUpdate).setString(17, "United Kingdom")
      verify(csUpdate).execute()
    }

    "call underlying procedures with correct parameters for Company" in {
      val db          = mock[Database]
      val conn        = mock[Connection]
      val csGetScheme = mock[CallableStatement]
      val rsScheme    = mock[ResultSet]
      val csCreate    = mock[CallableStatement]
      val csVersion   = mock[CallableStatement]
      val csUpdate    = mock[CallableStatement]

      stubCommon(db, conn, csGetScheme, rsScheme, csCreate, csVersion, csUpdate)

      val repo = new CisFormpRepository(db)

      val request = CreateAndUpdateSubcontractorDatabaseRecord(
        cisId = "abc-123",
        subcontractorType = Company,
        utr = Some("3234567890"),
        partnerUtr = None,
        crn = Some("CRN-999"),
        firstName = None,
        secondName = None,
        surname = None,
        nino = None,
        partnershipTradingName = None,
        tradingName = Some("ABC Limited"),
        addressLine1 = Some("3 Business Park"),
        addressLine2 = Some("Block A"),
        city = Some("Manchester"),
        county = Some("Greater Manchester"),
        country = Some("United Kingdom"),
        postcode = Some("M1 1AA"),
        emailAddress = Some("company@test.com"),
        phoneNumber = Some("01611234567"),
        mobilePhoneNumber = Some("07888888888"),
        worksReferenceNumber = Some("COMP-001")
      )

      repo.createAndUpdateSubcontractor(request).futureValue

      verify(csCreate).setString(3, "company")
      verify(csUpdate).setString(6, "CRN-999")
      verify(csUpdate).setNull(eqTo(7), anyInt())
      verify(csUpdate).setNull(eqTo(8), anyInt())
      verify(csUpdate).setNull(eqTo(9), anyInt())
      verify(csUpdate).setNull(eqTo(10), anyInt())
      verify(csUpdate).setNull(eqTo(11), anyInt())
      verify(csUpdate).setString(12, "ABC Limited")
      verify(csUpdate).setString(15, "Manchester")
      verify(csUpdate).setString(16, "Greater Manchester")
      verify(csUpdate).setString(17, "United Kingdom")
      verify(csUpdate).execute()
    }
  }

  "createMonthlyReturnItem" - {

    "calls MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return_Item with correct parameters and executes" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val call = "{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return_Item(?, ?, ?, ?, ?) }"
      when(conn.prepareCall(eqTo(call))).thenReturn(cs)

      val repo = new CisFormpRepository(db)

      val req = CreateMonthlyReturnItemRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        resourceReference = 98765L
      )

      repo.createMonthlyReturnItem(req).futureValue mustBe ()

      verify(conn).prepareCall(eqTo(call))
      verify(cs).setString(1, "abc-123")
      verify(cs).setInt(2, 2025)
      verify(cs).setInt(3, 1)
      verify(cs).setString(4, "N")
      verify(cs).setLong(5, 98765L)
      verify(cs).execute()
      verify(cs).close()
    }
  }

  "deleteMonthlyReturnItem" - {

    "calls MONTHLY_RETURN_PROCS_2016.Delete_Monthly_Return_Item with correct parameters and executes" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val call = "{ call MONTHLY_RETURN_PROCS_2016.Delete_Monthly_Return_Item(?, ?, ?, ?, ?) }"
      when(conn.prepareCall(eqTo(call))).thenReturn(cs)

      val repo = new CisFormpRepository(db)

      val req = DeleteMonthlyReturnItemRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        resourceReference = 98765L
      )

      repo.deleteMonthlyReturnItem(req).futureValue mustBe ()

      verify(conn).prepareCall(eqTo(call))
      verify(cs).setString(1, "abc-123")
      verify(cs).setInt(2, 2025)
      verify(cs).setInt(3, 1)
      verify(cs).setString(4, "N")
      verify(cs).setLong(5, 98765L)
      verify(cs).execute()
      verify(cs).close()
    }
  }

  "syncMonthlyReturnItems" - {

    "deletes + creates (distinct) in one transaction, validates status, and updates scheme version once when amendment = N" in {

      val db   = mock[Database]
      val conn = mock[Connection]

      val csGetEdit   = mock[CallableStatement]
      val csGetScheme = mock[CallableStatement]
      val csUpdateVer = mock[CallableStatement]

      val csDelete1 = mock[CallableStatement]
      val csDelete2 = mock[CallableStatement]
      val csCreate1 = mock[CallableStatement]
      val csCreate2 = mock[CallableStatement]

      val rsEditScheme     = mock[ResultSet]
      val rsMonthlyReturn  = mock[ResultSet]
      val rsItems          = mock[ResultSet]
      val rsSubcontractors = mock[ResultSet]
      val rsSubmission     = mock[ResultSet]
      val rsSchemeProc     = mock[ResultSet]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val callGetEdit   = "{ call MONTHLY_RETURN_PROCS_2016.Get_Monthly_Return_For_Edit(?, ?, ?, ?, ?, ?, ?, ?, ?) }"
      val callGetScheme = "{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"
      val callDelete    = "{ call MONTHLY_RETURN_PROCS_2016.Delete_Monthly_Return_Item(?, ?, ?, ?, ?) }"
      val callCreate    = "{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return_Item(?, ?, ?, ?, ?) }"
      val callUpdateVer = "{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"

      when(conn.prepareCall(eqTo(callGetEdit))).thenReturn(csGetEdit)
      when(conn.prepareCall(eqTo(callGetScheme))).thenReturn(csGetScheme)
      when(conn.prepareCall(eqTo(callDelete))).thenReturn(csDelete1, csDelete2)
      when(conn.prepareCall(eqTo(callCreate))).thenReturn(csCreate1, csCreate2)
      when(conn.prepareCall(eqTo(callUpdateVer))).thenReturn(csUpdateVer)

      when(csGetEdit.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsEditScheme)
      when(csGetEdit.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsMonthlyReturn)
      when(csGetEdit.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsItems)
      when(csGetEdit.getObject(eqTo(8), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(csGetEdit.getObject(eqTo(9), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)

      when(rsEditScheme.next()).thenReturn(false)

      when(rsMonthlyReturn.next()).thenReturn(true, false)
      when(rsMonthlyReturn.getLong("monthly_return_id")).thenReturn(1L)
      when(rsMonthlyReturn.getInt("tax_year")).thenReturn(2025)
      when(rsMonthlyReturn.getInt("tax_month")).thenReturn(1)
      when(rsMonthlyReturn.getString("status")).thenReturn("STARTED")
      when(rsMonthlyReturn.getLong("superseded_by")).thenReturn(0L)
      when(rsMonthlyReturn.wasNull()).thenReturn(true)

      when(rsItems.next()).thenReturn(false)
      when(rsSubcontractors.next()).thenReturn(false)
      when(rsSubmission.next()).thenReturn(false)

      when(csGetScheme.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsSchemeProc)
      when(rsSchemeProc.next()).thenReturn(true, false)
      when(rsSchemeProc.getInt("version")).thenReturn(5)
      when(rsSchemeProc.wasNull()).thenReturn(false)

      when(csUpdateVer.getInt(2)).thenReturn(6)

      val repo = new CisFormpRepository(db)

      val req = SyncMonthlyReturnItemsRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        createResourceReferences = Seq(10L, 10L, 20L),
        deleteResourceReferences = Seq(1L, 2L, 2L)
      )

      repo.syncMonthlyReturnItems(req).futureValue mustBe ()

      verify(csGetEdit).execute()
      verify(csDelete1).execute()
      verify(csDelete2).execute()
      verify(csCreate1).execute()
      verify(csCreate2).execute()
      verify(csUpdateVer).execute()

      verify(conn, times(2)).prepareCall(eqTo(callDelete))
      verify(conn, times(2)).prepareCall(eqTo(callCreate))
      verify(conn, times(1)).prepareCall(eqTo(callUpdateVer))
    }

    "deletes + creates (distinct) in one transaction, validates status, and updates scheme version once when amendment = Y" in {

      val db   = mock[Database]
      val conn = mock[Connection]

      val csGetEdit   = mock[CallableStatement]
      val csGetScheme = mock[CallableStatement]
      val csUpdateVer = mock[CallableStatement]

      val csDelete1 = mock[CallableStatement]
      val csDelete2 = mock[CallableStatement]
      val csCreate1 = mock[CallableStatement]
      val csCreate2 = mock[CallableStatement]

      val rsEditScheme     = mock[ResultSet]
      val rsMonthlyReturn  = mock[ResultSet]
      val rsItems          = mock[ResultSet]
      val rsSubcontractors = mock[ResultSet]
      val rsSubmission     = mock[ResultSet]
      val rsSchemeProc     = mock[ResultSet]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val callGetEdit   = "{ call MONTHLY_RETURN_PROCS_2016.Get_Monthly_Return_For_Edit(?, ?, ?, ?, ?, ?, ?, ?, ?) }"
      val callGetScheme = "{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"
      val callDelete    = "{ call MONTHLY_RETURN_PROCS_2016.Delete_Monthly_Return_Item(?, ?, ?, ?, ?) }"
      val callCreate    = "{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return_Item(?, ?, ?, ?, ?) }"
      val callUpdateVer = "{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"

      when(conn.prepareCall(eqTo(callGetEdit))).thenReturn(csGetEdit)
      when(conn.prepareCall(eqTo(callGetScheme))).thenReturn(csGetScheme)
      when(conn.prepareCall(eqTo(callDelete))).thenReturn(csDelete1, csDelete2)
      when(conn.prepareCall(eqTo(callCreate))).thenReturn(csCreate1, csCreate2)
      when(conn.prepareCall(eqTo(callUpdateVer))).thenReturn(csUpdateVer)

      when(csGetEdit.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsEditScheme)
      when(csGetEdit.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsMonthlyReturn)
      when(csGetEdit.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsItems)
      when(csGetEdit.getObject(eqTo(8), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(csGetEdit.getObject(eqTo(9), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)

      when(rsEditScheme.next()).thenReturn(false)

      when(rsMonthlyReturn.next()).thenReturn(true, false)
      when(rsMonthlyReturn.getLong("monthly_return_id")).thenReturn(1L)
      when(rsMonthlyReturn.getInt("tax_year")).thenReturn(2025)
      when(rsMonthlyReturn.getInt("tax_month")).thenReturn(1)
      when(rsMonthlyReturn.getString("status")).thenReturn("STARTED")
      when(rsMonthlyReturn.getLong("superseded_by")).thenReturn(0L)
      when(rsMonthlyReturn.wasNull()).thenReturn(true)

      when(rsItems.next()).thenReturn(false)
      when(rsSubcontractors.next()).thenReturn(false)
      when(rsSubmission.next()).thenReturn(false)

      when(csGetScheme.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsSchemeProc)
      when(rsSchemeProc.next()).thenReturn(true, false)
      when(rsSchemeProc.getInt("version")).thenReturn(5)
      when(rsSchemeProc.wasNull()).thenReturn(false)

      when(csUpdateVer.getInt(2)).thenReturn(6)

      val repo = new CisFormpRepository(db)

      val req = SyncMonthlyReturnItemsRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 1,
        amendment = "Y",
        createResourceReferences = Seq(10L, 10L, 20L),
        deleteResourceReferences = Seq(1L, 2L, 2L)
      )

      repo.syncMonthlyReturnItems(req).futureValue mustBe ()

      verify(csGetEdit).execute()
      verify(csDelete1).execute()
      verify(csDelete2).execute()
      verify(csCreate1).execute()
      verify(csCreate2).execute()
      verify(csUpdateVer).execute()

      verify(conn, times(2)).prepareCall(eqTo(callDelete))
      verify(conn, times(2)).prepareCall(eqTo(callCreate))
      verify(conn, times(1)).prepareCall(eqTo(callUpdateVer))
    }

    "fails when monthly return status is not STARTED/VALIDATED" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csGetEdit = mock[CallableStatement]

      val rsScheme         = mock[ResultSet]
      val rsMonthlyReturn  = mock[ResultSet]
      val rsItems          = mock[ResultSet]
      val rsSubcontractors = mock[ResultSet]
      val rsSubmission     = mock[ResultSet]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      val callGetMonthlyReturnForEdit =
        "{ call MONTHLY_RETURN_PROCS_2016.Get_Monthly_Return_For_Edit(?, ?, ?, ?, ?, ?, ?, ?, ?) }"

      when(conn.prepareCall(eqTo(callGetMonthlyReturnForEdit))).thenReturn(csGetEdit)

      when(csGetEdit.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(csGetEdit.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsMonthlyReturn)
      when(csGetEdit.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsItems)
      when(csGetEdit.getObject(eqTo(8), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(csGetEdit.getObject(eqTo(9), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)

      when(rsScheme.next()).thenReturn(false)

      when(rsMonthlyReturn.next()).thenReturn(true, false)
      when(rsMonthlyReturn.getLong("monthly_return_id")).thenReturn(1111L)
      when(rsMonthlyReturn.getInt("tax_year")).thenReturn(2025)
      when(rsMonthlyReturn.getInt("tax_month")).thenReturn(1)
      when(rsMonthlyReturn.getString("status")).thenReturn("SUBMITTED")

      when(rsItems.next()).thenReturn(false)
      when(rsSubcontractors.next()).thenReturn(false)
      when(rsSubmission.next()).thenReturn(false)

      val repo = new CisFormpRepository(db)

      val req = SyncMonthlyReturnItemsRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        createResourceReferences = Seq(5L),
        deleteResourceReferences = Seq(1L)
      )

      val ex = repo.syncMonthlyReturnItems(req).failed.futureValue
      ex.getMessage must include("Cannot sync monthly return items when status is SUBMITTED")
    }
  }

  "getSubcontractorList" - {

    "calls SUBCONTRACTOR_PROCS.Get_Subcontractor_List, parses subcontractors and closes resources" in {
      val db       = mock[Database]
      val conn     = mock[Connection]
      val cs       = mock[CallableStatement]
      val rsScheme = mock[ResultSet]
      val rsSubs   = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      val call = "{ call SUBCONTRACTOR_PROCS.Get_Subcontractor_List(?, ?, ?) }"
      when(conn.prepareCall(eqTo(call))).thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsSubs)

      when(rsSubs.next()).thenReturn(true, false)

      when(rsSubs.getLong("subcontractor_id")).thenReturn(1L)
      when(rsSubs.getInt("subbie_resource_ref")).thenReturn(10)
      when(rsSubs.getString("type")).thenReturn("soletrader")

      when(rsSubs.getString("utr")).thenReturn("1234567890")
      when(rsSubs.getInt("page_visited")).thenReturn(2)
      when(rsSubs.wasNull()).thenReturn(false)
      when(rsSubs.getString("partner_utr")).thenReturn(null)
      when(rsSubs.getString("crn")).thenReturn(null)
      when(rsSubs.getString("firstname")).thenReturn("John")
      when(rsSubs.getString("nino")).thenReturn("AA123456A")
      when(rsSubs.getString("secondname")).thenReturn(null)
      when(rsSubs.getString("surname")).thenReturn("Smith")
      when(rsSubs.getString("partnership_tradingname")).thenReturn(null)
      when(rsSubs.getString("tradingname")).thenReturn("ACME")

      when(rsSubs.getString("address_line_1")).thenReturn("1 Main Street")
      when(rsSubs.getString("address_line_2")).thenReturn(null)
      when(rsSubs.getString("address_line_3")).thenReturn(null)
      when(rsSubs.getString("address_line_4")).thenReturn(null)
      when(rsSubs.getString("country")).thenReturn("United Kingdom")
      when(rsSubs.getString("postcode")).thenReturn("AA1 1AA")
      when(rsSubs.getString("email_address")).thenReturn(null)
      when(rsSubs.getString("phone_number")).thenReturn(null)
      when(rsSubs.getString("mobile_phone_number")).thenReturn(null)
      when(rsSubs.getString("works_reference_number")).thenReturn(null)

      when(rsSubs.getInt("version")).thenReturn(1)
      when(rsSubs.wasNull()).thenReturn(false)
      when(rsSubs.getString("tax_treatment")).thenReturn(null)
      when(rsSubs.getString("updated_tax_treatment")).thenReturn(null)
      when(rsSubs.getString("verification_number")).thenReturn(null)

      when(rsSubs.getTimestamp("create_date")).thenReturn(Timestamp.valueOf("2026-01-10 09:00:00"))
      when(rsSubs.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2026-01-11 10:00:00"))
      when(rsSubs.getString("matched")).thenReturn(null)
      when(rsSubs.getString("verified")).thenReturn(null)
      when(rsSubs.getString("auto_verified")).thenReturn(null)
      when(rsSubs.getTimestamp("verification_date")).thenReturn(null)
      when(rsSubs.getTimestamp("last_monthly_return_date")).thenReturn(null)

      when(rsSubs.getInt("pending_verifications")).thenReturn(0)
      when(rsSubs.wasNull()).thenReturn(false)

      val repo = new CisFormpRepository(db)

      val out = repo.getSubcontractorList("cis-123").futureValue

      out.subcontractors must have size 1
      val s = out.subcontractors.head
      println(out.subcontractors.head)
      s.subcontractorId mustBe 1L
      s.subcontractorType mustBe Some("soletrader")
      s.utr mustBe Some("1234567890")
      s.pageVisited mustBe Some(2)
      s.firstName mustBe Some("John")
      s.nino mustBe Some("AA123456A")
      s.surname mustBe Some("Smith")
      s.tradingName mustBe Some("ACME")
      s.addressLine1 mustBe Some("1 Main Street")
      s.country mustBe Some("United Kingdom")
      s.postcode mustBe Some("AA1 1AA")
      s.version mustBe Some(1)
      s.createDate mustBe Some(LocalDateTime.of(2026, 1, 10, 9, 0, 0))
      s.lastUpdate mustBe Some(LocalDateTime.of(2026, 1, 11, 10, 0, 0))
      s.pendingVerifications mustBe Some(0)

      verify(conn).prepareCall(eqTo(call))
      verify(cs).setString(1, "cis-123")
      verify(cs).registerOutParameter(2, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(3, OracleTypes.CURSOR)
      verify(cs).execute()

      verify(rsScheme).close()
      verify(rsSubs).close()
      verify(cs).close()
    }

    "returns empty list when subcontractor cursor is null" in {
      val db       = mock[Database]
      val conn     = mock[Connection]
      val cs       = mock[CallableStatement]
      val rsScheme = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      val call = "{ call SUBCONTRACTOR_PROCS.Get_Subcontractor_List(?, ?, ?) }"
      when(conn.prepareCall(eqTo(call))).thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(null)

      val repo = new CisFormpRepository(db)

      val out = repo.getSubcontractorList("cis-123").futureValue
      out.subcontractors mustBe empty

      verify(cs).execute()
      verify(rsScheme).close()
      verify(cs).close()
    }
  }

  "getGovTalkStatus" - {

    "must return one record and close resources" in {
      val db        = mock[Database]
      val conn      = mock[Connection]
      val cs        = mock[CallableStatement]
      val rsRecords = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsRecords)

      when(rsRecords.next()).thenReturn(true, false)
      when(rsRecords.getString("user_identifier")).thenReturn("12")
      when(rsRecords.getString("formResultId")).thenReturn("123abc")
      when(rsRecords.getString("correlationid")).thenReturn("abc123qwe")
      when(rsRecords.getString("form_lock")).thenReturn("N")
      when(rsRecords.getTimestamp("create_timestamp")).thenReturn(Timestamp.valueOf("2026-04-05 12:34:56"))
      when(rsRecords.getTimestamp("endstate_timestamp")).thenReturn(null)
      when(rsRecords.getTimestamp("last_mesg_timestamp")).thenReturn(Timestamp.valueOf("2026-04-05 12:34:56"))
      when(rsRecords.getInt("num_polls")).thenReturn(0)
      when(rsRecords.getInt("poll_interval")).thenReturn(0)
      when(rsRecords.getString("protocol_status")).thenReturn("dataRequest")
      when(rsRecords.getString("gatewayurl")).thenReturn("www.test.com")

      val repo   = new CisFormpRepository(db)
      val result = repo.getGovTalkStatus(GetGovTalkStatusRequest("12", "123abc")).futureValue

      result.govtalk_status must have size 1
      val record = result.govtalk_status.head
      record.userIdentifier mustBe "12"
      record.formResultID mustBe "123abc"
      record.correlationID mustBe "abc123qwe"
      record.formLock mustBe "N"
      record.createDate mustBe Some(Timestamp.valueOf("2026-04-05 12:34:56").toLocalDateTime)
      record.endStateDate mustBe None
      record.lastMessageDate mustBe Timestamp.valueOf("2026-04-05 12:34:56").toLocalDateTime
      record.numPolls mustBe 0
      record.pollInterval mustBe 0
      record.protocolStatus mustBe "dataRequest"
      record.gatewayURL mustBe "www.test.com"

      verify(conn).prepareCall("{ call SUBMISSION_ADMIN.SelectGovTalkStatus(?, ?, ?) }")
      verify(cs).execute()
    }

    "must return empty record list when no rows" in {
      val db        = mock[Database]
      val conn      = mock[Connection]
      val cs        = mock[CallableStatement]
      val rsRecords = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(3, classOf[ResultSet])).thenReturn(rsRecords)
      when(rsRecords.next()).thenReturn(false)

      val repo   = new CisFormpRepository(db)
      val result = repo.getGovTalkStatus(GetGovTalkStatusRequest("12", "123abc")).futureValue

      result.govtalk_status mustBe empty

      verify(conn).prepareCall("{ call SUBMISSION_ADMIN.SelectGovTalkStatus(?, ?, ?) }")
      verify(cs).registerOutParameter(3, OracleTypes.CURSOR)
      verify(cs).execute()
    }
  }

  "updateGovTalkStatusCorrelationId" - {

    "calls CallUpdateGetGovTalkStatusCorrelationId SP with correct params and executes" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      val call = "{ call SUBMISSION_ADMIN.UpdateGovTalkStatusCorr(?, ?, ?, ?, ?) }"
      when(conn.prepareCall(eqTo(call))).thenReturn(cs)

      val repo = new CisFormpRepository(db)

      val req = UpdateGovTalkStatusCorrelationIdRequest(
        userIdentifier = "1",
        formResultID = "12890",
        correlationID = "C742D5DEE7EB4D15B4F7EFD50B890525",
        pollInterval = 1,
        gatewayURL = "http://example.com"
      )

      repo.updateGovTalkStatusCorrelationId(req).futureValue mustBe ()

      verify(conn).prepareCall(eqTo(call))
      verify(cs).setString(1, "1")
      verify(cs).setString(2, "12890")
      verify(cs).setString(3, "C742D5DEE7EB4D15B4F7EFD50B890525")
      verify(cs).setInt(4, 1)
      verify(cs).setString(5, "http://example.com")
      verify(cs).execute()
      verify(cs).close()
    }
  }

  "resetGovTalkStatus" - {

    "call SUBMISSION_ADMIN.ResetGovTalkStatusRecord with correct parameters and execute" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      val call =
        "{ call SUBMISSION_ADMIN.ResetGovTalkStatusRecord(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"

      when(conn.prepareCall(eqTo(call))).thenReturn(cs)

      val repo = new CisFormpRepository(db)

      val request = ResetGovTalkStatusRequest(
        userIdentifier = "1",
        formResultID = "12890",
        oldProtocolStatus = "dataRequest",
        gatewayURL = "http://baseurl.com/submission/ChRIS/CISR/Filing/sync/CIS300MR"
      )

      repo.resetGovTalkStatus(request).futureValue

      val tsCaptorCreateDate      = ArgumentCaptor.forClass(classOf[Timestamp])
      val tsCaptorLastMessageDate = ArgumentCaptor.forClass(classOf[Timestamp])

      verify(conn).prepareCall(eqTo(call))

      verify(cs).setString(1, request.userIdentifier)
      verify(cs).setString(2, request.formResultID)
      verify(cs).setString(3, "empty")
      verify(cs).setString(4, "N")
      verify(cs).setTimestamp(eqTo(5), tsCaptorCreateDate.capture())
      verify(cs).setNull(6, Types.TIMESTAMP)
      verify(cs).setTimestamp(eqTo(7), tsCaptorLastMessageDate.capture())
      verify(cs).setInt(8, 0)
      verify(cs).setInt(9, 0)
      verify(cs).setString(10, request.oldProtocolStatus)
      verify(cs).setString(11, "initial")
      verify(cs).setString(12, request.gatewayURL)

      verify(cs).execute()
      verify(cs).close()

      tsCaptorCreateDate.getValue      must not be null
      tsCaptorLastMessageDate.getValue must not be null
    }
  }

  "updateGovTalkStatus" - {

    "call SUBMISSION_ADMIN.UpdateGovtalkStatus with correct parameters and execute" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      val call =
        "{ call SUBMISSION_ADMIN.UpdateGovtalkStatus(?, ?, ?, ?) }"

      when(conn.prepareCall(eqTo(call))).thenReturn(cs)

      val repo = new CisFormpRepository(db)

      val request = UpdateGovTalkStatusRequest(
        userIdentifier = "1",
        formResultID = "12890",
        endStateDate = Some(LocalDateTime.parse("2026-02-13T00:00:00")),
        protocolStatus = "dataRequest"
      )

      repo.updateGovTalkStatus(request).futureValue

      verify(conn).prepareCall(eqTo(call))

      verify(cs).setString(1, request.userIdentifier)
      verify(cs).setString(2, request.formResultID)
      verify(cs).setString(3, request.protocolStatus)
      verify(cs).setOptionalTimestamp(4, request.endStateDate)

      verify(cs).execute()
      verify(cs).close()
    }
  }

  "createGovTalkStatusRecord" - {

    "call SUBMISSION_ADMIN.InsertInitialGovTalkStatus with correct parameters and execute" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      val call =
        "{ call SUBMISSION_ADMIN.InsertInitialGovTalkStatus(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"

      when(conn.prepareCall(eqTo(call))).thenReturn(cs)

      val repo = new CisFormpRepository(db)

      val request = CreateGovTalkStatusRecordRequest(
        userIdentifier = "1",
        formResultID = "12890",
        correlationID = "C742D5DEE7EB4D15B4F7EFD50B890525",
        gatewayURL = "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
      )

      repo.createGovTalkStatusRecord(request).futureValue

      val tsCaptorCreateDate      = ArgumentCaptor.forClass(classOf[Timestamp])
      val tsCaptorLastMessageDate = ArgumentCaptor.forClass(classOf[Timestamp])

      verify(conn).prepareCall(eqTo(call))

      verify(cs).setString(1, request.userIdentifier)
      verify(cs).setString(2, request.formResultID)
      verify(cs).setString(3, request.correlationID)
      verify(cs).setString(4, "N")
      verify(cs).setTimestamp(eqTo(5), tsCaptorCreateDate.capture())
      verify(cs).setNull(6, Types.TIMESTAMP)
      verify(cs).setTimestamp(eqTo(7), tsCaptorLastMessageDate.capture())
      verify(cs).setInt(8, 0)
      verify(cs).setInt(9, 0)
      verify(cs).setString(10, "initial")
      verify(cs).setString(11, request.gatewayURL)

      verify(cs).execute()
      verify(cs).close()

      tsCaptorCreateDate.getValue      must not be null
      tsCaptorLastMessageDate.getValue must not be null
    }
  }

  "updateMonthlyReturnItem" - {

    "call Update_Monthly_Return_Item and Update_Scheme_Version SPs in transaction" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csGetScheme  = mock[CallableStatement]
      val rsScheme     = mock[ResultSet]
      val csUpdateItem = mock[CallableStatement]
      val csUpdateVer  = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      when(conn.prepareCall(eqTo("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }")))
        .thenReturn(csGetScheme)

      when(csGetScheme.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)

      when(rsScheme.next()).thenReturn(true, false)
      when(rsScheme.getInt("version")).thenReturn(69)
      when(rsScheme.wasNull()).thenReturn(false)

      val updateItemCall =
        "{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return_Item(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
      when(conn.prepareCall(eqTo(updateItemCall))).thenReturn(csUpdateItem)

      val updateVersionCall = "{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"
      when(conn.prepareCall(eqTo(updateVersionCall))).thenReturn(csUpdateVer)
      when(csUpdateVer.getInt(2)).thenReturn(70)

      val repo = new CisFormpRepository(db)

      val req = UpdateMonthlyReturnItemRequest(
        instanceId = "1",
        taxYear = 2015,
        taxMonth = 4,
        amendment = "N",
        itemResourceReference = 9L,
        totalPayments = "1000.00",
        costOfMaterials = "100.00",
        totalDeducted = "100.00",
        subcontractorName = "Charles, C",
        verificationNumber = Some("V1000000009")
      )

      repo.updateMonthlyReturnItem(req).futureValue mustBe ()

      verify(csGetScheme).setString(1, "1")
      verify(csGetScheme).execute()

      verify(conn).prepareCall(eqTo(updateItemCall))
      verify(csUpdateItem).setString(1, "1")
      verify(csUpdateItem).setInt(2, 2015)
      verify(csUpdateItem).setInt(3, 4)

      verify(csUpdateItem).setString(4, "N")

      verify(csUpdateItem).setLong(5, 9L)
      verify(csUpdateItem).setString(6, "1000.00")
      verify(csUpdateItem).setString(7, "100.00")
      verify(csUpdateItem).setString(8, "100.00")
      verify(csUpdateItem).setString(9, "Charles, C")
      verify(csUpdateItem).setOptionalString(10, Some("V1000000009"))

      verify(csUpdateItem).setNull(11, Types.INTEGER)
      verify(csUpdateItem).registerOutParameter(11, Types.INTEGER)

      verify(csUpdateItem).execute()

      verify(conn).prepareCall(eqTo(updateVersionCall))
      verify(csUpdateVer).setString(1, "1")
      verify(csUpdateVer).setInt(2, 69)
      verify(csUpdateVer).registerOutParameter(2, Types.INTEGER)
      verify(csUpdateVer).execute()
      verify(csUpdateVer).getInt(2)

      verify(rsScheme).close()
      verify(csGetScheme).close()
      verify(csUpdateItem).close()
      verify(csUpdateVer).close()
    }
  }

  "getNewestVerificationBatch" - {

    "calls SP and returns scheme,subs,verificationBatch,verifications,submissions,monthlyReturn, mrSubmission data" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      val rsScheme            = mock[ResultSet]
      val rsSubcontractors    = mock[ResultSet]
      val rsVerificationBatch = mock[ResultSet]
      val rsVerifications     = mock[ResultSet]
      val rsSubmission        = mock[ResultSet]
      val rsMonthlyReturn     = mock[ResultSet]
      val rsMrSubmission      = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo(CisStoredProcedures.CallGetNewestVerificationBatch)))
        .thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(cs.getObject(eqTo(4), eqTo(classOf[ResultSet]))).thenReturn(rsVerificationBatch)
      when(cs.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsVerifications)
      when(cs.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)
      when(cs.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsMonthlyReturn)
      when(cs.getObject(eqTo(8), eqTo(classOf[ResultSet]))).thenReturn(rsMrSubmission)

      when(rsScheme.next()).thenReturn(true, false)
      when(rsScheme.getInt("scheme_id")).thenReturn(999)
      when(rsScheme.getString("instance_id")).thenReturn("abc-123")
      when(rsScheme.getString("aoref")).thenReturn("123PA00123456")
      when(rsScheme.getString("tax_office_number")).thenReturn("163")
      when(rsScheme.getString("tax_office_reference")).thenReturn("AB0063")
      when(rsScheme.getString("utr")).thenReturn(null)
      when(rsScheme.getString("name")).thenReturn(null)
      when(rsScheme.getString("email_address")).thenReturn(null)
      when(rsScheme.getString("display_welcome_page")).thenReturn(null)
      when(rsScheme.getInt("pre_pop_count")).thenReturn(0)
      when(rsScheme.wasNull()).thenReturn(true)
      when(rsScheme.getString("pre_pop_successful")).thenReturn(null)
      when(rsScheme.getInt("subcontractor_counter")).thenReturn(0)
      when(rsScheme.wasNull()).thenReturn(true)
      when(rsScheme.getInt("verif_batch_counter")).thenReturn(0)
      when(rsScheme.wasNull()).thenReturn(true)
      when(rsScheme.getTimestamp("create_date")).thenReturn(null)
      when(rsScheme.getTimestamp("last_update")).thenReturn(null)
      when(rsScheme.getInt("version")).thenReturn(0)
      when(rsScheme.wasNull()).thenReturn(true)

      when(rsSubcontractors.next()).thenReturn(true, false)
      when(rsSubcontractors.getLong("subcontractor_id")).thenReturn(1L)
      when(rsSubcontractors.getLong("subbie_resource_ref")).thenReturn(10L)
      when(rsSubcontractors.wasNull()).thenReturn(false)
      when(rsSubcontractors.getString("type")).thenReturn("soletrader")
      when(rsSubcontractors.getString("utr")).thenReturn("1111111111")
      when(rsSubcontractors.getInt("page_visited")).thenReturn(2)
      when(rsSubcontractors.wasNull()).thenReturn(false)
      when(rsSubcontractors.getString("partner_utr")).thenReturn(null)
      when(rsSubcontractors.getString("crn")).thenReturn(null)
      when(rsSubcontractors.getString("firstname")).thenReturn("John")
      when(rsSubcontractors.getString("nino")).thenReturn("AA123456A")
      when(rsSubcontractors.getString("secondname")).thenReturn(null)
      when(rsSubcontractors.getString("surname")).thenReturn("Smith")
      when(rsSubcontractors.getString("partnership_tradingname")).thenReturn(null)
      when(rsSubcontractors.getString("tradingname")).thenReturn("ACME")
      when(rsSubcontractors.getString("address_line_1")).thenReturn("1 Main Street")
      when(rsSubcontractors.getString("address_line_2")).thenReturn(null)
      when(rsSubcontractors.getString("address_line_3")).thenReturn(null)
      when(rsSubcontractors.getString("address_line_4")).thenReturn(null)
      when(rsSubcontractors.getString("country")).thenReturn("United Kingdom")
      when(rsSubcontractors.getString("postcode")).thenReturn("AA1 1AA")
      when(rsSubcontractors.getString("email_address")).thenReturn(null)
      when(rsSubcontractors.getString("phone_number")).thenReturn(null)
      when(rsSubcontractors.getString("mobile_phone_number")).thenReturn(null)
      when(rsSubcontractors.getString("works_reference_number")).thenReturn(null)
      when(rsSubcontractors.getInt("version")).thenReturn(1)
      when(rsSubcontractors.wasNull()).thenReturn(false)
      when(rsSubcontractors.getString("tax_treatment")).thenReturn(null)
      when(rsSubcontractors.getString("updated_tax_treatment")).thenReturn(null)
      when(rsSubcontractors.getString("verification_number")).thenReturn(null)
      when(rsSubcontractors.getTimestamp("create_date")).thenReturn(null)
      when(rsSubcontractors.getTimestamp("last_update")).thenReturn(null)
      when(rsSubcontractors.getString("matched")).thenReturn(null)
      when(rsSubcontractors.getString("verified")).thenReturn(null)
      when(rsSubcontractors.getString("auto_verified")).thenReturn(null)
      when(rsSubcontractors.getTimestamp("verification_date")).thenReturn(null)
      when(rsSubcontractors.getTimestamp("last_monthly_return_date")).thenReturn(null)
      when(rsSubcontractors.getInt("pending_verifications")).thenReturn(0)
      when(rsSubcontractors.wasNull()).thenReturn(false)

      when(rsVerificationBatch.next()).thenReturn(true, false)
      when(rsVerificationBatch.getLong("verification_batch_id")).thenReturn(55L)
      when(rsVerificationBatch.getLong("scheme_id")).thenReturn(999L)
      when(rsVerificationBatch.getLong("verifications_counter")).thenReturn(1L)
      when(rsVerificationBatch.wasNull()).thenReturn(false)
      when(rsVerificationBatch.getLong("verif_batch_resource_ref")).thenReturn(101L)
      when(rsVerificationBatch.wasNull()).thenReturn(false)
      when(rsVerificationBatch.getString("proceed_session")).thenReturn("Y")
      when(rsVerificationBatch.getString("confirm_arrangement")).thenReturn("Y")
      when(rsVerificationBatch.getString("confirm_correct")).thenReturn("Y")
      when(rsVerificationBatch.getString("status")).thenReturn("SUBMITTED")
      when(rsVerificationBatch.getString("verification_number")).thenReturn("VB123")
      when(rsVerificationBatch.getTimestamp("create_date")).thenReturn(Timestamp.valueOf("2026-04-01 10:00:00"))
      when(rsVerificationBatch.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2026-04-02 11:00:00"))
      when(rsVerificationBatch.getInt("version")).thenReturn(1)
      when(rsVerificationBatch.wasNull()).thenReturn(false)

      when(rsVerifications.next()).thenReturn(true, false)
      when(rsVerifications.getLong("verification_id")).thenReturn(9001L)
      when(rsVerifications.getString("matched")).thenReturn("Y")
      when(rsVerifications.getString("verification_number")).thenReturn("V0001")
      when(rsVerifications.getString("tax_treatment")).thenReturn("NET")
      when(rsVerifications.getString("action_indicator")).thenReturn("A")
      when(rsVerifications.getLong("verification_batch_id")).thenReturn(55L)
      when(rsVerifications.wasNull()).thenReturn(false)
      when(rsVerifications.getLong("scheme_id")).thenReturn(999L)
      when(rsVerifications.wasNull()).thenReturn(false)
      when(rsVerifications.getLong("subcontractor_id")).thenReturn(1L)
      when(rsVerifications.wasNull()).thenReturn(false)
      when(rsVerifications.getString("subcontractor_name")).thenReturn("ACME")
      when(rsVerifications.getLong("verification_resource_ref")).thenReturn(777L)
      when(rsVerifications.wasNull()).thenReturn(false)
      when(rsVerifications.getString("proceed")).thenReturn("Y")
      when(rsVerifications.getTimestamp("create_date")).thenReturn(Timestamp.valueOf("2026-04-01 10:00:00"))
      when(rsVerifications.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2026-04-02 11:00:00"))
      when(rsVerifications.getInt("version")).thenReturn(1)
      when(rsVerifications.wasNull()).thenReturn(false)

      when(rsSubmission.next()).thenReturn(true, false)
      when(rsSubmission.getLong("submission_id")).thenReturn(500L)
      when(rsSubmission.getString("submission_type")).thenReturn("VERIFICATIONS")
      when(rsSubmission.getLong("active_object_id")).thenReturn(55L)
      when(rsSubmission.wasNull()).thenReturn(false)
      when(rsSubmission.getString("status")).thenReturn("ACCEPTED")
      when(rsSubmission.getString("hmrc_mark_generated")).thenReturn(null)
      when(rsSubmission.getString("hmrc_mark_ggis")).thenReturn(null)
      when(rsSubmission.getString("email_recipient")).thenReturn("ops@test.com")
      when(rsSubmission.getString("accepted_time")).thenReturn(null)
      when(rsSubmission.getTimestamp("create_date")).thenReturn(Timestamp.valueOf("2026-04-03 09:00:00"))
      when(rsSubmission.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2026-04-03 09:30:00"))
      when(rsSubmission.getLong("scheme_id")).thenReturn(999L)
      when(rsSubmission.getString("agent_id")).thenReturn(null)
      when(rsSubmission.wasNull()).thenReturn(true)
      when(rsSubmission.getTimestamp("submission_request_date")).thenReturn(Timestamp.valueOf("2026-04-03 08:00:00"))
      when(rsSubmission.getString("govtalk_error_code")).thenReturn(null)
      when(rsSubmission.getString("govtalk_error_type")).thenReturn(null)
      when(rsSubmission.getString("govtalk_error_message")).thenReturn(null)

      when(rsMonthlyReturn.next()).thenReturn(true, false)
      when(rsMonthlyReturn.getLong("monthly_return_id")).thenReturn(66666L)
      when(rsMonthlyReturn.getInt("tax_year")).thenReturn(2025)
      when(rsMonthlyReturn.getInt("tax_month")).thenReturn(1)
      when(rsMonthlyReturn.getString("nil_return_indicator")).thenReturn(null)
      when(rsMonthlyReturn.getString("dec_emp_status_considered")).thenReturn(null)
      when(rsMonthlyReturn.getString("dec_all_subs_verified")).thenReturn(null)
      when(rsMonthlyReturn.getString("dec_information_correct")).thenReturn(null)
      when(rsMonthlyReturn.getString("dec_no_more_sub_payments")).thenReturn(null)
      when(rsMonthlyReturn.getString("dec_nil_return_no_payments")).thenReturn(null)
      when(rsMonthlyReturn.getString("status")).thenReturn("STARTED")
      when(rsMonthlyReturn.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2026-04-01 12:00:00"))
      when(rsMonthlyReturn.wasNull()).thenReturn(true)

      when(rsMrSubmission.next()).thenReturn(true, false)
      when(rsMrSubmission.getLong("submission_id")).thenReturn(600L)
      when(rsMrSubmission.getString("submission_type")).thenReturn("MONTHLY_RETURN")
      when(rsMrSubmission.getLong("active_object_id")).thenReturn(66666L)
      when(rsMrSubmission.wasNull()).thenReturn(false)
      when(rsMrSubmission.getString("status")).thenReturn("ACCEPTED")
      when(rsMrSubmission.getString("hmrc_mark_generated")).thenReturn(null)
      when(rsMrSubmission.getString("hmrc_mark_ggis")).thenReturn(null)
      when(rsMrSubmission.getString("email_recipient")).thenReturn(null)
      when(rsMrSubmission.getTimestamp("submission_request_date")).thenReturn(Timestamp.valueOf("2026-04-03 08:00:00"))
      when(rsMrSubmission.getString("accepted_time")).thenReturn(null)
      when(rsMrSubmission.getTimestamp("create_date")).thenReturn(Timestamp.valueOf("2026-04-03 09:00:00"))
      when(rsMrSubmission.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2026-04-03 09:30:00"))
      when(rsMrSubmission.getLong("scheme_id")).thenReturn(999L)
      when(rsMrSubmission.getString("agent_id")).thenReturn(null)
      when(rsMrSubmission.getString("govtalk_error_code")).thenReturn(null)
      when(rsMrSubmission.getString("govtalk_error_type")).thenReturn(null)
      when(rsMrSubmission.getString("govtalk_error_message")).thenReturn(null)
      when(rsMrSubmission.wasNull()).thenReturn(true)

      val repo = new CisFormpRepository(db)

      val out = repo.getNewestVerificationBatch("abc-123").futureValue

      out.scheme.isDefined mustBe true
      out.subcontractors must have size 1
      out.verificationBatch.isDefined mustBe true
      out.verifications  must have size 1
      out.submission.isDefined mustBe true
      out.monthlyReturn.isDefined mustBe true
      out.monthlyReturnSubmission.isDefined mustBe true

      out.scheme.head.schemeId mustBe 999
      out.subcontractors.head.subcontractorId mustBe 1L
      out.verificationBatch.head.verificationBatchId mustBe 55L
      out.verifications.head.verificationId mustBe 9001L
      out.submission.head.submissionId mustBe 500L
      out.monthlyReturn.head.monthlyReturnId mustBe 66666L
      out.monthlyReturnSubmission.head.submissionId mustBe 600L

      verify(cs).setString(1, "abc-123")
      verify(cs).registerOutParameter(2, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(3, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(4, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(5, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(6, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(7, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(8, OracleTypes.CURSOR)
      verify(cs).execute()

      verify(rsScheme).close()
      verify(rsSubcontractors).close()
      verify(rsVerificationBatch).close()
      verify(rsVerifications).close()
      verify(rsSubmission).close()
      verify(rsMonthlyReturn).close()
      verify(rsMrSubmission).close()
      verify(cs).close()
    }

    "returns empty lists when all cursor are empty" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      val rsScheme            = mock[ResultSet]
      val rsSubcontractors    = mock[ResultSet]
      val rsVerificationBatch = mock[ResultSet]
      val rsVerifications     = mock[ResultSet]
      val rsSubmission        = mock[ResultSet]
      val rsMonthlyReturn     = mock[ResultSet]
      val rsMrSubmission      = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo(CisStoredProcedures.CallGetNewestVerificationBatch)))
        .thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(cs.getObject(eqTo(4), eqTo(classOf[ResultSet]))).thenReturn(rsVerificationBatch)
      when(cs.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsVerifications)
      when(cs.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)
      when(cs.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsMonthlyReturn)
      when(cs.getObject(eqTo(8), eqTo(classOf[ResultSet]))).thenReturn(rsMrSubmission)

      when(rsScheme.next()).thenReturn(false)
      when(rsSubcontractors.next()).thenReturn(false)
      when(rsVerificationBatch.next()).thenReturn(false)
      when(rsVerifications.next()).thenReturn(false)
      when(rsSubmission.next()).thenReturn(false)
      when(rsMonthlyReturn.next()).thenReturn(false)
      when(rsMrSubmission.next()).thenReturn(false)

      val repo = new CisFormpRepository(db)
      val out  = repo.getNewestVerificationBatch("abc-123").futureValue

      out.scheme mustBe None
      out.subcontractors mustBe empty
      out.verificationBatch mustBe None
      out.verifications mustBe empty
      out.submission mustBe None
      out.monthlyReturn mustBe empty
      out.monthlyReturnSubmission mustBe empty

      verify(cs).execute()
      verify(rsScheme).close()
      verify(rsSubcontractors).close()
      verify(rsVerificationBatch).close()
      verify(rsVerifications).close()
      verify(rsSubmission).close()
      verify(rsMonthlyReturn).close()
      verify(rsMrSubmission).close()
      verify(cs).close()
    }
  }

  "getCurrentVerificationBatch" - {

    "calls SP and returns subs,verificationBatch,verifications data" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      val rsScheme            = mock[ResultSet]
      val rsSubcontractors    = mock[ResultSet]
      val rsVerificationBatch = mock[ResultSet]
      val rsVerifications     = mock[ResultSet]
      val rsSubmission        = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo(CisStoredProcedures.CallGetCurrentVerificationBatch)))
        .thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(cs.getObject(eqTo(4), eqTo(classOf[ResultSet]))).thenReturn(rsVerificationBatch)
      when(cs.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsVerifications)
      when(cs.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)

      when(rsScheme.next()).thenReturn(true, false)
      when(rsScheme.getInt("scheme_id")).thenReturn(999)
      when(rsScheme.getString("instance_id")).thenReturn("abc-123")
      when(rsScheme.getString("aoref")).thenReturn("123PA00123456")
      when(rsScheme.getString("tax_office_number")).thenReturn("163")
      when(rsScheme.getString("tax_office_reference")).thenReturn("AB0063")
      when(rsScheme.getString("utr")).thenReturn(null)
      when(rsScheme.getString("name")).thenReturn(null)
      when(rsScheme.getString("email_address")).thenReturn(null)
      when(rsScheme.getString("display_welcome_page")).thenReturn(null)
      when(rsScheme.getInt("pre_pop_count")).thenReturn(0);
      when(rsScheme.wasNull()).thenReturn(true)
      when(rsScheme.getString("pre_pop_successful")).thenReturn(null)
      when(rsScheme.getInt("subcontractor_counter")).thenReturn(0);
      when(rsScheme.wasNull()).thenReturn(true)
      when(rsScheme.getInt("verif_batch_counter")).thenReturn(0);
      when(rsScheme.wasNull()).thenReturn(true)
      when(rsScheme.getTimestamp("create_date")).thenReturn(null)
      when(rsScheme.getTimestamp("last_update")).thenReturn(null)
      when(rsScheme.getInt("version")).thenReturn(0);
      when(rsScheme.wasNull()).thenReturn(true)

      when(rsSubcontractors.next()).thenReturn(true, false)
      when(rsSubcontractors.getLong("subcontractor_id")).thenReturn(1L)
      when(rsSubcontractors.getLong("subbie_resource_ref")).thenReturn(10L);
      when(rsSubcontractors.wasNull()).thenReturn(false)
      when(rsSubcontractors.getString("type")).thenReturn("soletrader")
      when(rsSubcontractors.getString("utr")).thenReturn("1111111111")
      when(rsSubcontractors.getInt("page_visited")).thenReturn(2);
      when(rsSubcontractors.wasNull()).thenReturn(false)
      when(rsSubcontractors.getString("partner_utr")).thenReturn(null)
      when(rsSubcontractors.getString("crn")).thenReturn(null)
      when(rsSubcontractors.getString("firstname")).thenReturn("John")
      when(rsSubcontractors.getString("nino")).thenReturn("AA123456A")
      when(rsSubcontractors.getString("secondname")).thenReturn(null)
      when(rsSubcontractors.getString("surname")).thenReturn("Smith")
      when(rsSubcontractors.getString("partnership_tradingname")).thenReturn(null)
      when(rsSubcontractors.getString("tradingname")).thenReturn("ACME")
      when(rsSubcontractors.getString("address_line_1")).thenReturn("1 Main Street")
      when(rsSubcontractors.getString("address_line_2")).thenReturn(null)
      when(rsSubcontractors.getString("address_line_3")).thenReturn(null)
      when(rsSubcontractors.getString("address_line_4")).thenReturn(null)
      when(rsSubcontractors.getString("country")).thenReturn("United Kingdom")
      when(rsSubcontractors.getString("postcode")).thenReturn("AA1 1AA")
      when(rsSubcontractors.getString("email_address")).thenReturn(null)
      when(rsSubcontractors.getString("phone_number")).thenReturn(null)
      when(rsSubcontractors.getString("mobile_phone_number")).thenReturn(null)
      when(rsSubcontractors.getString("works_reference_number")).thenReturn(null)
      when(rsSubcontractors.getInt("version")).thenReturn(1);
      when(rsSubcontractors.wasNull()).thenReturn(false)
      when(rsSubcontractors.getString("tax_treatment")).thenReturn(null)
      when(rsSubcontractors.getString("updated_tax_treatment")).thenReturn(null)
      when(rsSubcontractors.getString("verification_number")).thenReturn(null)
      when(rsSubcontractors.getTimestamp("create_date")).thenReturn(null)
      when(rsSubcontractors.getTimestamp("last_update")).thenReturn(null)
      when(rsSubcontractors.getString("matched")).thenReturn(null)
      when(rsSubcontractors.getString("verified")).thenReturn(null)
      when(rsSubcontractors.getString("auto_verified")).thenReturn(null)
      when(rsSubcontractors.getTimestamp("verification_date")).thenReturn(null)
      when(rsSubcontractors.getTimestamp("last_monthly_return_date")).thenReturn(null)
      when(rsSubcontractors.getInt("pending_verifications")).thenReturn(0);
      when(rsSubcontractors.wasNull()).thenReturn(false)

      when(rsVerificationBatch.next()).thenReturn(true, false)
      when(rsVerificationBatch.getLong("verification_batch_id")).thenReturn(55L)
      when(rsVerificationBatch.getLong("scheme_id")).thenReturn(999L)
      when(rsVerificationBatch.getLong("verifications_counter")).thenReturn(1L);
      when(rsVerificationBatch.wasNull()).thenReturn(false)
      when(rsVerificationBatch.getLong("verif_batch_resource_ref")).thenReturn(101L);
      when(rsVerificationBatch.wasNull()).thenReturn(false)
      when(rsVerificationBatch.getString("proceed_session")).thenReturn("Y")
      when(rsVerificationBatch.getString("confirm_arrangement")).thenReturn("Y")
      when(rsVerificationBatch.getString("confirm_correct")).thenReturn("Y")
      when(rsVerificationBatch.getString("status")).thenReturn("SUBMITTED")
      when(rsVerificationBatch.getString("verification_number")).thenReturn("VB123")
      when(rsVerificationBatch.getTimestamp("create_date")).thenReturn(Timestamp.valueOf("2026-04-01 10:00:00"))
      when(rsVerificationBatch.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2026-04-02 11:00:00"))
      when(rsVerificationBatch.getInt("version")).thenReturn(1);
      when(rsVerificationBatch.wasNull()).thenReturn(false)

      when(rsVerifications.next()).thenReturn(true, false)
      when(rsVerifications.getLong("verification_id")).thenReturn(9001L)
      when(rsVerifications.getString("matched")).thenReturn("Y")
      when(rsVerifications.getString("verification_number")).thenReturn("V0001")
      when(rsVerifications.getString("tax_treatment")).thenReturn("NET")
      when(rsVerifications.getString("action_indicator")).thenReturn("A")
      when(rsVerifications.getLong("verification_batch_id")).thenReturn(55L);
      when(rsVerifications.wasNull()).thenReturn(false)
      when(rsVerifications.getLong("scheme_id")).thenReturn(999L);
      when(rsVerifications.wasNull()).thenReturn(false)
      when(rsVerifications.getLong("subcontractor_id")).thenReturn(1L);
      when(rsVerifications.wasNull()).thenReturn(false)
      when(rsVerifications.getString("subcontractor_name")).thenReturn("ACME")
      when(rsVerifications.getLong("verification_resource_ref")).thenReturn(777L);
      when(rsVerifications.wasNull()).thenReturn(false)
      when(rsVerifications.getString("proceed")).thenReturn("Y")
      when(rsVerifications.getTimestamp("create_date")).thenReturn(Timestamp.valueOf("2026-04-01 10:00:00"))
      when(rsVerifications.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2026-04-02 11:00:00"))
      when(rsVerifications.getInt("version")).thenReturn(1);
      when(rsVerifications.wasNull()).thenReturn(false)

      when(rsSubmission.next()).thenReturn(true, false)
      when(rsSubmission.getLong("submission_id")).thenReturn(500L)
      when(rsSubmission.getString("submission_type")).thenReturn("VERIFICATIONS")
      when(rsSubmission.getLong("active_object_id")).thenReturn(55L);
      when(rsSubmission.wasNull()).thenReturn(false)
      when(rsSubmission.getString("status")).thenReturn("ACCEPTED")
      when(rsSubmission.getString("hmrc_mark_generated")).thenReturn(null)
      when(rsSubmission.getString("hmrc_mark_ggis")).thenReturn(null)
      when(rsSubmission.getString("email_recipient")).thenReturn("ops@test.com")
      when(rsSubmission.getString("accepted_time")).thenReturn(null)
      when(rsSubmission.getTimestamp("create_date")).thenReturn(Timestamp.valueOf("2026-04-03 09:00:00"))
      when(rsSubmission.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2026-04-03 09:30:00"))
      when(rsSubmission.getLong("scheme_id")).thenReturn(999L)
      when(rsSubmission.getString("agent_id")).thenReturn(null)
      when(rsSubmission.wasNull()).thenReturn(true)
      when(rsSubmission.getTimestamp("submission_request_date")).thenReturn(Timestamp.valueOf("2026-04-03 08:00:00"))
      when(rsSubmission.getString("govtalk_error_code")).thenReturn(null)
      when(rsSubmission.getString("govtalk_error_type")).thenReturn(null)
      when(rsSubmission.getString("govtalk_error_message")).thenReturn(null)

      val repo = new CisFormpRepository(db)

      val out = repo.getCurrentVerificationBatch("abc-123").futureValue

      out.scheme.isDefined mustBe true
      out.subcontractors must have size 1
      out.verificationBatch.isDefined mustBe true
      out.verifications  must have size 1
      out.submission.isDefined mustBe true

      out.scheme.head.schemeId mustBe 999
      out.subcontractors.head.subcontractorId mustBe 1L
      out.verificationBatch.head.verificationBatchId mustBe 55L
      out.verifications.head.verificationId mustBe 9001L
      out.submission.head.submissionId mustBe 500L

      verify(cs).setString(1, "abc-123")
      verify(cs).registerOutParameter(2, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(3, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(4, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(5, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(6, OracleTypes.CURSOR)
      verify(cs).execute()

      verify(rsScheme).close()
      verify(rsSubcontractors).close()
      verify(rsVerificationBatch).close()
      verify(rsVerifications).close()
      verify(rsSubmission).close()
      verify(cs).close()
    }

    "returns empty lists when all cursor are empty" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      val rsScheme            = mock[ResultSet]
      val rsSubcontractors    = mock[ResultSet]
      val rsVerificationBatch = mock[ResultSet]
      val rsVerifications     = mock[ResultSet]
      val rsSubmission        = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo(CisStoredProcedures.CallGetCurrentVerificationBatch)))
        .thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(cs.getObject(eqTo(4), eqTo(classOf[ResultSet]))).thenReturn(rsVerificationBatch)
      when(cs.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsVerifications)
      when(cs.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)

      when(rsScheme.next()).thenReturn(false)
      when(rsSubcontractors.next()).thenReturn(false)
      when(rsVerificationBatch.next()).thenReturn(false)
      when(rsVerifications.next()).thenReturn(false)
      when(rsSubmission.next()).thenReturn(false)

      val repo = new CisFormpRepository(db)
      val out  = repo.getCurrentVerificationBatch("abc-123").futureValue

      out.scheme mustBe None
      out.subcontractors mustBe empty
      out.verificationBatch mustBe None
      out.verifications mustBe empty
      out.submission mustBe None

      verify(cs).execute()
      verify(rsScheme).close()
      verify(rsSubcontractors).close()
      verify(rsVerificationBatch).close()
      verify(rsVerifications).close()
      verify(rsSubmission).close()
      verify(cs).close()
    }
  }

  "deleteUnsubmittedMonthlyReturn" - {

    "call DELETE_MONTHLY_RETURN with correct parameters" in {
      val db          = mock[Database]
      val conn        = mock[Connection]
      val csGetScheme = mock[CallableStatement]
      val rsScheme    = mock[ResultSet]
      val csDelete    = mock[CallableStatement]
      val csVersion   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any])(conn)
      }

      when(conn.prepareCall("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"))
        .thenReturn(csGetScheme)
      when(csGetScheme.getObject(eqTo(2), eqTo(classOf[ResultSet])))
        .thenReturn(rsScheme)
      when(rsScheme.next()).thenReturn(true)
      when(rsScheme.getInt("version")).thenReturn(1)

      val deleteCall = "{ call MONTHLY_RETURN_PROCS_2016.DELETE_MONTHLY_RETURN(?, ?, ?, ?) }"

      when(conn.prepareCall(eqTo(deleteCall))).thenReturn(csDelete)

      val versionCall =
        "{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"

      when(conn.prepareCall(eqTo(versionCall)))
        .thenReturn(csVersion)

      when(csVersion.getInt(2)).thenReturn(2)

      val repo = new CisFormpRepository(db)

      val request = DeleteUnsubmittedMonthlyReturnRequest(
        instanceId = "1",
        taxYear = 2026,
        taxMonth = 4,
        amendment = "Y"
      )

      repo.deleteUnsubmittedMonthlyReturn(request).futureValue

      verify(csDelete).setString(1, request.instanceId)
      verify(csDelete).setInt(2, request.taxYear)
      verify(csDelete).setInt(3, request.taxMonth)
      verify(csDelete).setString(4, request.amendment)
      verify(csDelete).execute()
    }
  }

  "createAmendedMonthlyReturn" - {

    "call amend monthly return with correct parameters and execute" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any])
        f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      val repo = new CisFormpRepository(db)

      val request = CreateAmendedMonthlyReturnRequest(
        instanceId = "1",
        taxYear = 2026,
        taxMonth = 4,
        version = 0
      )

      repo.createAmendedMonthlyReturn(request).futureValue

      verify(conn).prepareCall(anyArg[String])

      verify(cs).setString(1, request.instanceId)
      verify(cs).setInt(2, request.taxYear)
      verify(cs).setInt(3, request.taxMonth)
      verify(cs).setInt(4, request.version)

      verify(cs).execute()
      verify(cs).close()
    }
  }

  "createVerificationBatchAndVerifications" - {

    "creates a verification batch then creates verifications for each distinct subcontractor ref and returns batch ref" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csCreateBatch = mock[CallableStatement]
      val csCreateV1    = mock[CallableStatement]
      val csCreateV2    = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val callCreateBatch = "{ call VERIFICATION_PROCS.Create_Verification_Batch(?, ?) }"
      val callCreateVerif = "{ call VERIFICATION_PROCS.Create_Verification(?, ?, ?, ?) }"

      when(conn.prepareCall(eqTo(callCreateBatch))).thenReturn(csCreateBatch)
      when(conn.prepareCall(eqTo(callCreateVerif))).thenReturn(csCreateV1, csCreateV2)

      when(csCreateBatch.getLong(2)).thenReturn(10L)

      val repo = new CisFormpRepository(db)

      val req = CreateVerificationBatchAndVerificationsRequest(
        instanceId = "abc-123",
        verificationResourceReferences = Seq(111L, 111L, 222L),
        actionIndicator = None
      )

      val out = repo.createVerificationBatchAndVerifications(req).futureValue
      out.verificationBatchResourceReference mustBe 10L

      verify(conn).prepareCall(eqTo(callCreateBatch))
      verify(csCreateBatch).setString(1, "abc-123")
      verify(csCreateBatch).registerOutParameter(2, Types.NUMERIC)
      verify(csCreateBatch).execute()
      verify(csCreateBatch).getLong(2)
      verify(csCreateBatch).close()

      verify(conn, times(2)).prepareCall(eqTo(callCreateVerif))

      verify(csCreateV1).setString(1, "abc-123")
      verify(csCreateV1).setLong(2, 10L)
      verify(csCreateV1).setLong(3, 111L)
      verify(csCreateV1).setString(4, null)
      verify(csCreateV1).execute()
      verify(csCreateV1).close()

      verify(csCreateV2).setString(1, "abc-123")
      verify(csCreateV2).setLong(2, 10L)
      verify(csCreateV2).setLong(3, 222L)
      verify(csCreateV2).setString(4, null)
      verify(csCreateV2).execute()
      verify(csCreateV2).close()
    }

    "propagates failure if Create_Verification_Batch fails" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csCreateBatch = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val callCreateBatch = "{ call VERIFICATION_PROCS.Create_Verification_Batch(?, ?) }"
      when(conn.prepareCall(eqTo(callCreateBatch))).thenReturn(csCreateBatch)

      when(csCreateBatch.execute()).thenThrow(new RuntimeException("boom"))

      val repo = new CisFormpRepository(db)

      val req = CreateVerificationBatchAndVerificationsRequest(
        instanceId = "abc-123",
        verificationResourceReferences = Seq(111L),
        actionIndicator = None
      )

      val ex = repo.createVerificationBatchAndVerifications(req).failed.futureValue
      ex.getMessage must include("boom")

      verify(csCreateBatch).close()
      verify(conn, never).prepareCall(eqTo("{ call VERIFICATION_PROCS.Create_Verification(?, ?, ?, ?) }"))
    }

    "propagates failure if Create_Verification fails" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csCreateBatch = mock[CallableStatement]
      val csCreateV1    = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val callCreateBatch = "{ call VERIFICATION_PROCS.Create_Verification_Batch(?, ?) }"
      val callCreateVerif = "{ call VERIFICATION_PROCS.Create_Verification(?, ?, ?, ?) }"

      when(conn.prepareCall(eqTo(callCreateBatch))).thenReturn(csCreateBatch)
      when(conn.prepareCall(eqTo(callCreateVerif))).thenReturn(csCreateV1)

      when(csCreateBatch.getLong(2)).thenReturn(10L)
      when(csCreateV1.execute()).thenThrow(new RuntimeException("verif boom"))

      val repo = new CisFormpRepository(db)

      val req = CreateVerificationBatchAndVerificationsRequest(
        instanceId = "abc-123",
        verificationResourceReferences = Seq(111L),
        actionIndicator = None
      )

      val ex = repo.createVerificationBatchAndVerifications(req).failed.futureValue
      ex.getMessage must include("verif boom")

      verify(csCreateBatch).close()
      verify(csCreateV1).close()
    }
  }

  "getSubmittedMonthlyReturnsData" - {

    "calls MONTHLY_RETURN_PROCS_2016.GET_SUB_MONTHLY_RETURN_DATA and returns list of instance IDs" in {
      val db   = mock[Database]
      val conn = mock[java.sql.Connection]
      val cs   = mock[CallableStatement]

      val rsMonthlyReturn = mock[ResultSet]
      val rsItems         = mock[ResultSet]
      val rsScheme        = mock[ResultSet]
      val rsSubmission    = mock[ResultSet]

      val request = GetSubmittedMonthlyReturnsDataRequest("abc-123", 2025, 2, "Y")

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.GET_SUB_MONTHLY_RETURN_DATA(?, ?, ?, ?, ?, ?, ? ,?) }"))
        .thenReturn(cs)
      when(cs.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsMonthlyReturn)
      when(cs.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsItems)
      when(cs.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(cs.getObject(eqTo(8), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)

      when(rsScheme.next()).thenReturn(true, false)
      when(rsScheme.getString("instance_id")).thenReturn("abc-123")
      when(rsScheme.getString("aoref")).thenReturn("123pa132456789")
      when(rsScheme.getString("tax_office_number")).thenReturn("123")
      when(rsScheme.getString("tax_office_reference")).thenReturn("AB456")

      when(rsMonthlyReturn.next()).thenReturn(true, false)
      when(rsMonthlyReturn.getString("tax_year")).thenReturn("2025")
      when(rsMonthlyReturn.getString("tax_month")).thenReturn("2")
      when(rsMonthlyReturn.getString("nil_return_indicator")).thenReturn("Y")
      when(rsMonthlyReturn.getString("status")).thenReturn("PENDING")
      when(rsMonthlyReturn.getTimestamp("last_update")).thenReturn(Timestamp.valueOf("2025-01-31 12:34:56"))

      val repo   = new CisFormpRepository(db)
      val result = repo.getSubmittedMonthlyReturnsData(request).futureValue

      result.scheme.instanceId mustBe "abc-123"
      result.monthlyReturn must have size 1
      result.monthlyReturn.head.nilReturnIndicator mustBe Some("Y")
      result.monthlyReturn.head.status mustBe Some("PENDING")
    }
  }

  "modifyVerifications" - {

    "creates / delete verifications for each distinct subcontractor ref and returns Unit" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csDeleteV1 = mock[CallableStatement]
      val csDeleteV2 = mock[CallableStatement]
      val csCreateV1 = mock[CallableStatement]
      val csCreateV2 = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val callDeleteVerification = "{ call VERIFICATION_PROCS.DELETE_VERIFICATION_2(?, ?) }"
      val callCreateVerif        = "{ call VERIFICATION_PROCS.Create_Verification(?, ?, ?, ?) }"

      when(conn.prepareCall(eqTo(callDeleteVerification))).thenReturn(csDeleteV1, csDeleteV2)
      when(conn.prepareCall(eqTo(callCreateVerif))).thenReturn(csCreateV1, csCreateV2)

      val repo = new CisFormpRepository(db)

      val req = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L, 111L, 222L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L, 333L, 444L),
            actionIndicator = None
          )
        )
      )

      val result: Unit = repo.modifyVerifications(req).futureValue
      result mustBe ()

      verify(conn, times(2)).prepareCall(eqTo(callDeleteVerification))

      verify(csDeleteV1).setString(1, "abc-123")
      verify(csDeleteV1).setLong(2, 111L)
      verify(csDeleteV1).execute()
      verify(csDeleteV1).close()

      verify(csDeleteV2).setString(1, "abc-123")
      verify(csDeleteV2).setLong(2, 222L)
      verify(csDeleteV2).execute()
      verify(csDeleteV2).close()

      verify(conn, times(2)).prepareCall(eqTo(callCreateVerif))

      verify(csCreateV1).setString(1, "abc-123")
      verify(csCreateV1).setLong(2, 10L)
      verify(csCreateV1).setLong(3, 333L)
      verify(csCreateV1).setString(4, null)
      verify(csCreateV1).execute()
      verify(csCreateV1).close()

      verify(csCreateV2).setString(1, "abc-123")
      verify(csCreateV2).setLong(2, 10L)
      verify(csCreateV2).setLong(3, 444L)
      verify(csCreateV2).setString(4, null)
      verify(csCreateV2).execute()
      verify(csCreateV2).close()
    }

    "delete verifications only for each distinct subcontractor ref and returns Unit" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csDeleteV1 = mock[CallableStatement]
      val csDeleteV2 = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val callDeleteVerification = "{ call VERIFICATION_PROCS.DELETE_VERIFICATION_2(?, ?) }"
      val callCreateVerif        = "{ call VERIFICATION_PROCS.Create_Verification(?, ?, ?, ?) }"

      when(conn.prepareCall(eqTo(callDeleteVerification))).thenReturn(csDeleteV1, csDeleteV2)

      val repo = new CisFormpRepository(db)

      val req = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L, 111L, 222L)
          )
        ),
        createVerifications = None
      )

      val result: Unit = repo.modifyVerifications(req).futureValue
      result mustBe ()

      verify(conn, times(2)).prepareCall(eqTo(callDeleteVerification))
      verify(conn, never()).prepareCall(eqTo(callCreateVerif))

      verify(csDeleteV1).setString(1, "abc-123")
      verify(csDeleteV1).setLong(2, 111L)
      verify(csDeleteV1).execute()
      verify(csDeleteV1).close()

      verify(csDeleteV2).setString(1, "abc-123")
      verify(csDeleteV2).setLong(2, 222L)
      verify(csDeleteV2).execute()
      verify(csDeleteV2).close()
    }

    "creates verifications only for each distinct subcontractor ref and returns Unit" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csCreateV1 = mock[CallableStatement]
      val csCreateV2 = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val callDeleteVerification = "{ call VERIFICATION_PROCS.DELETE_VERIFICATION_2(?, ?) }"
      val callCreateVerif        = "{ call VERIFICATION_PROCS.Create_Verification(?, ?, ?, ?) }"

      when(conn.prepareCall(eqTo(callCreateVerif))).thenReturn(csCreateV1, csCreateV2)

      val repo = new CisFormpRepository(db)

      val req = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = None,
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L, 333L, 444L),
            actionIndicator = None
          )
        )
      )

      val result: Unit = repo.modifyVerifications(req).futureValue
      result mustBe ()

      verify(conn, never()).prepareCall(eqTo(callDeleteVerification))
      verify(conn, times(2)).prepareCall(eqTo(callCreateVerif))

      verify(csCreateV1).setString(1, "abc-123")
      verify(csCreateV1).setLong(2, 10L)
      verify(csCreateV1).setLong(3, 333L)
      verify(csCreateV1).setString(4, null)
      verify(csCreateV1).execute()
      verify(csCreateV1).close()

      verify(csCreateV2).setString(1, "abc-123")
      verify(csCreateV2).setLong(2, 10L)
      verify(csCreateV2).setLong(3, 444L)
      verify(csCreateV2).setString(4, null)
      verify(csCreateV2).execute()
      verify(csCreateV2).close()
    }

    "propagates failure if Delete_Verification fails" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csDeleteV1 = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val callDeleteVerification = "{ call VERIFICATION_PROCS.DELETE_VERIFICATION_2(?, ?) }"

      when(conn.prepareCall(eqTo(callDeleteVerification))).thenReturn(csDeleteV1)

      when(csDeleteV1.execute()).thenThrow(new RuntimeException("verification boom"))

      val repo = new CisFormpRepository(db)

      val req = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L),
            actionIndicator = None
          )
        )
      )

      val ex = repo.modifyVerifications(req).failed.futureValue
      ex.getMessage must include("verification boom")

      verify(csDeleteV1).close()
    }

    "propagates failure if Create_Verification fails" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csDeleteV1 = mock[CallableStatement]
      val csCreateV1 = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val callDeleteVerification = "{ call VERIFICATION_PROCS.DELETE_VERIFICATION_2(?, ?) }"
      val callCreateVerification = "{ call VERIFICATION_PROCS.Create_Verification(?, ?, ?, ?) }"

      when(conn.prepareCall(eqTo(callDeleteVerification))).thenReturn(csDeleteV1)
      when(conn.prepareCall(eqTo(callCreateVerification))).thenReturn(csCreateV1)

      when(csCreateV1.execute()).thenThrow(new RuntimeException("verification boom"))

      val repo = new CisFormpRepository(db)

      val req = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L),
            actionIndicator = None
          )
        )
      )

      val ex = repo.modifyVerifications(req).failed.futureValue
      ex.getMessage must include("verification boom")

      verify(csDeleteV1).close()
      verify(csCreateV1).close()
    }
  }

  "createSubmissionForVerification" - {

    "creates submission, updates verification batch, updates each verification (all in one transaction)" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      val csGetScheme = mock[CallableStatement]
      val rsScheme    = mock[ResultSet]

      when(conn.prepareCall(eqTo(CisStoredProcedures.CallGetScheme))).thenReturn(csGetScheme)
      when(csGetScheme.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)

      when(rsScheme.next()).thenReturn(true, false)
      when(rsScheme.getLong("scheme_id")).thenReturn(123L)

      when(rsScheme.getInt("version")).thenReturn(1)
      when(rsScheme.wasNull()).thenReturn(false)
      when(rsScheme.getString("email_address")).thenReturn(null)

      val csCreateSubmission = mock[CallableStatement]
      when(conn.prepareCall(eqTo(CisStoredProcedures.CallCreateSubmission))).thenReturn(csCreateSubmission)
      when(csCreateSubmission.getLong(9)).thenReturn(555L)

      val csUpdateBatch = mock[CallableStatement]
      when(conn.prepareCall(eqTo(CisStoredProcedures.CallUpdateVerificationBatch))).thenReturn(csUpdateBatch)

      val csUpdateV1 = mock[CallableStatement]
      val csUpdateV2 = mock[CallableStatement]
      when(conn.prepareCall(eqTo(CisStoredProcedures.CallUpdateVerification))).thenReturn(csUpdateV1, csUpdateV2)

      val repo = new CisFormpRepository(db)

      val req = CreateSubmissionAndUpdateVerificationsRequest(
        instanceId = "abc-123",
        verificationBatchId = 999L,
        verificationBatchResourceRef = 77L,
        emailRecipient = "ops@example.com",
        irMarkGenerated = Some("IR_MARK"),
        verifications = Seq(
          VerificationToUpdate(
            subcontractorName = "ACME LTD",
            verificationResourceRef = 10L,
            proceedVerification = "Y"
          ),
          VerificationToUpdate(
            subcontractorName = "BETA LTD",
            verificationResourceRef = 20L,
            proceedVerification = "N"
          )
        ),
        agentId = None
      )

      val out = repo.createSubmissionAndUpdateVerifications(req).futureValue
      out.submissionId mustBe 555L

      verify(conn).prepareCall(eqTo(CisStoredProcedures.CallCreateSubmission))
      verify(csCreateSubmission).setString(1, "abc-123")
      verify(csCreateSubmission).setString(2, "VERIFICATIONS")
      verify(csCreateSubmission).setLong(3, 999L)
      verify(csCreateSubmission).setString(4, "IR_MARK")
      verify(csCreateSubmission).setString(5, null)
      verify(csCreateSubmission).setString(6, "ops@example.com")
      verify(csCreateSubmission).setString(7, null)
      verify(csCreateSubmission).setString(8, "STARTED")
      verify(csCreateSubmission).registerOutParameter(9, Types.NUMERIC)
      verify(csCreateSubmission).execute()

      verify(conn).prepareCall(eqTo(CisStoredProcedures.CallUpdateVerificationBatch))
      verify(csUpdateBatch).setLong(1, 77L)
      verify(csUpdateBatch).setLong(2, 123L)

      verify(csUpdateBatch).setNull(3, Types.VARCHAR)
      verify(csUpdateBatch).setString(4, "Y")
      verify(csUpdateBatch).setString(5, "Y")
      verify(csUpdateBatch).setString(6, "STARTED")
      verify(csUpdateBatch).setNull(7, Types.VARCHAR)

      verify(csUpdateBatch).setNull(8, Types.INTEGER)
      verify(csUpdateBatch).registerOutParameter(8, Types.INTEGER)
      verify(csUpdateBatch).execute()

      verify(conn, times(2)).prepareCall(eqTo(CisStoredProcedures.CallUpdateVerification))

      verify(csUpdateV1).setString(1, "abc-123")
      verify(csUpdateV1).setLong(2, 77L)
      verify(csUpdateV1).setLong(3, 10L)

      verify(csUpdateV1).setNull(4, Types.CHAR) // matched
      verify(csUpdateV1).setNull(5, Types.VARCHAR) // verification_number
      verify(csUpdateV1).setNull(6, Types.VARCHAR) // tax_treatment

      verify(csUpdateV1).setString(7, "VERIFY") // action_indicator
      verify(csUpdateV1).setString(8, "Y") // proceed
      verify(csUpdateV1).setString(9, "ACME LTD") // subcontractor_name

      verify(csUpdateV1).setNull(10, Types.INTEGER)
      verify(csUpdateV1).registerOutParameter(10, Types.INTEGER)
      verify(csUpdateV1).execute()

      verify(csUpdateV2).setString(1, "abc-123")
      verify(csUpdateV2).setLong(2, 77L)
      verify(csUpdateV2).setLong(3, 20L)

      verify(csUpdateV2).setNull(4, Types.CHAR)
      verify(csUpdateV2).setNull(5, Types.VARCHAR)
      verify(csUpdateV2).setNull(6, Types.VARCHAR)

      verify(csUpdateV2).setString(7, "MATCH")
      verify(csUpdateV2).setString(8, "N")
      verify(csUpdateV2).setString(9, "BETA LTD")

      verify(csUpdateV2).setNull(10, Types.INTEGER)
      verify(csUpdateV2).registerOutParameter(10, Types.INTEGER)
      verify(csUpdateV2).execute()

      verify(conn).prepareCall(eqTo(CisStoredProcedures.CallGetScheme))
      verify(csGetScheme).execute()

      verify(rsScheme).close()
      verify(csGetScheme).close()
      verify(csCreateSubmission).close()
      verify(csUpdateBatch).close()
      verify(csUpdateV1).close()
      verify(csUpdateV2).close()
    }
  }

  "getBatchPollSubmissions" - {
    "register both cursors, execute the proc, and assemble the response" in {
      val db                    = mock[Database]
      val mockConnection        = mock[Connection]
      val mockCallableStatement = mock[CallableStatement]
      val verificationRs        = mock[ResultSet]
      val monthlyReturnRs       = mock[ResultSet]

      when(db.withConnection(any[Connection => Any]())).thenAnswer { (inv: InvocationOnMock) =>
        val block = inv.getArgument[Connection => Any](0)
        block(mockConnection)
      }

      when(mockConnection.prepareCall(CisStoredProcedures.CallGetBatchPollSubmissions))
        .thenReturn(mockCallableStatement)

      when(mockCallableStatement.getObject(1, classOf[ResultSet])).thenReturn(verificationRs)
      when(mockCallableStatement.getObject(2, classOf[ResultSet])).thenReturn(monthlyReturnRs)

      when(verificationRs.next()).thenReturn(true, false)
      when(verificationRs.getLong("submission_id")).thenReturn(1L)
      when(verificationRs.getString("submission_type")).thenReturn("VERIFICATION")
      when(verificationRs.getString("agent_id")).thenReturn(null)
      when(verificationRs.getString("tax_office_number")).thenReturn("123")
      when(verificationRs.getString("tax_office_reference")).thenReturn("AB456")
      when(verificationRs.getString("instance_id")).thenReturn("INST1")
      when(verificationRs.getString("status")).thenReturn("PENDING")
      when(verificationRs.getLong("verif_batch_resource_ref")).thenReturn(99L)

      when(monthlyReturnRs.next()).thenReturn(true, false)
      when(monthlyReturnRs.getLong("submission_id")).thenReturn(2L)
      when(monthlyReturnRs.getString("submission_type")).thenReturn("MONTHLY_RETURN")
      when(monthlyReturnRs.getString("status")).thenReturn("PENDING")
      when(monthlyReturnRs.getString("tax_office_number")).thenReturn("123")
      when(monthlyReturnRs.getString("tax_office_reference")).thenReturn("AB456")
      when(monthlyReturnRs.getInt("tax_year")).thenReturn(2025)
      when(monthlyReturnRs.getInt("tax_month")).thenReturn(3)
      when(monthlyReturnRs.getString("instance_id")).thenReturn("INST2")
      when(monthlyReturnRs.getString("agent_id")).thenReturn("AGENT1")

      val repo   = new CisFormpRepository(db)
      val result = repo.getBatchPollSubmissions().futureValue

      verify(mockCallableStatement).registerOutParameter(1, OracleTypes.CURSOR)
      verify(mockCallableStatement).registerOutParameter(2, OracleTypes.CURSOR)
      verify(mockCallableStatement).execute()

      result.verificationSubmissions.size mustBe 1
      result.verificationSubmissions.head.submissionId mustBe 1L
      result.monthlyReturnSubmissions.size mustBe 1
      result.monthlyReturnSubmissions.head.submissionId mustBe 2L
    }
  }
  "updateVerificationSubmission" - {

    "fetches existing submission then updates with merged values" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      // --- Get_Submission ---
      val csGetSubmission = mock[CallableStatement]
      val rsSubmission    = mock[ResultSet]

      when(conn.prepareCall(eqTo(CisStoredProcedures.CallGetSubmission))).thenReturn(csGetSubmission)
      when(csGetSubmission.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)

      when(rsSubmission.next()).thenReturn(true, false)
      when(rsSubmission.getLong("submission_id")).thenReturn(42L)
      when(rsSubmission.getString("submission_type")).thenReturn("VERIFICATIONS")
      when(rsSubmission.getLong("active_object_id")).thenReturn(99L)
      when(rsSubmission.getString("status")).thenReturn("ACCEPTED")
      when(rsSubmission.getString("hmrc_mark_generated")).thenReturn("existing-mark")
      when(rsSubmission.getString("hmrc_mark_ggis")).thenReturn("existing-ggis")
      when(rsSubmission.getString("email_recipient")).thenReturn("test@example.com")
      when(rsSubmission.getString("accepted_time")).thenReturn("2026-01-01T10:00:00")
      when(rsSubmission.getTimestamp("create_date")).thenReturn(null)
      when(rsSubmission.getTimestamp("last_update")).thenReturn(null)
      when(rsSubmission.getLong("scheme_id")).thenReturn(123L)
      when(rsSubmission.getString("agent_id")).thenReturn("AGENT-1")
      when(rsSubmission.getTimestamp("submission_request_date"))
        .thenReturn(java.sql.Timestamp.valueOf("2026-01-01 09:00:00"))
      when(rsSubmission.getString("govtalk_error_code")).thenReturn(null)
      when(rsSubmission.getString("govtalk_error_type")).thenReturn(null)
      when(rsSubmission.getString("govtalk_error_message")).thenReturn(null)
      when(rsSubmission.wasNull()).thenReturn(false)

      // --- Update_Submission ---
      val csUpdateSubmission = mock[CallableStatement]
      when(
        conn.prepareCall(eqTo(CisStoredProcedures.CallUpdateSubmission))
      ).thenReturn(csUpdateSubmission)

      val repo = new CisFormpRepository(db)

      val req = UpdateVerificationSubmissionRequest(
        instanceId = "abc-123",
        verificationBatchResourceRef = 77L,
        hmrcMarkGenerated = Some("request-ir-mark"),
        submissionRequestDate = Some(LocalDateTime.parse("2026-02-02T09:00:00")),
        submittableStatus = "FATAL_ERROR",
        govtalkErrorCode = Some("500"),
        govtalkErrorType = Some("timeOut"),
        govtalkErrorMessage = Some("timeOut")
      )

      repo.updateVerificationSubmission(req).futureValue

      // ---- Verify Get_Submission was called ----
      verify(conn).prepareCall(eqTo(CisStoredProcedures.CallGetSubmission))
      verify(csGetSubmission).setString(1, "abc-123")
      verify(csGetSubmission).setLong(2, 77L)
      verify(csGetSubmission).registerOutParameter(3, OracleTypes.CURSOR)
      verify(csGetSubmission).execute()

      // ---- Verify Update_Submission with merged parameters (matches Oracle SP parameter order) ----
      verify(conn).prepareCall(eqTo(CisStoredProcedures.CallUpdateSubmission))
      verify(csUpdateSubmission).setString(1, "VERIFICATIONS") // p_submission_type
      verify(csUpdateSubmission).setLong(2, 99L) // p_active_object_id
      verify(csUpdateSubmission).setString(3, "request-ir-mark") // p_hmrc_mark_generated (from request)
      verify(csUpdateSubmission).setString(4, "existing-ggis") // p_hmrc_mark_ggis (from existing)
      verify(csUpdateSubmission).setString(5, "test@example.com") // p_email_recipient (from existing)
      verify(csUpdateSubmission)
        .setTimestamp(6, java.sql.Timestamp.valueOf("2026-02-02 09:00:00")) // p_submission_request_date (from request)
      verify(csUpdateSubmission).setString(7, "2026-01-01T10:00:00") // p_accepted_time (from existing)
      verify(csUpdateSubmission).setString(8, "AGENT-1") // p_agent_id (from existing)
      verify(csUpdateSubmission).setString(9, "FATAL_ERROR") // p_submittable_status (from request)
      verify(csUpdateSubmission).setString(10, "500") // p_govtalk_error_code (from request)
      verify(csUpdateSubmission).setString(11, "timeOut") // p_govtalk_error_type (from request)
      verify(csUpdateSubmission).setString(12, "timeOut") // p_govtalk_error_message (from request)
      verify(csUpdateSubmission).setString(13, "abc-123") // p_instance_id
      verify(csUpdateSubmission).setLong(14, 77L) // p_verif_batch_resource_ref
      verify(csUpdateSubmission).execute()

      // ---- Verify no callUpdateVerificationBatch or loadScheme ----
      verify(conn, never()).prepareCall(eqTo(CisStoredProcedures.CallUpdateVerificationBatch))
      verify(conn, never()).prepareCall(eqTo(CisStoredProcedures.CallGetScheme))
    }
  }

  "processVerificationResponseFromChris" - {

    val validChrisResponseRequest =
      ProcessVerificationResponseFromChrisRequest(
        instanceId = "abc-123",
        verificationBatchResourceRef = 222L,
        acceptedTime = "2026-06-15T10:05:00Z",
        submissionStatus = "ACCEPTED",
        irMarkReceived = Some("irmark"),
        verificationResults = Seq(
          VerificationResult(
            resourceRef = 456L,
            matched = Some("Y"),
            verified = Some("Y"),
            verificationNumber = Some("V123456"),
            taxTreatment = "NET",
            verifiedDate = Some(LocalDateTime.parse("2026-06-15T10:05:00"))
          )
        )
      )

    "updates subcontractor, verification batch, verification and submission in one transaction" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csGetExisting        = mock[CallableStatement]
      val rsSubmission         = mock[ResultSet]
      val rsVerificationBatch  = mock[ResultSet]
      val rsVerifications      = mock[ResultSet]
      val rsSubcontractors     = mock[ResultSet]
      val rsScheme             = mock[ResultSet]
      val csUpdateSub          = mock[CallableStatement]
      val csUpdateBatch        = mock[CallableStatement]
      val csUpdateVerification = mock[CallableStatement]
      val csUpdateSubmission   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      when(conn.prepareCall(eqTo(CisStoredProcedures.CallGetSubmissionWithVerificationBatch))).thenReturn(csGetExisting)
      when(conn.prepareCall(eqTo(CisStoredProcedures.CallUpdateSubcontractor))).thenReturn(csUpdateSub)
      when(conn.prepareCall(eqTo(CisStoredProcedures.CallUpdateVerificationBatch))).thenReturn(csUpdateBatch)
      when(conn.prepareCall(eqTo(CisStoredProcedures.CallUpdateVerification))).thenReturn(csUpdateVerification)
      when(conn.prepareCall(eqTo(CisStoredProcedures.CallUpdateSubmission))).thenReturn(csUpdateSubmission)

      when(csGetExisting.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)
      when(csGetExisting.getObject(eqTo(4), eqTo(classOf[ResultSet]))).thenReturn(rsVerificationBatch)
      when(csGetExisting.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsVerifications)
      when(csGetExisting.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(csGetExisting.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)

      when(rsSubmission.next()).thenReturn(true, false)
      when(rsSubmission.getLong("submission_id")).thenReturn(555L)
      when(rsSubmission.getString("submission_type")).thenReturn("VERIFICATIONS")
      when(rsSubmission.getLong("active_object_id")).thenReturn(99L)
      when(rsSubmission.getString("status")).thenReturn("STARTED")
      when(rsSubmission.getString("hmrc_mark_generated")).thenReturn("old-irmark")
      when(rsSubmission.getString("hmrc_mark_ggis")).thenReturn(null)
      when(rsSubmission.getString("email_recipient")).thenReturn("test@test.com")
      when(rsSubmission.getString("accepted_time")).thenReturn(null)
      when(rsSubmission.getLong("scheme_id")).thenReturn(123L)
      when(rsSubmission.getString("agent_id")).thenReturn("agent-123")
      when(rsSubmission.getString("govtalk_error_code")).thenReturn(null)
      when(rsSubmission.getString("govtalk_error_type")).thenReturn(null)
      when(rsSubmission.getString("govtalk_error_message")).thenReturn(null)
      when(rsSubmission.wasNull()).thenReturn(false)

      when(rsVerificationBatch.next()).thenReturn(true, false)
      when(rsVerificationBatch.getLong("verification_batch_id")).thenReturn(99L)
      when(rsVerificationBatch.getLong("scheme_id")).thenReturn(123L)
      when(rsVerificationBatch.getLong("verifications_counter")).thenReturn(1L)
      when(rsVerificationBatch.getLong("verif_batch_resource_ref")).thenReturn(222L)
      when(rsVerificationBatch.getString("proceed_session")).thenReturn("Y")
      when(rsVerificationBatch.getString("confirm_arrangement")).thenReturn("Y")
      when(rsVerificationBatch.getString("confirm_correct")).thenReturn("Y")
      when(rsVerificationBatch.getString("status")).thenReturn("STARTED")
      when(rsVerificationBatch.getString("verification_number")).thenReturn("VB123")
      when(rsVerificationBatch.getInt("version")).thenReturn(1)
      when(rsVerificationBatch.wasNull()).thenReturn(false)

      when(rsVerifications.next()).thenReturn(true, false)
      when(rsVerifications.getLong("verification_id")).thenReturn(1001L)
      when(rsVerifications.getString("matched")).thenReturn(null)
      when(rsVerifications.getString("verification_number")).thenReturn(null)
      when(rsVerifications.getString("tax_treatment")).thenReturn(null)
      when(rsVerifications.getString("action_indicator")).thenReturn("VERIFY")
      when(rsVerifications.getLong("verification_batch_id")).thenReturn(99L)
      when(rsVerifications.getLong("scheme_id")).thenReturn(123L)
      when(rsVerifications.getLong("subcontractor_id")).thenReturn(999L)
      when(rsVerifications.getString("subcontractor_name")).thenReturn("John Smith")
      when(rsVerifications.getLong("verification_resource_ref")).thenReturn(456L)
      when(rsVerifications.getString("proceed")).thenReturn("Y")
      when(rsVerifications.getInt("version")).thenReturn(1)
      when(rsVerifications.wasNull()).thenReturn(false)

      when(rsSubcontractors.next()).thenReturn(true, false)
      when(rsSubcontractors.getLong("subcontractor_id")).thenReturn(999L)
      when(rsSubcontractors.getLong("subbie_resource_ref")).thenReturn(456L)
      when(rsSubcontractors.getString("type")).thenReturn("soletrader")
      when(rsSubcontractors.getString("utr")).thenReturn("1234567890")
      when(rsSubcontractors.getString("firstname")).thenReturn("John")
      when(rsSubcontractors.getString("nino")).thenReturn("AA123456A")
      when(rsSubcontractors.getString("auto_verified")).thenReturn("N")
      when(rsSubcontractors.getString("secondname")).thenReturn("Q")
      when(rsSubcontractors.getString("surname")).thenReturn("Smith")
      when(rsSubcontractors.getString("address_line_1")).thenReturn("1 Test Street")
      when(rsSubcontractors.getString("address_line_2")).thenReturn("Flat 2")
      when(rsSubcontractors.getString("address_line_3")).thenReturn("London")
      when(rsSubcontractors.getString("address_line_4")).thenReturn("Greater London")
      when(rsSubcontractors.getString("updated_tax_treatment")).thenReturn("NET")
      when(rsSubcontractors.getString("country")).thenReturn("United Kingdom")
      when(rsSubcontractors.getString("postcode")).thenReturn("AA1 1AA")
      when(rsSubcontractors.getString("email_address")).thenReturn("test@test.com")
      when(rsSubcontractors.getString("phone_number")).thenReturn("01234567890")
      when(rsSubcontractors.getString("mobile_phone_number")).thenReturn("07123456789")
      when(rsSubcontractors.getString("works_reference_number")).thenReturn("WR-123")
      when(rsSubcontractors.getInt("version")).thenReturn(1)
      when(rsSubcontractors.wasNull()).thenReturn(false)

      when(rsScheme.next()).thenReturn(true, false)
      when(rsScheme.getInt("scheme_id")).thenReturn(123)
      when(rsScheme.getString("instance_id")).thenReturn("abc-123")
      when(rsScheme.getString("aoref")).thenReturn("123PA00123456")
      when(rsScheme.getString("tax_office_number")).thenReturn("123")
      when(rsScheme.getString("tax_office_reference")).thenReturn("AB456")
      when(rsScheme.getString("email_address")).thenReturn(null)
      when(rsScheme.wasNull()).thenReturn(false)

      val repo = new CisFormpRepository(db)

      val request = ProcessVerificationResponseFromChrisRequest(
        instanceId = "abc-123",
        verificationBatchResourceRef = 222L,
        acceptedTime = "2026-06-15T10:05:00Z",
        submissionStatus = "ACCEPTED",
        irMarkReceived = Some("irmark"),
        verificationResults = Seq(
          VerificationResult(
            resourceRef = 456L,
            matched = Some("Y"),
            verified = Some("Y"),
            verificationNumber = Some("V123456"),
            taxTreatment = "NET",
            verifiedDate = Some(LocalDateTime.parse("2026-06-15T10:05:00"))
          )
        )
      )

      repo.processVerificationResponseFromChris(request).futureValue

      verify(csGetExisting).setString(1, "abc-123")
      verify(csGetExisting).setLong(2, 222L)
      verify(csGetExisting).registerOutParameter(3, OracleTypes.CURSOR)
      verify(csGetExisting).registerOutParameter(4, OracleTypes.CURSOR)
      verify(csGetExisting).registerOutParameter(5, OracleTypes.CURSOR)
      verify(csGetExisting).registerOutParameter(6, OracleTypes.CURSOR)
      verify(csGetExisting).registerOutParameter(7, OracleTypes.CURSOR)
      verify(csGetExisting).execute()

      verify(csUpdateSub).setLong(1, 123L)
      verify(csUpdateSub).setLong(2, 456L)
      verify(csUpdateSub).setString(23, "Y")
      verify(csUpdateSub).setString(24, "N")
      verify(csUpdateSub).setString(25, "Y")
      verify(csUpdateSub).setString(26, "V123456")
      verify(csUpdateSub).setString(27, "NET")
      verify(csUpdateSub).setString(28, "NET")
      verify(csUpdateSub).execute()

      verify(csUpdateBatch).setLong(1, 222L)
      verify(csUpdateBatch).setLong(2, 123L)
      verify(csUpdateBatch).setString(4, "Y")
      verify(csUpdateBatch).setString(5, "Y")
      verify(csUpdateBatch).setString(6, "ACCEPTED")
      verify(csUpdateBatch).execute()

      verify(csUpdateVerification).setString(1, "abc-123")
      verify(csUpdateVerification).setLong(2, 222L)
      verify(csUpdateVerification).setLong(3, 456L)
      verify(csUpdateVerification).setString(4, "Y")
      verify(csUpdateVerification).setString(5, "V123456")
      verify(csUpdateVerification).setString(6, "NET")
      verify(csUpdateVerification).setString(7, "VERIFY")
      verify(csUpdateVerification).setString(8, "Y")
      verify(csUpdateVerification).setString(9, "John Smith")
      verify(csUpdateVerification).execute()

      verify(csUpdateSubmission).setString(1, "VERIFICATIONS")
      verify(csUpdateSubmission).setLong(2, 99L)
      verify(csUpdateSubmission).setString(3, "old-irmark")
      verify(csUpdateSubmission).setString(4, "irmark")
      verify(csUpdateSubmission).setString(5, "test@test.com")
      verify(csUpdateSubmission).setString(8, "agent-123")
      verify(csUpdateSubmission).setString(9, "ACCEPTED")
      verify(csUpdateSubmission).setString(13, "abc-123")
      verify(csUpdateSubmission).setLong(14, 222L)
      verify(csUpdateSubmission).execute()
    }

    "throws when scheme is missing" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csGetExisting       = mock[CallableStatement]
      val rsSubmission        = mock[ResultSet]
      val rsVerificationBatch = mock[ResultSet]
      val rsVerifications     = mock[ResultSet]
      val rsSubcontractors    = mock[ResultSet]
      val rsScheme            = mock[ResultSet]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      when(conn.prepareCall(eqTo(CisStoredProcedures.CallGetSubmissionWithVerificationBatch))).thenReturn(csGetExisting)

      when(csGetExisting.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)
      when(csGetExisting.getObject(eqTo(4), eqTo(classOf[ResultSet]))).thenReturn(rsVerificationBatch)
      when(csGetExisting.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsVerifications)
      when(csGetExisting.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(csGetExisting.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)

      when(rsSubmission.next()).thenReturn(true, false)
      when(rsVerificationBatch.next()).thenReturn(true, false)
      when(rsVerifications.next()).thenReturn(false)
      when(rsSubcontractors.next()).thenReturn(false)
      when(rsScheme.next()).thenReturn(false)

      val repo = new CisFormpRepository(db)

      val ex = repo.processVerificationResponseFromChris(validChrisResponseRequest).failed.futureValue

      ex mustBe a[RuntimeException]
      ex.getMessage mustBe "No scheme found for instanceId=abc-123"

      verify(conn, never).prepareCall(eqTo(CisStoredProcedures.CallUpdateSubcontractor))
      verify(conn, never).prepareCall(eqTo(CisStoredProcedures.CallUpdateVerificationBatch))
      verify(conn, never).prepareCall(eqTo(CisStoredProcedures.CallUpdateVerification))
      verify(conn, never).prepareCall(eqTo(CisStoredProcedures.CallUpdateSubmission))
    }

    "throws when verification batch is missing" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csGetExisting       = mock[CallableStatement]
      val rsSubmission        = mock[ResultSet]
      val rsVerificationBatch = mock[ResultSet]
      val rsVerifications     = mock[ResultSet]
      val rsSubcontractors    = mock[ResultSet]
      val rsScheme            = mock[ResultSet]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      when(conn.prepareCall(eqTo(CisStoredProcedures.CallGetSubmissionWithVerificationBatch))).thenReturn(csGetExisting)

      when(csGetExisting.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)
      when(csGetExisting.getObject(eqTo(4), eqTo(classOf[ResultSet]))).thenReturn(rsVerificationBatch)
      when(csGetExisting.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsVerifications)
      when(csGetExisting.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(csGetExisting.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)

      stubSubmissionRow(rsSubmission)
      when(rsVerificationBatch.next()).thenReturn(false)
      when(rsVerifications.next()).thenReturn(false)
      when(rsSubcontractors.next()).thenReturn(false)
      stubSchemeRow(rsScheme)

      val repo = new CisFormpRepository(db)

      val ex = repo.processVerificationResponseFromChris(validChrisResponseRequest).failed.futureValue

      ex mustBe a[RuntimeException]
      ex.getMessage must include("No verification batch found for instanceId=abc-123")
    }

    "throws when verification is missing" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csGetExisting       = mock[CallableStatement]
      val rsSubmission        = mock[ResultSet]
      val rsVerificationBatch = mock[ResultSet]
      val rsVerifications     = mock[ResultSet]
      val rsSubcontractors    = mock[ResultSet]
      val rsScheme            = mock[ResultSet]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      when(conn.prepareCall(eqTo(CisStoredProcedures.CallGetSubmissionWithVerificationBatch))).thenReturn(csGetExisting)

      when(csGetExisting.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)
      when(csGetExisting.getObject(eqTo(4), eqTo(classOf[ResultSet]))).thenReturn(rsVerificationBatch)
      when(csGetExisting.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsVerifications)
      when(csGetExisting.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(csGetExisting.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)

      stubSubmissionRow(rsSubmission)
      stubVerificationBatchRow(rsVerificationBatch)
      when(rsVerifications.next()).thenReturn(false)
      when(rsSubcontractors.next()).thenReturn(false)
      stubSchemeRow(rsScheme)

      val repo = new CisFormpRepository(db)

      val ex = repo.processVerificationResponseFromChris(validChrisResponseRequest).failed.futureValue

      ex mustBe a[RuntimeException]
      ex.getMessage mustBe "No verification found for resourceRef=456"
    }

    "throws when subcontractor is missing" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csGetExisting       = mock[CallableStatement]
      val rsSubmission        = mock[ResultSet]
      val rsVerificationBatch = mock[ResultSet]
      val rsVerifications     = mock[ResultSet]
      val rsSubcontractors    = mock[ResultSet]
      val rsScheme            = mock[ResultSet]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      when(conn.prepareCall(eqTo(CisStoredProcedures.CallGetSubmissionWithVerificationBatch))).thenReturn(csGetExisting)

      when(csGetExisting.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)
      when(csGetExisting.getObject(eqTo(4), eqTo(classOf[ResultSet]))).thenReturn(rsVerificationBatch)
      when(csGetExisting.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsVerifications)
      when(csGetExisting.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(csGetExisting.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)

      stubSubmissionRow(rsSubmission)
      stubVerificationBatchRow(rsVerificationBatch)
      stubVerificationRow(rsVerifications)
      when(rsSubcontractors.next()).thenReturn(false)
      stubSchemeRow(rsScheme)

      val repo = new CisFormpRepository(db)

      val ex = repo.processVerificationResponseFromChris(validChrisResponseRequest).failed.futureValue

      ex mustBe a[RuntimeException]
      ex.getMessage mustBe "No subcontractor found for resourceRef=456"
    }

    def stubSubmissionRow(rs: ResultSet): Unit = {
      when(rs.next()).thenReturn(true, false)
      when(rs.getLong("submission_id")).thenReturn(555L)
      when(rs.getString("submission_type")).thenReturn("VERIFICATIONS")
      when(rs.getLong("active_object_id")).thenReturn(99L)
      when(rs.getString("status")).thenReturn("STARTED")
      when(rs.getString("hmrc_mark_generated")).thenReturn("old-irmark")
      when(rs.getString("hmrc_mark_ggis")).thenReturn(null)
      when(rs.getString("email_recipient")).thenReturn("test@test.com")
      when(rs.getString("accepted_time")).thenReturn(null)
      when(rs.getLong("scheme_id")).thenReturn(123L)
      when(rs.getString("agent_id")).thenReturn("agent-123")
      when(rs.getString("govtalk_error_code")).thenReturn(null)
      when(rs.getString("govtalk_error_type")).thenReturn(null)
      when(rs.getString("govtalk_error_message")).thenReturn(null)
      when(rs.wasNull()).thenReturn(false)
    }

    def stubVerificationBatchRow(rs: ResultSet): Unit = {
      when(rs.next()).thenReturn(true, false)
      when(rs.getLong("verification_batch_id")).thenReturn(99L)
      when(rs.getLong("scheme_id")).thenReturn(123L)
      when(rs.getLong("verifications_counter")).thenReturn(1L)
      when(rs.getLong("verif_batch_resource_ref")).thenReturn(222L)
      when(rs.getString("proceed_session")).thenReturn("Y")
      when(rs.getString("confirm_arrangement")).thenReturn("Y")
      when(rs.getString("confirm_correct")).thenReturn("Y")
      when(rs.getString("status")).thenReturn("STARTED")
      when(rs.getString("verification_number")).thenReturn("VB123")
      when(rs.getInt("version")).thenReturn(1)
      when(rs.wasNull()).thenReturn(false)
    }

    def stubVerificationRow(rs: ResultSet): Unit = {
      when(rs.next()).thenReturn(true, false)
      when(rs.getLong("verification_id")).thenReturn(1001L)
      when(rs.getString("matched")).thenReturn(null)
      when(rs.getString("verification_number")).thenReturn(null)
      when(rs.getString("tax_treatment")).thenReturn(null)
      when(rs.getString("action_indicator")).thenReturn("VERIFY")
      when(rs.getLong("verification_batch_id")).thenReturn(99L)
      when(rs.getLong("scheme_id")).thenReturn(123L)
      when(rs.getLong("subcontractor_id")).thenReturn(999L)
      when(rs.getString("subcontractor_name")).thenReturn("John Smith")
      when(rs.getLong("verification_resource_ref")).thenReturn(456L)
      when(rs.getString("proceed")).thenReturn("Y")
      when(rs.getInt("version")).thenReturn(1)
      when(rs.wasNull()).thenReturn(false)
    }

    def stubSchemeRow(rs: ResultSet): Unit = {
      when(rs.next()).thenReturn(true, false)
      when(rs.getInt("scheme_id")).thenReturn(123)
      when(rs.getString("instance_id")).thenReturn("abc-123")
      when(rs.getString("aoref")).thenReturn("123PA00123456")
      when(rs.getString("tax_office_number")).thenReturn("123")
      when(rs.getString("tax_office_reference")).thenReturn("AB456")
      when(rs.getString("email_address")).thenReturn(null)
      when(rs.wasNull()).thenReturn(false)
    }

    "throws when subcontractor is missing for matching verification" in {
      val db   = mock[Database]
      val conn = mock[Connection]

      val csGetExisting       = mock[CallableStatement]
      val rsSubmission        = mock[ResultSet]
      val rsVerificationBatch = mock[ResultSet]
      val rsVerifications     = mock[ResultSet]
      val rsSubcontractors    = mock[ResultSet]
      val rsScheme            = mock[ResultSet]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        inv.getArgument(0, classOf[Connection => Any]).apply(conn)
      }

      when(conn.prepareCall(eqTo(CisStoredProcedures.CallGetSubmissionWithVerificationBatch))).thenReturn(csGetExisting)

      when(csGetExisting.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsSubmission)
      when(csGetExisting.getObject(eqTo(4), eqTo(classOf[ResultSet]))).thenReturn(rsVerificationBatch)
      when(csGetExisting.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsVerifications)
      when(csGetExisting.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsSubcontractors)
      when(csGetExisting.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)

      stubSubmissionRow(rsSubmission)
      stubVerificationBatchRow(rsVerificationBatch)
      stubVerificationRow(rsVerifications)
      stubSchemeRow(rsScheme)

      when(rsSubcontractors.next()).thenReturn(false)

      val repo = new CisFormpRepository(db)

      val ex = repo.processVerificationResponseFromChris(validChrisResponseRequest).failed.futureValue

      ex mustBe a[RuntimeException]
      ex.getMessage mustBe "No subcontractor found for resourceRef=456"

      verify(conn, never).prepareCall(eqTo(CisStoredProcedures.CallUpdateSubcontractor))
      verify(conn, never).prepareCall(eqTo(CisStoredProcedures.CallUpdateVerification))
      verify(conn, never).prepareCall(eqTo(CisStoredProcedures.CallUpdateVerificationBatch))
      verify(conn, never).prepareCall(eqTo(CisStoredProcedures.CallUpdateSubmission))
    }
  }

  private def mockSingleSubcontractor(rs: ResultSet): Unit = {
    when(rs.next()).thenReturn(true, false)

    when(rs.getLong("subcontractor_id")).thenReturn(1L)
    when(rs.getLong("subbie_resource_ref")).thenReturn(10L)

    when(rs.getString("type")).thenReturn("company")
    when(rs.getString("tradingname")).thenReturn("Gamma Builders")

    when(rs.wasNull()).thenReturn(true)

    Seq(
      "utr",
      "partner_utr",
      "crn",
      "firstname",
      "nino",
      "secondname",
      "surname",
      "partnership_tradingname",
      "address_line_1",
      "address_line_2",
      "address_line_3",
      "address_line_4",
      "country",
      "postcode",
      "email_address",
      "phone_number",
      "mobile_phone_number",
      "works_reference_number",
      "tax_treatment",
      "updated_tax_treatment",
      "verification_number",
      "matched",
      "verified",
      "auto_verified"
    ).foreach { field =>
      when(rs.getString(field)).thenReturn(null)
    }
  }

  private val callGetSubcontractorForDelete =
    "{ call SUBCONTRACTOR_PROCS.Get_Subcontractor_For_Delete(?, ?, ?, ?, ?, ?) }"

  private trait Ctx {

    val db: Database          = mock[Database]
    val conn: Connection      = mock[Connection]
    val cs: CallableStatement = mock[CallableStatement]

    val rs3: ResultSet = mock[ResultSet]
    val rs4: ResultSet = mock[ResultSet]
    val rs5: ResultSet = mock[ResultSet]

    val repo = new CisFormpRepository(db)

    when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
      val f = inv.getArgument(0, classOf[Connection => Any])
      f(conn)
    }

    when(conn.prepareCall(eqTo(callGetSubcontractorForDelete))).thenReturn(cs)

    when(cs.getObject(3, classOf[ResultSet])).thenReturn(rs3)
    when(cs.getObject(4, classOf[ResultSet])).thenReturn(rs4)
    when(cs.getObject(5, classOf[ResultSet])).thenReturn(rs5)

    def verifyStoredProcedureCalled(): Unit = {
      verify(conn).prepareCall(eqTo(callGetSubcontractorForDelete))

      verify(cs).setString(1, "cis-123")
      verify(cs).setLong(2, 10L)

      verify(cs).registerOutParameter(3, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(4, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(5, OracleTypes.CURSOR)
      verify(cs).registerOutParameter(6, Types.VARCHAR)

      verify(cs).execute()
      verify(cs).getObject(3, classOf[ResultSet])
      verify(cs).getObject(4, classOf[ResultSet])
      verify(cs).getObject(5, classOf[ResultSet])
      verify(cs).close()
    }
  }

  "getSubcontractorForDelete" - {

    "calls stored procedure and returns name and true when flag is 'true'" in new Ctx {

      mockSingleSubcontractor(rs4)

      when(cs.getString(6)).thenReturn("true")

      val out =
        repo.getSubcontractorForDelete("cis-123", 10L).futureValue

      out mustBe GetSubcontractorForDeleteResponse(
        subcontractorName = "Gamma Builders",
        subcontractorCanBeDeleted = true
      )

      verifyStoredProcedureCalled()
    }

    "returns name and false when flag is 'false'" in new Ctx {

      mockSingleSubcontractor(rs4)

      when(cs.getString(6)).thenReturn("false")

      val out =
        repo.getSubcontractorForDelete("cis-123", 10L).futureValue

      out mustBe GetSubcontractorForDeleteResponse(
        subcontractorName = "Gamma Builders",
        subcontractorCanBeDeleted = false
      )

      verifyStoredProcedureCalled()
    }

    "maps trimmed 'true' as true" in new Ctx {

      mockSingleSubcontractor(rs4)

      when(cs.getString(6)).thenReturn(" true ")

      val out =
        repo.getSubcontractorForDelete("cis-123", 10L).futureValue

      out.subcontractorName mustBe "Gamma Builders"
      out.subcontractorCanBeDeleted mustBe true

      verifyStoredProcedureCalled()
    }

    "returns false when flag is null" in new Ctx {

      mockSingleSubcontractor(rs4)

      when(cs.getString(6)).thenReturn(null)

      val out =
        repo.getSubcontractorForDelete("cis-123", 10L).futureValue

      out.subcontractorName mustBe "Gamma Builders"
      out.subcontractorCanBeDeleted mustBe false

      verifyStoredProcedureCalled()
    }

    "throws IllegalStateException when no subcontractor is returned" in new Ctx {

      when(rs4.next()).thenReturn(false)
      when(cs.getString(6)).thenReturn("true")

      val exception =
        repo
          .getSubcontractorForDelete("cis-123", 10L)
          .failed
          .futureValue

      exception mustBe an[IllegalStateException]

      exception.getMessage must include(
        "No subcontractor found"
      )
    }

    "throws IllegalStateException when multiple subcontractors are returned" in new Ctx {

      when(rs4.next()).thenReturn(true, true, false)
      when(rs4.getLong("subcontractor_id")).thenReturn(1L, 2L)
      when(rs4.getLong("subbie_resource_ref")).thenReturn(10L, 11L)
      when(rs4.getString("type")).thenReturn("company")
      when(rs4.getString("tradingname")).thenReturn("Gamma Builders")
      when(rs4.wasNull()).thenReturn(true)
      Seq(
        "utr",
        "partner_utr",
        "crn",
        "firstname",
        "nino",
        "secondname",
        "surname",
        "partnership_tradingname",
        "address_line_1",
        "address_line_2",
        "address_line_3",
        "address_line_4",
        "country",
        "postcode",
        "email_address",
        "phone_number",
        "mobile_phone_number",
        "works_reference_number",
        "tax_treatment",
        "updated_tax_treatment",
        "verification_number",
        "matched",
        "verified",
        "auto_verified"
      ).foreach(f => when(rs4.getString(f)).thenReturn(null))
      when(cs.getString(6)).thenReturn("true")

      val exception =
        repo.getSubcontractorForDelete("cis-123", 10L).failed.futureValue

      exception mustBe an[IllegalStateException]
      exception.getMessage must include("Expected exactly one subcontractor")
    }
  }

}
