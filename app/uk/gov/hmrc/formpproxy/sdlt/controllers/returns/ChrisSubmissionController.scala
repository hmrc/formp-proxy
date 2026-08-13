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

package uk.gov.hmrc.formpproxy.sdlt.controllers.returns

import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.sdlt.models.submission.*
import uk.gov.hmrc.formpproxy.sdlt.services.submission.ChrisSubmissionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChrisSubmissionController @Inject() (
  authorise: AuthAction,
  service: ChrisSubmissionService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def lockReturn(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[LockReturnRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .lockReturn(body)
              .map { lockReturnResponse =>
                if (lockReturnResponse.success) Ok(Json.toJson(lockReturnResponse))
                else Conflict(Json.obj("message" -> "Version conflict acquiring return lock"))
              }
              .recover { case t =>
                logger.error("[lockReturn] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def createSubmission(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[CreateSubmissionRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .createSubmission(body)
              .map { createSubmissionReturn =>
                Created(Json.toJson(createSubmissionReturn))
              }
              .recover { case t =>
                logger.error("[createSubmission] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def updateSubmission(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateSubmissionRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .updateSubmission(body)
              .map { updateSubmissionReturn =>
                Ok(Json.toJson(updateSubmissionReturn))
              }
              .recover { case t =>
                logger.error("[updateSubmission] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def createSubmissionErrorDetail(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[CreateSubmissionErrorDetailRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .createSubmissionErrorDetail(body)
              .map { createSubmissionErrorDetailReturn =>
                Created(Json.toJson(createSubmissionErrorDetailReturn))
              }
              .recover { case t =>
                logger.error("[createSubmissionErrorDetail] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def deleteSubmissionErrorDetail(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[DeleteSubmissionErrorDetailRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .deleteSubmissionErrorDetail(body)
              .map { deleteSubmissionErrorDetailReturn =>
                Ok(Json.toJson(deleteSubmissionErrorDetailReturn))
              }
              .recover { case t =>
                logger.error("[deleteSubmissionErrorDetail] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def insertInitialGovTalkStatus(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[InsertInitialGovTalkStatusRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .insertInitialGovTalkStatus(body)
              .map { govTalkStatusReturn =>
                Created(Json.toJson(govTalkStatusReturn))
              }
              .recover { case t =>
                logger.error("[insertInitialGovTalkStatus] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def resetGovTalkStatus(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[ResetGovTalkStatusRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .resetGovTalkStatus(body)
              .map { govTalkStatusReturn =>
                Ok(Json.toJson(govTalkStatusReturn))
              }
              .recover { case t =>
                logger.error("[resetGovTalkStatus] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def updateGovTalkStatus(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateGovTalkStatusRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .updateGovTalkStatus(body)
              .map { govTalkStatusReturn =>
                Ok(Json.toJson(govTalkStatusReturn))
              }
              .recover { case t =>
                logger.error("[updateGovTalkStatus] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def updateGovTalkStatusCorrelationId(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateGovTalkStatusCorrelationIdRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .updateGovTalkStatusCorrelationId(body)
              .map { govTalkStatusReturn =>
                Ok(Json.toJson(govTalkStatusReturn))
              }
              .recover { case t =>
                logger.error("[updateGovTalkStatusCorrelationId] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def updateGovTalkStatusLock(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateGovTalkStatusLockRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .updateGovTalkStatusLock(body)
              .map { govTalkStatusReturn =>
                Ok(Json.toJson(govTalkStatusReturn))
              }
              .recover { case t =>
                logger.error("[updateGovTalkStatusLock] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def updateGovTalkStatistics(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateGovTalkStatisticsRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .updateGovTalkStatistics(body)
              .map { govTalkStatusReturn =>
                Ok(Json.toJson(govTalkStatusReturn))
              }
              .recover { case t =>
                logger.error("[updateGovTalkStatistics] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def deleteGovTalkStatus(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[DeleteGovTalkStatusRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .deleteGovTalkStatus(body)
              .map { govTalkStatusReturn =>
                Ok(Json.toJson(govTalkStatusReturn))
              }
              .recover { case t =>
                logger.error("[deleteGovTalkStatus] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def selectGovTalkStatus(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[SelectGovTalkStatusRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .selectGovTalkStatus(body)
              .map { selectGovTalkStatusResponse =>
                Ok(Json.toJson(selectGovTalkStatusResponse))
              }
              .recover { case t =>
                logger.error("[selectGovTalkStatus] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def selectGovTalkFormResultId(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[SelectGovTalkFormResultIdRequest]
        .fold(
          errs =>
            Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
          body =>
            service
              .selectGovTalkFormResultId(body)
              .map { selectGovTalkFormResultIdResponse =>
                Ok(Json.toJson(selectGovTalkFormResultIdResponse))
              }
              .recover { case t =>
                logger.error("[selectGovTalkFormResultId] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }
}
