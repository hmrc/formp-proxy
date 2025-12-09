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

class UpdateSchemeVersionRequestSpec extends AnyWordSpec with Matchers {

  "UpdateSchemeVersionRequest (JSON)" should {

    "read and write with version" in {
      val json = Json.parse("""
          |{
          |  "version": 5
          |}
        """.stripMargin)

      val model = json.as[UpdateSchemeVersionRequest]
      model.version mustBe 5

      Json.toJson(model) mustBe json
    }

    "read and write with version 0" in {
      val json = Json.parse("""
          |{
          |  "version": 0
          |}
        """.stripMargin)

      val model = json.as[UpdateSchemeVersionRequest]
      model.version mustBe 0

      Json.toJson(model) mustBe json
    }

    "fail to read missing version" in {
      val json = Json.parse("""
          |{
          |}
        """.stripMargin)

      val result = json.validate[UpdateSchemeVersionRequest]
      result.isError mustBe true
    }

    "fail to read version with wrong type" in {
      val json = Json.parse("""
          |{
          |  "version": "not-a-number"
          |}
        """.stripMargin)

      val result = json.validate[UpdateSchemeVersionRequest]
      result.isError mustBe true
    }
  }
}
