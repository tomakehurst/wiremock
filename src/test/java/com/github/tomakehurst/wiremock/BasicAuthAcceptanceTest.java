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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import org.junit.Test;

public class BasicAuthAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void matchesPreemptiveBasicAuthWhenCredentialAreCorrect() {
    stubFor(
        get(urlEqualTo("/basic/auth/preemptive"))
            .withBasicAuth("the-username", "thepassword")
            .willReturn(aResponse().withStatus(200)));

    WireMockResponse response =
        testClient.getWithPreemptiveCredentials(
            "/basic/auth/preemptive", wireMockServer.port(), "the-username", "thepassword");

    assertThat(response.statusCode(), is(200));
  }

  @Test
  public void doesNotMatchPreemptiveBasicAuthWhenCredentialsAreIncorrect() {
    stubFor(
        get(urlEqualTo("/basic/auth/preemptive"))
            .withBasicAuth("the-username", "thepassword")
            .willReturn(aResponse().withStatus(200)));

    WireMockResponse response =
        testClient.getWithPreemptiveCredentials(
            "/basic/auth/preemptive", wireMockServer.port(), "the-username", "WRONG!!!");

    assertThat(response.statusCode(), is(404));
  }

  @Test
  public void matcheswhenBASICInHeaderIsAllUpperCase() {
    stubFor(
        get(urlEqualTo("/basic/auth/case-insensitive"))
            .withBasicAuth("tom", "secret")
            .willReturn(aResponse().withStatus(200)));

    assertThat(
        testClient
            .get(
                "/basic/auth/case-insensitive",
                withHeader("Authorization", "BASIC dG9tOnNlY3JldA=="))
            .statusCode(),
        is(200));
  }
}
