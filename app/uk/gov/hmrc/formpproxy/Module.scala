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

package uk.gov.hmrc.formpproxy

import play.api.inject.{Binding, Module as AppModule}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.formpproxy.actions.{AuthAction, DefaultAuthAction}
import uk.gov.hmrc.formpproxy.cis.repositories.{CisFormpRepository, CisMonthlyReturnSource}
import uk.gov.hmrc.formpproxy.sdlt.repositories.{SdltFormpRepository, SdltSource}

class Module extends AppModule:

  override def bindings(
    environment: Environment,
    configuration: Configuration
  ): Seq[Binding[_]] =
    List(
      bind[AuthAction].to(classOf[DefaultAuthAction]),
      bind[CisMonthlyReturnSource].to(classOf[CisFormpRepository]),
      bind[SdltSource].to(classOf[SdltFormpRepository])
    )
