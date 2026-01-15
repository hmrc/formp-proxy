package uk.gov.hmrc.formpproxy.cis.models.requests

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class CreateMonthlyReturnRequestSpec extends AnyFreeSpec with Matchers {

  "CreateMonthlyReturnRequest JSON format" - {

    "read from JSON correctly" in {
      val json = Json.obj(
        "instanceId" -> "abc-123",
        "taxYear"    -> 2025,
        "taxMonth"   -> 2
      )

      val result = json.as[CreateMonthlyReturnRequest]

      result mustBe CreateMonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear    = 2025,
        taxMonth   = 2
      )
    }

    "write to JSON correctly" in {
      val model = CreateMonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear    = 2025,
        taxMonth   = 2
      )

      val json = Json.toJson(model)

      json mustBe Json.obj(
        "instanceId" -> "abc-123",
        "taxYear"    -> 2025,
        "taxMonth"   -> 2
      )
    }
  }
}
