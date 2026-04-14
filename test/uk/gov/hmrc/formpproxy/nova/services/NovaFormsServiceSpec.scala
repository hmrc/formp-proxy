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

class NovaFormsServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private trait Setup {
    val mockRepo: NovaSource      = mock[NovaSource]
    val service: NovaFormsService = new NovaFormsServiceImpl(mockRepo)
  }

  "getForms" - {
    "passes defaults when formType and formStatus not provided" in new Setup {
      when(mockRepo.getForms(eqTo("cred-123"), eqTo("NOVA"), eqTo("'UNSUBMITTED'")))
        .thenReturn(Future.successful(FormsResponse(Seq.empty)))

      service.getForms("cred-123", None, None).futureValue mustBe FormsResponse(Seq.empty)
      verify(mockRepo).getForms(eqTo("cred-123"), eqTo("NOVA"), eqTo("'UNSUBMITTED'"))
    }

    "quotes caller-provided formStatus for SQL IN clause" in new Setup {
      when(mockRepo.getForms(eqTo("cred-123"), eqTo("NOVA"), eqTo("'SUBMITTING','SUBMITTED'")))
        .thenReturn(Future.successful(FormsResponse(Seq.empty)))

      service.getForms("cred-123", Some("NOVA"), Some("SUBMITTING,SUBMITTED")).futureValue
      verify(mockRepo).getForms(eqTo("cred-123"), eqTo("NOVA"), eqTo("'SUBMITTING','SUBMITTED'"))
    }

    "quotes single formStatus value" in new Setup {
      when(mockRepo.getForms(eqTo("cred-123"), eqTo("NOVA"), eqTo("'UNSUBMITTED'")))
        .thenReturn(Future.successful(FormsResponse(Seq.empty)))

      service.getForms("cred-123", None, Some("UNSUBMITTED")).futureValue
      verify(mockRepo).getForms(eqTo("cred-123"), eqTo("NOVA"), eqTo("'UNSUBMITTED'"))
    }

    "propagates failure" in new Setup {
      when(mockRepo.getForms(any[String], any[String], any[String]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(service.getForms("cred-123", None, None).failed) { ex =>
        ex.getMessage mustBe "boom"
      }
    }
  }

  "getForm" - {
    "delegates to repository" in new Setup {
      when(mockRepo.getForm(eqTo(12345L)))
        .thenReturn(Future.successful(None))

      service.getForm(12345L).futureValue mustBe None
      verify(mockRepo).getForm(eqTo(12345L))
    }
  }

  "createForm" - {
    "delegates to repository with userId" in new Setup {
      when(mockRepo.createForm(eqTo("cred-123")))
        .thenReturn(Future.successful(StoreFormResponse(12345L, 0L)))

      service.createForm(CreateFormRequest("cred-123")).futureValue mustBe StoreFormResponse(12345L, 0L)
      verify(mockRepo).createForm(eqTo("cred-123"))
    }
  }

  "updateForm" - {
    "delegates to repository" in new Setup {
      val request = UpdateFormRequest("cred-123", "UNSUBMITTED", 1L)
      when(mockRepo.updateForm(eqTo(12345L), eqTo(request)))
        .thenReturn(Future.successful(StoreFormResponse(12345L, 2L)))

      service.updateForm(12345L, request).futureValue mustBe StoreFormResponse(12345L, 2L)
    }
  }

  "updateFormStatus" - {
    "delegates to repository" in new Setup {
      val request = UpdateFormStatusRequest("SUBMITTING", None, None, 2L)
      when(mockRepo.updateFormStatus(eqTo(12345L), eqTo(request)))
        .thenReturn(Future.successful(StoreFormResponse(12345L, 3L)))

      service.updateFormStatus(12345L, request).futureValue mustBe StoreFormResponse(12345L, 3L)
    }
  }

  "deleteForm" - {
    "delegates to repository" in new Setup {
      when(mockRepo.deleteForm(eqTo(12345L)))
        .thenReturn(Future.successful(()))

      service.deleteForm(12345L).futureValue mustBe ()
      verify(mockRepo).deleteForm(eqTo(12345L))
    }
  }
}
