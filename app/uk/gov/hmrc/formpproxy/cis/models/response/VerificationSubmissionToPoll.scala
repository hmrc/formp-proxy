package uk.gov.hmrc.formpproxy.cis.models.response

import play.api.libs.json.{Json, OFormat}

case class VerificationSubmissionToPoll(
  submissionId: Long,
  submissionType: String,
  agentId: Option[String],
  taxOfficeNumber: String,
  taxOfficeReference: String,
  instanceId: String,
  status: String,
  verificationBatchResourceRef: Long
)

object VerificationSubmissionToPoll {
  given format: OFormat[VerificationSubmissionToPoll] =
    Json.format[VerificationSubmissionToPoll]
}
