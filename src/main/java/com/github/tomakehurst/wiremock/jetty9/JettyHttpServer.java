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

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.core.WireMockApp.ADMIN_CONTEXT_ROOT;
import static java.util.concurrent.Executors.newScheduledThreadPool;

import com.github.tomakehurst.wiremock.common.*;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockApp;
import com.github.tomakehurst.wiremock.http.AdminRequestHandler;
import com.github.tomakehurst.wiremock.http.HttpServer;
import com.github.tomakehurst.wiremock.http.RequestHandler;
import com.github.tomakehurst.wiremock.http.StubRequestHandler;
import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.servlet.*;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;
import javax.servlet.DispatcherType;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.NetworkTrafficListener;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class JettyHttpServer implements HttpServer {
  private static final String FILES_URL_MATCH = String.format("/%s/*", WireMockApp.FILES_ROOT);
  private static final String[] GZIPPABLE_METHODS = new String[] {"POST", "PUT", "PATCH", "DELETE"};
  private static final int DEFAULT_ACCEPTORS = 3;

  static {
    System.setProperty("org.eclipse.jetty.server.HttpChannelState.DEFAULT_TIMEOUT", "300000");
    System.setProperty("org.eclipse.jetty.http.HttpGenerator.STRICT", "true");
  }

  private final Server jettyServer;
  private final ServerConnector httpConnector;
  private final ServerConnector httpsConnector;

  private ScheduledExecutorService scheduledExecutorService;

  public JettyHttpServer(
      Options options,
      AdminRequestHandler adminRequestHandler,
      StubRequestHandler stubRequestHandler) {
    jettyServer = createServer(options);

    NetworkTrafficListenerAdapter networkTrafficListenerAdapter =
        new NetworkTrafficListenerAdapter(options.networkTrafficListener());

    if (options.getHttpDisabled()) {
      httpConnector = null;
    } else {
      httpConnector =
          createHttpConnector(
              options.bindAddress(),
              options.portNumber(),
              options.jettySettings(),
              networkTrafficListenerAdapter);
      jettyServer.addConnector(httpConnector);
    }

    if (options.httpsSettings().enabled()) {
      httpsConnector =
          createHttpsConnector(
              jettyServer,
              options.bindAddress(),
              options.httpsSettings(),
              options.jettySettings(),
              networkTrafficListenerAdapter);
      jettyServer.addConnector(httpsConnector);
    } else {
      httpsConnector = null;
    }

    applyAdditionalServerConfiguration(jettyServer, options);

    jettyServer.setHandler(createHandler(options, adminRequestHandler, stubRequestHandler));

    finalizeSetup(options);
  }

  protected void applyAdditionalServerConfiguration(Server jettyServer, Options options) {}

  protected HandlerCollection createHandler(
      Options options,
      AdminRequestHandler adminRequestHandler,
      StubRequestHandler stubRequestHandler) {
    Notifier notifier = options.notifier();
    ServletContextHandler adminContext = addAdminContext(adminRequestHandler, notifier);
    ServletContextHandler mockServiceContext =
        addMockServiceContext(
            stubRequestHandler,
            options.filesRoot(),
            options.getAsynchronousResponseSettings(),
            options.getChunkedEncodingPolicy(),
            options.getStubCorsEnabled(),
            notifier);

    HandlerCollection handlers = new HandlerCollection();
    handlers.setHandlers(ArrayUtils.addAll(extensionHandlers(), adminContext));

    if (options.getGzipDisabled()) {
      handlers.addHandler(mockServiceContext);
    } else {
      addGZipHandler(mockServiceContext, handlers);
    }

    return handlers;
  }

  private void addGZipHandler(
      ServletContextHandler mockServiceContext, HandlerCollection handlers) {
    Class<?> gzipHandlerClass = null;

    try {
      gzipHandlerClass = Class.forName("org.eclipse.jetty.servlets.gzip.GzipHandler");
    } catch (ClassNotFoundException e) {
      try {
        gzipHandlerClass = Class.forName("org.eclipse.jetty.server.handler.gzip.GzipHandler");
      } catch (ClassNotFoundException e1) {
        throwUnchecked(e1);
      }
    }

    try {
      HandlerWrapper gzipWrapper = (HandlerWrapper) gzipHandlerClass.newInstance();
      setGZippableMethods(gzipWrapper, gzipHandlerClass);
      gzipWrapper.setHandler(mockServiceContext);
      handlers.addHandler(gzipWrapper);
    } catch (Exception e) {
      throwUnchecked(e);
    }
  }

  private static void setGZippableMethods(HandlerWrapper gzipHandler, Class<?> gzipHandlerClass) {
    try {
      Method addIncludedMethods =
          gzipHandlerClass.getDeclaredMethod("addIncludedMethods", String[].class);
      addIncludedMethods.invoke(gzipHandler, new Object[] {GZIPPABLE_METHODS});
    } catch (Exception ignored) {
    }
  }

  protected void finalizeSetup(Options options) {
    if (!options.jettySettings().getStopTimeout().isPresent()) {
      jettyServer.setStopTimeout(1000);
    }
  }

  protected Server createServer(Options options) {
    final Server server = new Server(options.threadPoolFactory().buildThreadPool(options));
    final JettySettings jettySettings = options.jettySettings();
    final Optional<Long> stopTimeout = jettySettings.getStopTimeout();
    if (stopTimeout.isPresent()) {
      server.setStopTimeout(stopTimeout.get());
    }
    return server;
  }

  /** Extend only this method if you want to add additional handlers to Jetty. */
  protected Handler[] extensionHandlers() {
    return new Handler[] {};
  }

  @Override
  public void start() {
    try {
      jettyServer.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    long timeout = System.currentTimeMillis() + 30000;
    while (!jettyServer.isStarted()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // no-op
      }
      if (System.currentTimeMillis() > timeout) {
        throw new RuntimeException("Server took too long to start up.");
      }
    }
  }

  @Override
  public void stop() {
    try {
      if (scheduledExecutorService != null) {
        scheduledExecutorService.shutdown();
      }

      jettyServer.stop();
      jettyServer.join();
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
    return httpConnector.getLocalPort();
  }

  @Override
  public int httpsPort() {
    return httpsConnector.getLocalPort();
  }

  public long stopTimeout() {
    return jettyServer.getStopTimeout();
  }

  protected ServerConnector createHttpConnector(
      String bindAddress, int port, JettySettings jettySettings, NetworkTrafficListener listener) {

    HttpConfiguration httpConfig = createHttpConfig(jettySettings);

    ServerConnector connector =
        createServerConnector(
            bindAddress, jettySettings, port, listener, new HttpConnectionFactory(httpConfig));

    return connector;
  }

  protected ServerConnector createHttpsConnector(
      Server server,
      String bindAddress,
      HttpsSettings httpsSettings,
      JettySettings jettySettings,
      NetworkTrafficListener listener) {

    // Added to support Android https communication.
    SslContextFactory sslContextFactory = buildSslContextFactory();

    sslContextFactory.setKeyStorePath(httpsSettings.keyStorePath());
    sslContextFactory.setKeyStorePassword(httpsSettings.keyStorePassword());
    sslContextFactory.setKeyManagerPassword(httpsSettings.keyManagerPassword());
    sslContextFactory.setKeyStoreType(httpsSettings.keyStoreType());
    if (httpsSettings.hasTrustStore()) {
      sslContextFactory.setTrustStorePath(httpsSettings.trustStorePath());
      sslContextFactory.setTrustStorePassword(httpsSettings.trustStorePassword());
    }
    sslContextFactory.setNeedClientAuth(httpsSettings.needClientAuth());

    HttpConfiguration httpConfig = createHttpConfig(jettySettings);
    httpConfig.addCustomizer(new SecureRequestCustomizer());

    final int port = httpsSettings.port();

    HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfig);
    SslConnectionFactory sslConnectionFactory =
        new SslConnectionFactory(sslContextFactory, "http/1.1");
    ConnectionFactory[] connectionFactories =
        ArrayUtils.addAll(
            new ConnectionFactory[] {sslConnectionFactory, httpConnectionFactory},
            buildAdditionalConnectionFactories(
                httpsSettings, httpConnectionFactory, sslConnectionFactory));

    return createServerConnector(bindAddress, jettySettings, port, listener, connectionFactories);
  }

  protected ConnectionFactory[] buildAdditionalConnectionFactories(
      HttpsSettings httpsSettings,
      HttpConnectionFactory httpConnectionFactory,
      SslConnectionFactory sslConnectionFactory) {
    return new ConnectionFactory[] {};
  }

  // Override this for platform-specific impls
  protected SslContextFactory buildSslContextFactory() {
    return new SslContextFactory();
  }

  protected HttpConfiguration createHttpConfig(JettySettings jettySettings) {
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setRequestHeaderSize(jettySettings.getRequestHeaderSize().or(8192));
    httpConfig.setSendDateHeader(false);
    return httpConfig;
  }

  protected ServerConnector createServerConnector(
      String bindAddress,
      JettySettings jettySettings,
      int port,
      NetworkTrafficListener listener,
      ConnectionFactory... connectionFactories) {

    int acceptors = jettySettings.getAcceptors().or(DEFAULT_ACCEPTORS);
    NetworkTrafficServerConnector connector =
        new NetworkTrafficServerConnector(
            jettyServer, null, null, null, acceptors, 2, connectionFactories);

    connector.setPort(port);
    connector.addNetworkTrafficListener(listener);
    setJettySettings(jettySettings, connector);
    connector.setHost(bindAddress);
    return connector;
  }

  private void setJettySettings(JettySettings jettySettings, ServerConnector connector) {
    if (jettySettings.getAcceptQueueSize().isPresent()) {
      connector.setAcceptQueueSize(jettySettings.getAcceptQueueSize().get());
    }

    if (jettySettings.getIdleTimeout().isPresent()) {
      connector.setIdleTimeout(jettySettings.getIdleTimeout().get());
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private ServletContextHandler addMockServiceContext(
      StubRequestHandler stubRequestHandler,
      FileSource fileSource,
      AsynchronousResponseSettings asynchronousResponseSettings,
      Options.ChunkedEncodingPolicy chunkedEncodingPolicy,
      boolean stubCorsEnabled,
      Notifier notifier) {
    ServletContextHandler mockServiceContext = new ServletContextHandler(jettyServer, "/");

    mockServiceContext.setInitParameter("org.eclipse.jetty.servlet.Default.maxCacheSize", "0");
    mockServiceContext.setInitParameter(
        "org.eclipse.jetty.servlet.Default.resourceBase", fileSource.getPath());
    mockServiceContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

    mockServiceContext.addServlet(DefaultServlet.class, FILES_URL_MATCH);

    mockServiceContext.setAttribute(
        JettyFaultInjectorFactory.class.getName(), new JettyFaultInjectorFactory());
    mockServiceContext.setAttribute(StubRequestHandler.class.getName(), stubRequestHandler);
    mockServiceContext.setAttribute(Notifier.KEY, notifier);
    mockServiceContext.setAttribute(
        Options.ChunkedEncodingPolicy.class.getName(), chunkedEncodingPolicy);
    ServletHolder servletHolder =
        mockServiceContext.addServlet(WireMockHandlerDispatchingServlet.class, "/");
    servletHolder.setInitParameter(
        RequestHandler.HANDLER_CLASS_KEY, StubRequestHandler.class.getName());
    servletHolder.setInitParameter(
        FaultInjectorFactory.INJECTOR_CLASS_KEY, JettyFaultInjectorFactory.class.getName());
    servletHolder.setInitParameter(
        WireMockHandlerDispatchingServlet.SHOULD_FORWARD_TO_FILES_CONTEXT, "true");

    if (asynchronousResponseSettings.isEnabled()) {
      scheduledExecutorService = newScheduledThreadPool(asynchronousResponseSettings.getThreads());
      mockServiceContext.setAttribute(
          WireMockHandlerDispatchingServlet.ASYNCHRONOUS_RESPONSE_EXECUTOR,
          scheduledExecutorService);
    }

    mockServiceContext.setAttribute(
        MultipartRequestConfigurer.KEY, buildMultipartRequestConfigurer());

    MimeTypes mimeTypes = new MimeTypes();
    mimeTypes.addMimeMapping("json", "application/json");
    mimeTypes.addMimeMapping("html", "text/html");
    mimeTypes.addMimeMapping("xml", "application/xml");
    mimeTypes.addMimeMapping("txt", "text/plain");
    mockServiceContext.setMimeTypes(mimeTypes);
    mockServiceContext.setWelcomeFiles(
        new String[] {"index.json", "index.html", "index.xml", "index.txt"});

    NotFoundHandler errorHandler = new NotFoundHandler(mockServiceContext);
    mockServiceContext.setErrorHandler(errorHandler);

    mockServiceContext.addFilter(
        ContentTypeSettingFilter.class, FILES_URL_MATCH, EnumSet.of(DispatcherType.FORWARD));
    mockServiceContext.addFilter(
        TrailingSlashFilter.class, FILES_URL_MATCH, EnumSet.allOf(DispatcherType.class));

    if (stubCorsEnabled) {
      addCorsFilter(mockServiceContext);
    }

    return mockServiceContext;
  }

  private ServletContextHandler addAdminContext(
      AdminRequestHandler adminRequestHandler, Notifier notifier) {
    ServletContextHandler adminContext = new ServletContextHandler(jettyServer, ADMIN_CONTEXT_ROOT);

    adminContext.setInitParameter("org.eclipse.jetty.servlet.Default.maxCacheSize", "0");

    String javaVendor = System.getProperty("java.vendor");
    if (javaVendor != null && javaVendor.toLowerCase().contains("android")) {
      // Special case for Android, fixes IllegalArgumentException("resource assets not found."):
      //  The Android ClassLoader apparently does not resolve directories.
      //  Furthermore, lib assets will be merged into a single asset directory when a jar file is
      // assimilated into an apk.
      //  As resources can be addressed like "assets/swagger-ui/index.html", a static path element
      // will suffice.
      adminContext.setInitParameter("org.eclipse.jetty.servlet.Default.resourceBase", "assets");
    } else {
      adminContext.setInitParameter(
          "org.eclipse.jetty.servlet.Default.resourceBase",
          Resources.getResource("assets").toString());
    }

    Resources.getResource("assets/swagger-ui/index.html");

    adminContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
    ServletHolder swaggerUiServletHolder =
        adminContext.addServlet(DefaultServlet.class, "/swagger-ui/*");
    swaggerUiServletHolder.setAsyncSupported(false);
    adminContext.addServlet(DefaultServlet.class, "/recorder/*");

    ServletHolder servletHolder =
        adminContext.addServlet(WireMockHandlerDispatchingServlet.class, "/");
    servletHolder.setInitParameter(
        RequestHandler.HANDLER_CLASS_KEY, AdminRequestHandler.class.getName());
    adminContext.setAttribute(AdminRequestHandler.class.getName(), adminRequestHandler);
    adminContext.setAttribute(Notifier.KEY, notifier);

    adminContext.setAttribute(MultipartRequestConfigurer.KEY, buildMultipartRequestConfigurer());

    addCorsFilter(adminContext);

    return adminContext;
  }

  private void addCorsFilter(ServletContextHandler context) {
    context.addFilter(buildCorsFilter(), "/*", EnumSet.of(DispatcherType.REQUEST));
  }

  private FilterHolder buildCorsFilter() {
    FilterHolder filterHolder = new FilterHolder(CrossOriginFilter.class);
    filterHolder.setInitParameters(
        ImmutableMap.of(
            "chainPreflight", "false",
            "allowedOrigins", "*",
            "allowedHeaders", "*",
            "allowedMethods", "OPTIONS,GET,POST,PUT,PATCH,DELETE"));
    return filterHolder;
  }

  // Override this for platform-specific impls
  protected MultipartRequestConfigurer buildMultipartRequestConfigurer() {
    return new DefaultMultipartRequestConfigurer();
  }

  private static class NetworkTrafficListenerAdapter implements NetworkTrafficListener {
    private final WiremockNetworkTrafficListener wiremockNetworkTrafficListener;

    NetworkTrafficListenerAdapter(WiremockNetworkTrafficListener wiremockNetworkTrafficListener) {
      this.wiremockNetworkTrafficListener = wiremockNetworkTrafficListener;
    }

    @Override
    public void opened(Socket socket) {
      wiremockNetworkTrafficListener.opened(socket);
    }

    @Override
    public void incoming(Socket socket, ByteBuffer bytes) {
      wiremockNetworkTrafficListener.incoming(socket, bytes);
    }

    @Override
    public void outgoing(Socket socket, ByteBuffer bytes) {
      wiremockNetworkTrafficListener.outgoing(socket, bytes);
    }

    @Override
    public void closed(Socket socket) {
      wiremockNetworkTrafficListener.closed(socket);
    }
  }
}
