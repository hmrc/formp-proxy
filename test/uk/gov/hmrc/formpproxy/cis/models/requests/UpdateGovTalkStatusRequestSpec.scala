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

package uk.gov.hmrc.formpproxy.cis.models.requests

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import java.time.LocalDateTime

class UpdateGovTalkStatusRequestSpec extends AnyWordSpec with Matchers {

  "UpdateGovTalkStatusRequest (JSON)" should {

    "read and write with SoleTrader type" in {
      val json = Json.parse("""
                              |{
                              |  "userIdentifier": "1",
                              |  "formResultID": "12890",
                              |  "endStateDate": "2026-02-03T00:00:00",
                              |  "protocolStatus": "dataRequest"
                              |}
            """.stripMargin)

      val model = json.as[UpdateGovTalkStatusRequest]
      model.userIdentifier mustBe "1"
      model.formResultID mustBe "12890"
      model.endStateDate mustBe LocalDateTime.parse("2026-02-03T00:00:00")
      model.protocolStatus mustBe "dataRequest"

      Json.toJson(model) mustBe json
    }

    "fail to read missing userIdentifier" in {
      val json = Json.parse("""
                              |{
                              |  "formResultID": "12890",
                              |  "endStateDate": "2026-02-03T00:00:00",
                              |  "protocolStatus": "dataRequest"
                              |}
            """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusRequest]
      result.isError mustBe true
    }

    "fail to read missing formResultID" in {
      val json = Json.parse("""
                              |{
                              |  "userIdentifier": "1",
                              |  "endStateDate": "2026-02-03T00:00:00",
                              |  "protocolStatus": "dataRequest"
                              |}
            """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusRequest]
      result.isError mustBe true
    }

    "fail to read missing endStateDate" in {
      val json = Json.parse("""
                              |{
                              |  "userIdentifier": "1",
                              |  "formResultID": "12890",
                              |  "protocolStatus": "dataRequest"
                              |}
            """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusRequest]
      result.isError mustBe true
    }

    "fail to read missing protocolStatus" in {
      val json = Json.parse("""
                              |{
                              |  "userIdentifier": "1",
                              |  "formResultID": "12890",
                              |  "endStateDate": "2026-02-03T00:00:00"
                              |}
            """.stripMargin)

      val result = json.validate[UpdateGovTalkStatusRequest]
      result.isError mustBe true
    }
  }
}
