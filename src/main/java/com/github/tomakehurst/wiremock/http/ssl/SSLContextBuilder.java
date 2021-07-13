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

import static com.github.tomakehurst.wiremock.common.ArrayFunctions.concat;
import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.common.ListFunctions.splitByType;
import static java.util.Collections.addAll;

import com.github.tomakehurst.wiremock.common.Pair;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

public class SSLContextBuilder {

  private final Set<KeyManager> keyManagers = new LinkedHashSet<>();
  private final Set<TrustManager> trustManagers = new LinkedHashSet<>();

  public static SSLContextBuilder create() {
    return new SSLContextBuilder();
  }

  public SSLContextBuilder loadTrustMaterial(final KeyStore truststore)
      throws KeyStoreException, NoSuchAlgorithmException {
    return loadTrustMaterial(truststore, null);
  }

  public SSLContextBuilder loadTrustMaterial(
      final KeyStore truststore, final TrustStrategy trustStrategy)
      throws NoSuchAlgorithmException, KeyStoreException {

    String algorithm = TrustManagerFactory.getDefaultAlgorithm();
    TrustManager[] tms = loadTrustManagers(truststore, algorithm);
    TrustManager[] allTms = concat(tms, loadDefaultTrustManagers());

    Pair<List<TrustManager>, List<X509ExtendedTrustManager>> split =
        splitByType(allTms, X509ExtendedTrustManager.class);
    List<TrustManager> otherTms = split.a;
    List<X509ExtendedTrustManager> x509Tms = split.b;
    if (!x509Tms.isEmpty()) {
      CompositeTrustManager trustManager = new CompositeTrustManager(x509Tms);
      TrustManager tm =
          trustStrategy == null ? trustManager : addStrategy(trustManager, trustStrategy);
      this.trustManagers.add(tm);
    }
    this.trustManagers.addAll(otherTms);
    return this;
  }

  public SSLContextBuilder loadTrustMaterial(final TrustStrategy trustStrategy) {

    TrustManager[] tms = loadDefaultTrustManagers();
    TrustManager[] tmsWithStrategy = addStrategy(tms, trustStrategy);

    addAll(this.trustManagers, tmsWithStrategy);
    return this;
  }

  private TrustManager[] loadTrustManagers(KeyStore truststore, String algorithm)
      throws NoSuchAlgorithmException, KeyStoreException {
    final TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(algorithm);
    tmfactory.init(truststore);
    TrustManager[] tms = tmfactory.getTrustManagers();
    return tms == null ? new TrustManager[0] : tms;
  }

  private TrustManager[] loadDefaultTrustManagers() {
    try {
      return loadTrustManagers(null, TrustManagerFactory.getDefaultAlgorithm());
    } catch (NoSuchAlgorithmException | KeyStoreException e) {
      return throwUnchecked(e, null);
    }
  }

  private TrustManager[] addStrategy(TrustManager[] allTms, TrustStrategy trustStrategy) {
    TrustManager[] withStrategy = new TrustManager[allTms.length];
    for (int i = 0; i < allTms.length; i++) {
      withStrategy[i] = addStrategy(allTms[i], trustStrategy);
    }
    return withStrategy;
  }

  private TrustManager addStrategy(TrustManager tm, TrustStrategy trustStrategy) {
    if (tm instanceof X509ExtendedTrustManager) {
      return new TrustManagerDelegate((X509ExtendedTrustManager) tm, trustStrategy);
    } else {
      return tm;
    }
  }

  public SSLContextBuilder loadKeyMaterial(final KeyStore keystore, final char[] keyPassword)
      throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
    final KeyManagerFactory kmfactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmfactory.init(keystore, keyPassword);
    final KeyManager[] kms = kmfactory.getKeyManagers();
    if (kms != null) {
      addAll(keyManagers, kms);
    }
    return this;
  }

  protected void initSSLContext(
      final SSLContext sslContext,
      final Collection<KeyManager> keyManagers,
      final Collection<TrustManager> trustManagers)
      throws KeyManagementException {
    sslContext.init(
        !keyManagers.isEmpty() ? keyManagers.toArray(new KeyManager[0]) : null,
        !trustManagers.isEmpty() ? trustManagers.toArray(new TrustManager[0]) : null,
        null);
  }

  public SSLContext build() throws NoSuchAlgorithmException, KeyManagementException {
    final SSLContext sslContext = SSLContext.getInstance("TLS");
    initSSLContext(sslContext, keyManagers, trustManagers);
    return sslContext;
  }

  static class TrustManagerDelegate extends X509ExtendedTrustManager {

    private final X509ExtendedTrustManager trustManager;
    private final TrustStrategy trustStrategy;

    TrustManagerDelegate(
        final X509ExtendedTrustManager trustManager, final TrustStrategy trustStrategy) {
      this.trustManager = trustManager;
      this.trustStrategy = trustStrategy;
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType)
        throws CertificateException {
      this.trustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType)
        throws CertificateException {
      if (!this.trustStrategy.isTrusted(chain, authType)) {
        this.trustManager.checkServerTrusted(chain, authType);
      }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return this.trustManager.getAcceptedIssuers();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
        throws CertificateException {
      trustManager.checkClientTrusted(chain, authType, socket);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
        throws CertificateException {
      if (!this.trustStrategy.isTrusted(chain, authType, socket)) {
        trustManager.checkServerTrusted(chain, authType, socket);
      }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
        throws CertificateException {
      trustManager.checkClientTrusted(chain, authType, engine);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
        throws CertificateException {
      if (!this.trustStrategy.isTrusted(chain, authType, engine)) {
        trustManager.checkServerTrusted(chain, authType, engine);
      }
    }
  }

  @Override
  public String toString() {
    return "[keyManagers=" + keyManagers + ", trustManagers=" + trustManagers + "]";
  }
}
