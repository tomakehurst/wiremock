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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import org.hamcrest.Matchers;
import org.junit.Test;

public class DateHeaderAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void returnsOnlyStubbedDateHeader() {

    stubFor(
        get(urlEqualTo("/stubbed/dateheader"))
            .willReturn(
                aResponse().withStatus(200).withHeader("Date", "Sun, 06 Nov 1994 08:49:37 GMT")));

    WireMockResponse response = testClient.get("/stubbed/dateheader");

    assertThat(response.headers().get("Date"), contains("Sun, 06 Nov 1994 08:49:37 GMT"));
  }

  @Test
  public void returnsNoDateHeaderIfNotStubbed() {

    stubFor(get(urlEqualTo("/nodateheader")).willReturn(aResponse().withStatus(200)));

    WireMockResponse response = testClient.get("/nodateheader");

    assertThat(response.headers().get("Date"), is(Matchers.<String>empty()));
  }
}
