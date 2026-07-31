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

import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.formpproxy.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait CisAuthOrApiKeyAction extends ActionBuilder[Request, AnyContent]

@Singleton
class DefaultCisAuthOrApiKeyAction @Inject() (
  override val authConnector: AuthConnector,
  appConfig: AppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends CisAuthOrApiKeyAction
    with AuthorisedFunctions
    with Logging {

  private val ApiKeyHeader = "X-API-Key"

  override def invokeBlock[A](
    request: Request[A],
    block: Request[A] => Future[Result]
  ): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    if (hc.sessionId.isDefined)
      authorised()(block(request))
        .recoverWith { case ae: AuthorisationException =>
          logger.warn(s"[Auth] Authorisation Exception ${ae.reason}")

          apiKey(request, block)
        }
    else
      apiKey(request, block)
  }

  private def apiKey[A](
    request: Request[A],
    block: Request[A] => Future[Result]
  ): Future[Result] =
    request.headers.get(ApiKeyHeader) match {
      case Some(key) if key == appConfig.cisInternalServiceApiKey =>
        block(request)

      case _ =>
        Future.successful(Results.Unauthorized)
    }

}
