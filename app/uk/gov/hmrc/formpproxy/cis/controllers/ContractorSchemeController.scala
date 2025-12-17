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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.cis.models.requests.{ApplyPrepopulationRequest, CreateSubcontractorRequest, UpdateSchemeVersionRequest}
import uk.gov.hmrc.formpproxy.cis.models.{CreateContractorSchemeParams, UpdateContractorSchemeParams}
import uk.gov.hmrc.formpproxy.cis.services.ContractorSchemeService
import uk.gov.hmrc.formpproxy.cis.utils.JsResultUtils.*
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

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

  def createScheme: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[CreateContractorSchemeParams]
        .foldErrorsIntoBadRequest(contractorScheme =>
          service
            .createScheme(contractorScheme)
            .map(id => Created(Json.obj("schemeId" -> id)))
            .recover {
              case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
              case t: Throwable             =>
                logger.error("[createScheme] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
        )
    }

  def updateScheme: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateContractorSchemeParams]
        .foldErrorsIntoBadRequest(contractorScheme =>
          service
            .updateScheme(contractorScheme)
            .map(version => Ok(Json.obj("version" -> version)))
            .recover {
              case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
              case t: Throwable             =>
                logger.error("[updateScheme] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
        )
    }

  def updateSchemeVersion: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[UpdateSchemeVersionRequest]
        .foldErrorsIntoBadRequest { case UpdateSchemeVersionRequest(instanceId, version) =>
          service
            .updateSchemeVersion(instanceId, version)
            .map(version => Ok(Json.obj("version" -> version)))
            .recover {
              case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
              case t: Throwable             =>
                logger.error("[updateSchemeVersion] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
        }
    }

  def createSubcontractor: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[CreateSubcontractorRequest]
        .foldErrorsIntoBadRequest { case CreateSubcontractorRequest(schemeId, subcontractorType, version) =>
          service
            .createSubcontractor(schemeId, subcontractorType, version)
            .map(version => Ok(Json.obj("subbieResourceRef" -> version)))
            .recover {
              case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
              case t: Throwable             =>
                logger.error("[createSubcontractor] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
        }

    }

  def applyPrepopulation: Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body
        .validate[ApplyPrepopulationRequest]
        .foldErrorsIntoBadRequest { prepopReq =>
          service
            .applyPrepopulation(prepopReq)
            .map(version => Ok(Json.obj("version" -> version)))
            .recover {
              case e: UpstreamErrorResponse => Status(e.statusCode)(Json.obj("message" -> e.message))
              case t: Throwable             =>
                logger.error("[applyPrepopulation] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
        }
    }

}
