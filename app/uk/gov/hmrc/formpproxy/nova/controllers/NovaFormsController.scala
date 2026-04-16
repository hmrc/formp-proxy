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
import uk.gov.hmrc.formpproxy.nova.models.{CreateFormRequest, UpdateFormRequest, UpdateFormStatusRequest}
import uk.gov.hmrc.formpproxy.nova.repositories.{FormAlreadyUpdatedException, FormNotFoundException}
import uk.gov.hmrc.formpproxy.nova.services.NovaFormsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NovaFormsController @Inject() (
  authorise: AuthAction,
  service: NovaFormsService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getForms(userId: String, formType: Option[String], formStatus: Option[String]): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getForms(userId, formType, formStatus)
        .map(response => Ok(Json.toJson(response)))
        .recover { case t: Throwable =>
          logger.error("[getForms] failed", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def getForm(formId: Long): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getForm(formId)
        .map {
          case Some(form) => Ok(Json.toJson(form))
          case None       =>
            NotFound(Json.obj("code" -> "FORM_NOT_FOUND", "message" -> s"No form found with id $formId"))
        }
        .recover { case t: Throwable =>
          logger.error("[getForm] failed", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def createForm(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[CreateFormRequest]
        .fold(
          errors => scala.concurrent.Future.successful(BadRequest(Json.obj("message" -> "Invalid request body"))),
          body =>
            if (body.userId.trim.isEmpty)
              scala.concurrent.Future.successful(BadRequest(Json.obj("message" -> "userId must not be blank")))
            else
              service
                .createForm(body)
                .map(response => Created(Json.toJson(response)))
                .recover { case t: Throwable =>
                  logger.error("[createForm] failed", t)
                  InternalServerError(Json.obj("message" -> "Unexpected error"))
                }
        )
    }

  def updateForm(formId: Long): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateFormRequest]
        .fold(
          errors => scala.concurrent.Future.successful(BadRequest(Json.obj("message" -> "Invalid request body"))),
          body =>
            service
              .updateForm(formId, body)
              .map(response => Ok(Json.toJson(response)))
              .recover(handleFormErrors("updateForm", formId))
        )
    }

  def updateFormStatus(formId: Long): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateFormStatusRequest]
        .fold(
          errors => scala.concurrent.Future.successful(BadRequest(Json.obj("message" -> "Invalid request body"))),
          body =>
            service
              .updateFormStatus(formId, body)
              .map(response => Ok(Json.toJson(response)))
              .recover(handleFormErrors("updateFormStatus", formId))
        )
    }

  def deleteForm(formId: Long): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .deleteForm(formId)
        .map(_ => NoContent)
        .recover(handleFormErrors("deleteForm", formId))
    }

  private def handleFormErrors(operation: String, formId: Long): PartialFunction[Throwable, play.api.mvc.Result] = {
    case _: FormNotFoundException       =>
      NotFound(Json.obj("code" -> "FORM_NOT_FOUND", "message" -> s"No form found with id $formId"))
    case _: FormAlreadyUpdatedException =>
      Conflict(Json.obj("code" -> "FORM_ALREADY_UPDATED", "message" -> "Form already updated by another session"))
    case t: Throwable                   =>
      logger.error(s"[$operation] failed", t)
      InternalServerError(Json.obj("message" -> "Unexpected error"))
  }
}
