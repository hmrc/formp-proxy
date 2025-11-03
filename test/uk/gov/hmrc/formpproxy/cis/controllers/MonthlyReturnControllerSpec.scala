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

package uk.gov.hmrc.formpproxy.cis.controllers

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{ControllerComponents, PlayBodyParsers, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.formpproxy.cis.models.requests.{CreateNilMonthlyReturnRequest, InstanceIdRequest}
import uk.gov.hmrc.formpproxy.cis.models.response.CreateNilMonthlyReturnResponse
import uk.gov.hmrc.formpproxy.cis.models.{MonthlyReturn, UserMonthlyReturns, requests}
import uk.gov.hmrc.formpproxy.cis.services.MonthlyReturnService
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class MonthlyReturnControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "MonthlyReturnController retrieveMonthlyReturns" - {

    "returns 200 with wrapper when service succeeds (happy path)" in new Setup {
      when(mockService.getAllMonthlyReturns(eqTo("abc-123")))
        .thenReturn(Future.successful(nonEmptyWrapper))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj("instanceId" -> "abc-123"))
      val res: Future[Result]       = controller.retrieveMonthlyReturns(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      contentAsJson(res) mustBe Json.toJson(nonEmptyWrapper)
      verify(mockService).getAllMonthlyReturns(eqTo("abc-123"))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with empty wrapper when service returns no rows" in new Setup {
      when(mockService.getAllMonthlyReturns(eqTo("abc-123")))
        .thenReturn(Future.successful(UserMonthlyReturns(Seq.empty)))

      val req = makeJsonRequest(Json.obj("instanceId" -> "abc-123"))
      val res = controller.retrieveMonthlyReturns(req)

      status(res) mustBe OK
      contentAsJson(res) mustBe Json.toJson(UserMonthlyReturns(Seq.empty))
    }

    "returns 400 when JSON body is an empty object" in new Setup {
      val req = makeJsonRequest(Json.obj())
      val res = controller.retrieveMonthlyReturns(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "returns 400 when instanceId is missing" in new Setup {
      val req = makeJsonRequest(Json.obj("somethingElse" -> "oops"))
      val res = controller.retrieveMonthlyReturns(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val err = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)
      when(mockService.getAllMonthlyReturns(eqTo("abc-123")))
        .thenReturn(Future.failed(err))

      val req = makeJsonRequest(Json.obj("instanceId" -> "abc-123"))
      val res = controller.retrieveMonthlyReturns(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.getAllMonthlyReturns(eqTo("abc-123")))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = makeJsonRequest(Json.obj("instanceId" -> "abc-123"))
      val res = controller.retrieveMonthlyReturns(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }
  }

  "MonthlyReturnController createNilMonthlyReturn" - {

    "returns 201 with status when service succeeds" in new Setup {
      val request  = CreateNilMonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 2,
        decInformationCorrect = "Y",
        decNilReturnNoPayments = "Y"
      )
      val response = CreateNilMonthlyReturnResponse(status = "STARTED")

      when(mockService.createNilMonthlyReturn(eqTo(request)))
        .thenReturn(Future.successful(response))

      // Typed request body matches Action[CreateNilMonthlyReturnRequest]
      val req: FakeRequest[CreateNilMonthlyReturnRequest] =
        FakeRequest(POST, "/formp-proxy/monthly-return/nil/create").withBody(request)

      val res: Future[Result] = controller.createNilMonthlyReturn(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      contentAsJson(res) mustBe Json.obj("status" -> "STARTED")

      verify(mockService).createNilMonthlyReturn(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }

    "propagates UpstreamErrorResponse" in new Setup {
      val request = CreateNilMonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 2,
        decInformationCorrect = "Y",
        decNilReturnNoPayments = "Y"
      )
      val err     = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)

      when(mockService.createNilMonthlyReturn(eqTo(request)))
        .thenReturn(Future.failed(err))

      val req: FakeRequest[CreateNilMonthlyReturnRequest] =
        FakeRequest(POST, "/formp-proxy/monthly-return/nil/create").withBody(request)

      val res = controller.createNilMonthlyReturn(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")

      verify(mockService).createNilMonthlyReturn(eqTo(request))
      verifyNoMoreInteractions(mockService)
    }
  }

  "MonthlyReturnController getSchemeEmail" - {

    "returns 200 with email when service succeeds" in new Setup {
      when(mockService.getSchemeEmail(eqTo("abc-123"))).thenReturn(Future.successful(Some("x@y.com")))

      val req: FakeRequest[InstanceIdRequest] =
        FakeRequest(POST, "/formp-proxy/scheme/email").withBody(requests.InstanceIdRequest("abc-123"))
      val res: Future[Result]                 = controller.getSchemeEmail(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "email").asOpt[String] mustBe Some("x@y.com")
      verify(mockService).getSchemeEmail(eqTo("abc-123"))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with null when service returns None" in new Setup {
      when(mockService.getSchemeEmail(eqTo("abc-123"))).thenReturn(Future.successful(None))

      val req: FakeRequest[InstanceIdRequest] =
        FakeRequest(POST, "/formp-proxy/scheme/email").withBody(requests.InstanceIdRequest("abc-123"))
      val res                                 = controller.getSchemeEmail(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "email").toOption.flatMap(_.asOpt[String]) mustBe None
    }

    "propagates UpstreamErrorResponse" in new Setup {
      val err = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)
      when(mockService.getSchemeEmail(eqTo("abc-123"))).thenReturn(Future.failed(err))

      val req: FakeRequest[InstanceIdRequest] =
        FakeRequest(POST, "/formp-proxy/scheme/email").withBody(requests.InstanceIdRequest("abc-123"))
      val res                                 = controller.getSchemeEmail(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService: MonthlyReturnService = mock[MonthlyReturnService]
    val controller                        = new MonthlyReturnController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue) =
      FakeRequest(POST, "/formp-proxy/monthly-returns")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)

    private def mkReturn(id: Long, month: Int, year: Int = 2025): MonthlyReturn =
      MonthlyReturn(
        monthlyReturnId = id,
        taxYear = year,
        taxMonth = month,
        nilReturnIndicator = Some("N"),
        decEmpStatusConsidered = Some("Y"),
        decAllSubsVerified = Some("Y"),
        decInformationCorrect = Some("Y"),
        decNoMoreSubPayments = Some("N"),
        decNilReturnNoPayments = Some("N"),
        status = Some("SUBMITTED"),
        lastUpdate = Some(LocalDateTime.parse("2025-01-01T00:00:00")),
        amendment = Some("N"),
        supersededBy = None
      )

    val nonEmptyWrapper: UserMonthlyReturns =
      UserMonthlyReturns(Seq(mkReturn(66666L, 1), mkReturn(66667L, 7)))
  }
}
