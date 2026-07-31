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

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatest.freespec.AnyFreeSpec
import play.api.http.Status.CREATED
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.formpproxy.actions.{CisAuthOrApiKeyAction, DefaultCisAuthOrApiKeyAction}
import uk.gov.hmrc.formpproxy.actions.FakeAuthAction
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.requests.*
import uk.gov.hmrc.formpproxy.cis.services.SubmissionService
import uk.gov.hmrc.formpproxy.config.AppConfig

import scala.concurrent.Future

class SubmissionControllerSpec extends SpecBase {

  trait Setup {
    val expectedApiKey               = "test-cis-api-key"
    val service: SubmissionService   = mock[SubmissionService]
    val auth: FakeAuthAction         = new FakeAuthAction(cc.parsers)
    val authConnector: AuthConnector = mock[AuthConnector]
    val appConfig: AppConfig         = mock[AppConfig]

    when(appConfig.cisInternalServiceApiKey)
      .thenReturn(expectedApiKey)

    val cisAuthOrApiKeyAction: CisAuthOrApiKeyAction =
      new DefaultCisAuthOrApiKeyAction(
        authConnector = authConnector,
        appConfig = appConfig,
        parser = new BodyParsers.Default(cc.parsers)
      )

    lazy val controller =
      new SubmissionController(
        authorise = auth,
        cisAuthOrApiKeyAction = cisAuthOrApiKeyAction,
        service = service,
        cc = cc
      )

    def postCisJson(
      path: String,
      json: JsValue
    ): FakeRequest[JsValue] =
      postJson(path, json)
        .withHeaders("X-API-Key" -> expectedApiKey)
  }

  def setup: Setup = new Setup {}

  "POST /submissions (createSubmission)" - {

    "returns 201 Created with submissionId on valid payload" in {
      val s = setup; import s.*

      when(service.createSubmission(any[CreateSubmissionRequest]))
        .thenReturn(Future.successful("sub-123"))

      val json = Json.toJson(
        CreateSubmissionRequest(
          instanceId = "123",
          taxYear = 2024,
          taxMonth = 4,
          amendment = "N",
          hmrcMarkGenerated = Some("Dj5TVJDyRYCn9zta5EdySeY4fyA="),
          emailRecipient = Some("test@test.com")
        )
      )

      val result = controller
        .createSubmission()
        .apply(
          postJson("/submissions", json)
        )

      status(result) mustBe CREATED
      contentAsJson(result) mustBe Json.obj("submissionId" -> "sub-123")
      verify(service).createSubmission(any[CreateSubmissionRequest])
    }

    "returns 400 BadRequest for invalid JSON" in {
      val s = setup; import s.*

      val bad    = Json.obj("nope" -> "nope")
      val result = controller
        .createSubmission()
        .apply(
          postJson("/submissions", bad)
        )

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      verify(service, never()).createSubmission(any[CreateSubmissionRequest])
    }

    "maps service failure to 500 with error body" in {
      val s = setup; import s.*

      when(service.createSubmission(any[CreateSubmissionRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val json = Json.toJson(CreateSubmissionRequest("123", 2024, 4, "N"))

      val result = controller
        .createSubmission()
        .apply(
          postJson("/submissions", json)
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")
    }
  }

  "POST /submissions/update (updateSubmission)" - {

    "returns 204 NoContent on valid payload" in {
      val s = setup
      import s.*

      val request = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        amendment = "N",
        hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
        submittableStatus = "ACCEPTED"
      )

      when(service.updateSubmission(any[UpdateSubmissionRequest]))
        .thenReturn(Future.successful(()))

      val result =
        controller
          .updateSubmission()
          .apply(
            postCisJson(
              "/submissions/update",
              Json.toJson(request)
            )
          )

      status(result) mustBe NO_CONTENT

      verify(service)
        .updateSubmission(any[UpdateSubmissionRequest])

      verifyNoMoreInteractions(service)
      verifyNoInteractions(authConnector)
    }

    "returns 400 BadRequest for invalid JSON" in {
      val s = setup
      import s.*

      val result =
        controller
          .updateSubmission()
          .apply(
            postCisJson(
              "/submissions/update",
              Json.obj("bad" -> "json")
            )
          )

      status(result) mustBe BAD_REQUEST

      (contentAsJson(result) \ "message")
        .as[String] mustBe "Invalid payload"

      verify(service, never())
        .updateSubmission(any[UpdateSubmissionRequest])

      verifyNoInteractions(authConnector)
    }

    "maps service failure to 500" in {
      val s = setup
      import s.*

      val request = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        amendment = "N",
        hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
        submittableStatus = "ACCEPTED"
      )

      when(service.updateSubmission(any[UpdateSubmissionRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result =
        controller
          .updateSubmission()
          .apply(
            postCisJson(
              "/submissions/update",
              Json.toJson(request)
            )
          )

      status(result) mustBe INTERNAL_SERVER_ERROR

      verify(service)
        .updateSubmission(any[UpdateSubmissionRequest])

      verifyNoMoreInteractions(service)
      verifyNoInteractions(authConnector)
    }

    "returns 401 Unauthorized when the API key is missing" in {
      val s = setup
      import s.*

      val request = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        amendment = "N",
        hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
        submittableStatus = "ACCEPTED"
      )

      val result =
        controller
          .updateSubmission()
          .apply(
            postJson(
              "/submissions/update",
              Json.toJson(request)
            )
          )

      status(result) mustBe UNAUTHORIZED

      verifyNoInteractions(service)
      verifyNoInteractions(authConnector)
    }

    "returns 401 Unauthorized when the API key is incorrect" in {
      val s = setup
      import s.*

      val request = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        amendment = "N",
        hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
        submittableStatus = "ACCEPTED"
      )

      val result =
        controller
          .updateSubmission()
          .apply(
            postJson(
              "/submissions/update",
              Json.toJson(request)
            ).withHeaders("X-API-Key" -> "wrong-key")
          )

      status(result) mustBe UNAUTHORIZED

      verifyNoInteractions(service)
      verifyNoInteractions(authConnector)
    }
  }
}
