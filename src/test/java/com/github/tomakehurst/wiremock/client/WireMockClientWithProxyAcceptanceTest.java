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
package com.github.tomakehurst.wiremock.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

public class WireMockClientWithProxyAcceptanceTest {

  private static WireMockServer wireMockServer;
  private static WireMockTestClient testClient;
  private static HttpProxyServer proxyServer;

  @BeforeClass
  public static void init() {
    wireMockServer = new WireMockServer(DYNAMIC_PORT);
    wireMockServer.start();
    proxyServer = DefaultHttpProxyServer.bootstrap().withPort(0).start();

    testClient = new WireMockTestClient(wireMockServer.port());
  }

  @AfterClass
  public static void stopServer() {
    wireMockServer.stop();
    proxyServer.stop();
  }

  @Test
  public void supportsProxyingWithTheStaticClient() {
    WireMock.configureFor(
        "http",
        "localhost",
        wireMockServer.port(),
        proxyServer.getListenAddress().getHostString(),
        proxyServer.getListenAddress().getPort());

    givenThat(get(urlEqualTo("/my/new/resource")).willReturn(aResponse().withStatus(304)));

    assertThat(testClient.get("/my/new/resource").statusCode(), is(304));
  }

  @Test
  public void supportsProxyingWithTheInstanceClient() {
    WireMock wireMock =
        WireMock.create()
            .scheme("http")
            .host("localhost")
            .port(wireMockServer.port())
            .urlPathPrefix("")
            .hostHeader(null)
            .proxyHost(proxyServer.getListenAddress().getHostString())
            .proxyPort(proxyServer.getListenAddress().getPort())
            .build();

    wireMock.register(
        get(urlEqualTo("/my/new/resource"))
            .willReturn(
                aResponse().withBody("{\"address\":\"Puerto Banús, Málaga\"}").withStatus(200)));

    assertThat(
        testClient.get("/my/new/resource").content(), is("{\"address\":\"Puerto Banús, Málaga\"}"));
  }
}
