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

package uk.gov.hmrc.formpproxy.repositories

import oracle.jdbc.OracleTypes

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.db.{Database, NamedDatabase}

import scala.concurrent.{ExecutionContext, Future}
import java.sql.ResultSet
import scala.annotation.tailrec
import uk.gov.hmrc.formpproxy.models.{MonthlyReturn, UserMonthlyReturns}

trait CisMonthlyReturnSource {
  def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns]
  def createMonthlyReturn(instanceId: String, taxYear: Int, taxMonth: Int, nilReturnIndicator: String): Future[Unit]
  def updateSchemeVersion(instanceId: String, version: Int): Future[Int] // returns new version out param
  def updateMonthlyReturn(
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    amendment: String,
    decEmpStatusConsidered: Option[String],
    decAllSubsVerified: Option[String],
    decInformationCorrect: Option[String],
    decNoMoreSubPayments: Option[String],
    decNilReturnNoPayments: Option[String],
    nilReturnIndicator: String,
    status: String,
    version: Int
  ): Future[Int]
}

@Singleton
class CisFormpRepository @Inject()(@NamedDatabase("cis") db: Database)(implicit ec: ExecutionContext)
  extends CisMonthlyReturnSource with Logging {
  
  override def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns] = {
    logger.info(s"[CIS] getMonthlyReturns(instanceId=$instanceId)")
    Future {
      db.withConnection { conn =>
        val cs = conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }")
        try {
          cs.setString(1, instanceId)
          cs.registerOutParameter(2, OracleTypes.CURSOR)
          cs.registerOutParameter(3, OracleTypes.CURSOR)
          cs.execute()

          val rsScheme = cs.getObject(2, classOf[ResultSet])
          val schemeVersion: Option[Int] =
            try if (rsScheme != null && rsScheme.next()) Option(rsScheme.getInt("version")) else None
            finally if (rsScheme != null) rsScheme.close()

          val rsMonthly = cs.getObject(3, classOf[ResultSet])
          val returns =
            try collectMonthlyReturns(rsMonthly)
            finally if (rsMonthly != null) rsMonthly.close()

          UserMonthlyReturns(returns, schemeVersion)
      } finally {
        cs.close()
        }
      }
    }
  }

  @tailrec
  private def collectMonthlyReturns(rs: ResultSet, acc: Seq[MonthlyReturn] = Nil): Seq[MonthlyReturn] =
    if (!rs.next()) acc
    else {
      val mr = MonthlyReturn(
        monthlyReturnId = rs.getLong("monthly_return_id"),
        taxYear         = rs.getInt("tax_year"),
        taxMonth        = rs.getInt("tax_month"),
        nilReturnIndicator     = Option(rs.getString("nil_return_indicator")),
        decEmpStatusConsidered = Option(rs.getString("dec_emp_status_considered")),
        decAllSubsVerified     = Option(rs.getString("dec_all_subs_verified")),
        decInformationCorrect  = Option(rs.getString("dec_information_correct")),
        decNoMoreSubPayments   = Option(rs.getString("dec_no_more_sub_payments")),
        decNilReturnNoPayments = Option(rs.getString("dec_nil_return_no_payments")),
        status                 = Option(rs.getString("status")),
        lastUpdate             = Option(rs.getTimestamp("last_update")).map(_.toLocalDateTime),
        amendment              = Option(rs.getString("amendment")),
        supersededBy           = { val v = rs.getLong("superseded_by"); if (rs.wasNull()) None else Some(v) }
      )
      collectMonthlyReturns(rs, acc :+ mr)
    }

  override def createMonthlyReturn(instanceId: String, taxYear: Int, taxMonth: Int, nilReturnIndicator: String): Future[Unit] = Future {
    db.withConnection { conn =>
      val cs = conn.prepareCall("{ call monthly_return_procs_2016.Create_Monthly_Return(?, ?, ?, ?) }")
      try {
        cs.setString(1, instanceId)
        cs.setInt(2, taxYear)
        cs.setInt(3, taxMonth)
        cs.setString(4, nilReturnIndicator)
        cs.execute()
        ()
      } finally cs.close()
    }
  }

  override def updateSchemeVersion(instanceId: String, version: Int): Future[Int] = Future {
    db.withConnection { conn =>
      val cs = conn.prepareCall("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }")
      try {
        cs.setString(1, instanceId)
        cs.setInt(2, version)
        cs.execute()
        cs.getInt(2) // p_version out param with new version
      } finally cs.close()
    }
  }

  override def updateMonthlyReturn(
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    amendment: String,
    decEmpStatusConsidered: Option[String],
    decAllSubsVerified: Option[String],
    decInformationCorrect: Option[String],
    decNoMoreSubPayments: Option[String],
    decNilReturnNoPayments: Option[String],
    nilReturnIndicator: String,
    status: String,
    version: Int
  ): Future[Int] = Future {
    db.withConnection { conn =>
      val cs = conn.prepareCall("{ call MONTHLY_RETURNS_PROCS_2016.Update_monthly_return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
      try {
        cs.setString(1, instanceId)
        cs.setInt(2, taxYear)
        cs.setInt(3, taxMonth)
        cs.setString(4, amendment)
        cs.setString(5, decEmpStatusConsidered.orNull)
        cs.setString(6, decAllSubsVerified.orNull)
        cs.setString(7, decInformationCorrect.orNull)
        cs.setString(8, decNoMoreSubPayments.orNull)
        cs.setString(9, decNilReturnNoPayments.orNull)
        cs.setString(10, nilReturnIndicator)
        cs.setString(11, status)
        cs.setInt(12, version)
        cs.execute()
        cs.getInt(12) // p_version out param with new version
      } finally cs.close()
    }
  }
}