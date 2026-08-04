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
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.cis.services.VerificationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.formpproxy.cis.models.requests._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class VerificationController @Inject() (
  authorise: AuthAction,
  service: VerificationService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getNewestVerificationBatch(instanceId: String): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getNewestVerificationBatch(instanceId)
        .map(res => Ok(Json.toJson(res)))
        .recover { case t =>
          logger.error(s"[getNewestVerificationBatch] failed (instanceId=$instanceId)", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def getCurrentVerificationBatch(instanceId: String): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getCurrentVerificationBatch(instanceId)
        .map(res => Ok(Json.toJson(res)))
        .recover { case t =>
          logger.error(s"[getCurrentVerificationBatch] failed (instanceId=$instanceId)", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def createVerificationBatchAndVerifications(): Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body
        .validate[CreateVerificationBatchAndVerificationsRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          req =>
            service
              .createVerificationBatchAndVerifications(req)
              .map(res => Ok(Json.toJson(res)))
              .recover { case t =>
                logger.error("[createVerificationBatchAndVerifications] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def modifyVerifications(): Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body
        .validate[ModifyVerificationsRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          req =>
            service
              .modifyVerifications(req)
              .map(_ => NoContent)
              .recover { case t =>
                logger.error("[modifyVerifications] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def createSubmissionAndUpdateVerifications(): Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body
        .validate[CreateSubmissionAndUpdateVerificationsRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          req =>
            service
              .createSubmissionAndUpdateVerifications(req)
              .map(res => Ok(Json.toJson(res)))
              .recover { case t =>
                logger.error("[createSubmissionForVerification] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def updateVerificationSubmission(): Action[JsValue] =
    Action(parse.json).async { implicit request =>
      request.body
        .validate[UpdateVerificationSubmissionRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          req =>
            service
              .updateVerificationSubmission(req)
              .map(_ => NoContent)
              .recover { case t =>
                logger.error("[updateVerificationSubmission] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def processVerificationResponseFromChris(): Action[JsValue] =
    Action(parse.json).async { implicit request =>
      request.body
        .validate[ProcessVerificationResponseFromChrisRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          req =>
            service
              .processVerificationResponseFromChris(req)
              .map(_ => NoContent)
              .recover { case t =>
                logger.error("[processVerificationResponseFromChris] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def getSubmissionWithVerificationBatchByRefs(
    instanceId: String,
    verificationBatchResourceRef: Long
  ): Action[AnyContent] =
    authorise.async { implicit request =>
      if (instanceId.isBlank)
        Future.successful(BadRequest(Json.obj("message" -> "instanceId must not be blank")))
      else
        handleGetSubmissionWithVerificationBatch(
          GetSubmissionWithVerificationBatchRequest(
            instanceId = instanceId,
            verificationBatchResourceRef = verificationBatchResourceRef
          )
        )
    }

  def getSubmissionWithVerificationBatch: Action[JsValue] =
    Action(parse.json).async { implicit request =>
      request.body
        .validate[GetSubmissionWithVerificationBatchRequest]
        .fold(
          errors =>
            Future.successful(
              BadRequest(
                Json.obj(
                  "message" -> "Invalid payload",
                  "errors"  -> JsError.toJson(errors)
                )
              )
            ),
          handleGetSubmissionWithVerificationBatch
        )
    }

  private def handleGetSubmissionWithVerificationBatch(
    request: GetSubmissionWithVerificationBatchRequest
  ): Future[Result] =
    service
      .getSubmissionWithVerificationBatch(request)
      .map(response => Ok(Json.toJson(response)))
      .recover { case NonFatal(exception) =>
        logger.error(
          s"[VerificationController][getSubmissionWithVerificationBatch] failed for " +
            s"instanceId=${request.instanceId}, " +
            s"verificationBatchResourceRef=${request.verificationBatchResourceRef}",
          exception
        )

        InternalServerError(Json.obj("message" -> "Unexpected error"))
      }

  def getSubmittedVerifications(): Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body
        .validate[GetSubmittedVerificationsRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          req =>
            service
              .getSubmittedVerifications(req)
              .map(res => Ok(Json.toJson(res)))
              .recover { case t =>
                logger.error("[getSubmittedVerifications] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

}
