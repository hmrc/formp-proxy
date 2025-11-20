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

package uk.gov.hmrc.formpproxy.sdlt.controllers.returns

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

import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.sdlt.models.*
import uk.gov.hmrc.formpproxy.sdlt.models.vendor.*
import uk.gov.hmrc.formpproxy.sdlt.services.ReturnService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VendorReturnsController @Inject() (
  authorise: AuthAction,
  service: ReturnService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def createVendor(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[CreateVendorRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .createVendor(body)
              .map { CreateVendorReturn =>
                Created(Json.toJson(CreateVendorReturn))
              }
              .recover { case t =>
                logger.error("[createVendor] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def updateVendor(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateVendorRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .updateVendor(body)
              .map { UpdateVendorReturn =>
                Ok(Json.toJson(UpdateVendorReturn))
              }
              .recover { case t =>
                logger.error("[updateVendor] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def deleteVendor(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[DeleteVendorRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .deleteVendor(body)
              .map { DeleteVendorReturn =>
                Ok(Json.toJson(DeleteVendorReturn))
              }
              .recover { case t =>
                logger.error("[deleteVendor] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

}
