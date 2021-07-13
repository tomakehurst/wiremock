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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.common.Timing;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Very slow and not likely to change any time soon")
public class LogTimingAcceptanceTest extends AcceptanceTestBase {

  @BeforeClass
  public static void setupServer() {
    setupServer(options().asynchronousResponseEnabled(true).asynchronousResponseThreads(5));
  }

  @Test
  public void serveEventIncludesTotalAndServeDuration() {
    stubFor(get("/time-me").willReturn(ok()));

    // Create some work
    for (int i = 0; i < 2500; i++) {
      stubFor(get("/time-me/" + i).willReturn(ok()));
    }

    testClient.get("/time-me");

    ServeEvent serveEvent = getAllServeEvents().get(0);

    assertThat(serveEvent.getTiming().getServeTime(), greaterThan(0));
    assertThat(serveEvent.getTiming().getTotalTime(), greaterThan(0));
  }

  @Test
  public void includesAddedDelayInTotalWhenAsync() {
    final int DELAY = 500;

    stubFor(
        post("/time-me/async")
            .withRequestBody(equalToXml("<value>1111</value>"))
            .willReturn(ok().withFixedDelay(DELAY)));

    // Create some work
    for (int i = 0; i < 500; i++) {
      stubFor(
          post("/time-me/async")
              .withRequestBody(equalToXml("<value>123456" + i + " </value>"))
              .willReturn(ok()));
    }

    testClient.postXml("/time-me/async", "<value>1111</value>");
    ServeEvent serveEvent = getAllServeEvents().get(0);

    Timing timing = serveEvent.getTiming();
    assertThat(timing.getAddedDelay(), is(DELAY));
    assertThat(timing.getProcessTime(), greaterThan(0));
    //        assertThat(timing.getResponseSendTime(), greaterThan(0)); // Hard for this not to be
    // flakey without some kind of throttling on the loopback adapter
    assertThat(timing.getServeTime(), is(timing.getProcessTime() + timing.getResponseSendTime()));
    assertThat(
        timing.getTotalTime(), is(timing.getProcessTime() + timing.getResponseSendTime() + DELAY));
  }
}
