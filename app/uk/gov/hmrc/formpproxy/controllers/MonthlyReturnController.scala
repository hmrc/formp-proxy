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

package uk.gov.hmrc.formpproxy.controllers

import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.models.UserMonthlyReturns
import uk.gov.hmrc.formpproxy.models.requests.InstanceIdRequest
import uk.gov.hmrc.formpproxy.services.MonthlyReturnService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MonthlyReturnController @Inject()(
                                         authorise: AuthAction,
                                         service: MonthlyReturnService,
                                         cc: ControllerComponents
                                       )(implicit ec: ExecutionContext)
  extends BackendController(cc) with Logging {

  def retrieveMonthlyReturns: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body.validate[InstanceIdRequest].fold(
        errs =>
          Future.successful(
            BadRequest(Json.obj(
              "message" -> "Invalid JSON body",
              "errors"  -> JsError.toJson(errs)
            ))
          ),
        instanceIdRequest =>
          service
            .getAllMonthlyReturns(instanceIdRequest.instanceId)
            .map { (payload: UserMonthlyReturns) => Ok(Json.toJson(payload)) }
            .recover {
              case u: UpstreamErrorResponse =>
                Status(u.statusCode)(Json.obj("message" -> u.message))
              case t: Throwable =>
                logger.error("[retrieveMonthlyReturns] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
    }

  def createMonthlyReturn: Action[JsValue] = authorise.async(parse.json) { implicit request =>
    val body = request.body
    val instanceId = (body \ "instanceId").as[String]
    val taxYear    = (body \ "taxYear").as[Int]
    val taxMonth   = (body \ "taxMonth").as[Int]
    val nil        = (body \ "nilReturnIndicator").as[String]
    service.createMonthlyReturn(instanceId, taxYear, taxMonth, nil)
      .map(_ => NoContent)
  }

  def updateSchemeVersion: Action[JsValue] = authorise.async(parse.json) { implicit request =>
    val instanceId = (request.body \ "instanceId").as[String]
    val version    = (request.body \ "version").as[Int]
    service.updateSchemeVersion(instanceId, version).map(v => Ok(Json.obj("version" -> v)))
  }

  def updateMonthlyReturn: Action[JsValue] = authorise.async(parse.json) { implicit request =>
    val b = request.body
    def optStr(name: String): Option[String] = (b \ name).asOpt[String]
    service.updateMonthlyReturn(
      instanceId = (b \ "instanceId").as[String],
      taxYear    = (b \ "taxYear").as[Int],
      taxMonth   = (b \ "taxMonth").as[Int],
      amendment  = (b \ "amendment").as[String],
      decEmpStatusConsidered = optStr("decEmpStatusConsidered"),
      decAllSubsVerified     = optStr("decAllSubsVerified"),
      decInformationCorrect  = optStr("decInformationCorrect"),
      decNoMoreSubPayments   = optStr("decNoMoreSubPayments"),
      decNilReturnNoPayments = optStr("decNilReturnNoPayments"),
      nilReturnIndicator     = (b \ "nilReturnIndicator").as[String],
      status                 = (b \ "status").as[String],
      version                = (b \ "version").as[Int]
    ).map(v => Ok(Json.obj("version" -> v)))
  }
}