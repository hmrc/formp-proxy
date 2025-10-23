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

package uk.gov.hmrc.formpproxy.base

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.*
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents}
import play.api.test.Helpers.{CONTENT_TYPE, JSON, POST, stubControllerComponents}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext


trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with DefaultAwaitTimeout
    with OptionValues
    with ScalaFutures
    with MockitoSugar
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  lazy val cc: ControllerComponents = stubControllerComponents()
  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  def withJson[A: Writes](req: FakeRequest[_], body: A): FakeRequest[JsValue] =
    req.withBody(Json.toJson(body)).withHeaders(CONTENT_TYPE -> JSON)

  def postJson[A: Writes](path: String, body: A): FakeRequest[JsValue] =
    withJson(FakeRequest(POST, path), body)

}