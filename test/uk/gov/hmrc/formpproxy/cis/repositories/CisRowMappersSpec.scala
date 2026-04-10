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

package uk.gov.hmrc.formpproxy.cis.repositories

import org.mockito.Mockito.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.sql.{ResultSet, Timestamp}
import java.time.LocalDateTime

class CisRowMappersSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  "CisRowMappers collectors" - {

    "return Nil when ResultSet is null" in {
      CisRowMappers.collectMonthlyReturns(null) mustBe Nil
      CisRowMappers.collectSchemes(null) mustBe Nil
      CisRowMappers.collectMonthlyReturnItems(null) mustBe Nil
      CisRowMappers.collectSubmissions(null) mustBe Nil
      CisRowMappers.collectSubcontractors(null) mustBe Nil
      CisRowMappers.collectGovtTalkStatusRecords(null) mustBe Nil
    }

    "return Nil when rs.next() is false" in {
      val rs = mock[ResultSet]
      when(rs.next()).thenReturn(false)

      CisRowMappers.collectMonthlyReturns(rs) mustBe Nil
      CisRowMappers.collectSchemes(rs) mustBe Nil
      CisRowMappers.collectMonthlyReturnItems(rs) mustBe Nil
      CisRowMappers.collectSubmissions(rs) mustBe Nil
      CisRowMappers.collectSubcontractors(rs) mustBe Nil
      CisRowMappers.collectGovtTalkStatusRecords(rs) mustBe Nil

      verify(rs, times(6)).next()
      verifyNoMoreInteractions(rs)
    }

    "collectMonthlyReturns reads rows until rs.next() is false" in {
      val rs = mock[ResultSet]

      when(rs.next()).thenReturn(true, true, false)

      when(rs.getLong("monthly_return_id")).thenReturn(1L, 2L)
      when(rs.getInt("tax_year")).thenReturn(2025, 2025)
      when(rs.getInt("tax_month")).thenReturn(1, 2)

      when(rs.getString("nil_return_indicator")).thenReturn(null)
      when(rs.getString("dec_emp_status_considered")).thenReturn(null)
      when(rs.getString("dec_all_subs_verified")).thenReturn(null)
      when(rs.getString("dec_information_correct")).thenReturn(null)
      when(rs.getString("dec_no_more_sub_payments")).thenReturn(null)
      when(rs.getString("dec_nil_return_no_payments")).thenReturn(null)
      when(rs.getString("status")).thenReturn(null)
      when(rs.getString("amendment")).thenReturn(null)

      when(rs.getTimestamp("last_update")).thenReturn(null)
      when(rs.getLong("superseded_by")).thenReturn(0L)
      when(rs.wasNull()).thenReturn(true)

      val out = CisRowMappers.collectMonthlyReturns(rs)

      out.map(_.monthlyReturnId) mustBe Seq(1L, 2L)
      out.map(_.taxMonth) mustBe Seq(1, 2)
      out.foreach { r =>
        r.taxYear mustBe 2025
        r.lastUpdate mustBe None
        r.supersededBy mustBe None
      }

      verify(rs, times(3)).next()
      verify(rs, times(2)).getLong("monthly_return_id")
      verify(rs, times(2)).getInt("tax_year")
      verify(rs, times(2)).getInt("tax_month")
    }

    "collectGovtTalkStatusRecords reads rows until rs.next() is false" in {
      val rs = mock[ResultSet]

      when(rs.next()).thenReturn(true, true, false)

      when(rs.getString("user_identifier")).thenReturn("1", "1")
      when(rs.getString("formResultId")).thenReturn("AB25", "AB25")
      when(rs.getString("correlationid")).thenReturn("ABC1", "ABC2")
      when(rs.getString("form_lock")).thenReturn("N", "N")
      when(rs.getTimestamp("create_timestamp")).thenReturn(null)
      when(rs.getTimestamp("endstate_timestamp")).thenReturn(null)
      val lastMsgTime = LocalDateTime.of(2024, 1, 1, 10, 0)
      when(rs.getTimestamp("last_mesg_timestamp"))
        .thenReturn(Timestamp.valueOf(lastMsgTime))
      when(rs.getInt("num_polls")).thenReturn(0, 1)
      when(rs.getInt("poll_interval")).thenReturn(0, 1)
      when(rs.getString("gatewayurl")).thenReturn("www.url1.com", "www.url2.com")

      val out = CisRowMappers.collectGovtTalkStatusRecords(rs)

      out.map(_.userIdentifier) mustBe Seq("1", "1")
      out.map(_.formResultID) mustBe Seq("AB25", "AB25")
      out.map(_.correlationID) mustBe Seq("ABC1", "ABC2")
      out.map(_.formLock) mustBe Seq("N", "N")
      out.map(_.formLock) mustBe Seq("N", "N")
      out.map(_.createDate) mustBe Seq(None, None)
      out.map(_.endStateDate) mustBe Seq(None, None)
      out.map(_.lastMessageDate) mustBe Seq(lastMsgTime, lastMsgTime)
      out.map(_.numPolls) mustBe Seq(0, 1)
      out.map(_.pollInterval) mustBe Seq(0, 1)
      out.map(_.gatewayURL) mustBe Seq("www.url1.com", "www.url2.com")

      verify(rs, times(3)).next()
    }

