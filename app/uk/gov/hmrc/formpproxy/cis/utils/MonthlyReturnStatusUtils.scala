package uk.gov.hmrc.formpproxy.cis.utils

object MonthlyReturnStatusUtils {
  def mapStatus(raw: Option[String]): String =
    raw.map(_.trim.toUpperCase) match {
      case Some("STARTED")            => "STARTED"
      case Some("VALIDATED")          => "VALIDATED"
      case Some("PENDING")            => "PENDING"
      case Some("ACCEPTED")           => "PENDING"
      case Some("DEPARTMENTAL_ERROR") => "REJECTED"
      case Some("FATAL_ERROR")        => "REJECTED"
      case _                          => "STARTED"
    }
}
