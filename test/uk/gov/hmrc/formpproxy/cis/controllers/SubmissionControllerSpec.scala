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
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.FakeAuthAction
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateSubmissionRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.formpproxy.cis.services.SubmissionService

import scala.concurrent.Future

class SubmissionControllerSpec extends SpecBase {
  trait Setup {
    val service: SubmissionService = mock[SubmissionService]
    val auth: FakeAuthAction       = new FakeAuthAction(cc.parsers)
    lazy val controller            = new SubmissionController(auth, service, cc)
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

      val json = Json.toJson(CreateSubmissionRequest("123", 2024, 4))

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
      val s = setup; import s.*

      when(service.updateSubmission(any[UpdateSubmissionRequest]))
        .thenReturn(Future.successful(()))

      val json = Json.toJson(
        UpdateSubmissionRequest(
          instanceId = "123",
          taxYear = 2024,
          taxMonth = 4,
          hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
          submittableStatus = "ACCEPTED"
        )
      )

      val result = controller
        .updateSubmission()
        .apply(
          postJson("/submissions/update", json)
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
          postJson("/submissions/update", bad)
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
          hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
          submittableStatus = "ACCEPTED"
        )
      )

      val result = controller
        .updateSubmission()
        .apply(
          postJson("/submissions/update", json)
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
