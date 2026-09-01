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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthOrInternalAuthAction
import uk.gov.hmrc.formpproxy.cis.models.requests.{ApplyPrepopulationRequest, UpdateSchemeVersionRequest}
import uk.gov.hmrc.formpproxy.cis.models.{CreateContractorSchemeParams, UpdateContractorSchemeParams}
import uk.gov.hmrc.formpproxy.cis.services.ContractorSchemeService
import uk.gov.hmrc.formpproxy.cis.utils.JsResultUtils.*
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client.{IAAction, Predicate, Resource}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ContractorSchemeController @Inject() (
  authOrInternalAuth: AuthOrInternalAuthAction,
  service: ContractorSchemeService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private val contractorSchemes      = Resource.from("formp-proxy", "formp-proxy/cis/contractor-schemes")
  private val readContractorSchemes  = authOrInternalAuth(Predicate.Permission(contractorSchemes, IAAction("READ")))
  private val writeContractorSchemes = authOrInternalAuth(Predicate.Permission(contractorSchemes, IAAction("WRITE")))

  def getScheme(instanceId: String): Action[AnyContent] =
    readContractorSchemes.async { implicit request =>
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
    writeContractorSchemes.async(parse.json) { implicit request =>
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
    writeContractorSchemes.async(parse.json) { implicit request =>
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
    writeContractorSchemes.async(parse.json) { implicit request =>
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

  def applyPrepopulation: Action[JsValue] =
    writeContractorSchemes.async(parse.json) { implicit request =>
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
