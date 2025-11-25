package uk.gov.hmrc.formpproxy.sdlt.models.agent

import play.api.libs.json.{Json, OFormat}

case class DeleteAgentRequest (
  storn: String,
  agentReferenceNumber: String                            
)

object DeleteAgentRequest {
  implicit val format: OFormat[DeleteAgentRequest] = Json.format[DeleteAgentRequest]
}

case class DeleteAgentReturn (
  deleted: Boolean
)

object DeleteAgentReturn {
  implicit val format: OFormat[DeleteAgentReturn] = Json.format[DeleteAgentReturn]
}
