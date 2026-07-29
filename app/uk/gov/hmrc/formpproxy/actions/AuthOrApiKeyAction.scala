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
import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait AuthOrApiKeyAction extends ActionBuilder[Request, AnyContent]

class DefaultAuthOrApiKeyAction @Inject() (
  override val authConnector: AuthConnector,
  configuration: Configuration,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends AuthOrApiKeyAction
    with AuthorisedFunctions
    with Logging {

  private val expectedApiKey: String = configuration.get[String]("internal-service-api-key")

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    if (hc.sessionId.isDefined)
      authorised()(block(request)).recoverWith { case ae: AuthorisationException =>
        logger.warn(s"[Auth] Authorisation Exception ${ae.reason}")
        apiKey(request, block)
      }
    else
      apiKey(request, block)
  }

  private def apiKey[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] =
    request.headers.get("X-API-Key") match {
      case Some(key) if key == expectedApiKey => block(request)
      case _                                  => Future.successful(Results.Unauthorized)
    }
}
