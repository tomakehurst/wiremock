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
package com.github.tomakehurst.wiremock.servlet;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.core.WireMockApp.ADMIN_CONTEXT_ROOT;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class AltHttpServerFactory implements HttpServerFactory {

  @Override
  public HttpServer buildHttpServer(
      Options options,
      AdminRequestHandler adminRequestHandler,
      StubRequestHandler stubRequestHandler) {

    final Server jettyServer = new Server(0);
    ConsoleNotifier notifier = new ConsoleNotifier(false);
    ServletContextHandler adminContext =
        addAdminContext(jettyServer, adminRequestHandler, notifier);
    ServletContextHandler mockServiceContext =
        addMockServiceContext(jettyServer, stubRequestHandler, options.filesRoot(), notifier);

    HandlerCollection handlers = new HandlerCollection();
    handlers.setHandlers(new Handler[] {adminContext, mockServiceContext});
    jettyServer.setHandler(handlers);

    return new HttpServer() {

      @Override
      public void start() {
        try {
          jettyServer.start();
        } catch (Exception e) {
          throwUnchecked(e);
        }
      }

      @Override
      public void stop() {
        try {
          jettyServer.stop();
        } catch (Exception e) {
          throwUnchecked(e);
        }
      }

      @Override
      public boolean isRunning() {
        return jettyServer.isRunning();
      }

      @Override
      public int port() {
        return ((ServerConnector) jettyServer.getConnectors()[0]).getLocalPort();
      }

      @Override
      public int httpsPort() {
        return 0;
      }
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private ServletContextHandler addMockServiceContext(
      Server jettyServer,
      StubRequestHandler stubRequestHandler,
      FileSource fileSource,
      Notifier notifier) {
    ServletContextHandler mockServiceContext = new ServletContextHandler(jettyServer, "/");

    mockServiceContext.setAttribute(StubRequestHandler.class.getName(), stubRequestHandler);
    mockServiceContext.setAttribute(Notifier.KEY, notifier);
    ServletHolder servletHolder =
        mockServiceContext.addServlet(WireMockHandlerDispatchingServlet.class, "/");
    servletHolder.setInitParameter(
        RequestHandler.HANDLER_CLASS_KEY, StubRequestHandler.class.getName());
    servletHolder.setInitParameter(
        WireMockHandlerDispatchingServlet.SHOULD_FORWARD_TO_FILES_CONTEXT, "false");

    return mockServiceContext;
  }

  private ServletContextHandler addAdminContext(
      Server jettyServer, AdminRequestHandler adminRequestHandler, Notifier notifier) {
    ServletContextHandler adminContext = new ServletContextHandler(jettyServer, ADMIN_CONTEXT_ROOT);
    ServletHolder servletHolder =
        adminContext.addServlet(WireMockHandlerDispatchingServlet.class, "/");
    servletHolder.setInitParameter(
        RequestHandler.HANDLER_CLASS_KEY, AdminRequestHandler.class.getName());
    adminContext.setAttribute(AdminRequestHandler.class.getName(), adminRequestHandler);
    adminContext.setAttribute(Notifier.KEY, notifier);
    return adminContext;
  }
}
