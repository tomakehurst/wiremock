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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.testsupport.Network;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class WarDeploymentAcceptanceTest {

  @Rule public ExpectedException expectedException = ExpectedException.none();

  private Server jetty;

  private WireMockTestClient testClient;

  @Before
  public void init() throws Exception {
    String webAppRootPath = sampleWarRootDir() + "/src/main/webapp";
    WebAppContext context = new WebAppContext(webAppRootPath, "/wiremock");

    int port = attemptToStartOnRandomPort(context);

    WireMock.configureFor("localhost", port, "/wiremock");
    testClient = new WireMockTestClient(port);
  }

  private int attemptToStartOnRandomPort(WebAppContext context) throws Exception {
    int port;

    int attemptsRemaining = 3;
    while (true) {
      port = Network.findFreePort();
      jetty = new Server(port);
      jetty.setHandler(context);
      try {
        jetty.start();
        break;
      } catch (Exception e) {
        attemptsRemaining--;
        if (attemptsRemaining > 0) {
          continue;
        }

        throw e;
      }
    }
    return port;
  }

  @After
  public void cleanup() throws Exception {
    jetty.stop();
    WireMock.configure();
  }

  @Test
  public void servesBakedInStubResponse() {
    WireMockResponse response = testClient.get("/wiremock/api/mytest");
    assertThat(response.content(), containsString("YES"));
  }

  @Test
  public void acceptsAndReturnsStubMapping() {
    givenThat(
        get(urlEqualTo("/war/stub"))
            .willReturn(aResponse().withStatus(HTTP_OK).withBody("War stub OK")));

    assertThat(testClient.get("/wiremock/war/stub").content(), is("War stub OK"));
  }

  @Test
  public void tryingToShutDownGives500() {
    try {
      shutdownServer();
      fail("Expected a VerificationException");
    } catch (VerificationException e) {
      assertThat(e.getMessage(), containsString("500"));
    }
  }

  @Test
  public void tryingToSaveMappingsGives500() {
    try {
      saveAllMappings();
      fail("Expected a VerificationException");
    } catch (VerificationException e) {
      assertThat(e.getMessage(), containsString("500"));
    }
  }
}
