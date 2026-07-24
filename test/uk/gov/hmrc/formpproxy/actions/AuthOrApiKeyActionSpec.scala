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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.mvc.{BodyParsers, Request, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments}
import uk.gov.hmrc.formpproxy.base.SpecBase

import scala.concurrent.Future

class AuthOrApiKeyActionSpec extends SpecBase {

  private val expectedApiKey = "test-api-key"

  private def action(authConnector: AuthConnector): AuthOrApiKeyAction =
    new DefaultAuthOrApiKeyAction(
      authConnector,
      Configuration("internal-service-api-key" -> expectedApiKey),
      new BodyParsers.Default(cc.parsers)
    )

  private def invoke[A](authConnector: AuthConnector, request: Request[A]) =
    action(authConnector).invokeBlock(request, _ => Future.successful(Results.Ok))

  private def authConnectorReturning(result: Future[Unit]): AuthConnector = {
    val connector = mock[AuthConnector]
    when(connector.authorise[Unit](any(), any())(any(), any())).thenReturn(result)
    connector
  }

  "AuthOrApiKeyAction" - {

    "allow the request through when a session is present and authorisation succeeds" in {
      val connector = authConnectorReturning(Future.successful(()))
      val request   = FakeRequest().withHeaders("X-Session-ID" -> "session-123")

      status(invoke(connector, request)) mustBe OK
    }

    "fall back to the X-API-Key header when a session is present but authorisation fails" in {
      val connector = authConnectorReturning(Future.failed(InsufficientEnrolments()))
      val request   = FakeRequest()
        .withHeaders("X-Session-ID" -> "session-123", "X-API-Key" -> expectedApiKey)

      status(invoke(connector, request)) mustBe OK
    }

    "return 401 Unauthorized when authorisation fails and the X-API-Key header does not match" in {
      val connector = authConnectorReturning(Future.failed(InsufficientEnrolments()))
      val request   = FakeRequest()
        .withHeaders("X-Session-ID" -> "session-123", "X-API-Key" -> "wrong-key")

      status(invoke(connector, request)) mustBe UNAUTHORIZED
    }

    "allow the request through on the X-API-Key header when no session is present" in {
      val connector = mock[AuthConnector]

      status(invoke(connector, FakeRequest().withHeaders("X-API-Key" -> expectedApiKey))) mustBe OK
    }

    "return 401 Unauthorized when no session is present and the X-API-Key header does not match" in {
      val connector = mock[AuthConnector]

      status(invoke(connector, FakeRequest().withHeaders("X-API-Key" -> "wrong-key"))) mustBe UNAUTHORIZED
    }

    "return 401 Unauthorized when no session is present and the X-API-Key header is missing" in {
      val connector = mock[AuthConnector]

      status(invoke(connector, FakeRequest())) mustBe UNAUTHORIZED
    }
  }
}
