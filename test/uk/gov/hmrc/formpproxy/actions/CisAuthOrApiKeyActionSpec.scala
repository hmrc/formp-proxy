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

package uk.gov.hmrc.formpproxy.actions

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verifyNoInteractions, when}
import play.api.mvc.{BodyParsers, Request, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments}
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.config.AppConfig

import scala.concurrent.Future

class CisAuthOrApiKeyActionSpec extends SpecBase {

  private val expectedApiKey = "test-cis-api-key"

  private def action(
    authConnector: AuthConnector,
    appConfig: AppConfig
  ): CisAuthOrApiKeyAction =
    new DefaultCisAuthOrApiKeyAction(
      authConnector = authConnector,
      appConfig = appConfig,
      parser = new BodyParsers.Default(cc.parsers)
    )

  private def invoke[A](
    authConnector: AuthConnector,
    appConfig: AppConfig,
    request: Request[A]
  ): Future[Result] =
    action(authConnector, appConfig)
      .invokeBlock(request, _ => Future.successful(Results.Ok))

  private def mockAppConfig: AppConfig = {
    val appConfig = mock[AppConfig]

    when(appConfig.cisInternalServiceApiKey)
      .thenReturn(expectedApiKey)

    appConfig
  }

  private def authConnectorReturning(
    result: Future[Unit]
  ): AuthConnector = {
    val connector = mock[AuthConnector]

    when(
      connector.authorise[Unit](
        any(),
        any()
      )(any(), any())
    ).thenReturn(result)

    connector
  }

  "DefaultCisAuthOrApiKeyAction" - {

    "allow the request when a session is present and authorisation succeeds" in {
      val connector = authConnectorReturning(Future.successful(()))
      val appConfig = mockAppConfig

      val request =
        FakeRequest()
          .withHeaders("X-Session-ID" -> "session-123")

      status(invoke(connector, appConfig, request)) mustBe OK
    }

    "fall back to the X-API-Key header when a session is present but authorisation fails" in {
      val connector =
        authConnectorReturning(
          Future.failed(InsufficientEnrolments())
        )

      val appConfig = mockAppConfig

      val request =
        FakeRequest()
          .withHeaders(
            "X-Session-ID" -> "session-123",
            "X-API-Key"    -> expectedApiKey
          )

      status(invoke(connector, appConfig, request)) mustBe OK
    }

    "return Unauthorized when authorisation fails and the X-API-Key header does not match" in {
      val connector =
        authConnectorReturning(
          Future.failed(InsufficientEnrolments())
        )

      val appConfig = mockAppConfig

      val request =
        FakeRequest()
          .withHeaders(
            "X-Session-ID" -> "session-123",
            "X-API-Key"    -> "wrong-key"
          )

      status(invoke(connector, appConfig, request)) mustBe UNAUTHORIZED
    }

    "return Unauthorized when authorisation fails and the X-API-Key header is missing" in {
      val connector =
        authConnectorReturning(
          Future.failed(InsufficientEnrolments())
        )

      val appConfig = mockAppConfig

      val request =
        FakeRequest()
          .withHeaders("X-Session-ID" -> "session-123")

      status(invoke(connector, appConfig, request)) mustBe UNAUTHORIZED
    }

    "allow the request using the X-API-Key header when no session is present" in {
      val connector = mock[AuthConnector]
      val appConfig = mockAppConfig

      val request =
        FakeRequest()
          .withHeaders("X-API-Key" -> expectedApiKey)

      status(invoke(connector, appConfig, request)) mustBe OK

      verifyNoInteractions(connector)
    }

    "return Unauthorized when no session is present and the X-API-Key header does not match" in {
      val connector = mock[AuthConnector]
      val appConfig = mockAppConfig

      val request =
        FakeRequest()
          .withHeaders("X-API-Key" -> "wrong-key")

      status(invoke(connector, appConfig, request)) mustBe UNAUTHORIZED

      verifyNoInteractions(connector)
    }

    "return Unauthorized when no session is present and the X-API-Key header is missing" in {
      val connector = mock[AuthConnector]
      val appConfig = mockAppConfig

      status(
        invoke(
          connector,
          appConfig,
          FakeRequest()
        )
      ) mustBe UNAUTHORIZED

      verifyNoInteractions(connector)
    }
  }
}
