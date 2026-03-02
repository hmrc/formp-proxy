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
//import uk.gov.hmrc.formpproxy.sdlt.models.GetReturnRecordsRequest
//import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository
//
//class GetReturnsOrderingIntegrationSpec
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
//  "getReturns" should {
//
//    "returns correct number of IN-PROGRESS returns ordered by last update date DESC" in {
//      val request = GetReturnRecordsRequest(
//        storn = "STN001",
//        status = None,
//        deletionFlag = false,
//        pageType = Some("IN-PROGRESS"),
//        pageNumber = None
//      )
//
//      val result = repo.sdltGetReturns(request).futureValue
//
//      result.returnSummaryList.length mustBe 6
//      result.returnSummaryList.map(_.returnReference) mustBe Seq("5", "6", "4", "1", "2", "3")
//    }
//
//    "returns correct number of SUBMITTED returns ordered by submitted date DESC" in {
//      val request = GetReturnRecordsRequest(
//        storn = "STN001",
//        status = None,
//        deletionFlag = false,
//        pageType = Some("SUBMITTED"),
//        pageNumber = None
//      )
//
//      val result = repo.sdltGetReturns(request).futureValue
//
//      result.returnSummaryList.length mustBe 3
//      result.returnSummaryList.map(_.returnReference) mustBe Seq("8", "9", "7")
//    }
//
//    "returns correct number of DUE-FOR-DELETION returns ordered by purge date ASC" in {
//      val request = GetReturnRecordsRequest(
//        storn = "STN001",
//        status = None,
//        deletionFlag = true,
//        pageType = Some("IN-PROGRESS"),
//        pageNumber = None
//      )
//
//      val result = repo.sdltGetReturns(request).futureValue
//
//      result.returnSummaryList.length mustBe 3
//      result.returnSummaryList.map(_.returnReference) mustBe Seq("4", "6", "5")
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
//      orgStatement.setString(1, "STN001")
//      orgStatement.execute()
//      orgStatement.close()
//
//      val returnStatement = conn.prepareStatement(
//        """
//          |INSERT INTO SDLT_FILE_DATA.RETURN (RETURN_ID, STORN, PURCHASER_COUNTER, VENDOR_COUNTER, LAND_COUNTER, PURGE_DATE, RETURN_RESOURCE_REF, STATUS, CREATE_DATE, LAST_UPDATE_DATE)
//          |VALUES (?, 'STN001', 1, 1, 1, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, TO_DATE(?, 'YYYY-MM-DD'), TO_DATE(?, 'YYYY-MM-DD'))
//          |""".stripMargin
//      )
//
//      val returnTestData = Seq(
//        (1000001L, "2027-01-01", 1L, "STARTED", "2026-02-15", "2026-02-15"), // IN-PROGRESS
//        (1000002L, "2027-01-01", 2L, "ACCEPTED", "2026-02-10", "2026-02-10"),
//        (1000003L, "2027-01-01", 3L, "ACCEPTED", "2026-02-15", "2026-02-05"),
//        (1000004L, "2026-02-21", 4L, "STARTED", "2026-02-20", "2026-02-20"), // DUE-FOR-DELETION
//        (1000005L, "2026-03-01", 5L, "ACCEPTED", "2026-02-28", "2026-02-28"),
//        (1000006L, "2026-02-26", 6L, "STARTED", "2026-02-25", "2026-02-25"),
//        (1000007L, "2027-01-01", 7L, "SUBMITTED", "2026-02-08", "2026-02-08"), // SUBMITTED
//        (1000008L, "2027-01-01", 8L, "SUBMITTED", "2026-02-18", "2026-02-18"),
//        (1000009L, "2027-01-01", 9L, "SUBMITTED", "2026-02-13", "2026-02-13")
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
//      val submissionStatement = conn.prepareStatement(
//        """
//          |INSERT INTO SDLT_FILE_DATA.SUBMISSION (SUBMISSION_ID, RETURN_ID, STORN, SUBMITTED_DATE, CREATE_DATE, LAST_UPDATE_DATE)
//          |VALUES (?, ?, 'STN001', TO_DATE(?, 'YYYY-MM-DD'), TO_DATE(?, 'YYYY-MM-DD'), TO_DATE(?, 'YYYY-MM-DD'))
//          |""".stripMargin
//      )
//
//      val submissionTestData = Seq(
//        (2000001L, 1000007L, "2026-02-08", "2026-02-08", "2026-02-08"),
//        (2000002L, 1000008L, "2026-02-18", "2026-02-18", "2026-02-18"),
//        (2000003L, 1000009L, "2026-02-13", "2026-02-13", "2026-02-13")
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
//    }
//  }
//
//  private def cleanupTestReturns(): Unit = {
//    db.withConnection { conn =>
//      val cleanupSubmissionsStatement = conn.prepareStatement(
//        """
//          |DELETE FROM SDLT_FILE_DATA.SUBMISSION
//          |WHERE STORN = 'STN001'
//          |""".stripMargin
//      )
//      cleanupSubmissionsStatement.execute()
//      cleanupSubmissionsStatement.close()
//
//      val cleanupReturnsStatement = conn.prepareStatement(
//        """
//          |DELETE FROM SDLT_FILE_DATA.RETURN
//          |WHERE STORN = 'STN001'
//          |""".stripMargin
//      )
//      cleanupReturnsStatement.execute()
//      cleanupReturnsStatement.close()
//
//      val cleanupOrgStatement = conn.prepareStatement(
//        """
//          |DELETE FROM SDLT_FILE_DATA.SDLT_ORGANISATION
//          |WHERE STORN = 'STN001'
//          |""".stripMargin
//      )
//      cleanupOrgStatement.execute()
//      cleanupOrgStatement.close()
//    }
//  }
//}
