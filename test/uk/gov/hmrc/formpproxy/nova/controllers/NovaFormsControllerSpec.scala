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

package uk.gov.hmrc.formpproxy.nova.controllers

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.FakeAuthAction
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.nova.models.*
import uk.gov.hmrc.formpproxy.nova.repositories.{FormAlreadyUpdatedException, FormNotFoundException}
import uk.gov.hmrc.formpproxy.nova.services.NovaFormsService

import scala.concurrent.Future

class NovaFormsControllerSpec extends SpecBase {

  trait Setup {
    val mockService: NovaFormsService = mock[NovaFormsService]
    val auth: FakeAuthAction          = new FakeAuthAction(cc.parsers)
    lazy val controller               = new NovaFormsController(auth, mockService, cc)
  }

  def setup: Setup = new Setup {}

  private val sampleFormSummary = FormSummary(
    formId = 12345L,
    userId = "cred-GGsomeCredId001",
    formType = "NOVA",
    versionId = 1L,
    creationTimestamp = "2026-03-01T10:00:00Z",
    formStatus = "UNSUBMITTED"
  )

  private val sampleFormDetail = FormDetail(
    formId = 12345L,
    userId = "cred-GGsomeCredId001",
    formType = "NOVA",
    versionId = 2L,
    creationTimestamp = "2026-03-01T10:00:00Z",
    formStatus = "UNSUBMITTED",
    submissionDatetime = None,
    submissionReference = None
  )

