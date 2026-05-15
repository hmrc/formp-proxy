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

import uk.gov.hmrc.formpproxy.cis.models.GovTalkErrorStatus.*
import uk.gov.hmrc.formpproxy.cis.models.{GovTalkErrorStatus, GovTalkErrorValues}

object GovTalkErrorMapper {

  private val SystemErrorType       = "systemError"
  private val DepartmentalErrorType = "departmentalError"
  private val TimeOutType           = "timeOut"
  private val DepartmentalErrorCode = "3001"
  private val NoResponseErrorCode   = "xxxx"
  private val NoResponseMessage     = "timed out"
  private val ServerErrorRange      = 500 to 505

  def apply(status: GovTalkErrorStatus): GovTalkErrorValues = status match {
    case RecoverableError(code, text) =>
      GovTalkErrorValues(Some(code), Some(SystemErrorType), Some(text))

    case FatalError(code, text) =>
      GovTalkErrorValues(Some(code), Some(SystemErrorType), Some(text))

    case DepartmentalError(text) =>
      GovTalkErrorValues(Some(DepartmentalErrorCode), Some(DepartmentalErrorType), Some(text))

    case ServerError(httpStatus) if ServerErrorRange.contains(httpStatus) =>
      GovTalkErrorValues(Some(httpStatus.toString), Some(TimeOutType), Some(TimeOutType))

    case NoResponse =>
      GovTalkErrorValues(Some(NoResponseErrorCode), Some(TimeOutType), Some(NoResponseMessage))

    case _ =>
      GovTalkErrorValues.empty
  }
}
