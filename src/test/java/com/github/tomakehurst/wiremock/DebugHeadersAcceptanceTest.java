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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import java.util.UUID;
import org.junit.Test;

public class DebugHeadersAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void returnsMatchedStubIdHeaderWhenStubMatched() {
    UUID stubId = UUID.randomUUID();
    wireMockServer.stubFor(get("/the-match").withId(stubId).willReturn(ok()));

    WireMockResponse response = testClient.get("/the-match");

    assertThat(response.firstHeader("Matched-Stub-Id"), is(stubId.toString()));
    assertThat(response.firstHeader("Matched-Stub-Name"), nullValue());
  }

  @Test
  public void returnsMatchedStubNameHeaderWhenNamedStubMatched() {
    UUID stubId = UUID.randomUUID();
    String name = "My Stub";

    wireMockServer.stubFor(get("/the-match").withId(stubId).withName(name).willReturn(ok()));

    WireMockResponse response = testClient.get("/the-match");

    assertThat(response.firstHeader("Matched-Stub-Id"), is(stubId.toString()));
    assertThat(response.firstHeader("Matched-Stub-Name"), is(name));
  }

  @Test
  public void doesNotReturnEitherHeaderIfNoStubMatched() {
    WireMockResponse response = testClient.get("/the-non-match");

    assertThat(response.firstHeader("Matched-Stub-Id"), nullValue());
    assertThat(response.firstHeader("Matched-Stub-Name"), nullValue());
  }
}
