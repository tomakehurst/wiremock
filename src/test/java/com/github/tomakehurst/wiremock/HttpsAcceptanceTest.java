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
package com.github.tomakehurst.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.testsupport.TestFiles.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FatalStartupException;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.google.common.io.Resources;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.HttpResponse;
import org.apache.http.MalformedChunkCodingException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HttpsAcceptanceTest {

  private WireMockServer wireMockServer;
  private WireMockServer proxy;
  private HttpClient httpClient;

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  @After
  public void serverShutdown() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }

    if (proxy != null) {
      proxy.shutdown();
    }
  }

  @Test
  public void shouldReturnStubOnSpecifiedPort() throws Exception {
    startServerWithDefaultKeystore();
    stubFor(
        get(urlEqualTo("/https-test"))
            .willReturn(aResponse().withStatus(200).withBody("HTTPS content")));

    assertThat(contentFor(url("/https-test")), is("HTTPS content"));
  }

  @Test
  public void shouldReturnOnlyOnHttpsWhenHttpDisabled() throws Exception {
    // HTTP
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage(
        "Not listening on HTTP port. Either HTTP is not enabled or the WireMock server is stopped.");
    // HTTPS
    WireMockConfiguration config = wireMockConfig().httpDisabled(true).dynamicHttpsPort();
    wireMockServer = new WireMockServer(config);
    wireMockServer.start();
    WireMock.configureFor("https", "localhost", wireMockServer.httpsPort());
    httpClient = HttpClientFactory.createClient();

    stubFor(
        get(urlEqualTo("/https-test"))
            .willReturn(aResponse().withStatus(200).withBody("HTTPS content")));

    wireMockServer.port();
    assertThat(contentFor(url("/https-test")), is("HTTPS content"));
  }

  @Rule public final ExpectedException exception = ExpectedException.none();

  @Test
  public void connectionResetByPeerFault() throws IOException {
    assumeFalse(
        "This feature does not work on Windows " + "because of differing native socket behaviour",
        SystemUtils.IS_OS_WINDOWS);

    startServerWithDefaultKeystore();
    stubFor(
        get(urlEqualTo("/connection/reset"))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    try {
      httpClient.execute(new HttpGet(url("/connection/reset"))).getEntity();
      fail("Expected a SocketException or SSLException to be thrown");
    } catch (Exception e) {
      assertThat(
          e.getClass().getName(),
          Matchers.anyOf(is(SocketException.class.getName()), is(SSLException.class.getName())));
    }
  }

  @Test
  public void emptyResponseFault() {
    startServerWithDefaultKeystore();
    stubFor(
        get(urlEqualTo("/empty/response")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

    getAndAssertUnderlyingExceptionInstanceClass(
        url("/empty/response"), NoHttpResponseException.class);
  }

  @Test
  public void malformedResponseChunkFault() {
    startServerWithDefaultKeystore();
    stubFor(
        get(urlEqualTo("/malformed/response"))
            .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

    getAndAssertUnderlyingExceptionInstanceClass(
        url("/malformed/response"), MalformedChunkCodingException.class);
  }

  @Test
  public void randomDataOnSocketFault() {
    startServerWithDefaultKeystore();
    stubFor(
        get(urlEqualTo("/random/data"))
            .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

    getAndAssertUnderlyingExceptionInstanceClass(url("/random/data"), ProtocolException.class);
  }

  @Test(expected = Exception.class)
  public void throwsExceptionWhenBadAlternativeKeystore() {
    String testKeystorePath = Resources.getResource("bad-keystore").toString();
    startServerWithKeystore(testKeystorePath);
  }

  @Test
  public void acceptsAlternativeKeystore() throws Exception {
    String testKeystorePath = Resources.getResource("test-keystore").toString();
    startServerWithKeystore(testKeystorePath);
    stubFor(
        get(urlEqualTo("/https-test"))
            .willReturn(aResponse().withStatus(200).withBody("HTTPS content")));

    assertThat(contentFor(url("/https-test")), is("HTTPS content"));
  }

  @Test
  public void acceptsAlternativeKeystoreWithNonDefaultPassword() throws Exception {
    String testKeystorePath = Resources.getResource("test-keystore-pwd").toString();
    startServerWithKeystore(testKeystorePath, "nondefaultpass", "password");
    stubFor(
        get(urlEqualTo("/https-test"))
            .willReturn(aResponse().withStatus(200).withBody("HTTPS content")));

    assertThat(contentFor(url("/https-test")), is("HTTPS content"));
  }

  @Test
  public void acceptsAlternativeKeystoreWithNonDefaultKeyManagerPassword() throws Exception {
    String keystorePath = Resources.getResource("test-keystore-key-man-pwd").toString();
    startServerWithKeystore(keystorePath, "password", "anotherpassword");
    stubFor(
        get(urlEqualTo("/alt-password-https"))
            .willReturn(aResponse().withStatus(200).withBody("HTTPS content")));

    assertThat(contentFor(url("/alt-password-https")), is("HTTPS content"));
  }

  @Test
  public void failsToStartWithAlternativeKeystoreWithWrongKeyManagerPassword() {
    try {
      String keystorePath = Resources.getResource("test-keystore-key-man-pwd").toString();
      startServerWithKeystore(keystorePath, "password", "wrongpassword");
      fail("Expected a SocketException or SSLHandshakeException to be thrown");
    } catch (Exception e) {
      assertThat(e.getClass().getName(), is(FatalStartupException.class.getName()));
    }
  }

  @Test
  public void rejectsWithoutClientCertificate() {
    startServerEnforcingClientCert(KEY_STORE_PATH, TRUST_STORE_PATH, TRUST_STORE_PASSWORD);
    wireMockServer.stubFor(
        get(urlEqualTo("/https-test"))
            .willReturn(aResponse().withStatus(200).withBody("HTTPS content")));

    try {
      contentFor(url("/https-test")); // this lacks the required client certificate
      fail("Expected a SocketException, SSLHandshakeException or SSLException to be thrown");
    } catch (Exception e) {
      assertThat(
          e.getClass().getName(),
          Matchers.anyOf(
              is(SocketException.class.getName()),
              is(SSLHandshakeException.class.getName()),
              is(SSLException.class.getName())));
    }
  }

  @Test
  public void acceptWithClientCertificate() throws Exception {
    String testTrustStorePath = TRUST_STORE_PATH;
    String testClientCertPath = TRUST_STORE_PATH;

    startServerEnforcingClientCert(KEY_STORE_PATH, testTrustStorePath, TRUST_STORE_PASSWORD);
    wireMockServer.stubFor(
        get(urlEqualTo("/https-test"))
            .willReturn(aResponse().withStatus(200).withBody("HTTPS content")));

    assertThat(
        secureContentFor(url("/https-test"), testClientCertPath, TRUST_STORE_PASSWORD),
        is("HTTPS content"));
  }

  @Test
  public void supportsProxyingWhenTargetRequiresClientCert() throws Exception {
    startServerEnforcingClientCert(KEY_STORE_PATH, TRUST_STORE_PATH, TRUST_STORE_PASSWORD);
    wireMockServer.stubFor(
        get(urlEqualTo("/client-cert-proxy")).willReturn(aResponse().withStatus(200)));

    proxy =
        new WireMockServer(
            wireMockConfig()
                .port(Options.DYNAMIC_PORT)
                .trustStorePath(TRUST_STORE_PATH)
                .trustStorePassword(TRUST_STORE_PASSWORD));
    proxy.start();
    proxy.stubFor(
        get(urlEqualTo("/client-cert-proxy"))
            .willReturn(
                aResponse().proxiedFrom("https://localhost:" + wireMockServer.httpsPort())));

    HttpGet get = new HttpGet("http://localhost:" + proxy.port() + "/client-cert-proxy");
    HttpResponse response = httpClient.execute(get);
    assertThat(response.getStatusLine().getStatusCode(), is(200));
  }

  @Test
  public void proxyingFailsWhenTargetServiceRequiresClientCertificatesAndProxyDoesNotSend()
      throws Exception {
    startServerEnforcingClientCert(KEY_STORE_PATH, TRUST_STORE_PATH, TRUST_STORE_PASSWORD);
    wireMockServer.stubFor(
        get(urlEqualTo("/client-cert-proxy-fail")).willReturn(aResponse().withStatus(200)));

    proxy = new WireMockServer(wireMockConfig().port(Options.DYNAMIC_PORT));
    proxy.start();
    proxy.stubFor(
        get(urlEqualTo("/client-cert-proxy-fail"))
            .willReturn(
                aResponse().proxiedFrom("https://localhost:" + wireMockServer.httpsPort())));

    HttpGet get = new HttpGet("http://localhost:" + proxy.port() + "/client-cert-proxy-fail");
    HttpResponse response = httpClient.execute(get);
    assertThat(response.getStatusLine().getStatusCode(), is(500));
  }

  private String url(String path) {
    return String.format("https://localhost:%d%s", wireMockServer.httpsPort(), path);
  }

  private static String toPath(String resourcePath) {
    try {
      return new File(Resources.getResource(resourcePath).toURI()).getCanonicalPath();
    } catch (Exception e) {
      return throwUnchecked(e, String.class);
    }
  }

  private void getAndAssertUnderlyingExceptionInstanceClass(String url, Class<?> expectedClass) {
    boolean thrown = false;
    try {
      contentFor(url);
    } catch (Exception e) {
      Throwable cause = e.getCause();
      e.printStackTrace();
      if (cause != null) {
        assertThat(e.getCause(), instanceOf(expectedClass));
      } else {
        assertThat(e, instanceOf(expectedClass));
      }

      thrown = true;
    }

    assertTrue("No exception was thrown", thrown);
  }

  private String contentFor(String url) throws Exception {
    HttpGet get = new HttpGet(url);
    HttpResponse response = httpClient.execute(get);
    String content = EntityUtils.toString(response.getEntity());
    return content;
  }

  private void startServerEnforcingClientCert(
      String keystorePath, String truststorePath, String trustStorePassword) {
    WireMockConfiguration config = wireMockConfig().dynamicPort().dynamicHttpsPort();
    if (keystorePath != null) {
      config.keystorePath(keystorePath);
    }
    if (truststorePath != null) {
      config.trustStorePath(truststorePath);
      config.trustStorePassword(trustStorePassword);
      config.needClientAuth(true);
    }
    config.bindAddress("localhost");

    wireMockServer = new WireMockServer(config);
    wireMockServer.start();
    WireMock.configureFor("https", "localhost", wireMockServer.httpsPort());

    httpClient = HttpClientFactory.createClient();
  }

  private void startServerWithKeystore(
      String keystorePath, String keystorePassword, String keyManagerPassword) {
    WireMockConfiguration config = wireMockConfig().dynamicPort().dynamicHttpsPort();
    if (keystorePath != null) {
      config
          .keystorePath(keystorePath)
          .keystorePassword(keystorePassword)
          .keyManagerPassword(keyManagerPassword);
    }

    wireMockServer = new WireMockServer(config);
    wireMockServer.start();
    WireMock.configureFor(wireMockServer.port());

    httpClient = HttpClientFactory.createClient();
  }

  private void startServerWithKeystore(String keystorePath) {
    startServerWithKeystore(keystorePath, "password", "password");
  }

  private void startServerWithDefaultKeystore() {
    startServerWithKeystore(null);
  }

  static String secureContentFor(String url, String clientTrustStore, String trustStorePassword)
      throws Exception {
    KeyStore trustStore = readKeyStore(clientTrustStore, trustStorePassword);

    SSLContext sslcontext =
        SSLContexts.custom()
            .loadTrustMaterial(null, new TrustSelfSignedStrategy())
            .loadKeyMaterial(trustStore, trustStorePassword.toCharArray())
            .setKeyStoreType("pkcs12")
            .setProtocol("TLS")
            .build();

    SSLConnectionSocketFactory sslsf =
        new SSLConnectionSocketFactory(
            sslcontext,
            null, // supported protocols
            null, // supported cipher suites
            NoopHostnameVerifier.INSTANCE);

    CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

    HttpGet get = new HttpGet(url);
    HttpResponse response = httpClient.execute(get);
    String content = EntityUtils.toString(response.getEntity());
    return content;
  }

  static KeyStore readKeyStore(String path, String password)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (FileInputStream instream = new FileInputStream(path)) {
      trustStore.load(instream, password.toCharArray());
    }
    return trustStore;
  }
}
