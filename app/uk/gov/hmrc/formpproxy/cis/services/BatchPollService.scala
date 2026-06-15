package uk.gov.hmrc.formpproxy.cis.services

import uk.gov.hmrc.formpproxy.cis.models.response.GetBatchPollSubmissionsResponse
import uk.gov.hmrc.formpproxy.cis.repositories.CisMonthlyReturnSource

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class BatchPollService @Inject() (
  repo: CisMonthlyReturnSource
) {

  def getBatchPollSubmissions(): Future[GetBatchPollSubmissionsResponse] =
    repo.getBatchPollSubmissions()
}
