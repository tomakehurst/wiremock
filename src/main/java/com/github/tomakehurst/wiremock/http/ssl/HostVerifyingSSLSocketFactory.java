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
package com.github.tomakehurst.wiremock.http.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class HostVerifyingSSLSocketFactory extends SSLSocketFactory {

  private final SSLSocketFactory delegate;

  public HostVerifyingSSLSocketFactory(SSLSocketFactory delegate) {
    this.delegate = delegate;
  }

  public String[] getDefaultCipherSuites() {
    return delegate.getDefaultCipherSuites();
  }

  public String[] getSupportedCipherSuites() {
    return delegate.getSupportedCipherSuites();
  }

  public Socket createSocket(Socket s, String host, int port, boolean autoClose)
      throws IOException {
    return verifyHosts(delegate.createSocket(s, host, port, autoClose));
  }

  public Socket createSocket() throws IOException {
    return verifyHosts(delegate.createSocket());
  }

  public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
    return verifyHosts(delegate.createSocket(host, port));
  }

  public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
      throws IOException, UnknownHostException {
    return verifyHosts(delegate.createSocket(host, port, localHost, localPort));
  }

  public Socket createSocket(InetAddress host, int port) throws IOException {
    return verifyHosts(delegate.createSocket(host, port));
  }

  public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
      throws IOException {
    return verifyHosts(delegate.createSocket(address, port, localAddress, localPort));
  }

  public static Socket verifyHosts(Socket socket) {
    if (socket instanceof SSLSocket) {
      SSLSocket sslSocket = (SSLSocket) socket;
      SSLParameters sslParameters = sslSocket.getSSLParameters();
      sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
      sslSocket.setSSLParameters(sslParameters);
    }
    return socket;
  }
}
