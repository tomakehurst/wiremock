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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Rule;
import org.junit.Test;

public class ResponseDelayAsynchronousAcceptanceTest {

  private static final int SOCKET_TIMEOUT_MILLISECONDS = 500;
  private static final int SHORTER_THAN_SOCKET_TIMEOUT = SOCKET_TIMEOUT_MILLISECONDS / 2;

  private ExecutorService httpClientExecutor = Executors.newCachedThreadPool();

  @Rule public WireMockRule wireMockRule = new WireMockRule(getOptions());

  private WireMockConfiguration getOptions() {
    WireMockConfiguration wireMockConfiguration = new WireMockConfiguration();
    wireMockConfiguration.jettyAcceptors(1).containerThreads(8);
    wireMockConfiguration.asynchronousResponseEnabled(true);
    wireMockConfiguration.asynchronousResponseThreads(10);
    wireMockConfiguration.dynamicPort();
    return wireMockConfiguration;
  }

  @Test
  public void addsFixedDelayAsynchronously() throws Exception {
    stubFor(get("/delayed").willReturn(ok().withFixedDelay(SHORTER_THAN_SOCKET_TIMEOUT)));

    List<Future<TimedHttpResponse>> responses =
        httpClientExecutor.invokeAll(getHttpRequestCallables(5));

    for (Future<TimedHttpResponse> response : responses) {
      TimedHttpResponse timedResponse = response.get();
      assertThat(timedResponse.response.getStatusLine().getStatusCode(), is(200));
      assertThat(timedResponse.milliseconds, greaterThan((double) SHORTER_THAN_SOCKET_TIMEOUT));
    }
  }

  @Test
  public void addsRandomDelayAsynchronously() throws Exception {
    stubFor(get("/delayed").willReturn(ok().withUniformRandomDelay(100, 300)));

    List<Future<TimedHttpResponse>> responses =
        httpClientExecutor.invokeAll(getHttpRequestCallables(5));

    for (Future<TimedHttpResponse> response : responses) {
      TimedHttpResponse timedResponse = response.get();
      assertThat(timedResponse.response.getStatusLine().getStatusCode(), is(200));
      assertThat(timedResponse.milliseconds, greaterThan(100.0));
    }
  }

  private List<Callable<TimedHttpResponse>> getHttpRequestCallables(int requestCount)
      throws IOException {
    List<Callable<TimedHttpResponse>> requests = new ArrayList<>();
    for (int i = 0; i < requestCount; i++) {
      final Stopwatch stopwatch = Stopwatch.createStarted();
      requests.add(
          new Callable<TimedHttpResponse>() {
            @Override
            public TimedHttpResponse call() throws Exception {
              CloseableHttpResponse response =
                  HttpClientFactory.createClient(SOCKET_TIMEOUT_MILLISECONDS)
                      .execute(
                          new HttpGet(
                              String.format("http://localhost:%d/delayed", wireMockRule.port())));

              return new TimedHttpResponse(response, stopwatch.stop().elapsed(MILLISECONDS));
            }
          });
    }
    return requests;
  }

  private static class TimedHttpResponse {
    public final HttpResponse response;
    public final double milliseconds;

    public TimedHttpResponse(HttpResponse response, long milliseconds) {
      this.response = response;
      this.milliseconds = milliseconds;
    }
  }
}
