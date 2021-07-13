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

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import com.github.tomakehurst.wiremock.common.Notifier;
import java.net.Socket;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.net.ssl.*;

public class CertificateGeneratingX509ExtendedKeyManager extends DelegatingX509ExtendedKeyManager {

  private final DynamicKeyStore dynamicKeyStore;
  private final HostNameMatcher hostNameMatcher;
  private final OnceOnlyNotifier notifier;

  public CertificateGeneratingX509ExtendedKeyManager(
      X509ExtendedKeyManager keyManager,
      DynamicKeyStore dynamicKeyStore,
      HostNameMatcher hostNameMatcher,
      Notifier notifier) {
    super(keyManager);
    this.dynamicKeyStore = requireNonNull(dynamicKeyStore);
    this.hostNameMatcher = requireNonNull(hostNameMatcher);
    this.notifier = new OnceOnlyNotifier(notifier);
  }

  @Override
  public PrivateKey getPrivateKey(String alias) {
    PrivateKey original = super.getPrivateKey(alias);
    return original != null ? original : dynamicKeyStore.getPrivateKey(alias);
  }

  @Override
  public X509Certificate[] getCertificateChain(String alias) {
    X509Certificate[] original = super.getCertificateChain(alias);
    return original != null ? original : dynamicKeyStore.getCertificateChain(alias);
  }

  @Override
  public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
    String defaultAlias = super.chooseServerAlias(keyType, issuers, socket);
    ExtendedSSLSession handshakeSession = getHandshakeSession(socket);
    return tryToChooseServerAlias(keyType, defaultAlias, handshakeSession);
  }

  private ExtendedSSLSession getHandshakeSession(Socket socket) {
    if (socket instanceof SSLSocket) {
      SSLSocket sslSocket = (SSLSocket) socket;
      SSLSession sslSession = getHandshakeSessionIfSupported(sslSocket);
      return getHandshakeSession(sslSession);
    } else {
      return null;
    }
  }

  private SSLSession getHandshakeSessionIfSupported(SSLSocket sslSocket) {
    try {
      return sslSocket.getHandshakeSession();
    } catch (UnsupportedOperationException e) {
      notify("your SSL Provider does not support SSLSocket.getHandshakeSession()", e);
      return null;
    }
  }

  @Override
  public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
    String defaultAlias = super.chooseEngineServerAlias(keyType, issuers, engine);
    ExtendedSSLSession handshakeSession = getHandshakeSession(engine);
    return tryToChooseServerAlias(keyType, defaultAlias, handshakeSession);
  }

  private ExtendedSSLSession getHandshakeSession(SSLEngine sslEngine) {
    SSLSession sslSession = getHandshakeSessionIfSupported(sslEngine);
    return getHandshakeSession(sslSession);
  }

  private SSLSession getHandshakeSessionIfSupported(SSLEngine sslEngine) {
    try {
      return sslEngine.getHandshakeSession();
    } catch (UnsupportedOperationException | NullPointerException e) {
      notify("your SSL Provider does not support SSLEngine.getHandshakeSession()", e);
      return null;
    }
  }

  private static ExtendedSSLSession getHandshakeSession(SSLSession handshakeSession) {
    if (handshakeSession instanceof ExtendedSSLSession) {
      return (ExtendedSSLSession) handshakeSession;
    } else {
      return null;
    }
  }

  /**
   * @param keyType non null, may be invalid
   * @param defaultAlias nullable
   * @param handshakeSession nullable
   */
  private String tryToChooseServerAlias(
      String keyType, String defaultAlias, ExtendedSSLSession handshakeSession) {
    if (defaultAlias != null && handshakeSession != null) {
      return chooseServerAlias(keyType, defaultAlias, handshakeSession);
    } else {
      return defaultAlias;
    }
  }

  /**
   * @param keyType non null, guaranteed to be valid
   * @param defaultAlias non null, guaranteed to match a private key entry
   * @param handshakeSession non null
   */
  private String chooseServerAlias(
      String keyType, String defaultAlias, ExtendedSSLSession handshakeSession) {
    List<SNIHostName> requestedServerNames = getSNIHostNames(handshakeSession);
    if (requestedServerNames.isEmpty()) {
      return defaultAlias;
    } else {
      return chooseServerAlias(keyType, defaultAlias, requestedServerNames);
    }
  }

  private List<SNIHostName> getSNIHostNames(ExtendedSSLSession handshakeSession) {
    List<SNIServerName> requestedServerNames = getRequestedServerNames(handshakeSession);
    return requestedServerNames.stream()
        .filter(SNIHostName.class::isInstance)
        .map(SNIHostName.class::cast)
        .collect(Collectors.toList());
  }

  private List<SNIServerName> getRequestedServerNames(ExtendedSSLSession handshakeSession) {
    try {
      return handshakeSession.getRequestedServerNames();
    } catch (UnsupportedOperationException e) {
      notify("your SSL Provider does not support ExtendedSSLSession.getRequestedServerNames()", e);
      return emptyList();
    }
  }

  /**
   * @param keyType non null, guaranteed to be valid
   * @param defaultAlias non null, guaranteed to match a private key entry
   * @param requestedServerNames non null, non empty
   */
  private String chooseServerAlias(
      String keyType, String defaultAlias, List<SNIHostName> requestedServerNames) {
    X509Certificate[] certificateChain = super.getCertificateChain(defaultAlias);
    if (certificateChain != null && matches(certificateChain[0], requestedServerNames)) {
      return defaultAlias;
    } else {
      try {
        SNIHostName requestedServerName = requestedServerNames.get(0);
        dynamicKeyStore.generateCertificateIfNecessary(keyType, requestedServerName);
        return requestedServerName.getAsciiName();
      } catch (KeyStoreException | CertificateGenerationUnsupportedException e) {
        notify(
            "certificates cannot be generated; perhaps the sun internal classes are not available?",
            e);
        return defaultAlias;
      }
    }
  }

  private boolean matches(X509Certificate x509Certificate, List<SNIHostName> requestedServerNames) {
    return requestedServerNames.stream()
        .anyMatch(sniHostName -> hostNameMatcher.matches(x509Certificate, sniHostName));
  }

  private void notify(String reason, Exception e) {
    notifier.error(
        "Dynamic certificate generation is not supported because "
            + reason
            + lineSeparator()
            + "All sites will be served using the normal WireMock HTTPS certificate.",
        e);
  }

  private static class OnceOnlyNotifier implements Notifier {

    private final Notifier notifier;
    private final OnceOnly onceOnly = new OnceOnly();

    private OnceOnlyNotifier(Notifier notifier) {
      this.notifier = notifier;
    }

    @Override
    public void info(String message) {
      if (onceOnly.unused()) {
        notifier.info(message);
      }
    }

    @Override
    public void error(String message) {
      if (onceOnly.unused()) {
        notifier.error(message);
      }
    }

    @Override
    public void error(String message, Throwable t) {
      if (onceOnly.unused()) {
        notifier.error(message, t);
      }
    }
  }

  private static class OnceOnly {
    private final AtomicBoolean used = new AtomicBoolean(false);

    boolean unused() {
      return used.compareAndSet(false, true);
    }
  }
}
