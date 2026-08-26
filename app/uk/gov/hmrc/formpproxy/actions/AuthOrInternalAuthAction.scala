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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, Predicate}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthOrInternalAuthAction @Inject() (
  override val authConnector: AuthConnector,
  auth: BackendAuthComponents,
  bodyParsers: BodyParsers.Default
)(implicit ec: ExecutionContext)
    extends AuthorisedFunctions
    with Logging {

  def apply(permission: Predicate.Permission): ActionBuilder[Request, AnyContent] =
    new ActionBuilder[Request, AnyContent] {

      override def parser: BodyParser[AnyContent] = bodyParsers

      override protected def executionContext: ExecutionContext = ec

      override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

        if (hc.sessionId.isDefined)
          authorised()(block(request)).recoverWith { case ae: AuthorisationException =>
            logger.warn(s"[Auth] session rejected, falling back to internal-auth ${ae.reason}")
            internalAuth(permission, request, block)
          }
        else
          internalAuth(permission, request, block)
      }
    }

  private def internalAuth[A](
    permission: Predicate.Permission,
    request: Request[A],
    block: Request[A] => Future[Result]
  ): Future[Result] =
    auth.authorizedAction(permission).invokeBlock(request, block)
}
