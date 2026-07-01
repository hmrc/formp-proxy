package uk.gov.hmrc.formpproxy.cis.models.responses

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.formpproxy.cis.models.response.GetSubcontractorForDeleteResponse

class GetSubcontractorForDeleteResponseSpec extends AnyFreeSpec with Matchers {

  "GetSubcontractorForDeleteResponse" - {

    "must serialise to JSON correctly" in {
      val model = GetSubcontractorForDeleteResponse(
        subcontractorCanBeDeleted = true
      )

      val result = Json.toJson(model)

      result mustBe Json.obj(
        "subcontractorCanBeDeleted" -> true
      )
    }

    "must deserialise from JSON correctly" in {
      val json = Json.obj(
        "subcontractorCanBeDeleted" -> false
      )

      val result = json.as[GetSubcontractorForDeleteResponse]

      result mustBe GetSubcontractorForDeleteResponse(
        subcontractorCanBeDeleted = false
      )
    }

    "must handle round-trip conversion correctly" in {
      val model = GetSubcontractorForDeleteResponse(
        subcontractorCanBeDeleted = true
      )

      val json   = Json.toJson(model)
      val parsed = json.as[GetSubcontractorForDeleteResponse]

      parsed mustBe model
    }

    "must fail to deserialise when field is missing" in {
      val json = Json.obj()

      val result = json.validate[GetSubcontractorForDeleteResponse]

      result.isError mustBe true
    }

    "must fail to deserialise when field type is incorrect" in {
      val json = Json.obj(
        "subcontractorCanBeDeleted" -> "not-a-boolean"
      )

      val result = json.validate[GetSubcontractorForDeleteResponse]

      result.isError mustBe true
    }
  }
}
