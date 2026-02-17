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

package uk.gov.hmrc.formpproxy.cis.models.requests

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import java.time.LocalDateTime

class UpdateGovTalkStatusStatisticsRequestSpec extends AnyWordSpec with Matchers {

  "UpdateGovTalkStatusStatisticsRequest (JSON)" should {

    "read and write complete request" in {
      val json = Json.parse("""
          |{
          |  "userIdentifier": "123456789",
          |  "formResultID": "SUB123456",
          |  "lastMessageDate": "2026-02-16T10:30:00",
          |  "numPolls": 3,
          |  "pollInterval": 300,
          |  "gatewayURL": "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
          |}
        """.stripMargin)

      val model = json.as[UpdateGovTalkStatusStatisticsRequest]
      model.userIdentifier mustBe "123456789"
      model.formResultID mustBe "SUB123456"
      model.lastMessageDate mustBe LocalDateTime.parse("2026-02-16T10:30:00")
      model.numPolls mustBe 3
      model.pollInterval mustBe 300
      model.gatewayURL mustBe "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"

      Json.toJson(model) mustBe json
    }

    "read and write with zero polls" in {
      val json = Json.parse("""
          |{
          |  "userIdentifier": "123456789",
          |  "formResultID": "SUB123456",
          |  "lastMessageDate": "2026-02-16T10:30:00",
          |  "numPolls": 0,
          |  "pollInterval": 0,
          |  "gatewayURL": "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
          |}
        """.stripMargin)

      val model = json.as[UpdateGovTalkStatusStatisticsRequest]
      model.numPolls mustBe 0
      model.pollInterval mustBe 0

      Json.toJson(model) mustBe json
    }

    "read and write with high poll numbers" in {
      val json = Json.parse("""
          |{
          |  "userIdentifier": "123456789",
          |  "formResultID": "SUB123456",
          |  "lastMessageDate": "2026-02-16T10:30:00",
          |  "numPolls": 100,
          |  "pollInterval": 3600,
          |  "gatewayURL": "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
          |}
        """.stripMargin)

      val model = json.as[UpdateGovTalkStatusStatisticsRequest]
      model.numPolls mustBe 100
      model.pollInterval mustBe 3600
    }

    "fail to read missing userIdentifier" in {
      val json = Json.parse("""
          |{
          |  "formResultID": "SUB123456",
          |  "lastMessageDate": "2026-02-16T10:30:00",
          |  "numPolls": 3,
          |  "pollInterval": 300,
          |  "gatewayURL": "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
          |}
        """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusStatisticsRequest]
      result.isError mustBe true
    }

    "fail to read missing formResultID" in {
      val json = Json.parse("""
          |{
          |  "userIdentifier": "123456789",
          |  "lastMessageDate": "2026-02-16T10:30:00",
          |  "numPolls": 3,
          |  "pollInterval": 300,
          |  "gatewayURL": "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
          |}
        """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusStatisticsRequest]
      result.isError mustBe true
    }

    "fail to read missing lastMessageDate" in {
      val json = Json.parse("""
          |{
          |  "userIdentifier": "123456789",
          |  "formResultID": "SUB123456",
          |  "numPolls": 3,
          |  "pollInterval": 300,
          |  "gatewayURL": "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
          |}
        """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusStatisticsRequest]
      result.isError mustBe true
    }

    "fail to read missing numPolls" in {
      val json = Json.parse("""
          |{
          |  "userIdentifier": "123456789",
          |  "formResultID": "SUB123456",
          |  "lastMessageDate": "2026-02-16T10:30:00",
          |  "pollInterval": 300,
          |  "gatewayURL": "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
          |}
        """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusStatisticsRequest]
      result.isError mustBe true
    }

    "fail to read missing pollInterval" in {
      val json = Json.parse("""
          |{
          |  "userIdentifier": "123456789",
          |  "formResultID": "SUB123456",
          |  "lastMessageDate": "2026-02-16T10:30:00",
          |  "numPolls": 3,
          |  "gatewayURL": "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
          |}
        """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusStatisticsRequest]
      result.isError mustBe true
    }

    "fail to read missing gatewayURL" in {
      val json = Json.parse("""
          |{
          |  "userIdentifier": "123456789",
          |  "formResultID": "SUB123456",
          |  "lastMessageDate": "2026-02-16T10:30:00",
          |  "numPolls": 3,
          |  "pollInterval": 300
          |}
        """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusStatisticsRequest]
      result.isError mustBe true
    }

    "fail to read invalid date format" in {
      val json = Json.parse("""
          |{
          |  "userIdentifier": "123456789",
          |  "formResultID": "SUB123456",
          |  "lastMessageDate": "invalid-date",
          |  "numPolls": 3,
          |  "pollInterval": 300,
          |  "gatewayURL": "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
          |}
        """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusStatisticsRequest]
      result.isError mustBe true
    }

    "fail to read when numPolls is not a number" in {
      val json = Json.parse("""
          |{
          |  "userIdentifier": "123456789",
          |  "formResultID": "SUB123456",
          |  "lastMessageDate": "2026-02-16T10:30:00",
          |  "numPolls": "not-a-number",
          |  "pollInterval": 300,
          |  "gatewayURL": "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
          |}
        """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusStatisticsRequest]
      result.isError mustBe true
    }

    "fail to read when pollInterval is not a number" in {
      val json = Json.parse("""
          |{
          |  "userIdentifier": "123456789",
          |  "formResultID": "SUB123456",
          |  "lastMessageDate": "2026-02-16T10:30:00",
          |  "numPolls": 3,
          |  "pollInterval": "not-a-number",
          |  "gatewayURL": "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
          |}
        """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusStatisticsRequest]
      result.isError mustBe true
    }
  }
}
