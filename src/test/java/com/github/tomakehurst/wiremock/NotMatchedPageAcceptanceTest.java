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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.testsupport.TestFiles.file;
import static com.github.tomakehurst.wiremock.testsupport.TestHttpHeader.withHeader;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.equalsMultiLine;
import static com.github.tomakehurst.wiremock.verification.notmatched.PlainTextStubNotMatchedRenderer.CONSOLE_WIDTH_HEADER_KEY;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Gzip;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.requestfilter.FieldTransformer;
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilterAction;
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestWrapper;
import com.github.tomakehurst.wiremock.extension.requestfilter.StubRequestFilter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import com.github.tomakehurst.wiremock.verification.notmatched.NotMatchedRenderer;
import java.util.List;
import org.apache.http.entity.ByteArrayEntity;
import org.junit.After;
import org.junit.Test;

public class NotMatchedPageAcceptanceTest {

  WireMockServer wm;
  WireMockTestClient testClient;

  @After
  public void stop() {
    wm.stop();
  }

  @Test
  public void rendersAPlainTextDiffWhenStubNotMatchedAndANearMissIsAvailable() {
    configure();

    stubFor(
        post("/thing")
            .withName(
                "The post stub with a really long name that ought to wrap and let us see exactly how that looks when it is done")
            .withHeader("X-My-Header", containing("correct value"))
            .withHeader("Accept", matching("text/plain.*"))
            .withRequestBody(
                equalToJson(
                    "{                              \n"
                        + "    \"thing\": {               \n"
                        + "        \"stuff\": [1, 2, 3]   \n"
                        + "    }                          \n"
                        + "}"))
            .willReturn(ok()));

    WireMockResponse response =
        testClient.postJson(
            "/thin",
            "{                        \n"
                + "    \"thing\": {           \n"
                + "        \"nothing\": {}    \n"
                + "    }                      \n"
                + "}",
            withHeader("X-My-Header", "wrong value"),
            withHeader("Accept", "text/plain"));

    assertThat(response.content(), equalsMultiLine(file("not-found-diff-sample_ascii.txt")));
  }

  @Test
  public void adjustsWidthWhenConsoleWidthHeaderSpecified() {
    configure();

    stubFor(
        post("/thing")
            .withName(
                "The post stub with a really long name that ought to wrap and let us see exactly how that looks when it is done")
            .withHeader("X-My-Header", containing("correct value"))
            .withHeader("Accept", matching("text/plain.*"))
            .withRequestBody(
                equalToJson(
                    "{                              \n"
                        + "    \"thing\": {               \n"
                        + "        \"stuff\": [1, 2, 3]   \n"
                        + "    }                          \n"
                        + "}"))
            .willReturn(ok()));

    WireMockResponse response =
        testClient.postJson(
            "/thin",
            "{                        \n"
                + "    \"thing\": {           \n"
                + "        \"nothing\": {}    \n"
                + "    }                      \n"
                + "}",
            withHeader("X-My-Header", "wrong value"),
            withHeader("Accept", "text/plain"),
            withHeader(CONSOLE_WIDTH_HEADER_KEY, "69"));

    System.out.println(response.content());
    assertThat(response.content(), equalsMultiLine(file("not-found-diff-sample_ascii-narrow.txt")));
  }

  @Test
  public void rendersAPlainTextDiffWhenRequestIsOnlyUrlAndMethod() {
    configure();

    stubFor(get("/another-url").withRequestBody(absent()).willReturn(ok()));

    WireMockResponse response = testClient.get("/gettable");

    assertThat(response.statusCode(), is(404));
  }

  @Test
  public void showsADefaultMessageWhenNoStubsWerePresent() {
    configure();

    WireMockResponse response = testClient.get("/no-stubs-to-match");

    assertThat(response.statusCode(), is(404));
    assertThat(response.firstHeader(CONTENT_TYPE), is("text/plain"));
    assertThat(
        response.content(),
        is("No response could be served as there are no stub mappings in this WireMock instance."));
  }

