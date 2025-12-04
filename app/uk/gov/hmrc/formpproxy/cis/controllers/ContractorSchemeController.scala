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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.cis.models.UserMonthlyReturns
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateNilMonthlyReturnRequest, InstanceIdRequest}
import uk.gov.hmrc.formpproxy.cis.services.{ContractorSchemeService, MonthlyReturnService}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContractorSchemeController @Inject() (
  authorise: AuthAction,
  service: ContractorSchemeService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getScheme(instanceId: String): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getScheme(instanceId)
        .map {
          case Some(scheme) => Ok(Json.toJson(scheme))
          case None         => NotFound(Json.obj("message" -> "Scheme not found"))
        }
        .recover {
          case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
          case t: Throwable             =>
            logger.error("[getScheme] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }
}
