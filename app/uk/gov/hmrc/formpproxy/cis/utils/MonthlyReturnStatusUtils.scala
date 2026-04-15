/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
