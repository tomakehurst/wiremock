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
import static com.github.tomakehurst.wiremock.testsupport.TestFiles.filePath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Test;

public class TransferEncodingAcceptanceTest {

  WireMockServer wm;
  WireMockTestClient testClient;

  @Test
  public void sendsContentLengthWhenTransferEncodingChunkedPolicyIsNever() {
    startWithChunkedEncodingPolicy(Options.ChunkedEncodingPolicy.NEVER);

    final String url = "/content-length-encoding";
    final String body = "Body content";

    wm.stubFor(get(url).willReturn(ok(body)));

    WireMockResponse response = testClient.get(url);
    assertThat(response.statusCode(), is(200));

    String expectedContentLength = String.valueOf(body.getBytes().length);
    assertThat(response.firstHeader("Transfer-Encoding"), nullValue());
    assertThat(response.firstHeader("Content-Length"), is(expectedContentLength));
  }

  @Test
  public void sendsTransferEncodingChunkedWhenPolicyIsAlways() {
    startWithChunkedEncodingPolicy(Options.ChunkedEncodingPolicy.ALWAYS);

    final String url = "/chunked-encoding-always";
    final String body = "Body content";

    wm.stubFor(get(url).willReturn(ok(body)));

    WireMockResponse response = testClient.get(url);
    assertThat(response.statusCode(), is(200));

    assertThat(response.firstHeader("Transfer-Encoding"), is("chunked"));
    assertThat(response.firstHeader("Content-Length"), nullValue());
  }

  @Test
  public void sendsTransferEncodingChunkedWhenPolicyIsBodyFileAndBodyFileIsUsed() {
    startWithChunkedEncodingPolicy(Options.ChunkedEncodingPolicy.BODY_FILE);

    final String fileUrl = "/chunked-encoding-body";
    final String inlineBodyUrl = "/chunked-encoding-body-file";

    wm.stubFor(get(fileUrl).willReturn(ok().withBodyFile("plain-example.txt")));
    wm.stubFor(get(inlineBodyUrl).willReturn(ok("Body content")));

    WireMockResponse response = testClient.get(fileUrl);
    assertThat(response.statusCode(), is(200));
    assertThat(response.firstHeader("Transfer-Encoding"), is("chunked"));
    assertThat(response.firstHeader("Content-Length"), nullValue());

    response = testClient.get(inlineBodyUrl);
    assertThat(response.statusCode(), is(200));
    assertThat(response.firstHeader("Transfer-Encoding"), nullValue());
    assertThat(response.firstHeader("Content-Length"), notNullValue());
  }

  @Test
  public void sendsContentLengthWhenTransferEncodingChunkedPolicyIsNeverAndDribbleDelayIsApplied() {
    startWithChunkedEncodingPolicy(Options.ChunkedEncodingPolicy.NEVER);

    final String url = "/content-length-encoding";
    final String body = "Slightly longer body content in this string";

    wm.stubFor(get(url).willReturn(ok(body).withChunkedDribbleDelay(5, 200)));

    WireMockResponse response = testClient.get(url);
    assertThat(response.statusCode(), is(200));

    String expectedContentLength = String.valueOf(body.getBytes().length);
    assertThat(response.firstHeader("Transfer-Encoding"), nullValue());
    assertThat(response.firstHeader("Content-Length"), is(expectedContentLength));
  }

  @Test
  public void sendsSpecifiedContentLengthInResponseWhenChunkedEncodingEnabled() throws Exception {
    startWithChunkedEncodingPolicy(Options.ChunkedEncodingPolicy.ALWAYS);

    String path = "/length";
    wm.stubFor(get(path).willReturn(ok("stuff").withHeader("Content-Length", "1234")));

    CloseableHttpClient httpClient = HttpClientFactory.createClient();
    HttpGet request = new HttpGet(wm.baseUrl() + path);
    try (final CloseableHttpResponse response = httpClient.execute(request)) {
      assertThat(response.getFirstHeader("Content-Length").getValue(), is("1234"));
    }
  }

  @Test
  public void sendsSpecifiedContentLengthInResponseWhenChunkedEncodingDisabled() throws Exception {
    startWithChunkedEncodingPolicy(Options.ChunkedEncodingPolicy.NEVER);

    String path = "/length";
    wm.stubFor(get(path).willReturn(ok("stuff").withHeader("Content-Length", "1234")));

    CloseableHttpClient httpClient = HttpClientFactory.createClient();
    HttpGet request = new HttpGet(wm.baseUrl() + path);
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      assertThat(response.getFirstHeader("Content-Length").getValue(), is("1234"));
    }
  }

  private void startWithChunkedEncodingPolicy(Options.ChunkedEncodingPolicy chunkedEncodingPolicy) {
    wm =
        new WireMockServer(
            wireMockConfig()
                .dynamicPort()
                .withRootDirectory(filePath("test-file-root"))
                .useChunkedTransferEncoding(chunkedEncodingPolicy));
    wm.start();

    testClient = new WireMockTestClient(wm.port());
  }

  @After
  public void cleanup() {
    wm.stop();
  }
}
