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
package com.github.tomakehurst.wiremock.verification.diff;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.http.RequestMethod.*;
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static com.github.tomakehurst.wiremock.verification.diff.JUnitStyleDiffRenderer.junitStyleDiffMessage;
import static java.lang.System.lineSeparator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.Test;

public class DiffTest {

  @Test
  public void correctlyRendersJUnitStyleDiffMessage() {
    String diff = junitStyleDiffMessage("expected", "actual");

    assertThat(diff, is(" expected:<\nexpected> but was:<\nactual>"));
  }

  @Test
  public void showsDiffForNonMatchingRequestMethod() {
    Diff diff =
        new Diff(
            newRequestPattern(GET, urlEqualTo("/thing")).build(),
            mockRequest().method(POST).url("/thing"));

    assertThat(
        diff.toString(), is(junitStyleDiffMessage("GET\n" + "/thing\n", "POST\n" + "/thing\n")));
  }

  @Test
  public void showsDiffForUrlEqualTo() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/expected")).build(), mockRequest().url("/actual"));

    assertThat(
        diff.toString(), is(junitStyleDiffMessage("ANY\n" + "/expected\n", "ANY\n" + "/actual\n")));
  }

  @Test
  public void showsDiffForUrlPathMatching() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlPathMatching("/expected/.*")).build(),
            mockRequest().url("/actual"));

    assertThat(
        diff.toString(),
        is(junitStyleDiffMessage("ANY\n" + "[path regex] /expected/.*\n", "ANY\n" + "/actual\n")));
  }

  @Test
  public void showsDiffsForSingleNonMatchingHeaderAndMatchingHeader() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("X-My-Header", equalTo("expected"))
                .build(),
            mockRequest()
                .url("/thing")
                .header("Content-Type", "application/json")
                .header("X-My-Header", "actual"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "ANY\n"
                    + "/thing\n"
                    + "\n"
                    + "Content-Type: application/json\n"
                    + "X-My-Header: expected\n",
                "ANY\n"
                    + "/thing\n"
                    + "\n"
                    + "Content-Type: application/json\n"
                    + "X-My-Header: actual\n")));
  }

  @Test
  public void showsDiffWhenRequestHeaderIsAbsent() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing"))
                .withHeader("X-My-Header", equalTo("expected"))
                .build(),
            mockRequest().url("/thing"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "ANY\n" + "/thing\n" + "\n" + "X-My-Header: expected\n",
                "ANY\n" + "/thing\n" + "\n" + "\n")));
  }

  @Test
  public void showsHeaders() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing"))
                .withHeader("X-My-Header", equalTo("expected"))
                .build(),
            mockRequest().url("/thing"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "ANY\n" + "/thing\n" + "\n" + "X-My-Header: expected\n",
                "ANY\n" + "/thing\n\n\n")));
  }

  @Test
  public void showsRequestBody() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing"))
                .withRequestBody(
                    equalToJson(
                        "{\n"
                            + "    \"outer\": {\n"
                            + "        \"inner\": {\n"
                            + "            \"thing\": 1\n"
                            + "        }\n"
                            + "    }\n"
                            + "}"))
                .build(),
            mockRequest().url("/thing").body("{\n" + "    \"outer\": {}\n" + "}"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "ANY\n"
                    + "/thing\n"
                    + "\n"
                    + "{"
                    + lineSeparator()
                    + "  \"outer\" : {"
                    + lineSeparator()
                    + "    \"inner\" : {"
                    + lineSeparator()
                    + "      \"thing\" : 1"
                    + lineSeparator()
                    + "    }"
                    + lineSeparator()
                    + "  }"
                    + lineSeparator()
                    + "}",
                "ANY\n"
                    + "/thing\n"
                    + "\n"
                    + "{"
                    + lineSeparator()
                    + "  \"outer\" : { }"
                    + lineSeparator()
                    + "}")));
  }

  @Test
  public void prettyPrintsJsonRequestBody() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing"))
                .withRequestBody(equalToJson("{\"outer\": {\"inner:\": {\"thing\": 1}}}"))
                .build(),
            mockRequest().url("/thing").body("{\"outer\": {}}"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "ANY\n"
                    + "/thing\n"
                    + "\n"
                    + "{"
                    + lineSeparator()
                    + "  \"outer\" : {"
                    + lineSeparator()
                    + "    \"inner:\" : {"
                    + lineSeparator()
                    + "      \"thing\" : 1"
                    + lineSeparator()
                    + "    }"
                    + lineSeparator()
                    + "  }"
                    + lineSeparator()
                    + "}",
                "ANY\n"
                    + "/thing\n"
                    + "\n"
                    + "{"
                    + lineSeparator()
                    + "  \"outer\" : { }"
                    + lineSeparator()
                    + "}")));
  }

  @Test
  public void showsJsonPathExpectations() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing"))
                .withRequestBody(matchingJsonPath("@.notfound"))
                .withRequestBody(matchingJsonPath("@.nothereeither"))
                .build(),
            mockRequest()
                .url("/thing")
                .body(
                    "{\n"
                        + "    \"outer\": {\n"
                        + "        \"inner:\": {\n"
                        + "            \"thing\": 1\n"
                        + "        }\n"
                        + "    }\n"
                        + "}"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "ANY\n" + "/thing\n" + "\n" + "@.notfound\n" + "@.nothereeither",
                "ANY\n"
                    + "/thing\n"
                    + "\n"
                    + "{\n"
                    + "    \"outer\": {\n"
                    + "        \"inner:\": {\n"
                    + "            \"thing\": 1\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n"
                    + "{\n"
                    + "    \"outer\": {\n"
                    + "        \"inner:\": {\n"
                    + "            \"thing\": 1\n"
                    + "        }\n"
                    + "    }\n"
                    + "}")));
  }

  @Test
  public void prettyPrintsXml() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing"))
                .withRequestBody(
                    equalToXml(
                        "<my-elements><one attr-one=\"1111\" /><two /><three /></my-elements>"))
                .build(),
            mockRequest()
                .url("/thing")
                .body("<my-elements><one attr-one=\"2222\" /><two /><three /></my-elements>"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "ANY\n"
                    + "/thing\n"
                    + "\n"
                    + "<my-elements>"
                    + lineSeparator()
                    + "  <one attr-one=\"1111\"/>"
                    + lineSeparator()
                    + "  <two/>"
                    + lineSeparator()
                    + "  <three/>"
                    + lineSeparator()
                    + "</my-elements>"
                    + lineSeparator(),
                "ANY\n"
                    + "/thing\n"
                    + "\n"
                    + "<my-elements>"
                    + lineSeparator()
                    + "  <one attr-one=\"2222\"/>"
                    + lineSeparator()
                    + "  <two/>"
                    + lineSeparator()
                    + "  <three/>"
                    + lineSeparator()
                    + "</my-elements>"
                    + lineSeparator())));
  }

  @Test
  public void showsCookiesInDiffWhenNotMatching() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing"))
                .withCookie("my_cookie", equalTo("expected-cookie"))
                .build(),
            mockRequest().url("/thing").cookie("my_cookie", "actual-cookie"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "ANY\n" + "/thing\n" + "\n" + "Cookie: my_cookie=expected-cookie\n",
                "ANY\n" + "/thing\n" + "\n" + "actual-cookie\n")));
  }

  @Test
  public void showsQueryParametersInDiffWhenNotMatching() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlPathEqualTo("/thing"))
                .withQueryParam("search", equalTo("everything"))
                .build(),
            mockRequest().url("/thing?search=nothing"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "ANY\n" + "/thing?search=nothing\n" + "\n" + "Query: search = everything\n",
                "ANY\n" + "/thing?search=nothing\n" + "\n" + "search: nothing\n")));
  }

  @Test
  public void showsCookiesInDiffAbsentFromRequest() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing"))
                .withCookie("my_cookie", equalTo("expected-cookie"))
                .build(),
            mockRequest().url("/thing"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "ANY\n" + "/thing\n" + "\n" + "Cookie: my_cookie=expected-cookie\n",
                "ANY\n" + "/thing\n\n\n")));
  }

  @Test
  public void indicatesThatAnInlineCustomMatcherDidNotMatch() {
    Diff diff =
        new Diff(
            newRequestPattern(GET, urlEqualTo("/thing"))
                .andMatching(
                    new ValueMatcher<Request>() {
                      @Override
                      public MatchResult match(Request value) {
                        return MatchResult.noMatch();
                      }
                    })
                .build(),
            mockRequest().method(GET).url("/thing"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "GET\n" + "/thing\n" + "\n" + "[custom matcher]", "GET\n" + "/thing\n\n ")));
  }

  @Test
  public void indicatesThatANamedCustomMatcherDidNotMatch() {
    Diff diff =
        new Diff(
            newRequestPattern(GET, urlEqualTo("/thing")).andMatching("my-custom-matcher").build(),
            mockRequest().method(GET).url("/thing"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "GET\n" + "/thing\n" + "\n" + "[custom matcher: my-custom-matcher]",
                "GET\n" + "/thing\n" + "\n" + "[custom matcher: my-custom-matcher]")));
  }

  @Test
  public void handlesAbsentRequestBody() {
    Diff diff =
        new Diff(
            newRequestPattern(POST, urlEqualTo("/thing")).withRequestBody(absent()).build(),
            mockRequest().method(POST).body("not absent").url("/thing"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "POST\n" + "/thing\n\n" + "(absent)", "POST\n" + "/thing\n\n" + "not absent")));
  }

  @Test
  public void indicatesThatScenarioStateDiffersWhenStubAndRequestOtherwiseMatch() {
    Diff diff =
        new Diff(
            get("/stateful")
                .inScenario("my-steps")
                .whenScenarioStateIs("step2")
                .willReturn(ok("Yep"))
                .build(),
            mockRequest().method(GET).url("/stateful"),
            Scenario.STARTED);

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "GET\n" + "/stateful\n\n" + "[Scenario 'my-steps' state: step2]",
                "GET\n" + "/stateful\n\n" + "[Scenario 'my-steps' state: Started]")));
  }

  @Test
  public void includesSpecificDiffForJsonPathSubMatchWhenElementFound() {
    Diff diff =
        new Diff(
            post("/submatch")
                .withRequestBody(matchingJsonPath("$.name", containing("Tom")))
                .willReturn(ok("Yep"))
                .build(),
            mockRequest().method(POST).url("/submatch").body("{\"name\": \"Rob\"}"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "POST\n" + "/submatch\n\n" + "$.name [contains] Tom",
                "POST\n" + "/submatch\n\n" + "Rob")));
  }

  @Test
  public void includesSpecificDiffForJsonPathSubMatchWhenElementNotFound() {
    Diff diff =
        new Diff(
            post("/submatch")
                .withRequestBody(matchingJsonPath("$.name", containing("Tom")))
                .willReturn(ok("Yep"))
                .build(),
            mockRequest().method(POST).url("/submatch").body("{\"id\": \"abc123\"}"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "POST\n" + "/submatch\n\n" + "$.name [contains] Tom",
                "POST\n" + "/submatch\n\n" + "{\"id\": \"abc123\"}")));
  }

  @Test
  public void includeHostnameIfSpecifiedWithEqualTo() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing")).withHost(equalTo("my.host")).build(),
            mockRequest().host("wrong.host").url("/thing"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "my.host\n" + "ANY\n" + "/thing\n", "wrong.host\n" + "ANY\n" + "/thing\n")));
  }

  @Test
  public void includeHostnameIfSpecifiedWithNonEqualTo() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing")).withHost(containing("my.host")).build(),
            mockRequest().host("wrong.host").url("/thing"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "[contains] my.host\n" + "ANY\n" + "/thing\n",
                "wrong.host\n" + "ANY\n" + "/thing\n")));
  }

  @Test
  public void includePortIfSpecified() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing")).withPort(5544).build(),
            mockRequest().port(6543).url("/thing"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "5544\n" + "ANY\n" + "/thing\n", "6543\n" + "ANY\n" + "/thing\n")));
  }

  @Test
  public void includeSchemeIfSpecified() {
    Diff diff =
        new Diff(
            newRequestPattern(ANY, urlEqualTo("/thing")).withScheme("https").build(),
            mockRequest().scheme("http").url("/thing"));

    assertThat(
        diff.toString(),
        is(
            junitStyleDiffMessage(
                "https\n" + "ANY\n" + "/thing\n", "http\n" + "ANY\n" + "/thing\n")));
  }
}
