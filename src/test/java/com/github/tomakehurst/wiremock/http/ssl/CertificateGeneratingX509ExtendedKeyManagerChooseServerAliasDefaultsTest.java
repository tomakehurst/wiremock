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
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.testsupport.TestNotifier;
import java.net.Socket;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Collections;
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import org.junit.Test;

public class CertificateGeneratingX509ExtendedKeyManagerChooseServerAliasDefaultsTest {

  private final X509ExtendedKeyManager keyManagerMock = mock(X509ExtendedKeyManager.class);
  private final Socket nonSslSocketMock = mock(Socket.class);
  private final Socket nullSocket = null;
  private final SSLSocket sslSocketMock = mock(SSLSocket.class);
  private final SSLSession nonExtendedSslSessionMock = mock(SSLSession.class);
  private final ExtendedSSLSession extendedSslSessionMock = mock(ExtendedSSLSession.class);
  private final TestNotifier testNotifier = new TestNotifier();

  private final CertificateGeneratingX509ExtendedKeyManager certificateGeneratingKeyManager =
      new CertificateGeneratingX509ExtendedKeyManager(
          keyManagerMock,
          mock(DynamicKeyStore.class),
          new ApacheHttpHostNameMatcher(),
          testNotifier);
  private final Principal[] nullPrincipals = null;

  {
    when(keyManagerMock.chooseServerAlias("RSA", nullPrincipals, sslSocketMock))
        .thenReturn("default_alias");
    when(sslSocketMock.getHandshakeSession()).thenReturn(extendedSslSessionMock);
  }

  @Test
  public void returnsNullIfDefaultAliasReturnsNull() {
    given(keyManagerMock.chooseServerAlias("RSA", nullPrincipals, nonSslSocketMock))
        .willReturn(null);

    String alias =
        certificateGeneratingKeyManager.chooseServerAlias("RSA", nullPrincipals, nonSslSocketMock);

    assertNull(alias);
  }

  @Test
  public void returnsDefaultIfSocketIsNull() {
    given(keyManagerMock.chooseServerAlias("RSA", nullPrincipals, nullSocket))
        .willReturn("default_alias");

    String alias =
        certificateGeneratingKeyManager.chooseServerAlias("RSA", nullPrincipals, nullSocket);

    assertEquals("default_alias", alias);
  }

  @Test
  public void returnsDefaultIfSocketIsNotAnAnSSLSocket() {
    given(keyManagerMock.chooseServerAlias("RSA", nullPrincipals, nonSslSocketMock))
        .willReturn("default_alias");

    String alias =
        certificateGeneratingKeyManager.chooseServerAlias("RSA", nullPrincipals, nonSslSocketMock);

    assertEquals("default_alias", alias);
  }

  @Test
  public void returnsDefaultIfHandshakeSessionIsNotSupported() {
    given(sslSocketMock.getHandshakeSession()).willThrow(new UnsupportedOperationException());

    String alias =
        certificateGeneratingKeyManager.chooseServerAlias("RSA", nullPrincipals, sslSocketMock);

    assertEquals("default_alias", alias);
    assertThat(
        testNotifier.getErrorMessages(),
        contains(
            "Dynamic certificate generation is not supported because your SSL Provider does not support SSLSocket.getHandshakeSession()"
                + lineSeparator()
                + "All sites will be served using the normal WireMock HTTPS certificate."));
  }

  @Test
  public void returnsDefaultIfHandshakeSessionIsNotAnAnExtendedSSLSession() {
    given(sslSocketMock.getHandshakeSession()).willReturn(nonExtendedSslSessionMock);

    String alias =
        certificateGeneratingKeyManager.chooseServerAlias("RSA", nullPrincipals, sslSocketMock);

    assertEquals("default_alias", alias);
  }

  @Test
  public void returnsDefaultIfGetRequestedServerNamesIsNotSupported() {
    given(extendedSslSessionMock.getRequestedServerNames())
        .willThrow(new UnsupportedOperationException());

    String alias =
        certificateGeneratingKeyManager.chooseServerAlias("RSA", nullPrincipals, sslSocketMock);

    assertEquals("default_alias", alias);
    assertThat(
        testNotifier.getErrorMessages(),
        contains(
            "Dynamic certificate generation is not supported because your SSL Provider does not support ExtendedSSLSession.getRequestedServerNames()"
                + lineSeparator()
                + "All sites will be served using the normal WireMock HTTPS certificate."));
  }

  @Test
  public void returnsDefaultIfThereAreNoSNIServerNames() {
    given(extendedSslSessionMock.getRequestedServerNames())
        .willReturn(Collections.<SNIServerName>emptyList());

    String alias =
        certificateGeneratingKeyManager.chooseServerAlias("RSA", nullPrincipals, sslSocketMock);

    assertEquals("default_alias", alias);
  }

  @Test
  public void returnsDefaultIfThereAreNoSNIHostNames() {
    SNIServerName notAnSIHostName = new SNIServerName(1, new byte[0]) {};
    given(extendedSslSessionMock.getRequestedServerNames())
        .willReturn(singletonList(notAnSIHostName));

    String alias =
        certificateGeneratingKeyManager.chooseServerAlias("RSA", nullPrincipals, sslSocketMock);

    assertEquals("default_alias", alias);
  }

  @Test
  public void returnsDefaultIfAnSNIHostNameMatchesTheDefaultCertificate() {
    SNIServerName hostName1 = new SNIHostName("example.com");
    SNIServerName hostName2 = new SNIHostName("wiremock.org");
    SNIServerName hostName3 = new SNIHostName("example.org");
    given(extendedSslSessionMock.getRequestedServerNames())
        .willReturn(asList(hostName1, hostName2, hostName3));

    X509Certificate matchingCertificate = certificateWithCn("CN=wiremock.org");
    given(keyManagerMock.getCertificateChain("default_alias"))
        .willReturn(new X509Certificate[] {matchingCertificate});

    String alias =
        certificateGeneratingKeyManager.chooseServerAlias("RSA", nullPrincipals, sslSocketMock);

    assertEquals("default_alias", alias);
  }

  private X509Certificate certificateWithCn(String cn) {
    X509Certificate certificate = mock(X509Certificate.class);
    when(certificate.getSubjectX500Principal()).thenReturn(new X500Principal(cn));
    return certificate;
  }

  public CertificateGeneratingX509ExtendedKeyManagerChooseServerAliasDefaultsTest()
      throws Exception {}
}
