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
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.FakeAuthAction
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.nova.models.*
import uk.gov.hmrc.formpproxy.nova.repositories.{FormAlreadyUpdatedException, FormNotFoundException}
import uk.gov.hmrc.formpproxy.nova.services.NovaFormDataService

import scala.concurrent.Future

class NovaFormDataControllerSpec extends SpecBase {

  trait Setup {
    val mockService: NovaFormDataService = mock[NovaFormDataService]
    val auth: FakeAuthAction             = new FakeAuthAction(cc.parsers)
    lazy val controller                  = new NovaFormDataController(auth, mockService, cc)
  }

  def setup: Setup = new Setup {}

  private val sampleFormData = FormDataResponse(
    formId = 12345L,
    userId = "cred-123",
    formType = "NOVA",
    versionId = 3L,
    creationTimestamp = "2026-03-01T10:00:00Z",
    formStatus = "UNSUBMITTED",
    sections = Seq(FormSection("/client", Some("<Client>data</Client>"), Some("COMPLETE")))
  )

  "getFormData" - {
    "returns 200 with form data" in {
      val s = setup; import s.*
      when(mockService.getFormData(eqTo(12345L), eqTo(Seq("/client"))))
        .thenReturn(Future.successful(Some(sampleFormData)))

      val result = controller.getFormData(12345L, Seq("/client"))(
        FakeRequest(GET, "/nova/forms/12345/data?formDataIds=/client")
      )

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(sampleFormData)
    }

    "returns 404 when form not found" in {
      val s = setup; import s.*
      when(mockService.getFormData(eqTo(99999L), any[Seq[String]]))
        .thenReturn(Future.successful(None))

      val result = controller.getFormData(99999L, Seq("/client"))(
        FakeRequest(GET, "/nova/forms/99999/data?formDataIds=/client")
      )

      status(result) mustBe NOT_FOUND
      (contentAsJson(result) \ "code").as[String] mustBe "FORM_NOT_FOUND"
    }

    "returns 400 when formDataIds is empty" in {
      val s = setup; import s.*

      val result = controller.getFormData(12345L, Seq.empty)(
        FakeRequest(GET, "/nova/forms/12345/data")
      )

      status(result) mustBe BAD_REQUEST
    }

    "returns 400 when formDataIds contain unsafe characters" in {
      val s = setup; import s.*

      val result = controller.getFormData(12345L, Seq("' OR '1'='1"))(
        FakeRequest(GET, "/nova/forms/12345/data?formDataIds=' OR '1'='1")
      )

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] must include("invalid characters")
    }