    "collectVerificationBatches reads rows until rs.next() is false" in {
      val rs = mock[ResultSet]

      when(rs.next()).thenReturn(true, true, false)

      when(rs.getLong("verification_batch_id")).thenReturn(10L, 11L)
      when(rs.getLong("scheme_id")).thenReturn(999L, 999L)

      when(rs.getLong("verifications_counter")).thenReturn(0L, 2L)
      when(rs.getLong("verif_batch_resource_ref")).thenReturn(0L, 123L)

      when(rs.wasNull()).thenReturn(
        true,
        true,
        false,
        false
      )

      when(rs.getString("proceed_session")).thenReturn(null, "session-1")
      when(rs.getString("confirm_arrangement")).thenReturn(null, "Y")
      when(rs.getString("confirm_correct")).thenReturn(null, "Y")
      when(rs.getString("status")).thenReturn("STARTED", "SUBMITTED")
      when(rs.getString("verification_number")).thenReturn(null, "VB123")

      val create1 = LocalDateTime.of(2026, 4, 1, 10, 0)
      val update1 = LocalDateTime.of(2026, 4, 2, 11, 0)
      val create2 = LocalDateTime.of(2026, 4, 3, 12, 0)
      val update2 = LocalDateTime.of(2026, 4, 4, 13, 0)

      when(rs.getTimestamp("create_date"))
        .thenReturn(Timestamp.valueOf(create1), Timestamp.valueOf(create2))
      when(rs.getTimestamp("last_update"))
        .thenReturn(Timestamp.valueOf(update1), Timestamp.valueOf(update2))

      when(rs.getInt("version")).thenReturn(1, 2)

      val out = CisRowMappers.collectVerificationBatches(rs)

      out.map(_.verificationBatchId) mustBe Seq(10L, 11L)
      out.map(_.schemeId) mustBe Seq(999L, 999L)

      out.head.verificationsCounter mustBe None
      out.head.verifBatchResourceRef mustBe None
      out.head.proceedSession mustBe None
      out.head.confirmArrangement mustBe None
      out.head.confirmCorrect mustBe None
      out.head.status mustBe Some("STARTED")
      out.head.verificationNumber mustBe None
      out.head.createDate mustBe Some(create1)
      out.head.lastUpdate mustBe Some(update1)

      out(1).verificationsCounter mustBe Some(2L)
      out(1).verifBatchResourceRef mustBe Some(123L)
      out(1).proceedSession mustBe Some("session-1")
      out(1).confirmArrangement mustBe Some("Y")
      out(1).confirmCorrect mustBe Some("Y")
      out(1).status mustBe Some("SUBMITTED")
      out(1).verificationNumber mustBe Some("VB123")
      out(1).createDate mustBe Some(create2)
      out(1).lastUpdate mustBe Some(update2)

      verify(rs, times(3)).next()
      verify(rs, times(2)).getLong("verification_batch_id")
      verify(rs, times(2)).getLong("scheme_id")
    }

    "collectVerifications reads rows until rs.next() is false" in {
      val rs = mock[ResultSet]

      when(rs.next()).thenReturn(true, true, false)

      when(rs.getLong("verification_id")).thenReturn(1L, 2L)

      when(rs.getString("matched")).thenReturn("Y", null)
      when(rs.getString("verification_number")).thenReturn("V123", "V456")
      when(rs.getString("tax_treatment")).thenReturn("NET", "GROSS")
      when(rs.getString("action_indicator")).thenReturn("A", null)

      when(rs.getLong("verification_batch_id")).thenReturn(0L, 77L)
      when(rs.getLong("scheme_id")).thenReturn(0L, 999L)
      when(rs.getLong("subcontractor_id")).thenReturn(0L, 555L)
      when(rs.getLong("verification_resource_ref")).thenReturn(0L, 888L)

      when(rs.wasNull()).thenReturn(
        true, true, true, true, false, false, false, false
      )

      when(rs.getString("subcontractor_name")).thenReturn("ABC LTD", "XYZ LTD")
      when(rs.getString("proceed")).thenReturn("Y", "N")

      val create1 = LocalDateTime.of(2026, 4, 1, 10, 0)
      val update1 = LocalDateTime.of(2026, 4, 2, 11, 0)
      val create2 = LocalDateTime.of(2026, 4, 3, 12, 0)
      val update2 = LocalDateTime.of(2026, 4, 4, 13, 0)

      when(rs.getTimestamp("create_date"))
        .thenReturn(Timestamp.valueOf(create1), Timestamp.valueOf(create2))
      when(rs.getTimestamp("last_update"))
        .thenReturn(Timestamp.valueOf(update1), Timestamp.valueOf(update2))

      when(rs.getInt("version")).thenReturn(1, 2)

      val out = CisRowMappers.collectVerifications(rs)

      out.map(_.verificationId) mustBe Seq(1L, 2L)

      out.head.matched mustBe Some("Y")
      out.head.verificationNumber mustBe Some("V123")
      out.head.taxTreatment mustBe Some("NET")
      out.head.actionIndicator mustBe Some("A")
      out.head.verificationBatchId mustBe None
      out.head.schemeId mustBe None
      out.head.subcontractorId mustBe None
      out.head.verificationResourceRef mustBe None
      out.head.subcontractorName mustBe Some("ABC LTD")
      out.head.proceed mustBe Some("Y")
      out.head.createDate mustBe Some(create1)
      out.head.lastUpdate mustBe Some(update1)

      out(1).matched mustBe None
      out(1).verificationNumber mustBe Some("V456")
      out(1).taxTreatment mustBe Some("GROSS")
      out(1).actionIndicator mustBe None
      out(1).verificationBatchId mustBe Some(77L)
      out(1).schemeId mustBe Some(999L)
      out(1).subcontractorId mustBe Some(555L)
      out(1).verificationResourceRef mustBe Some(888L)
      out(1).subcontractorName mustBe Some("XYZ LTD")
      out(1).proceed mustBe Some("N")
      out(1).createDate mustBe Some(create2)
      out(1).lastUpdate mustBe Some(update2)

      verify(rs, times(3)).next()
      verify(rs, times(2)).getLong("verification_id")
    }
  }
}
