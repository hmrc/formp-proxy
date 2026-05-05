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

package uk.gov.hmrc.formpproxy.cis.controllers

import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.cis.services.VerificationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.formpproxy.cis.models.requests.CreateVerificationBatchAndVerificationsRequest

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VerificationController @Inject() (
  authorise: AuthAction,
  service: VerificationService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getNewestVerificationBatch(instanceId: String): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getNewestVerificationBatch(instanceId)
        .map(res => Ok(Json.toJson(res)))
        .recover { case t =>
          logger.error(s"[getNewestVerificationBatch] failed (instanceId=$instanceId)", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def getCurrentVerificationBatch(instanceId: String): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getCurrentVerificationBatch(instanceId)
        .map(res => Ok(Json.toJson(res)))
        .recover { case t =>
          logger.error(s"[getCurrentVerificationBatch] failed (instanceId=$instanceId)", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def createVerificationBatchAndVerifications(): Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body
        .validate[CreateVerificationBatchAndVerificationsRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          req =>
            service
              .createVerificationBatchAndVerifications(req)
              .map(res => Ok(Json.toJson(res)))
              .recover { case t =>
                logger.error("[createVerificationBatchAndVerifications] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }
}
