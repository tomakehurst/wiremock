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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.testsupport.Network;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class WireMockJUnitRuleTest {

  public static class BasicWireMockRule {

    @Rule public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Test
    public void canRegisterStubAndFetchOnCorrectPort() {
      givenThat(get(urlEqualTo("/rule/test")).willReturn(aResponse().withBody("Rule test body")));

      WireMockTestClient testClient = new WireMockTestClient(wireMockRule.port());

      assertThat(testClient.get("/rule/test").content(), is("Rule test body"));
    }
  }

  /**
   * Tests that WireMockRule run as a @Rule resets the WireMock server between tests. If it doesn't
   * do so, one of the two tests will fail (probably 'B', but that's not guaranteed, as JUnit
   * doesn't guarantee the order of test execution).
   */
  public static class WireMockJournalIsResetBetweenMultipleTests {

    @Rule public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void init() {
      WireMock.configureFor(wireMockRule.port());
    }

    @Test
    public void noPreviousRequestsUntilOneMadeA() {
      assertNoPreviousRequestsReceived();
      assertCanRegisterStubAndFetchOnCorrectPort(wireMockRule.port());
    }

    @Test
    public void noPreviousRequestsUntilOneMadeB() {
      assertNoPreviousRequestsReceived();
      assertCanRegisterStubAndFetchOnCorrectPort(wireMockRule.port());
    }
  }

  public static class WireMockRuleAsJUnit411ClassRule {

    @ClassRule
    public static WireMockClassRule classRule =
        new WireMockClassRule(wireMockConfig().dynamicPort());

    @Rule public WireMockClassRule instanceRule = classRule;

    @Before
    public void init() {
      WireMock.configureFor(classRule.port());
    }

    @Test
    public void testStubAndFetchOnce() {
      assertNoPreviousRequestsReceived();
      assertCanRegisterStubAndFetchOnCorrectPort(classRule.port());
    }

    @Test
    public void testStubAndFetchAgain() {
      assertNoPreviousRequestsReceived(); // Will fail if reset() not called after the previous test
      // case
      assertCanRegisterStubAndFetchOnCorrectPort(classRule.port());
    }
  }

  /**
   * Tests that WireMockClassRule run as a @Rule resets the WireMock server between tests. If it
   * doesn't do so, one of the two tests will fail (probably 'B', but that's not guaranteed, as
   * JUnit doesn't guarantee the order of test execution).
   */
  public static
  class WireMockJournalIsResetBetweenMultipleTestsWithWireMockRuleAsJUnit411ClassRule {

    @ClassRule
    public static WireMockClassRule wireMockRule1 =
        new WireMockClassRule(wireMockConfig().dynamicPort());

    @Rule public WireMockClassRule instancewireMockRule1 = wireMockRule1;

    @ClassRule
    public static WireMockClassRule wireMockRule2 =
        new WireMockClassRule(wireMockConfig().dynamicPort());

    @Rule public WireMockClassRule instancewireMockRule2 = wireMockRule2;

    @Test
    public void noPreviousRequestsUntilOneMadeA() {
      assertNoPreviousRequestsReceived(instancewireMockRule1);
      assertNoPreviousRequestsReceived(instancewireMockRule2);

      assertCanRegisterStubAndFetchOnCorrectPort(instancewireMockRule1);
      assertCanRegisterStubAndFetchOnCorrectPort(instancewireMockRule2);
    }

    @Test
    public void noPreviousRequestsUntilOneMadeB() {
      assertNoPreviousRequestsReceived(instancewireMockRule1);
      assertNoPreviousRequestsReceived(instancewireMockRule2);

      assertCanRegisterStubAndFetchOnCorrectPort(instancewireMockRule1);
      assertCanRegisterStubAndFetchOnCorrectPort(instancewireMockRule2);
    }

    private static void assertNoPreviousRequestsReceived(WireMockClassRule wireMockRule) {
      wireMockRule.verify(0, getRequestedFor(urlMatching(".*")));
    }

    private static void assertCanRegisterStubAndFetchOnCorrectPort(WireMockClassRule wireMockRule) {
      wireMockRule.givenThat(
          get(urlEqualTo("/rule/test")).willReturn(aResponse().withBody("Rule test body")));

      WireMockTestClient testClient = new WireMockTestClient(wireMockRule.port());

      assertThat(testClient.get("/rule/test").content(), is("Rule test body"));
    }
  }

  public static class PortNumbers {

    private static final int RULE_HTTP_PORT = Network.findFreePort();
    private static final int RULE_HTTPS_PORT = Network.findFreePort();
    private static final int CLASSRULE_HTTP_PORT = Network.findFreePort();
    private static final int CLASSRULE_HTTPS_PORT = Network.findFreePort();

    @Rule
    public WireMockRule wireMockRule =
        new WireMockRule(wireMockConfig().port(RULE_HTTP_PORT).httpsPort(RULE_HTTPS_PORT));

    @ClassRule
    public static WireMockClassRule wireMockClassRule =
        new WireMockClassRule(
            wireMockConfig().port(CLASSRULE_HTTP_PORT).httpsPort(CLASSRULE_HTTPS_PORT));

    @Test
    public void reportedPortIsAsConfiguredInRule() {
      assertThat(wireMockRule.port(), equalTo(RULE_HTTP_PORT));
    }

    @Test
    public void reportedPortIsAsConfiguredInClassRule() {
      assertThat(wireMockClassRule.port(), equalTo(CLASSRULE_HTTP_PORT));
    }

    @Test
    public void reportedHttpsPortIsAsConfiguredInRule() {
      assertThat(wireMockRule.httpsPort(), equalTo(RULE_HTTPS_PORT));
    }

    @Test
    public void reportedHttpsPortIsAsConfiguredInClassRule() {
      assertThat(wireMockClassRule.httpsPort(), equalTo(CLASSRULE_HTTPS_PORT));
    }
  }

  public static class RuleStubbing {

    public static final int PORT1 = Network.findFreePort();
    public static final int PORT2 = Network.findFreePort();
    public static final int PORT3 = Network.findFreePort();
    public static final int PORT4 = Network.findFreePort();

    @ClassRule
    public static WireMockClassRule serviceOne =
        new WireMockClassRule(wireMockConfig().port(PORT1));

    @ClassRule
    public static WireMockClassRule serviceTwo =
        new WireMockClassRule(wireMockConfig().port(PORT2));

    @Rule public WireMockRule serviceThree = new WireMockRule(wireMockConfig().port(PORT3));
    @Rule public WireMockRule serviceFour = new WireMockRule(wireMockConfig().port(PORT4));

    @Rule public WireMockRule portZeroRule = new WireMockRule(wireMockConfig().port(0));

    @Rule
    public WireMockClassRule portZeroClassRule = new WireMockClassRule(wireMockConfig().port(0));

    @Test
    public void canStubAndVerifyMultipleWireMockRulesWithoutInterferenceBetweenRuleInstances() {
      setupStubbing(serviceOne, "service one");
      setupStubbing(serviceTwo, "service two");
      setupStubbing(serviceThree, "service three");
      setupStubbing(serviceFour, "service four");

      stubIsCalledAndResponseIsCorrect(serviceOne, PORT1, "service one");
      stubIsCalledAndResponseIsCorrect(serviceTwo, PORT2, "service two");
      stubIsCalledAndResponseIsCorrect(serviceThree, PORT3, "service three");
      stubIsCalledAndResponseIsCorrect(serviceFour, PORT4, "service four");
    }

    @Test
    public void canStubOnPortZero() {
      setupStubbing(portZeroRule, "port zero rule");
      setupStubbing(portZeroClassRule, "port zero class rule");

      stubIsCalledAndResponseIsCorrect(portZeroRule, portZeroRule.port(), "port zero rule");
      stubIsCalledAndResponseIsCorrect(
          portZeroClassRule, portZeroClassRule.port(), "port zero class rule");
    }

    private void setupStubbing(Stubbing stubbing, String body) {
      stubbing.stubFor(get(urlEqualTo("/test")).willReturn(aResponse().withBody(body)));
    }

    private void stubIsCalledAndResponseIsCorrect(
        Stubbing stubbing, int port, String expectedText) {
      assertThat(new WireMockTestClient(port).get("/test").content(), is(expectedText));
      stubbing.verify(getRequestedFor(urlEqualTo("/test")));
    }
  }

  public static class ListenerTest {

    @Rule
    public WireMockRule wireMockRule =
        new WireMockRule(wireMockConfig().dynamicPort().notifier(new ConsoleNotifier(true)));

    @Test
    public void requestReceivedByListener() {
      final List<String> urls = new ArrayList<String>();
      wireMockRule.addMockServiceRequestListener(
          new RequestListener() {
            @Override
            public void requestReceived(Request request, Response response) {
              urls.add(request.getUrl());
            }
          });
      wireMockRule.stubFor(
          get(urlEqualTo("/test/listener")).willReturn(aResponse().withBody("Listener")));

      WireMockTestClient testClient = new WireMockTestClient(wireMockRule.port());
      assertThat(testClient.get("/test/listener").content(), is("Listener"));
      assertThat(urls.size(), is(1));
      assertThat(urls.get(0), is("/test/listener"));
    }
  }

  public static class HttpsOnly {

    @Rule
    public WireMockRule wireMockRule =
        new WireMockRule(wireMockConfig().dynamicHttpsPort().httpDisabled(true));

    @Test
    public void exposesHttpsOnly() throws Exception {
      wireMockRule.stubFor(any(anyUrl()).willReturn(ok()));

      HttpClient client = HttpClientFactory.createClient();

      HttpGet request = new HttpGet("https://localhost:" + wireMockRule.httpsPort() + "/anything");
      HttpResponse response = client.execute(request);

      assertThat(response.getStatusLine().getStatusCode(), is(200));
    }
  }

  private static void assertNoPreviousRequestsReceived() {
    verify(0, getRequestedFor(urlMatching(".*")));
  }

  public static void assertCanRegisterStubAndFetchOnCorrectPort(int port) {
    givenThat(get(urlEqualTo("/rule/test")).willReturn(aResponse().withBody("Rule test body")));

    WireMockTestClient testClient = new WireMockTestClient(port);

    assertThat(testClient.get("/rule/test").content(), is("Rule test body"));
  }
}
