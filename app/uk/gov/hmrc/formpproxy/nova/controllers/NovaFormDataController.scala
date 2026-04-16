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

package uk.gov.hmrc.formpproxy.nova.controllers

import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.nova.models.{NotificationRefsRequest, StoreFormDataRequest}
import uk.gov.hmrc.formpproxy.nova.repositories.{FormAlreadyUpdatedException, FormNotFoundException}
import uk.gov.hmrc.formpproxy.nova.services.NovaFormDataService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NovaFormDataController @Inject() (
  authorise: AuthAction,
  service: NovaFormDataService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private val SafeFormDataIdPattern = """^[a-zA-Z0-9/\-_%]+$""".r

  def getFormData(formId: Long, formDataIds: Seq[String]): Action[AnyContent] =
    authorise.async { implicit request =>
      if (formDataIds.isEmpty)
        Future.successful(BadRequest(Json.obj("message" -> "formDataIds must not be empty")))
      else if (formDataIds.exists(id => SafeFormDataIdPattern.findFirstIn(id).isEmpty))
        Future.successful(BadRequest(Json.obj("message" -> "formDataIds contain invalid characters")))
      else
        service
          .getFormData(formId, formDataIds)
          .map {
            case Some(data) => Ok(Json.toJson(data))
            case None       =>
              NotFound(Json.obj("code" -> "FORM_NOT_FOUND", "message" -> s"No form found with id $formId"))
          }
          .recover { case t: Throwable =>
            logger.error("[getFormData] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
          }
    }

  def storeFormData(formId: Long, formDataId: String): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[StoreFormDataRequest]
        .fold(
          _ => Future.successful(BadRequest(Json.obj("message" -> "Invalid request body"))),
          body =>
            service
              .storeFormData(formId, formDataId, body)
              .map(response => Ok(Json.toJson(response)))
              .recover(handleFormErrors("storeFormData"))
        )
    }

  def getNovaNotificationRef(formId: Long): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[NotificationRefsRequest]
        .fold(
          _ => Future.successful(BadRequest(Json.obj("message" -> "Invalid request body"))),
          body =>
            if (body.requestRefNumCount < 1)
              Future.successful(BadRequest(Json.obj("message" -> "requestRefNumCount must be at least 1")))
            else
              service
                .getNovaNotificationRef(formId, body)
                .map(response => Ok(Json.toJson(response)))
                .recover(handleFormErrors("getNovaNotificationRef"))
        )
    }

  private def handleFormErrors(operation: String): PartialFunction[Throwable, play.api.mvc.Result] = {
    case _: FormNotFoundException       =>
      NotFound(Json.obj("code" -> "FORM_NOT_FOUND", "message" -> "Form not found"))
    case _: FormAlreadyUpdatedException =>
      Conflict(Json.obj("code" -> "FORM_ALREADY_UPDATED", "message" -> "Form already updated by another session"))
    case t: Throwable                   =>
      logger.error(s"[$operation] failed", t)
      InternalServerError(Json.obj("message" -> "Unexpected error"))
  }
}
