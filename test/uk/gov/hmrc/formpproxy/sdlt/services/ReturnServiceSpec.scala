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

package uk.gov.hmrc.formpproxy.sdlt.services

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.*
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.formpproxy.base.SpecBase
import uk.gov.hmrc.formpproxy.sdlt.models.*
import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository

import scala.concurrent.Future

final class ReturnServiceSpec extends SpecBase {

  private def mkCreateRequest(stornId: String = "STORN12345"): CreateReturnRequest =
    CreateReturnRequest(
      stornId = stornId,
      purchaserIsCompany = "N",
      surNameOrCompanyName = "Smith",
      houseNumber = Some(42),
      addressLine1 = "High Street",
      addressLine2 = Some("Kensington"),
      addressLine3 = Some("London"),
      addressLine4 = None,
      postcode = Some("SW1A 1AA"),
      transactionType = "RESIDENTIAL"
    )

  private def mkGetReturnResponse(
    returnResourceRef: String = "100001",
    stornId: String = "STORN12345"
  ): GetReturnRequest =
    GetReturnRequest(
      stornId = Some(stornId),
      returnResourceRef = Some(returnResourceRef),
      sdltOrganisation = Some(
        SdltOrganisation(
          isReturnUser = Some("Y"),
          storn = Some(stornId)
        )
      ),
      returnInfo = Some(
        ReturnInfo(
          returnID = Some(returnResourceRef),
          storn = Some(stornId),
          status = Some("STARTED")
        )
      ),
      purchaser = None,
      companyDetails = None,
      vendor = None,
      land = None,
      transaction = None,
      returnAgent = None,
      agent = None,
      lease = None,
      taxCalculation = None,
      submission = None,
      submissionErrorDetails = None,
      residency = None
    )

