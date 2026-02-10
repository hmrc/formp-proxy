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
import uk.gov.hmrc.formpproxy.sdlt.models.purchaser._

class PurchaserModelsSpec extends AnyFreeSpec with Matchers {

  "CreatePurchaserRequest" - {

    "must serialize to JSON correctly with all fields populated for individual" in {
      val request = CreatePurchaserRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        isCompany = Some("NO"),
        isTrustee = Some("NO"),
        isConnectedToVendor = Some("NO"),
        isRepresentedByAgent = Some("NO"),
        title = Some("Mr"),
        surname = Some("Smith"),
        forename1 = Some("John"),
        forename2 = Some("James"),
        companyName = None,
        houseNumber = Some("123"),
        address1 = Some("Main Street"),
        address2 = Some("Apartment 4B"),
        address3 = Some("City Center"),
        address4 = Some("Greater London"),
        postcode = Some("SW1A 1AA"),
        phone = Some("07777123456"),
        nino = Some("AB123456C"),
        isUkCompany = None,
        hasNino = Some("YES"),
        dateOfBirth = Some("1980-01-15"),
        registrationNumber = None,
        placeOfRegistration = None
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "isCompany").as[String] mustBe "NO"
      (json \ "isTrustee").as[String] mustBe "NO"
      (json \ "isConnectedToVendor").as[String] mustBe "NO"
      (json \ "isRepresentedByAgent").as[String] mustBe "NO"
      (json \ "title").as[String] mustBe "Mr"
      (json \ "surname").as[String] mustBe "Smith"
      (json \ "forename1").as[String] mustBe "John"
      (json \ "forename2").as[String] mustBe "James"
      (json \ "houseNumber").as[String] mustBe "123"
      (json \ "address1").as[String] mustBe "Main Street"
      (json \ "address2").as[String] mustBe "Apartment 4B"
      (json \ "address3").as[String] mustBe "City Center"
      (json \ "address4").as[String] mustBe "Greater London"
      (json \ "postcode").as[String] mustBe "SW1A 1AA"
      (json \ "phone").as[String] mustBe "07777123456"
      (json \ "nino").as[String] mustBe "AB123456C"
      (json \ "hasNino").as[String] mustBe "YES"
      (json \ "dateOfBirth").as[String] mustBe "1980-01-15"
    }

