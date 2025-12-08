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

class MonthlyReturnControllerIntegrationSpec
  extends Matchers
    with ScalaFutures
    with IntegrationPatience
    with ApplicationWithWiremock {

  private val endpoint = "monthly-returns"

  "POST /formp-proxy/monthly-returns" should {

//    "return 200 with wrapper when authorised and JSON is valid" in {
//      AuthStub.authorised()
//
//      val res = postJson(endpoint, Json.obj("instanceId" -> "abc-123"))
//
//      res.status mustBe OK
//      (res.json \ "monthlyReturnList").asOpt[Seq[JsValue]] must not be empty
//    }

    "return 400 when JSON is missing required fields" in {
      AuthStub.authorised()

      val res1 = postJson(endpoint, Json.obj())
      res1.status mustBe BAD_REQUEST
      (res1.json \ "message").as[String].toLowerCase must include("invalid json")
    }

    "return 401 when there is no active session" in {
      AuthStub.unauthorised()

      val res = postJson(endpoint, Json.obj("instanceId" -> "abc-123"))
      res.status mustBe UNAUTHORIZED
    }

    "return 404 for unknown endpoint (routing sanity)" in {
      AuthStub.authorised()
      val res = postJson("/does-not-exist", Json.obj("instanceId" -> "abc-123"))
      res.status mustBe NOT_FOUND
    }
  }
}