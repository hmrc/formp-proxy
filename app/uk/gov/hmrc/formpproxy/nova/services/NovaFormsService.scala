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

package uk.gov.hmrc.formpproxy.nova.services

import com.google.inject.ImplementedBy
import uk.gov.hmrc.formpproxy.nova.models.*
import uk.gov.hmrc.formpproxy.nova.repositories.NovaSource

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@ImplementedBy(classOf[NovaFormsServiceImpl])
trait NovaFormsService {
  def getForms(userId: String, formType: Option[String], formStatus: Option[String]): Future[FormsResponse]
  def getForm(formId: Long): Future[Option[FormDetail]]
  def createForm(request: CreateFormRequest): Future[StoreFormResponse]
  def updateForm(formId: Long, request: UpdateFormRequest): Future[StoreFormResponse]
  def updateFormStatus(formId: Long, request: UpdateFormStatusRequest): Future[StoreFormResponse]
  def deleteForm(formId: Long): Future[Unit]
}

@Singleton
class NovaFormsServiceImpl @Inject() (repo: NovaSource) extends NovaFormsService {

  private val DefaultFormType   = "NOVA"
  private val DefaultFormStatus = "'UNSUBMITTED'"

  override def getForms(userId: String, formType: Option[String], formStatus: Option[String]): Future[FormsResponse] =
    repo.getForms(
      userId,
      formType.getOrElse(DefaultFormType),
      formStatus.map(quoteForInClause).getOrElse(DefaultFormStatus)
    )

  private def quoteForInClause(status: String): String =
    status.split(",").map(_.trim).map(s => s"'$s'").mkString(",")

  override def getForm(formId: Long): Future[Option[FormDetail]] =
    repo.getForm(formId)

  override def createForm(request: CreateFormRequest): Future[StoreFormResponse] =
    repo.createForm(request.userId)

  override def updateForm(formId: Long, request: UpdateFormRequest): Future[StoreFormResponse] =
    repo.updateForm(formId, request)

  override def updateFormStatus(formId: Long, request: UpdateFormStatusRequest): Future[StoreFormResponse] =
    repo.updateFormStatus(formId, request)

  override def deleteForm(formId: Long): Future[Unit] =
    repo.deleteForm(formId)
}
