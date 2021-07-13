/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.verification.diff.JUnitStyleDiffRenderer.junitStyleDiffMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.github.tomakehurst.wiremock.testsupport.TestNotifier;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import ignored.ManyUnmatchedRequestsTest;
import ignored.SingleUnmatchedRequestTest;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

@RunWith(Enclosed.class)
public class NearMissesRuleAcceptanceTest {

  public static class NearMissesRuleTest {

    static TestNotifier testNotifier = new TestNotifier();

    @Rule public ExpectedException thrown = ExpectedException.none();

    @ClassRule
    public static WireMockRule wm =
        new WireMockRule(
            options()
                .dynamicPort()
                .notifier(testNotifier)
                .withRootDirectory("src/main/resources/empty"),
            false);

    WireMockTestClient client;

    @Before
    public void init() {
      client = new WireMockTestClient(wm.port());
      testNotifier.reset();
      wm.resetAll();
    }

    @Test
    public void logsUnmatchedRequestsAtErrorWithNearMisses() throws Exception {
      wm.stubFor(get(urlEqualTo("/near-miss")).willReturn(aResponse().withStatus(200)));
      wm.stubFor(get(urlEqualTo("/miss")).willReturn(aResponse().withStatus(200)));

      client.post("/a-near-mis", new StringEntity(""));

      assertThat(
          testNotifier.getErrorMessages(),
          hasItem(
              allOf(
                  containsString("Request was not matched"),
                  containsString("/a-near-mis"),
                  containsString("/near-miss"),
                  containsString("HTTP method does not match"),
                  containsString("URL does not match"))));
    }

    @Test
    public void throwsVerificationExceptionIfSomeRequestsWentUnmatched() {
      String message = runTestAndGetMessage(ManyUnmatchedRequestsTest.class);

      assertThat(message, containsString("2 requests were unmatched by any stub mapping"));
      assertThat(
          message, containsString(junitStyleDiffMessage("GET\n/hit\n", "GET\n/near-misssss\n")));
      assertThat(
          message, containsString(junitStyleDiffMessage("GET\n/hit\n", "GET\n/a-near-mis\n")));
    }

    @Test
    public void throwsVerificationExceptionIfASingleRequestWentUnmatched() {
      String message = runTestAndGetMessage(SingleUnmatchedRequestTest.class);
      assertThat(
          message,
          containsString("A request was unmatched by any stub mapping. Closest stub mapping was:"));
      assertThat(
          message, containsString(junitStyleDiffMessage("GET\n/hit\n", "GET\n/near-misssss\n")));
    }

    @Test
    public void shouldFindNearMatch() {
      thrown.expect(VerificationException.class);
      thrown.expectMessage("No requests exactly matched. Most similar request was:");

      client.get("/123");

      wm.verify(getRequestedFor(urlPathEqualTo("/")));
    }

    private static String runTestAndGetMessage(Class<?> testClass) {
      final AtomicReference<String> message = new AtomicReference<>("");

      JUnitCore junit = new JUnitCore();
      junit.addListener(
          new RunListener() {

            @Override
            public void testFailure(Failure failure) throws Exception {
              message.set(failure.getMessage());
            }
          });
      junit.run(testClass);
      return message.get();
    }
  }

  public static class CustomMatcherWithNearMissesTest {

    @ClassRule
    public static WireMockRule wm =
        new WireMockRule(
            options()
                .dynamicPort()
                .withRootDirectory("src/main/resources/empty")
                .extensions(
                    new RequestMatcherExtension() {
                      @Override
                      public MatchResult match(Request request, Parameters parameters) {
                        return MatchResult.partialMatch(0.5);
                      }

                      @Override
                      public String getName() {
                        return "always-match";
                      }
                    }),
            false);

    WireMockTestClient client;

    @Before
    public void init() {
      client = new WireMockTestClient(wm.port());
      wm.resetAll();
    }

    @Test
    public void successfullyCalculatesNearMissesWhenACustomMatcherIsRegistered() {
      wm.stubFor(requestMatching("always-match").willReturn(aResponse()));

      client.get("/");

      assertThat(wm.findNearMissesForAllUnmatchedRequests().size(), is(1));
    }
  }
}
