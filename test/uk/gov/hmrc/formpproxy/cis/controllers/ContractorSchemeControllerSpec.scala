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
import uk.gov.hmrc.formpproxy.cis.models.{Company, ContractorScheme, CreateContractorSchemeParams, Partnership, SoleTrader, Trust, UpdateContractorSchemeParams}
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
      val res: Future[Result]          = controller.getScheme("abc-123")(req)

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

  "ContractorSchemeController.updateScheme" - {

    "returns 200 with version when service succeeds (happy path)" in new Setup {
      val updateParams = testUpdateParams
      when(mockService.updateScheme(eqTo(updateParams)))
        .thenReturn(Future.successful(2))

      val req                 = FakeRequest(PUT, "/scheme").withBody(Json.toJson(updateParams))
      val res: Future[Result] = controller.updateScheme(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "version").as[Int] mustBe 2
      verify(mockService).updateScheme(eqTo(updateParams))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is invalid" in new Setup {
      val invalidJson         = Json.obj("schemeId" -> 123)
      val req                 = FakeRequest(PUT, "/scheme").withBody(invalidJson)
      val res: Future[Result] = controller.updateScheme(req)

      status(res) mustBe BAD_REQUEST
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "returns 400 when JSON body has wrong types" in new Setup {
      val invalidJson         = Json.obj(
        "schemeId"                -> "not-a-number",
        "instanceId"              -> "abc-123",
        "accountsOfficeReference" -> "123456789",
        "taxOfficeNumber"         -> "0001",
        "taxOfficeReference"      -> "AB12345"
      )
      val req                 = FakeRequest(PUT, "/scheme").withBody(invalidJson)
      val res: Future[Result] = controller.updateScheme(req)

      status(res) mustBe BAD_REQUEST
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val updateParams = testUpdateParams
      val err          = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)
      when(mockService.updateScheme(eqTo(updateParams)))
        .thenReturn(Future.failed(err))

      val req                 = FakeRequest(PUT, "/scheme").withBody(Json.toJson(updateParams))
      val res: Future[Result] = controller.updateScheme(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
      verify(mockService).updateScheme(eqTo(updateParams))
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val updateParams = testUpdateParams
      when(mockService.updateScheme(eqTo(updateParams)))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req                 = FakeRequest(PUT, "/scheme").withBody(Json.toJson(updateParams))
      val res: Future[Result] = controller.updateScheme(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
      verify(mockService).updateScheme(eqTo(updateParams))
      verifyNoMoreInteractions(mockService)
    }

    "accepts UpdateContractorSchemeParams with no optional fields" in new Setup {
      val updateParams = testUpdateParamsMinimal
      when(mockService.updateScheme(eqTo(updateParams)))
        .thenReturn(Future.successful(3))

      val req                 = FakeRequest(PUT, "/scheme").withBody(Json.toJson(updateParams))
      val res: Future[Result] = controller.updateScheme(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "version").as[Int] mustBe 3
      verify(mockService).updateScheme(eqTo(updateParams))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ContractorSchemeController.createScheme" - {

    "returns 200 with schemeId when service succeeds (happy path)" in new Setup {
      val createParams = testCreateParams
      when(mockService.createScheme(eqTo(createParams)))
        .thenReturn(Future.successful(123))

      val req                 = FakeRequest(POST, "/scheme").withBody(Json.toJson(createParams))
      val res: Future[Result] = controller.createScheme(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "schemeId").as[Int] mustBe 123
      verify(mockService).createScheme(eqTo(createParams))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is invalid" in new Setup {
      val invalidJson         = Json.obj("instanceId" -> "abc-123")
      val req                 = FakeRequest(POST, "/scheme").withBody(invalidJson)
      val res: Future[Result] = controller.createScheme(req)

      status(res) mustBe BAD_REQUEST
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "returns 400 when JSON body has wrong types" in new Setup {
      val invalidJson         = Json.obj(
        "instanceId"              -> 123,
        "accountsOfficeReference" -> "123456789",
        "taxOfficeNumber"         -> "0001",
        "taxOfficeReference"      -> "AB12345"
      )
      val req                 = FakeRequest(POST, "/scheme").withBody(invalidJson)
      val res: Future[Result] = controller.createScheme(req)

      status(res) mustBe BAD_REQUEST
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val createParams = testCreateParams
      val err          = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)
      when(mockService.createScheme(eqTo(createParams)))
        .thenReturn(Future.failed(err))

      val req                 = FakeRequest(POST, "/scheme").withBody(Json.toJson(createParams))
      val res: Future[Result] = controller.createScheme(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
      verify(mockService).createScheme(eqTo(createParams))
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      val createParams = testCreateParams
      when(mockService.createScheme(eqTo(createParams)))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req                 = FakeRequest(POST, "/scheme").withBody(Json.toJson(createParams))
      val res: Future[Result] = controller.createScheme(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
      verify(mockService).createScheme(eqTo(createParams))
      verifyNoMoreInteractions(mockService)
    }

    "accepts CreateContractorSchemeParams with no optional fields" in new Setup {
      val createParams = testCreateParamsMinimal
      when(mockService.createScheme(eqTo(createParams)))
        .thenReturn(Future.successful(789))

      val req                 = FakeRequest(POST, "/scheme").withBody(Json.toJson(createParams))
      val res: Future[Result] = controller.createScheme(req)

      status(res) mustBe CREATED
      (contentAsJson(res) \ "schemeId").as[Int] mustBe 789
      verify(mockService).createScheme(eqTo(createParams))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ContractorSchemeController.updateSchemeVersion" - {

    "returns 200 with version when service succeeds (happy path)" in new Setup {
      when(mockService.updateSchemeVersion(eqTo("abc-123"), eqTo(1)))
        .thenReturn(Future.successful(2))

      val req                 = FakeRequest(PUT, "/scheme/abc-123/version").withBody(Json.obj("version" -> 1))
      val res: Future[Result] = controller.updateSchemeVersion("abc-123")(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "version").as[Int] mustBe 2
      verify(mockService).updateSchemeVersion(eqTo("abc-123"), eqTo(1))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is invalid" in new Setup {
      val invalidJson         = Json.obj("wrongField" -> 1)
      val req                 = FakeRequest(PUT, "/scheme/abc-123/version").withBody(invalidJson)
      val res: Future[Result] = controller.updateSchemeVersion("abc-123")(req)

      status(res) mustBe BAD_REQUEST
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "returns 400 when JSON body has wrong types" in new Setup {
      val invalidJson         = Json.obj("version" -> "not-a-number")
      val req                 = FakeRequest(PUT, "/scheme/abc-123/version").withBody(invalidJson)
      val res: Future[Result] = controller.updateSchemeVersion("abc-123")(req)

      status(res) mustBe BAD_REQUEST
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val err = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)
      when(mockService.updateSchemeVersion(eqTo("abc-123"), eqTo(1)))
        .thenReturn(Future.failed(err))

      val req                 = FakeRequest(PUT, "/scheme/abc-123/version").withBody(Json.obj("version" -> 1))
      val res: Future[Result] = controller.updateSchemeVersion("abc-123")(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
      verify(mockService).updateSchemeVersion(eqTo("abc-123"), eqTo(1))
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.updateSchemeVersion(eqTo("abc-123"), eqTo(1)))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req                 = FakeRequest(PUT, "/scheme/abc-123/version").withBody(Json.obj("version" -> 1))
      val res: Future[Result] = controller.updateSchemeVersion("abc-123")(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
      verify(mockService).updateSchemeVersion(eqTo("abc-123"), eqTo(1))
      verifyNoMoreInteractions(mockService)
    }
  }

  "ContractorSchemeController.createSubcontractor" - {

    "returns 200 with version when service succeeds with SoleTrader (happy path)" in new Setup {
      when(mockService.createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1)))
        .thenReturn(Future.successful(2))

      val req                 =
        FakeRequest(POST, "/scheme/123/subcontractor").withBody(
          Json.obj("subcontractorType" -> "soletrader", "version" -> 1)
        )
      val res: Future[Result] = controller.createSubcontractor(123)(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "version").as[Int] mustBe 2
      verify(mockService).createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with version when service succeeds with Company" in new Setup {
      when(mockService.createSubcontractor(eqTo(456), eqTo(Company), eqTo(3)))
        .thenReturn(Future.successful(4))

      val req                 =
        FakeRequest(POST, "/scheme/456/subcontractor").withBody(
          Json.obj("subcontractorType" -> "company", "version" -> 3)
        )
      val res: Future[Result] = controller.createSubcontractor(456)(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "version").as[Int] mustBe 4
      verify(mockService).createSubcontractor(eqTo(456), eqTo(Company), eqTo(3))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with version when service succeeds with Partnership" in new Setup {
      when(mockService.createSubcontractor(eqTo(789), eqTo(Partnership), eqTo(5)))
        .thenReturn(Future.successful(6))

      val req                 =
        FakeRequest(POST, "/scheme/789/subcontractor").withBody(
          Json.obj("subcontractorType" -> "partnership", "version" -> 5)
        )
      val res: Future[Result] = controller.createSubcontractor(789)(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "version").as[Int] mustBe 6
      verify(mockService).createSubcontractor(eqTo(789), eqTo(Partnership), eqTo(5))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with version when service succeeds with Trust" in new Setup {
      when(mockService.createSubcontractor(eqTo(999), eqTo(Trust), eqTo(7)))
        .thenReturn(Future.successful(8))

      val req                 =
        FakeRequest(POST, "/scheme/999/subcontractor").withBody(
          Json.obj("subcontractorType" -> "trust", "version" -> 7)
        )
      val res: Future[Result] = controller.createSubcontractor(999)(req)

      status(res) mustBe OK
      (contentAsJson(res) \ "version").as[Int] mustBe 8
      verify(mockService).createSubcontractor(eqTo(999), eqTo(Trust), eqTo(7))
      verifyNoMoreInteractions(mockService)
    }

    "returns 400 when JSON body is invalid" in new Setup {
      val invalidJson         = Json.obj("subcontractorType" -> "soletrader")
      val req                 = FakeRequest(POST, "/scheme/123/subcontractor").withBody(invalidJson)
      val res: Future[Result] = controller.createSubcontractor(123)(req)

      status(res) mustBe BAD_REQUEST
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "returns 400 when JSON body has wrong types" in new Setup {
      val invalidJson         = Json.obj("subcontractorType" -> "soletrader", "version" -> "not-a-number")
      val req                 = FakeRequest(POST, "/scheme/123/subcontractor").withBody(invalidJson)
      val res: Future[Result] = controller.createSubcontractor(123)(req)

      status(res) mustBe BAD_REQUEST
      contentType(res) mustBe Some(JSON)
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid JSON body"
      verifyNoInteractions(mockService)
    }

    "propagates UpstreamErrorResponse (status & message)" in new Setup {
      val err = UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)
      when(mockService.createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1)))
        .thenReturn(Future.failed(err))

      val req                 =
        FakeRequest(POST, "/scheme/123/subcontractor").withBody(
          Json.obj("subcontractorType" -> "soletrader", "version" -> 1)
        )
      val res: Future[Result] = controller.createSubcontractor(123)(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
      verify(mockService).createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1))
      verifyNoMoreInteractions(mockService)
    }

    "returns 500 with generic message on unexpected exception" in new Setup {
      when(mockService.createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1)))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req                 =
        FakeRequest(POST, "/scheme/123/subcontractor").withBody(
          Json.obj("subcontractorType" -> "soletrader", "version" -> 1)
        )
      val res: Future[Result] = controller.createSubcontractor(123)(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
      verify(mockService).createSubcontractor(eqTo(123), eqTo(SoleTrader), eqTo(1))
      verifyNoMoreInteractions(mockService)
    }
  }

  trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
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

    val testCreateParams: CreateContractorSchemeParams = CreateContractorSchemeParams(
      instanceId = "abc-123",
      accountsOfficeReference = "111111111",
      taxOfficeNumber = "0003",
      taxOfficeReference = "EF23456",
      utr = Some("9876543210"),
      name = Some("Test Contractor"),
      emailAddress = Some("test@example.com"),
      displayWelcomePage = Some("Y"),
      prePopCount = Some(5),
      prePopSuccessful = Some("Y")
    )

    val testCreateParamsMinimal: CreateContractorSchemeParams = CreateContractorSchemeParams(
      instanceId = "ghi-789",
      accountsOfficeReference = "555555555",
      taxOfficeNumber = "0005",
      taxOfficeReference = "XX99999"
    )

    val testUpdateParams: UpdateContractorSchemeParams = UpdateContractorSchemeParams(
      schemeId = 789,
      instanceId = "abc-123",
      accountsOfficeReference = "111111111",
      taxOfficeNumber = "0003",
      taxOfficeReference = "EF23456",
      utr = Some("9876543210"),
      name = Some("Updated Contractor"),
      emailAddress = Some("updated@example.com"),
      displayWelcomePage = Some("Y"),
      prePopCount = Some(10),
      prePopSuccessful = Some("Y"),
      version = Some(1)
    )

    val testUpdateParamsMinimal: UpdateContractorSchemeParams = UpdateContractorSchemeParams(
      schemeId = 456,
      instanceId = "ghi-789",
      accountsOfficeReference = "555555555",
      taxOfficeNumber = "0005",
      taxOfficeReference = "XX99999",
      version = Some(2)
    )
  }
}
