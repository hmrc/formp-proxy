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

package uk.gov.hmrc.formpproxy.shared.utils

import java.sql.PreparedStatement

object StatementUtils {

  extension (ps: PreparedStatement) {

    def setOptionalString(index: Int, value: Option[String]): Unit =
      value match {
        case Some(v) => ps.setString(index, v)
        case None    => ps.setNull(index, java.sql.Types.VARCHAR)
      }

    def setOptionalInt(index: Int, value: Option[Int]): Unit =
      value match {
        case Some(v) => ps.setInt(index, v)
        case None    => ps.setNull(index, java.sql.Types.INTEGER)
      }

    def setOptionalLong(index: Int, value: Option[Long]): Unit =
      value match {
        case Some(v) => ps.setLong(index, v)
        case None    => ps.setNull(index, java.sql.Types.BIGINT)
      }

    def setOptionalTimestamp(index: Int, value: Option[java.time.LocalDateTime]): Unit =
      value match {
        case Some(v) => ps.setTimestamp(index, java.sql.Timestamp.valueOf(v))
        case None    => ps.setNull(index, java.sql.Types.TIMESTAMP)
      }
  }
}
