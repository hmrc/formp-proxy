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

package uk.gov.hmrc.formpproxy.sdlt.repositories

import org.mockito.ArgumentMatchers.{any as anyArg, eq as eqTo}
import org.mockito.Mockito.*
import play.api.db.Database
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.sdlt.models.*
import uk.gov.hmrc.formpproxy.sdlt.models.vendor.*
import uk.gov.hmrc.formpproxy.sdlt.models.agents.*
import uk.gov.hmrc.formpproxy.sdlt.models.agents.{CreateReturnAgentRequest, DeleteReturnAgentRequest, UpdateReturnAgentRequest}

import java.sql.*

final class SdltFormpRepositorySpec extends SpecBase {

  "sdltCreateReturn" - {

    "call Create_Return stored procedure with correct parameters and return submission ID" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call RETURN_PROCS.Create_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(cs)
      when(cs.getLong(11)).thenReturn(100001L)

      val repo = new SdltFormpRepository(db)

      val request = CreateReturnRequest(
        stornId = "STORN12345",
        purchaserIsCompany = "N",
        surNameOrCompanyName = "Smith",
        houseNumber = Some(42),
        addressLine1 = "High Street",
        addressLine2 = Some("Kensington"),
        addressLine3 = Some("London"),
        addressLine4 = None,
        postcode = Some("SW1A 1AA"),
        transactionType = "RESIDENTIAL"
      )

      val result = repo.sdltCreateReturn(request).futureValue

      result mustBe "100001"

      verify(conn).prepareCall("{ call RETURN_PROCS.Create_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
      verify(cs).setString(1, "STORN12345")
      verify(cs).setString(2, "N")
      verify(cs).setString(3, "Smith")
      verify(cs).setString(4, "42")
      verify(cs).setString(5, "High Street")
      verify(cs).setString(6, "Kensington")
      verify(cs).setString(7, "London")
      verify(cs).setNull(8, Types.VARCHAR)
      verify(cs).setString(9, "SW1A 1AA")
      verify(cs).setString(10, "RESIDENTIAL")
      verify(cs).registerOutParameter(11, Types.NUMERIC)
      verify(cs).execute()
      verify(cs).close()
    }

    "call Create_Return with company purchaser" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)
      when(cs.getLong(11)).thenReturn(100002L)

      val repo = new SdltFormpRepository(db)

      val request = CreateReturnRequest(
        stornId = "STORN99999",
        purchaserIsCompany = "Y",
        surNameOrCompanyName = "ABC Property Ltd",
        houseNumber = Some(100),
        addressLine1 = "Business Park",
        addressLine2 = Some("Westminster"),
        addressLine3 = Some("London"),
        addressLine4 = None,
        postcode = Some("W1B 2EL"),
        transactionType = "NON_RESIDENTIAL"
      )

      val result = repo.sdltCreateReturn(request).futureValue

      result mustBe "100002"

