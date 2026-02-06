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

package uk.gov.hmrc.formpproxy.cis.models.response

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.formpproxy.cis.models.GovTalkStatusRecord

import java.time.LocalDateTime

class GetGovTalkStatusResponseSpec extends AnyWordSpec with Matchers {

  "GetGovTalkStatusResponse JSON format" should {

    "serialize and deserialize correctly" in {
      val model = GetGovTalkStatusResponse(govtallk_status =
        Seq(
          GovTalkStatusRecord(
            userIdentifier = "1",
            formResultID = "12890",
            correlationID = "C742D5DEE7EB4D15B4F7EFD50B890525",
            formLock = "N",
            createDate = None,
            endStateDate = Some(LocalDateTime.parse("2026-02-05T00:00:00")),
            lastMessageDate = LocalDateTime.parse("2025-02-05T00:00:00"),
            numPolls = 0,
            pollInterval = 0,
            protocolStatus = "dataRequest",
            gatewayURL = "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/sync/VATDEC"
          )
        )
      )

      val json = Json.toJson(model)
      json.as[GetGovTalkStatusResponse] mustBe model
    }
  }
}
