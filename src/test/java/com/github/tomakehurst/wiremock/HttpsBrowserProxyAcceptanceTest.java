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
import static com.github.tomakehurst.wiremock.core.WireMockApp.FILES_ROOT;
import static com.github.tomakehurst.wiremock.core.WireMockApp.MAPPINGS_ROOT;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.testsupport.TestFiles.TRUST_STORE_PASSWORD;
import static com.github.tomakehurst.wiremock.testsupport.TestFiles.TRUST_STORE_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.common.FatalStartupException;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.http.ssl.HostVerifyingSSLSocketFactory;
import com.github.tomakehurst.wiremock.http.ssl.SSLContextBuilder;
import com.github.tomakehurst.wiremock.http.ssl.X509KeyStore;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.testsupport.TestFiles;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.*;

public class HttpsBrowserProxyAcceptanceTest {

  private static final String TARGET_KEYSTORE_WITH_CUSTOM_CERT = TestFiles.KEY_STORE_PATH;
  private static final String PROXY_KEYSTORE_WITH_CUSTOM_CA_CERT = TestFiles.KEY_STORE_WITH_CA_PATH;
  private static final String NO_PREEXISTING_KEYSTORE_PATH =
      tempNonExistingPath("wiremock-keystores", "ca-keystore.jks");

  @ClassRule
  public static WireMockClassRule target =
      new WireMockClassRule(
          wireMockConfig()
              .httpDisabled(true)
              .keystorePath(TARGET_KEYSTORE_WITH_CUSTOM_CERT)
              .dynamicHttpsPort());

  @Rule public WireMockClassRule instanceRule = target;

  @ClassRule
  public static WireMockClassRule proxy =
      new WireMockClassRule(
          wireMockConfig()
              .dynamicPort()
              .dynamicHttpsPort()
              .fileSource(new SingleRootFileSource(setupTempFileRoot()))
              .caKeystorePath(NO_PREEXISTING_KEYSTORE_PATH)
              .enableBrowserProxying(true)
              .trustAllProxyTargets(true));

  @Rule public WireMockClassRule instanceProxyRule = proxy;

  private WireMockTestClient testClient;

  @Before
  public void addAResourceToProxy() {
    testClient = new WireMockTestClient(target.httpsPort());
  }

  @Test
  public void canProxyHttpsInBrowserProxyMode() throws Exception {
    target.stubFor(get(urlEqualTo("/whatever")).willReturn(aResponse().withBody("Got it")));

    assertThat(
        testClient.getViaProxy(target.url("/whatever"), proxy.port()).content(), is("Got it"));
  }

  @Test
  public void canProxyHttpsInBrowserHttpsProxyMode() throws Exception {
    target.stubFor(get(urlEqualTo("/whatever")).willReturn(aResponse().withBody("Got it")));

    WireMockResponse response =
        testClient.getViaProxy(target.url("/whatever"), proxy.httpsPort(), "https");
    assertThat(response.content(), is("Got it"));
  }

  @Test
  @Ignore("Jetty doesn't yet support proxying via HTTP2")
  public void canProxyHttpsUsingHttp2InBrowserHttpsProxyMode() throws Exception {

    HttpClient httpClient = Http2ClientFactory.create();
    ProxyConfiguration proxyConfig = httpClient.getProxyConfiguration();
    HttpProxy httpProxy = new HttpProxy(new Origin.Address("localhost", proxy.httpsPort()), true);
    proxyConfig.getProxies().add(httpProxy);

    target.stubFor(get(urlEqualTo("/whatever")).willReturn(aResponse().withBody("Got it")));

    ContentResponse response = httpClient.GET(target.url("/whatever"));
    assertThat(response.getContentAsString(), is("Got it"));
  }

  @Test
  public void canStubHttpsInBrowserProxyMode() throws Exception {
    target.stubFor(
        get(urlEqualTo("/stubbed")).willReturn(aResponse().withBody("Should Not Be Returned")));
    proxy.stubFor(get(urlEqualTo("/stubbed")).willReturn(aResponse().withBody("Stubbed Value")));
    target.stubFor(
        get(urlEqualTo("/not_stubbed"))
            .willReturn(aResponse().withBody("Should be served from target")));

    assertThat(
        testClient.getViaProxy(target.url("/stubbed"), proxy.port()).content(),
        is("Stubbed Value"));
    assertThat(
        testClient.getViaProxy(target.url("/not_stubbed"), proxy.port()).content(),
        is("Should be served from target"));
  }

