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

package uk.gov.hmrc.formpproxy.sdlt.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.formpproxy.sdlt.models.lease._

class LeaseModelsSpec extends AnyFreeSpec with Matchers {

  "LeasePayload" - {

    "must serialize to JSON correctly with all fields populated" in {
      val payload = LeasePayload(
        isAnnualRentOver1000 = Some("YES"),
        contractEndDate = Some("2030-12-31"),
        contractStartDate = Some("2025-01-01"),
        leaseType = Some("COMMERCIAL"),
        netPresentValue = Some("50000"),
        totalPremiumPayable = Some("10000"),
        rentFreePeriod = Some("NO"),
        startingRent = Some("12000"),
        startingRentEndDate = Some("2026-01-01"),
        laterRentKnown = Some("YES"),
        vatAmount = Some("2400")
      )

      val json = Json.toJson(payload)

      (json \ "isAnnualRentOver1000").as[String] mustBe "YES"
      (json \ "contractEndDate").as[String] mustBe "2030-12-31"
      (json \ "contractStartDate").as[String] mustBe "2025-01-01"
      (json \ "leaseType").as[String] mustBe "COMMERCIAL"
      (json \ "netPresentValue").as[String] mustBe "50000"
      (json \ "totalPremiumPayable").as[String] mustBe "10000"
      (json \ "rentFreePeriod").as[String] mustBe "NO"
      (json \ "startingRent").as[String] mustBe "12000"
      (json \ "startingRentEndDate").as[String] mustBe "2026-01-01"
      (json \ "laterRentKnown").as[String] mustBe "YES"
      (json \ "vatAmount").as[String] mustBe "2400"
    }

    "must serialize to JSON correctly with no fields populated" in {
      val payload = LeasePayload(
        isAnnualRentOver1000 = None,
        contractEndDate = None,
        contractStartDate = None,
        leaseType = None,
        netPresentValue = None,
        totalPremiumPayable = None,
        rentFreePeriod = None,
        startingRent = None,
        startingRentEndDate = None,
        laterRentKnown = None,
        vatAmount = None
      )

      val json = Json.toJson(payload)

      (json \ "isAnnualRentOver1000").toOption mustBe None
      (json \ "contractEndDate").toOption mustBe None
      (json \ "contractStartDate").toOption mustBe None
      (json \ "leaseType").toOption mustBe None
      (json \ "netPresentValue").toOption mustBe None
      (json \ "totalPremiumPayable").toOption mustBe None
      (json \ "rentFreePeriod").toOption mustBe None
      (json \ "startingRent").toOption mustBe None
      (json \ "startingRentEndDate").toOption mustBe None
      (json \ "laterRentKnown").toOption mustBe None
      (json \ "vatAmount").toOption mustBe None
    }

    "must serialize to JSON correctly with partial fields populated" in {
      val payload = LeasePayload(
        isAnnualRentOver1000 = Some("YES"),
        contractEndDate = Some("2029-06-30"),
        contractStartDate = Some("2025-07-01"),
        leaseType = Some("RESIDENTIAL"),
        netPresentValue = None,
        totalPremiumPayable = None,
        rentFreePeriod = None,
        startingRent = Some("9000"),
        startingRentEndDate = None,
        laterRentKnown = None,
        vatAmount = None
      )

      val json = Json.toJson(payload)

      (json \ "isAnnualRentOver1000").as[String] mustBe "YES"
      (json \ "contractEndDate").as[String] mustBe "2029-06-30"
      (json \ "leaseType").as[String] mustBe "RESIDENTIAL"
      (json \ "startingRent").as[String] mustBe "9000"
      (json \ "netPresentValue").toOption mustBe None
      (json \ "vatAmount").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "isAnnualRentOver1000" -> "YES",
        "contractEndDate"      -> "2030-12-31",
        "contractStartDate"    -> "2025-01-01",
        "leaseType"            -> "COMMERCIAL",
        "netPresentValue"      -> "50000",
        "totalPremiumPayable"  -> "10000",
        "rentFreePeriod"       -> "NO",
        "startingRent"         -> "12000",
        "startingRentEndDate"  -> "2026-01-01",
        "laterRentKnown"       -> "YES",
        "vatAmount"            -> "2400"
      )

      val result = json.validate[LeasePayload]

      result mustBe a[JsSuccess[_]]
      val payload = result.get

      payload.isAnnualRentOver1000 mustBe Some("YES")
      payload.contractEndDate mustBe Some("2030-12-31")
      payload.contractStartDate mustBe Some("2025-01-01")
      payload.leaseType mustBe Some("COMMERCIAL")
      payload.netPresentValue mustBe Some("50000")
      payload.totalPremiumPayable mustBe Some("10000")
      payload.rentFreePeriod mustBe Some("NO")
      payload.startingRent mustBe Some("12000")
      payload.startingRentEndDate mustBe Some("2026-01-01")
      payload.laterRentKnown mustBe Some("YES")
      payload.vatAmount mustBe Some("2400")
    }

