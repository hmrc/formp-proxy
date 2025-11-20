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

package uk.gov.hmrc.formpproxy.sdlt.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.formpproxy.sdlt.models.vendor._

class VendorModelsSpec extends AnyFreeSpec with Matchers {

  "CreateVendorRequest" - {

    "must serialize to JSON correctly with all fields populated" in {
      val request = CreateVendorRequest(
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
        addressLine4 = Some("Greater London"),
        postcode = Some("SW1A 1AA"),
        isRepresentedByAgent = "N"
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "title").as[String] mustBe "Mr"
      (json \ "forename1").as[String] mustBe "John"
      (json \ "forename2").as[String] mustBe "James"
      (json \ "name").as[String] mustBe "Smith"
      (json \ "houseNumber").as[String] mustBe "123"
      (json \ "addressLine1").as[String] mustBe "Main Street"
      (json \ "addressLine2").as[String] mustBe "Apartment 4B"
      (json \ "addressLine3").as[String] mustBe "City Center"
      (json \ "addressLine4").as[String] mustBe "Greater London"
      (json \ "postcode").as[String] mustBe "SW1A 1AA"
      (json \ "isRepresentedByAgent").as[String] mustBe "N"
    }

    "must serialize to JSON correctly with only required fields" in {
      val request = CreateVendorRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
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

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "name").as[String] mustBe "Company Vendor Ltd"
      (json \ "addressLine1").as[String] mustBe "Business Park"
      (json \ "isRepresentedByAgent").as[String] mustBe "Y"
      (json \ "title").toOption mustBe None
      (json \ "forename1").toOption mustBe None
      (json \ "forename2").toOption mustBe None
      (json \ "houseNumber").toOption mustBe None
      (json \ "addressLine2").toOption mustBe None
      (json \ "addressLine3").toOption mustBe None
      (json \ "addressLine4").toOption mustBe None
      (json \ "postcode").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "title"                -> "Mr",
        "forename1"            -> "John",
        "forename2"            -> "James",
        "name"                 -> "Smith",
        "houseNumber"          -> "123",
        "addressLine1"         -> "Main Street",
        "addressLine2"         -> "Apartment 4B",
        "addressLine3"         -> "City Center",
        "addressLine4"         -> "Greater London",
        "postcode"             -> "SW1A 1AA",
        "isRepresentedByAgent" -> "N"
      )

      val result = json.validate[CreateVendorRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.title mustBe Some("Mr")
      request.forename1 mustBe Some("John")
      request.forename2 mustBe Some("James")
      request.name mustBe "Smith"
      request.houseNumber mustBe Some("123")
      request.addressLine1 mustBe "Main Street"
      request.addressLine2 mustBe Some("Apartment 4B")
      request.addressLine3 mustBe Some("City Center")
      request.addressLine4 mustBe Some("Greater London")
      request.postcode mustBe Some("SW1A 1AA")
      request.isRepresentedByAgent mustBe "N"
    }

