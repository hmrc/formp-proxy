package uk.gov.hmrc.formpproxy.cis.controllers

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.cis.services.BatchPollService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class BatchPollController @Inject() (
  authorise: AuthAction,
  service: BatchPollService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getBatchPollSubmissions(): Action[AnyContent] = authorise.async { implicit request =>
    service
      .getBatchPollSubmissions()
      .map(response => Ok(Json.toJson(response)))
      .recover { case t =>
        logger.error("[getBatchPollSubmissions] failed", t)
        InternalServerError(Json.obj("message" -> "Unexpected error"))
      }
  }
}
