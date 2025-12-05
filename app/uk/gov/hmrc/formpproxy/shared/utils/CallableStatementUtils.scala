/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.formpproxy.shared.utils

import java.sql.{CallableStatement, Types}

object CallableStatementUtils {

  extension (cs: CallableStatement) {
    def setOptionalString(index: Int, value: Option[String]): Unit =
      value match {
        case Some(v) if v != null => cs.setString(index, v)
        case _                    => cs.setNull(index, Types.VARCHAR)
      }

    def setOptionalInt(index: Int, value: Option[Int]): Unit =
      value match {
        case Some(v) => cs.setInt(index, v)
        case None    => cs.setNull(index, Types.NUMERIC)
      }
  }

}
