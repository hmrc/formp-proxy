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

package uk.gov.hmrc.formpproxy.cis

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.formpproxy.itutil.{ApplicationWithWiremock, AuthStub}

final class SubmissionControllerIntegrationSpec
  extends Matchers
    with ScalaFutures
    with IntegrationPatience
    with ApplicationWithWiremock {

  private val createPath = "/submissions/create"
  private val updatePath = "/submissions/update"

  "SubmissionController" should {

    "POST /formp-proxy/submissions (createSubmission)" should {

      "returns 400 when JSON is missing required fields" in {
        AuthStub.authorised()

        val res = postAwait(createPath, Json.obj())

        res.status mustBe BAD_REQUEST
        (res.json \ "message").as[String].toLowerCase must include ("invalid")
      }

      "returns 401 when there is no active session" in {
        AuthStub.unauthorised()

        val res = postAwait(createPath, Json.obj(
          "instanceId" -> "123",
          "taxYear"    -> 2024,
          "taxMonth"   -> 4
        ))

        res.status mustBe UNAUTHORIZED
      }

      "returns 404 for unknown endpoint (routing sanity)" in {
        AuthStub.authorised()

        val res = postAwait("/does-not-exist", Json.obj(
          "instanceId" -> "123",
          "taxYear"    -> 2024,
          "taxMonth"   -> 4
        ))

        res.status mustBe NOT_FOUND
      }
    }

    "POST /formp-proxy/submissions/update (updateSubmission)" should {

      "returns 400 when JSON is missing required fields" in {
        AuthStub.authorised()

        val res = postAwait(updatePath, Json.obj("instanceId" -> "123"))

        res.status mustBe BAD_REQUEST
        (res.json \ "message").as[String].toLowerCase must include ("invalid")
      }

      "returns 401 when there is no active session" in {
        AuthStub.unauthorised()

        val json = Json.obj(
          "instanceId"        -> "123",
          "taxYear"           -> 2024,
          "taxMonth"          -> 4,
          "hmrcMarkGenerated" -> "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
          "submittableStatus" -> "ACCEPTED"
        )

        val res = postAwait(updatePath, json)

        res.status mustBe UNAUTHORIZED
      }
    }
  }
}