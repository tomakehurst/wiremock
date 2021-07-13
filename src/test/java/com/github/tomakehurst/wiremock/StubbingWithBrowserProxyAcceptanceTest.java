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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.util.EntityUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class StubbingWithBrowserProxyAcceptanceTest {

  static final String EXPECTED_RESPONSE_BODY = "Got it";

  @ClassRule
  public static WireMockClassRule wm =
      new WireMockClassRule(
          wireMockConfig()
              .dynamicPort()
              .enableBrowserProxying(true)
              .notifier(new ConsoleNotifier(true)));

  @Rule public WireMockClassRule instance = wm;

  static CloseableHttpClient client;

  @BeforeClass
  public static void init() {
    client =
        HttpClientBuilder.create()
            .setDnsResolver(new CustomLocalTldDnsResolver("internal"))
            .setProxy(new HttpHost("localhost", wm.port()))
            .build();
  }

  @Test
  public void matchesOnHostname() throws Exception {
    stubFor(
        get(urlPathEqualTo("/mypath"))
            .withHost(equalTo("righthost.internal"))
            .willReturn(ok(EXPECTED_RESPONSE_BODY)));

    HttpUriRequest request = RequestBuilder.get("http://righthost.internal/mypath").build();
    makeRequestAndAssertOk(request);
  }

  @Test
  public void doesNotMatchOnHostnameWhenIncorrect() throws Exception {
    stubFor(
        get(urlPathEqualTo("/mypath"))
            .withHost(equalTo("righthost.internal"))
            .willReturn(ok(EXPECTED_RESPONSE_BODY)));

    HttpUriRequest request = RequestBuilder.get("http://wronghost.internal/mypath").build();
    makeRequestAndAssertNotOk(request);
  }

  @Test
  public void matchesAnyHostnameWhenNotSpecified() throws Exception {
    stubFor(get(urlPathEqualTo("/mypath")).willReturn(ok(EXPECTED_RESPONSE_BODY)));

    HttpUriRequest request = RequestBuilder.get("http://whatever.internal/mypath").build();
    makeRequestAndAssertOk(request);
  }

  @Test
  public void matchesPortNumber() throws Exception {
    stubFor(get(urlPathEqualTo("/mypath")).withPort(1234).willReturn(ok(EXPECTED_RESPONSE_BODY)));

    HttpUriRequest request = RequestBuilder.get("http://localhost:1234/mypath").build();
    makeRequestAndAssertOk(request);
  }

  @Test
  public void doesNotMatchOnPortNumberWhenIncorrect() throws Exception {
    stubFor(get(urlPathEqualTo("/mypath")).withPort(1234).willReturn(ok(EXPECTED_RESPONSE_BODY)));

    HttpUriRequest request = RequestBuilder.get("http://localhost:4321/mypath").build();
    makeRequestAndAssertNotOk(request);
  }

  @Test
  public void matchesOnScheme() throws Exception {
    stubFor(
        get(urlPathEqualTo("/mypath")).withScheme("http").willReturn(ok(EXPECTED_RESPONSE_BODY)));

    HttpUriRequest request = RequestBuilder.get("http://whatever/mypath").build();
    makeRequestAndAssertOk(request);
  }

  @Test
  public void doesNotMatchWhenSchemeIncorrect() throws Exception {
    stubFor(
        get(urlPathEqualTo("/mypath")).withScheme("https").willReturn(ok(EXPECTED_RESPONSE_BODY)));

    HttpUriRequest request = RequestBuilder.get("http://whatever/mypath").build();
    makeRequestAndAssertNotOk(request);
  }

  private void makeRequestAndAssertOk(HttpUriRequest request) throws Exception {
    try (CloseableHttpResponse response = client.execute(request)) {
      assertThat(EntityUtils.toString(response.getEntity()), is(EXPECTED_RESPONSE_BODY));
    }
  }

  private void makeRequestAndAssertNotOk(HttpUriRequest request) throws Exception {
    try (CloseableHttpResponse response = client.execute(request)) {
      assertThat(EntityUtils.toString(response.getEntity()), not(is(EXPECTED_RESPONSE_BODY)));
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
}