    "must serialize to JSON correctly with all fields populated for company" in {
      val request = CreatePurchaserRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        isCompany = Some("YES"),
        isTrustee = Some("NO"),
        isConnectedToVendor = Some("NO"),
        isRepresentedByAgent = Some("NO"),
        title = None,
        surname = None,
        forename1 = None,
        forename2 = None,
        companyName = Some("Tech Corp Ltd"),
        houseNumber = None,
        address1 = Some("Business Park"),
        address2 = None,
        address3 = None,
        address4 = None,
        postcode = Some("EC1A 1BB"),
        phone = Some("02012345678"),
        nino = None,
        isUkCompany = Some("YES"),
        hasNino = Some("NO"),
        dateOfBirth = None,
        registrationNumber = Some("12345678"),
        placeOfRegistration = None
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "isCompany").as[String] mustBe "YES"
      (json \ "companyName").as[String] mustBe "Tech Corp Ltd"
      (json \ "address1").as[String] mustBe "Business Park"
      (json \ "postcode").as[String] mustBe "EC1A 1BB"
      (json \ "phone").as[String] mustBe "02012345678"
      (json \ "isUkCompany").as[String] mustBe "YES"
      (json \ "hasNino").as[String] mustBe "NO"
      (json \ "registrationNumber").as[String] mustBe "12345678"
    }

    "must serialize to JSON correctly with only required fields" in {
      val request = CreatePurchaserRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        isCompany = Some("NO"),
        isTrustee = Some("NO"),
        isConnectedToVendor = Some("NO"),
        isRepresentedByAgent = Some("NO"),
        title = None,
        surname = None,
        forename1 = None,
        forename2 = None,
        companyName = None,
        houseNumber = None,
        address1 = Some("Main Street"),
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

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "isCompany").as[String] mustBe "NO"
      (json \ "address1").as[String] mustBe "Main Street"
      (json \ "title").toOption mustBe None
      (json \ "surname").toOption mustBe None
      (json \ "companyName").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "isCompany"            -> "NO",
        "isTrustee"            -> "NO",
        "isConnectedToVendor"  -> "NO",
        "isRepresentedByAgent" -> "NO",
        "title"                -> "Mr",
        "surname"              -> "Smith",
        "forename1"            -> "John",
        "forename2"            -> "James",
        "houseNumber"          -> "123",
        "address1"             -> "Main Street",
        "address2"             -> "Apartment 4B",
        "address3"             -> "City Center",
        "address4"             -> "Greater London",
        "postcode"             -> "SW1A 1AA",
        "phone"                -> "07777123456",
        "nino"                 -> "AB123456C",
        "hasNino"              -> "YES",
        "dateOfBirth"          -> "1980-01-15"
      )

      val result = json.validate[CreatePurchaserRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.isCompany mustBe Some("NO")
      request.isTrustee mustBe Some("NO")
      request.isConnectedToVendor mustBe Some("NO")
      request.isRepresentedByAgent mustBe Some("NO")
      request.title mustBe Some("Mr")
      request.surname mustBe Some("Smith")
      request.forename1 mustBe Some("John")
      request.forename2 mustBe Some("James")
      request.houseNumber mustBe Some("123")
      request.address1 mustBe Some("Main Street")
      request.address2 mustBe Some("Apartment 4B")
      request.postcode mustBe Some("SW1A 1AA")
      request.phone mustBe Some("07777123456")
      request.nino mustBe Some("AB123456C")
      request.hasNino mustBe Some("YES")
      request.dateOfBirth mustBe Some("1980-01-15")
    }

    "must deserialize from JSON correctly with only required fields" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "isCompany"            -> "NO",
        "isTrustee"            -> "NO",
        "isConnectedToVendor"  -> "NO",
        "isRepresentedByAgent" -> "NO",
        "address1"             -> "Main Street"
      )

      val result = json.validate[CreatePurchaserRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.isCompany mustBe Some("NO")
      request.address1 mustBe Some("Main Street")
      request.title mustBe None
      request.surname mustBe None
      request.companyName mustBe None
    }

    "must fail to deserialize when required field stornId is missing" in {
      val json = Json.obj(
        "returnResourceRef"    -> "100001",
        "isCompany"            -> "NO",
        "isTrustee"            -> "NO",
        "isConnectedToVendor"  -> "NO",
        "isRepresentedByAgent" -> "NO",
        "address1"             -> "Main Street"
      )

      val result = json.validate[CreatePurchaserRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field returnResourceRef is missing" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "isCompany"            -> "NO",
        "isTrustee"            -> "NO",
        "isConnectedToVendor"  -> "NO",
        "isRepresentedByAgent" -> "NO",
        "address1"             -> "Main Street"
      )

      val result = json.validate[CreatePurchaserRequest]

      result.isError mustBe true
    }

  }

  "CreatePurchaserReturn" - {

    "must serialize to JSON correctly" in {
      val response = CreatePurchaserReturn(
        purchaserResourceRef = "P100001",
        purchaserId = "PID123"
      )

      val json = Json.toJson(response)

      (json \ "purchaserResourceRef").as[String] mustBe "P100001"
      (json \ "purchaserId").as[String] mustBe "PID123"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "purchaserResourceRef" -> "P100001",
        "purchaserId"          -> "PID123"
      )

      val result = json.validate[CreatePurchaserReturn]

      result mustBe a[JsSuccess[_]]
      val response = result.get

      response.purchaserResourceRef mustBe "P100001"
      response.purchaserId mustBe "PID123"
    }

    "must fail to deserialize when purchaserResourceRef is missing" in {
      val json = Json.obj("purchaserId" -> "PID123")

      val result = json.validate[CreatePurchaserReturn]

      result.isError mustBe true
    }

    "must fail to deserialize when purchaserId is missing" in {
      val json = Json.obj("purchaserResourceRef" -> "P100001")

      val result = json.validate[CreatePurchaserReturn]

      result.isError mustBe true
    }
  }

  "UpdatePurchaserRequest" - {

    "must serialize to JSON correctly with all fields populated" in {
      val request = UpdatePurchaserRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
        isCompany = Some("NO"),
        isTrustee = Some("NO"),
        isConnectedToVendor = Some("YES"),
        isRepresentedByAgent = Some("YES"),
        title = Some("Mrs"),
        surname = Some("Doe"),
        forename1 = Some("Jane"),
        forename2 = Some("Marie"),
        companyName = None,
        houseNumber = Some("456"),
        address1 = Some("Oak Avenue"),
        address2 = Some("Suite 10"),
        address3 = Some("Downtown"),
        address4 = Some("Greater Manchester"),
        postcode = Some("W1A 1AA"),
        phone = Some("07777654321"),
        nino = Some("CD987654B"),
        nextPurchaserId = Some("P100002"),
        isUkCompany = None,
        hasNino = Some("YES"),
        dateOfBirth = Some("1985-05-20"),
        registrationNumber = None,
        placeOfRegistration = None
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "purchaserResourceRef").as[String] mustBe "P100001"
      (json \ "isCompany").as[String] mustBe "NO"
      (json \ "isTrustee").as[String] mustBe "NO"
      (json \ "isConnectedToVendor").as[String] mustBe "YES"
      (json \ "isRepresentedByAgent").as[String] mustBe "YES"
      (json \ "title").as[String] mustBe "Mrs"
      (json \ "surname").as[String] mustBe "Doe"
      (json \ "forename1").as[String] mustBe "Jane"
      (json \ "forename2").as[String] mustBe "Marie"
      (json \ "houseNumber").as[String] mustBe "456"
      (json \ "address1").as[String] mustBe "Oak Avenue"
      (json \ "address2").as[String] mustBe "Suite 10"
      (json \ "postcode").as[String] mustBe "W1A 1AA"
      (json \ "phone").as[String] mustBe "07777654321"
      (json \ "nino").as[String] mustBe "CD987654B"
      (json \ "nextPurchaserId").as[String] mustBe "P100002"
      (json \ "hasNino").as[String] mustBe "YES"
      (json \ "dateOfBirth").as[String] mustBe "1985-05-20"
    }

    "must serialize to JSON correctly with only required fields" in {
      val request = UpdatePurchaserRequest(
        stornId = "STORN99999",
        returnResourceRef = "100002",
        purchaserResourceRef = "P100002",
        isCompany = Some("YES"),
        isTrustee = Some("NO"),
        isConnectedToVendor = Some("NO"),
        isRepresentedByAgent = Some("NO"),
        title = None,
        surname = None,
        forename1 = None,
        forename2 = None,
        companyName = Some("Updated Corp Ltd"),
        houseNumber = None,
        address1 = Some("New Business Park"),
        address2 = None,
        address3 = None,
        address4 = None,
        postcode = None,
        phone = None,
        nino = None,
        nextPurchaserId = None,
        isUkCompany = Some("YES"),
        hasNino = Some("NO"),
        dateOfBirth = None,
        registrationNumber = Some("87654321"),
        placeOfRegistration = None
      )

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "purchaserResourceRef").as[String] mustBe "P100002"
      (json \ "isCompany").as[String] mustBe "YES"
      (json \ "companyName").as[String] mustBe "Updated Corp Ltd"
      (json \ "address1").as[String] mustBe "New Business Park"
      (json \ "isUkCompany").as[String] mustBe "YES"
      (json \ "registrationNumber").as[String] mustBe "87654321"
      (json \ "nextPurchaserId").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "purchaserResourceRef" -> "P100001",
        "isCompany"            -> "NO",
        "isTrustee"            -> "NO",
        "isConnectedToVendor"  -> "YES",
        "isRepresentedByAgent" -> "YES",
        "title"                -> "Mrs",
        "surname"              -> "Doe",
        "forename1"            -> "Jane",
        "forename2"            -> "Marie",
        "houseNumber"          -> "456",
        "address1"             -> "Oak Avenue",
        "address2"             -> "Suite 10",
        "postcode"             -> "W1A 1AA",
        "phone"                -> "07777654321",
        "nino"                 -> "CD987654B",
        "nextPurchaserId"      -> "P100002",
        "hasNino"              -> "YES",
        "dateOfBirth"          -> "1985-05-20"
      )

      val result = json.validate[UpdatePurchaserRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.purchaserResourceRef mustBe "P100001"
      request.isCompany mustBe Some("NO")
      request.isConnectedToVendor mustBe Some("YES")
      request.title mustBe Some("Mrs")
      request.surname mustBe Some("Doe")
      request.forename1 mustBe Some("Jane")
      request.nextPurchaserId mustBe Some("P100002")
    }

    "must deserialize from JSON correctly with only required fields" in {
      val json = Json.obj(
        "stornId"              -> "STORN99999",
        "returnResourceRef"    -> "100002",
        "purchaserResourceRef" -> "P100002",
        "isCompany"            -> "YES",
        "isTrustee"            -> "NO",
        "isConnectedToVendor"  -> "NO",
        "isRepresentedByAgent" -> "NO",
        "address1"             -> "New Business Park"
      )

      val result = json.validate[UpdatePurchaserRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN99999"
      request.returnResourceRef mustBe "100002"
      request.purchaserResourceRef mustBe "P100002"
      request.isCompany mustBe Some("YES")
      request.address1 mustBe Some("New Business Park")
      request.nextPurchaserId mustBe None
    }

    "must fail to deserialize when required field purchaserResourceRef is missing" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "isCompany"            -> "NO",
        "isTrustee"            -> "NO",
        "isConnectedToVendor"  -> "NO",
        "isRepresentedByAgent" -> "NO",
        "address1"             -> "Oak Avenue"
      )

      val result = json.validate[UpdatePurchaserRequest]

      result.isError mustBe true
    }
  }

  "UpdatePurchaserReturn" - {

    "must serialize to JSON correctly when updated is true" in {
      val response = UpdatePurchaserReturn(updated = true)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when updated is false" in {
      val response = UpdatePurchaserReturn(updated = false)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when updated is true" in {
      val json = Json.obj("updated" -> true)

      val result = json.validate[UpdatePurchaserReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe true
    }

    "must fail to deserialize when updated field is missing" in {
      val json = Json.obj()

      val result = json.validate[UpdatePurchaserReturn]

      result.isError mustBe true
    }
  }

  "DeletePurchaserRequest" - {

    "must serialize to JSON correctly" in {
      val request = DeletePurchaserRequest(
        storn = "STORN12345",
        purchaserResourceRef = "P100001",
        returnResourceRef = "100001"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "purchaserResourceRef").as[String] mustBe "P100001"
      (json \ "returnResourceRef").as[String] mustBe "100001"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn"                -> "STORN12345",
        "purchaserResourceRef" -> "P100001",
        "returnResourceRef"    -> "100001"
      )

      val result = json.validate[DeletePurchaserRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.purchaserResourceRef mustBe "P100001"
      request.returnResourceRef mustBe "100001"
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "purchaserResourceRef" -> "P100001",
        "returnResourceRef"    -> "100001"
      )

      val result = json.validate[DeletePurchaserRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when purchaserResourceRef is missing" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[DeletePurchaserRequest]

      result.isError mustBe true
    }
  }

  "DeletePurchaserReturn" - {

    "must serialize to JSON correctly when deleted is true" in {
      val response = DeletePurchaserReturn(deleted = true)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when deleted is false" in {
      val response = DeletePurchaserReturn(deleted = false)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when deleted is true" in {
      val json = Json.obj("deleted" -> true)

      val result = json.validate[DeletePurchaserReturn]

      result mustBe a[JsSuccess[_]]
      result.get.deleted mustBe true
    }

    "must fail to deserialize when deleted field is missing" in {
      val json = Json.obj()

      val result = json.validate[DeletePurchaserReturn]

      result.isError mustBe true
    }
  }

  "CreateCompanyDetailsRequest" - {

    "must serialize to JSON correctly with all fields populated" in {
      val request = CreateCompanyDetailsRequest(
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

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "purchaserResourceRef").as[String] mustBe "P100001"
      (json \ "utr").as[String] mustBe "1234567890"
      (json \ "vatReference").as[String] mustBe "GB123456789"
      (json \ "compTypeBank").as[String] mustBe "N"
      (json \ "compTypeBuilder").as[String] mustBe "N"
      (json \ "compTypeOcompany").as[String] mustBe "Y"
      (json \ "compTypeProperty").as[String] mustBe "N"
    }

    "must serialize to JSON correctly with only required fields" in {
      val request = CreateCompanyDetailsRequest(
        stornId = "STORN12345",
        returnResourceRef = "100001",
        purchaserResourceRef = "P100001",
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

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "purchaserResourceRef").as[String] mustBe "P100001"
      (json \ "utr").toOption mustBe None
      (json \ "vatReference").toOption mustBe None
      (json \ "compTypeBank").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "purchaserResourceRef" -> "P100001",
        "utr"                  -> "1234567890",
        "vatReference"         -> "GB123456789",
        "compTypeBank"         -> "N",
        "compTypeBuilder"      -> "N",
        "compTypeBuildsoc"     -> "N",
        "compTypeCentgov"      -> "N",
        "compTypeIndividual"   -> "N",
        "compTypeInsurance"    -> "N",
        "compTypeLocalauth"    -> "N",
        "compTypeOcharity"     -> "N",
        "compTypeOcompany"     -> "Y",
        "compTypeOfinancial"   -> "N",
        "compTypePartship"     -> "N",
        "compTypeProperty"     -> "N",
        "compTypePubliccorp"   -> "N",
        "compTypeSoletrader"   -> "N",
        "compTypePenfund"      -> "N"
      )

      val result = json.validate[CreateCompanyDetailsRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.purchaserResourceRef mustBe "P100001"
      request.utr mustBe Some("1234567890")
      request.vatReference mustBe Some("GB123456789")
      request.compTypeBank mustBe Some("N")
      request.compTypeOcompany mustBe Some("Y")
    }

    "must deserialize from JSON correctly with only required fields" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "purchaserResourceRef" -> "P100001"
      )

      val result = json.validate[CreateCompanyDetailsRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.purchaserResourceRef mustBe "P100001"
      request.utr mustBe None
      request.vatReference mustBe None
      request.compTypeBank mustBe None
    }

    "must fail to deserialize when required field stornId is missing" in {
      val json = Json.obj(
        "returnResourceRef"    -> "100001",
        "purchaserResourceRef" -> "P100001"
      )

      val result = json.validate[CreateCompanyDetailsRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field returnResourceRef is missing" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "purchaserResourceRef" -> "P100001"
      )

      val result = json.validate[CreateCompanyDetailsRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when required field purchaserResourceRef is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[CreateCompanyDetailsRequest]

      result.isError mustBe true
    }
  }

  "CreateCompanyDetailsReturn" - {

    "must serialize to JSON correctly" in {
      val response = CreateCompanyDetailsReturn(
        companyDetailsId = "CDID123"
      )

      val json = Json.toJson(response)

      (json \ "companyDetailsId").as[String] mustBe "CDID123"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "companyDetailsId" -> "CDID123"
      )

      val result = json.validate[CreateCompanyDetailsReturn]

      result mustBe a[JsSuccess[_]]
      val response = result.get

      response.companyDetailsId mustBe "CDID123"
    }

    "must fail to deserialize when companyDetailsId is missing" in {
      val json = Json.obj("companyDetailsResourceRef" -> "CD100001")

      val result = json.validate[CreateCompanyDetailsReturn]

      result.isError mustBe true
    }
  }

  "UpdateCompanyDetailsRequest" - {

    "must serialize to JSON correctly with all fields populated" in {
      val request = UpdateCompanyDetailsRequest(
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

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "purchaserResourceRef").as[String] mustBe "P100001"
      (json \ "utr").as[String] mustBe "9876543210"
      (json \ "vatReference").as[String] mustBe "GB987654321"
      (json \ "compTypeBuilder").as[String] mustBe "Y"
      (json \ "compTypeProperty").as[String] mustBe "Y"
      (json \ "compTypeOcompany").as[String] mustBe "Y"
    }

    "must serialize to JSON correctly with only required fields" in {
      val request = UpdateCompanyDetailsRequest(
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

      val json = Json.toJson(request)

      (json \ "stornId").as[String] mustBe "STORN99999"
      (json \ "returnResourceRef").as[String] mustBe "100002"
      (json \ "purchaserResourceRef").as[String] mustBe "P100002"
      (json \ "utr").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "stornId"              -> "STORN12345",
        "returnResourceRef"    -> "100001",
        "purchaserResourceRef" -> "P100001",
        "utr"                  -> "9876543210",
        "vatReference"         -> "GB987654321",
        "compTypeBuilder"      -> "Y",
        "compTypeProperty"     -> "Y",
        "compTypeOcompany"     -> "Y"
      )

      val result = json.validate[UpdateCompanyDetailsRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN12345"
      request.utr mustBe Some("9876543210")
      request.compTypeBuilder mustBe Some("Y")
      request.compTypeProperty mustBe Some("Y")
    }

    "must deserialize from JSON correctly with only required fields" in {
      val json = Json.obj(
        "stornId"              -> "STORN99999",
        "returnResourceRef"    -> "100002",
        "purchaserResourceRef" -> "P100002"
      )

      val result = json.validate[UpdateCompanyDetailsRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.stornId mustBe "STORN99999"
      request.returnResourceRef mustBe "100002"
      request.purchaserResourceRef mustBe "P100002"
      request.utr mustBe None
    }

    "must fail to deserialize when required field purchaserResourceRef is missing" in {
      val json = Json.obj(
        "stornId"           -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      val result = json.validate[UpdateCompanyDetailsRequest]

      result.isError mustBe true
    }
  }

  "UpdateCompanyDetailsReturn" - {

    "must serialize to JSON correctly when updated is true" in {
      val response = UpdateCompanyDetailsReturn(updated = true)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when updated is false" in {
      val response = UpdateCompanyDetailsReturn(updated = false)

      val json = Json.toJson(response)

      (json \ "updated").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when updated is true" in {
      val json = Json.obj("updated" -> true)

      val result = json.validate[UpdateCompanyDetailsReturn]

      result mustBe a[JsSuccess[_]]
      result.get.updated mustBe true
    }

    "must fail to deserialize when updated field is missing" in {
      val json = Json.obj()

      val result = json.validate[UpdateCompanyDetailsReturn]

      result.isError mustBe true
    }
  }

  "DeleteCompanyDetailsRequest" - {

    "must serialize to JSON correctly" in {
      val request = DeleteCompanyDetailsRequest(
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

      val result = json.validate[DeleteCompanyDetailsRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj("returnResourceRef" -> "100001")

      val result = json.validate[DeleteCompanyDetailsRequest]

      result.isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj("storn" -> "STORN12345")

      val result = json.validate[DeleteCompanyDetailsRequest]

      result.isError mustBe true
    }
  }

  "DeleteCompanyDetailsReturn" - {

    "must serialize to JSON correctly when deleted is true" in {
      val response = DeleteCompanyDetailsReturn(deleted = true)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when deleted is false" in {
      val response = DeleteCompanyDetailsReturn(deleted = false)

      val json = Json.toJson(response)

      (json \ "deleted").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when deleted is true" in {
      val json = Json.obj("deleted" -> true)

      val result = json.validate[DeleteCompanyDetailsReturn]

      result mustBe a[JsSuccess[_]]
      result.get.deleted mustBe true
    }

    "must fail to deserialize when deleted field is missing" in {
      val json = Json.obj()

      val result = json.validate[DeleteCompanyDetailsReturn]

      result.isError mustBe true
    }
  }
}
