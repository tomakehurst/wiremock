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

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Implementation of {@link X509ExtendedTrustManager} that delegates to multiple nested
 * X509ExtendedTrustManagers.
 *
 * <p>{@link javax.net.ssl.SSLContext#init(javax.net.ssl.KeyManager[], javax.net.ssl.TrustManager[],
 * java.security.SecureRandom)} accepts an array of {@link javax.net.ssl.TrustManager} instances,
 * but {@link sun.security.ssl.SSLContextImpl#chooseTrustManager(javax.net.ssl.TrustManager[])}
 * chooses the first instance of X509TrustManager in the array. So in order to provide a composite
 * trust manager that will trust based on the decision of more than one X509TrustManager we need to
 * create a new implementation that delegates its decision to one or more real X509TrustManager
 * instances.
 *
 * <p>The contract of this class is that a check will pass if it passes against any of its trust
 * managers. If it passes against none of them, the {@link CertificateException} thrown by the last
 * of them is propagated.
 */
class CompositeTrustManager extends X509ExtendedTrustManager {

  private final List<X509ExtendedTrustManager> trustManagers;
  private final X509Certificate[] acceptedIssuers;

  CompositeTrustManager(List<X509ExtendedTrustManager> trustManagers) {
    if (trustManagers.isEmpty()) {
      throw new IllegalArgumentException("A trust manager must be provided");
    }
    this.trustManagers = new ArrayList<>(trustManagers);
    this.acceptedIssuers = loadAcceptedIssuers(this.trustManagers);
  }

  private X509Certificate[] loadAcceptedIssuers(List<X509ExtendedTrustManager> trustManagers) {
    List<X509Certificate> result = new ArrayList<>();
    for (X509TrustManager trustManager : trustManagers) {
      result.addAll(asList(trustManager.getAcceptedIssuers()));
    }
    return result.toArray(new X509Certificate[0]);
  }

  @Override
  public void checkClientTrusted(final X509Certificate[] chain, final String authType)
      throws CertificateException {
    checkAllTrustManagers(
        new CertificateChecker() {
          @Override
          public void check(X509ExtendedTrustManager tm) throws CertificateException {
            tm.checkClientTrusted(chain, authType);
          }
        });
  }

  @Override
  public void checkServerTrusted(final X509Certificate[] chain, final String authType)
      throws CertificateException {
    checkAllTrustManagers(
        new CertificateChecker() {
          @Override
          public void check(X509ExtendedTrustManager tm) throws CertificateException {
            tm.checkServerTrusted(chain, authType);
          }
        });
  }

  @Override
  public void checkClientTrusted(
      final X509Certificate[] chain, final String authType, final Socket socket)
      throws CertificateException {
    checkAllTrustManagers(
        new CertificateChecker() {
          @Override
          public void check(X509ExtendedTrustManager tm) throws CertificateException {
            tm.checkClientTrusted(chain, authType, socket);
          }
        });
  }

  @Override
  public void checkServerTrusted(
      final X509Certificate[] chain, final String authType, final Socket socket)
      throws CertificateException {
    checkAllTrustManagers(
        new CertificateChecker() {
          @Override
          public void check(X509ExtendedTrustManager tm) throws CertificateException {
            tm.checkServerTrusted(chain, authType, socket);
          }
        });
  }

  @Override
  public void checkClientTrusted(
      final X509Certificate[] chain, final String authType, final SSLEngine engine)
      throws CertificateException {
    checkAllTrustManagers(
        new CertificateChecker() {
          @Override
          public void check(X509ExtendedTrustManager tm) throws CertificateException {
            tm.checkClientTrusted(chain, authType, engine);
          }
        });
  }

  @Override
  public void checkServerTrusted(
      final X509Certificate[] chain, final String authType, final SSLEngine engine)
      throws CertificateException {
    checkAllTrustManagers(
        new CertificateChecker() {
          @Override
          public void check(X509ExtendedTrustManager tm) throws CertificateException {
            tm.checkServerTrusted(chain, authType, engine);
          }
        });
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return copyOf(acceptedIssuers, acceptedIssuers.length);
  }

  private void checkAllTrustManagers(CertificateChecker certificateChecker)
      throws CertificateException {
    for (Iterator<X509ExtendedTrustManager> iterator = trustManagers.iterator();
        iterator.hasNext(); ) {
      X509ExtendedTrustManager tm = iterator.next();
      try {
        certificateChecker.check(tm);
        break;
      } catch (CertificateException e) {
        if (!iterator.hasNext()) {
          throw e;
        }
      }
    }
  }

  private interface CertificateChecker {
    void check(X509ExtendedTrustManager tm) throws CertificateException;
  }
}
