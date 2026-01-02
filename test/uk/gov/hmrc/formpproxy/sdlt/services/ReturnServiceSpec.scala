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
import uk.gov.hmrc.formpproxy.sdlt.models.vendor.*
import uk.gov.hmrc.formpproxy.sdlt.models.purchaser.*
import uk.gov.hmrc.formpproxy.sdlt.models.agents.*
import uk.gov.hmrc.formpproxy.sdlt.models.returns.SdltReturnRecordResponse
import uk.gov.hmrc.formpproxy.sdlt.repositories.{SdltFormpRepoDataHelper, SdltFormpRepository}

import scala.concurrent.Future

final class ReturnServiceSpec extends SpecBase with SdltFormpRepoDataHelper {

  trait ReturnsFixture {
    val repo: SdltFormpRepository = mock[SdltFormpRepository]
    val service: ReturnService    = new ReturnService(repo)
  }

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

    "must delegate to repository " in {
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

  "ReturnService getSDLTReturns" - {
    "call sdltGetReturns" in new ReturnsFixture {

      when(repo.sdltGetReturns(eqTo(requestReturns)))
        .thenReturn(Future.successful(expectedResponse))

      val result: SdltReturnRecordResponse = service.getSDLTReturns(requestReturns).futureValue
      result mustBe actualResponse

      verify(repo).sdltGetReturns(eqTo(requestReturns))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService createVendor" - {

    "must delegate to repository " in {
      val repo                                 = mock[SdltFormpRepository]
      val service                              = new ReturnService(repo)
      val request: CreateVendorRequest         = CreateVendorRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        title = Some("Mr"),
        forename1 = Some("John"),
        forename2 = Some("James"),
        name = "Smith",
        houseNumber = Some("123"),
        addressLine1 = "Main Street",
        addressLine2 = Some("Apartment 4B"),
        addressLine3 = Some("City Center"),
        addressLine4 = None,
        postcode = Some("SW1A 1AA"),
        isRepresentedByAgent = "N"
      )
      val expectedResponse: CreateVendorReturn = CreateVendorReturn(
        vendorResourceRef = "V100001",
        vendorId = "VID123"
      )

      when(repo.sdltCreateVendor(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: CreateVendorReturn = service.createVendor(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltCreateVendor(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle minimal vendor request" in {
      val repo                                 = mock[SdltFormpRepository]
      val service                              = new ReturnService(repo)
      val request: CreateVendorRequest         = CreateVendorRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        title = None,
        forename1 = None,
        forename2 = None,
        name = "Company Vendor Ltd",
        houseNumber = None,
        addressLine1 = "Business Park",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        isRepresentedByAgent = "Y"
      )
      val expectedResponse: CreateVendorReturn = CreateVendorReturn(
        vendorResourceRef = "V100002",
        vendorId = "VID456"
      )

      when(repo.sdltCreateVendor(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: CreateVendorReturn = service.createVendor(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltCreateVendor(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                         = mock[SdltFormpRepository]
      val service                      = new ReturnService(repo)
      val request: CreateVendorRequest = CreateVendorRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        name = "Smith",
        addressLine1 = "Main Street",
        isRepresentedByAgent = "N"
      )
      val boom                         = new RuntimeException("database connection failed")

      when(repo.sdltCreateVendor(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createVendor(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltCreateVendor(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService updateVendor" - {

    "must delegate to repository " in {
      val repo                                 = mock[SdltFormpRepository]
      val service                              = new ReturnService(repo)
      val request: UpdateVendorRequest         = UpdateVendorRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        title = Some("Mrs"),
        forename1 = Some("Jane"),
        forename2 = None,
        name = "Doe",
        houseNumber = Some("456"),
        addressLine1 = "Oak Avenue",
        addressLine2 = Some("Suite 10"),
        addressLine3 = None,
        addressLine4 = None,
        postcode = Some("W1A 1AA"),
        isRepresentedByAgent = "Y",
        vendorResourceRef = "V100001",
        nextVendorId = None
      )
      val expectedResponse: UpdateVendorReturn = UpdateVendorReturn(updated = true)

      when(repo.sdltUpdateVendor(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: UpdateVendorReturn = service.updateVendor(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltUpdateVendor(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must return false when update fails" in {
      val repo                                 = mock[SdltFormpRepository]
      val service                              = new ReturnService(repo)
      val request: UpdateVendorRequest         = UpdateVendorRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        name = "Updated Vendor",
        addressLine1 = "New Street",
        isRepresentedByAgent = "N",
        vendorResourceRef = "V100002"
      )
      val expectedResponse: UpdateVendorReturn = UpdateVendorReturn(updated = false)

      when(repo.sdltUpdateVendor(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: UpdateVendorReturn = service.updateVendor(request).futureValue
      result.updated mustBe false

      verify(repo).sdltUpdateVendor(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                         = mock[SdltFormpRepository]
      val service                      = new ReturnService(repo)
      val request: UpdateVendorRequest = UpdateVendorRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        name = "Doe",
        addressLine1 = "Oak Avenue",
        isRepresentedByAgent = "Y",
        vendorResourceRef = "V100001"
      )
      val boom                         = new RuntimeException("database timeout")

      when(repo.sdltUpdateVendor(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateVendor(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltUpdateVendor(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService deleteVendor" - {

    "must delegate to repository " in {
      val repo                                 = mock[SdltFormpRepository]
      val service                              = new ReturnService(repo)
      val request: DeleteVendorRequest         = DeleteVendorRequest(
        storn = "STORN12345",
        vendorResourceRef = "V100001",
        returnResourceRef = "100001"
      )
      val expectedResponse: DeleteVendorReturn = DeleteVendorReturn(deleted = true)

      when(repo.sdltDeleteVendor(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: DeleteVendorReturn = service.deleteVendor(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltDeleteVendor(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must return false when delete fails" in {
      val repo                                 = mock[SdltFormpRepository]
      val service                              = new ReturnService(repo)
      val request: DeleteVendorRequest         = DeleteVendorRequest(
        storn = "STORN99999",
        vendorResourceRef = "V999999",
        returnResourceRef = "100002"
      )
      val expectedResponse: DeleteVendorReturn = DeleteVendorReturn(deleted = false)

      when(repo.sdltDeleteVendor(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: DeleteVendorReturn = service.deleteVendor(request).futureValue
      result.deleted mustBe false

      verify(repo).sdltDeleteVendor(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                         = mock[SdltFormpRepository]
      val service                      = new ReturnService(repo)
      val request: DeleteVendorRequest = DeleteVendorRequest(
        storn = "STORN12345",
        vendorResourceRef = "V100001",
        returnResourceRef = "100001"
      )
      val boom                         = new RuntimeException("database error")

      when(repo.sdltDeleteVendor(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteVendor(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltDeleteVendor(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService createReturnAgent" - {

    "must delegate to repository " in {
      val repo                                      = mock[SdltFormpRepository]
      val service                                   = new ReturnService(repo)
      val request: CreateReturnAgentRequest         = CreateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR",
        name = "Smith & Partners LLP",
        houseNumber = Some("10"),
        addressLine1 = "Legal District",
        addressLine2 = Some("Business Quarter"),
        addressLine3 = Some("Manchester"),
        addressLine4 = None,
        postcode = "M1 1AA",
        phoneNumber = Some("0161234567"),
        email = Some("agent@smithpartners.com"),
        agentReference = Some("AGT123456"),
        isAuthorised = Some("Y")
      )
      val expectedResponse: CreateReturnAgentReturn = CreateReturnAgentReturn(
        returnAgentID = "RA100001"
      )

      when(repo.sdltCreateReturnAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: CreateReturnAgentReturn = service.createReturnAgent(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltCreateReturnAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle minimal agent request" in {
      val repo                                      = mock[SdltFormpRepository]
      val service                                   = new ReturnService(repo)
      val request: CreateReturnAgentRequest         = CreateReturnAgentRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        agentType = "ACCOUNTANT",
        name = "Quick Accounting",
        houseNumber = None,
        addressLine1 = "High Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = "EC1A 1BB",
        phoneNumber = None,
        email = None,
        agentReference = None,
        isAuthorised = None
      )
      val expectedResponse: CreateReturnAgentReturn = CreateReturnAgentReturn(
        returnAgentID = "RA100002"
      )

      when(repo.sdltCreateReturnAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: CreateReturnAgentReturn = service.createReturnAgent(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltCreateReturnAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle different agent types" in {
      val repo                                        = mock[SdltFormpRepository]
      val service                                     = new ReturnService(repo)
      val solicitorRequest: CreateReturnAgentRequest  = CreateReturnAgentRequest(
        stornId = "STORN11111",
        returnResourceRef = "100003",
        agentType = "SOLICITOR",
        name = "Law Firm",
        addressLine1 = "Legal Street",
        postcode = "LS1 1AA"
      )
      val accountantRequest: CreateReturnAgentRequest = CreateReturnAgentRequest(
        stornId = "STORN22222",
        returnResourceRef = "100004",
        agentType = "ACCOUNTANT",
        name = "Accounting Firm",
        addressLine1 = "Finance Street",
        postcode = "FS1 1AA"
      )

      when(repo.sdltCreateReturnAgent(eqTo(solicitorRequest)))
        .thenReturn(Future.successful(CreateReturnAgentReturn("RA100003")))
      when(repo.sdltCreateReturnAgent(eqTo(accountantRequest)))
        .thenReturn(Future.successful(CreateReturnAgentReturn("RA100004")))

      service.createReturnAgent(solicitorRequest).futureValue.returnAgentID mustBe "RA100003"
      service.createReturnAgent(accountantRequest).futureValue.returnAgentID mustBe "RA100004"

      verify(repo).sdltCreateReturnAgent(eqTo(solicitorRequest))
      verify(repo).sdltCreateReturnAgent(eqTo(accountantRequest))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                              = mock[SdltFormpRepository]
      val service                           = new ReturnService(repo)
      val request: CreateReturnAgentRequest = CreateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR",
        name = "Smith & Partners LLP",
        addressLine1 = "Legal District",
        postcode = "M1 1AA"
      )
      val boom                              = new RuntimeException("database connection failed")

      when(repo.sdltCreateReturnAgent(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createReturnAgent(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltCreateReturnAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService updateReturnAgent" - {

    "must delegate to repository " in {
      val repo                                      = mock[SdltFormpRepository]
      val service                                   = new ReturnService(repo)
      val request: UpdateReturnAgentRequest         = UpdateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR",
        name = "Updated Smith & Partners LLP",
        houseNumber = Some("20"),
        addressLine1 = "New Legal District",
        addressLine2 = Some("Updated Quarter"),
        addressLine3 = Some("Manchester"),
        addressLine4 = None,
        postcode = "M2 2BB",
        phoneNumber = Some("0161999888"),
        email = Some("updated@smithpartners.com"),
        agentReference = Some("AGT999999"),
        isAuthorised = Some("Y")
      )
      val expectedResponse: UpdateReturnAgentReturn = UpdateReturnAgentReturn(updated = true)

      when(repo.sdltUpdateReturnAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: UpdateReturnAgentReturn = service.updateReturnAgent(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltUpdateReturnAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must return false when update fails" in {
      val repo                                      = mock[SdltFormpRepository]
      val service                                   = new ReturnService(repo)
      val request: UpdateReturnAgentRequest         = UpdateReturnAgentRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        agentType = "ACCOUNTANT",
        name = "Updated Accounting",
        addressLine1 = "New High Street",
        postcode = "EC2A 2BB"
      )
      val expectedResponse: UpdateReturnAgentReturn = UpdateReturnAgentReturn(updated = false)

      when(repo.sdltUpdateReturnAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: UpdateReturnAgentReturn = service.updateReturnAgent(request).futureValue
      result.updated mustBe false

      verify(repo).sdltUpdateReturnAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                              = mock[SdltFormpRepository]
      val service                           = new ReturnService(repo)
      val request: UpdateReturnAgentRequest = UpdateReturnAgentRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR",
        name = "Smith & Partners LLP",
        addressLine1 = "Legal District",
        postcode = "M1 1AA"
      )
      val boom                              = new RuntimeException("database timeout")

      when(repo.sdltUpdateReturnAgent(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateReturnAgent(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltUpdateReturnAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService deleteReturnAgent" - {

    "must delegate to repository " in {
      val repo                                      = mock[SdltFormpRepository]
      val service                                   = new ReturnService(repo)
      val request: DeleteReturnAgentRequest         = DeleteReturnAgentRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR"
      )
      val expectedResponse: DeleteReturnAgentReturn = DeleteReturnAgentReturn(deleted = true)

      when(repo.sdltDeleteReturnAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: DeleteReturnAgentReturn = service.deleteReturnAgent(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltDeleteReturnAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must return false when delete fails" in {
      val repo                                      = mock[SdltFormpRepository]
      val service                                   = new ReturnService(repo)
      val request: DeleteReturnAgentRequest         = DeleteReturnAgentRequest(
        storn = "STORN99999",
        returnResourceRef = "100002",
        agentType = "ACCOUNTANT"
      )
      val expectedResponse: DeleteReturnAgentReturn = DeleteReturnAgentReturn(deleted = false)

      when(repo.sdltDeleteReturnAgent(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: DeleteReturnAgentReturn = service.deleteReturnAgent(request).futureValue
      result.deleted mustBe false

      verify(repo).sdltDeleteReturnAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle different agent types" in {
      val repo                                        = mock[SdltFormpRepository]
      val service                                     = new ReturnService(repo)
      val solicitorRequest: DeleteReturnAgentRequest  = DeleteReturnAgentRequest(
        storn = "STORN11111",
        returnResourceRef = "100003",
        agentType = "SOLICITOR"
      )
      val accountantRequest: DeleteReturnAgentRequest = DeleteReturnAgentRequest(
        storn = "STORN22222",
        returnResourceRef = "100004",
        agentType = "ACCOUNTANT"
      )

      when(repo.sdltDeleteReturnAgent(eqTo(solicitorRequest)))
        .thenReturn(Future.successful(DeleteReturnAgentReturn(true)))
      when(repo.sdltDeleteReturnAgent(eqTo(accountantRequest)))
        .thenReturn(Future.successful(DeleteReturnAgentReturn(true)))

      service.deleteReturnAgent(solicitorRequest).futureValue.deleted mustBe true
      service.deleteReturnAgent(accountantRequest).futureValue.deleted mustBe true

      verify(repo).sdltDeleteReturnAgent(eqTo(solicitorRequest))
      verify(repo).sdltDeleteReturnAgent(eqTo(accountantRequest))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                              = mock[SdltFormpRepository]
      val service                           = new ReturnService(repo)
      val request: DeleteReturnAgentRequest = DeleteReturnAgentRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        agentType = "SOLICITOR"
      )
      val boom                              = new RuntimeException("database error")

      when(repo.sdltDeleteReturnAgent(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteReturnAgent(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltDeleteReturnAgent(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService updateReturnVersion" - {

    "must delegate to repository " in {
      val repo                                        = mock[SdltFormpRepository]
      val service                                     = new ReturnService(repo)
      val request: ReturnVersionUpdateRequest         = ReturnVersionUpdateRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        currentVersion = "1"
      )
      val expectedResponse: ReturnVersionUpdateReturn = ReturnVersionUpdateReturn(newVersion = 2)

      when(repo.sdltUpdateReturnVersion(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: ReturnVersionUpdateReturn = service.updateReturnVersion(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltUpdateReturnVersion(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle version increment from 0 to 1" in {
      val repo                                        = mock[SdltFormpRepository]
      val service                                     = new ReturnService(repo)
      val request: ReturnVersionUpdateRequest         = ReturnVersionUpdateRequest(
        storn = "STORN11111",
        returnResourceRef = "100003",
        currentVersion = "0"
      )
      val expectedResponse: ReturnVersionUpdateReturn = ReturnVersionUpdateReturn(newVersion = 1)

      when(repo.sdltUpdateReturnVersion(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: ReturnVersionUpdateReturn = service.updateReturnVersion(request).futureValue
      result.newVersion mustBe 1

      verify(repo).sdltUpdateReturnVersion(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle higher version numbers" in {
      val repo                                        = mock[SdltFormpRepository]
      val service                                     = new ReturnService(repo)
      val request: ReturnVersionUpdateRequest         = ReturnVersionUpdateRequest(
        storn = "STORN99999",
        returnResourceRef = "100002",
        currentVersion = "5"
      )
      val expectedResponse: ReturnVersionUpdateReturn = ReturnVersionUpdateReturn(newVersion = 6)

      when(repo.sdltUpdateReturnVersion(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: ReturnVersionUpdateReturn = service.updateReturnVersion(request).futureValue
      result.newVersion mustBe 6

      verify(repo).sdltUpdateReturnVersion(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle very high version numbers" in {
      val repo                                        = mock[SdltFormpRepository]
      val service                                     = new ReturnService(repo)
      val request: ReturnVersionUpdateRequest         = ReturnVersionUpdateRequest(
        storn = "STORN77777",
        returnResourceRef = "999999",
        currentVersion = "10"
      )
      val expectedResponse: ReturnVersionUpdateReturn = ReturnVersionUpdateReturn(newVersion = 11)

      when(repo.sdltUpdateReturnVersion(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: ReturnVersionUpdateReturn = service.updateReturnVersion(request).futureValue
      result.newVersion mustBe 11

      verify(repo).sdltUpdateReturnVersion(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle multiple consecutive version updates" in {
      val repo                                 = mock[SdltFormpRepository]
      val service                              = new ReturnService(repo)
      val request1: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        currentVersion = "1"
      )
      val request2: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        currentVersion = "2"
      )
      val request3: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        currentVersion = "3"
      )

      when(repo.sdltUpdateReturnVersion(eqTo(request1)))
        .thenReturn(Future.successful(ReturnVersionUpdateReturn(2)))
      when(repo.sdltUpdateReturnVersion(eqTo(request2)))
        .thenReturn(Future.successful(ReturnVersionUpdateReturn(3)))
      when(repo.sdltUpdateReturnVersion(eqTo(request3)))
        .thenReturn(Future.successful(ReturnVersionUpdateReturn(4)))

      service.updateReturnVersion(request1).futureValue.newVersion mustBe 2
      service.updateReturnVersion(request2).futureValue.newVersion mustBe 3
      service.updateReturnVersion(request3).futureValue.newVersion mustBe 4

      verify(repo).sdltUpdateReturnVersion(eqTo(request1))
      verify(repo).sdltUpdateReturnVersion(eqTo(request2))
      verify(repo).sdltUpdateReturnVersion(eqTo(request3))
      verifyNoMoreInteractions(repo)
    }

    "must handle different return references" in {
      val repo                                 = mock[SdltFormpRepository]
      val service                              = new ReturnService(repo)
      val request1: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
        storn = "STORN11111",
        returnResourceRef = "100001",
        currentVersion = "1"
      )
      val request2: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
        storn = "STORN22222",
        returnResourceRef = "100002",
        currentVersion = "1"
      )

      when(repo.sdltUpdateReturnVersion(eqTo(request1)))
        .thenReturn(Future.successful(ReturnVersionUpdateReturn(2)))
      when(repo.sdltUpdateReturnVersion(eqTo(request2)))
        .thenReturn(Future.successful(ReturnVersionUpdateReturn(2)))

      service.updateReturnVersion(request1).futureValue.newVersion mustBe 2
      service.updateReturnVersion(request2).futureValue.newVersion mustBe 2

      verify(repo).sdltUpdateReturnVersion(eqTo(request1))
      verify(repo).sdltUpdateReturnVersion(eqTo(request2))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                                = mock[SdltFormpRepository]
      val service                             = new ReturnService(repo)
      val request: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        currentVersion = "1"
      )
      val boom                                = new RuntimeException("database connection failed")

      when(repo.sdltUpdateReturnVersion(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateReturnVersion(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltUpdateReturnVersion(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must call repository exactly once per request" in {
      val repo                                = mock[SdltFormpRepository]
      val service                             = new ReturnService(repo)
      val request: ReturnVersionUpdateRequest = ReturnVersionUpdateRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        currentVersion = "1"
      )

      when(repo.sdltUpdateReturnVersion(eqTo(request)))
        .thenReturn(Future.successful(ReturnVersionUpdateReturn(2)))

      service.updateReturnVersion(request).futureValue

      verify(repo, times(1)).sdltUpdateReturnVersion(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService createPurchaser" - {

    "must delegate to repository " in {
      val repo                                    = mock[SdltFormpRepository]
      val service                                 = new ReturnService(repo)
      val request: CreatePurchaserRequest         = CreatePurchaserRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        isCompany = "N",
        isTrustee = "N",
        isConnectedToVendor = "N",
        isRepresentedByAgent = "N",
        title = Some("Mr"),
        surname = Some("Smith"),
        forename1 = Some("John"),
        forename2 = Some("James"),
        companyName = None,
        houseNumber = Some("123"),
        address1 = "Main Street",
        address2 = Some("Apartment 4B"),
        address3 = Some("City Center"),
        address4 = None,
        postcode = Some("SW1A 1AA"),
        phone = Some("07777123456"),
        nino = Some("AB123456C"),
        isUkCompany = None,
        hasNino = Some("Y"),
        dateOfBirth = Some("1980-01-15"),
        registrationNumber = None,
        placeOfRegistration = None
      )
      val expectedResponse: CreatePurchaserReturn = CreatePurchaserReturn(
        purchaserResourceRef = "P100001",
        purchaserId = "PID123"
      )

      when(repo.sdltCreatePurchaser(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: CreatePurchaserReturn = service.createPurchaser(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltCreatePurchaser(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle minimal purchaser request" in {
      val repo                                    = mock[SdltFormpRepository]
      val service                                 = new ReturnService(repo)
      val request: CreatePurchaserRequest         = CreatePurchaserRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        isCompany = "N",
        isTrustee = "N",
        isConnectedToVendor = "N",
        isRepresentedByAgent = "N",
        title = None,
        surname = None,
        forename1 = None,
        forename2 = None,
        companyName = None,
        houseNumber = None,
        address1 = "Business Park",
        address2 = None,
        address3 = None,
        address4 = None,
        postcode = None,
        phone = None,
        nino = None,
        isUkCompany = None,
        hasNino = None,
        dateOfBirth = None,
        registrationNumber = None,
        placeOfRegistration = None
      )
      val expectedResponse: CreatePurchaserReturn = CreatePurchaserReturn(
        purchaserResourceRef = "P100002",
        purchaserId = "PID456"
      )

      when(repo.sdltCreatePurchaser(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: CreatePurchaserReturn = service.createPurchaser(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltCreatePurchaser(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle company purchaser request" in {
      val repo                                    = mock[SdltFormpRepository]
      val service                                 = new ReturnService(repo)
      val request: CreatePurchaserRequest         = CreatePurchaserRequest(
        stornId = "STORN77777",
        returnResourceRef = "100003",
        isCompany = "Y",
        isTrustee = "N",
        isConnectedToVendor = "N",
        isRepresentedByAgent = "N",
        title = None,
        surname = None,
        forename1 = None,
        forename2 = None,
        companyName = Some("Tech Corp Ltd"),
        houseNumber = None,
        address1 = "Business Park",
        address2 = None,
        address3 = None,
        address4 = None,
        postcode = Some("EC1A 1BB"),
        phone = Some("02012345678"),
        nino = None,
        isUkCompany = Some("Y"),
        hasNino = Some("N"),
        dateOfBirth = None,
        registrationNumber = Some("12345678"),
        placeOfRegistration = None
      )
      val expectedResponse: CreatePurchaserReturn = CreatePurchaserReturn(
        purchaserResourceRef = "P100003",
        purchaserId = "PID789"
      )

      when(repo.sdltCreatePurchaser(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: CreatePurchaserReturn = service.createPurchaser(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltCreatePurchaser(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                            = mock[SdltFormpRepository]
      val service                         = new ReturnService(repo)
      val request: CreatePurchaserRequest = CreatePurchaserRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        isCompany = "N",
        isTrustee = "N",
        isConnectedToVendor = "N",
        isRepresentedByAgent = "N",
        address1 = "Main Street"
      )
      val boom                            = new RuntimeException("database connection failed")

      when(repo.sdltCreatePurchaser(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createPurchaser(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltCreatePurchaser(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService updatePurchaser" - {

    "must delegate to repository " in {
      val repo                                    = mock[SdltFormpRepository]
      val service                                 = new ReturnService(repo)
      val request: UpdatePurchaserRequest         = UpdatePurchaserRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        isCompany = "N",
        isTrustee = "N",
        isConnectedToVendor = "Y",
        isRepresentedByAgent = "Y",
        title = Some("Mrs"),
        surname = Some("Doe"),
        forename1 = Some("Jane"),
        forename2 = None,
        companyName = None,
        houseNumber = Some("456"),
        address1 = "Oak Avenue",
        address2 = Some("Suite 10"),
        address3 = None,
        address4 = None,
        postcode = Some("W1A 1AA"),
        phone = Some("07777654321"),
        nino = Some("CD987654B"),
        nextPurchaserId = None,
        isUkCompany = None,
        hasNino = Some("Y"),
        dateOfBirth = Some("1985-05-20"),
        registrationNumber = None,
        placeOfRegistration = None
      )
      val expectedResponse: UpdatePurchaserReturn = UpdatePurchaserReturn(updated = true)

      when(repo.sdltUpdatePurchaser(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: UpdatePurchaserReturn = service.updatePurchaser(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltUpdatePurchaser(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must return false when update fails" in {
      val repo                                    = mock[SdltFormpRepository]
      val service                                 = new ReturnService(repo)
      val request: UpdatePurchaserRequest         = UpdatePurchaserRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        purchaserResourceRef = "P100002",
        isCompany = "Y",
        isTrustee = "N",
        isConnectedToVendor = "N",
        isRepresentedByAgent = "N",
        companyName = Some("Updated Corp"),
        address1 = "New Street"
      )
      val expectedResponse: UpdatePurchaserReturn = UpdatePurchaserReturn(updated = false)

      when(repo.sdltUpdatePurchaser(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: UpdatePurchaserReturn = service.updatePurchaser(request).futureValue
      result.updated mustBe false

      verify(repo).sdltUpdatePurchaser(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                            = mock[SdltFormpRepository]
      val service                         = new ReturnService(repo)
      val request: UpdatePurchaserRequest = UpdatePurchaserRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        isCompany = "N",
        isTrustee = "N",
        isConnectedToVendor = "N",
        isRepresentedByAgent = "N",
        address1 = "Oak Avenue"
      )
      val boom                            = new RuntimeException("database timeout")

      when(repo.sdltUpdatePurchaser(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updatePurchaser(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltUpdatePurchaser(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService deletePurchaser" - {

    "must delegate to repository " in {
      val repo                                    = mock[SdltFormpRepository]
      val service                                 = new ReturnService(repo)
      val request: DeletePurchaserRequest         = DeletePurchaserRequest(
        storn = "STORN12345",
        purchaserResourceRef = "P100001",
        returnResourceRef = "100001"
      )
      val expectedResponse: DeletePurchaserReturn = DeletePurchaserReturn(deleted = true)

      when(repo.sdltDeletePurchaser(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: DeletePurchaserReturn = service.deletePurchaser(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltDeletePurchaser(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must return false when delete fails" in {
      val repo                                    = mock[SdltFormpRepository]
      val service                                 = new ReturnService(repo)
      val request: DeletePurchaserRequest         = DeletePurchaserRequest(
        storn = "STORN99999",
        purchaserResourceRef = "P999999",
        returnResourceRef = "100002"
      )
      val expectedResponse: DeletePurchaserReturn = DeletePurchaserReturn(deleted = false)

      when(repo.sdltDeletePurchaser(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: DeletePurchaserReturn = service.deletePurchaser(request).futureValue
      result.deleted mustBe false

      verify(repo).sdltDeletePurchaser(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                            = mock[SdltFormpRepository]
      val service                         = new ReturnService(repo)
      val request: DeletePurchaserRequest = DeletePurchaserRequest(
        storn = "STORN12345",
        purchaserResourceRef = "P100001",
        returnResourceRef = "100001"
      )
      val boom                            = new RuntimeException("database error")

      when(repo.sdltDeletePurchaser(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deletePurchaser(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltDeletePurchaser(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService createCompanyDetails" - {

    "must delegate to repository " in {
      val repo                                         = mock[SdltFormpRepository]
      val service                                      = new ReturnService(repo)
      val request: CreateCompanyDetailsRequest         = CreateCompanyDetailsRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        utr = Some("1234567890"),
        vatReference = Some("GB123456789"),
        compTypeBank = Some("N"),
        compTypeBuilder = Some("N"),
        compTypeBuildsoc = Some("N"),
        compTypeCentgov = Some("N"),
        compTypeIndividual = Some("N"),
        compTypeInsurance = Some("N"),
        compTypeLocalauth = Some("N"),
        compTypeOcharity = Some("N"),
        compTypeOcompany = Some("Y"),
        compTypeOfinancial = Some("N"),
        compTypePartship = Some("N"),
        compTypeProperty = Some("N"),
        compTypePubliccorp = Some("N"),
        compTypeSoletrader = Some("N"),
        compTypePenfund = Some("N")
      )
      val expectedResponse: CreateCompanyDetailsReturn = CreateCompanyDetailsReturn(
        companyDetailsId = "CDID123"
      )

      when(repo.sdltCreateCompanyDetails(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: CreateCompanyDetailsReturn = service.createCompanyDetails(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltCreateCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must handle minimal company details request" in {
      val repo                                         = mock[SdltFormpRepository]
      val service                                      = new ReturnService(repo)
      val request: CreateCompanyDetailsRequest         = CreateCompanyDetailsRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        purchaserResourceRef = "P100002",
        utr = None,
        vatReference = None,
        compTypeBank = None,
        compTypeBuilder = None,
        compTypeBuildsoc = None,
        compTypeCentgov = None,
        compTypeIndividual = None,
        compTypeInsurance = None,
        compTypeLocalauth = None,
        compTypeOcharity = None,
        compTypeOcompany = None,
        compTypeOfinancial = None,
        compTypePartship = None,
        compTypeProperty = None,
        compTypePubliccorp = None,
        compTypeSoletrader = None,
        compTypePenfund = None
      )
      val expectedResponse: CreateCompanyDetailsReturn = CreateCompanyDetailsReturn(
        companyDetailsId = "CDID456"
      )

      when(repo.sdltCreateCompanyDetails(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: CreateCompanyDetailsReturn = service.createCompanyDetails(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltCreateCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                                 = mock[SdltFormpRepository]
      val service                              = new ReturnService(repo)
      val request: CreateCompanyDetailsRequest = CreateCompanyDetailsRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        utr = Some("1234567890"),
        vatReference = None,
        compTypeBank = None,
        compTypeBuilder = None,
        compTypeBuildsoc = None,
        compTypeCentgov = None,
        compTypeIndividual = None,
        compTypeInsurance = None,
        compTypeLocalauth = None,
        compTypeOcharity = None,
        compTypeOcompany = Some("Y"),
        compTypeOfinancial = None,
        compTypePartship = None,
        compTypeProperty = None,
        compTypePubliccorp = None,
        compTypeSoletrader = None,
        compTypePenfund = None
      )
      val boom                                 = new RuntimeException("database connection failed")

      when(repo.sdltCreateCompanyDetails(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.createCompanyDetails(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltCreateCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService updateCompanyDetails" - {

    "must delegate to repository " in {
      val repo                                         = mock[SdltFormpRepository]
      val service                                      = new ReturnService(repo)
      val request: UpdateCompanyDetailsRequest         = UpdateCompanyDetailsRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        utr = Some("9876543210"),
        vatReference = Some("GB987654321"),
        compTypeBank = Some("N"),
        compTypeBuilder = Some("Y"),
        compTypeBuildsoc = Some("N"),
        compTypeCentgov = Some("N"),
        compTypeIndividual = Some("N"),
        compTypeInsurance = Some("N"),
        compTypeLocalauth = Some("N"),
        compTypeOcharity = Some("N"),
        compTypeOcompany = Some("Y"),
        compTypeOfinancial = Some("N"),
        compTypePartship = Some("N"),
        compTypeProperty = Some("Y"),
        compTypePubliccorp = Some("N"),
        compTypeSoletrader = Some("N"),
        compTypePenfund = Some("N")
      )
      val expectedResponse: UpdateCompanyDetailsReturn = UpdateCompanyDetailsReturn(updated = true)

      when(repo.sdltUpdateCompanyDetails(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: UpdateCompanyDetailsReturn = service.updateCompanyDetails(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltUpdateCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must return false when update fails" in {
      val repo                                         = mock[SdltFormpRepository]
      val service                                      = new ReturnService(repo)
      val request: UpdateCompanyDetailsRequest         = UpdateCompanyDetailsRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        purchaserResourceRef = "P100002",
        utr = None,
        vatReference = None,
        compTypeBank = None,
        compTypeBuilder = None,
        compTypeBuildsoc = None,
        compTypeCentgov = None,
        compTypeIndividual = None,
        compTypeInsurance = None,
        compTypeLocalauth = None,
        compTypeOcharity = None,
        compTypeOcompany = None,
        compTypeOfinancial = None,
        compTypePartship = None,
        compTypeProperty = None,
        compTypePubliccorp = None,
        compTypeSoletrader = None,
        compTypePenfund = None
      )
      val expectedResponse: UpdateCompanyDetailsReturn = UpdateCompanyDetailsReturn(updated = false)

      when(repo.sdltUpdateCompanyDetails(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: UpdateCompanyDetailsReturn = service.updateCompanyDetails(request).futureValue
      result.updated mustBe false

      verify(repo).sdltUpdateCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                                 = mock[SdltFormpRepository]
      val service                              = new ReturnService(repo)
      val request: UpdateCompanyDetailsRequest = UpdateCompanyDetailsRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        utr = Some("1234567890"),
        vatReference = None,
        compTypeBank = None,
        compTypeBuilder = None,
        compTypeBuildsoc = None,
        compTypeCentgov = None,
        compTypeIndividual = None,
        compTypeInsurance = None,
        compTypeLocalauth = None,
        compTypeOcharity = None,
        compTypeOcompany = Some("Y"),
        compTypeOfinancial = None,
        compTypePartship = None,
        compTypeProperty = None,
        compTypePubliccorp = None,
        compTypeSoletrader = None,
        compTypePenfund = None
      )
      val boom                                 = new RuntimeException("database timeout")

      when(repo.sdltUpdateCompanyDetails(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.updateCompanyDetails(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltUpdateCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

  "ReturnService deleteCompanyDetails" - {

    "must delegate to repository " in {
      val repo                                         = mock[SdltFormpRepository]
      val service                                      = new ReturnService(repo)
      val request: DeleteCompanyDetailsRequest         = DeleteCompanyDetailsRequest(
        storn = "STORN12345",
        returnResourceRef = "100001"
      )
      val expectedResponse: DeleteCompanyDetailsReturn = DeleteCompanyDetailsReturn(deleted = true)

      when(repo.sdltDeleteCompanyDetails(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: DeleteCompanyDetailsReturn = service.deleteCompanyDetails(request).futureValue
      result mustBe expectedResponse

      verify(repo).sdltDeleteCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must return false when delete fails" in {
      val repo                                         = mock[SdltFormpRepository]
      val service                                      = new ReturnService(repo)
      val request: DeleteCompanyDetailsRequest         = DeleteCompanyDetailsRequest(
        storn = "STORN99999",
        returnResourceRef = "100002"
      )
      val expectedResponse: DeleteCompanyDetailsReturn = DeleteCompanyDetailsReturn(deleted = false)

      when(repo.sdltDeleteCompanyDetails(eqTo(request)))
        .thenReturn(Future.successful(expectedResponse))

      val result: DeleteCompanyDetailsReturn = service.deleteCompanyDetails(request).futureValue
      result.deleted mustBe false

      verify(repo).sdltDeleteCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(repo)
    }

    "must propagate failures from repository" in {
      val repo                                 = mock[SdltFormpRepository]
      val service                              = new ReturnService(repo)
      val request: DeleteCompanyDetailsRequest = DeleteCompanyDetailsRequest(
        storn = "STORN12345",
        returnResourceRef = "100001"
      )
      val boom                                 = new RuntimeException("database error")

      when(repo.sdltDeleteCompanyDetails(eqTo(request)))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.deleteCompanyDetails(request).failed.futureValue
      ex mustBe boom

      verify(repo).sdltDeleteCompanyDetails(eqTo(request))
      verifyNoMoreInteractions(repo)
    }
  }

}
