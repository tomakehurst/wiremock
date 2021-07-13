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
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509ExtendedTrustManager;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

public class CompositeTrustManagerTest {

  private final X509ExtendedTrustManager trustManager1 = mockX509ExtendedTrustManager();
  private final X509ExtendedTrustManager trustManager2 = mockX509ExtendedTrustManager();
  private final X509Certificate[] chain = new X509Certificate[0];
  private final String authType = "AN_AUTH_TYPE";

  @Test
  public void checkServerTrustedPassesForSingleTrustManager() throws CertificateException {

    CompositeTrustManager compositeTrustManager =
        new CompositeTrustManager(singletonList(trustManager1));

    compositeTrustManager.checkServerTrusted(chain, authType);
  }

  @Test
  public void checkServerTrustedFailsForSingleTrustManager() throws CertificateException {

    final CertificateException invalidCertForTrustManager1 =
        new CertificateException("Invalid cert for trustManager1");

    willThrow(invalidCertForTrustManager1).given(trustManager1).checkServerTrusted(chain, authType);

    final CompositeTrustManager compositeTrustManager =
        new CompositeTrustManager(singletonList(trustManager1));

    CertificateException thrown =
        assertThrows(
            CertificateException.class,
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                compositeTrustManager.checkServerTrusted(chain, authType);
              }
            });
    assertEquals(invalidCertForTrustManager1, thrown);
  }

  @Test
  public void checkServerTrustedIfBothWouldPass() throws CertificateException {

    CompositeTrustManager compositeTrustManager =
        new CompositeTrustManager(asList(trustManager1, trustManager2));

    compositeTrustManager.checkServerTrusted(chain, authType);
  }

  @Test
  public void checkServerTrustedIfFirstWouldPass() throws CertificateException {

    willThrow(new CertificateException("Invalid cert for trustManager2"))
        .given(trustManager2)
        .checkServerTrusted(chain, authType);

    CompositeTrustManager compositeTrustManager =
        new CompositeTrustManager(asList(trustManager1, trustManager2));

    compositeTrustManager.checkServerTrusted(chain, authType);
  }

  @Test
  public void checkServerTrustedIfSecondWouldPass() throws CertificateException {

    willThrow(new CertificateException("Invalid cert for trustManager1"))
        .given(trustManager1)
        .checkServerTrusted(chain, authType);

    CompositeTrustManager compositeTrustManager =
        new CompositeTrustManager(asList(trustManager1, trustManager2));

    compositeTrustManager.checkServerTrusted(chain, authType);
  }

  @Test
  public void checkServerNotTrustedIfNeitherPass() throws CertificateException {

    final CertificateException invalidCertForTrustManager2 =
        new CertificateException("Invalid cert for trustManager2");

    willThrow(new CertificateException("Invalid cert for trustManager1"))
        .given(trustManager1)
        .checkServerTrusted(chain, authType);
    willThrow(invalidCertForTrustManager2).given(trustManager2).checkServerTrusted(chain, authType);

    final CompositeTrustManager compositeTrustManager =
        new CompositeTrustManager(asList(trustManager1, trustManager2));

    CertificateException thrown =
        assertThrows(
            CertificateException.class,
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                compositeTrustManager.checkServerTrusted(chain, authType);
              }
            });

    assertEquals(invalidCertForTrustManager2, thrown);
  }

  @Test
  public void returnAllAcceptedIssuers() {

    final X509Certificate cert1 = mock(X509Certificate.class, "cert1");
    final X509Certificate cert2 = mock(X509Certificate.class, "cert2");
    final X509Certificate cert3 = mock(X509Certificate.class, "cert3");
    final X509Certificate cert4 = mock(X509Certificate.class, "cert4");

    given(trustManager1.getAcceptedIssuers()).willReturn(new X509Certificate[] {cert1, cert2});
    given(trustManager2.getAcceptedIssuers()).willReturn(new X509Certificate[] {cert3, cert4});

    final CompositeTrustManager compositeTrustManager =
        new CompositeTrustManager(asList(trustManager1, trustManager2));

    X509Certificate[] acceptedIssuers = compositeTrustManager.getAcceptedIssuers();

    assertArrayEquals(new X509Certificate[] {cert1, cert2, cert3, cert4}, acceptedIssuers);

    acceptedIssuers[2] = mock(X509Certificate.class, "cert5");

    assertArrayEquals(
        new X509Certificate[] {cert1, cert2, cert3, cert4},
        compositeTrustManager.getAcceptedIssuers());
  }

  private X509ExtendedTrustManager mockX509ExtendedTrustManager() {
    final X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);
    when(trustManager.getAcceptedIssuers()).thenReturn(new X509Certificate[0]);
    return trustManager;
  }
}
