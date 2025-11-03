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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.formpproxy.actions.{AuthAction, DefaultAuthAction}
import uk.gov.hmrc.formpproxy.cis.repositories.{CisFormpRepository, CisMonthlyReturnSource}
import uk.gov.hmrc.formpproxy.cis.utils.CisFormpStub

class ModuleSpec extends AnyWordSpec with Matchers {

  "Module bindings" should {

    "bind AuthAction -> DefaultAuthAction and CisMonthlyReturnSource -> CisFormpStub when feature is ON" in {
      val app = new GuiceApplicationBuilder()
        .overrides(new Module)
        .configure("feature-switch.cis-formp-stubbed" -> true)
        .build()

      running(app) {
        val inj = app.injector
        inj.instanceOf(classOf[AuthAction]) mustBe a[DefaultAuthAction]
        inj.instanceOf(classOf[CisMonthlyReturnSource]) mustBe a[CisFormpStub]
      }
    }

    "bind CisMonthlyReturnSource -> CisFormpRepository when feature is OFF" in {
      val app = new GuiceApplicationBuilder()
        .overrides(new Module)
        .configure("feature-switch.cis-formp-stubbed" -> false)
        .build()

      running(app) {
        val inj = app.injector
        inj.instanceOf(classOf[CisMonthlyReturnSource]) mustBe a[CisFormpRepository]
      }
    }
  }
}
