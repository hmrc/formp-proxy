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
      when(rs.wasNull()).thenReturn(true) // for getOptionalLong("superseded_by")

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
  }
}