  @Test
  public void supportsCustomNoMatchRenderer() {
    configure(
        wireMockConfig()
            .notMatchedRenderer(
                new NotMatchedRenderer() {
                  @Override
                  protected ResponseDefinition render(Admin admin, Request request) {
                    return ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(403)
                        .withBody("No you don't!")
                        .build();
                  }
                }));

    WireMockResponse response = testClient.get("/should-not-match");

    assertThat(response.statusCode(), is(403));
    assertThat(response.content(), is("No you don't!"));
  }

  @Test
  public void returns404AndDiffReportWhenPlusSymbolInQuery() {
    configure();

    WireMockResponse response =
        testClient.get("/some/api/records?sort=updated+asc&filter_updated_gt=2019-01-02");
    System.err.println(response.content());

    assertThat(response.statusCode(), is(404));
    assertThat(response.content(), containsString("No response could be served"));
  }

  @Test
  public void indicatesWhenWrongScenarioStateIsTheReasonForNonMatch() {
    configure();

    stubFor(
        post("/thing")
            .inScenario("thing states")
            .whenScenarioStateIs("first")
            .willReturn(ok("Done!")));

    WireMockResponse response = testClient.postJson("/thing", "{}");

    assertThat(
        response.content(), equalsMultiLine(file("not-found-diff-sample_scenario-state.txt")));
  }

  @Test
  public void showsDescriptiveDiffLineForLogicalOrWithAbsent() {
    configure();

    stubFor(
        get(urlPathEqualTo("/or"))
            .withHeader("X-Maybe", equalTo("one").or(absent()))
            .willReturn(ok()));

    WireMockResponse response = testClient.get("/or", withHeader("X-Maybe", "wrong"));

    assertThat(response.statusCode(), is(404));
    assertThat(response.content(), equalsMultiLine(file("not-found-diff-sample-logical-or.txt")));
  }

  @Test
  public void requestValuesTransformedByRequestFilterAreShownInDiff() {
    configure(
        wireMockConfig()
            .extensions(
                new StubRequestFilter() {
                  @Override
                  public RequestFilterAction filter(Request request) {
                    Request wrappedRequest =
                        RequestWrapper.create()
                            .transformHeader(
                                "X-My-Header",
                                new FieldTransformer<List<String>>() {
                                  @Override
                                  public List<String> transform(List<String> source) {
                                    return singletonList("modified value");
                                  }
                                })
                            .wrap(request);
                    return RequestFilterAction.continueWith(wrappedRequest);
                  }

                  @Override
                  public String getName() {
                    return "thing-changer-filter";
                  }
                }));

    stubFor(get("/filter").withHeader("X-My-Header", equalTo("original value")).willReturn(ok()));

    WireMockResponse response =
        testClient.get("/filter", withHeader("X-My-Header", "original value"));

    assertThat(response.statusCode(), is(404));
    assertThat(response.content(), containsString("| X-My-Header: modified value"));
  }

  @Test
  public void showsNotFoundDiffMessageForNonStandardHttpMethods() {
    configure();
    stubFor(request("PAAARP", urlPathEqualTo("/pip")).willReturn(ok()));

    WireMockResponse response = testClient.request("PAAARP", "/pop");

    assertThat(response.statusCode(), is(404));
    assertThat(response.content(), containsString("Request was not matched"));
  }

  @Test
  public void showsNotFoundDiffMessageWhenRequestBodyIsGZipped() {
    configure();
    stubFor(
        post(urlPathEqualTo("/gzip"))
            .withHeader("Content-Encoding", equalToIgnoreCase("gzip"))
            .withRequestBody(equalToJson("{\"id\":\"ok\"}"))
            .willReturn(ok()));

    ByteArrayEntity entity = new ByteArrayEntity(Gzip.gzip("{\"id\":\"wrong\"}"));
    WireMockResponse response =
        testClient.post("/gzip", entity, withHeader("Content-Encoding", "gzip"));

    assertThat(response.statusCode(), is(404));
    assertThat(response.content(), containsString("Request was not matched"));
  }

  private void configure() {
    configure(wireMockConfig().dynamicPort());
  }

  private void configure(WireMockConfiguration options) {
    options.dynamicPort().withRootDirectory("src/test/resources/empty");
    wm = new WireMockServer(options);
    wm.start();
    testClient = new WireMockTestClient(wm.port());
    WireMock.configureFor(wm.port());
  }
}
