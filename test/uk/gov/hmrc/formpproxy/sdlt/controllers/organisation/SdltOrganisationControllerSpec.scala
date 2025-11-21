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

package uk.gov.hmrc.formpproxy.sdlt.controllers.organisation

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{ControllerComponents, PlayBodyParsers, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.formpproxy.sdlt.models.Agent
import uk.gov.hmrc.formpproxy.sdlt.models.organisation.{GetSdltOrgByStornRequest, GetSdltOrgRequest}
import uk.gov.hmrc.formpproxy.sdlt.services.organisation.SdltOrganisationService
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.{ExecutionContext, Future}

class SdltOrganisationControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "SdltOrganisationController getSdltOrganisation" - {

    "returns 200 with full organisation data when service succeeds" in new Setup {
      val getRequest: GetSdltOrgByStornRequest =
        GetSdltOrgByStornRequest(storn = "STORN12345")

      when(mockService.getSDLTOrganisation(eqTo("STORN12345")))
        .thenReturn(Future.successful(fullOrganisationData))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.toJson(getRequest))
      val res: Future[Result]       = controller.getSDLTOrganisation()(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)

      val json = contentAsJson(res)
      (json \ "storn").asOpt[String] mustBe Some("STORN12345")
      (json \ "version").asOpt[String] mustBe Some("1")
      (json \ "isReturnUser").asOpt[String] mustBe Some("Y")
      (json \ "doNotDisplayWelcomePage").asOpt[String] mustBe Some("N")

      val agents = (json \ "agents").as[Seq[JsValue]]
      agents.size mustBe 1
      (agents.head \ "name").asOpt[String] mustBe Some("Smith & Co Solicitors")
      (agents.head \ "postcode").asOpt[String] mustBe Some("SW1A 2AA")

      verify(mockService).getSDLTOrganisation(eqTo("STORN12345"))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with minimal organisation data" in new Setup {
      val getRequest = GetSdltOrgByStornRequest("STORN99999")

      when(mockService.getSDLTOrganisation(eqTo("STORN99999")))
        .thenReturn(Future.successful(minimalOrganisationData))

      val req = makeJsonRequest(Json.toJson(getRequest))
      val res = controller.getSDLTOrganisation()(req)

      status(res) mustBe OK

      val json = contentAsJson(res)
      (json \ "storn").asOpt[String] mustBe Some("STORN99999")
      (json \ "version").asOpt[String] mustBe None
      (json \ "isReturnUser").asOpt[String] mustBe None
      (json \ "doNotDisplayWelcomePage").asOpt[String] mustBe None
      (json \ "agents").as[Seq[JsValue]].size mustBe 0
    }

    "returns 400 when JSON body is empty" in new Setup {
      val req = makeJsonRequest(Json.obj())
      val res = controller.getSDLTOrganisation()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "returns 400 when storn is missing" in new Setup {
      val invalidJson: JsObject = Json.obj("foo" -> "bar")

      val req = makeJsonRequest(invalidJson)
      val res = controller.getSDLTOrganisation()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val getRequest = GetSdltOrgByStornRequest("STORN12345")

      val err = UpstreamErrorResponse(
        "FORMP organisation service unavailable",
        BAD_GATEWAY,
        BAD_GATEWAY
      )

      when(mockService.getSDLTOrganisation(eqTo("STORN12345")))
        .thenReturn(Future.failed(err))

      val req = makeJsonRequest(Json.toJson(getRequest))
      val res = controller.getSDLTOrganisation()(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("FORMP organisation service unavailable")

      verify(mockService).getSDLTOrganisation(eqTo("STORN12345"))
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val getRequest = GetSdltOrgByStornRequest("STORN12345")

      when(mockService.getSDLTOrganisation(eqTo("STORN12345")))
        .thenReturn(Future.failed(new RuntimeException("Database timeout")))

      val req = makeJsonRequest(Json.toJson(getRequest))
      val res = controller.getSDLTOrganisation()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).getSDLTOrganisation(eqTo("STORN12345"))
      verifyNoMoreInteractions(mockService)
    }

    "handles service returning different storn" in new Setup {
      val getRequest = GetSdltOrgByStornRequest("STORN12345")

      val differentData =
        fullOrganisationData.copy(storn = Some("STORN99999"))

      when(mockService.getSDLTOrganisation(eqTo("STORN12345")))
        .thenReturn(Future.successful(differentData))

      val req = makeJsonRequest(Json.toJson(getRequest))
      val res = controller.getSDLTOrganisation()(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "storn").asOpt[String] mustBe Some("STORN99999")
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService: SdltOrganisationService = mock[SdltOrganisationService]
    val controller                           = new SdltOrganisationController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/sdlt/organisation")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)

    val fullOrganisationData: GetSdltOrgRequest =
      GetSdltOrgRequest(
        storn = Some("STORN12345"),
        version = Some("1"),
        isReturnUser = Some("Y"),
        doNotDisplayWelcomePage = Some("N"),
        agents = Seq(
          Agent(
            agentId = Some("AGT001"),
            storn = Some("STORN12345"),
            name = Some("Smith & Co Solicitors"),
            houseNumber = Some("10"),
            address1 = Some("Downing Street"),
            address2 = Some("Westminster"),
            address3 = Some("London"),
            address4 = None,
            postcode = Some("SW1A 2AA"),
            phone = Some("02071234567"),
            email = Some("info@smithco.co.uk"),
            dxAddress = Some("DX 12345 London"),
            agentResourceReference = Some("AGT-RES-001")
          )
        )
      )

    val minimalOrganisationData: GetSdltOrgRequest =
      GetSdltOrgRequest(
        storn = Some("STORN99999"),
        version = None,
        isReturnUser = None,
        doNotDisplayWelcomePage = None,
        agents = Seq.empty
      )
  }
}
