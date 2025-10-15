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
import uk.gov.hmrc.formpproxy.models.requests.CreateNilMonthlyReturnRequest
import uk.gov.hmrc.formpproxy.models.response.CreateNilMonthlyReturnResponse

import scala.concurrent.{ExecutionContext, Future}
import java.sql.{Connection, ResultSet, Types}
import scala.annotation.tailrec
import uk.gov.hmrc.formpproxy.models.{MonthlyReturn, UserMonthlyReturns}

trait CisMonthlyReturnSource {
  def getAllMonthlyReturns(instanceId: String): Future[UserMonthlyReturns]
  def createNilMonthlyReturn(request: CreateNilMonthlyReturnRequest): Future[CreateNilMonthlyReturnResponse]
  def getSchemeEmail(instanceId: String): Future[Option[String]]
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
          try () finally if (rsScheme != null) rsScheme.close()

          val rsMonthly = cs.getObject(3, classOf[ResultSet])
          val returns =
            try collectMonthlyReturns(rsMonthly)
            finally if (rsMonthly != null) rsMonthly.close()

          UserMonthlyReturns(returns)
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

  override def createNilMonthlyReturn(request: CreateNilMonthlyReturnRequest): Future[CreateNilMonthlyReturnResponse] = {
    logger.info(s"[CIS] createNilMonthlyReturn(instanceId=${request.instanceId}, taxYear=${request.taxYear}, taxMonth=${request.taxMonth})")
    Future {
      db.withTransaction { conn =>
        val schemeVersionBefore = getSchemeVersion(conn, request.instanceId)

        callCreateMonthlyReturn(conn, request)
        callUpdateSchemeVersion(conn, request.instanceId, schemeVersionBefore)
        callUpdateMonthlyReturn(conn, request)

        CreateNilMonthlyReturnResponse(status = "STARTED")
      }
    }
  }

  private val CallCreateMonthlyReturn = "{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }"
  private val CallUpdateSchemeVersion = "{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"
  private val CallUpdateMonthlyReturn = "{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"

  private def callCreateMonthlyReturn(conn: Connection, req: CreateNilMonthlyReturnRequest): Unit = {
    val cs = conn.prepareCall(CallCreateMonthlyReturn)
    try {
      cs.setString(1, req.instanceId)
      cs.setInt(2, req.taxYear)
      cs.setInt(3, req.taxMonth)
      cs.setString(4, "Y")
      cs.execute()
    } finally cs.close()
  }

  private def callUpdateSchemeVersion(conn: Connection, instanceId: String, currentVersion: Int): Int = {
    val cs = conn.prepareCall(CallUpdateSchemeVersion)
    try {
      cs.setString(1, instanceId)
      cs.setInt(2, currentVersion)
      cs.registerOutParameter(2, Types.INTEGER)
      cs.execute()
      cs.getInt(2)
    } finally cs.close()
  }

  private def callUpdateMonthlyReturn(conn: Connection, req: CreateNilMonthlyReturnRequest): Unit = {
    val cs = conn.prepareCall(CallUpdateMonthlyReturn)
    try {
      cs.setString(1, req.instanceId)
      cs.setInt(2, req.taxYear)
      cs.setInt(3, req.taxMonth)
      cs.setString(4, "N")
      cs.setNull(5, Types.VARCHAR)
      cs.setNull(6, Types.VARCHAR)
      cs.setString(7, req.decInformationCorrect)
      cs.setNull(8, Types.CHAR)
      cs.setString(9, req.decNilReturnNoPayments)
      cs.setString(10, "Y")
      cs.setString(11, "STARTED")
      cs.setInt(12, 0)
      cs.registerOutParameter(12, Types.INTEGER)
      cs.execute()
    } finally cs.close()
  }


  private val SqlGetSchemeVersion =
    "select version from scheme where instance_id = ?"

  private val CallGetSchemeEmail =
    "{ call IntGetSchemeSp(?, ?) }"

  private def getSchemeVersion(conn: Connection, instanceId: String): Int = {
    val statement = conn.prepareStatement(SqlGetSchemeVersion)
    try {
      statement.setString(1, instanceId)
      val rs = statement.executeQuery()
      try {
        if (!rs.next())
          throw new RuntimeException(s"No SCHEME row for instance_id=$instanceId")
        rs.getInt(1)
      } finally rs.close()
    } finally statement.close()
  }

  override def getSchemeEmail(instanceId: String): Future[Option[String]] = {
    logger.info(s"[CIS] getSchemeEmail(instanceId=$instanceId)")
    Future {
      db.withConnection { conn =>
        val cs = conn.prepareCall(CallGetSchemeEmail)
        try {
          cs.setString(1, instanceId)
          cs.registerOutParameter(2, Types.VARCHAR)
          cs.execute()

          val email = cs.getString(2)
          Option(email).map(_.trim).filter(_.nonEmpty)
        } finally cs.close()
      }
    }
  }
  
}