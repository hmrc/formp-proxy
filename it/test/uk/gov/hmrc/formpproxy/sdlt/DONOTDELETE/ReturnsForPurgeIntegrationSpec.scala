/*
 * Copyright 2026 HM Revenue & Customs
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

//package uk.gov.hmrc.formpproxy.sdlt.DONOTDELETE
//
//import org.scalatest.BeforeAndAfterEach
//import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import org.scalatestplus.play.guice.GuiceOneAppPerSuite
//import play.api.db.{DBApi, Database}
//import uk.gov.hmrc.formpproxy.sdlt.models.{DeleteReturnRequest, GetReturnsForPurgeRequest}
//import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository
//
//import java.time.LocalDate
//
//class ReturnsForPurgeIntegrationSpec
//  extends AnyWordSpec
//    with Matchers
//    with ScalaFutures
//    with IntegrationPatience
//    with GuiceOneAppPerSuite
//    with BeforeAndAfterEach {
//
//  private def db: Database =
//    app.injector.instanceOf[DBApi].database("sdlt")
//
//  private lazy val repo = app.injector.instanceOf[SdltFormpRepository]
//
//  override protected def beforeEach(): Unit = {
//    super.beforeEach()
//    cleanupTestReturns()
//    setupTestReturns()
//  }
//
//  "getReturnsForPurge" should {
//
//    "returns the returns due for purge and excludes those not yet due" in {
//      val request = GetReturnsForPurgeRequest(
//        purgeDate = LocalDate.parse("2026-06-29")
//      )
//
//      val result = repo.sdltGetReturnsForPurge(request).futureValue
//
//      val resourceRefs = result.returnsForPurge.map(_.returnResourceRef)
//      resourceRefs must contain ("9000001")
//      resourceRefs must contain ("9000002")
//      resourceRefs must not contain ("9000003")
//      resourceRefs must not contain ("9000004")
//    }
//
//    "maps each due return to its storn, return resource ref and status" in {
//      val request = GetReturnsForPurgeRequest(
//        purgeDate = LocalDate.parse("2026-06-29")
//      )
//
//      val result = repo.sdltGetReturnsForPurge(request).futureValue
//
//      val dueReturn = result.returnsForPurge.find(_.returnResourceRef == "9000001").get
//      dueReturn.storn mustBe "STN700"
//      dueReturn.status mustBe "SUBMITTED"
//    }
//
//    "includes a submitted return on the 30-day boundary and a return on the 90-day boundary" in {
//      val request = GetReturnsForPurgeRequest(
//        purgeDate = LocalDate.parse("2026-06-29")
//      )
//
//      val result = repo.sdltGetReturnsForPurge(request).futureValue
//
//      val resourceRefs = result.returnsForPurge.map(_.returnResourceRef)
//      resourceRefs must contain ("9000010")
//      resourceRefs must contain ("9000013")
//      resourceRefs must contain ("9000015")
//    }
//
//    "excludes returns that are one day short of the 30-day and 90-day windows" in {
//      val request = GetReturnsForPurgeRequest(
//        purgeDate = LocalDate.parse("2026-06-29")
//      )
//
//      val result = repo.sdltGetReturnsForPurge(request).futureValue
//
//      val resourceRefs = result.returnsForPurge.map(_.returnResourceRef)
//      resourceRefs must not contain ("9000011")
//      resourceRefs must not contain ("9000014")
//    }
//
//    "excludes a non-submitted return older than 30 days but less than 90 days" in {
//      val request = GetReturnsForPurgeRequest(
//        purgeDate = LocalDate.parse("2026-06-29")
//      )
//
//      val result = repo.sdltGetReturnsForPurge(request).futureValue
//
//      val resourceRefs = result.returnsForPurge.map(_.returnResourceRef)
//      resourceRefs must not contain ("9000012")
//    }
//  }
//
//  "deleteReturn" should {
//
//    "deletes the return and its child rows so it is no longer due for purge" in {
//      val deleteRequest = DeleteReturnRequest(
//        storn = "STN700",
//        returnResourceRef = "9000001"
//      )
//
//      repo.sdltDeleteReturn(deleteRequest).futureValue.deleted mustBe true
//
//      val request = GetReturnsForPurgeRequest(
//        purgeDate = LocalDate.parse("2026-06-29")
//      )
//
//      val result = repo.sdltGetReturnsForPurge(request).futureValue
//      result.returnsForPurge.map(_.returnResourceRef) must not contain ("9000001")
//
//      countRowsForReturn("RETURN", 7000001L) mustBe 0
//      countRowsForReturn("SUBMISSION", 7000001L) mustBe 0
//      countRowsForReturn("TRANSACTION", 7000001L) mustBe 0
//    }
//
//    "fails when the return does not exist" in {
//      val deleteRequest = DeleteReturnRequest(
//        storn = "STN700",
//        returnResourceRef = "9999999"
//      )
//
//      repo.sdltDeleteReturn(deleteRequest).failed.futureValue mustBe a[java.sql.SQLException]
//    }
//  }
//
//  private def setupTestReturns(): Unit = {
//    db.withConnection { conn =>
//      val orgStatement = conn.prepareStatement(
//        """
//          |INSERT INTO SDLT_FILE_DATA.SDLT_ORGANISATION (STORN, CREATE_DATE, LAST_UPDATE_DATE)
//          |VALUES (?, SYSDATE, SYSDATE)""".stripMargin
//      )
//
//      orgStatement.setString(1, "STN700")
//      orgStatement.execute()
//      orgStatement.close()
//
//      val returnStatement = conn.prepareStatement(
//        """
//          |INSERT INTO SDLT_FILE_DATA.RETURN (RETURN_ID, STORN, PURCHASER_COUNTER, VENDOR_COUNTER, LAND_COUNTER, PURGE_DATE, RETURN_RESOURCE_REF, STATUS, CREATE_DATE, LAST_UPDATE_DATE)
//          |VALUES (?, 'STN700', 1, 1, 1, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, TO_DATE(?, 'YYYY-MM-DD'), TO_DATE(?, 'YYYY-MM-DD'))
//          |""".stripMargin
//      )
//
//      val returnTestData = Seq(
//        (7000001L, "2020-01-01", 9000001L, "SUBMITTED", "2020-01-01", "2020-01-01"), // DUE (submitted, purge date over 30 days old)
//        (7000002L, "2020-01-01", 9000002L, "STARTED", "2020-01-01", "2020-01-01"),   // DUE (any status, purge date over 90 days old)
//        (7000003L, "2099-01-01", 9000003L, "SUBMITTED", "2026-01-01", "2026-01-01"), // NOT DUE (purge date in the future)
//        (7000004L, "2099-01-01", 9000004L, "STARTED", "2026-01-01", "2026-01-01")    // NOT DUE (purge date in the future)
//      )
//
//      returnTestData.foreach { case (returnId, purgeDate, resourceRef, status, createdDate, lastUpdateDate) =>
//        returnStatement.setLong(1, returnId)
//        returnStatement.setString(2, purgeDate)
//        returnStatement.setLong(3, resourceRef)
//        returnStatement.setString(4, status)
//        returnStatement.setString(5, createdDate)
//        returnStatement.setString(6, lastUpdateDate)
//        returnStatement.addBatch()
//      }
//      returnStatement.executeBatch()
//      returnStatement.close()
//
//      val boundaryReturnStatement = conn.prepareStatement(
//        """
//          |INSERT INTO SDLT_FILE_DATA.RETURN (RETURN_ID, STORN, PURCHASER_COUNTER, VENDOR_COUNTER, LAND_COUNTER, PURGE_DATE, RETURN_RESOURCE_REF, STATUS, CREATE_DATE, LAST_UPDATE_DATE)
//          |VALUES (?, 'STN700', 1, 1, 1, TRUNC(SYSDATE) - ?, ?, ?, SYSDATE, SYSDATE)
//          |""".stripMargin
//      )
//
//      val boundaryTestData = Seq(
//        (7000010L, 30, 9000010L, "SUBMITTED"),
//        (7000011L, 29, 9000011L, "SUBMITTED"),
//        (7000012L, 31, 9000012L, "STARTED"),
//        (7000013L, 90, 9000013L, "STARTED"),
//        (7000014L, 89, 9000014L, "STARTED"),
//        (7000015L, 30, 9000015L, "SUBMITTED_NO_RECEIPT")
//      )
//
//      boundaryTestData.foreach { case (returnId, daysAgo, resourceRef, status) =>
//        boundaryReturnStatement.setLong(1, returnId)
//        boundaryReturnStatement.setInt(2, daysAgo)
//        boundaryReturnStatement.setLong(3, resourceRef)
//        boundaryReturnStatement.setString(4, status)
//        boundaryReturnStatement.addBatch()
//      }
//      boundaryReturnStatement.executeBatch()
//      boundaryReturnStatement.close()
//
//      val submissionStatement = conn.prepareStatement(
//        """
//          |INSERT INTO SDLT_FILE_DATA.SUBMISSION (SUBMISSION_ID, RETURN_ID, STORN, SUBMITTED_DATE, CREATE_DATE, LAST_UPDATE_DATE)
//          |VALUES (?, ?, 'STN700', TO_DATE(?, 'YYYY-MM-DD'), TO_DATE(?, 'YYYY-MM-DD'), TO_DATE(?, 'YYYY-MM-DD'))
//          |""".stripMargin
//      )
//
//      val submissionTestData = Seq(
//        (8000001L, 7000001L, "2020-01-01", "2020-01-01", "2020-01-01")
//      )
//
//      submissionTestData.foreach { case (submissionId, returnId, submittedDate, createdDate, lastUpdateDate) =>
//        submissionStatement.setLong(1, submissionId)
//        submissionStatement.setLong(2, returnId)
//        submissionStatement.setString(3, submittedDate)
//        submissionStatement.setString(4, createdDate)
//        submissionStatement.setString(5, lastUpdateDate)
//        submissionStatement.addBatch()
//      }
//      submissionStatement.executeBatch()
//      submissionStatement.close()
//
//      val transactionStatement = conn.prepareStatement(
//        """
//          |INSERT INTO SDLT_FILE_DATA.TRANSACTION (TRANSACTION_ID, RETURN_ID, TRANSACTION_DESCRIPTION, CREATE_DATE, LAST_UPDATE_DATE)
//          |VALUES (?, ?, ?, SYSDATE, SYSDATE)
//          |""".stripMargin
//      )
//
//      transactionStatement.setLong(1, 8500001L)
//      transactionStatement.setLong(2, 7000001L)
//      transactionStatement.setString(3, "F")
//      transactionStatement.execute()
//      transactionStatement.close()
//    }
//  }
//
//  private def cleanupTestReturns(): Unit = {
//    db.withConnection { conn =>
//      val cleanupSubmissionsStatement = conn.prepareStatement(
//        """
//          |DELETE FROM SDLT_FILE_DATA.SUBMISSION
//          |WHERE STORN = 'STN700'
//          |""".stripMargin
//      )
//      cleanupSubmissionsStatement.execute()
//      cleanupSubmissionsStatement.close()
//
//      val cleanupTransactionsStatement = conn.prepareStatement(
//        """
//          |DELETE FROM SDLT_FILE_DATA.TRANSACTION
//          |WHERE RETURN_ID IN (SELECT RETURN_ID FROM SDLT_FILE_DATA.RETURN WHERE STORN = 'STN700')
//          |""".stripMargin
//      )
//      cleanupTransactionsStatement.execute()
//      cleanupTransactionsStatement.close()
//
//      val cleanupReturnsStatement = conn.prepareStatement(
//        """
//          |DELETE FROM SDLT_FILE_DATA.RETURN
//          |WHERE STORN = 'STN700'
//          |""".stripMargin
//      )
//      cleanupReturnsStatement.execute()
//      cleanupReturnsStatement.close()
//
//      val cleanupOrgStatement = conn.prepareStatement(
//        """
//          |DELETE FROM SDLT_FILE_DATA.SDLT_ORGANISATION
//          |WHERE STORN = 'STN700'
//          |""".stripMargin
//      )
//      cleanupOrgStatement.execute()
//      cleanupOrgStatement.close()
//    }
//  }
//
//  private def countRowsForReturn(table: String, returnId: Long): Int =
//    db.withConnection { conn =>
//      val statement = conn.prepareStatement(
//        s"SELECT COUNT(*) FROM SDLT_FILE_DATA.$table WHERE RETURN_ID = ?"
//      )
//      statement.setLong(1, returnId)
//      val resultSet = statement.executeQuery()
//      try {
//        resultSet.next()
//        resultSet.getInt(1)
//      } finally {
//        resultSet.close()
//        statement.close()
//      }
//    }
//}
