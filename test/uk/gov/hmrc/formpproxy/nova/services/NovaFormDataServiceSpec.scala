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

package uk.gov.hmrc.formpproxy.nova.services

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.formpproxy.nova.models.*
import uk.gov.hmrc.formpproxy.nova.repositories.NovaSource

import scala.concurrent.Future

class NovaFormDataServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private trait Setup {
    val mockRepo: NovaSource         = mock[NovaSource]
    val service: NovaFormDataService = new NovaFormDataServiceImpl(mockRepo)
  }

  "getFormData" - {
    "delegates to repository" in new Setup {
      when(mockRepo.getFormData(eqTo(12345L), eqTo(Seq("/client"))))
        .thenReturn(Future.successful(None))

      service.getFormData(12345L, Seq("/client")).futureValue mustBe None
      verify(mockRepo).getFormData(eqTo(12345L), eqTo(Seq("/client")))
    }

    "propagates failure" in new Setup {
      when(mockRepo.getFormData(any[Long], any[Seq[String]]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(service.getFormData(12345L, Seq("/client")).failed) { ex =>
        ex.getMessage mustBe "boom"
      }
    }
  }

  "storeFormData" - {
    "delegates to repository" in new Setup {
      val request = StoreFormDataRequest("<data/>", 3L)
      when(mockRepo.storeFormData(eqTo(12345L), eqTo("/client"), eqTo(request)))
        .thenReturn(Future.successful(StoreFormDataResponse(12345L, "/client", 4L)))

      service.storeFormData(12345L, "/client", request).futureValue mustBe StoreFormDataResponse(12345L, "/client", 4L)
    }

    "propagates failure" in new Setup {
      when(mockRepo.storeFormData(any[Long], any[String], any[StoreFormDataRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(service.storeFormData(12345L, "/client", StoreFormDataRequest("<data/>", 1L)).failed) { ex =>
        ex.getMessage mustBe "boom"
      }
    }
  }

  "getNovaNotificationRef" - {
    "delegates to repository" in new Setup {
      val request = NotificationRefsRequest(1, "cred-123", 3L)
      when(mockRepo.getNovaNotificationRef(eqTo(12345L), eqTo(request)))
        .thenReturn(Future.successful(NotificationRefsResponse(26, 100001L, 100001L, 4L)))

      service.getNovaNotificationRef(12345L, request).futureValue mustBe NotificationRefsResponse(
        26,
        100001L,
        100001L,
        4L
      )
    }

    "propagates failure" in new Setup {
      when(mockRepo.getNovaNotificationRef(any[Long], any[NotificationRefsRequest]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(service.getNovaNotificationRef(12345L, NotificationRefsRequest(1, "cred-123", 1L)).failed) { ex =>
        ex.getMessage mustBe "boom"
      }
    }
  }
}
