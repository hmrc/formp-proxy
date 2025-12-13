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

package uk.gov.hmrc.formpproxy.charities.controllers

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.PlayBodyParsers
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.actions.FakeAuthAction
import uk.gov.hmrc.formpproxy.charities.services.UnregulatedDonationsService

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import uk.gov.hmrc.formpproxy.charities.models.SaveUnregulatedDonationRequest

class UnregulatedDonationsControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "UnregulatedDonationsController" - {

    "getTotalUnregulatedDonations" - {

      "returns 200 with total when service succeeds with a some total (happy path)" in new Setup {
        when(mockService.getTotalUnregulatedDonations(eqTo("abc-123")))
          .thenReturn(Future.successful(Some(BigDecimal("1234.56"))))

        val req: FakeRequest[AnyContent] = 
          FakeRequest(GET, "/formp-proxy/charities/abc-123/unregulated-donations")

        val res: Future[Result]       = controller.getTotalUnregulatedDonations("abc-123")(req)

        status(res) mustBe OK
        contentType(res) mustBe Some(JSON)
        contentAsJson(res) mustBe Json.obj("unregulatedDonationsTotal" -> 1234.56)

        verify(mockService).getTotalUnregulatedDonations(eqTo("abc-123"))
        verifyNoMoreInteractions(mockService)
      }

      "returns 404 when service returns none" in new Setup {
        when(mockService.getTotalUnregulatedDonations(eqTo("abc-123")))
          .thenReturn(Future.successful(None))

        val req: FakeRequest[AnyContent] = 
          FakeRequest(GET, "/formp-proxy/charities/abc-123/unregulated-donations")

        val res: Future[Result]       = controller.getTotalUnregulatedDonations("abc-123")(req)

        status(res) mustBe NOT_FOUND
        contentType(res) mustBe Some(JSON)
        contentAsJson(res) mustBe Json.obj("message" -> "No unregulated donations found")

        verify(mockService).getTotalUnregulatedDonations(eqTo("abc-123"))
        verifyNoMoreInteractions(mockService)
      }

      "returns 500 with generic message on unexpected exception" in new Setup {
        when(mockService.getTotalUnregulatedDonations(eqTo("abc-123")))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val req = FakeRequest(GET, "/formp-proxy/charities/abc-123/unregulated-donations")
        val res = controller.getTotalUnregulatedDonations("abc-123")(req)

        status(res) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
      }
    }

    "saveUnregulatedDonation" - {

      "returns 200 when service succeeds (happy path)" in new Setup {
        when(mockService.saveUnregulatedDonation(eqTo("abc-123"), eqTo(SaveUnregulatedDonationRequest(123, BigDecimal("1234.56")))))
          .thenReturn(Future.successful(()))

        val req: FakeRequest[SaveUnregulatedDonationRequest] = 
          FakeRequest(POST, "/formp-proxy/charities/abc-123/unregulated-donations")
          .withBody(SaveUnregulatedDonationRequest(123, BigDecimal("1234.56")))

        val res = controller.saveUnregulatedDonation("abc-123")(req)

        status(res) mustBe OK
        contentType(res) mustBe Some(JSON)
        contentAsJson(res) mustBe Json.obj("success" -> true)

        verify(mockService).saveUnregulatedDonation(eqTo("abc-123"), eqTo(SaveUnregulatedDonationRequest(123, BigDecimal("1234.56"))))
      }

      "returns 500 with generic message on unexpected exception" in new Setup {
        when(mockService.saveUnregulatedDonation(eqTo("abc-123"), eqTo(SaveUnregulatedDonationRequest(123, BigDecimal("1234.56")))))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val req: FakeRequest[SaveUnregulatedDonationRequest] = 
          FakeRequest(POST, "/formp-proxy/charities/abc-123/unregulated-donations")
          .withBody(SaveUnregulatedDonationRequest(123, BigDecimal("1234.56")))

        val res = controller.saveUnregulatedDonation("abc-123")(req)

        status(res) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"

        verify(mockService).saveUnregulatedDonation(eqTo("abc-123"), eqTo(SaveUnregulatedDonationRequest(123, BigDecimal("1234.56"))))
      }
    }
  }

  

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)

    val mockService: UnregulatedDonationsService = mock[UnregulatedDonationsService]

    val controller = new UnregulatedDonationsController(fakeAuth, mockService, cc)      
  }
}
