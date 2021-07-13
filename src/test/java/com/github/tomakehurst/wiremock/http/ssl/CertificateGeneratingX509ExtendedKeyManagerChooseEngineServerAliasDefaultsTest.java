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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.testsupport.TestNotifier;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Collections;
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import org.junit.Test;

public class CertificateGeneratingX509ExtendedKeyManagerChooseEngineServerAliasDefaultsTest {

  private final X509ExtendedKeyManager keyManagerMock = mock(X509ExtendedKeyManager.class);
  private final SSLEngine sslEngineMock = mock(SSLEngine.class);
  private final SSLEngine nullSslEngine = null;
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
    when(keyManagerMock.chooseEngineServerAlias("RSA", nullPrincipals, sslEngineMock))
        .thenReturn("default_alias");
    when(sslEngineMock.getHandshakeSession()).thenReturn(extendedSslSessionMock);
  }

  @Test
  public void returnsNullIfDefaultAliasReturnsNull() {
    given(keyManagerMock.chooseEngineServerAlias("RSA", nullPrincipals, sslEngineMock))
        .willReturn(null);

    String alias =
        certificateGeneratingKeyManager.chooseEngineServerAlias(
            "RSA", nullPrincipals, sslEngineMock);

    assertNull(alias);
  }

  @Test
  public void returnsDefaultIfEngineIsNull() {
    given(keyManagerMock.chooseEngineServerAlias("RSA", nullPrincipals, nullSslEngine))
        .willReturn("default_alias");

    String alias =
        certificateGeneratingKeyManager.chooseEngineServerAlias(
            "RSA", nullPrincipals, nullSslEngine);

    assertEquals("default_alias", alias);
  }

  @Test
  public void returnsDefaultIfHandshakeSessionIsNotSupported() {
    given(sslEngineMock.getHandshakeSession()).willThrow(new UnsupportedOperationException());

    String alias =
        certificateGeneratingKeyManager.chooseEngineServerAlias(
            "RSA", nullPrincipals, sslEngineMock);

    assertEquals("default_alias", alias);
    assertThat(
        testNotifier.getErrorMessages(),
        contains(
            "Dynamic certificate generation is not supported because your SSL Provider does not support SSLEngine.getHandshakeSession()"
                + lineSeparator()
                + "All sites will be served using the normal WireMock HTTPS certificate."));
  }

  @Test
  public void returnsDefaultIfHandshakeSessionIsNotAnAnExtendedSSLSession() {
    given(sslEngineMock.getHandshakeSession()).willReturn(nonExtendedSslSessionMock);

    String alias =
        certificateGeneratingKeyManager.chooseEngineServerAlias(
            "RSA", nullPrincipals, sslEngineMock);

    assertEquals("default_alias", alias);
  }

  @Test
  public void returnsDefaultIfGetRequestedServerNamesIsNotSupported() {
    given(extendedSslSessionMock.getRequestedServerNames())
        .willThrow(new UnsupportedOperationException());

    String alias =
        certificateGeneratingKeyManager.chooseEngineServerAlias(
            "RSA", nullPrincipals, sslEngineMock);

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
        certificateGeneratingKeyManager.chooseEngineServerAlias(
            "RSA", nullPrincipals, sslEngineMock);

    assertEquals("default_alias", alias);
  }

  @Test
  public void returnsDefaultIfThereAreNoSNIHostNames() {
    SNIServerName notAnSIHostName = new SNIServerName(1, new byte[0]) {};
    given(extendedSslSessionMock.getRequestedServerNames())
        .willReturn(Collections.singletonList(notAnSIHostName));

    String alias =
        certificateGeneratingKeyManager.chooseEngineServerAlias(
            "RSA", nullPrincipals, sslEngineMock);

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
        certificateGeneratingKeyManager.chooseEngineServerAlias(
            "RSA", nullPrincipals, sslEngineMock);

    assertEquals("default_alias", alias);
  }

  private X509Certificate certificateWithCn(String cn) {
    X509Certificate certificate = mock(X509Certificate.class);
    when(certificate.getSubjectX500Principal()).thenReturn(new X500Principal(cn));
    return certificate;
  }

  public CertificateGeneratingX509ExtendedKeyManagerChooseEngineServerAliasDefaultsTest()
      throws Exception {}
}
