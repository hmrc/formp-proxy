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

package uk.gov.hmrc.formpproxy

import uk.gov.hmrc.formpproxy.itutil.{ApplicationWithWiremock, AuthStub, WireMockConstants}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse

class DatabaseIntegrationSpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with ApplicationWithWiremock {

  // Override the configuration to turn OFF the stub
  override val extraConfig: Map[String, Any] = {
    Map[String, Any](
      "microservice.services.auth.host" -> WireMockConstants.stubHost,
      "microservice.services.auth.port" -> WireMockConstants.stubPort,
      "feature-switch.cis-formp-stubbed" -> false  // Turn OFF stub to test real database
    )
  }

  private def postJsonRaw(uri: String, body: JsValue): HttpResponse =
    postRaw(uri, body).futureValue

  "Database Integration Tests (Stub OFF)" should {

    "test getSchemeEmail endpoint with real database" in {
      AuthStub.authorised()

      val res = postJsonRaw("scheme/email", Json.obj("instanceId" -> "test-instance-id"))
      
      println(s"Response status: ${res.status}")
      println(s"Response body: ${res.body}")
      
      res.status mustBe 200
    }

    "test monthly returns endpoint with real database" in {
      AuthStub.authorised()

      val res = postJson("monthly-returns", Json.obj("instanceId" -> "test-instance-id"))

      println(s"Response status: ${res.status}")
      println(s"Response body: ${res.body}")
      
      res.status mustBe 200
    }
  }
}