    "returns 500 when service throws" in {
      val s = setup; import s.*
      when(mockService.getFormData(any[Long], any[Seq[String]]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val result = controller.getFormData(12345L, Seq("/client"))(
        FakeRequest(GET, "/nova/forms/12345/data?formDataIds=/client")
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "storeFormData" - {
    "returns 200 with updated versionId" in {
      val s = setup; import s.*
      when(mockService.storeFormData(eqTo(12345L), eqTo("/client"), any[StoreFormDataRequest]))
        .thenReturn(Future.successful(StoreFormDataResponse(12345L, "/client", 4L)))

      val body   = StoreFormDataRequest("<Client>data</Client>", 3L)
      val result = controller.storeFormData(12345L, "/client")(
        withJson(FakeRequest(PUT, "/nova/forms/12345/data/%2Fclient"), body)
      )

      status(result) mustBe OK
      (contentAsJson(result) \ "versionId").as[Long] mustBe 4L
      (contentAsJson(result) \ "formDataId").as[String] mustBe "/client"
    }

    "returns 404 when form not found (ORA-20001)" in {
      val s = setup; import s.*
      when(mockService.storeFormData(any[Long], any[String], any[StoreFormDataRequest]))
        .thenReturn(Future.failed(FormNotFoundException("not found")))

      val body   = StoreFormDataRequest("<data/>", 1L)
      val result = controller.storeFormData(99999L, "/client")(
        withJson(FakeRequest(PUT, "/nova/forms/99999/data/%2Fclient"), body)
      )

      status(result) mustBe NOT_FOUND
    }

    "returns 409 when version conflict (ORA-20000)" in {
      val s = setup; import s.*
      when(mockService.storeFormData(any[Long], any[String], any[StoreFormDataRequest]))
        .thenReturn(Future.failed(FormAlreadyUpdatedException("stale")))

      val body   = StoreFormDataRequest("<data/>", 0L)
      val result = controller.storeFormData(12345L, "/client")(
        withJson(FakeRequest(PUT, "/nova/forms/12345/data/%2Fclient"), body)
      )

      status(result) mustBe CONFLICT
    }

    "returns 500 when service throws unexpected error" in {
      val s = setup; import s.*
      when(mockService.storeFormData(any[Long], any[String], any[StoreFormDataRequest]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val body   = StoreFormDataRequest("<data/>", 1L)
      val result = controller.storeFormData(12345L, "/client")(
        withJson(FakeRequest(PUT, "/nova/forms/12345/data/%2Fclient"), body)
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "getNovaNotificationRef" - {
    "returns 400 when requestRefNumCount is zero" in {
      val s = setup; import s.*

      val body   = NotificationRefsRequest(0, "cred-123", 3L)
      val result = controller.getNovaNotificationRef(12345L)(
        withJson(FakeRequest(POST, "/nova/forms/12345/notification-refs"), body)
      )

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] must include("requestRefNumCount")
    }

    "returns 400 when requestRefNumCount is negative" in {
      val s = setup; import s.*

      val body   = NotificationRefsRequest(-1, "cred-123", 3L)
      val result = controller.getNovaNotificationRef(12345L)(
        withJson(FakeRequest(POST, "/nova/forms/12345/notification-refs"), body)
      )

      status(result) mustBe BAD_REQUEST
    }

    "returns 200 with notification refs" in {
      val s = setup; import s.*
      when(mockService.getNovaNotificationRef(eqTo(12345L), any[NotificationRefsRequest]))
        .thenReturn(Future.successful(NotificationRefsResponse(26, 100001L, 100001L, 4L)))

      val body   = NotificationRefsRequest(1, "cred-123", 3L)
      val result = controller.getNovaNotificationRef(12345L)(
        withJson(FakeRequest(POST, "/nova/forms/12345/notification-refs"), body)
      )

      status(result) mustBe OK
      (contentAsJson(result) \ "taxYear").as[Int] mustBe 26
      (contentAsJson(result) \ "minReference").as[Long] mustBe 100001L
      (contentAsJson(result) \ "maxReference").as[Long] mustBe 100001L
      (contentAsJson(result) \ "version").as[Long] mustBe 4L
    }

    "returns 404 when form not found (ORA-20001)" in {
      val s = setup; import s.*
      when(mockService.getNovaNotificationRef(any[Long], any[NotificationRefsRequest]))
        .thenReturn(Future.failed(FormNotFoundException("not found")))

      val body   = NotificationRefsRequest(1, "cred-123", 1L)
      val result = controller.getNovaNotificationRef(99999L)(
        withJson(FakeRequest(POST, "/nova/forms/99999/notification-refs"), body)
      )

      status(result) mustBe NOT_FOUND
    }

    "returns 409 when version conflict (ORA-20000)" in {
      val s = setup; import s.*
      when(mockService.getNovaNotificationRef(any[Long], any[NotificationRefsRequest]))
        .thenReturn(Future.failed(FormAlreadyUpdatedException("stale")))

      val body   = NotificationRefsRequest(1, "cred-123", 0L)
      val result = controller.getNovaNotificationRef(12345L)(
        withJson(FakeRequest(POST, "/nova/forms/12345/notification-refs"), body)
      )

      status(result) mustBe CONFLICT
    }

    "returns 500 when service throws unexpected error" in {
      val s = setup; import s.*
      when(mockService.getNovaNotificationRef(any[Long], any[NotificationRefsRequest]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val body   = NotificationRefsRequest(1, "cred-123", 1L)
      val result = controller.getNovaNotificationRef(12345L)(
        withJson(FakeRequest(POST, "/nova/forms/12345/notification-refs"), body)
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
