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
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.{verify, when}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource
import uk.gov.hmrc.formpproxy.cis.models.response.{Subcontractor, GetSubcontractorListResponse}
import scala.concurrent.Future


final class SubcontractorControllerIntegrationSpec
  extends Matchers
    with ScalaFutures
    with IntegrationPatience
    with ApplicationWithWiremock
    with MockitoSugar {


  private val createPath = "/cis/subcontractor/create"
  private val updatePath = "/cis/subcontractor/update"
  private val listPath = "/cis/subcontractors/cis-123"

  private val mockRepo: CisMonthlyReturnSource = mock[CisMonthlyReturnSource]

  override lazy val app = new GuiceApplicationBuilder()
    .configure(extraConfig)
    .overrides(
      bind[CisMonthlyReturnSource].toInstance(mockRepo)
    )
    .build()


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

  "GET /cis/subcontractors/:cisId (getSubcontractorList)" should {

    "returns 401 when there is no active session" in {
      AuthStub.unauthorised()

      val res = getAwait(listPath)

      res.status mustBe UNAUTHORIZED
    }

    "returns 200 and subcontractor list when authorised" in {
      AuthStub.authorised()

      val response = GetSubcontractorListResponse(
        subcontractors = List(
          Subcontractor(
            subcontractorId = 1L,
            subbieResourceRef = 10,
            `type` = "soletrader",
            utr = Some("1234567890"),
            pageVisited = None,
            partnerUtr = None,
            crn = None,
            firstName = None,
            nino = None,
            secondName = None,
            surname = None,
            partnershipTradingName = None,
            tradingName = None,
            addressLine1 = None,
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            country = None,
            postcode = None,
            emailAddress = None,
            phoneNumber = None,
            mobilePhoneNumber = None,
            worksReferenceNumber = None,
            version = None,
            taxTreatment = None,
            updatedTaxTreatment = None,
            verificationNumber = None,
            createDate = None,
            lastUpdate = None,
            matched = None,
            verified = None,
            autoVerified = None,
            verificationDate = None,
            lastMonthlyReturnDate = None,
            pendingVerifications = None
          )
        )
      )

      when(mockRepo.getSubcontractorList("cis-123"))
        .thenReturn(Future.successful(response))

      val res = getAwait(listPath)

      res.status mustBe OK
      (res.json \ "subcontractors").as[Seq[play.api.libs.json.JsValue]] must have size 1
      ((res.json \ "subcontractors")(0) \ "subbieResourceRef").as[Int] mustBe 10
      ((res.json \ "subcontractors")(0) \ "type").as[String] mustBe "soletrader"
      ((res.json \ "subcontractors")(0) \ "utr").as[String] mustBe "1234567890"

      verify(mockRepo).getSubcontractorList("cis-123")
    }
  }

}
