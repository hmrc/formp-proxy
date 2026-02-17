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
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.services.GovTalkService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GovTalkController @Inject() (
  authorise: AuthAction,
  service: GovTalkService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getGovTalkStatus: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[GetGovTalkStatusRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .getGovTalkStatus(body)
              .map(response => Ok(Json.toJson(response)))
              .recover { case t =>
                logger.error("[getGovTalkStatus] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def resetGovTalkStatus: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[ResetGovTalkStatusRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .resetGovTalkStatus(body)
              .map(_ => NoContent)
              .recover { case t =>
                logger.error("[getGovTalkStatus] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def updateGovTalkStatus: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateGovTalkStatusRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .updateGovTalkStatus(body)
              .map(_ => NoContent)
              .recover { case t =>
                logger.error("[updateGovTalkStatus] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def createGovTalkStatusRecord: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[CreateGovTalkStatusRecordRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .createGovTalkStatusRecord(body)
              .map(_ => Created)
              .recover { case t =>
                logger.error("[createGovTalkStatusRecord] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }
}
