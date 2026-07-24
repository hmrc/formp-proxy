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
//import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository
//
//class SubmissionsForPollingIntegrationSpec
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
//    cleanupTestSubmissions()
//    setupTestSubmissions()
//  }
//
//  "getSubmissionsForPolling" should {
//
//    "returns accepted submissions older than 5 minutes and excludes the rest" in {
//      val result = repo.sdltGetSubmissionsForPolling().futureValue
//
//      val submissionIds = result.submissions.map(_.submissionId)
//      submissionIds must contain ("9500001")
//      submissionIds must not contain ("9500002")
//      submissionIds must not contain ("9500003")
//      submissionIds must not contain ("9500004")
//    }
//
//    "maps each due submission to its storn, submission id, return resource ref and status" in {
//      val result = repo.sdltGetSubmissionsForPolling().futureValue
//
//      val dueSubmission = result.submissions.find(_.submissionId == "9500001").get
//      dueSubmission.storn mustBe "STN710"
//      dueSubmission.returnResourceRef mustBe "9100001"
//      dueSubmission.submissionStatus mustBe "ACCEPTED"
//    }
//
//    "includes an accepted submission just past the 5 minute window and excludes one within it" in {
//      val result = repo.sdltGetSubmissionsForPolling().futureValue
//
//      val submissionIds = result.submissions.map(_.submissionId)
//      submissionIds must contain ("9500006")
//      submissionIds must not contain ("9500005")
//    }
//  }
//
//  private def setupTestSubmissions(): Unit = {
//    db.withConnection { conn =>
//      val orgStatement = conn.prepareStatement(
//        """
//          |INSERT INTO SDLT_FILE_DATA.SDLT_ORGANISATION (STORN, CREATE_DATE, LAST_UPDATE_DATE)
//          |VALUES (?, SYSDATE, SYSDATE)""".stripMargin
//      )
//
//      orgStatement.setString(1, "STN710")
//      orgStatement.execute()
//      orgStatement.close()
//
//      val returnStatement = conn.prepareStatement(
//        """
//          |INSERT INTO SDLT_FILE_DATA.RETURN (RETURN_ID, STORN, PURCHASER_COUNTER, VENDOR_COUNTER, LAND_COUNTER, RETURN_RESOURCE_REF, STATUS, CREATE_DATE, LAST_UPDATE_DATE)
//          |VALUES (?, 'STN710', 1, 1, 1, ?, ?, SYSDATE, SYSDATE)
//          |""".stripMargin
//      )
//
//      val returnTestData = Seq(
//        (8000001L, 9100001L, "PENDING"),
//        (8000002L, 9100002L, "PENDING"),
//        (8000003L, 9100003L, "SUBMITTED"),
//        (8000004L, 9100004L, "STARTED"),
//        (8000005L, 9100005L, "PENDING"),
//        (8000006L, 9100006L, "PENDING")
//      )
//
//      returnTestData.foreach { case (returnId, resourceRef, status) =>
//        returnStatement.setLong(1, returnId)
//        returnStatement.setLong(2, resourceRef)
//        returnStatement.setString(3, status)
//        returnStatement.addBatch()
//      }
//      returnStatement.executeBatch()
//      returnStatement.close()
//
//      val submissionStatement = conn.prepareStatement(
//        """
//          |INSERT INTO SDLT_FILE_DATA.SUBMISSION (SUBMISSION_ID, RETURN_ID, STORN, SUBMISSION_STATUS, SUBMISSION_REQUEST_DATE, CREATE_DATE, LAST_UPDATE_DATE)
//          |VALUES (?, ?, 'STN710', ?, SYSDATE - (? / 86400), SYSDATE, SYSDATE)
//          |""".stripMargin
//      )
//
//      val submissionTestData = Seq(
//        (9500001L, 8000001L, "ACCEPTED", 600),   // DUE (accepted, requested 10 minutes ago)
//        (9500002L, 8000002L, "ACCEPTED", 120),   // NOT DUE (accepted but requested within the last 5 minutes)
//        (9500003L, 8000003L, "SUBMITTED", 3600), // NOT DUE (already submitted)
//        (9500004L, 8000004L, "PENDING", 3600),   // NOT DUE (not accepted)
//        (9500005L, 8000005L, "ACCEPTED", 60),    // NOT DUE (accepted, 1 minute ago)
//        (9500006L, 8000006L, "ACCEPTED", 310)    // DUE (accepted, just past the 5 minute window)
//      )
//
//      submissionTestData.foreach { case (submissionId, returnId, status, secondsAgo) =>
//        submissionStatement.setLong(1, submissionId)
//        submissionStatement.setLong(2, returnId)
//        submissionStatement.setString(3, status)
//        submissionStatement.setInt(4, secondsAgo)
//        submissionStatement.addBatch()
//      }
//      submissionStatement.executeBatch()
//      submissionStatement.close()
//    }
//  }
//
//  private def cleanupTestSubmissions(): Unit = {
//    db.withConnection { conn =>
//      val cleanupSubmissionStatement = conn.prepareStatement(
//        """
//          |DELETE FROM SDLT_FILE_DATA.SUBMISSION
//          |WHERE STORN = 'STN710'
//          |""".stripMargin
//      )
//      cleanupSubmissionStatement.execute()
//      cleanupSubmissionStatement.close()
//
//      val cleanupReturnStatement = conn.prepareStatement(
//        """
//          |DELETE FROM SDLT_FILE_DATA.RETURN
//          |WHERE STORN = 'STN710'
//          |""".stripMargin
//      )
//      cleanupReturnStatement.execute()
//      cleanupReturnStatement.close()
//
//      val cleanupOrgStatement = conn.prepareStatement(
//        """
//          |DELETE FROM SDLT_FILE_DATA.SDLT_ORGANISATION
//          |WHERE STORN = 'STN710'
//          |""".stripMargin
//      )
//      cleanupOrgStatement.execute()
//      cleanupOrgStatement.close()
//    }
//  }
//}
