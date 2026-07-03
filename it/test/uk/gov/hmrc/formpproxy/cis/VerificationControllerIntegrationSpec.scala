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

package uk.gov.hmrc.formpproxy.cis

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.http.Status.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNull, Json}
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource
import uk.gov.hmrc.formpproxy.itutil.{ApplicationWithWiremock, AuthStub}

import scala.concurrent.Future

class VerificationControllerIntegrationSpec
    extends Matchers
    with ScalaFutures
    with IntegrationPatience
    with ApplicationWithWiremock {

  private lazy val stubRepo: CisMonthlyReturnSource = {
    val m = org.mockito.Mockito.mock(classOf[CisMonthlyReturnSource])
    when(m.modifyVerifications(any())).thenReturn(Future.successful(()))
    when(m.updateVerificationSubmission(any())).thenReturn(Future.successful(()))
    m
  }

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(extraConfig)
    .overrides(bind[CisMonthlyReturnSource].toInstance(stubRepo))
    .build()

  "GET /formp-proxy/cis/verification-batch/current/:instanceId " should {

    "return 401 when there is no active session" in {
      AuthStub.unauthorised()

      val res = getResponse("cis/verification-batch/current/abc-123").futureValue

      res.status mustBe UNAUTHORIZED
    }

    "return 404 for unknown endpoint (routing sanity)" in {
      AuthStub.authorised()

      val res = getResponse("does-not-exist").futureValue

      res.status mustBe NOT_FOUND
    }
  }

  "POST /formp-proxy/cis/verification-batch/modify " should {

    val endpoint = "cis/verification-batch/modify"

    "return 400 when JSON is missing required fields" in {
      AuthStub.authorised()

      val res1 = postAwait(endpoint, Json.obj())
      res1.status mustBe BAD_REQUEST
      (res1.json \ "message").as[String].toLowerCase must include("invalid payload")
    }

    "return 401 when there is no active session" in {
      AuthStub.unauthorised()

      val res = postAwait(
        endpoint,
        Json.obj(
          "instanceId"          -> "abc-123",
          "deleteVerifications" -> Json.obj(
            "verificationResourceReferences" -> Seq(111L, 222L)
          ),
          "createVerifications" -> Json.obj(
            "verificationBatchResourceRef"   -> 10L,
            "verificationResourceReferences" -> Seq(333L, 444L),
            "actionIndicator"                -> JsNull
          )
        )
      )

      res.status mustBe UNAUTHORIZED
    }

    "return 204 when the request is valid" in {
      AuthStub.authorised()

      val res = postAwait(
        endpoint,
        Json.obj(
          "instanceId"          -> "abc-123",
          "deleteVerifications" -> Json.obj(
            "verificationResourceReferences" -> Seq(111L, 222L)
          ),
          "createVerifications" -> Json.obj(
            "verificationBatchResourceRef"   -> 10L,
            "verificationResourceReferences" -> Seq(333L, 444L),
            "actionIndicator"                -> JsNull
          )
        )
      )

      res.status mustBe NO_CONTENT
    }

    "return 404 for unknown endpoint (routing sanity)" in {
      AuthStub.authorised()

      val res = postAwait(
        "does-not-exist",
        Json.obj(
          "instanceId"          -> "abc-123",
          "deleteVerifications" -> Json.obj(
            "verificationResourceReferences" -> Seq(111L, 222L)
          ),
          "createVerifications" -> Json.obj(
            "verificationBatchResourceRef"   -> 10L,
            "verificationResourceReferences" -> Seq(333L, 444L),
            "actionIndicator"                -> JsNull
          )
        )
      )

      res.status mustBe NOT_FOUND
    }
  }

  "POST /formp-proxy/cis/verification/submission/update " should {

    val endpoint = "cis/verification/submission/update"

    "return 400 when JSON is missing required fields" in {
      AuthStub.authorised()

      val res1 = postAwait(endpoint, Json.obj())
      res1.status mustBe BAD_REQUEST
      (res1.json \ "message").as[String].toLowerCase must include("invalid payload")
    }

    "return 401 when there is no active session" in {
      AuthStub.unauthorised()

      val res = postAwait(
        endpoint,
        Json.obj(
          "instanceId"                   -> "abc-123",
          "verificationBatchId"          -> 99L,
          "verificationBatchResourceRef" -> 77L,
          "submittableStatus"            -> "FATAL_ERROR",
          "govtalkErrorCode"             -> "500",
          "govtalkErrorType"             -> "timeOut",
          "govtalkErrorMessage"          -> "timeOut"
        )
      )

      res.status mustBe UNAUTHORIZED
    }

    "return 204 when the request is valid" in {
      AuthStub.authorised()

      val res = postAwait(
        endpoint,
        Json.obj(
          "instanceId"                   -> "abc-123",
          "verificationBatchId"          -> 99L,
          "verificationBatchResourceRef" -> 77L,
          "submittableStatus"            -> "FATAL_ERROR",
          "govtalkErrorCode"             -> "500",
          "govtalkErrorType"             -> "timeOut",
          "govtalkErrorMessage"          -> "timeOut"
        )
      )

      res.status mustBe NO_CONTENT
    }

    "return 404 for unknown endpoint (routing sanity)" in {
      AuthStub.authorised()

      val res = postAwait(
        "does-not-exist",
        Json.obj(
          "instanceId"                   -> "abc-123",
          "verificationBatchId"          -> 99L,
          "verificationBatchResourceRef" -> 77L,
          "submittableStatus"            -> "FATAL_ERROR"
        )
      )

      res.status mustBe NOT_FOUND
    }
  }

}
