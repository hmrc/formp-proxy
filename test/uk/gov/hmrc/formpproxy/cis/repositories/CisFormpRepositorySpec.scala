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
import org.mockito.ArgumentMatchers.{any as anyArg, eq as eqTo}
import org.mockito.Mockito.*
import play.api.db.Database
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.{Company, CreateContractorSchemeParams, Partnership, SoleTrader, Trust, UpdateContractorSchemeParams}
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateNilMonthlyReturnRequest, CreateSubmissionRequest, UpdateSubmissionRequest}

import java.time.Instant
import java.sql.*

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
      val rsMonthly = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }
      when(conn.prepareCall(anyArg[String])).thenReturn(cs)
      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(null)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsMonthly)
      when(rsMonthly.next()).thenReturn(false)

      val repo = new CisFormpRepository(db)
      val out  = repo.getAllMonthlyReturns("abc-123").futureValue

      out.monthlyReturnList mustBe empty
      verify(conn).prepareCall(eqTo("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }"))
      verify(cs).execute()
    }
  }

  "createNilMonthlyReturn" - {

    "call underlying procedures with correct parameters and return STARTED" in {
      val db          = mock[Database]
      val conn        = mock[java.sql.Connection]
      val csCreate    = mock[CallableStatement]
      val csVersion   = mock[CallableStatement]
      val csUpdate    = mock[CallableStatement]
      val csGetScheme = mock[CallableStatement]
      val rsScheme    = mock[java.sql.ResultSet]

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
      when(
        conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
      )
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

      verify(conn).prepareCall(
        "{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
      )
      verify(csUpdate).execute()
    }
  }

  "createSubmission" - {

    "look up ids, call SPs, return the submission id, and close resources" in {
      val db        = mock[Database]
      val conn      = mock[Connection]
      val rsMonthly = mock[ResultSet]
      val csAll     = mock[CallableStatement]
      val csCreate  = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }")))
        .thenReturn(csAll)
      when(csAll.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsMonthly)
      when(rsMonthly.next()).thenReturn(true, false)
      when(rsMonthly.getInt("tax_year")).thenReturn(2024)
      when(rsMonthly.getInt("tax_month")).thenReturn(4)
      when(rsMonthly.getLong("monthly_return_id")).thenReturn(7777L)

      when(conn.prepareCall(eqTo("{ call SUBMISSION_PROCS.Create_Submission(?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(csCreate)
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
      val db        = mock[Database]
      val conn      = mock[Connection]
      val rsScheme  = mock[ResultSet]
      val rsMonthly = mock[ResultSet]
      val csScheme  = mock[CallableStatement]
      val csAll     = mock[CallableStatement]
      val csUpdate  = mock[CallableStatement]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"))).thenReturn(csScheme)
      when(csScheme.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsScheme)
      when(rsScheme.next()).thenReturn(true)
      when(rsScheme.getLong("scheme_id")).thenReturn(9999L)

      when(conn.prepareCall(eqTo("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }")))
        .thenReturn(csAll)
      when(csAll.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsMonthly)
      when(rsMonthly.next()).thenReturn(true, false)
      when(rsMonthly.getInt("tax_year")).thenReturn(2024)
      when(rsMonthly.getInt("tax_month")).thenReturn(4)
      when(rsMonthly.getLong("monthly_return_id")).thenReturn(7777L)

      when(
        conn.prepareCall(
          eqTo("{ call SUBMISSION_PROCS_2016.UPDATE_MR_SUBMISSION(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
        )
      )
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
      verify(conn).prepareCall(
        eqTo("{ call SUBMISSION_PROCS_2016.UPDATE_MR_SUBMISSION(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
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

    "throw when Get_All_Monthly_Returns returns null monthly cursor (outer null check)" in {
      val db         = mock[Database]
      val connection = mock[Connection]
      val cs         = mock[CallableStatement]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any])
        f(connection)
      }
      when(connection.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }")).thenReturn(cs)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(null) // ← null monthly cursor

      val repo   = new CisFormpRepository(db)
      val method = classOf[CisFormpRepository].getDeclaredMethod(
        "getMonthlyReturnId",
        classOf[Connection],
        classOf[String],
        classOf[Int],
        classOf[Int]
      )
      method.setAccessible(true)
      val thrown = intercept[java.lang.reflect.InvocationTargetException] {
        method.invoke(repo, connection, "abc-123", Int.box(2025), Int.box(2))
      }
      val cause  = thrown.getCause.asInstanceOf[RuntimeException]
      cause.getMessage must include("null monthly cursor")
    }

    "throw when no MONTHLY_RETURN matches the requested year/month" in {
      val db         = mock[Database]
      val connection = mock[Connection]
      val cs         = mock[CallableStatement]
      val rs         = mock[ResultSet]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any])
        f(connection)
      }
      when(connection.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }")).thenReturn(cs)
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
        classOf[Int]
      )
      method.setAccessible(true)
      val thrown = intercept[java.lang.reflect.InvocationTargetException] {
        method.invoke(repo, connection, "abc-123", Int.box(2025), Int.box(2))
      }
      val cause  = thrown.getCause.asInstanceOf[RuntimeException]
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
      when(rs.getString("last_update")).thenReturn("2025-12-04T10:15:30Z")
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

      val repo   = new CisFormpRepository(db)
      val result = repo.getScheme("null-cursor-123").failed.futureValue

      result mustBe a[RuntimeException]
      result.getMessage must include("int_Get_Scheme returned null cursor")

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

  "createSubcontractor" - {

    "call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR with correct parameters for SoleTrader and return version" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(eqTo("{ call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR(?, ?, ?, ?) }")))
        .thenReturn(cs)
      when(cs.getInt(4)).thenReturn(2)

      val repo   = new CisFormpRepository(db)
      val result = repo.createSubcontractor(123, SoleTrader, 1).futureValue
      result mustBe 2

      verify(conn).prepareCall(eqTo("{ call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR(?, ?, ?, ?) }"))
      verify(cs).setInt(1, 123)
      verify(cs).setInt(2, 1)
      verify(cs).setString(3, "soletrader")
      verify(cs).registerOutParameter(4, OracleTypes.INTEGER)
      verify(cs).execute()
      verify(cs).getInt(4)
      verify(cs).close()
    }

    "call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR with correct parameters for Company" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(eqTo("{ call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR(?, ?, ?, ?) }")))
        .thenReturn(cs)
      when(cs.getInt(4)).thenReturn(4)

      val repo   = new CisFormpRepository(db)
      val result = repo.createSubcontractor(456, Company, 3).futureValue
      result mustBe 4

      verify(cs).setInt(1, 456)
      verify(cs).setInt(2, 3)
      verify(cs).setString(3, "company")
      verify(cs).execute()
      verify(cs).getInt(4)
    }

    "call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR with correct parameters for Partnership" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(eqTo("{ call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR(?, ?, ?, ?) }")))
        .thenReturn(cs)
      when(cs.getInt(4)).thenReturn(6)

      val repo   = new CisFormpRepository(db)
      val result = repo.createSubcontractor(789, Partnership, 5).futureValue
      result mustBe 6

      verify(cs).setString(3, "partnership")
    }

    "call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR with correct parameters for Trust" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(eqTo("{ call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR(?, ?, ?, ?) }")))
        .thenReturn(cs)
      when(cs.getInt(4)).thenReturn(8)

      val repo   = new CisFormpRepository(db)
      val result = repo.createSubcontractor(999, Trust, 7).futureValue
      result mustBe 8

      verify(cs).setString(3, "trust")
    }
  }
}
