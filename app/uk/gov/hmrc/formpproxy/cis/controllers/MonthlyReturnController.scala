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
import uk.gov.hmrc.formpproxy.actions.AuthOrInternalAuthAction
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.services.MonthlyReturnService
import uk.gov.hmrc.formpproxy.cis.utils.JsResultUtils.*
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client.{IAAction, Predicate, Resource}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class MonthlyReturnController @Inject() (
  authOrInternalAuth: AuthOrInternalAuthAction,
  service: MonthlyReturnService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private val monthlyReturns       = Resource.from("formp-proxy", "formp-proxy/cis/monthly-returns")
  private val readMonthlyReturns   = authOrInternalAuth(Predicate.Permission(monthlyReturns, IAAction("READ")))
  private val writeMonthlyReturns  = authOrInternalAuth(Predicate.Permission(monthlyReturns, IAAction("WRITE")))
  private val deleteMonthlyReturns = authOrInternalAuth(Predicate.Permission(monthlyReturns, IAAction("DELETE")))

  def retrieveMonthlyReturns: Action[JsValue] =
    readMonthlyReturns.async(parse.json) { implicit request =>
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
    readMonthlyReturns.async(parse.json) { implicit request =>
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

  def retrieveSubmittedMonthlyReturns: Action[JsValue] =
    readMonthlyReturns.async(parse.json) { implicit request =>
      request.body
        .validate[InstanceIdRequest]
        .foldErrorsIntoBadRequest { req =>
          service
            .getSubmittedMonthlyReturns(req.instanceId)
            .map(payload => Ok(Json.toJson(payload)))
            .recover { case NonFatal(e) =>
              logger.error("[retrieveUnsubmittedMonthlyReturns] failed", e)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
        }
    }

  def createNilMonthlyReturn: Action[CreateNilMonthlyReturnRequest] =
    writeMonthlyReturns.async(parse.json[CreateNilMonthlyReturnRequest]) { implicit request =>
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

  def updateMonthlyReturn: Action[UpdateMonthlyReturnRequest] =
    writeMonthlyReturns.async(parse.json[UpdateMonthlyReturnRequest]) { implicit request =>
      service
        .updateMonthlyReturn(request.body)
        .map(_ => NoContent)
        .recover {
          case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
          case NonFatal(t)              =>
            logger.error("[updateMonthlyReturn] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def updateMonthlyReturnItem(): Action[UpdateMonthlyReturnItemRequest] =
    writeMonthlyReturns.async(parse.json[UpdateMonthlyReturnItemRequest]) { implicit request =>
      service
        .updateMonthlyReturnItem(request.body)
        .map(_ => NoContent)
        .recover {
          case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
          case t: Throwable             =>
            logger.error("[updateMonthlyReturnItem] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def createMonthlyReturn: Action[CreateMonthlyReturnRequest] =
    writeMonthlyReturns.async(parse.json[CreateMonthlyReturnRequest]) { implicit request =>
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
    readMonthlyReturns.async(parse.json[InstanceIdRequest]) { implicit request =>
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
    readMonthlyReturns.async(parse.json) { implicit request =>
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
    writeMonthlyReturns.async(parse.json[SyncMonthlyReturnItemsRequest]) { implicit request =>
      service
        .syncMonthlyReturnItems(request.body)
        .map(_ => NoContent)
        .recover { case NonFatal(e) =>
          logger.error("[syncMonthlyReturnItems] failed", e)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def deleteMonthlyReturnItem: Action[DeleteMonthlyReturnItemRequest] =
    deleteMonthlyReturns.async(parse.json[DeleteMonthlyReturnItemRequest]) { implicit request =>
      service
        .deleteMonthlyReturnItem(request.body)
        .map(_ => NoContent)
        .recover { case NonFatal(e) =>
          logger.error("[deleteMonthlyReturnItem] failed", e)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def deleteUnsubmittedMonthlyReturn: Action[DeleteUnsubmittedMonthlyReturnRequest] =
    deleteMonthlyReturns.async(parse.json[DeleteUnsubmittedMonthlyReturnRequest]) { implicit request =>
      service
        .deleteUnsubmittedMonthlyReturn(request.body)
        .map(_ => NoContent)
        .recover { case NonFatal(e) =>
          logger.error("[deleteUnsubmittedMonthlyReturn] failed", e)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def getMonthlyReturnComplete: Action[JsValue] =
    readMonthlyReturns.async(parse.json) { implicit request =>
      request.body
        .validate[GetMonthlyReturnCompleteRequest]
        .foldErrorsIntoBadRequest { req =>
          service
            .getMonthlyReturnComplete(req)
            .map(payload => Ok(Json.toJson(payload)))
            .recover { case NonFatal(e) =>
              logger.error("[getMonthlyReturnComplete] failed", e)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
        }
    }

  def retrieveSubmittedMonthlyReturnsData: Action[JsValue] =
    readMonthlyReturns.async(parse.json) { implicit request =>
      request.body
        .validate[GetSubmittedMonthlyReturnsDataRequest]
        .foldErrorsIntoBadRequest { request =>
          service
            .getSubmittedMonthlyReturnsData(request)
            .map(payload => Ok(Json.toJson(payload)))
            .recover { case NonFatal(e) =>
              logger.error("[retrieveSubmittedMonthlyReturnsData] failed", e)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
        }
    }
}
