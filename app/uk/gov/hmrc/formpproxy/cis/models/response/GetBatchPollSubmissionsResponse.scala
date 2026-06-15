package uk.gov.hmrc.formpproxy.cis.models.response

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.formpproxy.cis.models.Submission

case class GetBatchPollSubmissionsResponse(
  verificationSubmissions: Seq[Submission],
  monthlyReturnSubmissions: Seq[Submission]
)

object GetBatchPollSubmissionsResponse {
  given format: OFormat[GetBatchPollSubmissionsResponse] =
    Json.format[GetBatchPollSubmissionsResponse]
}
