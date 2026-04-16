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

package uk.gov.hmrc.formpproxy.nova.repositories

import uk.gov.hmrc.formpproxy.nova.models.{FormDetail, FormSummary}
import uk.gov.hmrc.formpproxy.shared.utils.ResultSetUtils.*

import java.sql.ResultSet

object NovaRowMappers {

  def readFormSummary(rs: ResultSet): FormSummary =
    FormSummary(
      formId = rs.getLong("form_id"),
      userId = rs.getString("user_id"),
      formType = rs.getString("form_type"),
      versionId = rs.getLong("version_id"),
      creationTimestamp = rs.getTimestamp("creation_timestamp").toInstant.toString,
      formStatus = rs.getString("form_status")
    )

  def readFormDetail(rs: ResultSet): FormDetail =
    FormDetail(
      formId = rs.getLong("form_id"),
      userId = rs.getString("user_id"),
      formType = rs.getString("form_type"),
      versionId = rs.getLong("version_id"),
      creationTimestamp = rs.getTimestamp("creation_timestamp").toInstant.toString,
      formStatus = rs.getString("form_status"),
      submissionDatetime = rs.getOptionalTimestamp("submission_datetime").map(_.toInstant.toString),
      submissionReference = rs.getOptionalString("submission_reference")
    )
}
