package uk.gov.hmrc.formpproxy.cis.models.requests

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.*

class SyncMonthlyReturnItemsRequestSpec extends AnyWordSpec with Matchers {

  "SyncMonthlyReturnItemsRequest JSON format" should {
    "serialize and deserialize" in {
      val model = SyncMonthlyReturnItemsRequest(
        instanceId = "instance-1",
        taxYear = 2026,
        taxMonth = 2,
        amendment = "amendment-1",
        createResourceReferences = Seq(10L, 20L),
        deleteResourceReferences = Seq(30L)
      )

      val json = Json.toJson(model)
      json.validate[SyncMonthlyReturnItemsRequest].asOpt shouldBe Some(model)
    }
  }
}
