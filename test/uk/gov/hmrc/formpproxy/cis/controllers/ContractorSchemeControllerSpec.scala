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
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, ControllerComponents, PlayBodyParsers, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.FakeAuthAction
import uk.gov.hmrc.formpproxy.cis.models.ContractorScheme
import uk.gov.hmrc.formpproxy.cis.services.ContractorSchemeService
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class ContractorSchemeControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "ContractorSchemeController.getScheme" - {

    "returns 200 with ContractorScheme when service succeeds (happy path)" in new Setup {
      val scheme = testScheme
      when(mockService.getScheme(eqTo("abc-123")))
        .thenReturn(Future.successful(Some(scheme)))

      val req: FakeRequest[AnyContent] = FakeRequest(GET, "/scheme/abc-123")
      val res: Future[Result]  = controller.getScheme("abc-123")(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      contentAsJson(res) mustBe Json.toJson(scheme)
      verify(mockService).getScheme(eqTo("abc-123"))
      verifyNoMoreInteractions(mockService)
    }

    "returns 404 when service returns None" in new Setup {
      when(mockService.getScheme(eqTo("unknown-123")))
        .thenReturn(Future.successful(None))

      val req = FakeRequest(GET, "/scheme/unknown-123")
      val res = controller.getScheme("unknown-123")(req)

      status(res) mustBe NOT_FOUND
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "message").as[String] mustBe "Scheme not found"
      verify(mockService).getScheme(eqTo("unknown-123"))
      verifyNoMoreInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val err = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)
      when(mockService.getScheme(eqTo("abc-123")))
        .thenReturn(Future.failed(err))

      val req = FakeRequest(GET, "/scheme/abc-123")
      val res = controller.getScheme("abc-123")(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
      verify(mockService).getScheme(eqTo("abc-123"))
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.getScheme(eqTo("abc-123")))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = FakeRequest(GET, "/scheme/abc-123")
      val res = controller.getScheme("abc-123")(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
      verify(mockService).getScheme(eqTo("abc-123"))
      verifyNoMoreInteractions(mockService)
    }

    "serializes ContractorScheme with all fields correctly" in new Setup {
      val scheme = ContractorScheme(
        schemeId = 123,
        instanceId = "abc-123",
        accountsOfficeReference = "123456789",
        taxOfficeNumber = "0001",
        taxOfficeReference = "AB12345",
        utr = Some("1234567890"),
        name = Some("Test Contractor"),
        emailAddress = Some("test@example.com"),
        displayWelcomePage = Some("Y"),
        prePopCount = Some(5),
        prePopSuccessful = Some("Y"),
        subcontractorCounter = Some(10),
        verificationBatchCounter = Some(2),
        lastUpdate = Some(Instant.parse("2025-12-04T10:15:30Z")),
        version = Some(1)
      )
      when(mockService.getScheme(eqTo("abc-123")))
        .thenReturn(Future.successful(Some(scheme)))

      val req = FakeRequest(GET, "/scheme/abc-123")
      val res = controller.getScheme("abc-123")(req)

      status(res) mustBe OK
      val json = contentAsJson(res)
      (json \ "schemeId").as[Int] mustBe 123
      (json \ "instanceId").as[String] mustBe "abc-123"
      (json \ "accountsOfficeReference").as[String] mustBe "123456789"
      (json \ "taxOfficeNumber").as[String] mustBe "0001"
      (json \ "taxOfficeReference").as[String] mustBe "AB12345"
      (json \ "utr").asOpt[String] mustBe Some("1234567890")
      (json \ "name").asOpt[String] mustBe Some("Test Contractor")
      (json \ "emailAddress").asOpt[String] mustBe Some("test@example.com")
      (json \ "displayWelcomePage").asOpt[String] mustBe Some("Y")
      (json \ "prePopCount").asOpt[Int] mustBe Some(5)
      (json \ "prePopSuccessful").asOpt[String] mustBe Some("Y")
      (json \ "subcontractorCounter").asOpt[Int] mustBe Some(10)
      (json \ "verificationBatchCounter").asOpt[Int] mustBe Some(2)
      (json \ "version").asOpt[Int] mustBe Some(1)
    }

    "serializes ContractorScheme with null optional fields" in new Setup {
      val scheme = ContractorScheme(
        schemeId = 456,
        instanceId = "def-456",
        accountsOfficeReference = "987654321",
        taxOfficeNumber = "0002",
        taxOfficeReference = "CD67890",
        utr = None,
        name = None,
        emailAddress = None,
        displayWelcomePage = None,
        prePopCount = None,
        prePopSuccessful = None,
        subcontractorCounter = None,
        verificationBatchCounter = None,
        lastUpdate = None,
        version = None
      )
      when(mockService.getScheme(eqTo("def-456")))
        .thenReturn(Future.successful(Some(scheme)))

      val req = FakeRequest(GET, "/scheme/def-456")
      val res = controller.getScheme("def-456")(req)

      status(res) mustBe OK
      val json = contentAsJson(res)
      (json \ "schemeId").as[Int] mustBe 456
      (json \ "instanceId").as[String] mustBe "def-456"
      (json \ "utr").asOpt[String] mustBe None
      (json \ "name").asOpt[String] mustBe None
      (json \ "emailAddress").asOpt[String] mustBe None
    }
  }

  trait Setup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private val fakeAuthAction           = new FakeAuthAction(parsers)

    val mockService: ContractorSchemeService = mock[ContractorSchemeService]
    val controller                           = new ContractorSchemeController(fakeAuthAction, mockService, cc)

    val testScheme: ContractorScheme = ContractorScheme(
      schemeId = 789,
      instanceId = "abc-123",
      accountsOfficeReference = "111111111",
      taxOfficeNumber = "0003",
      taxOfficeReference = "EF23456",
      utr = Some("9876543210"),
      name = Some("Another Contractor"),
      emailAddress = Some("another@example.com"),
      displayWelcomePage = Some("N"),
      prePopCount = Some(3),
      prePopSuccessful = Some("N"),
      subcontractorCounter = Some(5),
      verificationBatchCounter = Some(1),
      lastUpdate = Some(Instant.parse("2025-11-01T14:30:00Z")),
      version = Some(2)
    )
  }
}