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
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthOrInternalAuthAction
import uk.gov.hmrc.formpproxy.cis.models.GetSubcontractorList
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateAndUpdateSubcontractorRequest, DeleteSubcontractorRequest, UpdateSubcontractorRequest}
import uk.gov.hmrc.formpproxy.cis.models.response.GetSubcontractorResponse
import uk.gov.hmrc.formpproxy.cis.services.SubcontractorService
import uk.gov.hmrc.internalauth.client.{IAAction, Predicate, Resource}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.formpproxy.cis.models.response.GetSubcontractorResponse
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateAndUpdateSubcontractorRequest, DeleteSubcontractorRequest, UpdateSubcontractorForEditRequest}
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateAndUpdateSubcontractorRequest, DeleteSubcontractorRequest, UpdateSubcontractorRequest}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SubcontractorController @Inject() (
  authOrInternalAuth: AuthOrInternalAuthAction,
  service: SubcontractorService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private val subcontractors       = Resource.from("formp-proxy", "formp-proxy/cis/subcontractors")
  private val readSubcontractors   = authOrInternalAuth(Predicate.Permission(subcontractors, IAAction("READ")))
  private val writeSubcontractors  = authOrInternalAuth(Predicate.Permission(subcontractors, IAAction("WRITE")))
  private val deleteSubcontractors = authOrInternalAuth(Predicate.Permission(subcontractors, IAAction("DELETE")))

  def createAndUpdateSubcontractor(): Action[JsValue] =
    writeSubcontractors.async(parse.json) { implicit request =>
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
                logger.error("[createAndUpdateSubcontractor] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def updateSubcontractorForEdit(): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateSubcontractorForEditRequest]
        .fold(
          errs =>
            Future.successful(
              BadRequest(
                Json.obj(
                  "message" -> "Invalid payload",
                  "errors"  -> JsError.toJson(errs)
                )
              )
            ),
          body =>
            service
              .updateSubcontractorForEdit(body)
              .map(_ => NoContent)
              .recover { case t =>
                logger.error("[updateSubcontractorForEdit] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
              }
        )
    }

  def getSubcontractorList(cisId: String): Action[AnyContent] =
    readSubcontractors.async { implicit request =>
      service
        .getSubcontractorList(GetSubcontractorList(cisId))
        .map(res => Ok(Json.toJson(res)))
        .recover { case t =>
          logger.error(s"[getSubcontractorList] failed (cisId=$cisId)", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def getSubcontractorForDelete(
    cisId: String,
    subbieResourceRef: Long
  ): Action[AnyContent] =
    readSubcontractors.async { implicit request =>
      service
        .getSubcontractorForDelete(cisId, subbieResourceRef)
        .map(res => Ok(Json.toJson(res)))
        .recover { case t =>
          logger.error(
            s"[getSubcontractorForDelete] failed (cisId=$cisId, subbieResourceRef=$subbieResourceRef)",
            t
          )
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def getSubcontractor(
    cisId: String,
    subbieResourceRef: Long
  ): Action[AnyContent] =
    readSubcontractors.async { implicit request =>
      service
        .getSubcontractor(cisId, subbieResourceRef)
        .map(response => Ok(Json.toJson(response)))
        .recover { case t =>
          logger.error(
            s"[getSubcontractor] failed (cisId=$cisId, subbieResourceRef=$subbieResourceRef)",
            t
          )
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def deleteSubcontractor: Action[DeleteSubcontractorRequest] =
    deleteSubcontractors.async(parse.json[DeleteSubcontractorRequest]) { implicit request =>
      service
        .deleteSubcontractor(request.body)
        .map(_ => NoContent)
        .recover { case NonFatal(e) =>
          logger.error("[deleteSubcontractor] failed", e)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def updateSubcontractor: Action[JsValue] =
    writeSubcontractors.async(parse.json) { implicit request =>
      val submittedFields: Set[String] =
        (request.body \ "subcontractor")
          .asOpt[JsObject]
          .map(_.keys.toSet)
          .getOrElse(Set.empty)

      request.body
        .validate[UpdateSubcontractorRequest]
        .fold(
          errs =>
            Future.successful(
              BadRequest(
                Json.obj(
                  "message" -> "Invalid payload",
                  "errors"  -> JsError.toJson(errs)
                )
              )
            ),
          body =>
            body.subcontractor.subbieResourceRef match {
              case None =>
                Future.successful(
                  BadRequest(
                    Json.obj(
                      "message" -> "subbieResourceRef is required"
                    )
                  )
                )

              case Some(_) =>
                service
                  .updateSubcontractor(body, submittedFields)
                  .map(response => Ok(Json.toJson(response)))
                  .recover { case NonFatal(e) =>
                    logger.error("[updateSubcontractor] failed", e)
                    InternalServerError(Json.obj("message" -> "Unexpected error"))
                  }
            }
        )
    }

}
