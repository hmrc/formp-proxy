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

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.formpproxy.itutil.{ApplicationWithWiremock, AuthStub}

final class SubcontractorControllerIntegrationSpec
  extends Matchers
    with ScalaFutures
    with IntegrationPatience
    with ApplicationWithWiremock {

  private val createPath = "/cis/subcontractor/create"
  private val updatePath = "/cis/subcontractor/update"
  private val listPath = "/cis/subcontractors/cis-123"

  "SubcontractorController" should {

    "POST /cis/subcontractor/create (createSubcontractor)" should {

//      "returns 200 and subcontractor ID when request is valid" in {
//        AuthStub.authorised()
//
//        val json = Json.obj(
//          "schemeId"         -> 999,
//          "subcontractorType"-> "soletrader",
//          "version"          -> 1
//        )
//
//        val res = postJson(createPath, json)
//
//        res.status mustBe OK
//        (res.json \ "subbieResourceRef").as[Int] must(be > 0)
//      }
//
      "returns 400 when JSON is missing required fields" in {
        AuthStub.authorised()

        val res = postAwait(createPath, Json.obj())
        res.status mustBe BAD_REQUEST
        (res.json \ "message").as[String].toLowerCase must include("invalid")
      }

      "returns 401 when there is no active session" in {
        AuthStub.unauthorised()

        val json = Json.obj(
        "schemeId"         -> 999,
        "subcontractorType"-> "soletrader",
        "version"          -> 1
        )

        val res = postAwait(createPath, json)

        res.status mustBe UNAUTHORIZED
      }
    }



    "POST /cis/subcontractor/update (updateSubcontractor)" should {

      "returns 400 when JSON is missing required fields" in {
        AuthStub.authorised()

        val res = postAwait(updatePath, Json.obj())

        res.status mustBe BAD_REQUEST
        (res.json \ "message").as[String].toLowerCase must include("invalid")
      }

      "returns 401 when there is no active session" in {
        AuthStub.unauthorised()

        val json = Json.obj(
          "utr"              -> "1234567890",
          "pageVisited"      -> 1,
          "schemeId"         -> 999,
          "subbieResourceRef"-> 1
        )

        val res = postAwait(updatePath, json)

        res.status mustBe UNAUTHORIZED
      }

      "returns 404 for unknown endpoint (routing sanity)" in {
        AuthStub.authorised()

        val res = postAwait("/does-not-exist", Json.obj(
          "utr"              -> "1234567890",
          "pageVisited"      -> 1,
          "schemeId"         -> 999,
          "subbieResourceRef"-> 1
        ))

        res.status mustBe NOT_FOUND
      }
    }
  }
  

}
