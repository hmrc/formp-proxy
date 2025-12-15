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

package uk.gov.hmrc.formpproxy.charities.controllers

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.charities.models.SaveUnregulatedDonationRequest
import uk.gov.hmrc.formpproxy.charities.services.UnregulatedDonationsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UnregulatedDonationsController @Inject() (
  authorise: AuthAction,
  service: UnregulatedDonationsService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getTotalUnregulatedDonations(charityReference: String): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getTotalUnregulatedDonations(charityReference)
        .map {
          case Some(total) => Ok(Json.obj("unregulatedDonationsTotal" -> total))
          case None        => NotFound(Json.obj("message" -> "No unregulated donations found"))
        }
        .recover { case t: Throwable =>
          logger.error("[getTotalUnregulatedDonations] failed", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def saveUnregulatedDonation(charityReference: String): Action[SaveUnregulatedDonationRequest] =
    authorise.async(parse.json[SaveUnregulatedDonationRequest]) { implicit request =>
      service
        .saveUnregulatedDonation(charityReference, request.body)
        .map { _ =>
          Ok(Json.obj("success" -> true))
        }
        .recover { case t: Throwable =>
          logger.error("[saveUnregulatedDonation] failed", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

}