  @Test
  public void canRecordHttpsInBrowserProxyMode() throws Exception {

    // given
    proxy.startRecording(target.baseUrl());
    String recordedEndpoint = target.url("/record_me");

    // and
    target.stubFor(
        get(urlEqualTo("/record_me")).willReturn(aResponse().withBody("Target response")));

    // then
    assertThat(
        testClient.getViaProxy(recordedEndpoint, proxy.port()).content(), is("Target response"));

    // when
    proxy.stopRecording();

    // and
    target.stop();

    // then
    assertThat(
        testClient.getViaProxy(recordedEndpoint, proxy.port()).content(), is("Target response"));
  }

  @Test
  public void rejectsUntrustedTarget() {

    WireMockServer scepticalProxy =
        new WireMockServer(wireMockConfig().dynamicPort().enableBrowserProxying(true));

    try {
      scepticalProxy.start();

      target.stubFor(get(urlEqualTo("/whatever")).willReturn(aResponse().withBody("Got it")));

      WireMockResponse response =
          testClient.getViaProxy(target.url("/whatever"), scepticalProxy.port());

      assertThat(response.statusCode(), is(500));
    } finally {
      scepticalProxy.stop();
    }
  }

  @Test
  public void trustsTargetIfTrustStoreContainsItsCertificate() {

    WireMockServer scepticalProxy =
        new WireMockServer(
            wireMockConfig()
                .dynamicPort()
                .enableBrowserProxying(true)
                .trustStorePath(TRUST_STORE_PATH)
                .trustStorePassword(TRUST_STORE_PASSWORD));

    try {
      scepticalProxy.start();

      target.stubFor(get(urlEqualTo("/whatever")).willReturn(aResponse().withBody("Got it")));

      WireMockResponse response =
          testClient.getViaProxy(target.url("/whatever"), scepticalProxy.port());

      assertThat(response.statusCode(), is(200));
      assertThat(response.content(), is("Got it"));
    } finally {
      scepticalProxy.stop();
    }
  }

  @Test
  public void canTrustSpecificTargetHosts() {

    WireMockServer scepticalProxy =
        new WireMockServer(
            wireMockConfig()
                .dynamicPort()
                .enableBrowserProxying(true)
                .trustedProxyTargets("localhost"));

    try {
      scepticalProxy.start();

      target.stubFor(get(urlEqualTo("/whatever")).willReturn(aResponse().withBody("Got it")));

      WireMockResponse response =
          testClient.getViaProxy(target.url("/whatever"), scepticalProxy.port());

      assertThat(response.statusCode(), is(200));
      assertThat(response.content(), is("Got it"));
    } finally {
      scepticalProxy.stop();
    }
  }

  @Test
  public void certificatesSignedWithUsersRootCertificate() throws Exception {

    WireMockServer proxyWithCustomCaKeyStore =
        new WireMockServer(
            wireMockConfig()
                .dynamicPort()
                .enableBrowserProxying(true)
                .trustAllProxyTargets(true)
                .caKeystorePath(PROXY_KEYSTORE_WITH_CUSTOM_CA_CERT));

    try {
      proxyWithCustomCaKeyStore.start();
      KeyStore trustStore =
          HttpsAcceptanceTest.readKeyStore(PROXY_KEYSTORE_WITH_CUSTOM_CA_CERT, "password");

      // given
      CloseableHttpClient httpClient =
          HttpClients.custom()
              .setDnsResolver(new CustomLocalTldDnsResolver("internal"))
              .setSSLSocketFactory(sslSocketFactoryThatTrusts(trustStore))
              .setProxy(new HttpHost("localhost", proxyWithCustomCaKeyStore.port()))
              .build();

      // when
      httpClient.execute(
          new HttpGet("https://fake1.nowildcards1.internal:" + target.httpsPort() + "/whatever"));

      // then no exception is thrown

      // when
      httpClient.execute(
          new HttpGet("https://fake2.nowildcards2.internal:" + target.httpsPort() + "/whatever"));

      // then no exception is thrown
    } finally {
      proxyWithCustomCaKeyStore.stop();
    }
  }