  "ReturnService createSDLTReturn" - {

    "must delegate to repository (happy path)" in {
      val repo                         = mock[SdltFormpRepository]
      val service                      = new ReturnService(repo)
      val request: CreateReturnRequest = mkCreateRequest()

      when(repo.sdltCreateReturn(eqTo(request)))
        .thenReturn(Future.successful("100001"))

      val result: String = service.createSDLTReturn(request).futureValue
      result mustBe "100001"

      verify(repo).sdltCreateReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must return different IDs for different requests" in {
      val repo                          = mock[SdltFormpRepository]
      val service                       = new ReturnService(repo)
      val request1: CreateReturnRequest = mkCreateRequest("STORN11111")
      val request2: CreateReturnRequest = mkCreateRequest("STORN22222")

      when(repo.sdltCreateReturn(eqTo(request1)))
        .thenReturn(Future.successful("100001"))
      when(repo.sdltCreateReturn(eqTo(request2)))
        .thenReturn(Future.successful("100002"))

      service.createSDLTReturn(request1).futureValue mustBe "100001"
      service.createSDLTReturn(request2).futureValue mustBe "100002"

      verify(repo).sdltCreateReturn(eqTo(request1))
      verify(repo).sdltCreateReturn(eqTo(request2))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                         = mock[SdltFormpRepository]
      val service                      = new ReturnService(repo)
      val request: CreateReturnRequest = mkCreateRequest()
      val boom                         = new RuntimeException("database connection failed")

      when(repo.sdltCreateReturn(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createSDLTReturn(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltCreateReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle company purchaser requests" in {
      val repo                         = mock[SdltFormpRepository]
      val service                      = new ReturnService(repo)
      val request: CreateReturnRequest = mkCreateRequest().copy(
        purchaserIsCompany = "Y",
        surNameOrCompanyName = "ABC Property Ltd",
        transactionType = "NON_RESIDENTIAL"
      )

      when(repo.sdltCreateReturn(eqTo(request)))
        .thenReturn(Future.successful("100003"))

      val result: String = service.createSDLTReturn(request).futureValue
      result mustBe "100003"

      verify(repo).sdltCreateReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle minimal request with no optional fields" in {
      val repo                         = mock[SdltFormpRepository]
      val service                      = new ReturnService(repo)
      val request: CreateReturnRequest = mkCreateRequest().copy(
        houseNumber = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None
      )

      when(repo.sdltCreateReturn(eqTo(request)))
        .thenReturn(Future.successful("100004"))

      val result: String = service.createSDLTReturn(request).futureValue
      result mustBe "100004"

      verify(repo).sdltCreateReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle request with all optional fields populated" in {
      val repo                         = mock[SdltFormpRepository]
      val service                      = new ReturnService(repo)
      val request: CreateReturnRequest = mkCreateRequest().copy(
        houseNumber = Some(42),
        addressLine2 = Some("Kensington"),
        addressLine3 = Some("London"),
        addressLine4 = Some("Greater London"),
        postcode = Some("SW1A 1AA")
      )

      when(repo.sdltCreateReturn(eqTo(request)))
        .thenReturn(Future.successful("100005"))

      val result: String = service.createSDLTReturn(request).futureValue
      result mustBe "100005"

      verify(repo).sdltCreateReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle different transaction types" in {
      val repo                                       = mock[SdltFormpRepository]
      val service                                    = new ReturnService(repo)
      val residentialRequest: CreateReturnRequest    = mkCreateRequest().copy(transactionType = "RESIDENTIAL")
      val nonResidentialRequest: CreateReturnRequest = mkCreateRequest().copy(transactionType = "NON_RESIDENTIAL")
      val mixedRequest: CreateReturnRequest          = mkCreateRequest().copy(transactionType = "MIXED")

      when(repo.sdltCreateReturn(eqTo(residentialRequest)))
        .thenReturn(Future.successful("100006"))
      when(repo.sdltCreateReturn(eqTo(nonResidentialRequest)))
        .thenReturn(Future.successful("100007"))
      when(repo.sdltCreateReturn(eqTo(mixedRequest)))
        .thenReturn(Future.successful("100008"))

      service.createSDLTReturn(residentialRequest).futureValue mustBe "100006"
      service.createSDLTReturn(nonResidentialRequest).futureValue mustBe "100007"
      service.createSDLTReturn(mixedRequest).futureValue mustBe "100008"

      verify(repo).sdltCreateReturn(eqTo(residentialRequest))
      verify(repo).sdltCreateReturn(eqTo(nonResidentialRequest))
      verify(repo).sdltCreateReturn(eqTo(mixedRequest))
      verifyNoMoreInteractions(repo)
    }

    "must handle different storn ID formats" in {
      val repo                          = mock[SdltFormpRepository]
      val service                       = new ReturnService(repo)
      val request1: CreateReturnRequest = mkCreateRequest("STORN12345")
      val request2: CreateReturnRequest = mkCreateRequest("STORN-ABC-123")
      val request3: CreateReturnRequest = mkCreateRequest("12345678")

      when(repo.sdltCreateReturn(eqTo(request1)))
        .thenReturn(Future.successful("100009"))
      when(repo.sdltCreateReturn(eqTo(request2)))
        .thenReturn(Future.successful("100010"))
      when(repo.sdltCreateReturn(eqTo(request3)))
        .thenReturn(Future.successful("100011"))

      service.createSDLTReturn(request1).futureValue mustBe "100009"
      service.createSDLTReturn(request2).futureValue mustBe "100010"
      service.createSDLTReturn(request3).futureValue mustBe "100011"

      verify(repo).sdltCreateReturn(eqTo(request1))
      verify(repo).sdltCreateReturn(eqTo(request2))
      verify(repo).sdltCreateReturn(eqTo(request3))
      verifyNoMoreInteractions(repo)
    }

    "must call repository exactly once per request" in {
      val repo                         = mock[SdltFormpRepository]
      val service                      = new ReturnService(repo)
      val request: CreateReturnRequest = mkCreateRequest()

      when(repo.sdltCreateReturn(eqTo(request)))
        .thenReturn(Future.successful("100012"))

      service.createSDLTReturn(request).futureValue

      verify(repo, times(1)).sdltCreateReturn(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle consecutive requests independently" in {
      val repo                          = mock[SdltFormpRepository]
      val service                       = new ReturnService(repo)
      val request1: CreateReturnRequest = mkCreateRequest("STORN11111")
      val request2: CreateReturnRequest = mkCreateRequest("STORN22222")
      val request3: CreateReturnRequest = mkCreateRequest("STORN33333")

      when(repo.sdltCreateReturn(eqTo(request1)))
        .thenReturn(Future.successful("100013"))
      when(repo.sdltCreateReturn(eqTo(request2)))
        .thenReturn(Future.successful("100014"))
      when(repo.sdltCreateReturn(eqTo(request3)))
        .thenReturn(Future.successful("100015"))

      service.createSDLTReturn(request1).futureValue mustBe "100013"
      service.createSDLTReturn(request2).futureValue mustBe "100014"
      service.createSDLTReturn(request3).futureValue mustBe "100015"

      verify(repo, times(3)).sdltCreateReturn(org.mockito.ArgumentMatchers.any[CreateReturnRequest])
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService getSDLTReturn" - {

    "must delegate to repository" in {
      val repo                       = mock[SdltFormpRepository]
      val service                    = new ReturnService(repo)
      val response: GetReturnRequest = mkGetReturnResponse()

      when(repo.sdltGetReturn(eqTo("100001"), eqTo("STORN12345")))
        .thenReturn(Future.successful(response))

      val result: GetReturnRequest = service.getSDLTReturn("100001", "STORN12345").futureValue
      result mustBe response

      verify(repo).sdltGetReturn(eqTo("100001"), eqTo("STORN12345"))
      verifyNoMoreInteractions(repo)
    }

    "must return correct data for different return IDs" in {
      val repo                        = mock[SdltFormpRepository]
      val service                     = new ReturnService(repo)
      val response1: GetReturnRequest = mkGetReturnResponse("100001", "STORN11111")
      val response2: GetReturnRequest = mkGetReturnResponse("100002", "STORN22222")

      when(repo.sdltGetReturn(eqTo("100001"), eqTo("STORN11111")))
        .thenReturn(Future.successful(response1))
      when(repo.sdltGetReturn(eqTo("100002"), eqTo("STORN22222")))
        .thenReturn(Future.successful(response2))

      service.getSDLTReturn("100001", "STORN11111").futureValue mustBe response1
      service.getSDLTReturn("100002", "STORN22222").futureValue mustBe response2

      verify(repo).sdltGetReturn(eqTo("100001"), eqTo("STORN11111"))
      verify(repo).sdltGetReturn(eqTo("100002"), eqTo("STORN22222"))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo    = mock[SdltFormpRepository]
      val service = new ReturnService(repo)
      val boom    = new RuntimeException("database timeout")

      when(repo.sdltGetReturn(eqTo("100001"), eqTo("STORN12345")))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.getSDLTReturn("100001", "STORN12345").failed.futureValue
      ex mustBe boom

      verify(repo).sdltGetReturn(eqTo("100001"), eqTo("STORN12345"))
      verifyNoMoreInteractions(repo)
    }

    "must handle return with full nested data" in {
      val repo                           = mock[SdltFormpRepository]
      val service                        = new ReturnService(repo)
      val fullResponse: GetReturnRequest = mkGetReturnResponse().copy(
        purchaser = Some(
          Seq(
            Purchaser(
              purchaserID = Some("1"),
              isCompany = Some("N"),
              surname = Some("Smith")
            )
          )
        ),
        transaction = Some(
          Transaction(
            transactionID = Some("1"),
            totalConsideration = Some(BigDecimal("250000.00"))
          )
        ),
        taxCalculation = Some(
          TaxCalculation(
            taxCalculationID = Some("1"),
            taxDue = Some("2500.00")
          )
        )
      )

      when(repo.sdltGetReturn(eqTo("100001"), eqTo("STORN12345")))
        .thenReturn(Future.successful(fullResponse))

      val result: GetReturnRequest = service.getSDLTReturn("100001", "STORN12345").futureValue

      result.purchaser      must not be None
      result.transaction    must not be None
      result.taxCalculation must not be None

      verify(repo).sdltGetReturn(eqTo("100001"), eqTo("STORN12345"))
      verifyNoMoreInteractions(repo)
    }

    "must handle return with minimal data (all optional fields None)" in {
      val repo                              = mock[SdltFormpRepository]
      val service                           = new ReturnService(repo)
      val minimalResponse: GetReturnRequest = mkGetReturnResponse().copy(
        purchaser = None,
        companyDetails = None,
        vendor = None,
        land = None,
        transaction = None,
        returnAgent = None,
        agent = None,
        lease = None,
        taxCalculation = None,
        submission = None,
        submissionErrorDetails = None,
        residency = None
      )

      when(repo.sdltGetReturn(eqTo("100003"), eqTo("STORN12345")))
        .thenReturn(Future.successful(minimalResponse))

      val result: GetReturnRequest = service.getSDLTReturn("100003", "STORN12345").futureValue

      result.purchaser      must be(None)
      result.transaction    must be(None)
      result.taxCalculation must be(None)

      verify(repo).sdltGetReturn(eqTo("100003"), eqTo("STORN12345"))
      verifyNoMoreInteractions(repo)
    }

    "must handle different return resource ref formats" in {
      val repo                        = mock[SdltFormpRepository]
      val service                     = new ReturnService(repo)
      val response1: GetReturnRequest = mkGetReturnResponse("100001", "STORN12345")
      val response2: GetReturnRequest = mkGetReturnResponse("RRF-2024-001", "STORN12345")
      val response3: GetReturnRequest = mkGetReturnResponse("ABC-123-XYZ", "STORN12345")

      when(repo.sdltGetReturn(eqTo("100001"), eqTo("STORN12345")))
        .thenReturn(Future.successful(response1))
      when(repo.sdltGetReturn(eqTo("RRF-2024-001"), eqTo("STORN12345")))
        .thenReturn(Future.successful(response2))
      when(repo.sdltGetReturn(eqTo("ABC-123-XYZ"), eqTo("STORN12345")))
        .thenReturn(Future.successful(response3))

      service.getSDLTReturn("100001", "STORN12345").futureValue mustBe response1
      service.getSDLTReturn("RRF-2024-001", "STORN12345").futureValue mustBe response2
      service.getSDLTReturn("ABC-123-XYZ", "STORN12345").futureValue mustBe response3

      verify(repo).sdltGetReturn(eqTo("100001"), eqTo("STORN12345"))
      verify(repo).sdltGetReturn(eqTo("RRF-2024-001"), eqTo("STORN12345"))
      verify(repo).sdltGetReturn(eqTo("ABC-123-XYZ"), eqTo("STORN12345"))
      verifyNoMoreInteractions(repo)
    }

    "must handle different storn ID formats" in {
      val repo                        = mock[SdltFormpRepository]
      val service                     = new ReturnService(repo)
      val response1: GetReturnRequest = mkGetReturnResponse("100001", "STORN12345")
      val response2: GetReturnRequest = mkGetReturnResponse("100001", "STORN-ABC-123")
      val response3: GetReturnRequest = mkGetReturnResponse("100001", "87654321")

      when(repo.sdltGetReturn(eqTo("100001"), eqTo("STORN12345")))
        .thenReturn(Future.successful(response1))
      when(repo.sdltGetReturn(eqTo("100001"), eqTo("STORN-ABC-123")))
        .thenReturn(Future.successful(response2))
      when(repo.sdltGetReturn(eqTo("100001"), eqTo("87654321")))
        .thenReturn(Future.successful(response3))

      service.getSDLTReturn("100001", "STORN12345").futureValue mustBe response1
      service.getSDLTReturn("100001", "STORN-ABC-123").futureValue mustBe response2
      service.getSDLTReturn("100001", "87654321").futureValue mustBe response3

      verify(repo).sdltGetReturn(eqTo("100001"), eqTo("STORN12345"))
      verify(repo).sdltGetReturn(eqTo("100001"), eqTo("STORN-ABC-123"))
      verify(repo).sdltGetReturn(eqTo("100001"), eqTo("87654321"))
      verifyNoMoreInteractions(repo)
    }

    "must call repository exactly once per request" in {
      val repo                       = mock[SdltFormpRepository]
      val service                    = new ReturnService(repo)
      val response: GetReturnRequest = mkGetReturnResponse()

      when(repo.sdltGetReturn(eqTo("100001"), eqTo("STORN12345")))
        .thenReturn(Future.successful(response))

      service.getSDLTReturn("100001", "STORN12345").futureValue

      verify(repo, times(1)).sdltGetReturn(eqTo("100001"), eqTo("STORN12345"))
      verifyNoMoreInteractions(repo)
    }

    "must handle consecutive requests independently" in {
      val repo                        = mock[SdltFormpRepository]
      val service                     = new ReturnService(repo)
      val response1: GetReturnRequest = mkGetReturnResponse("100001", "STORN11111")
      val response2: GetReturnRequest = mkGetReturnResponse("100002", "STORN22222")
      val response3: GetReturnRequest = mkGetReturnResponse("100003", "STORN33333")

      when(repo.sdltGetReturn(eqTo("100001"), eqTo("STORN11111")))
        .thenReturn(Future.successful(response1))
      when(repo.sdltGetReturn(eqTo("100002"), eqTo("STORN22222")))
        .thenReturn(Future.successful(response2))
      when(repo.sdltGetReturn(eqTo("100003"), eqTo("STORN33333")))
        .thenReturn(Future.successful(response3))

      service.getSDLTReturn("100001", "STORN11111").futureValue mustBe response1
      service.getSDLTReturn("100002", "STORN22222").futureValue mustBe response2
      service.getSDLTReturn("100003", "STORN33333").futureValue mustBe response3

      verify(repo, times(3)).sdltGetReturn(
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString()
      )
      verifyNoMoreInteractions(repo)
    }

    "must handle return with partial nested data" in {
      val repo                              = mock[SdltFormpRepository]
      val service                           = new ReturnService(repo)
      val partialResponse: GetReturnRequest = mkGetReturnResponse().copy(
        purchaser = Some(
          Seq(
            Purchaser(
              purchaserID = Some("1"),
              isCompany = Some("N"),
              surname = Some("Smith")
            )
          )
        ),
        transaction = Some(
          Transaction(
            transactionID = Some("1"),
            totalConsideration = Some(BigDecimal("250000.00"))
          )
        ),
        taxCalculation = None,
        vendor = None,
        land = None
      )

      when(repo.sdltGetReturn(eqTo("100004"), eqTo("STORN12345")))
        .thenReturn(Future.successful(partialResponse))

      val result: GetReturnRequest = service.getSDLTReturn("100004", "STORN12345").futureValue

      result.purchaser      must not be None
      result.transaction    must not be None
      result.taxCalculation must be(None)
      result.vendor         must be(None)

      verify(repo).sdltGetReturn(eqTo("100004"), eqTo("STORN12345"))
      verifyNoMoreInteractions(repo)
    }
  }
}
