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
/*
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
package com.github.tomakehurst.wiremock.junit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.Matchers.containsString;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

public class WireMockRuleFailOnUnmatchedRequestsTest {

  public ExpectedException expectedException = ExpectedException.none();

  public WireMockRule wm =
      new WireMockRule(options().dynamicPort().withRootDirectory("src/main/resources/empty"), true);

  @Rule public TestRule chain = RuleChain.outerRule(expectedException).around(wm);

  WireMockTestClient client;

  @Before
  public void init() {
    client = new WireMockTestClient(wm.port());
  }

  @Test
  public void singleUnmatchedRequestShouldThrowVerificationException() {
    expectedException.expect(VerificationException.class);
    expectedException.expectMessage(containsString("A request was unmatched by any stub mapping"));
    wm.stubFor(get(urlEqualTo("/hit")).willReturn(aResponse().withStatus(200)));
    client.get("/near-misssss");
  }

  @Test
  public void manyUnmatchedRequestsShouldThrowVerificationException() {
    expectedException.expect(VerificationException.class);
    expectedException.expectMessage(
        containsString("3 requests were unmatched by any stub mapping"));
    wm.stubFor(get(urlEqualTo("/hit")).willReturn(aResponse().withStatus(200)));
    client.get("/near-misssss");
    client.get("/hat");
    client.get("/whatevs");
  }

  @Test
  public void unmatchedRequestButMatchedStubShouldThrowVerificationException() {
    expectedException.expect(VerificationException.class);
    expectedException.expectMessage(containsString("A request was unmatched by any stub mapping"));
    wm.stubFor(get(urlEqualTo("/hit")).willReturn(aResponse().withStatus(200)));
    client.get("/near-misssss");
    client.get("/hit");
  }

  @Test
  public void matchedRequestButUnmatchedStubShouldNotThrowVerificationException() {
    expectedException = ExpectedException.none();
    wm.stubFor(get(urlEqualTo("/hit")).willReturn(aResponse().withStatus(200)));
    wm.stubFor(get(urlEqualTo("/miss")).willReturn(aResponse().withStatus(404)));
    client.get("/hit");
  }

  @Test
  public void unmatchedRequestWithoutStubShouldThrowVerificationException() {
    expectedException.expect(VerificationException.class);
    expectedException.expectMessage(containsString("A request was unmatched by any stub mapping."));

    // Check that url details are part of the output error
    expectedException.expectMessage(containsString("\"url\" : \"/miss\""));

    client.get("/miss");
  }

  @Test
  public void unmatchedStubWithoutRequestShouldNotThrowVerificationException() {
    expectedException = ExpectedException.none();
    wm.stubFor(get(urlEqualTo("/miss")).willReturn(aResponse().withStatus(404)));
  }
}
