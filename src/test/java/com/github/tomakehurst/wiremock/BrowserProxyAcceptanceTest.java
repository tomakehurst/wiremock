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
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.junit.*;

public class BrowserProxyAcceptanceTest {

  @ClassRule
  public static WireMockClassRule target = new WireMockClassRule(wireMockConfig().dynamicPort());

  @Rule public WireMockClassRule instanceRule = target;

  private WireMockServer proxy;
  private WireMockTestClient testClient;

  @Before
  public void addAResourceToProxy() {
    testClient = new WireMockTestClient(target.port());

    proxy = new WireMockServer(wireMockConfig().dynamicPort().enableBrowserProxying(true));
    proxy.start();
  }

  @After
  public void stopServer() {
    if (proxy.isRunning()) {
      proxy.stop();
    }
  }

  @Test
  public void canProxyHttp() {
    target.stubFor(get(urlEqualTo("/whatever")).willReturn(aResponse().withBody("Got it")));

    assertThat(testClient.getViaProxy(url("/whatever"), proxy.port()).content(), is("Got it"));
  }

  @Test
  public void passesQueryParameters() {
    target.stubFor(
        get(urlEqualTo("/search?q=things&limit=10")).willReturn(aResponse().withStatus(200)));

    assertThat(
        testClient.getViaProxy(url("/search?q=things&limit=10"), proxy.port()).statusCode(),
        is(200));
  }

  private String url(String pathAndQuery) {
    return "http://localhost:" + target.port() + pathAndQuery;
  }
}
