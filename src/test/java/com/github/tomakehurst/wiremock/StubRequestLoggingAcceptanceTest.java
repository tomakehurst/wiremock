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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class StubRequestLoggingAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void logsEventsToNotifierWhenNotDisabled() {
    TestNotifier notifier = new TestNotifier();
    WireMockServer wm = new WireMockServer(wireMockConfig().dynamicPort().notifier(notifier));
    wm.start();
    testClient = new WireMockTestClient(wm.port());

    wm.stubFor(get("/log-me").willReturn(ok("body text")));

    testClient.get("/log-me");
    assertThat(notifier.infoMessages.size(), is(1));
    assertThat(
        notifier.infoMessages.get(0),
        allOf(
            containsString("Request received:"),
            containsString("/log-me"),
            containsString("body text")));
  }

  @Test
  public void doesNotLogEventsToNotifierWhenDisabled() {
    TestNotifier notifier = new TestNotifier();
    WireMockServer wm =
        new WireMockServer(
            wireMockConfig().dynamicPort().stubRequestLoggingDisabled(true).notifier(notifier));
    wm.start();
    testClient = new WireMockTestClient(wm.port());

    wm.stubFor(get("/log-me").willReturn(ok("body")));

    testClient.get("/log-me");
    assertThat(notifier.infoMessages.size(), is(0));
  }

  public static class TestNotifier implements Notifier {

    final List<String> infoMessages = new ArrayList<>();

    @Override
    public void info(String message) {
      infoMessages.add(message);
    }

    @Override
    public void error(String message) {}

    @Override
    public void error(String message, Throwable t) {}
  }
}
