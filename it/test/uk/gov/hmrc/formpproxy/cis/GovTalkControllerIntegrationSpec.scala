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
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.formpproxy.itutil.{ApplicationWithWiremock, AuthStub}

class GovTalkControllerIntegrationSpec
    extends Matchers
    with ScalaFutures
    with IntegrationPatience
    with ApplicationWithWiremock {

  "POST /formp-proxy/cis/govtalkstatus/get" should {

    val getEndpoint = "cis/govtalkstatus/get"

    "return 400 when JSON is missing required fields" in {
      AuthStub.authorised()

      val res1 = postAwait(getEndpoint, Json.obj())
      res1.status mustBe BAD_REQUEST
      (res1.json \ "message").as[String].toLowerCase must include("invalid payload")
    }

    "return 401 when there is no active session" in {
      AuthStub.unauthorised()

      val res = postAwait(getEndpoint, Json.obj("userIdentifier" -> "1", "formResultID" -> "12890"))
      res.status mustBe UNAUTHORIZED
    }

    "return 404 for unknown endpoint (routing sanity)" in {
      AuthStub.authorised()
      val res = postAwait("/does-not-exist", Json.obj("userIdentifier" -> "1", "formResultID" -> "12890"))
      res.status mustBe NOT_FOUND
    }
  }

  "POST /formp-proxy/cis/govtalkstatus/reset" should {

    val resetEndpoint = "cis/govtalkstatus/reset"

    "return 400 when JSON is missing required fields" in {
      AuthStub.authorised()

      val res1 = postAwait(resetEndpoint, Json.obj())
      res1.status mustBe BAD_REQUEST
      (res1.json \ "message").as[String].toLowerCase must include("invalid payload")
    }

    "return 401 when there is no active session" in {
      AuthStub.unauthorised()

      val res = postAwait(
        resetEndpoint,
        Json.obj(
          "userIdentifier"    -> "1",
          "formResultID"      -> "12890",
          "oldProtocolStatus" -> "dataRequest",
          "gatewayURL"        -> "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/action/VATDEC"
        )
      )
      res.status mustBe UNAUTHORIZED
    }

    "return 404 for unknown endpoint (routing sanity)" in {
      AuthStub.authorised()
      val res = postAwait(
        "/does-not-exist",
        Json.obj(
          "userIdentifier"    -> "1",
          "formResultID"      -> "12890",
          "oldProtocolStatus" -> "dataRequest",
          "gatewayURL"        -> "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/action/VATDEC"
        )
      )
      res.status mustBe NOT_FOUND
    }
  }

  "POST /formp-proxy/cis/govtalkstatus/create" should {

    val createEndpoint = "cis/govtalkstatus/create"

    "return 400 when JSON is missing required fields" in {
      AuthStub.authorised()

      val res1 = postAwait(createEndpoint, Json.obj())
      res1.status mustBe BAD_REQUEST
      (res1.json \ "message").as[String].toLowerCase must include("invalid payload")
    }

    "return 401 when there is no active session" in {
      AuthStub.unauthorised()

      val res = postAwait(
        createEndpoint,
        Json.obj(
          "userIdentifier" -> "1",
          "formResultID"   -> "12890",
          "correlationID"  -> "128903445",
          "gatewayURL"     -> "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/action/VATDEC"
        )
      )
      res.status mustBe UNAUTHORIZED
    }

    "return 404 for unknown endpoint (routing sanity)" in {
      AuthStub.authorised()
      val res = postAwait(
        "/does-not-exist",
        Json.obj(
          "userIdentifier" -> "1",
          "formResultID"   -> "12890",
          "correlationID"  -> "128903445",
          "gatewayURL"     -> "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/action/VATDEC"
        )
      )
      res.status mustBe NOT_FOUND
    }
  }
}
