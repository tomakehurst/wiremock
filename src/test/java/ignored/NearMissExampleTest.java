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
package ignored;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.testsupport.TestHttpHeader.withHeader;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class NearMissExampleTest {

  @Rule
  public WireMockRule wm =
      new WireMockRule(options().dynamicPort().withRootDirectory("src/main/resources/empty"), true);

  WireMockTestClient client;

  @Before
  public void init() {
    client = new WireMockTestClient(wm.port());
  }

  @Test
  public void showFullUnmatchedVerification() throws Exception {
    client.get("/some-other-thing");
    client.get("/totally-something-else");
    client.get("/whatever");
    client.post(
        "/my-near-miss",
        new StringEntity("{\"data\": { \"one\": 1}}", APPLICATION_JSON),
        withHeader("Content-Type", "application/json"),
        withHeader("X-Expected", "yes"),
        withHeader("X-Matched-1", "yes"),
        withHeader("Cookie", "this=that"),
        withHeader(
            "Authorization",
            new BasicCredentials("user", "wrong-pass").asAuthorizationHeaderValue()));

    wm.verify(
        postRequestedFor(urlEqualTo("/a-near-miss"))
            .withHeader("Content-Type", equalTo("text/json"))
            .withHeader("X-Expected", equalTo("yes"))
            .withHeader("X-Matched-1", matching("ye.*"))
            .withHeader("X-Matched-2", containing("no"))
            .withCookie("this", equalTo("other"))
            .withBasicAuth(new BasicCredentials("user", "pass"))
            .withRequestBody(equalToJson("{\"data\": { \"two\": 1}}")));
  }

  @Test
  public void showSingleUnmatchedRequest() {
    wm.stubFor(get(urlEqualTo("/hit")).willReturn(aResponse().withStatus(200)));
    client.get("/near-misssss");
  }

  @Test
  public void showManyUnmatchedRequests() {
    wm.stubFor(get(urlEqualTo("/hit")).willReturn(aResponse().withStatus(200)));
    client.get("/near-misssss");
    client.get("/hat");
    client.get("/whatevs");
  }
}
