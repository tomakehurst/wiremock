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
package com.github.tomakehurst.wiremock.jetty94;

import static com.github.tomakehurst.wiremock.jetty94.SslContexts.buildManInTheMiddleSslContextFactory;

import com.github.tomakehurst.wiremock.common.HttpsSettings;
import com.github.tomakehurst.wiremock.common.JettySettings;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.AdminRequestHandler;
import com.github.tomakehurst.wiremock.http.StubRequestHandler;
import com.github.tomakehurst.wiremock.jetty9.DefaultMultipartRequestConfigurer;
import com.github.tomakehurst.wiremock.jetty9.JettyHttpServer;
import com.github.tomakehurst.wiremock.servlet.MultipartRequestConfigurer;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.io.NetworkTrafficListener;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class Jetty94HttpServer extends JettyHttpServer {

  private ServerConnector mitmProxyConnector;

  public Jetty94HttpServer(
      Options options,
      AdminRequestHandler adminRequestHandler,
      StubRequestHandler stubRequestHandler) {
    super(options, adminRequestHandler, stubRequestHandler);
  }

  @Override
  protected MultipartRequestConfigurer buildMultipartRequestConfigurer() {
    return new DefaultMultipartRequestConfigurer();
  }

  @Override
  protected HttpConfiguration createHttpConfig(JettySettings jettySettings) {
    HttpConfiguration httpConfig = super.createHttpConfig(jettySettings);
    httpConfig.setSendXPoweredBy(false);
    httpConfig.setSendServerVersion(false);
    httpConfig.addCustomizer(new SecureRequestCustomizer());
    return httpConfig;
  }

  @Override
  protected ServerConnector createHttpConnector(
      String bindAddress, int port, JettySettings jettySettings, NetworkTrafficListener listener) {

    HttpConfiguration httpConfig = createHttpConfig(jettySettings);

    HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);

    return createServerConnector(
        bindAddress, jettySettings, port, listener, new HttpConnectionFactory(httpConfig), h2c);
  }

  @Override
  protected ServerConnector createHttpsConnector(
      Server server,
      String bindAddress,
      HttpsSettings httpsSettings,
      JettySettings jettySettings,
      NetworkTrafficListener listener) {
    SslContextFactory.Server http2SslContextFactory =
        SslContexts.buildHttp2SslContextFactory(httpsSettings);

    HttpConfiguration httpConfig = createHttpConfig(jettySettings);

    HttpConnectionFactory http = new HttpConnectionFactory(httpConfig);
    HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpConfig);

    ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();

    SslConnectionFactory ssl = new SslConnectionFactory(http2SslContextFactory, alpn.getProtocol());

    ConnectionFactory[] connectionFactories = {ssl, alpn, h2, http};

    return createServerConnector(
        bindAddress, jettySettings, httpsSettings.port(), listener, connectionFactories);
  }

  @Override
  protected HandlerCollection createHandler(
      Options options,
      AdminRequestHandler adminRequestHandler,
      StubRequestHandler stubRequestHandler) {
    HandlerCollection handler =
        super.createHandler(options, adminRequestHandler, stubRequestHandler);

    if (options.browserProxySettings().enabled()) {
      handler.addHandler(new ManInTheMiddleSslConnectHandler(mitmProxyConnector));
    }

    return handler;
  }

  @Override
  protected void applyAdditionalServerConfiguration(Server jettyServer, Options options) {
    if (options.browserProxySettings().enabled()) {
      final SslConnectionFactory ssl =
          new SslConnectionFactory(
              buildManInTheMiddleSslContextFactory(
                  options.httpsSettings(), options.browserProxySettings(), options.notifier()),
              /*
              If the proxy CONNECT request is made over HTTPS, and the
              actual content request is made using HTTP/2 tunneled over
              HTTPS, and an exception is thrown, the server blocks for 30
              seconds before flushing the response.

              To fix this, force HTTP/1.1 over TLS when tunneling HTTPS.

              This also means the HTTP mitmProxyConnector does not need the alpn &
              h2 connection factories as it will not use them.

              Unfortunately it has proven too hard to write a test to
              demonstrate the bug; it requires an HTTP client capable of
              doing ALPN & HTTP/2, which will only offer HTTP/1.1 in the
              ALPN negotiation when using HTTPS for the initial CONNECT
              request but will then offer both HTTP/1.1 and HTTP/2 for the
              actual request (this is how curl 7.64.1 behaves!). Neither
              Apache HTTP 4, 5, 5 Async, OkHttp, nor the Jetty client
              could do this. It might be possible to write one using
              Netty, but it would be hard and time consuming.
               */
              HttpVersion.HTTP_1_1.asString());

      HttpConfiguration httpConfig = createHttpConfig(options.jettySettings());
      HttpConnectionFactory http = new HttpConnectionFactory(httpConfig);
      mitmProxyConnector =
          new NetworkTrafficServerConnector(jettyServer, null, null, null, 2, 2, ssl, http);

      mitmProxyConnector.setPort(0);

      jettyServer.addConnector(mitmProxyConnector);
    }
  }
}