    "must deserialize from JSON correctly with no fields populated" in {
      val json = Json.obj()

      val result = json.validate[LeasePayload]

      result mustBe a[JsSuccess[_]]
      val payload = result.get

      payload.isAnnualRentOver1000 mustBe None
      payload.contractEndDate mustBe None
      payload.contractStartDate mustBe None
      payload.leaseType mustBe None
      payload.netPresentValue mustBe None
      payload.totalPremiumPayable mustBe None
      payload.rentFreePeriod mustBe None
      payload.startingRent mustBe None
      payload.startingRentEndDate mustBe None
      payload.laterRentKnown mustBe None
      payload.vatAmount mustBe None
    }
  }

  "CreateLeaseRequest" - {

    "must serialize to JSON correctly with all fields populated" in {
      val request = CreateLeaseRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        lease = LeasePayload(
          isAnnualRentOver1000 = Some("YES"),
          contractEndDate = Some("2030-12-31"),
          contractStartDate = Some("2025-01-01"),
          leaseType = Some("COMMERCIAL"),
          netPresentValue = Some("50000"),
          totalPremiumPayable = Some("10000"),
          rentFreePeriod = Some("NO"),
          startingRent = Some("12000"),
          startingRentEndDate = Some("2026-01-01"),
          laterRentKnown = Some("YES"),
          vatAmount = Some("2400")
        )
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "lease" \ "isAnnualRentOver1000").as[String] mustBe "YES"
      (json \ "lease" \ "contractEndDate").as[String] mustBe "2030-12-31"
      (json \ "lease" \ "contractStartDate").as[String] mustBe "2025-01-01"
      (json \ "lease" \ "leaseType").as[String] mustBe "COMMERCIAL"
      (json \ "lease" \ "netPresentValue").as[String] mustBe "50000"
      (json \ "lease" \ "totalPremiumPayable").as[String] mustBe "10000"
      (json \ "lease" \ "startingRent").as[String] mustBe "12000"
      (json \ "lease" \ "vatAmount").as[String] mustBe "2400"
    }

    "must serialize to JSON correctly with minimal lease payload" in {
      val request = CreateLeaseRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        lease = LeasePayload(
          isAnnualRentOver1000 = None,
          contractEndDate = None,
          contractStartDate = None,
          leaseType = None,
          netPresentValue = None,
          totalPremiumPayable = None,
          rentFreePeriod = None,
          startingRent = None,
          startingRentEndDate = None,
          laterRentKnown = None,
          vatAmount = None
        )
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "lease" \ "isAnnualRentOver1000").toOption mustBe None
      (json \ "lease" \ "leaseType").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "lease"             -> Json.obj(
          "isAnnualRentOver1000" -> "YES",
          "contractEndDate"      -> "2030-12-31",
          "contractStartDate"    -> "2025-01-01",
          "leaseType"            -> "COMMERCIAL",
          "netPresentValue"      -> "50000",
          "totalPremiumPayable"  -> "10000",
          "rentFreePeriod"       -> "NO",
          "startingRent"         -> "12000",
          "startingRentEndDate"  -> "2026-01-01",
          "laterRentKnown"       -> "YES",
          "vatAmount"            -> "2400"
        )
      )

      val result = json.validate[CreateLeaseRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.lease.isAnnualRentOver1000 mustBe Some("YES")
      request.lease.contractEndDate mustBe Some("2030-12-31")
      request.lease.contractStartDate mustBe Some("2025-01-01")
      request.lease.leaseType mustBe Some("COMMERCIAL")
      request.lease.netPresentValue mustBe Some("50000")
      request.lease.totalPremiumPayable mustBe Some("10000")
      request.lease.rentFreePeriod mustBe Some("NO")
      request.lease.startingRent mustBe Some("12000")
      request.lease.startingRentEndDate mustBe Some("2026-01-01")
      request.lease.laterRentKnown mustBe Some("YES")
      request.lease.vatAmount mustBe Some("2400")
    }

    "must deserialize from JSON correctly with empty lease payload" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "lease"             -> Json.obj()
      )

      val result = json.validate[CreateLeaseRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.lease.isAnnualRentOver1000 mustBe None
      request.lease.leaseType mustBe None
      request.lease.vatAmount mustBe None
    }

    "must fail to deserialize when required field stornId is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "lease"             -> Json.obj()
      )

      val result = json.validate[CreateLeaseRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field returnResourceRef is missing" in {
      val json = Json.obj(
        "stornId" -> "STORN12345",
        "lease"   -> Json.obj()
      )

      val result = json.validate[CreateLeaseRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field lease is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[CreateLeaseRequest]

      result.isError mustBe true
    }
  }

  "CreateLeaseReturn" - {

    "must serialize to JSON correctly when created is true" in {
      val response = CreateLeaseReturn(created = true)

      val json = Json.toJson(response)

      (json \ "created").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when created is false" in {
      val response = CreateLeaseReturn(created = false)

      val json = Json.toJson(response)

      (json \ "created").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when created is true" in {
      val json = Json.obj("created" -> true)

      val result = json.validate[CreateLeaseReturn]

      result mustBe a[JsSuccess[_]]
      result.get.created mustBe true
    }

    "must fail to deserialize when created field is missing" in {
      val json = Json.obj()

      val result = json.validate[CreateLeaseReturn]

      result.isError mustBe true
    }
  }

  "UpdateLeaseRequest" - {

    "must serialize to JSON correctly with all fields populated" in {
      val request = UpdateLeaseRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        lease = LeasePayload(
          isAnnualRentOver1000 = Some("YES"),
          contractEndDate = Some("2030-12-31"),
          contractStartDate = Some("2025-01-01"),
          leaseType = Some("COMMERCIAL"),
          netPresentValue = Some("60000"),
          totalPremiumPayable = Some("15000"),
          rentFreePeriod = Some("YES"),
          startingRent = Some("13000"),
          startingRentEndDate = Some("2026-01-01"),
          laterRentKnown = Some("NO"),
          vatAmount = Some("2600")
        )
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "lease" \ "isAnnualRentOver1000").as[String] mustBe "YES"
      (json \ "lease" \ "leaseType").as[String] mustBe "COMMERCIAL"
      (json \ "lease" \ "netPresentValue").as[String] mustBe "60000"
      (json \ "lease" \ "rentFreePeriod").as[String] mustBe "YES"
      (json \ "lease" \ "startingRent").as[String] mustBe "13000"
      (json \ "lease" \ "vatAmount").as[String] mustBe "2600"
    }

    "must serialize to JSON correctly with minimal lease payload" in {
      val request = UpdateLeaseRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        lease = LeasePayload(
          isAnnualRentOver1000 = None,
          contractEndDate = None,
          contractStartDate = None,
          leaseType = None,
          netPresentValue = None,
          totalPremiumPayable = None,
          rentFreePeriod = None,
          startingRent = None,
          startingRentEndDate = None,
          laterRentKnown = None,
          vatAmount = None
        )
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "lease" \ "isAnnualRentOver1000").toOption mustBe None
    }

    "must serialize to JSON correctly with partial lease payload" in {
      val request = UpdateLeaseRequest(
        stornId = "STORN77777",
        returnResourceRef = "100003",
        lease = LeasePayload(
          isAnnualRentOver1000 = Some("NO"),
          contractEndDate = None,
          contractStartDate = None,
          leaseType = Some("RESIDENTIAL"),
          netPresentValue = None,
          totalPremiumPayable = None,
          rentFreePeriod = None,
          startingRent = Some("8000"),
          startingRentEndDate = None,
          laterRentKnown = None,
          vatAmount = None
        )
      )

      val json = Json.toJson(request)

      (json \ "lease" \ "isAnnualRentOver1000").as[String] mustBe "NO"
      (json \ "lease" \ "leaseType").as[String] mustBe "RESIDENTIAL"
      (json \ "lease" \ "startingRent").as[String] mustBe "8000"
      (json \ "lease" \ "netPresentValue").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "lease"             -> Json.obj(
          "isAnnualRentOver1000" -> "YES",
          "contractEndDate"      -> "2030-12-31",
          "contractStartDate"    -> "2025-01-01",
          "leaseType"            -> "COMMERCIAL",
          "netPresentValue"      -> "60000",
          "totalPremiumPayable"  -> "15000",
          "rentFreePeriod"       -> "YES",
          "startingRent"         -> "13000",
          "startingRentEndDate"  -> "2026-01-01",
          "laterRentKnown"       -> "NO",
          "vatAmount"            -> "2600"
        )
      )

      val result = json.validate[UpdateLeaseRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.lease.isAnnualRentOver1000 mustBe Some("YES")
      request.lease.leaseType mustBe Some("COMMERCIAL")
      request.lease.netPresentValue mustBe Some("60000")
      request.lease.rentFreePeriod mustBe Some("YES")
      request.lease.startingRent mustBe Some("13000")
      request.lease.vatAmount mustBe Some("2600")
    }

    "must deserialize from JSON correctly with empty lease payload" in {
      val json = Json.obj(
        "stornId"           -> "STORN99999",
        "returnResourceRef" -> "100002",
        "lease"             -> Json.obj()
      )

      val result = json.validate[UpdateLeaseRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN99999"
      request.returnResourceRef mustBe "100002"
      request.lease.isAnnualRentOver1000 mustBe None
      request.lease.leaseType mustBe None
    }

    "must fail to deserialize when required field stornId is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "lease"             -> Json.obj()
      )

      val result = json.validate[UpdateLeaseRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field returnResourceRef is missing" in {
      val json = Json.obj(
        "stornId" -> "STORN12345",
        "lease"   -> Json.obj()
      )

      val result = json.validate[UpdateLeaseRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field lease is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[UpdateLeaseRequest]

      result.isError mustBe true
    }
  }

  "UpdateLeaseReturn" - {

    "must serialize to JSON correctly when updated is true" in {
      val response = UpdateLeaseReturn(updated = true)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when updated is false" in {
      val response = UpdateLeaseReturn(updated = false)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when updated is true" in {
      val json = Json.obj("updated" -> true)

      val result = json.validate[UpdateLeaseReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe true
    }

    "must fail to deserialize when updated field is missing" in {
      val json = Json.obj()

      val result = json.validate[UpdateLeaseReturn]

      result.isError mustBe true
    }
  }

  "DeleteLeaseRequest" - {

    "must serialize to JSON correctly" in {
      val request = DeleteLeaseRequest(
        storn = "STORN12345",
        returnResourceRef = "100001"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[DeleteLeaseRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[DeleteLeaseRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj(
        "storn" -> "STORN12345"
      )

      val result = json.validate[DeleteLeaseRequest]

      result.isError mustBe true
    }
  }

  "DeleteLeaseReturn" - {

    "must serialize to JSON correctly when deleted is true" in {
      val response = DeleteLeaseReturn(deleted = true)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when deleted is false" in {
      val response = DeleteLeaseReturn(deleted = false)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when deleted is true" in {
      val json = Json.obj("deleted" -> true)

      val result = json.validate[DeleteLeaseReturn]

      result mustBe a[JsSuccess[_]]
      result.get.deleted mustBe true
    }

    "must fail to deserialize when deleted field is missing" in {
      val json = Json.obj()

      val result = json.validate[DeleteLeaseReturn]

      result.isError mustBe true
    }
  }
}
