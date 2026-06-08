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

package uk.gov.hmrc.formpproxy.cis.utils

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.formpproxy.cis.models.GovTalkErrorStatus.*
import uk.gov.hmrc.formpproxy.cis.models.GovTalkErrorValues

class GovTalkErrorMapperSpec extends AnyWordSpec with Matchers {

  "GovTalkErrorMapper (F20b - Update Submission GovTalk)" should {

    "map a Recoverable error to systemError with the ChRIS error code and text" when {

      List("3000", "2005", "1000").foreach { code =>
        s"the ChRIS response code is $code" in {
          val text   = s"recoverable error $code"
          val result = GovTalkErrorMapper(RecoverableError(code, text))

          result mustBe GovTalkErrorValues(
            errorCode = Some(code),
            errorType = Some("systemError"),
            errorMessage = Some(text)
          )
        }
      }
    }

    "map a Fatal error to systemError with the fatal error code and text" in {
      val result = GovTalkErrorMapper(FatalError("9999", "fatal error text"))

      result mustBe GovTalkErrorValues(
        errorCode = Some("9999"),
        errorType = Some("systemError"),
        errorMessage = Some("fatal error text")
      )
    }

    "map a Departmental error to code 3001 and departmentalError with the ChRIS text" in {
      val result = GovTalkErrorMapper(DepartmentalError("departmental failure"))

      result mustBe GovTalkErrorValues(
        errorCode = Some("3001"),
        errorType = Some("departmentalError"),
        errorMessage = Some("departmental failure")
      )
    }

    "map Server errors (HTTP 500-505) to the HTTP status with timeOut type and message" when {

      (500 to 505).foreach { status =>
        s"the HTTP response code is $status" in {
          val result = GovTalkErrorMapper(ServerError(status))

          result mustBe GovTalkErrorValues(
            errorCode = Some(status.toString),
            errorType = Some("timeOut"),
            errorMessage = Some("timeOut")
          )
        }
      }
    }

    "leave the GOV Talk columns NULL for non-5xx HTTP statuses passed as ServerError" in {
      List(199, 200, 301, 400, 404, 499, 506, 599).foreach { status =>
        GovTalkErrorMapper(ServerError(status)) mustBe GovTalkErrorValues.empty
      }
    }

    "map No response (connection refused / I/O exception) to 'xxxx' with timeOut type and 'timed out' message" in {
      val result = GovTalkErrorMapper(NoResponse)

      result mustBe GovTalkErrorValues(
        errorCode = Some("xxxx"),
        errorType = Some("timeOut"),
        errorMessage = Some("timed out")
      )
    }

    "leave all GOV Talk columns NULL for any other status" in {
      GovTalkErrorMapper(OtherStatus) mustBe GovTalkErrorValues.empty
    }
  }
}
