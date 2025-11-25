package uk.gov.hmrc.formpproxy.sdlt.services.agent

import uk.gov.hmrc.formpproxy.sdlt.models.agent.{DeleteAgentRequest, DeleteAgentReturn}
import uk.gov.hmrc.formpproxy.sdlt.repositories.SdltFormpRepository

import javax.inject.Inject
import scala.concurrent.Future

class AgentService @Inject() (repo: SdltFormpRepository) {

  def deleteAgent(req: DeleteAgentRequest): Future[DeleteAgentReturn] =
    repo.sdltDeleteAgent(req)
}
