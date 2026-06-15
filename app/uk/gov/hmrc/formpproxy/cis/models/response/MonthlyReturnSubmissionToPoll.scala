package uk.gov.hmrc.formpproxy.cis.models.response

import play.api.libs.json.{Json, OFormat}

case class MonthlyReturnSubmissionToPoll(
  submissionId: Long,
  submissionType: String,
  status: String,
  taxOfficeNumber: String,
  taxOfficeReference: String,
  taxYear: String,
  taxMonth: String,
  instanceId: String,
  agentId: Option[String]
)

object MonthlyReturnSubmissionToPoll {
  given format: OFormat[MonthlyReturnSubmissionToPoll] =
    Json.format[MonthlyReturnSubmissionToPoll]
}
