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
import uk.gov.hmrc.formpproxy.actions.AuthOrInternalAuthAction
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.services.GovTalkService
import uk.gov.hmrc.internalauth.client.{IAAction, Predicate, Resource}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class GovTalkController @Inject() (
  authOrInternalAuth: AuthOrInternalAuthAction,
  service: GovTalkService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private val govtalk      = Resource.from("formp-proxy", "formp-proxy/cis/govtalk")
  private val readGovTalk  = authOrInternalAuth(Predicate.Permission(govtalk, IAAction("READ")))
  private val writeGovTalk = authOrInternalAuth(Predicate.Permission(govtalk, IAAction("WRITE")))

  def getGovTalkStatus: Action[JsValue] =
    readGovTalk.async(parse.json) { implicit request =>
      request.body
        .validate[GetGovTalkStatusRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .getGovTalkStatus(body)
              .map {
                case Some(response) => Ok(Json.toJson(response))
                case None           => NotFound
              }
              .recover { case t =>
                logger.error("[getGovTalkStatus] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def updateGovTalkStatusCorrelationId: Action[UpdateGovTalkStatusCorrelationIdRequest] =
    writeGovTalk.async(parse.json[UpdateGovTalkStatusCorrelationIdRequest]) { implicit request =>
      service
        .updateGovTalkStatusCorrelationId(request.body)
        .map(_ => NoContent)
        .recover { case NonFatal(e) =>
          logger.error("[updateGovTalkStatusCorrelationId] failed", e)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def resetGovTalkStatus: Action[JsValue] =
    writeGovTalk.async(parse.json) { implicit request =>
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
                logger.error("[resetGovTalkStatus] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def updateGovTalkStatus: Action[JsValue] =
    writeGovTalk.async(parse.json) { implicit request =>
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

  def updateGovTalkStatusStatistics: Action[JsValue] =
    writeGovTalk.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateGovTalkStatusStatisticsRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .updateGovTalkStatusStatistics(body)
              .map(_ => NoContent)
              .recover { case t =>
                logger.error("[updateGovTalkStatusStatistics] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def createGovTalkStatusRecord: Action[JsValue] =
    writeGovTalk.async(parse.json) { implicit request =>
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
