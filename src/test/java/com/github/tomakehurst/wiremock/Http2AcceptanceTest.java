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

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.eclipse.jetty.http.HttpVersion.HTTP_2;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Rule;
import org.junit.Test;

public class Http2AcceptanceTest {

  @Rule
  public WireMockRule wm = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

  @Test
  public void supportsHttp2Connections() throws Exception {
    HttpClient client = Http2ClientFactory.create();

    wm.stubFor(get("/thing").willReturn(ok("HTTP/2 response")));

    ContentResponse response = client.GET("https://localhost:" + wm.httpsPort() + "/thing");
    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void supportsHttp2PlaintextConnections() throws Exception {
    HttpClient client = Http2ClientFactory.create();

    wm.stubFor(get("/thing").willReturn(ok("HTTP/2 response")));

    ContentResponse response = client.GET("http://localhost:" + wm.port() + "/thing");
    assertThat(response.getVersion(), is(HTTP_2));
    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void supportsHttp1_1Connections() throws Exception {
    CloseableHttpClient client = HttpClientFactory.createClient();

    wm.stubFor(get("/thing").willReturn(ok("HTTP/1.1 response")));

    HttpGet get = new HttpGet("https://localhost:" + wm.httpsPort() + "/thing");
    try (CloseableHttpResponse response = client.execute(get)) {
      assertThat(response.getStatusLine().getStatusCode(), is(200));
    }
  }
}
