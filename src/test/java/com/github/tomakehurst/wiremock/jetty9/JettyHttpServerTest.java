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
package com.github.tomakehurst.wiremock.jetty9;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;

import com.github.tomakehurst.wiremock.admin.AdminRoutes;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.core.StubServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilter;
import com.github.tomakehurst.wiremock.http.AdminRequestHandler;
import com.github.tomakehurst.wiremock.http.BasicResponseRenderer;
import com.github.tomakehurst.wiremock.http.ResponseRenderer;
import com.github.tomakehurst.wiremock.http.StubRequestHandler;
import com.github.tomakehurst.wiremock.security.NoAuthenticator;
import com.github.tomakehurst.wiremock.verification.RequestJournal;
import java.lang.reflect.Field;
import java.util.Collections;
import org.eclipse.jetty.server.ServerConnector;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class JettyHttpServerTest {

  private Mockery context;
  private AdminRequestHandler adminRequestHandler;
  private StubRequestHandler stubRequestHandler;
  private JettyHttpServerFactory serverFactory = new JettyHttpServerFactory();

  @Before
  public void init() {
    context = new Mockery();
    Admin admin = context.mock(Admin.class);

    adminRequestHandler =
        new AdminRequestHandler(
            AdminRoutes.defaults(),
            admin,
            new BasicResponseRenderer(),
            new NoAuthenticator(),
            false,
            Collections.<RequestFilter>emptyList());
    stubRequestHandler =
        new StubRequestHandler(
            context.mock(StubServer.class),
            context.mock(ResponseRenderer.class),
            admin,
            Collections.<String, PostServeAction>emptyMap(),
            context.mock(RequestJournal.class),
            Collections.<RequestFilter>emptyList(),
            false);
  }

  @Test
  public void testStopTimeout() {
    long expectedStopTimeout = 500L;
    WireMockConfiguration config =
        WireMockConfiguration.wireMockConfig().jettyStopTimeout(expectedStopTimeout);

    JettyHttpServer jettyHttpServer =
        (JettyHttpServer)
            serverFactory.buildHttpServer(config, adminRequestHandler, stubRequestHandler);

    assertThat(jettyHttpServer.stopTimeout(), is(expectedStopTimeout));
  }

  @Test
  public void testStopTimeoutNotSet() {
    long expectedStopTimeout = 1000L;
    WireMockConfiguration config = WireMockConfiguration.wireMockConfig();

    JettyHttpServer jettyHttpServer =
        (JettyHttpServer)
            serverFactory.buildHttpServer(config, adminRequestHandler, stubRequestHandler);

    assertThat(jettyHttpServer.stopTimeout(), is(expectedStopTimeout));
  }

  @Test
  public void testHttpConnectorIsNullWhenHttpDisabled()
      throws NoSuchFieldException, IllegalAccessException {
    WireMockConfiguration config = WireMockConfiguration.wireMockConfig().httpDisabled(true);

    JettyHttpServer jettyHttpServer =
        (JettyHttpServer)
            serverFactory.buildHttpServer(config, adminRequestHandler, stubRequestHandler);

    Field httpConnectorField = JettyHttpServer.class.getDeclaredField("httpConnector");
    httpConnectorField.setAccessible(true);
    ServerConnector httpConnector = (ServerConnector) httpConnectorField.get(jettyHttpServer);

    assertNull(httpConnector);
  }
}