  "getForms" - {
    "returns 200 with forms" in {
      val s        = setup; import s.*
      val response = FormsResponse(Seq(sampleFormSummary))
      when(mockService.getForms(eqTo("cred-123"), eqTo(None), eqTo(None)))
        .thenReturn(Future.successful(response))

      val result = controller.getForms("cred-123", None, None)(FakeRequest(GET, "/nova/forms?userId=cred-123"))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(response)
    }

    "returns 200 with empty list" in {
      val s = setup; import s.*
      when(mockService.getForms(eqTo("cred-123"), eqTo(None), eqTo(None)))
        .thenReturn(Future.successful(FormsResponse(Seq.empty)))

      val result = controller.getForms("cred-123", None, None)(FakeRequest(GET, "/nova/forms?userId=cred-123"))

      status(result) mustBe OK
      (contentAsJson(result) \ "forms").as[Seq[FormSummary]] mustBe empty
    }

    "returns 500 when service throws" in {
      val s = setup; import s.*
      when(mockService.getForms(any[String], any[Option[String]], any[Option[String]]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val result = controller.getForms("cred-123", None, None)(FakeRequest(GET, "/nova/forms?userId=cred-123"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "getForm" - {
    "returns 200 with form detail" in {
      val s = setup; import s.*
      when(mockService.getForm(eqTo(12345L)))
        .thenReturn(Future.successful(Some(sampleFormDetail)))

      val result = controller.getForm(12345L)(FakeRequest(GET, "/nova/forms/12345"))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(sampleFormDetail)
    }

    "returns 404 when form not found" in {
      val s = setup; import s.*
      when(mockService.getForm(eqTo(99999L)))
        .thenReturn(Future.successful(None))

      val result = controller.getForm(99999L)(FakeRequest(GET, "/nova/forms/99999"))

      status(result) mustBe NOT_FOUND
      (contentAsJson(result) \ "code").as[String] mustBe "FORM_NOT_FOUND"
    }

    "returns 500 when service throws" in {
      val s = setup; import s.*
      when(mockService.getForm(any[Long]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val result = controller.getForm(12345L)(FakeRequest(GET, "/nova/forms/12345"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "createForm" - {
    "returns 201 with formId and versionId" in {
      val s = setup; import s.*
      when(mockService.createForm(any[CreateFormRequest]))
        .thenReturn(Future.successful(StoreFormResponse(12345L, 0L)))

      val result = controller.createForm()(postJson("/nova/forms", CreateFormRequest("cred-123")))

      status(result) mustBe CREATED
      (contentAsJson(result) \ "formId").as[Long] mustBe 12345L
      (contentAsJson(result) \ "versionId").as[Long] mustBe 0L
    }

    "returns 400 when userId is blank" in {
      val s = setup; import s.*

      val result = controller.createForm()(postJson("/nova/forms", CreateFormRequest("  ")))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] must include("userId")
    }

    "returns 500 when service throws" in {
      val s = setup; import s.*
      when(mockService.createForm(any[CreateFormRequest]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val result = controller.createForm()(postJson("/nova/forms", CreateFormRequest("cred-123")))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "updateForm" - {
    "returns 200 with updated versionId" in {
      val s = setup; import s.*
      when(mockService.updateForm(eqTo(12345L), any[UpdateFormRequest]))
        .thenReturn(Future.successful(StoreFormResponse(12345L, 2L)))

      val body   = UpdateFormRequest("cred-123", "UNSUBMITTED", 1L)
      val result = controller.updateForm(12345L)(withJson(FakeRequest(PUT, "/nova/forms/12345"), body))

      status(result) mustBe OK
      (contentAsJson(result) \ "versionId").as[Long] mustBe 2L
    }

    "returns 404 when form not found (ORA-20001)" in {
      val s = setup; import s.*
      when(mockService.updateForm(eqTo(99999L), any[UpdateFormRequest]))
        .thenReturn(Future.failed(FormNotFoundException("not found")))

      val body   = UpdateFormRequest("cred-123", "UNSUBMITTED", 1L)
      val result = controller.updateForm(99999L)(withJson(FakeRequest(PUT, "/nova/forms/99999"), body))

      status(result) mustBe NOT_FOUND
      (contentAsJson(result) \ "code").as[String] mustBe "FORM_NOT_FOUND"
    }

    "returns 409 when version conflict (ORA-20000)" in {
      val s = setup; import s.*
      when(mockService.updateForm(eqTo(12345L), any[UpdateFormRequest]))
        .thenReturn(Future.failed(FormAlreadyUpdatedException("stale")))

      val body   = UpdateFormRequest("cred-123", "UNSUBMITTED", 0L)
      val result = controller.updateForm(12345L)(withJson(FakeRequest(PUT, "/nova/forms/12345"), body))

      status(result) mustBe CONFLICT
      (contentAsJson(result) \ "code").as[String] mustBe "FORM_ALREADY_UPDATED"
    }

    "returns 500 when service throws unexpected error" in {
      val s = setup; import s.*
      when(mockService.updateForm(any[Long], any[UpdateFormRequest]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val body   = UpdateFormRequest("cred-123", "UNSUBMITTED", 1L)
      val result = controller.updateForm(12345L)(withJson(FakeRequest(PUT, "/nova/forms/12345"), body))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "updateFormStatus" - {
    "returns 200 with updated versionId" in {
      val s = setup; import s.*
      when(mockService.updateFormStatus(eqTo(12345L), any[UpdateFormStatusRequest]))
        .thenReturn(Future.successful(StoreFormResponse(12345L, 3L)))

      val body   = UpdateFormStatusRequest("SUBMITTING", None, None, 2L)
      val result = controller.updateFormStatus(12345L)(withJson(FakeRequest(PUT, "/nova/forms/12345/status"), body))

      status(result) mustBe OK
      (contentAsJson(result) \ "versionId").as[Long] mustBe 3L
    }

    "returns 404 when form not found (ORA-20001)" in {
      val s = setup; import s.*
      when(mockService.updateFormStatus(eqTo(99999L), any[UpdateFormStatusRequest]))
        .thenReturn(Future.failed(FormNotFoundException("not found")))

      val body   = UpdateFormStatusRequest("SUBMITTING", None, None, 1L)
      val result = controller.updateFormStatus(99999L)(withJson(FakeRequest(PUT, "/nova/forms/99999/status"), body))

      status(result) mustBe NOT_FOUND
    }

    "returns 409 when version conflict (ORA-20000)" in {
      val s = setup; import s.*
      when(mockService.updateFormStatus(eqTo(12345L), any[UpdateFormStatusRequest]))
        .thenReturn(Future.failed(FormAlreadyUpdatedException("stale")))

      val body   = UpdateFormStatusRequest("SUBMITTING", None, None, 0L)
      val result = controller.updateFormStatus(12345L)(withJson(FakeRequest(PUT, "/nova/forms/12345/status"), body))

      status(result) mustBe CONFLICT
    }

    "returns 500 when service throws unexpected error" in {
      val s = setup; import s.*
      when(mockService.updateFormStatus(any[Long], any[UpdateFormStatusRequest]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val body   = UpdateFormStatusRequest("SUBMITTING", None, None, 1L)
      val result = controller.updateFormStatus(12345L)(withJson(FakeRequest(PUT, "/nova/forms/12345/status"), body))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "deleteForm" - {
    "returns 204 on successful deletion" in {
      val s = setup; import s.*
      when(mockService.deleteForm(eqTo(12345L)))
        .thenReturn(Future.successful(()))

      val result = controller.deleteForm(12345L)(FakeRequest(DELETE, "/nova/forms/12345"))

      status(result) mustBe NO_CONTENT
      verify(mockService).deleteForm(eqTo(12345L))
      verifyNoMoreInteractions(mockService)
    }

    "returns 404 when form not found (ORA-20001)" in {
      val s = setup; import s.*
      when(mockService.deleteForm(eqTo(99999L)))
        .thenReturn(Future.failed(FormNotFoundException("not found")))

      val result = controller.deleteForm(99999L)(FakeRequest(DELETE, "/nova/forms/99999"))

      status(result) mustBe NOT_FOUND
      (contentAsJson(result) \ "code").as[String] mustBe "FORM_NOT_FOUND"
    }

    "returns 500 when service throws" in {
      val s = setup; import s.*
      when(mockService.deleteForm(any[Long]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val result = controller.deleteForm(12345L)(FakeRequest(DELETE, "/nova/forms/12345"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
