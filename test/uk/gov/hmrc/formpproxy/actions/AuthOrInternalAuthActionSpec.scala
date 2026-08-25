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

package uk.gov.hmrc.formpproxy.actions

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, BodyParsers, ControllerComponents, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.{AuthConnector, BearerTokenExpired}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client.{IAAction, Predicate, Resource, Retrieval}

import scala.concurrent.{ExecutionContext, Future}

class AuthOrInternalAuthActionSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "AuthOrInternalAuthAction" - {

    "lets a signed in user through without troubling internal-auth" in new Setup {
      when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.unit)

      val res: Future[Result] = endpoint(userRequest)

      status(res) mustBe OK
      contentAsString(res) mustBe "reached the endpoint"
      verifyNoInteractions(internalAuth)
    }

    "lets the batch jobs through on their token when there is no user session" in new Setup {
      when(internalAuth.stubAuth(Some(readReturns), Retrieval.EmptyRetrieval)).thenReturn(Future.unit)

      val res: Future[Result] = endpoint(serviceRequest)

      status(res) mustBe OK
      verifyNoInteractions(mockAuthConnector)
    }

    "falls back to internal-auth when the user session has expired" in new Setup {
      when(mockAuthConnector.authorise[Unit](any(), any())(any(), any()))
        .thenReturn(Future.failed(BearerTokenExpired()))
      when(internalAuth.stubAuth(Some(readReturns), Retrieval.EmptyRetrieval)).thenReturn(Future.unit)

      val res: Future[Result] = endpoint(userAndServiceRequest)

      status(res) mustBe OK
    }

    "turns away a caller carrying neither a session nor a token" in new Setup {
      when(internalAuth.stubAuth(Some(readReturns), Retrieval.EmptyRetrieval))
        .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

      val rejection: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
        await(endpoint(serviceRequest))
      }

      rejection.statusCode mustBe UNAUTHORIZED
    }

    "asks internal-auth for the permission the endpoint declared and not some other one" in new Setup {
      when(internalAuth.stubAuth(Some(deleteReturns), Retrieval.EmptyRetrieval)).thenReturn(Future.unit)

      val deleteEndpoint: Action[AnyContent] =
        authOrInternalAuth(deleteReturns).apply(_ => Results.Ok("reached the endpoint"))

      status(deleteEndpoint(serviceRequest)) mustBe OK

      verify(internalAuth).stubAuth(Some(deleteReturns), Retrieval.EmptyRetrieval)
      verify(internalAuth, never).stubAuth(Some(readReturns), Retrieval.EmptyRetrieval)
    }

    "lets a genuine failure from the endpoint bubble up rather than retrying it as a service call" in new Setup {
      when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.unit)

      val failing: Action[AnyContent] =
        authOrInternalAuth(readReturns).async(_ => Future.failed(new RuntimeException("Database connection failed")))

      val boom: RuntimeException = intercept[RuntimeException] {
        await(failing(userRequest))
      }

      boom.getMessage mustBe "Database connection failed"
      verifyNoInteractions(internalAuth)
    }
  }

  trait Setup {
    implicit val ec: ExecutionContext = ExecutionContext.global
    val cc: ControllerComponents      = stubControllerComponents()

    private val returns = Resource.from("formp-proxy", "formp-proxy/sdlt/returns")

    val readReturns: Predicate.Permission   = Predicate.Permission(returns, IAAction("READ"))
    val deleteReturns: Predicate.Permission = Predicate.Permission(returns, IAAction("DELETE"))

    val internalAuth: StubBehaviour      = mock[StubBehaviour]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]

    val authOrInternalAuth: AuthOrInternalAuthAction =
      new AuthOrInternalAuthAction(
        mockAuthConnector,
        BackendAuthComponentsStub(internalAuth)(cc, ec),
        new BodyParsers.Default(cc.parsers)
      )

    val endpoint: Action[AnyContent] = authOrInternalAuth(readReturns).apply(_ => Results.Ok("reached the endpoint"))

    val userRequest: FakeRequest[AnyContent] =
      FakeRequest(GET, "/formp-proxy/retrieve-return")
        .withHeaders("X-Session-ID" -> "session-abc")
        .withBody[AnyContent](AnyContentAsEmpty)

    val serviceRequest: FakeRequest[AnyContent] =
      FakeRequest(GET, "/formp-proxy/retrieve-return")
        .withHeaders(AUTHORIZATION -> "Token internal-auth")
        .withBody[AnyContent](AnyContentAsEmpty)

    val userAndServiceRequest: FakeRequest[AnyContent] =
      FakeRequest(GET, "/formp-proxy/retrieve-return")
        .withHeaders("X-Session-ID" -> "session-abc", AUTHORIZATION -> "Token internal-auth")
        .withBody[AnyContent](AnyContentAsEmpty)
  }
}
