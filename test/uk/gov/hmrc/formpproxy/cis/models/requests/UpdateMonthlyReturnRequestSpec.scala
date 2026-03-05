package uk.gov.hmrc.formpproxy.cis.models.requests

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}

class UpdateMonthlyReturnRequestSpec extends AnyWordSpec with Matchers {

  "UpdateMonthlyReturnRequest.format" should {

    "serialize and deserialize (round-trip) model" in {
      val model = UpdateMonthlyReturnRequest(
        instanceId = "instance-123",
        taxYear = 2024,
        taxMonth = 1,
        amendment = "N",
        nilReturnIndicator = "Y",
        status = "STARTED"
      )

      val json = Json.toJson(model)
      json.validate[UpdateMonthlyReturnRequest] shouldBe JsSuccess(model)
    }
  }
}
