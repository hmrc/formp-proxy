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

package uk.gov.hmrc.formpproxy.cis.utils

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class MonthlyReturnStatusUtilsSpec extends AnyWordSpec with Matchers {

  "mapStatus" should {

    "return STARTED when input is None" in {
      MonthlyReturnStatusUtils.mapStatus(None) shouldBe "STARTED"
    }

    "return STARTED for unknown values" in {
      MonthlyReturnStatusUtils.mapStatus(Some("UNKNOWN")) shouldBe "STARTED"
    }

    "handle lowercase input and trim spaces" in {
      MonthlyReturnStatusUtils.mapStatus(Some(" started ")) shouldBe "STARTED"
    }

    "map STARTED correctly" in {
      MonthlyReturnStatusUtils.mapStatus(Some("STARTED")) shouldBe "STARTED"
    }

    "map VALIDATED correctly" in {
      MonthlyReturnStatusUtils.mapStatus(Some("VALIDATED")) shouldBe "VALIDATED"
    }

    "map PENDING correctly" in {
      MonthlyReturnStatusUtils.mapStatus(Some("PENDING")) shouldBe "PENDING"
    }

    "map ACCEPTED to PENDING" in {
      MonthlyReturnStatusUtils.mapStatus(Some("ACCEPTED")) shouldBe "PENDING"
    }

    "map DEPARTMENTAL_ERROR to REJECTED" in {
      MonthlyReturnStatusUtils.mapStatus(Some("DEPARTMENTAL_ERROR")) shouldBe "REJECTED"
    }

    "map FATAL_ERROR to REJECTED" in {
      MonthlyReturnStatusUtils.mapStatus(Some("FATAL_ERROR")) shouldBe "REJECTED"
    }

    "handle mixed case input" in {
      MonthlyReturnStatusUtils.mapStatus(Some("pEnDiNg")) shouldBe "PENDING"
    }
  }
}
