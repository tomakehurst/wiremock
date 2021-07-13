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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WireMockClientAcceptanceTest {

  private WireMockServer wireMockServer;
  private WireMockTestClient testClient;

  @Before
  public void init() {
    wireMockServer = new WireMockServer(Options.DYNAMIC_PORT);
    wireMockServer.start();
    WireMock.configureFor(wireMockServer.port());
    testClient = new WireMockTestClient(wireMockServer.port());
  }

  @After
  public void stopServer() {
    wireMockServer.stop();
  }

  @Test
  public void buildsMappingWithUrlOnlyRequestAndStatusOnlyResponse() {
    WireMock wireMock = WireMock.create().port(wireMockServer.port()).build();
    wireMock.register(get(urlEqualTo("/my/new/resource")).willReturn(aResponse().withStatus(304)));

    assertThat(testClient.get("/my/new/resource").statusCode(), is(304));
  }

  @Test
  public void buildsMappingFromStaticSyntax() {
    givenThat(get(urlEqualTo("/my/new/resource")).willReturn(aResponse().withStatus(304)));

    assertThat(testClient.get("/my/new/resource").statusCode(), is(304));
  }

  @Test
  public void buildsMappingWithUrlOnyRequestAndResponseWithJsonBodyWithDiacriticSigns() {
    WireMock wireMock = WireMock.create().port(wireMockServer.port()).build();
    wireMock.register(
        get(urlEqualTo("/my/new/resource"))
            .willReturn(
                aResponse().withBody("{\"address\":\"Puerto Banús, Málaga\"}").withStatus(200)));

    assertThat(
        testClient.get("/my/new/resource").content(), is("{\"address\":\"Puerto Banús, Málaga\"}"));
  }
}
