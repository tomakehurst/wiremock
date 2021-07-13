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
import static com.github.tomakehurst.wiremock.testsupport.TestFiles.sampleWarRootDir;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.testsupport.Network;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Test;

/**
 * Tests that check if init parameters for servlets and the context of wiremock are passed on
 * correctly
 */
public class WarDeploymentParameterAcceptanceTest {
  private Server jetty;
  private WireMockTestClient testClient;

  @After
  public void cleanup() throws Exception {
    jetty.stop();
    WireMock.configure();
  }

  @Test
  public void testCustomMapping() throws Exception {
    // Test war deployment using a different servlet mapping path (see webappCustomMapping).
    init(sampleWarRootDir() + "/src/main/webappCustomMapping", "/mapping");
    givenThat(
        get(urlEqualTo("/war/stub"))
            .willReturn(aResponse().withStatus(HTTP_OK).withBody("War stub OK")));

    assertThat(testClient.get("/wiremock/mapping/war/stub").content(), is("War stub OK"));
  }

  @Test
  public void testLimitedRequestJournal() throws Exception {
    // Test war deployment usint a request journal restricted to two entries
    init(sampleWarRootDir() + "/src/main/webappLimitedRequestJournal", "");
    // We don't have to create a stub since failed requests are also recorded
    testClient.get("/wiremock/request1");
    testClient.get("/wiremock/request2");
    testClient.get("/wiremock/request3");

    // Only two requests are present since the oldest one is discarded
    verify(0, getRequestedFor(urlEqualTo("/request1")));
    verify(1, getRequestedFor(urlEqualTo("/request2")));
    verify(1, getRequestedFor(urlEqualTo("/request3")));
  }

  /**
   * Start jetty and wiremock. This is not an @Before method since we need to pass a parameter
   *
   * @param webInfPath Path where the WEB-INF directory for jetty resides
   * @param mappingPath Path where wiremock is mapped
   */
  private void init(String webInfPath, String mappingPath) throws Exception {
    int port = Network.findFreePort();
    jetty = new Server(port);
    WebAppContext context = new WebAppContext(webInfPath, "/wiremock");
    jetty.setHandler(context);
    jetty.start();

    WireMock.configureFor("localhost", port, "/wiremock" + mappingPath);
    testClient = new WireMockTestClient(port);
  }
}
