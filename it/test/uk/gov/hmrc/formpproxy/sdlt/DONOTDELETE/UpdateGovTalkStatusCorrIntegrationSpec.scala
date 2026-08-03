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
//import uk.gov.hmrc.formpproxy.sdlt.models.submission.UpdateGovTalkStatusCorrelationIdRequest
//import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository
//
//class UpdateGovTalkStatusCorrIntegrationSpec
//  extends AnyWordSpec
//    with Matchers
//    with ScalaFutures
//    with IntegrationPatience
//    with GuiceOneAppPerSuite
//    with BeforeAndAfterEach {
//
//  private val storn        = "STNIT001"
//  private val formResultId = "9200999"
//
//  private def db: Database =
//    app.injector.instanceOf[DBApi].database("sdlt")
//
//  private lazy val repo = app.injector.instanceOf[SdltFormpRepository]
//
//  override protected def beforeEach(): Unit = {
//    super.beforeEach()
//    cleanupGovTalkRow()
//    insertGovTalkRow()
//  }
//
//  override protected def afterEach(): Unit = {
//    cleanupGovTalkRow()
//    super.afterEach()
//  }
//
//  "updateGovTalkStatusCorrelationId" should {
//
//    "be accepted by SUBMISSION_ADMIN.UpdateGovTalkStatusCorr" in {
//      val request = UpdateGovTalkStatusCorrelationIdRequest(
//        userIdentifier = storn,
//        formResultId   = formResultId,
//        correlationId  = "CORR123456789012345678901234",
//        pollInterval   = 120,
//        gatewayUrl     = "http://chris.example/poll/abc"
//      )
//
//      repo.sdltUpdateGovTalkStatusCorrelationId(request).futureValue.success mustBe true
//    }
//
//    "write the correlation id, poll interval and gateway url to the row" in {
//      repo.sdltUpdateGovTalkStatusCorrelationId(UpdateGovTalkStatusCorrelationIdRequest(
//        userIdentifier = storn,
//        formResultId   = formResultId,
//        correlationId  = "CORR123456789012345678901234",
//        pollInterval   = 120,
//        gatewayUrl     = "http://chris.example/poll/abc"
//      )).futureValue
//
//      val row = selectGovTalkRow()
//
//      row("CORRELATIONID") mustBe "CORR123456789012345678901234"
//      row("POLL_INTERVAL") mustBe "120"
//      row("GATEWAYURL")    mustBe "http://chris.example/poll/abc"
//    }
//  }
//
//  private def insertGovTalkRow(): Unit =
//    db.withConnection { conn =>
//      val ps = conn.prepareStatement(
//        """INSERT INTO govtalk_status
//          |  (user_identifier, formresultid, correlationid, form_lock, create_timestamp,
//          |   last_mesg_timestamp, num_polls, poll_interval, protocol_status, gatewayurl)
//          |VALUES (?, ?, 'empty', 'N', SYSTIMESTAMP, SYSTIMESTAMP, 0, 0, 'initial', 'http://chris.example')""".stripMargin
//      )
//      try {
//        ps.setString(1, storn)
//        ps.setString(2, formResultId)
//        ps.executeUpdate()
//      } finally ps.close()
//    }
//
//  private def selectGovTalkRow(): Map[String, String] =
//    db.withConnection { conn =>
//      val ps = conn.prepareStatement(
//        "SELECT correlationid, poll_interval, gatewayurl FROM govtalk_status WHERE user_identifier = ? AND formresultid = ?"
//      )
//      try {
//        ps.setString(1, storn)
//        ps.setString(2, formResultId)
//        val rs = ps.executeQuery()
//        rs.next()
//        Map(
//          "CORRELATIONID" -> rs.getString("correlationid"),
//          "POLL_INTERVAL" -> rs.getString("poll_interval"),
//          "GATEWAYURL"    -> rs.getString("gatewayurl")
//        )
//      } finally ps.close()
//    }
//
//  private def cleanupGovTalkRow(): Unit =
//    db.withConnection { conn =>
//      val ps = conn.prepareStatement("DELETE FROM govtalk_status WHERE user_identifier = ? AND formresultid = ?")
//      try {
//        ps.setString(1, storn)
//        ps.setString(2, formResultId)
//        ps.executeUpdate()
//      } finally ps.close()
//    }
//}
