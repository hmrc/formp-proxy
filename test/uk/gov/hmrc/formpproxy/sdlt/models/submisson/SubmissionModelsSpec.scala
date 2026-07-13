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

package uk.gov.hmrc.formpproxy.sdlt.models.submisson

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.formpproxy.sdlt.models.submission.*

class SubmissionModelsSpec extends AnyFreeSpec with Matchers {

  "CreateSubmissionRequest" - {

    "must serialize to JSON correctly" in {
      val request = CreateSubmissionRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        email = Some("filer@example.com")
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "email").as[String] mustBe "filer@example.com"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001",
        "email"             -> "filer@example.com"
      )

      val result = json.validate[CreateSubmissionRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.email mustBe Some("filer@example.com")
    }

    "must fail to deserialize when required field storn is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "email"             -> "filer@example.com"
      )

      json.validate[CreateSubmissionRequest].isError mustBe true
    }

    "must fail to deserialize when required field returnResourceRef is missing" in {
      val json = Json.obj(
        "storn" -> "STORN12345",
        "email" -> "filer@example.com"
      )

      json.validate[CreateSubmissionRequest].isError mustBe true
    }

    "must deserialize when field email is missing" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      json.validate[CreateSubmissionRequest].isError mustBe false
    }
  }

  "SubmissionUpdate" - {

    "must serialize to JSON correctly with all fields populated" in {
      val update = SubmissionUpdate(
        IRMarkRecieved = Some("IRMARK-RECV-001"),
        utrn = Some("UTRN123456789"),
        email = Some("filer@example.com"),
        submissionRequestDate = Some("2026-01-15T10:00:00Z"),
        acceptedDate = Some("2026-01-15T10:05:00Z"),
        submittableStatus = Some("SUBMITTED"),
        govTalkErrorCode = Some("0"),
        govTalkErrorType = Some("business"),
        govTalkErrorMessage = Some("No errors"),
        IRMarkSent = Some("IRMARK-SENT-001")
      )

      val json = Json.toJson(update)

      (json \ "IRMarkRecieved").as[String] mustBe "IRMARK-RECV-001"
      (json \ "utrn").as[String] mustBe "UTRN123456789"
      (json \ "email").as[String] mustBe "filer@example.com"
      (json \ "submissionRequestDate").as[String] mustBe "2026-01-15T10:00:00Z"
      (json \ "acceptedDate").as[String] mustBe "2026-01-15T10:05:00Z"
      (json \ "submittableStatus").as[String] mustBe "SUBMITTED"
      (json \ "govTalkErrorCode").as[String] mustBe "0"
      (json \ "govTalkErrorType").as[String] mustBe "business"
      (json \ "govTalkErrorMessage").as[String] mustBe "No errors"
      (json \ "IRMarkSent").as[String] mustBe "IRMARK-SENT-001"
    }

    "must serialize to JSON correctly when all fields are None" in {
      val update = SubmissionUpdate(
        IRMarkRecieved = None,
        utrn = None,
        email = None,
        submissionRequestDate = None,
        acceptedDate = None,
        submittableStatus = None,
        govTalkErrorCode = None,
        govTalkErrorType = None,
        govTalkErrorMessage = None,
        IRMarkSent = None
      )

      val json = Json.toJson(update)

      (json \ "IRMarkRecieved").toOption mustBe None
      (json \ "utrn").toOption mustBe None
      (json \ "email").toOption mustBe None
      (json \ "submissionRequestDate").toOption mustBe None
      (json \ "acceptedDate").toOption mustBe None
      (json \ "submittableStatus").toOption mustBe None
      (json \ "govTalkErrorCode").toOption mustBe None
      (json \ "govTalkErrorType").toOption mustBe None
      (json \ "govTalkErrorMessage").toOption mustBe None
      (json \ "IRMarkSent").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "IRMarkRecieved"        -> "IRMARK-RECV-001",
        "utrn"                  -> "UTRN123456789",
        "email"                 -> "filer@example.com",
        "submissionRequestDate" -> "2026-01-15T10:00:00Z",
        "acceptedDate"          -> "2026-01-15T10:05:00Z",
        "submittableStatus"     -> "SUBMITTED",
        "govTalkErrorCode"      -> "0",
        "govTalkErrorType"      -> "business",
        "govTalkErrorMessage"   -> "No errors",
        "IRMarkSent"            -> "IRMARK-SENT-001"
      )

      val result = json.validate[SubmissionUpdate]

      result mustBe a[JsSuccess[_]]
      val update = result.get

      update.IRMarkRecieved mustBe Some("IRMARK-RECV-001")
      update.utrn mustBe Some("UTRN123456789")
      update.email mustBe Some("filer@example.com")
      update.submissionRequestDate mustBe Some("2026-01-15T10:00:00Z")
      update.acceptedDate mustBe Some("2026-01-15T10:05:00Z")
      update.submittableStatus mustBe Some("SUBMITTED")
      update.govTalkErrorCode mustBe Some("0")
      update.govTalkErrorType mustBe Some("business")
      update.govTalkErrorMessage mustBe Some("No errors")
      update.IRMarkSent mustBe Some("IRMARK-SENT-001")
    }

    "must deserialize from an empty JSON object with all fields as None" in {
      val json = Json.obj()

      val result = json.validate[SubmissionUpdate]

      result mustBe a[JsSuccess[_]]
      val update = result.get

      update.IRMarkRecieved mustBe None
      update.utrn mustBe None
      update.email mustBe None
      update.submissionRequestDate mustBe None
      update.acceptedDate mustBe None
      update.submittableStatus mustBe None
      update.govTalkErrorCode mustBe None
      update.govTalkErrorType mustBe None
      update.govTalkErrorMessage mustBe None
      update.IRMarkSent mustBe None
    }
  }

  "UpdateSubmissionRequest" - {

    "must serialize to JSON correctly with a populated submission" in {
      val request = UpdateSubmissionRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        submission = SubmissionUpdate(
          IRMarkRecieved = Some("IRMARK-RECV-001"),
          utrn = Some("UTRN123456789"),
          email = Some("filer@example.com"),
          submissionRequestDate = None,
          acceptedDate = None,
          submittableStatus = Some("SUBMITTED"),
          govTalkErrorCode = None,
          govTalkErrorType = None,
          govTalkErrorMessage = None,
          IRMarkSent = None
        )
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "submission" \ "IRMarkRecieved").as[String] mustBe "IRMARK-RECV-001"
      (json \ "submission" \ "utrn").as[String] mustBe "UTRN123456789"
      (json \ "submission" \ "email").as[String] mustBe "filer@example.com"
      (json \ "submission" \ "submittableStatus").as[String] mustBe "SUBMITTED"
      (json \ "submission" \ "acceptedDate").toOption mustBe None
    }

    "must deserialize from JSON correctly with a populated submission" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001",
        "submission"        -> Json.obj(
          "utrn"              -> "UTRN123456789",
          "submittableStatus" -> "SUBMITTED"
        )
      )

      val result = json.validate[UpdateSubmissionRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.submission.utrn mustBe Some("UTRN123456789")
      request.submission.submittableStatus mustBe Some("SUBMITTED")
      request.submission.email mustBe None
    }

    "must deserialize when submission is an empty object" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001",
        "submission"        -> Json.obj()
      )

      val result = json.validate[UpdateSubmissionRequest]

      result mustBe a[JsSuccess[_]]
      result.get.submission.utrn mustBe None
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "returnResourceRef" -> "100001",
        "submission"        -> Json.obj()
      )

      json.validate[UpdateSubmissionRequest].isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      val json = Json.obj(
        "storn"      -> "STORN12345",
        "submission" -> Json.obj()
      )

      json.validate[UpdateSubmissionRequest].isError mustBe true
    }

    "must fail to deserialize when submission is missing" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      json.validate[UpdateSubmissionRequest].isError mustBe true
    }
  }

  "SubmissionErrorDetail" - {

    "must serialize to JSON correctly" in {
      val detail = SubmissionErrorDetail(
        position = "/GovTalkMessage/Body/IRenvelope",
        errorMessage = "Invalid effective date"
      )

      val json = Json.toJson(detail)

      (json \ "position").as[String] mustBe "/GovTalkMessage/Body/IRenvelope"
      (json \ "errorMessage").as[String] mustBe "Invalid effective date"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "position"     -> "/GovTalkMessage/Body/IRenvelope",
        "errorMessage" -> "Invalid effective date"
      )

      val result = json.validate[SubmissionErrorDetail]

      result mustBe a[JsSuccess[_]]
      result.get.position mustBe "/GovTalkMessage/Body/IRenvelope"
      result.get.errorMessage mustBe "Invalid effective date"
    }

    "must fail to deserialize when position is missing" in {
      val json = Json.obj("errorMessage" -> "Invalid effective date")

      json.validate[SubmissionErrorDetail].isError mustBe true
    }

    "must fail to deserialize when errorMessage is missing" in {
      val json = Json.obj("position" -> "/GovTalkMessage/Body/IRenvelope")

      json.validate[SubmissionErrorDetail].isError mustBe true
    }
  }

  "CreateSubmissionErrorDetailRequest" - {

    "must serialize to JSON correctly with a nested error detail" in {
      val request = CreateSubmissionErrorDetailRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        submissionErrorDetails = SubmissionErrorDetail(
          position = "/GovTalkMessage/Body/IRenvelope",
          errorMessage = "Invalid effective date"
        )
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "submissionErrorDetails" \ "position").as[String] mustBe "/GovTalkMessage/Body/IRenvelope"
      (json \ "submissionErrorDetails" \ "errorMessage").as[String] mustBe "Invalid effective date"
    }

    "must deserialize from JSON correctly with a nested error detail" in {
      val json = Json.obj(
        "storn"                  -> "STORN12345",
        "returnResourceRef"      -> "100001",
        "submissionErrorDetails" -> Json.obj(
          "position"     -> "/GovTalkMessage/Body/IRenvelope",
          "errorMessage" -> "Invalid effective date"
        )
      )

      val result = json.validate[CreateSubmissionErrorDetailRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.submissionErrorDetails.position mustBe "/GovTalkMessage/Body/IRenvelope"
      request.submissionErrorDetails.errorMessage mustBe "Invalid effective date"
    }

    "must fail to deserialize when submissionErrorDetails is missing" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      json.validate[CreateSubmissionErrorDetailRequest].isError mustBe true
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj(
        "returnResourceRef"      -> "100001",
        "submissionErrorDetails" -> Json.obj(
          "position"     -> "/x",
          "errorMessage" -> "err"
        )
      )

      json.validate[CreateSubmissionErrorDetailRequest].isError mustBe true
    }
  }

  "DeleteSubmissionErrorDetailRequest" - {

    "must serialize to JSON correctly" in {
      val request = DeleteSubmissionErrorDetailRequest(
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

      val result = json.validate[DeleteSubmissionErrorDetailRequest]

      result mustBe a[JsSuccess[_]]
      result.get.storn mustBe "STORN12345"
      result.get.returnResourceRef mustBe "100001"
    }

    "must fail to deserialize when storn is missing" in {
      Json.obj("returnResourceRef" -> "100001").validate[DeleteSubmissionErrorDetailRequest].isError mustBe true
    }

    "must fail to deserialize when returnResourceRef is missing" in {
      Json.obj("storn" -> "STORN12345").validate[DeleteSubmissionErrorDetailRequest].isError mustBe true
    }
  }

  "CreateSubmissionReturn" - {

    "must serialize to JSON correctly when success is true" in {
      (Json.toJson(CreateSubmissionReturn(success = true)) \ "success").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when success is false" in {
      (Json.toJson(CreateSubmissionReturn(success = false)) \ "success").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when success is true" in {
      val result = Json.obj("success" -> true).validate[CreateSubmissionReturn]
      result mustBe a[JsSuccess[_]]
      result.get.success mustBe true
    }

    "must deserialize from JSON correctly when success is false" in {
      val result = Json.obj("success" -> false).validate[CreateSubmissionReturn]
      result mustBe a[JsSuccess[_]]
      result.get.success mustBe false
    }

    "must fail to deserialize when success is missing" in {
      Json.obj().validate[CreateSubmissionReturn].isError mustBe true
    }
  }

  "UpdateSubmissionReturn" - {

    "must serialize to JSON correctly when success is true" in {
      (Json.toJson(UpdateSubmissionReturn(success = true)) \ "success").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when success is false" in {
      (Json.toJson(UpdateSubmissionReturn(success = false)) \ "success").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when success is true" in {
      val result = Json.obj("success" -> true).validate[UpdateSubmissionReturn]
      result mustBe a[JsSuccess[_]]
      result.get.success mustBe true
    }

    "must deserialize from JSON correctly when success is false" in {
      val result = Json.obj("success" -> false).validate[UpdateSubmissionReturn]
      result mustBe a[JsSuccess[_]]
      result.get.success mustBe false
    }

    "must fail to deserialize when success is missing" in {
      Json.obj().validate[UpdateSubmissionReturn].isError mustBe true
    }
  }

  "CreateSubmissionErrorDetailReturn" - {

    "must serialize to JSON correctly when success is true" in {
      (Json.toJson(CreateSubmissionErrorDetailReturn(success = true)) \ "success").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when success is false" in {
      (Json.toJson(CreateSubmissionErrorDetailReturn(success = false)) \ "success").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when success is true" in {
      val result = Json.obj("success" -> true).validate[CreateSubmissionErrorDetailReturn]
      result mustBe a[JsSuccess[_]]
      result.get.success mustBe true
    }

    "must deserialize from JSON correctly when success is false" in {
      val result = Json.obj("success" -> false).validate[CreateSubmissionErrorDetailReturn]
      result mustBe a[JsSuccess[_]]
      result.get.success mustBe false
    }

    "must fail to deserialize when success is missing" in {
      Json.obj().validate[CreateSubmissionErrorDetailReturn].isError mustBe true
    }
  }

  "DeleteSubmissionErrorDetailReturn" - {

    "must serialize to JSON correctly when success is true" in {
      (Json.toJson(DeleteSubmissionErrorDetailReturn(success = true)) \ "success").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when success is false" in {
      (Json.toJson(DeleteSubmissionErrorDetailReturn(success = false)) \ "success").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when success is true" in {
      val result = Json.obj("success" -> true).validate[DeleteSubmissionErrorDetailReturn]
      result mustBe a[JsSuccess[_]]
      result.get.success mustBe true
    }

    "must deserialize from JSON correctly when success is false" in {
      val result = Json.obj("success" -> false).validate[DeleteSubmissionErrorDetailReturn]
      result mustBe a[JsSuccess[_]]
      result.get.success mustBe false
    }

    "must fail to deserialize when success is missing" in {
      Json.obj().validate[DeleteSubmissionErrorDetailReturn].isError mustBe true
    }
  }

  "GovTalkStatusReturn" - {

    "must serialize to JSON correctly when success is true" in {
      (Json.toJson(GovTalkStatusReturn(success = true)) \ "success").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when success is false" in {
      (Json.toJson(GovTalkStatusReturn(success = false)) \ "success").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when success is true" in {
      val result = Json.obj("success" -> true).validate[GovTalkStatusReturn]
      result mustBe a[JsSuccess[_]]
      result.get.success mustBe true
    }

    "must deserialize from JSON correctly when success is false" in {
      val result = Json.obj("success" -> false).validate[GovTalkStatusReturn]
      result mustBe a[JsSuccess[_]]
      result.get.success mustBe false
    }

    "must fail to deserialize when success is missing" in {
      Json.obj().validate[GovTalkStatusReturn].isError mustBe true
    }
  }

  "GovTalkStatusInitial" - {

    "must serialize to JSON correctly with all fields populated" in {
      val status = GovTalkStatusInitial(
        formLock = "LOCK-001",
        createTimestamp = "2026-01-15T10:00:00Z",
        endStateTimestamp = Some("2026-01-15T10:10:00Z"),
        lastMessageTimestamp = "2026-01-15T10:05:00Z",
        numberOfPolls = "3",
        pollInterval = "10",
        protocolStatus = "SUBMITTED",
        gatewayUrl = "https://gateway.example.gov.uk/submission"
      )

      val json = Json.toJson(status)

      (json \ "formLock").as[String] mustBe "LOCK-001"
      (json \ "createTimestamp").as[String] mustBe "2026-01-15T10:00:00Z"
      (json \ "endStateTimestamp").as[String] mustBe "2026-01-15T10:10:00Z"
      (json \ "lastMessageTimestamp").as[String] mustBe "2026-01-15T10:05:00Z"
      (json \ "numberOfPolls").as[String] mustBe "3"
      (json \ "pollInterval").as[String] mustBe "10"
      (json \ "protocolStatus").as[String] mustBe "SUBMITTED"
      (json \ "gatewayUrl").as[String] mustBe "https://gateway.example.gov.uk/submission"
    }

    "must serialize to JSON correctly when the optional endStateTimestamp is None" in {
      val status = GovTalkStatusInitial(
        formLock = "LOCK-001",
        createTimestamp = "2026-01-15T10:00:00Z",
        endStateTimestamp = None,
        lastMessageTimestamp = "2026-01-15T10:05:00Z",
        numberOfPolls = "3",
        pollInterval = "10",
        protocolStatus = "SUBMITTED",
        gatewayUrl = "https://gateway.example.gov.uk/submission"
      )

      val json = Json.toJson(status)

      (json \ "endStateTimestamp").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "formLock"             -> "LOCK-001",
        "createTimestamp"      -> "2026-01-15T10:00:00Z",
        "endStateTimestamp"    -> "2026-01-15T10:10:00Z",
        "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
        "numberOfPolls"        -> "3",
        "pollInterval"         -> "10",
        "protocolStatus"       -> "SUBMITTED",
        "gatewayUrl"           -> "https://gateway.example.gov.uk/submission"
      )

      val result = json.validate[GovTalkStatusInitial]

      result mustBe a[JsSuccess[_]]
      val status = result.get

      status.formLock mustBe "LOCK-001"
      status.createTimestamp mustBe "2026-01-15T10:00:00Z"
      status.endStateTimestamp mustBe Some("2026-01-15T10:10:00Z")
      status.lastMessageTimestamp mustBe "2026-01-15T10:05:00Z"
      status.numberOfPolls mustBe "3"
      status.pollInterval mustBe "10"
      status.protocolStatus mustBe "SUBMITTED"
      status.gatewayUrl mustBe "https://gateway.example.gov.uk/submission"
    }

    "must deserialize from JSON correctly when endStateTimestamp is absent" in {
      val json = Json.obj(
        "formLock"             -> "LOCK-001",
        "createTimestamp"      -> "2026-01-15T10:00:00Z",
        "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
        "numberOfPolls"        -> "3",
        "pollInterval"         -> "10",
        "protocolStatus"       -> "SUBMITTED",
        "gatewayUrl"           -> "https://gateway.example.gov.uk/submission"
      )

      val result = json.validate[GovTalkStatusInitial]

      result mustBe a[JsSuccess[_]]
      result.get.endStateTimestamp mustBe None
    }

    "must fail to deserialize when required field formLock is missing" in {
      val json = Json.obj(
        "createTimestamp"      -> "2026-01-15T10:00:00Z",
        "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
        "numberOfPolls"        -> "3",
        "pollInterval"         -> "10",
        "protocolStatus"       -> "SUBMITTED",
        "gatewayUrl"           -> "https://gateway.example.gov.uk/submission"
      )

      json.validate[GovTalkStatusInitial].isError mustBe true
    }

    "must fail to deserialize when required field gatewayUrl is missing" in {
      val json = Json.obj(
        "formLock"             -> "LOCK-001",
        "createTimestamp"      -> "2026-01-15T10:00:00Z",
        "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
        "numberOfPolls"        -> "3",
        "pollInterval"         -> "10",
        "protocolStatus"       -> "SUBMITTED"
      )

      json.validate[GovTalkStatusInitial].isError mustBe true
    }
  }

  "GovTalkStatusReset" - {

    "must serialize to JSON correctly with all fields populated" in {
      val status = GovTalkStatusReset(
        formLock = "LOCK-001",
        createTimestamp = "2026-01-15T10:00:00Z",
        endStateTimestamp = Some("2026-01-15T10:10:00Z"),
        lastMessageTimestamp = "2026-01-15T10:05:00Z",
        numberOfPolls = "3",
        pollInterval = "10",
        protocolStatusOld = "SUBMITTED",
        protocolStatusNew = "ACKNOWLEDGED",
        gatewayUrl = "https://gateway.example.gov.uk/submission"
      )

      val json = Json.toJson(status)

      (json \ "formLock").as[String] mustBe "LOCK-001"
      (json \ "createTimestamp").as[String] mustBe "2026-01-15T10:00:00Z"
      (json \ "endStateTimestamp").as[String] mustBe "2026-01-15T10:10:00Z"
      (json \ "lastMessageTimestamp").as[String] mustBe "2026-01-15T10:05:00Z"
      (json \ "numberOfPolls").as[String] mustBe "3"
      (json \ "pollInterval").as[String] mustBe "10"
      (json \ "protocolStatusOld").as[String] mustBe "SUBMITTED"
      (json \ "protocolStatusNew").as[String] mustBe "ACKNOWLEDGED"
      (json \ "gatewayUrl").as[String] mustBe "https://gateway.example.gov.uk/submission"
    }

    "must serialize to JSON correctly when the optional endStateTimestamp is None" in {
      val status = GovTalkStatusReset(
        formLock = "LOCK-001",
        createTimestamp = "2026-01-15T10:00:00Z",
        endStateTimestamp = None,
        lastMessageTimestamp = "2026-01-15T10:05:00Z",
        numberOfPolls = "3",
        pollInterval = "10",
        protocolStatusOld = "SUBMITTED",
        protocolStatusNew = "ACKNOWLEDGED",
        gatewayUrl = "https://gateway.example.gov.uk/submission"
      )

      (Json.toJson(status) \ "endStateTimestamp").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "formLock"             -> "LOCK-001",
        "createTimestamp"      -> "2026-01-15T10:00:00Z",
        "endStateTimestamp"    -> "2026-01-15T10:10:00Z",
        "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
        "numberOfPolls"        -> "3",
        "pollInterval"         -> "10",
        "protocolStatusOld"    -> "SUBMITTED",
        "protocolStatusNew"    -> "ACKNOWLEDGED",
        "gatewayUrl"           -> "https://gateway.example.gov.uk/submission"
      )

      val result = json.validate[GovTalkStatusReset]

      result mustBe a[JsSuccess[_]]
      val status = result.get

      status.formLock mustBe "LOCK-001"
      status.endStateTimestamp mustBe Some("2026-01-15T10:10:00Z")
      status.protocolStatusOld mustBe "SUBMITTED"
      status.protocolStatusNew mustBe "ACKNOWLEDGED"
      status.gatewayUrl mustBe "https://gateway.example.gov.uk/submission"
    }

    "must deserialize from JSON correctly when endStateTimestamp is absent" in {
      val json = Json.obj(
        "formLock"             -> "LOCK-001",
        "createTimestamp"      -> "2026-01-15T10:00:00Z",
        "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
        "numberOfPolls"        -> "3",
        "pollInterval"         -> "10",
        "protocolStatusOld"    -> "SUBMITTED",
        "protocolStatusNew"    -> "ACKNOWLEDGED",
        "gatewayUrl"           -> "https://gateway.example.gov.uk/submission"
      )

      val result = json.validate[GovTalkStatusReset]

      result mustBe a[JsSuccess[_]]
      result.get.endStateTimestamp mustBe None
    }

    "must fail to deserialize when required field protocolStatusNew is missing" in {
      val json = Json.obj(
        "formLock"             -> "LOCK-001",
        "createTimestamp"      -> "2026-01-15T10:00:00Z",
        "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
        "numberOfPolls"        -> "3",
        "pollInterval"         -> "10",
        "protocolStatusOld"    -> "SUBMITTED",
        "gatewayUrl"           -> "https://gateway.example.gov.uk/submission"
      )

      json.validate[GovTalkStatusReset].isError mustBe true
    }
  }

  "GovTalkStatusLock" - {

    "must serialize to JSON correctly" in {
      val status = GovTalkStatusLock(
        formLockOld = "LOCK-001",
        formLockNew = "LOCK-002",
        pollInterval = "10",
        gatewayUrl = "https://gateway.example.gov.uk/submission"
      )

      val json = Json.toJson(status)

      (json \ "formLockOld").as[String] mustBe "LOCK-001"
      (json \ "formLockNew").as[String] mustBe "LOCK-002"
      (json \ "pollInterval").as[String] mustBe "10"
      (json \ "gatewayUrl").as[String] mustBe "https://gateway.example.gov.uk/submission"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "formLockOld"  -> "LOCK-001",
        "formLockNew"  -> "LOCK-002",
        "pollInterval" -> "10",
        "gatewayUrl"   -> "https://gateway.example.gov.uk/submission"
      )

      val result = json.validate[GovTalkStatusLock]

      result mustBe a[JsSuccess[_]]
      val status = result.get

      status.formLockOld mustBe "LOCK-001"
      status.formLockNew mustBe "LOCK-002"
      status.pollInterval mustBe "10"
      status.gatewayUrl mustBe "https://gateway.example.gov.uk/submission"
    }

    "must fail to deserialize when formLockNew is missing" in {
      val json = Json.obj(
        "formLockOld"  -> "LOCK-001",
        "pollInterval" -> "10",
        "gatewayUrl"   -> "https://gateway.example.gov.uk/submission"
      )

      json.validate[GovTalkStatusLock].isError mustBe true
    }
  }

  "GovTalkStatusStatistics" - {

    "must serialize to JSON correctly" in {
      val status = GovTalkStatusStatistics(
        lastMessageTimestamp = "2026-01-15T10:05:00Z",
        numberOfPolls = "5",
        pollInterval = "10",
        gatewayUrl = "https://gateway.example.gov.uk/submission"
      )

      val json = Json.toJson(status)

      (json \ "lastMessageTimestamp").as[String] mustBe "2026-01-15T10:05:00Z"
      (json \ "numberOfPolls").as[String] mustBe "5"
      (json \ "pollInterval").as[String] mustBe "10"
      (json \ "gatewayUrl").as[String] mustBe "https://gateway.example.gov.uk/submission"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
        "numberOfPolls"        -> "5",
        "pollInterval"         -> "10",
        "gatewayUrl"           -> "https://gateway.example.gov.uk/submission"
      )

      val result = json.validate[GovTalkStatusStatistics]

      result mustBe a[JsSuccess[_]]
      val status = result.get

      status.lastMessageTimestamp mustBe "2026-01-15T10:05:00Z"
      status.numberOfPolls mustBe "5"
      status.pollInterval mustBe "10"
      status.gatewayUrl mustBe "https://gateway.example.gov.uk/submission"
    }

    "must fail to deserialize when numberOfPolls is missing" in {
      val json = Json.obj(
        "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
        "pollInterval"         -> "10",
        "gatewayUrl"           -> "https://gateway.example.gov.uk/submission"
      )

      json.validate[GovTalkStatusStatistics].isError mustBe true
    }
  }

  "InsertInitialGovTalkStatusRequest" - {

    "must serialize to JSON correctly with a nested govTalkStatus" in {
      val request = InsertInitialGovTalkStatusRequest(
        userIdentifier = "USER-001",
        formResultId = "FRID-001",
        correlationId = "CORR-001",
        govTalkStatus = GovTalkStatusInitial(
          formLock = "LOCK-001",
          createTimestamp = "2026-01-15T10:00:00Z",
          endStateTimestamp = None,
          lastMessageTimestamp = "2026-01-15T10:05:00Z",
          numberOfPolls = "3",
          pollInterval = "10",
          protocolStatus = "SUBMITTED",
          gatewayUrl = "https://gateway.example.gov.uk/submission"
        )
      )

      val json = Json.toJson(request)

      (json \ "userIdentifier").as[String] mustBe "USER-001"
      (json \ "formResultId").as[String] mustBe "FRID-001"
      (json \ "correlationId").as[String] mustBe "CORR-001"
      (json \ "govTalkStatus" \ "formLock").as[String] mustBe "LOCK-001"
      (json \ "govTalkStatus" \ "protocolStatus").as[String] mustBe "SUBMITTED"
    }

    "must deserialize from JSON correctly with a nested govTalkStatus" in {
      val json = Json.obj(
        "userIdentifier" -> "USER-001",
        "formResultId"   -> "FRID-001",
        "correlationId"  -> "CORR-001",
        "govTalkStatus"  -> Json.obj(
          "formLock"             -> "LOCK-001",
          "createTimestamp"      -> "2026-01-15T10:00:00Z",
          "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
          "numberOfPolls"        -> "3",
          "pollInterval"         -> "10",
          "protocolStatus"       -> "SUBMITTED",
          "gatewayUrl"           -> "https://gateway.example.gov.uk/submission"
        )
      )

      val result = json.validate[InsertInitialGovTalkStatusRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.userIdentifier mustBe "USER-001"
      request.formResultId mustBe "FRID-001"
      request.correlationId mustBe "CORR-001"
      request.govTalkStatus.formLock mustBe "LOCK-001"
      request.govTalkStatus.endStateTimestamp mustBe None
    }

    "must fail to deserialize when govTalkStatus is missing" in {
      val json = Json.obj(
        "userIdentifier" -> "USER-001",
        "formResultId"   -> "FRID-001",
        "correlationId"  -> "CORR-001"
      )

      json.validate[InsertInitialGovTalkStatusRequest].isError mustBe true
    }
  }

  "ResetGovTalkStatusRequest" - {

    "must serialize to JSON correctly with a nested govTalkStatus" in {
      val request = ResetGovTalkStatusRequest(
        userIdentifier = "USER-001",
        formResultId = "FRID-001",
        correlationId = "CORR-001",
        govTalkStatus = GovTalkStatusReset(
          formLock = "LOCK-001",
          createTimestamp = "2026-01-15T10:00:00Z",
          endStateTimestamp = None,
          lastMessageTimestamp = "2026-01-15T10:05:00Z",
          numberOfPolls = "3",
          pollInterval = "10",
          protocolStatusOld = "SUBMITTED",
          protocolStatusNew = "ACKNOWLEDGED",
          gatewayUrl = "https://gateway.example.gov.uk/submission"
        )
      )

      val json = Json.toJson(request)

      (json \ "userIdentifier").as[String] mustBe "USER-001"
      (json \ "formResultId").as[String] mustBe "FRID-001"
      (json \ "correlationId").as[String] mustBe "CORR-001"
      (json \ "govTalkStatus" \ "protocolStatusOld").as[String] mustBe "SUBMITTED"
      (json \ "govTalkStatus" \ "protocolStatusNew").as[String] mustBe "ACKNOWLEDGED"
    }

    "must deserialize from JSON correctly with a nested govTalkStatus" in {
      val json = Json.obj(
        "userIdentifier" -> "USER-001",
        "formResultId"   -> "FRID-001",
        "correlationId"  -> "CORR-001",
        "govTalkStatus"  -> Json.obj(
          "formLock"             -> "LOCK-001",
          "createTimestamp"      -> "2026-01-15T10:00:00Z",
          "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
          "numberOfPolls"        -> "3",
          "pollInterval"         -> "10",
          "protocolStatusOld"    -> "SUBMITTED",
          "protocolStatusNew"    -> "ACKNOWLEDGED",
          "gatewayUrl"           -> "https://gateway.example.gov.uk/submission"
        )
      )

      val result = json.validate[ResetGovTalkStatusRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.userIdentifier mustBe "USER-001"
      request.govTalkStatus.protocolStatusOld mustBe "SUBMITTED"
      request.govTalkStatus.protocolStatusNew mustBe "ACKNOWLEDGED"
    }

    "must fail to deserialize when govTalkStatus is missing" in {
      val json = Json.obj(
        "userIdentifier" -> "USER-001",
        "formResultId"   -> "FRID-001",
        "correlationId"  -> "CORR-001"
      )

      json.validate[ResetGovTalkStatusRequest].isError mustBe true
    }
  }

  "UpdateGovTalkStatusRequest" - {

    "must serialize to JSON correctly" in {
      val request = UpdateGovTalkStatusRequest(
        userIdentifier = "USER-001",
        formResultId = "FRID-001",
        endStateTimestamp = "2026-01-15T10:10:00Z",
        protocolStatus = "ACKNOWLEDGED"
      )

      val json = Json.toJson(request)

      (json \ "userIdentifier").as[String] mustBe "USER-001"
      (json \ "formResultId").as[String] mustBe "FRID-001"
      (json \ "endStateTimestamp").as[String] mustBe "2026-01-15T10:10:00Z"
      (json \ "protocolStatus").as[String] mustBe "ACKNOWLEDGED"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "userIdentifier"    -> "USER-001",
        "formResultId"      -> "FRID-001",
        "endStateTimestamp" -> "2026-01-15T10:10:00Z",
        "protocolStatus"    -> "ACKNOWLEDGED"
      )

      val result = json.validate[UpdateGovTalkStatusRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.userIdentifier mustBe "USER-001"
      request.formResultId mustBe "FRID-001"
      request.endStateTimestamp mustBe "2026-01-15T10:10:00Z"
      request.protocolStatus mustBe "ACKNOWLEDGED"
    }

    "must fail to deserialize when protocolStatus is missing" in {
      val json = Json.obj(
        "userIdentifier"    -> "USER-001",
        "formResultId"      -> "FRID-001",
        "endStateTimestamp" -> "2026-01-15T10:10:00Z"
      )

      json.validate[UpdateGovTalkStatusRequest].isError mustBe true
    }
  }

  "UpdateGovTalkStatusCorrelationIdRequest" - {

    "must serialize to JSON correctly" in {
      val request = UpdateGovTalkStatusCorrelationIdRequest(
        userIdentifier = "USER-001",
        formResultId = "FRID-001",
        correlationId = "CORR-001",
        endStateTimestamp = "2026-01-15T10:10:00Z",
        protocolStatus = "ACKNOWLEDGED"
      )

      val json = Json.toJson(request)

      (json \ "userIdentifier").as[String] mustBe "USER-001"
      (json \ "formResultId").as[String] mustBe "FRID-001"
      (json \ "correlationId").as[String] mustBe "CORR-001"
      (json \ "endStateTimestamp").as[String] mustBe "2026-01-15T10:10:00Z"
      (json \ "protocolStatus").as[String] mustBe "ACKNOWLEDGED"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "userIdentifier"    -> "USER-001",
        "formResultId"      -> "FRID-001",
        "correlationId"     -> "CORR-001",
        "endStateTimestamp" -> "2026-01-15T10:10:00Z",
        "protocolStatus"    -> "ACKNOWLEDGED"
      )

      val result = json.validate[UpdateGovTalkStatusCorrelationIdRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.userIdentifier mustBe "USER-001"
      request.correlationId mustBe "CORR-001"
      request.protocolStatus mustBe "ACKNOWLEDGED"
    }

    "must fail to deserialize when correlationId is missing" in {
      val json = Json.obj(
        "userIdentifier"    -> "USER-001",
        "formResultId"      -> "FRID-001",
        "endStateTimestamp" -> "2026-01-15T10:10:00Z",
        "protocolStatus"    -> "ACKNOWLEDGED"
      )

      json.validate[UpdateGovTalkStatusCorrelationIdRequest].isError mustBe true
    }
  }

  "UpdateGovTalkStatusLockRequest" - {

    "must serialize to JSON correctly with a nested govTalkStatus" in {
      val request = UpdateGovTalkStatusLockRequest(
        userIdentifier = "USER-001",
        formResultId = "FRID-001",
        govTalkStatus = GovTalkStatusLock(
          formLockOld = "LOCK-001",
          formLockNew = "LOCK-002",
          pollInterval = "10",
          gatewayUrl = "https://gateway.example.gov.uk/submission"
        )
      )

      val json = Json.toJson(request)

      (json \ "userIdentifier").as[String] mustBe "USER-001"
      (json \ "formResultId").as[String] mustBe "FRID-001"
      (json \ "govTalkStatus" \ "formLockOld").as[String] mustBe "LOCK-001"
      (json \ "govTalkStatus" \ "formLockNew").as[String] mustBe "LOCK-002"
    }

    "must deserialize from JSON correctly with a nested govTalkStatus" in {
      val json = Json.obj(
        "userIdentifier" -> "USER-001",
        "formResultId"   -> "FRID-001",
        "govTalkStatus"  -> Json.obj(
          "formLockOld"  -> "LOCK-001",
          "formLockNew"  -> "LOCK-002",
          "pollInterval" -> "10",
          "gatewayUrl"   -> "https://gateway.example.gov.uk/submission"
        )
      )

      val result = json.validate[UpdateGovTalkStatusLockRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.userIdentifier mustBe "USER-001"
      request.govTalkStatus.formLockOld mustBe "LOCK-001"
      request.govTalkStatus.formLockNew mustBe "LOCK-002"
    }

    "must fail to deserialize when govTalkStatus is missing" in {
      val json = Json.obj(
        "userIdentifier" -> "USER-001",
        "formResultId"   -> "FRID-001"
      )

      json.validate[UpdateGovTalkStatusLockRequest].isError mustBe true
    }
  }

  "UpdateGovTalkStatisticsRequest" - {

    "must serialize to JSON correctly with a nested govTalkStatus" in {
      val request = UpdateGovTalkStatisticsRequest(
        userIdentifier = "USER-001",
        formResultId = "FRID-001",
        govTalkStatus = GovTalkStatusStatistics(
          lastMessageTimestamp = "2026-01-15T10:05:00Z",
          numberOfPolls = "5",
          pollInterval = "10",
          gatewayUrl = "https://gateway.example.gov.uk/submission"
        )
      )

      val json = Json.toJson(request)

      (json \ "userIdentifier").as[String] mustBe "USER-001"
      (json \ "formResultId").as[String] mustBe "FRID-001"
      (json \ "govTalkStatus" \ "numberOfPolls").as[String] mustBe "5"
      (json \ "govTalkStatus" \ "pollInterval").as[String] mustBe "10"
    }

    "must deserialize from JSON correctly with a nested govTalkStatus" in {
      val json = Json.obj(
        "userIdentifier" -> "USER-001",
        "formResultId"   -> "FRID-001",
        "govTalkStatus"  -> Json.obj(
          "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
          "numberOfPolls"        -> "5",
          "pollInterval"         -> "10",
          "gatewayUrl"           -> "https://gateway.example.gov.uk/submission"
        )
      )

      val result = json.validate[UpdateGovTalkStatisticsRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.userIdentifier mustBe "USER-001"
      request.govTalkStatus.numberOfPolls mustBe "5"
      request.govTalkStatus.gatewayUrl mustBe "https://gateway.example.gov.uk/submission"
    }

    "must fail to deserialize when govTalkStatus is missing" in {
      val json = Json.obj(
        "userIdentifier" -> "USER-001",
        "formResultId"   -> "FRID-001"
      )

      json.validate[UpdateGovTalkStatisticsRequest].isError mustBe true
    }
  }

  "DeleteGovTalkStatusRequest" - {

    "must serialize to JSON correctly" in {
      val json = Json.toJson(DeleteGovTalkStatusRequest(resultId = "FRID-001"))

      (json \ "resultId").as[String] mustBe "FRID-001"
    }

    "must deserialize from JSON correctly" in {
      val result = Json.obj("resultId" -> "FRID-001").validate[DeleteGovTalkStatusRequest]

      result mustBe a[JsSuccess[_]]
      result.get.resultId mustBe "FRID-001"
    }

    "must fail to deserialize when resultId is missing" in {
      Json.obj().validate[DeleteGovTalkStatusRequest].isError mustBe true
    }
  }

  "SelectGovTalkStatusRequest" - {

    "must serialize to JSON correctly" in {
      val json = Json.toJson(
        SelectGovTalkStatusRequest(userIdentifier = "USER-001", formResultId = "FRID-001")
      )

      (json \ "userIdentifier").as[String] mustBe "USER-001"
      (json \ "formResultId").as[String] mustBe "FRID-001"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "userIdentifier" -> "USER-001",
        "formResultId"   -> "FRID-001"
      )

      val result = json.validate[SelectGovTalkStatusRequest]

      result mustBe a[JsSuccess[_]]
      result.get.userIdentifier mustBe "USER-001"
      result.get.formResultId mustBe "FRID-001"
    }

    "must fail to deserialize when userIdentifier is missing" in {
      Json.obj("formResultId" -> "FRID-001").validate[SelectGovTalkStatusRequest].isError mustBe true
    }

    "must fail to deserialize when formResultId is missing" in {
      Json.obj("userIdentifier" -> "USER-001").validate[SelectGovTalkStatusRequest].isError mustBe true
    }
  }

  "SelectGovTalkFormResultIdRequest" - {

    "must serialize to JSON correctly" in {
      val json = Json.toJson(SelectGovTalkFormResultIdRequest(userIdentifier = "USER-001"))

      (json \ "userIdentifier").as[String] mustBe "USER-001"
    }

    "must deserialize from JSON correctly" in {
      val result = Json.obj("userIdentifier" -> "USER-001").validate[SelectGovTalkFormResultIdRequest]

      result mustBe a[JsSuccess[_]]
      result.get.userIdentifier mustBe "USER-001"
    }

    "must fail to deserialize when userIdentifier is missing" in {
      Json.obj().validate[SelectGovTalkFormResultIdRequest].isError mustBe true
    }
  }

  "SelectGovTalkStatusResponse" - {

    "must serialize to JSON correctly with all fields populated" in {
      val response = SelectGovTalkStatusResponse(
        userIdentifier = Some("USER-001"),
        formResultId = Some("FRID-001"),
        correlationId = Some("CORR-001"),
        formLock = Some("LOCK-001"),
        createTimestamp = Some("2026-01-15T10:00:00Z"),
        endStateTimestamp = Some("2026-01-15T10:10:00Z"),
        lastMessageTimestamp = Some("2026-01-15T10:05:00Z"),
        numberOfPolls = Some("3"),
        pollInterval = Some("10"),
        protocolStatus = Some("SUBMITTED"),
        gatewayUrl = Some("https://gateway.example.gov.uk/submission")
      )

      val json = Json.toJson(response)

      (json \ "userIdentifier").as[String] mustBe "USER-001"
      (json \ "formResultId").as[String] mustBe "FRID-001"
      (json \ "correlationId").as[String] mustBe "CORR-001"
      (json \ "formLock").as[String] mustBe "LOCK-001"
      (json \ "createTimestamp").as[String] mustBe "2026-01-15T10:00:00Z"
      (json \ "endStateTimestamp").as[String] mustBe "2026-01-15T10:10:00Z"
      (json \ "lastMessageTimestamp").as[String] mustBe "2026-01-15T10:05:00Z"
      (json \ "numberOfPolls").as[String] mustBe "3"
      (json \ "pollInterval").as[String] mustBe "10"
      (json \ "protocolStatus").as[String] mustBe "SUBMITTED"
      (json \ "gatewayUrl").as[String] mustBe "https://gateway.example.gov.uk/submission"
    }

    "must serialize to JSON correctly when all fields are None" in {
      val response = SelectGovTalkStatusResponse(
        userIdentifier = None,
        formResultId = None,
        correlationId = None,
        formLock = None,
        createTimestamp = None,
        endStateTimestamp = None,
        lastMessageTimestamp = None,
        numberOfPolls = None,
        pollInterval = None,
        protocolStatus = None,
        gatewayUrl = None
      )

      val json = Json.toJson(response)

      (json \ "userIdentifier").toOption mustBe None
      (json \ "formResultId").toOption mustBe None
      (json \ "gatewayUrl").toOption mustBe None
    }

    "must deserialize from JSON correctly with all fields populated" in {
      val json = Json.obj(
        "userIdentifier"       -> "USER-001",
        "formResultId"         -> "FRID-001",
        "correlationId"        -> "CORR-001",
        "formLock"             -> "LOCK-001",
        "createTimestamp"      -> "2026-01-15T10:00:00Z",
        "endStateTimestamp"    -> "2026-01-15T10:10:00Z",
        "lastMessageTimestamp" -> "2026-01-15T10:05:00Z",
        "numberOfPolls"        -> "3",
        "pollInterval"         -> "10",
        "protocolStatus"       -> "SUBMITTED",
        "gatewayUrl"           -> "https://gateway.example.gov.uk/submission"
      )

      val result = json.validate[SelectGovTalkStatusResponse]

      result mustBe a[JsSuccess[_]]
      val response = result.get

      response.userIdentifier mustBe Some("USER-001")
      response.formResultId mustBe Some("FRID-001")
      response.correlationId mustBe Some("CORR-001")
      response.protocolStatus mustBe Some("SUBMITTED")
      response.gatewayUrl mustBe Some("https://gateway.example.gov.uk/submission")
    }

    "must deserialize from an empty JSON object with all fields as None" in {
      val result = Json.obj().validate[SelectGovTalkStatusResponse]

      result mustBe a[JsSuccess[_]]
      val response = result.get

      response.userIdentifier mustBe None
      response.formResultId mustBe None
      response.protocolStatus mustBe None
      response.gatewayUrl mustBe None
    }
  }

  "SelectGovTalkFormResultIdResponse" - {

    "must serialize to JSON correctly when formResultId is present" in {
      val json = Json.toJson(SelectGovTalkFormResultIdResponse(formResultId = Some("FRID-001")))

      (json \ "formResultId").as[String] mustBe "FRID-001"
    }

    "must serialize to JSON correctly when formResultId is None" in {
      val json = Json.toJson(SelectGovTalkFormResultIdResponse(formResultId = None))

      (json \ "formResultId").toOption mustBe None
    }

    "must deserialize from JSON correctly when formResultId is present" in {
      val result = Json.obj("formResultId" -> "FRID-001").validate[SelectGovTalkFormResultIdResponse]

      result mustBe a[JsSuccess[_]]
      result.get.formResultId mustBe Some("FRID-001")
    }

    "must deserialize from an empty JSON object with formResultId as None" in {
      val result = Json.obj().validate[SelectGovTalkFormResultIdResponse]

      result mustBe a[JsSuccess[_]]
      result.get.formResultId mustBe None
    }
  }

  "LockReturnRequest" - {

    "must serialize to JSON correctly" in {
      val request = LockReturnRequest(
        storn = "STORN12345",
        returnResourceRef = "100001",
        version = 3
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
      (json \ "returnResourceRef").as[String] mustBe "100001"
      (json \ "version").as[Int] mustBe 3
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001",
        "version"           -> 3
      )

      val result = json.validate[LockReturnRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
      request.returnResourceRef mustBe "100001"
      request.version mustBe 3
    }

    "must fail to deserialize when version is missing" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001"
      )

      json.validate[LockReturnRequest].isError mustBe true
    }

    "must fail to deserialize when version is not an integer" in {
      val json = Json.obj(
        "storn"             -> "STORN12345",
        "returnResourceRef" -> "100001",
        "version"           -> "three"
      )

      json.validate[LockReturnRequest].isError mustBe true
    }
  }

  "LockReturnResponse" - {

    "must serialize to JSON correctly when success is true" in {
      (Json.toJson(LockReturnResponse(success = true)) \ "success").as[Boolean] mustBe true
    }

    "must serialize to JSON correctly when success is false" in {
      (Json.toJson(LockReturnResponse(success = false)) \ "success").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly when success is true" in {
      val result = Json.obj("success" -> true).validate[LockReturnResponse]
      result mustBe a[JsSuccess[_]]
      result.get.success mustBe true
    }

    "must deserialize from JSON correctly when success is false" in {
      val result = Json.obj("success" -> false).validate[LockReturnResponse]
      result mustBe a[JsSuccess[_]]
      result.get.success mustBe false
    }

    "must fail to deserialize when success is missing" in {
      Json.obj().validate[LockReturnResponse].isError mustBe true
    }
  }
}
