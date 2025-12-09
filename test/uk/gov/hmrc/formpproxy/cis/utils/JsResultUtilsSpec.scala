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

package uk.gov.hmrc.formpproxy.cis.utils

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsPath, JsResult, JsSuccess, JsonValidationError}
import play.api.mvc.Results.Ok
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.cis.utils.JsResultUtils.*

import scala.concurrent.Future

class JsResultUtilsSpec extends AnyWordSpec with Matchers with ScalaFutures {

  "JsResultUtils.handleJsonErrors" should {

    "return BadRequest with error message and error details" in {
      val errors = Seq(
        (JsPath \ "field1", Seq(JsonValidationError("error.required"))),
        (JsPath \ "field2", Seq(JsonValidationError("error.invalid")))
      )

      val result = JsResultUtils.handleJsonErrors(errors).futureValue

      status(Future.successful(result)) mustBe BAD_REQUEST
      contentType(Future.successful(result)) mustBe Some(JSON)
      val json = contentAsJson(Future.successful(result))
      (json \ "message").as[String] mustBe "Invalid JSON body"
      (json \ "errors").isDefined mustBe true
    }

    "return BadRequest with single error" in {
      val errors = Seq(
        (JsPath \ "version", Seq(JsonValidationError("error.required")))
      )

      val result = JsResultUtils.handleJsonErrors(errors).futureValue

      status(Future.successful(result)) mustBe BAD_REQUEST
      (contentAsJson(Future.successful(result)) \ "message").as[String] mustBe "Invalid JSON body"
    }
  }

  "JsResultUtils.foldErrorsIntoBadRequest extension" should {

    "call mapFn when JsResult is JsSuccess" in {
      val success: JsResult[Int] = JsSuccess(42)
      var called                 = false

      val result = success.foldErrorsIntoBadRequest { value =>
        called = true
        value mustBe 42
        Future.successful(Ok("success"))
      }.futureValue

      called mustBe true
      status(Future.successful(result)) mustBe OK
    }

    "return BadRequest when JsResult is JsError" in {
      val error: JsResult[Int] = JsError(JsPath \ "field", JsonValidationError("error.required"))
      var called               = false

      val result = error.foldErrorsIntoBadRequest { _ =>
        called = true
        Future.successful(Ok("success"))
      }.futureValue

      called mustBe false
      status(Future.successful(result)) mustBe BAD_REQUEST
      (contentAsJson(Future.successful(result)) \ "message").as[String] mustBe "Invalid JSON body"
    }

    "return BadRequest with multiple errors when JsResult is JsError" in {
      val error: JsResult[String] = JsError(
        Seq(
          (JsPath \ "field1", Seq(JsonValidationError("error.required"))),
          (JsPath \ "field2", Seq(JsonValidationError("error.invalid")))
        )
      )

      val result = error.foldErrorsIntoBadRequest { _ =>
        Future.successful(Ok("success"))
      }.futureValue

      status(Future.successful(result)) mustBe BAD_REQUEST
      (contentAsJson(Future.successful(result)) \ "message").as[String] mustBe "Invalid JSON body"
    }
  }
}
