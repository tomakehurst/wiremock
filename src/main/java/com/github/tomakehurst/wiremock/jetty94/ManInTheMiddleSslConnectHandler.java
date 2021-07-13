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

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Promise;

public class ManInTheMiddleSslConnectHandler extends ConnectHandler {

  private final ServerConnector mitmProxyConnector;

  public ManInTheMiddleSslConnectHandler(ServerConnector mitmProxyConnector) {
    this.mitmProxyConnector = mitmProxyConnector;
  }

  @Override
  protected void connectToServer(
      HttpServletRequest request,
      String ignoredHost,
      int ignoredPort,
      Promise<SocketChannel> promise) {
    SocketChannel channel = null;
    try {
      channel = SocketChannel.open();
      channel.socket().setTcpNoDelay(true);
      channel.configureBlocking(false);

      String host = firstNonNull(mitmProxyConnector.getHost(), "localhost");
      int port = mitmProxyConnector.getLocalPort();
      InetSocketAddress address = newConnectAddress(host, port);

      channel.connect(address);
      promise.succeeded(channel);
    } catch (Throwable x) {
      close(channel);
      promise.failed(x);
    }
  }

  private void close(Closeable closeable) {
    try {
      if (closeable != null) closeable.close();
    } catch (Throwable x) {
      LOG.ignore(x);
    }
  }
}
