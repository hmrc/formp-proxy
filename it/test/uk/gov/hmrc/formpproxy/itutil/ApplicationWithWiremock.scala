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

package uk.gov.hmrc.formpproxy.itutil

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames as PlayHeaders
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.HttpReads.Implicits.*

import scala.concurrent.{ExecutionContext, Future}


trait ApplicationWithWiremock
  extends AnyFreeSpec
    with GuiceOneServerPerSuite
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures {

  lazy val wireMock = new WireMock

  val extraConfig: Map[String, Any] = {
    Map[String, Any](
      "microservice.services.auth.host" -> WireMockConstants.stubHost,
      "microservice.services.auth.port" -> WireMockConstants.stubPort,
      "feature-switch.cis-formp-stubbed" -> true
    )
  }

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(extraConfig)
    .build()

  lazy val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override protected def beforeAll(): Unit =
    wireMock.start()
    super.beforeAll()

  override def beforeEach(): Unit =
    wireMock.resetAll()
    super.beforeEach()

  override def afterAll(): Unit =
    wireMock.stop()
    super.afterAll()

  val baseUrl: String = s"http://localhost:$port/formp-proxy"

  private val commonHeaders: Seq[(String, String)] =
    Seq(
      PlayHeaders.AUTHORIZATION -> "Bearer it-token",
      HeaderNames.xSessionId -> "sessionId"
    )

  protected def get(path: String): Future[HttpResponse] =
    httpClient
      .get(url"$baseUrl/$path")
      .setHeader(commonHeaders *)
      .execute[HttpResponse]

  protected def post(path: String, body: JsValue): Future[HttpResponse] = {
    val url = s"$baseUrl/$path"
    httpClient.post(url"$url")
      .setHeader(
        commonHeaders ++ Seq(
          "Accept" -> "application/json",
          "Content-Type" -> "application/json"
        ) *
      )
      .withBody(body)
      .execute[HttpResponse]
  }

  protected def postJson(uri: String, body: JsValue): HttpResponse =
    post(uri, body).futureValue
}