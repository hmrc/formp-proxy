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

import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.{any as anyArg, eq as eqTo}
import play.api.db.Database

import java.sql.*
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.models.requests.{CreateAndTrackSubmissionRequest, UpdateSubmissionRequest}

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
      val db         = mock[Database]
      val conn       = mock[Connection]
      val csCreate   = mock[CallableStatement]
      val csVersion  = mock[CallableStatement]
      val csUpdate   = mock[CallableStatement]
      val psGetScheme= mock[PreparedStatement]
      val rsScheme   = mock[ResultSet]

      when(db.withTransaction(org.mockito.ArgumentMatchers.any[java.sql.Connection => Any]))
        .thenAnswer { inv =>
          val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
        }

      when(conn.prepareStatement("select version from scheme where instance_id = ?"))
        .thenReturn(psGetScheme)
      when(psGetScheme.executeQuery()).thenReturn(rsScheme)
      when(rsScheme.next()).thenReturn(true)
      when(rsScheme.getInt(1)).thenReturn(3)

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

      verify(conn).prepareStatement("select version from scheme where instance_id = ?")
      verify(psGetScheme).executeQuery()

      verify(conn).prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }")
      verify(csCreate).execute()

      verify(conn).prepareCall("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }")
      verify(csVersion).execute()

      verify(conn).prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
      verify(csUpdate).execute()
    }
  }

  "createAndTrackSubmission" - {

    "look up ids, call SPs, return the submission id, and close resources" in {
      val db       = mock[Database]
      val conn     = mock[Connection]
      val psScheme = mock[PreparedStatement]
      val rsScheme = mock[ResultSet]
      val psMonthly= mock[PreparedStatement]
      val rsMonthly= mock[ResultSet]
      val csCreate = mock[CallableStatement]
      val csTrack  = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareStatement(eqTo("select scheme_id from scheme where instance_id = ?")))
        .thenReturn(psScheme)
      when(conn.prepareStatement(eqTo(
        "select monthly_return_id from monthly_return " +
          "where scheme_id = ? and tax_year = ? and tax_month = ?"
      ))).thenReturn(psMonthly)

      when(psScheme.executeQuery()).thenReturn(rsScheme)
      when(rsScheme.next()).thenReturn(true)
      when(rsScheme.getLong(1)).thenReturn(9999L)

      when(psMonthly.executeQuery()).thenReturn(rsMonthly)
      when(rsMonthly.next()).thenReturn(true)
      when(rsMonthly.getLong(1)).thenReturn(7777L)

      when(conn.prepareCall(eqTo("{ call SUBMISSION_PROCS.Create_Submission(?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(csCreate)
      when(conn.prepareCall(eqTo("{ call HONESTY_DECLARATION_PROCS.TRACK_SUBMISSIONS(?, ?, ?, ?, ?, ?) }")))
        .thenReturn(csTrack)

      when(csCreate.getLong(9)).thenReturn(12345L)

      val repo = new CisFormpRepository(db)

      val req = CreateAndTrackSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        hmrcMarkGenerated = Some("Dj5TVJDyRYCn9zta5EdySeY4fyA="),
        emailRecipient = Some("ops@example.com"),
        agentId = None,
        subcontractorCount = Some(2),
        totalPaymentsMade = Some(BigDecimal(1000)),
        totalTaxDeducted = Some(BigDecimal(200))
      )

      val out = repo.createAndTrackSubmission(req).futureValue
      out mustBe "12345"

      verify(conn).prepareCall(eqTo("{ call SUBMISSION_PROCS.Create_Submission(?, ?, ?, ?, ?, ?, ?, ?, ?) }"))
      verify(csCreate).execute()

      verify(conn).prepareCall(eqTo("{ call HONESTY_DECLARATION_PROCS.TRACK_SUBMISSIONS(?, ?, ?, ?, ?, ?) }"))
      verify(csTrack).execute()
    }
  }

  "updateMonthlyReturnSubmission" - {

    "look up ids, call update SP with correct params, and close resources" in {
      val db       = mock[Database]
      val conn     = mock[Connection]
      val psScheme = mock[PreparedStatement]
      val rsScheme = mock[ResultSet]
      val psMonthly= mock[PreparedStatement]
      val rsMonthly= mock[ResultSet]
      val csUpdate = mock[CallableStatement]

      when(db.withConnection(anyArg[java.sql.Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[java.sql.Connection => Any]); f(conn)
      }

      when(conn.prepareStatement(eqTo("select scheme_id from scheme where instance_id = ?")))
        .thenReturn(psScheme)
      when(conn.prepareStatement(eqTo(
        "select monthly_return_id from monthly_return " +
          "where scheme_id = ? and tax_year = ? and tax_month = ?"
      ))).thenReturn(psMonthly)

      when(psScheme.executeQuery()).thenReturn(rsScheme)
      when(rsScheme.next()).thenReturn(true)
      when(rsScheme.getLong(1)).thenReturn(9999L)

      when(psMonthly.executeQuery()).thenReturn(rsMonthly)
      when(rsMonthly.next()).thenReturn(true)
      when(rsMonthly.getLong(1)).thenReturn(7777L)

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

      verify(conn).prepareStatement(eqTo("select scheme_id from scheme where instance_id = ?"))
      verify(conn).prepareStatement(eqTo(
        "select monthly_return_id from monthly_return where scheme_id = ? and tax_year = ? and tax_month = ?"
      ))
    }
  }


}