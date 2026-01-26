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

import uk.gov.hmrc.formpproxy.cis.models.{ContractorScheme, MonthlyReturn, MonthlyReturnItem, Subcontractor, Submission}
import uk.gov.hmrc.formpproxy.shared.utils.ResultSetUtils.*

import java.sql.ResultSet
import scala.annotation.tailrec

object CisRowMappers {

// public 1-arg collectors

  def collectMonthlyReturns(rs: ResultSet): Seq[MonthlyReturn] =
    collectMonthlyReturns(rs, Nil)

  def collectSchemes(rs: ResultSet): Seq[ContractorScheme] =
    collectSchemes(rs, Nil)

  def collectMonthlyReturnItems(rs: ResultSet): Seq[MonthlyReturnItem] =
    collectMonthlyReturnItems(rs, Nil)

  def collectSubmissions(rs: ResultSet): Seq[Submission] =
    collectSubmissions(rs, Nil)

  def collectSubcontractors(rs: ResultSet): Seq[Subcontractor] =
    collectSubcontractors(rs, Nil)

// private tail-recursive implementations

  @tailrec
  private def collectMonthlyReturns(rs: ResultSet, acc: Seq[MonthlyReturn]): Seq[MonthlyReturn] =
    if (rs == null || !rs.next()) acc
    else collectMonthlyReturns(rs, acc :+ readMonthlyReturn(rs))

  @tailrec
  private def collectSchemes(rs: ResultSet, acc: Seq[ContractorScheme]): Seq[ContractorScheme] =
    if (rs == null || !rs.next()) acc
    else collectSchemes(rs, acc :+ readContractorScheme(rs))

  @tailrec
  private def collectMonthlyReturnItems(rs: ResultSet, acc: Seq[MonthlyReturnItem]): Seq[MonthlyReturnItem] =
    if (rs == null || !rs.next()) acc
    else collectMonthlyReturnItems(rs, acc :+ readMonthlyReturnItem(rs))

  @tailrec
  private def collectSubmissions(rs: ResultSet, acc: Seq[Submission]): Seq[Submission] =
    if (rs == null || !rs.next()) acc
    else collectSubmissions(rs, acc :+ readSubmission(rs))

  @tailrec
  private def collectSubcontractors(rs: ResultSet, acc: Seq[Subcontractor]): Seq[Subcontractor] =
    if (rs == null || !rs.next()) acc
    else collectSubcontractors(rs, acc :+ readSubcontractor(rs))

// Row readers

  def readMonthlyReturn(rs: ResultSet): MonthlyReturn =
    MonthlyReturn(
      monthlyReturnId = rs.getLong("monthly_return_id"),
      taxYear = rs.getInt("tax_year"),
      taxMonth = rs.getInt("tax_month"),
      nilReturnIndicator = rs.getOptionalString("nil_return_indicator"),
      decEmpStatusConsidered = rs.getOptionalString("dec_emp_status_considered"),
      decAllSubsVerified = rs.getOptionalString("dec_all_subs_verified"),
      decInformationCorrect = rs.getOptionalString("dec_information_correct"),
      decNoMoreSubPayments = rs.getOptionalString("dec_no_more_sub_payments"),
      decNilReturnNoPayments = rs.getOptionalString("dec_nil_return_no_payments"),
      status = rs.getOptionalString("status"),
      lastUpdate = rs.getOptionalLocalDateTime("last_update"),
      amendment = rs.getOptionalString("amendment"),
      supersededBy = rs.getOptionalLong("superseded_by")
    )

  def readContractorScheme(rs: ResultSet): ContractorScheme =
    ContractorScheme(
      schemeId = rs.getInt("scheme_id"),
      instanceId = rs.getString("instance_id"),
      accountsOfficeReference = rs.getString("aoref"),
      taxOfficeNumber = rs.getString("tax_office_number"),
      taxOfficeReference = rs.getString("tax_office_reference"),
      utr = rs.getOptionalString("utr"),
      name = rs.getOptionalString("name"),
      emailAddress = rs.getOptionalString("email_address"),
      displayWelcomePage = rs.getOptionalString("display_welcome_page"),
      prePopCount = rs.getOptionalInt("pre_pop_count"),
      prePopSuccessful = rs.getOptionalString("pre_pop_successful"),
      subcontractorCounter = rs.getOptionalInt("subcontractor_counter"),
      verificationBatchCounter = rs.getOptionalInt("verif_batch_counter"),
      lastUpdate = rs.getOptionalInstant("last_update"),
      version = rs.getOptionalInt("version")
    )