  @Test
  public void certificatesSignedWithGeneratedRootCertificate() throws Exception {

    KeyStore trustStore =
        HttpsAcceptanceTest.readKeyStore(NO_PREEXISTING_KEYSTORE_PATH, "password");

    // given
    CloseableHttpClient httpClient =
        HttpClients.custom()
            .setDnsResolver(new CustomLocalTldDnsResolver("internal"))
            .setSSLSocketFactory(sslSocketFactoryThatTrusts(trustStore))
            .setProxy(new HttpHost("localhost", proxy.port()))
            .build();

    // when
    httpClient.execute(
        new HttpGet("https://fake1.nowildcards1.internal:" + target.httpsPort() + "/whatever"));

    // then no exception is thrown

    // when
    httpClient.execute(
        new HttpGet("https://fake2.nowildcards2.internal:" + target.httpsPort() + "/whatever"));

    // then no exception is thrown
  }

  @Test(expected = EOFException.class)
  public void failsIfCaKeystorePathIsNotAKeystore() throws IOException {
    new WireMockServer(
            options()
                .enableBrowserProxying(true)
                .caKeystorePath(Files.createTempFile("notakeystore", "jks").toString()))
        .start();
  }

  @Test(expected = FatalStartupException.class)
  public void failsIfCaKeystoreDoesNotContainACaCertificate() throws Exception {
    new WireMockServer(
            options().enableBrowserProxying(true).caKeystorePath(emptyKeyStore().toString()))
        .start();
  }

  @Test
  public void certificateAuthorityCertCanBeDownloaded() throws Exception {
    WireMockTestClient proxyTestClient = new WireMockTestClient(proxy.port());

    WireMockResponse certResponse = proxyTestClient.get("/__admin/certs/wiremock-ca.crt");
    assertEquals(200, certResponse.statusCode());
    assertEquals("application/x-pem-file", certResponse.firstHeader("Content-Type"));

    Certificate cert = decode(certResponse.content());
    X509KeyStore keyStore =
        new X509KeyStore(
            HttpsAcceptanceTest.readKeyStore(NO_PREEXISTING_KEYSTORE_PATH, "password"),
            "password".toCharArray());

    assertEquals(keyStore.getCertificateAuthority().certificateChain()[0], cert);
  }

  private Certificate decode(String body) throws Exception {
    String base64 =
        body.replace("-----BEGIN CERTIFICATE-----", "").replace("-----END CERTIFICATE-----", "");
    byte[] certBytes = Base64.getMimeDecoder().decode(base64);
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    return cf.generateCertificate(new ByteArrayInputStream(certBytes));
  }

  private SSLConnectionSocketFactory sslSocketFactoryThatTrusts(KeyStore trustStore)
      throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
    SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(trustStore).build();
    return new SSLConnectionSocketFactory(
        new HostVerifyingSSLSocketFactory(sslContext.getSocketFactory()),
        new NoopHostnameVerifier() // using Java's hostname verification
        );
  }

  private static File setupTempFileRoot() {
    try {
      File root = Files.createTempDirectory("wiremock").toFile();
      new File(root, MAPPINGS_ROOT).mkdirs();
      new File(root, FILES_ROOT).mkdirs();
      return root;
    } catch (IOException e) {
      return throwUnchecked(e, File.class);
    }
  }

  private static class CustomLocalTldDnsResolver implements DnsResolver {

    private final String tldToSendToLocalhost;

    public CustomLocalTldDnsResolver(String tldToSendToLocalhost) {
      this.tldToSendToLocalhost = tldToSendToLocalhost;
    }

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {

      if (host.endsWith("." + tldToSendToLocalhost)) {
        return new InetAddress[] {InetAddress.getLocalHost()};
      } else {
        return new SystemDefaultDnsResolver().resolve(host);
      }
    }
  }

  private static String tempNonExistingPath(String prefix, String filename) {
    try {
      Path tempDirectory = Files.createTempDirectory(prefix);
      return tempDirectory.resolve(filename).toFile().getAbsolutePath();
    } catch (IOException e) {
      return throwUnchecked(e, null);
    }
  }

  private static Path emptyKeyStore()
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    char[] password = "password".toCharArray();
    keyStore.load(null, password);
    Path keystoreNoCa = Files.createTempFile("keystore-with-no-ca", "jks");
    try (FileOutputStream fos = new FileOutputStream(keystoreNoCa.toFile())) {
      keyStore.store(fos, password);
    }
    return keystoreNoCa;
  }
}
