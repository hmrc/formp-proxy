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

object CisStoredProcedures {
  val CallCreateMonthlyReturn             = "{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return(?, ?, ?, ?) }"
  val CallUpdateSchemeVersion             = "{ call SCHEME_PROCS.Update_Version_Number(?, ?) }"
  val CallUpdateMonthlyReturn             =
    "{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallUpdateMonthlyReturnItem         =
    "{ call MONTHLY_RETURN_PROCS_2016.Update_Monthly_Return_Item(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallGetScheme                       = "{ call SCHEME_PROCS.int_Get_Scheme(?, ?) }"
  val CallCreateScheme                    = "{ call SCHEME_PROCS.Create_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallUpdateScheme                    = "{ call SCHEME_PROCS.Update_Scheme(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallCreateSubcontractor             = "{ call SUBCONTRACTOR_PROCS.CREATE_SUBCONTRACTOR(?, ?, ?, ?) }"
  val CallGetAllMonthlyReturns            = "{ call MONTHLY_RETURN_PROCS_2016.Get_All_Monthly_Returns(?, ?, ?) }"
  val CallGetUnsubmittedMonthlyReturns    = "{ call MONTHLY_RETURN_PROCS_2016.Get_Monthly_Returns(?, ?, ?) }"
  val CallGetMonthlyReturnForEdit         =
    "{ call MONTHLY_RETURN_PROCS_2016.Get_Monthly_Return_For_Edit(?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallUpdateSubcontractor             =
    "{ call SUBCONTRACTOR_PROCS.Update_Subcontractor(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallCreateSubmission                = "{ call SUBMISSION_PROCS.Create_Submission(?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallUpdateMonthlyReturnSubmission   =
    "{ call SUBMISSION_PROCS_2016.UPDATE_MR_SUBMISSION(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallCreateMonthlyReturnItem         = "{ call MONTHLY_RETURN_PROCS_2016.Create_Monthly_Return_Item(?, ?, ?, ?, ?) }"
  val CallDeleteMonthlyReturnItem         = "{ call MONTHLY_RETURN_PROCS_2016.Delete_Monthly_Return_Item(?, ?, ?, ?, ?) }"
  val CallGetSubcontractorList            = "{ call SUBCONTRACTOR_PROCS.Get_Subcontractor_List(?, ?, ?) }"
  val CallGetGovTalkStatus                =
    "{ call SUBMISSION_ADMIN.SelectGovTalkStatus(?, ?, ?) }"
  val UpdateGetGovTalkStatusCorrelationId = "{ call SUBMISSION_ADMIN.UpdateGovTalkStatusCorr(?, ?, ?, ?, ?) }"
  val CallResetGovTalkStatus              =
    "{ call SUBMISSION_ADMIN.ResetGovTalkStatusRecord(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallUpdateGovTalkStatus             =
    "{ call SUBMISSION_ADMIN.UpdateGovtalkStatus(?, ?, ?, ?) }"
}
