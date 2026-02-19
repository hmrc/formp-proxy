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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.cis.models.{UnsubmittedMonthlyReturns, UserMonthlyReturns}
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.services.MonthlyReturnService
import uk.gov.hmrc.formpproxy.cis.utils.JsResultUtils.*
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext
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
        .foldErrorsIntoBadRequest { req =>
          service
            .getAllMonthlyReturns(req.instanceId)
            .map(payload => Ok(Json.toJson(payload)))
            .recover { case NonFatal(e) =>
              logger.error("[retrieveMonthlyReturns] failed", e)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
        }
    }

  def retrieveUnsubmittedMonthlyReturns: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[InstanceIdRequest]
        .foldErrorsIntoBadRequest { req =>
          service
            .getUnsubmittedMonthlyReturns(req.instanceId)
            .map(payload => Ok(Json.toJson(payload)))
            .recover { case NonFatal(e) =>
              logger.error("[retrieveUnsubmittedMonthlyReturns] failed", e)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
        }
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

  def updateNilMonthlyReturn: Action[CreateNilMonthlyReturnRequest] =
    authorise.async(parse.json[CreateNilMonthlyReturnRequest]) { implicit request =>
      service
        .updateNilMonthlyReturn(request.body)
        .map(_ => NoContent)
        .recover {
          case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
          case t: Throwable             =>
            logger.error("[updateNilMonthlyReturn] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def updateMonthlyReturnItem(): Action[UpdateMonthlyReturnItemRequest] =
    authorise.async(parse.json[UpdateMonthlyReturnItemRequest]) { implicit request =>
      service
        .updateMonthlyReturnItem(request.body) //
        .map(_ => NoContent)
        .recover {
          case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
          case t: Throwable             =>
            logger.error("[updateMonthlyReturnItem] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def createMonthlyReturn: Action[CreateMonthlyReturnRequest] =
    authorise.async(parse.json[CreateMonthlyReturnRequest]) { implicit request =>
      service
        .createMonthlyReturn(request.body)
        .map(_ => Created)
        .recover {
          case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
          case NonFatal(t)              =>
            logger.error("[createMonthlyReturn] failed", t)
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

  def getMonthlyReturnForEdit: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[GetMonthlyReturnForEditRequest]
        .foldErrorsIntoBadRequest { req =>
          service
            .getMonthlyReturnForEdit(req)
            .map(payload => Ok(Json.toJson(payload)))
            .recover { case NonFatal(e) =>
              logger.error("[retrieveUnsubmittedMonthlyReturns] failed", e)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
        }
    }

  def syncMonthlyReturnItems: Action[SyncMonthlyReturnItemsRequest] =
    authorise.async(parse.json[SyncMonthlyReturnItemsRequest]) { implicit request =>
      service
        .syncMonthlyReturnItems(request.body)
        .map(_ => NoContent)
        .recover { case NonFatal(e) =>
          logger.error("[syncMonthlyReturnItems] failed", e)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def deleteMonthlyReturnItem: Action[DeleteMonthlyReturnItemRequest] =
    authorise.async(parse.json[DeleteMonthlyReturnItemRequest]) { implicit request =>
      service
        .deleteMonthlyReturnItem(request.body)
        .map(_ => NoContent)
        .recover { case NonFatal(e) =>
          logger.error("[deleteMonthlyReturnItem] failed", e)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

}
