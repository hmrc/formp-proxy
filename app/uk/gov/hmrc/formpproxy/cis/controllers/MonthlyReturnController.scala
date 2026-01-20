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

package uk.gov.hmrc.formpproxy.cis.controllers

import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.cis.models.{UnsubmittedMonthlyReturns, UserMonthlyReturns}
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateNilMonthlyReturnRequest, InstanceIdRequest}
import uk.gov.hmrc.formpproxy.cis.services.MonthlyReturnService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class MonthlyReturnController @Inject() (
  authorise: AuthAction,
  service: MonthlyReturnService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def retrieveMonthlyReturns: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[InstanceIdRequest]
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
          instanceIdRequest =>
            service
              .getAllMonthlyReturns(instanceIdRequest.instanceId)
              .map((payload: UserMonthlyReturns) => Ok(Json.toJson(payload)))
              .recover {
                case u: UpstreamErrorResponse =>
                  Status(u.statusCode)(Json.obj("message" -> u.message))
                case t: Throwable             =>
                  logger.error("[retrieveMonthlyReturns] failed", t)
                  InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def retrieveUnsubmittedMonthlyReturns: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[InstanceIdRequest]
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
          instanceIdRequest =>
            service
              .getUnsubmittedMonthlyReturns(instanceIdRequest.instanceId)
              .map((payload: UnsubmittedMonthlyReturns) => Ok(Json.toJson(payload)))
              .recover { case NonFatal(e) =>
                logger.error("[retrieveUnsubmittedMonthlyReturns] failed", e)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def createNilMonthlyReturn: Action[CreateNilMonthlyReturnRequest] =
    authorise.async(parse.json[CreateNilMonthlyReturnRequest]) { implicit request =>
      service
        .createNilMonthlyReturn(request.body)
        .map(result => Created(Json.toJson(result)))
        .recover {
          case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
          case t: Throwable             =>
            logger.error("[createNilMonthlyReturn] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def getSchemeEmail: Action[InstanceIdRequest] =
    authorise.async(parse.json[InstanceIdRequest]) { implicit request =>
      service
        .getSchemeEmail(request.body.instanceId)
        .map(email => Ok(Json.obj("email" -> email)))
        .recover {
          case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
          case t: Throwable             =>
            logger.error("[getSchemeEmail] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }
}