    "must deserialize from JSON correctly with only required fields" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "name"                 -> "Company Vendor Ltd",
        "addressLine1"         -> "Business Park",
        "isRepresentedByAgent" -> "Y"
      )

      val result = json.validate[CreateVendorRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.name mustBe "Company Vendor Ltd"
      request.addressLine1 mustBe "Business Park"
      request.isRepresentedByAgent mustBe "Y"
      request.title mustBe None
      request.forename1 mustBe None
      request.forename2 mustBe None
      request.houseNumber mustBe None
      request.addressLine2 mustBe None
      request.addressLine3 mustBe None
      request.addressLine4 mustBe None
      request.postcode mustBe None
    }

    "must fail to deserialize when required field stornId is missing" in {
      val json = Json.obj(
        "returnResourceRef"    -> "100001",
        "name"                 -> "Smith",
        "addressLine1"         -> "Main Street",
        "isRepresentedByAgent" -> "N"
      )

      val result = json.validate[CreateVendorRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field returnResourceRef is missing" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "name"                 -> "Smith",
        "addressLine1"         -> "Main Street",
        "isRepresentedByAgent" -> "N"
      )

      val result = json.validate[CreateVendorRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field name is missing" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "addressLine1"         -> "Main Street",
        "isRepresentedByAgent" -> "N"
      )

      val result = json.validate[CreateVendorRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field addressLine1 is missing" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "name"                 -> "Smith",
        "isRepresentedByAgent" -> "N"
      )

      val result = json.validate[CreateVendorRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field isRepresentedByAgent is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "name"              -> "Smith",
        "addressLine1"      -> "Main Street"
      )

      val result = json.validate[CreateVendorRequest]

      result.isError mustBe true
    }
  }

  "CreateVendorReturn" - {

    "must serialize to JSON correctly" in {
      val response = CreateVendorReturn(
        vendorResourceRef = "V100001",
        vendorId = "VID123"
      )

      val json = Json.toJson(response)

      (json \ "vendorResourceRef").as[String] mustBe "V100001"
      (json \ "vendorId").as[String] mustBe "VID123"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "vendorResourceRef" -> "V100001",
        "vendorId"          -> "VID123"
      )

      val result = json.validate[CreateVendorReturn]

      result mustBe a[JsSuccess[_]]
      val response = result.get

      response.vendorResourceRef mustBe "V100001"
      response.vendorId mustBe "VID123"
    }

    "must fail to deserialize when vendorResourceRef is missing" in {
      val json = Json.obj("vendorId" -> "VID123")

      val result = json.validate[CreateVendorReturn]

      result.isError mustBe true
    }

    "must fail to deserialize when vendorId is missing" in {
      val json = Json.obj("vendorResourceRef" -> "V100001")

      val result = json.validate[CreateVendorReturn]

      result.isError mustBe true
    }
  }

  "UpdateVendorRequest" - {

    "must serialize to JSON correctly with all fields populated" in {
      val request = UpdateVendorRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        title = Some("Mrs"),
        forename1 = Some("Jane"),
        forename2 = Some("Marie"),
        name = "Doe",
        houseNumber = Some("456"),
        addressLine1 = "Oak Avenue",
        addressLine2 = Some("Suite 10"),
        addressLine3 = Some("Downtown"),
        addressLine4 = Some("Greater Manchester"),
        postcode = Some("W1A 1AA"),
        isRepresentedByAgent = "Y",
        vendorResourceRef = "V100001",
        nextVendorId = Some("V100002")
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "title").as[String] mustBe "Mrs"
      (json \ "forename1").as[String] mustBe "Jane"
      (json \ "forename2").as[String] mustBe "Marie"
      (json \ "name").as[String] mustBe "Doe"
      (json \ "houseNumber").as[String] mustBe "456"
      (json \ "addressLine1").as[String] mustBe "Oak Avenue"
      (json \ "addressLine2").as[String] mustBe "Suite 10"
      (json \ "addressLine3").as[String] mustBe "Downtown"
      (json \ "addressLine4").as[String] mustBe "Greater Manchester"
      (json \ "postcode").as[String] mustBe "W1A 1AA"
      (json \ "isRepresentedByAgent").as[String] mustBe "Y"
      (json \ "vendorResourceRef").as[String] mustBe "V100001"
      (json \ "nextVendorId").as[String] mustBe "V100002"
    }

    "must serialize to JSON correctly with only required fields" in {
      val request = UpdateVendorRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        title = None,
        forename1 = None,
        forename2 = None,
        name = "Updated Vendor Ltd",
        houseNumber = None,
        addressLine1 = "New Business Park",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        isRepresentedByAgent = "N",
        vendorResourceRef = "V100002",
        nextVendorId = None
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "name").as[String] mustBe "Updated Vendor Ltd"
      (json \ "addressLine1").as[String] mustBe "New Business Park"
      (json \ "isRepresentedByAgent").as[String] mustBe "N"
      (json \ "vendorResourceRef").as[String] mustBe "V100002"
      (json \ "nextVendorId").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "title"                -> "Mrs",
        "forename1"            -> "Jane",
        "forename2"            -> "Marie",
        "name"                 -> "Doe",
        "houseNumber"          -> "456",
        "addressLine1"         -> "Oak Avenue",
        "addressLine2"         -> "Suite 10",
        "addressLine3"         -> "Downtown",
        "addressLine4"         -> "Greater Manchester",
        "postcode"             -> "W1A 1AA",
        "isRepresentedByAgent" -> "Y",
        "vendorResourceRef"    -> "V100001",
        "nextVendorId"         -> "V100002"
      )

      val result = json.validate[UpdateVendorRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.title mustBe Some("Mrs")
      request.forename1 mustBe Some("Jane")
      request.forename2 mustBe Some("Marie")
      request.name mustBe "Doe"
      request.houseNumber mustBe Some("456")
      request.addressLine1 mustBe "Oak Avenue"
      request.addressLine2 mustBe Some("Suite 10")
      request.addressLine3 mustBe Some("Downtown")
      request.addressLine4 mustBe Some("Greater Manchester")
      request.postcode mustBe Some("W1A 1AA")
      request.isRepresentedByAgent mustBe "Y"
      request.vendorResourceRef mustBe "V100001"
      request.nextVendorId mustBe Some("V100002")
    }

    "must deserialize from JSON correctly with only required fields" in {
      val json = Json.obj(
        "stornId"              -> "STORN99999",
        "returnResourceRef"    -> "100002",
        "name"                 -> "Updated Vendor Ltd",
        "addressLine1"         -> "New Business Park",
        "isRepresentedByAgent" -> "N",
        "vendorResourceRef"    -> "V100002"
      )

      val result = json.validate[UpdateVendorRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN99999"
      request.returnResourceRef mustBe "100002"
      request.name mustBe "Updated Vendor Ltd"
      request.addressLine1 mustBe "New Business Park"
      request.isRepresentedByAgent mustBe "N"
      request.vendorResourceRef mustBe "V100002"
      request.nextVendorId mustBe None
    }

    "must fail to deserialize when required field stornId is missing" in {
      val json = Json.obj(
        "returnResourceRef"    -> "100001",
        "name"                 -> "Doe",
        "addressLine1"         -> "Oak Avenue",
        "isRepresentedByAgent" -> "Y",
        "vendorResourceRef"    -> "V100001"
      )

      val result = json.validate[UpdateVendorRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field returnResourceRef is missing" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "name"                 -> "Doe",
        "addressLine1"         -> "Oak Avenue",
        "isRepresentedByAgent" -> "Y",
        "vendorResourceRef"    -> "V100001"
      )

      val result = json.validate[UpdateVendorRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field name is missing" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "addressLine1"         -> "Oak Avenue",
        "isRepresentedByAgent" -> "Y",
        "vendorResourceRef"    -> "V100001"
      )

      val result = json.validate[UpdateVendorRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field addressLine1 is missing" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "name"                 -> "Doe",
        "isRepresentedByAgent" -> "Y",
        "vendorResourceRef"    -> "V100001"
      )

      val result = json.validate[UpdateVendorRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field isRepresentedByAgent is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001",
        "name"              -> "Doe",
        "addressLine1"      -> "Oak Avenue",
        "vendorResourceRef" -> "V100001"
      )

      val result = json.validate[UpdateVendorRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field vendorResourceRef is missing" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "name"                 -> "Doe",
        "addressLine1"         -> "Oak Avenue",
        "isRepresentedByAgent" -> "Y"
      )

      val result = json.validate[UpdateVendorRequest]

      result.isError mustBe true
    }
  }

  "UpdateVendorReturn" - {

    "must serialize to JSON correctly when updated is true" in {
      val response = UpdateVendorReturn(updated = true)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when updated is false" in {
      val response = UpdateVendorReturn(updated = false)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when updated is true" in {
      val json = Json.obj("updated" -> true)

      val result = json.validate[UpdateVendorReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe true
    }

    "must deserialize from JSON correctly when updated is false" in {
      val json = Json.obj("updated" -> false)

      val result = json.validate[UpdateVendorReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe false
    }

    "must fail to deserialize when updated field is missing" in {
      val json = Json.obj()

      val result = json.validate[UpdateVendorReturn]

      result.isError mustBe true
    }
  }

  "DeleteVendorRequest" - {

    "must serialize to JSON correctly" in {
      val request = DeleteVendorRequest(
        storn = "STORN12345",
        vendorResourceRef = "V100001",
        returnResourceRef = "100001"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "vendorResourceRef").as[String] mustBe "V100001"
      (json \ "returnResourceRef").as[String] mustBe "100001"
    }

    "must serialize to JSON correctly with different values" in {
      val request = DeleteVendorRequest(
        storn = "STORN99999",
        vendorResourceRef = "V999999",
        returnResourceRef = "100002"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN99999"
      (json \ "vendorResourceRef").as[String] mustBe "V999999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "vendorResourceRef" -> "V100001",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[DeleteVendorRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.vendorResourceRef mustBe "V100001"
      request.returnResourceRef mustBe "100001"
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "vendorResourceRef" -> "V100001",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[DeleteVendorRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when vendorResourceRef is missing" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[DeleteVendorRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "vendorResourceRef" -> "V100001"
      )

      val result = json.validate[DeleteVendorRequest]

      result.isError mustBe true
    }
  }

  "DeleteVendorReturn" - {

    "must serialize to JSON correctly when deleted is true" in {
      val response = DeleteVendorReturn(deleted = true)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when deleted is false" in {
      val response = DeleteVendorReturn(deleted = false)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when deleted is true" in {
      val json = Json.obj("deleted" -> true)

      val result = json.validate[DeleteVendorReturn]

      result mustBe a[JsSuccess[_]]
      result.get.deleted mustBe true
    }

    "must deserialize from JSON correctly when deleted is false" in {
      val json = Json.obj("deleted" -> false)

      val result = json.validate[DeleteVendorReturn]

      result mustBe a[JsSuccess[_]]
      result.get.deleted mustBe false
    }

    "must fail to deserialize when deleted field is missing" in {
      val json = Json.obj()

      val result = json.validate[DeleteVendorReturn]

      result.isError mustBe true
    }
  }
}
