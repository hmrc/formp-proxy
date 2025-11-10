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

package uk.gov.hmrc.formpproxy.sdlt.controllers.returns

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

import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.sdlt.models._
import uk.gov.hmrc.formpproxy.sdlt.services.ReturnService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnsController @Inject() (
  authorise: AuthAction,
  service: ReturnService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def createSDLTReturn(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[CreateReturnRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .createSDLTReturn(body)
              .map { returnRefId =>
                Created(Json.obj("returnResourceRef" -> returnRefId))
              }
              .recover { case t =>
                logger.error("[createSubmission] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def getSDLTReturn(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[GetReturnByRefRequest]
        .fold(
          errs =>
            Future.successful(
              BadRequest(
                Json.obj(
                  "message" -> "Invalid JSON body",
                  "errors"  -> JsError.toJson(errs)
                )
              )
            ),
          getReturnRequest =>
            service
              .getSDLTReturn(getReturnRequest.returnResourceRef, getReturnRequest.storn)
              .map((payload: GetReturnRequest) => Ok(Json.toJson(payload)))
              .recover {
                case u: UpstreamErrorResponse =>
                  Status(u.statusCode)(Json.obj("message" -> u.message))
                case t: Throwable             =>
                  logger.error("[getSDLTReturn] failed", t)
                  InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

}
