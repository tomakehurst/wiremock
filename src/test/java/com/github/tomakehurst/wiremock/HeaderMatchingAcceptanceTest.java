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

import static com.github.tomakehurst.wiremock.testsupport.TestHttpHeader.withHeader;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.testsupport.MappingJsonSamples;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import org.junit.Test;

public class HeaderMatchingAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void mappingWithExactUrlMethodAndHeaderMatchingIsCreatedAndReturned() {
    testClient.addResponse(MappingJsonSamples.MAPPING_REQUEST_WITH_EXACT_HEADERS);

    WireMockResponse response =
        testClient.get(
            "/header/dependent",
            withHeader("Accept", "text/xml"),
            withHeader("If-None-Match", "abcd1234"));

    assertThat(response.statusCode(), is(304));
  }

  @Test
  public void mappingMatchedWithRegexHeaders() {
    testClient.addResponse(MappingJsonSamples.MAPPING_REQUEST_WITH_REGEX_HEADERS);

    WireMockResponse response =
        testClient.get(
            "/header/match/dependent",
            withHeader("Accept", "text/xml"),
            withHeader("If-None-Match", "abcd1234"));

    assertThat(response.statusCode(), is(304));
  }

  @Test
  public void mappingMatchedWithNegativeRegexHeader() {
    testClient.addResponse(MappingJsonSamples.MAPPING_REQUEST_WITH_NEGATIVE_REGEX_HEADERS);

    WireMockResponse response =
        testClient.get("/header/match/dependent", withHeader("Accept", "text/xml"));
    assertThat(response.statusCode(), is(HTTP_NOT_FOUND));

    response = testClient.get("/header/match/dependent", withHeader("Accept", "application/json"));
    assertThat(response.statusCode(), is(200));
  }
}