      verify(cs).setString(1, "STORN99999")
      verify(cs).setString(2, "Y")
      verify(cs).setString(3, "ABC Property Ltd")
      verify(cs).execute()
    }

    "handle optional fields being None" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)
      when(cs.getLong(11)).thenReturn(100003L)

      val repo = new SdltFormpRepository(db)

      val request = CreateReturnRequest(
        stornId = "STORN88888",
        purchaserIsCompany = "N",
        surNameOrCompanyName = "Johnson",
        houseNumber = None,
        addressLine1 = "Oak Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        transactionType = "RESIDENTIAL"
      )

      val result = repo.sdltCreateReturn(request).futureValue

      result mustBe "100003"

      verify(cs).setNull(4, Types.VARCHAR)
      verify(cs).setString(5, "Oak Street")
      verify(cs).setNull(6, Types.VARCHAR)
      verify(cs).setNull(7, Types.VARCHAR)
      verify(cs).setNull(8, Types.VARCHAR)
      verify(cs).setNull(9, Types.VARCHAR)
      verify(cs).execute()
    }
  }

  "sdltGetReturn" - {

    "call Get_Return_With_Residency and process all cursors successfully" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      val rsOrg       = mock[ResultSet]
      val rsReturn    = mock[ResultSet]
      val rsPurchaser = mock[ResultSet]
      val rsCompany   = mock[ResultSet]
      val rsVendor    = mock[ResultSet]
      val rsLand      = mock[ResultSet]
      val rsTrans     = mock[ResultSet]
      val rsRetAgent  = mock[ResultSet]
      val rsAgent     = mock[ResultSet]
      val rsLease     = mock[ResultSet]
      val rsTax       = mock[ResultSet]
      val rsSub       = mock[ResultSet]
      val rsSubErr    = mock[ResultSet]
      val rsRes       = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(
        conn.prepareCall(
          eqTo("{ call RETURN_PROCS.Get_Return_With_Residency(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
        )
      )
        .thenReturn(cs)

      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsOrg)
      when(cs.getObject(eqTo(4), eqTo(classOf[ResultSet]))).thenReturn(rsReturn)
      when(cs.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsPurchaser)
      when(cs.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsCompany)
      when(cs.getObject(eqTo(7), eqTo(classOf[ResultSet]))).thenReturn(rsVendor)
      when(cs.getObject(eqTo(8), eqTo(classOf[ResultSet]))).thenReturn(rsLand)
      when(cs.getObject(eqTo(9), eqTo(classOf[ResultSet]))).thenReturn(rsTrans)
      when(cs.getObject(eqTo(10), eqTo(classOf[ResultSet]))).thenReturn(rsRetAgent)
      when(cs.getObject(eqTo(11), eqTo(classOf[ResultSet]))).thenReturn(rsAgent)
      when(cs.getObject(eqTo(12), eqTo(classOf[ResultSet]))).thenReturn(rsLease)
      when(cs.getObject(eqTo(13), eqTo(classOf[ResultSet]))).thenReturn(rsTax)
      when(cs.getObject(eqTo(14), eqTo(classOf[ResultSet]))).thenReturn(rsSub)
      when(cs.getObject(eqTo(15), eqTo(classOf[ResultSet]))).thenReturn(rsSubErr)
      when(cs.getObject(eqTo(16), eqTo(classOf[ResultSet]))).thenReturn(rsRes)

      when(rsOrg.next()).thenReturn(true, false)
      when(rsOrg.getString("IS_RETURN_USER")).thenReturn("Y")
      when(rsOrg.getString("DO_NOT_DISPLAY_WELCOME_PAGE")).thenReturn("N")
      when(rsOrg.getString("STORN")).thenReturn("STORN12345")
      when(rsOrg.getString("VERSION")).thenReturn("1")

      when(rsReturn.next()).thenReturn(true, false)
      when(rsReturn.getString("RETURN_ID")).thenReturn("100001")
      when(rsReturn.getString("STORN")).thenReturn("STORN12345")
      when(rsReturn.getString("STATUS")).thenReturn("STARTED")
      when(rsReturn.getString("RETURN_RESOURCE_REF")).thenReturn("100001")

      when(rsPurchaser.next()).thenReturn(true, false)
      when(rsPurchaser.getString("PURCHASER_ID")).thenReturn("1")
      when(rsPurchaser.getString("IS_COMPANY")).thenReturn("N")
      when(rsPurchaser.getString("SURNAME")).thenReturn("Smith")

      when(rsCompany.next()).thenReturn(false)
      when(rsVendor.next()).thenReturn(true, false)
      when(rsVendor.getString("VENDOR_ID")).thenReturn("1")
      when(rsLand.next()).thenReturn(true, false)
      when(rsLand.getString("LAND_ID")).thenReturn("1")
      when(rsTrans.next()).thenReturn(true, false)
      when(rsTrans.getString("TRANSACTION_ID")).thenReturn("1")
      when(rsTrans.getBigDecimal("TOTAL_CONSIDERATION")).thenReturn(new java.math.BigDecimal("250000.00"))
      when(rsRetAgent.next()).thenReturn(false)
      when(rsAgent.next()).thenReturn(false)
      when(rsLease.next()).thenReturn(false)
      when(rsTax.next()).thenReturn(true, false)
      when(rsTax.getString("TAX_CALCULATION_ID")).thenReturn("1")
      when(rsTax.getString("TAX_DUE")).thenReturn("2500.00")
      when(rsSub.next()).thenReturn(true, false)
      when(rsSub.getString("SUBMISSION_ID")).thenReturn("1")
      when(rsSub.getString("SUBMISSION_STATUS")).thenReturn("STARTED")
      when(rsSubErr.next()).thenReturn(false)
      when(rsRes.next()).thenReturn(true, false)
      when(rsRes.getString("RESIDENCY_ID")).thenReturn("1")

      val repo = new SdltFormpRepository(db)

      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.stornId mustBe Some("STORN12345")
      result.returnResourceRef mustBe Some("100001")
      result.sdltOrganisation must not be None
      result.sdltOrganisation.get.storn mustBe Some("STORN12345")
      result.returnInfo       must not be None
      result.returnInfo.get.status mustBe Some("STARTED")
      result.purchaser        must not be None
      result.purchaser.get    must have size 1
      result.companyDetails mustBe None
      result.vendor           must not be None
      result.land             must not be None
      result.transaction      must not be None
      result.taxCalculation   must not be None
      result.submission       must not be None
      result.residency        must not be None

      verify(conn).prepareCall(
        "{ call RETURN_PROCS.Get_Return_With_Residency(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
      )
      verify(cs).setString(1, "STORN12345")
      verify(cs).setLong(2, 100001L)
      verify(cs).execute()
      verify(cs).close()
    }

    "handle null cursors gracefully" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(anyArg[Int], eqTo(classOf[ResultSet]))).thenReturn(null)

      val repo = new SdltFormpRepository(db)

      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.stornId mustBe Some("STORN12345")
      result.returnResourceRef mustBe Some("100001")
      result.sdltOrganisation mustBe None
      result.returnInfo mustBe None
      result.purchaser mustBe None
      result.vendor mustBe None
      result.transaction mustBe None

      verify(cs).execute()
    }

    "handle empty result sets" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      (3 to 16).foreach { pos =>
        val rs = mock[ResultSet]
        when(cs.getObject(eqTo(pos), eqTo(classOf[ResultSet]))).thenReturn(rs)
        when(rs.next()).thenReturn(false)
      }

      val repo = new SdltFormpRepository(db)

      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.stornId mustBe Some("STORN12345")
      result.returnResourceRef mustBe Some("100001")
      result.sdltOrganisation mustBe None
      result.purchaser mustBe None
      result.vendor mustBe None

      verify(cs).execute()
    }

    "process multiple purchasers correctly" in {
      val db          = mock[Database]
      val conn        = mock[Connection]
      val cs          = mock[CallableStatement]
      val rsPurchaser = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsPurchaser)
      when(rsPurchaser.next()).thenReturn(true, true, false)
      when(rsPurchaser.getString("PURCHASER_ID")).thenReturn("1", "2")
      when(rsPurchaser.getString("SURNAME")).thenReturn("Smith", "Jones")

      (3 to 16).foreach { pos =>
        if (pos != 5) {
          when(cs.getObject(eqTo(pos), eqTo(classOf[ResultSet]))).thenReturn(null)
        }
      }

      val repo = new SdltFormpRepository(db)

      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.purchaser     must not be None
      result.purchaser.get must have size 2
      result.purchaser.get.head.surname mustBe Some("Smith")
      result.purchaser.get.last.surname mustBe Some("Jones")
    }

    "process BigDecimal values in Transaction correctly" in {
      val db      = mock[Database]
      val conn    = mock[Connection]
      val cs      = mock[CallableStatement]
      val rsTrans = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(9), eqTo(classOf[ResultSet]))).thenReturn(rsTrans)
      when(rsTrans.next()).thenReturn(true, false)
      when(rsTrans.getString("TRANSACTION_ID")).thenReturn("1")
      when(rsTrans.getBigDecimal("TOTAL_CONSIDERATION")).thenReturn(new java.math.BigDecimal("250000.00"))
      when(rsTrans.getBigDecimal("CONSIDERATION_CASH")).thenReturn(new java.math.BigDecimal("200000.00"))
      when(rsTrans.getBigDecimal("RELIEF_AMOUNT")).thenReturn(null)

      (3 to 16).foreach { pos =>
        if (pos != 9) {
          when(cs.getObject(eqTo(pos), eqTo(classOf[ResultSet]))).thenReturn(null)
        }
      }

      val repo = new SdltFormpRepository(db)

      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.transaction must not be None
      result.transaction.get.totalConsideration mustBe Some(BigDecimal("250000.00"))
      result.transaction.get.considerationCash mustBe Some(BigDecimal("200000.00"))
      result.transaction.get.reliefAmount mustBe None
    }

    "convert returnResourceRef string to Long correctly" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)
      when(cs.getObject(anyArg[Int], eqTo(classOf[ResultSet]))).thenReturn(null)

      val repo = new SdltFormpRepository(db)

      repo.sdltGetReturn("999999", "STORN12345").futureValue

      verify(cs).setString(1, "STORN12345")
      verify(cs).setLong(2, 999999L)
    }

    "close all result sets properly" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      val resultSets = (3 to 16).map { pos =>
        val rs = mock[ResultSet]
        when(cs.getObject(eqTo(pos), eqTo(classOf[ResultSet]))).thenReturn(rs)
        when(rs.next()).thenReturn(false)
        rs
      }

      val repo = new SdltFormpRepository(db)

      repo.sdltGetReturn("100001", "STORN12345").futureValue

      resultSets.foreach { rs =>
        verify(rs).close()
      }
      verify(cs).close()
    }

    "handle company purchaser with company details" in {
      val db          = mock[Database]
      val conn        = mock[Connection]
      val cs          = mock[CallableStatement]
      val rsPurchaser = mock[ResultSet]
      val rsCompany   = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(5), eqTo(classOf[ResultSet]))).thenReturn(rsPurchaser)
      when(rsPurchaser.next()).thenReturn(true, false)
      when(rsPurchaser.getString("PURCHASER_ID")).thenReturn("1")
      when(rsPurchaser.getString("IS_COMPANY")).thenReturn("Y")
      when(rsPurchaser.getString("COMPANY_NAME")).thenReturn("ABC Property Ltd")

      when(cs.getObject(eqTo(6), eqTo(classOf[ResultSet]))).thenReturn(rsCompany)
      when(rsCompany.next()).thenReturn(true, false)
      when(rsCompany.getString("COMPANY_DETAILS_ID")).thenReturn("1")
      when(rsCompany.getString("UTR")).thenReturn("1234567890")
      when(rsCompany.getString("COMPANY_TYPE_OTHERCOMPANY")).thenReturn("Y")

      (3 to 16).foreach { pos =>
        if (pos != 5 && pos != 6) {
          when(cs.getObject(eqTo(pos), eqTo(classOf[ResultSet]))).thenReturn(null)
        }
      }

      val repo = new SdltFormpRepository(db)

      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.purchaser      must not be None
      result.purchaser.get.head.isCompany mustBe Some("Y")
      result.purchaser.get.head.companyName mustBe Some("ABC Property Ltd")
      result.companyDetails must not be None
      result.companyDetails.get.UTR mustBe Some("1234567890")
    }

    "process ReturnAgent data correctly" in {
      val db         = mock[Database]
      val conn       = mock[Connection]
      val cs         = mock[CallableStatement]
      val rsRetAgent = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(10), eqTo(classOf[ResultSet]))).thenReturn(rsRetAgent)
      when(rsRetAgent.next()).thenReturn(true, false)
      when(rsRetAgent.getString("RETURN_AGENT_ID")).thenReturn("1")
      when(rsRetAgent.getString("RETURN_ID")).thenReturn("100001")
      when(rsRetAgent.getString("AGENT_TYPE")).thenReturn("SOLICITOR")
      when(rsRetAgent.getString("NAME")).thenReturn("Legal Partners LLP")
      when(rsRetAgent.getString("HOUSE_NUMBER")).thenReturn("10")
      when(rsRetAgent.getString("ADDRESS_1")).thenReturn("Law Street")
      when(rsRetAgent.getString("ADDRESS_2")).thenReturn("Legal Quarter")
      when(rsRetAgent.getString("ADDRESS_3")).thenReturn("London")
      when(rsRetAgent.getString("ADDRESS_4")).thenReturn(null)
      when(rsRetAgent.getString("POSTCODE")).thenReturn("LE1 1AW")
      when(rsRetAgent.getString("PHONE")).thenReturn("02012345678")
      when(rsRetAgent.getString("EMAIL")).thenReturn("agent@legal.com")
      when(rsRetAgent.getString("DX_ADDRESS")).thenReturn("DX 12345")
      when(rsRetAgent.getString("REFERENCE")).thenReturn("REF123")
      when(rsRetAgent.getString("IS_AUTHORISED")).thenReturn("Y")

      (3 to 16).foreach { pos =>
        if (pos != 10) {
          when(cs.getObject(eqTo(pos), eqTo(classOf[ResultSet]))).thenReturn(null)
        }
      }

      val repo = new SdltFormpRepository(db)

      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.returnAgent     must not be None
      result.returnAgent.get must have size 1
      val agent = result.returnAgent.get.head
      agent.returnAgentID mustBe Some("1")
      agent.agentType mustBe Some("SOLICITOR")
      agent.name mustBe Some("Legal Partners LLP")
      agent.houseNumber mustBe Some("10")
      agent.address1 mustBe Some("Law Street")
      agent.postcode mustBe Some("LE1 1AW")
      agent.email mustBe Some("agent@legal.com")
      agent.isAuthorised mustBe Some("Y")
    }

    "process multiple ReturnAgents correctly" in {
      val db         = mock[Database]
      val conn       = mock[Connection]
      val cs         = mock[CallableStatement]
      val rsRetAgent = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(10), eqTo(classOf[ResultSet]))).thenReturn(rsRetAgent)
      when(rsRetAgent.next()).thenReturn(true, true, false)
      when(rsRetAgent.getString("RETURN_AGENT_ID")).thenReturn("1", "2")
      when(rsRetAgent.getString("NAME")).thenReturn("Agent One", "Agent Two")

      (3 to 16).foreach { pos =>
        if (pos != 10) {
          when(cs.getObject(eqTo(pos), eqTo(classOf[ResultSet]))).thenReturn(null)
        }
      }

      val repo = new SdltFormpRepository(db)

      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.returnAgent     must not be None
      result.returnAgent.get must have size 2
      result.returnAgent.get.head.name mustBe Some("Agent One")
      result.returnAgent.get.last.name mustBe Some("Agent Two")
    }

    "process Agent data correctly" in {
      val db      = mock[Database]
      val conn    = mock[Connection]
      val cs      = mock[CallableStatement]
      val rsAgent = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(11), eqTo(classOf[ResultSet]))).thenReturn(rsAgent)
      when(rsAgent.next()).thenReturn(true, false)
      when(rsAgent.getString("AGENT_ID")).thenReturn("1")
      when(rsAgent.getString("STORN")).thenReturn("STORN12345")
      when(rsAgent.getString("NAME")).thenReturn("Agent Name")
      when(rsAgent.getString("HOUSE_NUMBER")).thenReturn("100")
      when(rsAgent.getString("ADDRESS_1")).thenReturn("Agent Street")
      when(rsAgent.getString("ADDRESS_2")).thenReturn("Agent Town")
      when(rsAgent.getString("ADDRESS_3")).thenReturn(null)
      when(rsAgent.getString("ADDRESS_4")).thenReturn(null)
      when(rsAgent.getString("POSTCODE")).thenReturn("AG1 1NT")
      when(rsAgent.getString("PHONE")).thenReturn("01234567890")
      when(rsAgent.getString("EMAIL")).thenReturn("agent@test.com")
      when(rsAgent.getString("DX_ADDRESS")).thenReturn("DX123")
      when(rsAgent.getString("AGENT_RESOURCE_REF")).thenReturn("AGENTREF1")

      (3 to 16).foreach { pos =>
        if (pos != 11) {
          when(cs.getObject(eqTo(pos), eqTo(classOf[ResultSet]))).thenReturn(null)
        }
      }

      val repo = new SdltFormpRepository(db)

      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.agent must not be None
      result.agent.get.agentId mustBe Some("1")
      result.agent.get.storn mustBe Some("STORN12345")
      result.agent.get.name mustBe Some("Agent Name")
      result.agent.get.houseNumber mustBe Some("100")
      result.agent.get.address1 mustBe Some("Agent Street")
      result.agent.get.postcode mustBe Some("AG1 1NT")
      result.agent.get.email mustBe Some("agent@test.com")
      result.agent.get.agentResourceReference mustBe Some("AGENTREF1")
    }

    "process Lease data correctly with all fields" in {
      val db      = mock[Database]
      val conn    = mock[Connection]
      val cs      = mock[CallableStatement]
      val rsLease = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(12), eqTo(classOf[ResultSet]))).thenReturn(rsLease)
      when(rsLease.next()).thenReturn(true, false)
      when(rsLease.getString("LEASE_ID")).thenReturn("1")
      when(rsLease.getString("RETURN_ID")).thenReturn("100001")
      when(rsLease.getString("IS_ANNUAL_RENT_OVER_1000")).thenReturn("Y")
      when(rsLease.getString("BREAK_CLAUSE_TYPE")).thenReturn("TENANT")
      when(rsLease.getString("CONTRACT_START_DATE")).thenReturn("2025-01-01")
      when(rsLease.getString("CONTRACT_END_DATE")).thenReturn("2030-12-31")
      when(rsLease.getString("LEASE_TYPE")).thenReturn("NEW")
      when(rsLease.getString("MARKET_RENT")).thenReturn("15000")
      when(rsLease.getString("NET_PRESENT_VALUE")).thenReturn("50000")
      when(rsLease.getString("STARTING_RENT")).thenReturn("12000")
      when(rsLease.getString("SERVICE_CHARGE")).thenReturn("2000")
      when(rsLease.getString("VAT_AMOUNT")).thenReturn("2400")

      (3 to 16).foreach { pos =>
        if (pos != 12) {
          when(cs.getObject(eqTo(pos), eqTo(classOf[ResultSet]))).thenReturn(null)
        }
      }

      val repo = new SdltFormpRepository(db)

      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.lease must not be None
      val lease = result.lease.get
      lease.leaseID mustBe Some("1")
      lease.returnID mustBe Some("100001")
      lease.isAnnualRentOver1000 mustBe Some("Y")
      lease.breakClauseType mustBe Some("TENANT")
      lease.contractStartDate mustBe Some("2025-01-01")
      lease.contractEndDate mustBe Some("2030-12-31")
      lease.leaseType mustBe Some("NEW")
      lease.marketRent mustBe Some("15000")
      lease.netPresentValue mustBe Some("50000")
      lease.startingRent mustBe Some("12000")
      lease.serviceCharge mustBe Some("2000")
      lease.VATAmount mustBe Some("2400")
    }

    "process SubmissionErrorDetails correctly (single row)" in {
      val db       = mock[Database]
      val conn     = mock[Connection]
      val cs       = mock[CallableStatement]
      val rsSubErr = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(15), eqTo(classOf[ResultSet]))).thenReturn(rsSubErr)
      (3 to 16).foreach { pos =>
        if (pos != 15) when(cs.getObject(eqTo(pos), eqTo(classOf[ResultSet]))).thenReturn(null)
      }

      when(rsSubErr.next()).thenReturn(true, false)
      when(rsSubErr.getString("ERROR_DETAIL_ID")).thenReturn("E1")
      when(rsSubErr.getString("RETURN_ID")).thenReturn("100001")
      when(rsSubErr.getString("POSITION")).thenReturn("FIELD:postcode")
      when(rsSubErr.getString("ERROR_MESSAGE")).thenReturn("Invalid postcode format")
      when(rsSubErr.getString("STORN")).thenReturn("STORN12345")
      when(rsSubErr.getString("SUBMISSION_ID")).thenReturn("SUB123")

      val repo   = new SdltFormpRepository(db)
      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.submissionErrorDetails mustBe defined
      val e = result.submissionErrorDetails.value
      e.errorDetailID mustBe Some("E1")
      e.returnID mustBe Some("100001")
      e.position mustBe Some("FIELD:postcode")
      e.errorMessage mustBe Some("Invalid postcode format")
      e.storn mustBe Some("STORN12345")
      e.submissionID mustBe Some("SUB123")
    }

    "process SubmissionErrorDetails when multiple rows are returned (uses first row)" in {
      val db       = mock[Database]
      val conn     = mock[Connection]
      val cs       = mock[CallableStatement]
      val rsSubErr = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(15), eqTo(classOf[ResultSet]))).thenReturn(rsSubErr)
      (3 to 16).foreach { pos =>
        if (pos != 15) when(cs.getObject(eqTo(pos), eqTo(classOf[ResultSet]))).thenReturn(null)
      }

      when(rsSubErr.next()).thenReturn(true, true, false)
      when(rsSubErr.getString("ERROR_DETAIL_ID")).thenReturn("E1", "E2")
      when(rsSubErr.getString("RETURN_ID")).thenReturn("100001", "100001")
      when(rsSubErr.getString("POSITION")).thenReturn("FIELD:surname", "FIELD:address1")
      when(rsSubErr.getString("ERROR_MESSAGE")).thenReturn("Surname missing", "Address line 1 required")
      when(rsSubErr.getString("STORN")).thenReturn("STORN12345", "STORN12345")
      when(rsSubErr.getString("SUBMISSION_ID")).thenReturn("SUB123", "SUB123")

      val repo   = new SdltFormpRepository(db)
      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.submissionErrorDetails mustBe defined
      val e = result.submissionErrorDetails.value
      e.errorDetailID mustBe Some("E1")
      e.position mustBe Some("FIELD:surname")
      e.errorMessage mustBe Some("Surname missing")
    }

    "map null fields in SubmissionErrorDetails to None" in {
      val db       = mock[Database]
      val conn     = mock[Connection]
      val cs       = mock[CallableStatement]
      val rsSubErr = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }
      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(15), eqTo(classOf[ResultSet]))).thenReturn(rsSubErr)
      (3 to 16).foreach { pos =>
        if (pos != 15) when(cs.getObject(eqTo(pos), eqTo(classOf[ResultSet]))).thenReturn(null)
      }

      when(rsSubErr.next()).thenReturn(true, false)
      when(rsSubErr.getString("ERROR_DETAIL_ID")).thenReturn(null)
      when(rsSubErr.getString("RETURN_ID")).thenReturn("100001")
      when(rsSubErr.getString("POSITION")).thenReturn(null)
      when(rsSubErr.getString("ERROR_MESSAGE")).thenReturn(null)
      when(rsSubErr.getString("STORN")).thenReturn("STORN12345")
      when(rsSubErr.getString("SUBMISSION_ID")).thenReturn(null)

      val repo   = new SdltFormpRepository(db)
      val result = repo.sdltGetReturn("100001", "STORN12345").futureValue

      result.submissionErrorDetails mustBe defined
      val e = result.submissionErrorDetails.value
      e.errorDetailID mustBe None
      e.returnID mustBe Some("100001")
      e.position mustBe None
      e.errorMessage mustBe None
      e.storn mustBe Some("STORN12345")
      e.submissionID mustBe None
    }

  }

  "sdltCreateVendor" - {

    "call Create_Vendor stored procedure with correct parameters and return vendor IDs" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call VENDOR_PROCS.Create_Vendor(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(cs)
      when(cs.getLong(14)).thenReturn(100001L)
      when(cs.getLong(15)).thenReturn(1L)

      val repo = new SdltFormpRepository(db)

      val request = CreateVendorRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        title = Some("Mr"),
        forename1 = Some("John"),
        forename2 = Some("James"),
        name = "Smith",
        houseNumber = Some("123"),
        addressLine1 = "Main Street",
        addressLine2 = Some("Apartment 4B"),
        addressLine3 = Some("City Center"),
        addressLine4 = Some("Greater London"),
        postcode = Some("SW1A 1AA"),
        isRepresentedByAgent = "N"
      )

      val result = repo.sdltCreateVendor(request).futureValue

      result.vendorResourceRef mustBe "100001"
      result.vendorId mustBe "1"

      verify(conn).prepareCall("{ call VENDOR_PROCS.Create_Vendor(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
      verify(cs).setString(1, "STORN12345")
      verify(cs).setLong(2, 100001L)
      verify(cs).setString(3, "Mr")
      verify(cs).setString(4, "John")
      verify(cs).setString(5, "James")
      verify(cs).setString(6, "Smith")
      verify(cs).setString(7, "123")
      verify(cs).setString(8, "Main Street")
      verify(cs).setString(9, "Apartment 4B")
      verify(cs).setString(10, "City Center")
      verify(cs).setString(11, "Greater London")
      verify(cs).setString(12, "SW1A 1AA")
      verify(cs).setString(13, "N")
      verify(cs).registerOutParameter(14, Types.NUMERIC)
      verify(cs).registerOutParameter(15, Types.NUMERIC)
      verify(cs).execute()
      verify(cs).close()
    }

    "handle optional fields being None" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)
      when(cs.getLong(14)).thenReturn(100002L)
      when(cs.getLong(15)).thenReturn(2L)

      val repo = new SdltFormpRepository(db)

      val request = CreateVendorRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        title = None,
        forename1 = None,
        forename2 = None,
        name = "Company Vendor Ltd",
        houseNumber = None,
        addressLine1 = "Business Park",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        isRepresentedByAgent = "Y"
      )

      val result = repo.sdltCreateVendor(request).futureValue

      result.vendorResourceRef mustBe "100002"
      result.vendorId mustBe "2"

      verify(cs).setString(1, "STORN99999")
      verify(cs).setLong(2, 100002L)
      verify(cs).setNull(3, Types.VARCHAR)
      verify(cs).setNull(4, Types.VARCHAR)
      verify(cs).setNull(5, Types.VARCHAR)
      verify(cs).setString(6, "Company Vendor Ltd")
      verify(cs).setNull(7, Types.VARCHAR)
      verify(cs).setString(8, "Business Park")
      verify(cs).setNull(9, Types.VARCHAR)
      verify(cs).setNull(10, Types.VARCHAR)
      verify(cs).setNull(11, Types.VARCHAR)
      verify(cs).setNull(12, Types.VARCHAR)
      verify(cs).setString(13, "Y")
      verify(cs).execute()
    }
  }

  "sdltUpdateVendor" - {

    "call Update_Vendor stored procedure with correct parameters" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call VENDOR_PROCS.Update_Vendor(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(cs)

      val repo = new SdltFormpRepository(db)

      val request = UpdateVendorRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        title = Some("Mrs"),
        forename1 = Some("Jane"),
        forename2 = None,
        name = "Doe",
        houseNumber = Some("456"),
        addressLine1 = "Oak Avenue",
        addressLine2 = Some("Suite 10"),
        addressLine3 = None,
        addressLine4 = None,
        postcode = Some("W1A 1AA"),
        isRepresentedByAgent = "Y",
        vendorResourceRef = "100001",
        nextVendorId = Some("100002")
      )

      val result = repo.sdltUpdateVendor(request).futureValue

      result.updated mustBe true

      verify(conn).prepareCall("{ call VENDOR_PROCS.Update_Vendor(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
      verify(cs).setString(1, "STORN12345")
      verify(cs).setLong(2, 100001L)
      verify(cs).setString(3, "Mrs")
      verify(cs).setString(4, "Jane")
      verify(cs).setNull(5, Types.VARCHAR)
      verify(cs).setString(6, "Doe")
      verify(cs).setString(7, "456")
      verify(cs).setString(8, "Oak Avenue")
      verify(cs).setString(9, "Suite 10")
      verify(cs).setNull(10, Types.VARCHAR)
      verify(cs).setNull(11, Types.VARCHAR)
      verify(cs).setString(12, "W1A 1AA")
      verify(cs).setString(13, "Y")
      verify(cs).setLong(14, 100001L)
      verify(cs).setString(15, "100002")
      verify(cs).execute()
      verify(cs).close()
    }

    "handle minimal update with no optional fields" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      val repo = new SdltFormpRepository(db)

      val request = UpdateVendorRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        title = None,
        forename1 = None,
        forename2 = None,
        name = "Updated Vendor Ltd",
        houseNumber = None,
        addressLine1 = "New Business Park",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        isRepresentedByAgent = "N",
        vendorResourceRef = "100002",
        nextVendorId = None
      )

      val result = repo.sdltUpdateVendor(request).futureValue

      result.updated mustBe true

      verify(cs).setString(1, "STORN99999")
      verify(cs).setLong(2, 100002L)
      verify(cs).setNull(3, Types.VARCHAR)
      verify(cs).setNull(4, Types.VARCHAR)
      verify(cs).setNull(5, Types.VARCHAR)
      verify(cs).setString(6, "Updated Vendor Ltd")
      verify(cs).setNull(7, Types.VARCHAR)
      verify(cs).setString(8, "New Business Park")
      verify(cs).setNull(9, Types.VARCHAR)
      verify(cs).setNull(10, Types.VARCHAR)
      verify(cs).setNull(11, Types.VARCHAR)
      verify(cs).setNull(12, Types.VARCHAR)
      verify(cs).setString(13, "N")
      verify(cs).setLong(14, 100002L)
      verify(cs).setNull(15, Types.VARCHAR)
      verify(cs).execute()
    }
  }

  "sdltDeleteVendor" - {

    "call Delete_Vendor stored procedure with correct parameters" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call VENDOR_PROCS.Delete_Vendor(?, ?, ?) }"))).thenReturn(cs)

      val repo = new SdltFormpRepository(db)

      val request = DeleteVendorRequest(
        storn = "STORN12345",
        vendorResourceRef = "100001",
        returnResourceRef = "100001"
      )

      val result = repo.sdltDeleteVendor(request).futureValue

      result.deleted mustBe true

      verify(conn).prepareCall("{ call VENDOR_PROCS.Delete_Vendor(?, ?, ?) }")
      verify(cs).setString(1, "STORN12345")
      verify(cs).setLong(2, 100001L)
      verify(cs).setLong(3, 100001L)
      verify(cs).execute()
      verify(cs).close()
    }

    "handle different vendor resource references" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      val repo = new SdltFormpRepository(db)

      val request = DeleteVendorRequest(
        storn = "STORN99999",
        vendorResourceRef = "999999",
        returnResourceRef = "100002"
      )

      val result = repo.sdltDeleteVendor(request).futureValue

      result.deleted mustBe true

      verify(cs).setString(1, "STORN99999")
      verify(cs).setLong(2, 100002L)
      verify(cs).setLong(3, 999999L)
      verify(cs).execute()
    }
  }

  "sdltCreateReturnAgent" - {

    "call Create_Return_Agent stored procedure with correct parameters and return agent ID" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(
        conn.prepareCall(
          eqTo("{ call RETURN_AGENT_PROCS.Create_Return_Agent(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
        )
      )
        .thenReturn(cs)
      when(cs.getLong(16)).thenReturn(100001L)

      val repo = new SdltFormpRepository(db)

      val request = CreateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR",
        name = "Smith & Partners LLP",
        houseNumber = Some("10"),
        addressLine1 = "Legal District",
        addressLine2 = Some("Business Quarter"),
        addressLine3 = Some("Manchester"),
        addressLine4 = Some("Greater Manchester"),
        postcode = "M1 1AA",
        phoneNumber = Some("0161234567"),
        email = Some("agent@smithpartners.com"),
        agentReference = Some("AGT123456"),
        isAuthorised = Some("Y")
      )

      val result = repo.sdltCreateReturnAgent(request).futureValue

      result.returnAgentID mustBe "100001"

      verify(conn).prepareCall(
        "{ call RETURN_AGENT_PROCS.Create_Return_Agent(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
      )
      verify(cs).setString(1, "STORN12345")
      verify(cs).setLong(2, 100001L)
      verify(cs).setString(3, "SOLICITOR")
      verify(cs).setString(4, "Smith & Partners LLP")
      verify(cs).setString(5, "10")
      verify(cs).setString(6, "Legal District")
      verify(cs).setString(7, "Business Quarter")
      verify(cs).setString(8, "Manchester")
      verify(cs).setString(9, "Greater Manchester")
      verify(cs).setString(10, "M1 1AA")
      verify(cs).setString(11, "0161234567")
      verify(cs).setString(12, "agent@smithpartners.com")
      verify(cs).setNull(13, Types.VARCHAR)
      verify(cs).setString(14, "AGT123456")
      verify(cs).setString(15, "Y")
      verify(cs).registerOutParameter(16, Types.NUMERIC)
      verify(cs).execute()
      verify(cs).close()
    }

    "handle optional fields being None" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)
      when(cs.getLong(16)).thenReturn(100002L)

      val repo = new SdltFormpRepository(db)

      val request = CreateReturnAgentRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        agentType = "ACCOUNTANT",
        name = "Quick Accounting",
        houseNumber = None,
        addressLine1 = "High Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = "EC1A 1BB",
        phoneNumber = None,
        email = None,
        agentReference = None,
        isAuthorised = None
      )

      val result = repo.sdltCreateReturnAgent(request).futureValue

      result.returnAgentID mustBe "100002"

      verify(cs).setString(1, "STORN99999")
      verify(cs).setLong(2, 100002L)
      verify(cs).setString(3, "ACCOUNTANT")
      verify(cs).setString(4, "Quick Accounting")
      verify(cs).setNull(5, Types.VARCHAR)
      verify(cs).setString(6, "High Street")
      verify(cs).setNull(7, Types.VARCHAR)
      verify(cs).setNull(8, Types.VARCHAR)
      verify(cs).setNull(9, Types.VARCHAR)
      verify(cs).setString(10, "EC1A 1BB")
      verify(cs).setNull(11, Types.VARCHAR)
      verify(cs).setNull(12, Types.VARCHAR)
      verify(cs).setNull(13, Types.VARCHAR)
      verify(cs).setNull(14, Types.VARCHAR)
      verify(cs).setNull(15, Types.VARCHAR)
      verify(cs).execute()
    }
  }

  "sdltUpdateReturnAgent" - {

    "call UPDATE_RETURN_AGENT stored procedure with correct parameters" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(
        conn.prepareCall(
          eqTo("{ call RETURN_AGENT_PROCS.UPDATE_RETURN_AGENT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
        )
      )
        .thenReturn(cs)

      val repo = new SdltFormpRepository(db)

      val request = UpdateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR",
        name = "Updated Smith & Partners LLP",
        houseNumber = Some("20"),
        addressLine1 = "New Legal District",
        addressLine2 = Some("Updated Quarter"),
        addressLine3 = Some("Manchester"),
        addressLine4 = None,
        postcode = "M2 2BB",
        phoneNumber = Some("0161999888"),
        email = Some("updated@smithpartners.com"),
        agentReference = Some("AGT999999"),
        isAuthorised = Some("Y")
      )

      val result = repo.sdltUpdateReturnAgent(request).futureValue

      result.updated mustBe true

      verify(conn).prepareCall(
        "{ call RETURN_AGENT_PROCS.UPDATE_RETURN_AGENT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
      )
      verify(cs).setString(1, "STORN12345")
      verify(cs).setLong(2, 100001L)
      verify(cs).setString(3, "SOLICITOR")
      verify(cs).setString(4, "Updated Smith & Partners LLP")
      verify(cs).setString(5, "20")
      verify(cs).setString(6, "New Legal District")
      verify(cs).setString(7, "Updated Quarter")
      verify(cs).setString(8, "Manchester")
      verify(cs).setNull(9, Types.VARCHAR)
      verify(cs).setString(10, "M2 2BB")
      verify(cs).setString(11, "0161999888")
      verify(cs).setString(12, "updated@smithpartners.com")
      verify(cs).setNull(13, Types.VARCHAR)
      verify(cs).setString(14, "AGT999999")
      verify(cs).setString(15, "Y")
      verify(cs).execute()
      verify(cs).close()
    }

    "handle minimal update with no optional fields" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      val repo = new SdltFormpRepository(db)

      val request = UpdateReturnAgentRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        agentType = "ACCOUNTANT",
        name = "Updated Accounting",
        houseNumber = None,
        addressLine1 = "New High Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = "EC2A 2BB",
        phoneNumber = None,
        email = None,
        agentReference = None,
        isAuthorised = None
      )

      val result = repo.sdltUpdateReturnAgent(request).futureValue

      result.updated mustBe true

      verify(cs).setString(1, "STORN99999")
      verify(cs).setLong(2, 100002L)
      verify(cs).setString(3, "ACCOUNTANT")
      verify(cs).setString(4, "Updated Accounting")
      verify(cs).setNull(5, Types.VARCHAR)
      verify(cs).setString(6, "New High Street")
      verify(cs).setNull(7, Types.VARCHAR)
      verify(cs).setNull(8, Types.VARCHAR)
      verify(cs).setNull(9, Types.VARCHAR)
      verify(cs).setString(10, "EC2A 2BB")
      verify(cs).setNull(11, Types.VARCHAR)
      verify(cs).setNull(12, Types.VARCHAR)
      verify(cs).setNull(13, Types.VARCHAR)
      verify(cs).setNull(14, Types.VARCHAR)
      verify(cs).setNull(15, Types.VARCHAR)
      verify(cs).execute()
    }
  }

  "sdltDeleteReturnAgent" - {

    "call Delete_Return_Agent stored procedure with correct parameters" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call RETURN_AGENT_PROCS.Delete_Return_Agent(?, ?, ?) }"))).thenReturn(cs)

      val repo = new SdltFormpRepository(db)

      val request = DeleteReturnAgentRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR"
      )

      val result = repo.sdltDeleteReturnAgent(request).futureValue

      result.deleted mustBe true

      verify(conn).prepareCall("{ call RETURN_AGENT_PROCS.Delete_Return_Agent(?, ?, ?) }")
      verify(cs).setString(1, "STORN12345")
      verify(cs).setLong(2, 100001L)
      verify(cs).setString(3, "SOLICITOR")
      verify(cs).execute()
      verify(cs).close()
    }

    "handle different agent types" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      val repo = new SdltFormpRepository(db)

      val request = DeleteReturnAgentRequest(
        storn = "STORN99999",
        returnResourceRef = "100002",
        agentType = "ACCOUNTANT"
      )

      val result = repo.sdltDeleteReturnAgent(request).futureValue

      result.deleted mustBe true

      verify(cs).setString(1, "STORN99999")
      verify(cs).setLong(2, 100002L)
      verify(cs).setString(3, "ACCOUNTANT")
      verify(cs).execute()
    }
  }

  "sdltUpdateReturnVersion" - {

    "call Update_Version_Number stored procedure with correct parameters and return new version" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call RETURN_PROCS.Update_Version_Number(?, ?, ?) }"))).thenReturn(cs)
      when(cs.getInt(3)).thenReturn(2)

      val repo = new SdltFormpRepository(db)

      val request = ReturnVersionUpdateRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        currentVersion = "1"
      )

      val result = repo.sdltUpdateReturnVersion(request).futureValue

      result.newVersion mustBe 2

      verify(conn).prepareCall("{ call RETURN_PROCS.Update_Version_Number(?, ?, ?) }")
      verify(cs).setString(1, "STORN12345")
      verify(cs).setLong(2, 100001L)
      verify(cs).setLong(3, 1L)
      verify(cs).registerOutParameter(3, Types.NUMERIC)
      verify(cs).execute()
      verify(cs).close()
    }

    "handle version 0 to version 1" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)
      when(cs.getInt(3)).thenReturn(1)

      val repo = new SdltFormpRepository(db)

      val request = ReturnVersionUpdateRequest(
        storn = "STORN11111",
        returnResourceRef = "100003",
        currentVersion = "0"
      )

      val result = repo.sdltUpdateReturnVersion(request).futureValue

      result.newVersion mustBe 1

      verify(cs).setString(1, "STORN11111")
      verify(cs).setLong(2, 100003L)
      verify(cs).setLong(3, 0L)
      verify(cs).execute()
    }

    "handle higher version numbers" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)
      when(cs.getInt(3)).thenReturn(6)

      val repo = new SdltFormpRepository(db)

      val request = ReturnVersionUpdateRequest(
        storn = "STORN99999",
        returnResourceRef = "100002",
        currentVersion = "5"
      )

      val result = repo.sdltUpdateReturnVersion(request).futureValue

      result.newVersion mustBe 6

      verify(cs).setString(1, "STORN99999")
      verify(cs).setLong(2, 100002L)
      verify(cs).setLong(3, 5L)
      verify(cs).registerOutParameter(3, Types.NUMERIC)
      verify(cs).execute()
    }

    "handle very high version numbers" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)
      when(cs.getInt(3)).thenReturn(11)

      val repo = new SdltFormpRepository(db)

      val request = ReturnVersionUpdateRequest(
        storn = "STORN77777",
        returnResourceRef = "999999",
        currentVersion = "10"
      )

      val result = repo.sdltUpdateReturnVersion(request).futureValue

      result.newVersion mustBe 11

      verify(cs).setString(1, "STORN77777")
      verify(cs).setLong(2, 999999L)
      verify(cs).setLong(3, 10L)
      verify(cs).execute()
    }
  }

  "sdltGetOrganisation" - {

    "call Get_SDLT_Organisation stored procedure and map organisation and agents correctly" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      val rsOrg    = mock[ResultSet]
      val rsAgents = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call SDLT_ORGANISATION_PROCS.Get_SDLT_Organisation(?, ?, ?) }")))
        .thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsOrg)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsAgents)

      when(rsOrg.next()).thenReturn(true, false)
      when(rsOrg.getString("IS_RETURN_USER")).thenReturn("Y")
      when(rsOrg.getString("DO_NOT_DISPLAY_WELCOME_PAGE")).thenReturn("N")
      when(rsOrg.getString("STORN")).thenReturn("STORN12345")
      when(rsOrg.getString("VERSION")).thenReturn("1")

      when(rsAgents.next()).thenReturn(true, false)
      when(rsAgents.getString("AGENT_ID")).thenReturn("AGT001")
      when(rsAgents.getString("STORN")).thenReturn("STORN12345")
      when(rsAgents.getString("NAME")).thenReturn("Smith & Co Solicitors")
      when(rsAgents.getString("HOUSE_NUMBER")).thenReturn("10")
      when(rsAgents.getString("ADDRESS_1")).thenReturn("Downing Street")
      when(rsAgents.getString("ADDRESS_2")).thenReturn("Westminster")
      when(rsAgents.getString("ADDRESS_3")).thenReturn("London")
      when(rsAgents.getString("ADDRESS_4")).thenReturn(null)
      when(rsAgents.getString("POSTCODE")).thenReturn("SW1A 2AA")
      when(rsAgents.getString("PHONE")).thenReturn("02071234567")
      when(rsAgents.getString("EMAIL")).thenReturn("info@smithco.co.uk")
      when(rsAgents.getString("DX_ADDRESS")).thenReturn("DX 12345 London")
      when(rsAgents.getString("AGENT_RESOURCE_REF")).thenReturn("AGT-RES-001")

      val repo   = new SdltFormpRepository(db)
      val result = repo.sdltGetOrganisation("STORN12345").futureValue

      result.storn mustBe Some("STORN12345")
      result.version mustBe Some("1")
      result.isReturnUser mustBe Some("Y")
      result.doNotDisplayWelcomePage mustBe Some("N")

      result.agents must have size 1
      val agent = result.agents.head
      agent.agentId mustBe Some("AGT001")
      agent.storn mustBe Some("STORN12345")
      agent.name mustBe Some("Smith & Co Solicitors")
      agent.houseNumber mustBe Some("10")
      agent.address1 mustBe Some("Downing Street")
      agent.address2 mustBe Some("Westminster")
      agent.address3 mustBe Some("London")
      agent.address4 mustBe None
      agent.postcode mustBe Some("SW1A 2AA")
      agent.phone mustBe Some("02071234567")
      agent.email mustBe Some("info@smithco.co.uk")
      agent.dxAddress mustBe Some("DX 12345 London")
      agent.agentResourceReference mustBe Some("AGT-RES-001")

      verify(conn).prepareCall("{ call SDLT_ORGANISATION_PROCS.Get_SDLT_Organisation(?, ?, ?) }")
      verify(cs).setString(1, "STORN12345")
      verify(cs).execute()
      verify(cs).close()
      verify(rsOrg).close()
      verify(rsAgents).close()
    }

    "handle null cursors gracefully" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)
      when(cs.getObject(anyArg[Int], eqTo(classOf[ResultSet]))).thenReturn(null)

      val repo   = new SdltFormpRepository(db)
      val result = repo.sdltGetOrganisation("STORN99999").futureValue

      result.storn mustBe Some("STORN99999")
      result.version mustBe None
      result.isReturnUser mustBe None
      result.doNotDisplayWelcomePage mustBe None
      result.agents mustBe Seq.empty

      verify(cs).setString(1, "STORN99999")
      verify(cs).execute()
    }

    "handle empty organisation and agent result sets" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      val rsOrg    = mock[ResultSet]
      val rsAgents = mock[ResultSet]

      when(db.withConnection(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(anyArg[String])).thenReturn(cs)

      when(cs.getObject(eqTo(2), eqTo(classOf[ResultSet]))).thenReturn(rsOrg)
      when(cs.getObject(eqTo(3), eqTo(classOf[ResultSet]))).thenReturn(rsAgents)

      when(rsOrg.next()).thenReturn(false)
      when(rsAgents.next()).thenReturn(false)

      val repo   = new SdltFormpRepository(db)
      val result = repo.sdltGetOrganisation("STORN00000").futureValue

      result.storn mustBe Some("STORN00000")
      result.version mustBe None
      result.isReturnUser mustBe None
      result.doNotDisplayWelcomePage mustBe None
      result.agents mustBe empty

      verify(cs).execute()
      verify(rsOrg).close()
      verify(rsAgents).close()
    }
  }

  "sdltUpdatePredefinedAgent" - {

    "call Update_Predefined_Agent stored procedure with correct parameters" in {
      val db   = mock[Database]
      val conn = mock[Connection]
      val cs   = mock[CallableStatement]

      when(db.withTransaction(anyArg[Connection => Any])).thenAnswer { inv =>
        val f = inv.getArgument(0, classOf[Connection => Any]); f(conn)
      }

      when(conn.prepareCall(eqTo("{ call AGENT_PROCS.Update_Agent(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")))
        .thenReturn(cs)

      val repo = new SdltFormpRepository(db)

      val request = UpdatePredefinedAgentRequest(
        storn = "STN001",
        agentId = None,
        name = "Smith & Co Solicitors",
        houseNumber = None,
        address1 = "12 High Street",
        address2 = Some("London"),
        address3 = Some("Greater London"),
        address4 = None,
        postcode = Some("SW1A 1AA"),
        phone = "02071234567",
        email = "info@smithco.co.uk",
        dxAddress = None,
        agentResourceReference = "001"
      )

      val result = repo.sdltUpdatePredefinedAgent(request).futureValue

      result.updated mustBe true

      verify(conn).prepareCall("{ call AGENT_PROCS.Update_Agent(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) }")
      verify(cs).setString(1, request.storn)
      verify(cs).setLong(2, request.agentResourceReference.toLong)
      verify(cs).setString(3, request.name)
      verify(cs).setNull(4, Types.VARCHAR)
      verify(cs).setString(5, request.address1)
      verify(cs).setNull(6, Types.VARCHAR)
      verify(cs).setNull(7, Types.VARCHAR)
      verify(cs).setNull(8, Types.VARCHAR)
      verify(cs).setNull(9, Types.VARCHAR)
      verify(cs).setString(10, request.phone)
      verify(cs).setString(11, request.email)
      verify(cs).execute()
      verify(cs).close()
    }
  }
}
