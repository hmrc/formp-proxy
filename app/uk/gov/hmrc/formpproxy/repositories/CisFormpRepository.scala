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
  def createNilMonthlyReturn(
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    decEmpStatusConsidered: Option[String],
    decInformationCorrect: Option[String]
  ): Future[MonthlyReturn]
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


  override def createNilMonthlyReturn(
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    decEmpStatusConsidered: Option[String],
    decInformationCorrect: Option[String]
  ): Future[MonthlyReturn] = Future {
    db.withConnection { conn =>
      conn.setAutoCommit(false)
      try {
        val currentVersion: Int = {
          val cs0 = conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }")
          try {
            cs0.setString(1, instanceId)
            cs0.registerOutParameter(2, OracleTypes.CURSOR)
            cs0.registerOutParameter(3, OracleTypes.CURSOR)
            cs0.execute()

            val rsScheme = cs0.getObject(2, classOf[ResultSet])
            val version = try if (rsScheme != null && rsScheme.next()) rsScheme.getInt("version") else 1
            finally if (rsScheme != null) rsScheme.close()
            
            val rsMonthly = cs0.getObject(3, classOf[ResultSet])
            try if (rsMonthly != null) rsMonthly.close()
            catch { case _: Exception => () }
            
            version
          } finally cs0.close()
        }

        val cs1 = conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }")
        try {
          cs1.setString(1, instanceId)
          cs1.setInt(2, taxYear)
          cs1.setInt(3, taxMonth)
          cs1.setString(4, "Y")
          cs1.execute()
        } finally cs1.close()

        val cs2 = conn.prepareCall("{ call SCHEME_PROCS.Update_Version_Number(?, ?) }")
        try {
          cs2.setString(1, instanceId)
          cs2.setInt(2, currentVersion)
          cs2.registerOutParameter(2, java.sql.Types.INTEGER)
          cs2.execute()
          val newVersion = cs2.getInt(2)
          
          val cs3 = conn.prepareCall("{ call MONTHLY_RETURN_PROCS_2016.Update_monthly_return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")
          try {
            cs3.setString(1, instanceId)
            cs3.setInt(2, taxYear)
            cs3.setInt(3, taxMonth)
            cs3.setString(4, "N")
            cs3.setString(5, mapInactivityRequest(decEmpStatusConsidered))
            cs3.setString(6, "Y")
            cs3.setString(7, mapDeclaration(decInformationCorrect))
            cs3.setString(8, "Y")
            cs3.setString(9, "Y")
            cs3.setString(10, "Y")
            cs3.setString(11, "STARTED")
            cs3.setInt(12, newVersion)
            cs3.execute()
            
            conn.commit()
            MonthlyReturn(
              monthlyReturnId = 0L,
              taxYear = taxYear,
              taxMonth = taxMonth,
              nilReturnIndicator = Some("Y"),
              decEmpStatusConsidered = decEmpStatusConsidered,
              decAllSubsVerified = Some("Y"),
              decInformationCorrect = decInformationCorrect,
              decNoMoreSubPayments = Some("Y"),
              decNilReturnNoPayments = Some("Y"),
              status = Some("STARTED"),
              lastUpdate = Some(java.time.LocalDateTime.now()),
              amendment = Some("N"),
              supersededBy = None
            )
          } finally cs3.close()
        } finally cs2.close()
      } catch {
        case e: Exception =>
          conn.rollback()
          throw e
      } finally {
        conn.setAutoCommit(true)
      }
    }
  }

  private def mapInactivityRequest(value: Option[String]): String = value match {
    case Some("option1") => "Y"
    case Some("option2") => "N"
    case _ => null
  }

  private def mapDeclaration(value: Option[String]): String = value match {
    case Some("confirmed") => "Y"
    case _ => null
  }
}