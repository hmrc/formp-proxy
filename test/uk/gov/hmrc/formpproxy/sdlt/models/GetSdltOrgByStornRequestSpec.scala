package uk.gov.hmrc.formpproxy.sdlt.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.formpproxy.sdlt.models.organisation.GetSdltOrgByStornRequest

class GetSdltOrgByStornRequestSpec extends AnyFreeSpec with Matchers {

  "GetSdltOrgByStornRequest" - {

    "must serialize to JSON correctly" in {
      val request = GetSdltOrgByStornRequest(
        storn = "STORN12345"
      )

      val json = Json.toJson(request)

      (json \ "storn").as[String] mustBe "STORN12345"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "storn" -> "STORN12345"
      )

      val result = json.validate[GetSdltOrgByStornRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.storn mustBe "STORN12345"
    }

    "must fail to deserialize when storn is missing" in {
      val json = Json.obj()

      val result = json.validate[GetSdltOrgByStornRequest]

      result.isError mustBe true
    }
  }
}
