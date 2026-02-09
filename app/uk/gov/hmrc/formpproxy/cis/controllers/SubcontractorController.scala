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
import uk.gov.hmrc.formpproxy.cis.models.GetSubcontractorList
import uk.gov.hmrc.formpproxy.cis.services.SubcontractorService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.formpproxy.cis.models.requests.CreateAndUpdateSubcontractorRequest

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubcontractorController @Inject() (
  authorise: AuthAction,
  service: SubcontractorService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def createAndUpdateSubcontractor(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[CreateAndUpdateSubcontractorRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .createAndUpdateSubcontractor(body)
              .map(_ => NoContent)
              .recover { case t =>
                logger.error("[updateSubcontractor] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def getSubcontractorList(cisId: String): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getSubcontractorList(GetSubcontractorList(cisId))
        .map(res => Ok(Json.toJson(res)))
        .recover { case t =>
          logger.error(s"[getSubcontractorList] failed (cisId=$cisId)", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }
}
