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

package uk.gov.hmrc.formpproxy.sdlt.controllers.agents

import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.sdlt.models.agents.UpdatePredefinedAgentRequest
import uk.gov.hmrc.formpproxy.sdlt.services.agents.UpdatePredefinedAgentService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdatePredefinedAgentController @Inject()(
                                                 authorise: AuthAction,
                                                 service: UpdatePredefinedAgentService,
                                                 cc: ControllerComponents
                                               )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with Logging {

  def updatePredefinedAgent(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdatePredefinedAgentRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .updatePredefinedAgent(body)
              .map { updatePredefinedAgentResponse =>
                Ok(Json.toJson(updatePredefinedAgentResponse))
              }
              .recover { case t =>
                logger.error("[updatePredefinedAgent] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

}
