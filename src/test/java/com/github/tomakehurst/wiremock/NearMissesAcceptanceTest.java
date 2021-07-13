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
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static com.github.tomakehurst.wiremock.testsupport.TestHttpHeader.withHeader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.NearMiss;
import java.util.List;
import org.junit.Test;

public class NearMissesAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void nearMisses() {
    stubFor(
        get(urlEqualTo("/mypath"))
            .withHeader("My-Header", equalTo("matched"))
            .willReturn(aResponse().withStatus(200)));
    stubFor(
        get(urlEqualTo("/otherpath"))
            .withHeader("My-Header", equalTo("otherheaderval"))
            .willReturn(aResponse().withStatus(200)));
    stubFor(
        get(urlEqualTo("/yet/another/path"))
            .withHeader("X-Alt-Header", equalTo("matchonthis"))
            .willReturn(aResponse().withStatus(200)));

    testClient.get("/otherpath", withHeader("My-Header", "notmatched"));

    List<NearMiss> nearMisses = WireMock.findNearMissesForAllUnmatched();

    assertThat(nearMisses.get(0).getRequest().getUrl(), is("/otherpath"));
    assertThat(nearMisses.get(1).getRequest().getUrl(), is("/otherpath"));
    assertThat(nearMisses.get(2).getRequest().getUrl(), is("/otherpath"));

    assertThat(nearMisses.get(0).getStubMapping().getRequest().getUrl(), is("/otherpath"));
    assertThat(nearMisses.get(1).getStubMapping().getRequest().getUrl(), is("/yet/another/path"));
    assertThat(nearMisses.get(2).getStubMapping().getRequest().getUrl(), is("/mypath"));
  }

  @Test
  public void returnsAllUnmatchedRequests() {
    stubFor(
        get(urlEqualTo("/mypath"))
            .withHeader("My-Header", equalTo("matched"))
            .willReturn(aResponse().withStatus(200)));

    testClient.get("/unmatched/path");

    List<LoggedRequest> unmatched = WireMock.findUnmatchedRequests();

    assertThat(unmatched.size(), is(1));
    assertThat(unmatched.get(0).getUrl(), is("/unmatched/path"));
  }

  @Test
  public void returnsStubMappingNearMissesForARequest() {
    stubFor(
        get(urlEqualTo("/mypath"))
            .withHeader("My-Header", equalTo("matched"))
            .willReturn(aResponse().withStatus(200)));
    stubFor(
        get(urlEqualTo("/otherpath"))
            .withHeader("My-Header", equalTo("otherheaderval"))
            .willReturn(aResponse().withStatus(200)));
    stubFor(
        get(urlEqualTo("/yet/another/path"))
            .withHeader("X-Alt-Header", equalTo("matchonthis"))
            .willReturn(aResponse().withStatus(200)));

    List<NearMiss> nearMisses =
        WireMock.findNearMissesFor(
            LoggedRequest.createFrom(
                mockRequest().url("/otherpath").header("My-Header", "notmatched")));

    assertThat(nearMisses.get(0).getRequest().getUrl(), is("/otherpath"));
    assertThat(nearMisses.get(1).getRequest().getUrl(), is("/otherpath"));
    assertThat(nearMisses.get(2).getRequest().getUrl(), is("/otherpath"));

    assertThat(nearMisses.get(0).getStubMapping().getRequest().getUrl(), is("/otherpath"));
    assertThat(nearMisses.get(1).getStubMapping().getRequest().getUrl(), is("/yet/another/path"));
    assertThat(nearMisses.get(2).getStubMapping().getRequest().getUrl(), is("/mypath"));
  }

  @Test
  public void returnsRequestNearMissesForARequestPattern() {
    testClient.get("/actual11");
    testClient.get("/actual42");

    List<NearMiss> nearMisses =
        WireMock.findNearMissesFor(
            getRequestedFor(urlEqualTo("/actual4")).withRequestBody(containing("thing")));

    assertThat(nearMisses.size(), is(2));
    assertThat(nearMisses.get(0).getRequest().getUrl(), is("/actual42"));
    assertThat(nearMisses.get(1).getRequest().getUrl(), is("/actual11"));
  }
}
