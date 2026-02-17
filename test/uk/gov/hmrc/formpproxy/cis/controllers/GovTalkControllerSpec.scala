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

package uk.gov.hmrc.formpproxy.cis.controllers

import org.mockito.ArgumentMatchers.any
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
import uk.gov.hmrc.formpproxy.cis.models.*
import uk.gov.hmrc.formpproxy.cis.models.requests.UpdateGovTalkStatusCorrelationIdRequest
import uk.gov.hmrc.formpproxy.cis.models.response.*
import uk.gov.hmrc.formpproxy.cis.services.GovTalkService
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class GovTalkControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "GovTalkController getGovTalkStatus" - {

    "returns 200 with multiple records when service returns data" in new Setup {
      when(mockService.getGovTalkStatus(any()))
        .thenReturn(Future.successful(nonEmptyResponse))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj("userIdentifier" -> "1", "formResultID" -> "12890"))
      val res: Future[Result]       = controller.getGovTalkStatus(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      contentAsJson(res) mustBe Json.toJson(nonEmptyResponse)
      verify(mockService).getGovTalkStatus(any())
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with empty response when service returns no rows" in new Setup {
      when(mockService.getGovTalkStatus(any()))
        .thenReturn(Future.successful(GetGovTalkStatusResponse(Seq.empty)))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj("userIdentifier" -> "1", "formResultID" -> "12890"))
      val res: Future[Result]       = controller.getGovTalkStatus(req)

      status(res) mustBe OK
      contentAsJson(res) mustBe Json.toJson(GetGovTalkStatusResponse(Seq.empty))
      verify(mockService).getGovTalkStatus(any())
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is an empty object" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.getGovTalkStatus(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val err = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)
      when(mockService.getGovTalkStatus(any()))
        .thenReturn(Future.failed(err))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj("userIdentifier" -> "1", "formResultID" -> "12890"))
      val res: Future[Result]       = controller.getGovTalkStatus(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] must include("Unexpected error")
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.getGovTalkStatus(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj("userIdentifier" -> "1", "formResultID" -> "12890"))
      val res: Future[Result]       = controller.getGovTalkStatus(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }
  }

  "GovTalkController updateGovTalkStatusCorrelationId" - {

    "returns 204 when service succeeds" in new Setup {
      when(mockService.updateGovTalkStatusCorrelationId(any()))
        .thenReturn(Future.successful(()))

      val body = UpdateGovTalkStatusCorrelationIdRequest(
        userIdentifier = "1",
        formResultID = "12890",
        correlationID = "C742D5DEE7EB4D15B4F7EFD50B890525",
        pollInterval = 1,
        gatewayURL = "http://example.com/test"
      )

      val req: FakeRequest[UpdateGovTalkStatusCorrelationIdRequest] =
        FakeRequest(POST, "/formp-proxy/cis/govtalkstatus/update-correlationID")
          .withBody(body)

      val res: Future[Result] = controller.updateGovTalkStatusCorrelationId(req)

      status(res) mustBe NO_CONTENT
      verify(mockService).updateGovTalkStatusCorrelationId(any())
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.updateGovTalkStatusCorrelationId(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val body = UpdateGovTalkStatusCorrelationIdRequest(
        userIdentifier = "1",
        formResultID = "12890",
        correlationID = "C742D5DEE7EB4D15B4F7EFD50B890525",
        pollInterval = 1,
        gatewayURL = "http://example.com/test"
      )

      val req: FakeRequest[UpdateGovTalkStatusCorrelationIdRequest] =
        FakeRequest(POST, "/formp-proxy/cis/govtalkstatus/update-correlationID")
          .withBody(body)

      val res: Future[Result] = controller.updateGovTalkStatusCorrelationId(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }
  }

  "GovTalkController resetGovTalkStatus" - {

    "returns 200 with multiple records when service returns data" in new Setup {
      when(mockService.resetGovTalkStatus(any()))
        .thenReturn(Future.successful(()))

      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"    -> "1",
          "formResultID"      -> "12890",
          "correlationID"     -> "128903445",
          "formLock"          -> "N",
          "createDate"        -> "2019-01-01T00:00:00",
          "lastMessageDate"   -> "2019-01-01T00:00:00",
          "numPolls"          -> 0,
          "pollInterval"      -> 0,
          "oldProtocolStatus" -> "dataRequest",
          "newProtocolStatus" -> "dataRequest",
          "gatewayURL"        -> "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/action/VATDEC"
        )
      )
      val res: Future[Result]       = controller.resetGovTalkStatus(req)

      status(res) mustBe NO_CONTENT
      verify(mockService).resetGovTalkStatus(any())
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is an empty object" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.resetGovTalkStatus(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val err = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)
      when(mockService.resetGovTalkStatus(any()))
        .thenReturn(Future.failed(err))

      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"    -> "1",
          "formResultID"      -> "12890",
          "correlationID"     -> "128903445",
          "formLock"          -> "N",
          "createDate"        -> "2019-01-01T00:00:00",
          "lastMessageDate"   -> "2019-01-01T00:00:00",
          "numPolls"          -> 0,
          "pollInterval"      -> 0,
          "oldProtocolStatus" -> "dataRequest",
          "newProtocolStatus" -> "dataRequest",
          "gatewayURL"        -> "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/action/VATDEC"
        )
      )
      val res: Future[Result]       = controller.resetGovTalkStatus(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] must include("Unexpected error")
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.resetGovTalkStatus(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"    -> "1",
          "formResultID"      -> "12890",
          "correlationID"     -> "128903445",
          "formLock"          -> "N",
          "createDate"        -> "2019-01-01T00:00:00",
          "endStateDate"      -> "2019-01-01T00:00:00",
          "lastMessageDate"   -> "2019-01-01T00:00:00",
          "numPolls"          -> 0,
          "pollInterval"      -> 0,
          "oldProtocolStatus" -> "dataRequest",
          "newProtocolStatus" -> "dataRequest",
          "gatewayURL"        -> "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/action/VATDEC"
        )
      )
      val res: Future[Result]       = controller.resetGovTalkStatus(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }
  }

  "GovTalkController updateGovTalkStatus" - {

    "returns 204 when service updated the record successfully" in new Setup {
      when(mockService.updateGovTalkStatus(any()))
        .thenReturn(Future.successful(()))

      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier" -> "1",
          "formResultID"   -> "12890",
          "endStateDate"   -> "2026-02-03T00:00:00",
          "protocolStatus" -> "dataRequest"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatus(req)

      status(res) mustBe NO_CONTENT
      verify(mockService).updateGovTalkStatus(any())
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is an empty object" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateGovTalkStatus(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val err = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)
      when(mockService.updateGovTalkStatus(any()))
        .thenReturn(Future.failed(err))

      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier" -> "1",
          "formResultID"   -> "12890",
          "endStateDate"   -> "2026-02-03T00:00:00",
          "protocolStatus" -> "dataRequest"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatus(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] must include("Unexpected error")
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.updateGovTalkStatus(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier" -> "1",
          "formResultID"   -> "12890",
          "endStateDate"   -> "2026-02-03T00:00:00",
          "protocolStatus" -> "dataRequest"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatus(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }
  }

  "GovTalkController updateGovTalkStatusStatistics" - {

    "returns 204 NoContent when service successfully updates statistics" in new Setup {
      when(mockService.updateGovTalkStatusStatistics(any()))
        .thenReturn(Future.successful(()))

      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"  -> "123456789",
          "formResultID"    -> "SUB123456",
          "lastMessageDate" -> "2026-02-16T10:30:00",
          "numPolls"        -> 3,
          "pollInterval"    -> 300,
          "gatewayURL"      -> "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe NO_CONTENT
      verify(mockService).updateGovTalkStatusStatistics(any())
      verifyNoMoreInteractions(mockService)
    }

    "returns 204 NoContent with zero polls" in new Setup {
      when(mockService.updateGovTalkStatusStatistics(any()))
        .thenReturn(Future.successful(()))

      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"  -> "123456789",
          "formResultID"    -> "SUB123456",
          "lastMessageDate" -> "2026-02-16T10:30:00",
          "numPolls"        -> 0,
          "pollInterval"    -> 0,
          "gatewayURL"      -> "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe NO_CONTENT
      verify(mockService).updateGovTalkStatusStatistics(any())
      verifyNoMoreInteractions(mockService)
    }

    "returns 204 NoContent with high poll numbers" in new Setup {
      when(mockService.updateGovTalkStatusStatistics(any()))
        .thenReturn(Future.successful(()))

      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"  -> "123456789",
          "formResultID"    -> "SUB123456",
          "lastMessageDate" -> "2026-02-16T10:30:00",
          "numPolls"        -> 100,
          "pollInterval"    -> 3600,
          "gatewayURL"      -> "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe NO_CONTENT
      verify(mockService).updateGovTalkStatusStatistics(any())
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is an empty object" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(Json.obj())
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when userIdentifier is missing" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "formResultID"    -> "SUB123456",
          "lastMessageDate" -> "2026-02-16T10:30:00",
          "numPolls"        -> 3,
          "pollInterval"    -> 300,
          "gatewayURL"      -> "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when formResultID is missing" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"  -> "123456789",
          "lastMessageDate" -> "2026-02-16T10:30:00",
          "numPolls"        -> 3,
          "pollInterval"    -> 300,
          "gatewayURL"      -> "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when lastMessageDate is missing" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier" -> "123456789",
          "formResultID"   -> "SUB123456",
          "numPolls"       -> 3,
          "pollInterval"   -> 300,
          "gatewayURL"     -> "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when numPolls is missing" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"  -> "123456789",
          "formResultID"    -> "SUB123456",
          "lastMessageDate" -> "2026-02-16T10:30:00",
          "pollInterval"    -> 300,
          "gatewayURL"      -> "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when pollInterval is missing" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"  -> "123456789",
          "formResultID"    -> "SUB123456",
          "lastMessageDate" -> "2026-02-16T10:30:00",
          "numPolls"        -> 3,
          "gatewayURL"      -> "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when gatewayURL is missing" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"  -> "123456789",
          "formResultID"    -> "SUB123456",
          "lastMessageDate" -> "2026-02-16T10:30:00",
          "numPolls"        -> 3,
          "pollInterval"    -> 300
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "returns 400 when lastMessageDate has invalid format" in new Setup {
      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"  -> "123456789",
          "formResultID"    -> "SUB123456",
          "lastMessageDate" -> "invalid-date",
          "numPolls"        -> 3,
          "pollInterval"    -> 300,
          "gatewayURL"      -> "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      verifyNoInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val err = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)
      when(mockService.updateGovTalkStatusStatistics(any()))
        .thenReturn(Future.failed(err))

      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"  -> "123456789",
          "formResultID"    -> "SUB123456",
          "lastMessageDate" -> "2026-02-16T10:30:00",
          "numPolls"        -> 3,
          "pollInterval"    -> 300,
          "gatewayURL"      -> "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] must include("Unexpected error")
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.updateGovTalkStatusStatistics(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req: FakeRequest[JsValue] = makeJsonRequest(
        Json.obj(
          "userIdentifier"  -> "123456789",
          "formResultID"    -> "SUB123456",
          "lastMessageDate" -> "2026-02-16T10:30:00",
          "numPolls"        -> 3,
          "pollInterval"    -> 300,
          "gatewayURL"      -> "http://localhost:9712/submission/ChRIS/CISR/Filing/sync/CIS300MR"
        )
      )
      val res: Future[Result]       = controller.updateGovTalkStatusStatistics(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }
  }

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService: GovTalkService = mock[GovTalkService]
    val controller                  = new GovTalkController(fakeAuth, mockService, cc)

    def makeJsonRequest(body: JsValue): FakeRequest[JsValue] =
      FakeRequest(POST, "/formp-proxy/cis/govtalkstatus/get")
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(body)

    private def mkRecord(protocol: String, numPolls: Int, pollInterval: Int): GovTalkStatusRecord =
      GovTalkStatusRecord(
        userIdentifier = "1",
        formResultID = "12890",
        correlationID = "C742D5DEE7EB4D15B4F7EFD50B890525",
        formLock = "N",
        createDate = None,
        endStateDate = Some(LocalDateTime.parse("2026-02-05T00:00:00")),
        lastMessageDate = LocalDateTime.parse("2025-02-05T00:00:00"),
        numPolls = numPolls,
        pollInterval = pollInterval,
        protocolStatus = protocol,
        gatewayURL = "http://vat.chris.hmrc.gov.uk:9102/ChRIS/UKVAT/Filing/sync/VATDEC"
      )

    val nonEmptyResponse: GetGovTalkStatusResponse =
      GetGovTalkStatusResponse(Seq(mkRecord("dataRequest", 0, 0), mkRecord("endState", 1, 1)))
  }
}
