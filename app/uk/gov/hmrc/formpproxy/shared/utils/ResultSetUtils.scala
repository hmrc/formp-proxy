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

import java.sql.{ResultSet, Timestamp}

object ResultSetUtils {

  extension (resultSet: ResultSet) {

    def getOptionalInt(columnName: String): Option[Int] = {
      val intValue = resultSet.getInt(columnName)
      if (resultSet.wasNull()) None else Some(intValue)
    }

    def getOptionalLong(columnName: String): Option[Long] = {
      val longValue = resultSet.getLong(columnName)
      if (resultSet.wasNull()) None else Some(longValue)
    }

    def getOptionalString(columnName: String): Option[String] =
      Option(resultSet.getString(columnName))

    def getOptionalTimestamp(columnName: String): Option[Timestamp] =
      Option(resultSet.getTimestamp(columnName))

    def getOptionalLocalDateTime(columnName: String): Option[java.time.LocalDateTime] =
      getOptionalTimestamp(columnName).map(_.toLocalDateTime)

    def getOptionalInstant(columnName: String): Option[java.time.Instant] =
      getOptionalTimestamp(columnName).map(_.toInstant)
  }
}
