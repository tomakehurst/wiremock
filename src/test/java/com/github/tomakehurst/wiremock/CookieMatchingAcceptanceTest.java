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
import static com.github.tomakehurst.wiremock.testsupport.TestHttpHeader.withHeader;
import static com.google.common.net.HttpHeaders.COOKIE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import org.junit.Test;

public class CookieMatchingAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void matchesOnWellFormedCookie() {
    stubFor(
        get(urlEqualTo("/good/cookie"))
            .withCookie("my_cookie", containing("mycookievalue"))
            .willReturn(aResponse().withStatus(200)));

    WireMockResponse response =
        testClient.get("/good/cookie", withHeader(COOKIE, "my_cookie=xxx-mycookievalue-xxx"));

    assertThat(response.statusCode(), is(200));
  }

  @Test
  public void matchesWhenMultipleCookiesAreSentAndRequired() {
    stubFor(
        get(urlEqualTo("/good/cookies"))
            .withCookie("my_cookie", containing("mycookievalue"))
            .withCookie("my_other_cookie", equalTo("exact-other-value"))
            .willReturn(aResponse().withStatus(200)));

    WireMockResponse response =
        testClient.get(
            "/good/cookies",
            withHeader(
                COOKIE,
                "my_cookie=xxx-mycookievalue-xxx; my_other_cookie=exact-other-value; irrelevant_cookie=whatever"));

    assertThat(response.statusCode(), is(200));
  }

  @Test
  public void doesNotMatchWhenExpectedCookieIsAbsent() {
    stubFor(
        get(urlEqualTo("/missing/cookie"))
            .withCookie("my_cookie", containing("mycookievalue"))
            .willReturn(aResponse().withStatus(200)));

    WireMockResponse response =
        testClient.get(
            "/missing/cookie", withHeader(COOKIE, "the_wrong_cookie=xxx-mycookievalue-xxx"));

    assertThat(response.statusCode(), is(404));
  }

  @Test
  public void doesNotMatchWhenExpectedCookieHasTheWrongValue() {
    stubFor(
        get(urlEqualTo("/bad/cookie"))
            .withCookie("my_cookie", containing("mycookievalue"))
            .willReturn(aResponse().withStatus(200)));

    WireMockResponse response =
        testClient.get("/bad/cookie", withHeader(COOKIE, "my_cookie=youwontfindthis"));

    assertThat(response.statusCode(), is(404));
  }

  @Test
  public void doesNotMatchWhenExpectedCookieIsMalformed() {
    stubFor(
        get(urlEqualTo("/very-bad/cookie"))
            .withCookie("my_cookie", containing("mycookievalue"))
            .willReturn(aResponse().withStatus(200)));

    WireMockResponse response =
        testClient.get(
            "/very-bad/cookie", withHeader(COOKIE, "my_cookieyouwontfindthis;;sldfjskldjf%%"));

    assertThat(response.statusCode(), is(404));
  }

  @Test
  public void matchesWhenRequiredAbsentCookieIsAbsent() {
    stubFor(
        get(urlEqualTo("/absent/cookie"))
            .withCookie("not_this_cookie", absent())
            .willReturn(aResponse().withStatus(200)));

    WireMockResponse response =
        testClient.get(
            "/absent/cookie",
            withHeader(
                COOKIE,
                "my_cookie=xxx-mycookievalue-xxx; my_other_cookie=exact-other-value; irrelevant_cookie=whatever"));

    assertThat(response.statusCode(), is(200));
  }

  @Test
  public void doesNotMatchWhenRequiredAbsentCookieIsPresent() {
    stubFor(
        get(urlEqualTo("/absent/cookie"))
            .withCookie("my_cookie", absent())
            .willReturn(aResponse().withStatus(200)));

    WireMockResponse response =
        testClient.get(
            "/absent/cookie",
            withHeader(
                COOKIE,
                "my_cookie=xxx-mycookievalue-xxx; my_other_cookie=exact-other-value; irrelevant_cookie=whatever"));

    assertThat(response.statusCode(), is(404));
  }

  @Test
  public void revealsCookiesInLoggedRequests() {
    testClient.get(
        "/good/cookies",
        withHeader(
            COOKIE,
            "my_cookie=xxx-mycookievalue-xxx; my_other_cookie=exact-other-value; irrelevant_cookie=whatever"));

    List<LoggedRequest> requests = findAll(getRequestedFor(urlEqualTo("/good/cookies")));

    assertThat(requests.size(), is(1));
    assertThat(requests.get(0).getCookies().keySet(), hasItem("my_other_cookie"));
  }

  @Test
  public void matchesWhenRequiredCookieSentAsDuplicate() {
    stubFor(
        get(urlEqualTo("/duplicate/cookie"))
            .withCookie("my_cookie", containing("mycookievalue"))
            .withCookie("my_other_cookie", equalTo("value-2"))
            .willReturn(aResponse().withStatus(200)));

    WireMockResponse response =
        testClient.get(
            "/duplicate/cookie",
            withHeader(
                COOKIE,
                "my_cookie=xxx-mycookievalue-xxx; my_other_cookie=value-1; my_other_cookie=value-2"));

    assertThat(response.statusCode(), is(200));
  }
}
