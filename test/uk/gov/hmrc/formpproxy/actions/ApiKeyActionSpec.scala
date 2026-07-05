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

package uk.gov.hmrc.formpproxy.actions

import play.api.Configuration
import play.api.mvc.{BodyParsers, Request, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.base.SpecBase

import scala.concurrent.Future

class ApiKeyActionSpec extends SpecBase {

  private val expectedApiKey = "test-api-key"

  private val action: ApiKeyAction =
    new DefaultApiKeyAction(
      Configuration("internal-service-api-key" -> expectedApiKey),
      new BodyParsers.Default(cc.parsers)
    )

  private def invoke[A](request: Request[A]) =
    action.invokeBlock(request, _ => Future.successful(Results.Ok))

  "ApiKeyAction" - {

    "allow the request through when the X-API-Key header matches the configured key" in {
      status(invoke(FakeRequest().withHeaders("X-API-Key" -> expectedApiKey))) mustBe OK
    }

    "return 401 Unauthorized when the X-API-Key header does not match the configured key" in {
      status(invoke(FakeRequest().withHeaders("X-API-Key" -> "wrong-key"))) mustBe UNAUTHORIZED
    }

    "return 401 Unauthorized when the X-API-Key header is missing" in {
      status(invoke(FakeRequest())) mustBe UNAUTHORIZED
    }
  }
}
