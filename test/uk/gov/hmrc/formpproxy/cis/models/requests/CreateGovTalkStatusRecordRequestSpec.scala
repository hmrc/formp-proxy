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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.time.LocalDateTime

class CreateGovTalkStatusRecordRequestSpec extends AnyFreeSpec with Matchers {

  "CreateGovTalkStatusRecordRequest JSON format" - {

    "read from JSON correctly" in {
      val json = Json.obj(
        "userIdentifier"  -> "1",
        "formResultID"    -> "12890",
        "correlationID"   -> "C742D5DEE7EB4D15B4F7EFD50B890525",
        "formLock"        -> "N",
        "createDate"      -> "2026-04-05T00:00:00",
        "endStateDate"    -> "2026-04-05T00:00:00",
        "lastMessageDate" -> "2026-04-05T00:00:00",
        "numPolls"        -> 0,
        "pollInterval"    -> 0,
        "protocolStatus"  -> "dataRequest",
        "gatewayURL"      -> "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/action/VATDEC"
      )

      val result = json.as[CreateGovTalkStatusRecordRequest]

      result mustBe CreateGovTalkStatusRecordRequest(
        userIdentifier = "1",
        formResultID = "12890",
        correlationID = "C742D5DEE7EB4D15B4F7EFD50B890525",
        formLock = "N",
        createDate = Some(LocalDateTime.parse("2026-04-05T00:00:00")),
        endStateDate = Some(LocalDateTime.parse("2026-04-05T00:00:00")),
        lastMessageDate = LocalDateTime.parse("2026-04-05T00:00:00"),
        numPolls = 0,
        pollInterval = 0,
        protocolStatus = "dataRequest",
        gatewayURL = "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/action/VATDEC"
      )
    }

    "write to JSON correctly" in {
      val model = CreateGovTalkStatusRecordRequest(
        userIdentifier = "1",
        formResultID = "12890",
        correlationID = "C742D5DEE7EB4D15B4F7EFD50B890525",
        formLock = "N",
        createDate = Some(LocalDateTime.parse("2026-04-05T00:00:00")),
        endStateDate = Some(LocalDateTime.parse("2026-04-05T00:00:00")),
        lastMessageDate = LocalDateTime.parse("2026-04-05T00:00:00"),
        numPolls = 0,
        pollInterval = 0,
        protocolStatus = "dataRequest",
        gatewayURL = "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/action/VATDEC"
      )

      val json = Json.toJson(model)

      json mustBe Json.obj(
        "userIdentifier"  -> "1",
        "formResultID"    -> "12890",
        "correlationID"   -> "C742D5DEE7EB4D15B4F7EFD50B890525",
        "formLock"        -> "N",
        "createDate"      -> "2026-04-05T00:00:00",
        "endStateDate"    -> "2026-04-05T00:00:00",
        "lastMessageDate" -> "2026-04-05T00:00:00",
        "numPolls"        -> 0,
        "pollInterval"    -> 0,
        "protocolStatus"  -> "dataRequest",
        "gatewayURL"      -> "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/action/VATDEC"
      )
    }
  }
}
