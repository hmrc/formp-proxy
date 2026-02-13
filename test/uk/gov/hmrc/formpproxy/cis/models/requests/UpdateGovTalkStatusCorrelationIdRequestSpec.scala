package uk.gov.hmrc.formpproxy.cis.models.requests

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class UpdateGovTalkStatusCorrelationIdRequestSpec extends AnyFreeSpec with Matchers {

  "UpdateGovTalkStatusCorrelationIdRequest JSON format" - {

    "reads and writes" in {
      val json = Json.obj(
        "userIdentifier" -> "1",
        "formResultId"   -> "12890",
        "correlationId"  -> "C742D5DEE7EB4D15B4F7EFD50B890525",
        "pollInterval"   -> 1,
        "gatewayUrl"     -> "http://example.com"
      )

      val model = UpdateGovTalkStatusCorrelationIdRequest(
        userIdentifier = "1",
        formResultId   = "12890",
        correlationId  = "C742D5DEE7EB4D15B4F7EFD50B890525",
        pollInterval   = 1,
        gatewayUrl     = "http://example.com"
      )

      json.as[UpdateGovTalkStatusCorrelationIdRequest] mustBe model
      Json.toJson(model) mustBe json
    }
  }
}