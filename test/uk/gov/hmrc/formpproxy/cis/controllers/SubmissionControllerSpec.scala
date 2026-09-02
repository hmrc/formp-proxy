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
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, PlayBodyParsers}
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.formpproxy.actions.AuthOrInternalAuthAction
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.requests._
import uk.gov.hmrc.formpproxy.cis.services.SubmissionService
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, IAAction, Predicate, Resource, Retrieval}
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import scala.concurrent.Future

class SubmissionControllerSpec extends SpecBase {
  trait Setup {
    val service: SubmissionService = mock[SubmissionService]

    private val parsers: PlayBodyParsers = cc.parsers

    private val resource                             = Resource.from("formp-proxy", "formp-proxy/cis/submissions")
    val writeSubmissions: Predicate.Permission       = Predicate.Permission(resource, IAAction("WRITE"))
    val internalAuth: StubBehaviour                  = mock[StubBehaviour]
    val backendAuth: BackendAuthComponents           = BackendAuthComponentsStub(internalAuth)(cc, ec)
    val mockAuthConnector: AuthConnector             = mock[AuthConnector]
    val authOrInternalAuth: AuthOrInternalAuthAction =
      new AuthOrInternalAuthAction(mockAuthConnector, backendAuth, new BodyParsers.Default(parsers))

    when(internalAuth.stubAuth(Some(writeSubmissions), Retrieval.EmptyRetrieval)).thenReturn(Future.unit)

    lazy val controller = new SubmissionController(authOrInternalAuth, service, cc)
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
          postJson("/submissions", json).withHeaders(AUTHORIZATION -> "Token internal-auth")
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
          postJson("/submissions", bad).withHeaders(AUTHORIZATION -> "Token internal-auth")
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
          postJson("/submissions", json).withHeaders(AUTHORIZATION -> "Token internal-auth")
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")
    }
  }

  "POST /submissions/update (updateSubmission)" - {

    "returns 204 NoContent on valid payload" in {
      val s = setup; import s.*

      when(service.updateSubmission(any[UpdateSubmissionRequest]))
        .thenReturn(Future.successful(()))

      val json = Json.toJson(
        UpdateSubmissionRequest(
          instanceId = "123",
          taxYear = 2024,
          taxMonth = 4,
          amendment = "N",
          hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
          submittableStatus = "ACCEPTED"
        )
      )

      val result = controller
        .updateSubmission()
        .apply(
          postJson("/submissions/update", json).withHeaders(AUTHORIZATION -> "Token internal-auth")
        )

      status(result) mustBe NO_CONTENT
      verify(service).updateSubmission(any[UpdateSubmissionRequest])
    }

    "returns 400 BadRequest for invalid JSON" in {
      val s = setup; import s.*

      val bad = Json.obj("bad" -> "json")

      val result = controller
        .updateSubmission()
        .apply(
          postJson("/submissions/update", bad).withHeaders(AUTHORIZATION -> "Token internal-auth")
        )

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      verify(service, never()).updateSubmission(any[UpdateSubmissionRequest])
    }

    "maps service failure to 500 (no body expected)" in {
      val s = setup; import s.*

      when(service.updateSubmission(any[UpdateSubmissionRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val json = Json.toJson(
        UpdateSubmissionRequest(
          instanceId = "123",
          taxYear = 2024,
          taxMonth = 4,
          amendment = "N",
          hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
          submittableStatus = "ACCEPTED"
        )
      )

      val result = controller
        .updateSubmission()
        .apply(
          postJson("/submissions/update", json).withHeaders(AUTHORIZATION -> "Token internal-auth")
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