  def readMonthlyReturnItem(rs: ResultSet): MonthlyReturnItem =
    MonthlyReturnItem(
      monthlyReturnId = rs.getLong("monthly_return_id"),
      monthlyReturnItemId = rs.getLong("monthly_return_item_id"),
      totalPayments = Option(rs.getString("total_payments")),
      costOfMaterials = Option(rs.getString("cost_of_materials")),
      totalDeducted = Option(rs.getString("total_deducted")),
      unmatchedTaxRateIndicator = Option(rs.getString("unmatched_tax_rate_ind")),
      subcontractorId = rs.getOptionalLong("subcontractor_id"),
      subcontractorName = Option(rs.getString("subcontractor_name")),
      verificationNumber = Option(rs.getString("verification_number")),
      itemResourceReference = rs.getOptionalLong("item_resource_ref")
    )

  def readSubmission(rs: ResultSet): Submission =
    Submission(
      submissionId = rs.getLong("submission_id"),
      submissionType = rs.getString("submission_type"),
      activeObjectId = rs.getOptionalLong("active_object_id"),
      status = Option(rs.getString("status")),
      hmrcMarkGenerated = Option(rs.getString("hmrc_mark_generated")),
      hmrcMarkGgis = Option(rs.getString("hmrc_mark_ggis")),
      emailRecipient = Option(rs.getString("email_recipient")),
      acceptedTime = Option(rs.getString("accepted_time")),
      createDate = rs.getOptionalLocalDateTime("create_date"),
      lastUpdate = rs.getOptionalLocalDateTime("last_update"),
      schemeId = rs.getLong("scheme_id"),
      agentId = Option(rs.getString("agent_id")),
      l_Migrated = rs.getOptionalLong("l_migrated"),
      submissionRequestDate = rs.getOptionalLocalDateTime("submission_request_date"),
      govTalkErrorCode = Option(rs.getString("govtalk_error_code")),
      govTalkErrorType = Option(rs.getString("govtalk_error_type")),
      govTalkErrorMessage = Option(rs.getString("govtalk_error_message"))
    )

  def readSubcontractor(rs: ResultSet): Subcontractor =
    Subcontractor(
      subcontractorId = rs.getLong("subcontractor_id"),
      subbieResourceRef = rs.getOptionalLong("subbie_resource_ref"),
      subcontractorType = Option(rs.getString("type")),
      utr = Option(rs.getString("utr")),
      pageVisited = rs.getOptionalInt("page_visited"),
      partnerUtr = Option(rs.getString("partner_utr")),
      crn = Option(rs.getString("crn")),
      firstName = Option(rs.getString("firstname")),
      nino = Option(rs.getString("nino")),
      secondName = Option(rs.getString("secondname")),
      surname = Option(rs.getString("surname")),
      partnershipTradingName = Option(rs.getString("partnership_tradingname")),
      tradingName = Option(rs.getString("tradingname")),
      addressLine1 = Option(rs.getString("address_line_1")),
      addressLine2 = Option(rs.getString("address_line_2")),
      addressLine3 = Option(rs.getString("address_line_3")),
      addressLine4 = Option(rs.getString("address_line_4")),
      country = Option(rs.getString("country")),
      postCode = Option(rs.getString("postcode")),
      emailAddress = Option(rs.getString("email_address")),
      phoneNumber = Option(rs.getString("phone_number")),
      mobilePhoneNumber = Option(rs.getString("mobile_phone_number")),
      worksReferenceNumber = Option(rs.getString("works_reference_number")),
      version = rs.getOptionalInt("version"),
      taxTreatment = Option(rs.getString("tax_treatment")),
      updatedTaxTreatment = Option(rs.getString("updated_tax_treatment")),
      verificationNumber = Option(rs.getString("verification_number")),
      createDate = rs.getOptionalLocalDateTime("create_date"),
      lastUpdate = rs.getOptionalLocalDateTime("last_update"),
      matched = Option(rs.getString("matched")),
      verified = Option(rs.getString("verified")),
      autoVerified = Option(rs.getString("auto_verified")),
      verificationDate = rs.getOptionalLocalDateTime("verification_date"),
      lastMonthlyReturnDate = rs.getOptionalLocalDateTime("last_monthly_return_date"),
      pendingVerifications = rs.getOptionalInt("pending_verifications")
    )
}
