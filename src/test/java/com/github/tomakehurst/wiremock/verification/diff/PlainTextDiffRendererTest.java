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
import static com.github.tomakehurst.wiremock.common.Json.prettyPrint;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.github.tomakehurst.wiremock.matching.MockMultipart.mockPart;
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static com.github.tomakehurst.wiremock.testsupport.TestFiles.file;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.equalsMultiLine;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import java.util.Collections;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Test;

public class PlainTextDiffRendererTest {

  PlainTextDiffRenderer diffRenderer;

  @Before
  public void init() {
    diffRenderer =
        new PlainTextDiffRenderer(
            Collections.<String, RequestMatcherExtension>singletonMap(
                "my-custom-matcher", new MyCustomMatcher()));
  }

  @Test
  public void rendersWithDifferingUrlHeaderAndJsonBody() {
    Diff diff =
        new Diff(
            post("/thing")
                .withName(
                    "The post stub with a really long name that ought to wrap and let us see exactly how that looks when it is done")
                .withHeader("X-My-Header", containing("correct value"))
                .withHeader("Accept", matching("text/plain.*"))
                .withRequestBody(
                    equalToJson(
                        "{     \n"
                            + "    \"thing\": {               \n"
                            + "        \"stuff\": [1, 2, 3]   \n"
                            + "    }                          \n"
                            + "}"))
                .build(),
            mockRequest()
                .method(POST)
                .url("/thin")
                .header("X-My-Header", "wrong value")
                .header("Accept", "text/plain")
                .body(
                    "{                        \n"
                        + "    \"thing\": {           \n"
                        + "        \"nothing\": {}    \n"
                        + "    }                      \n"
                        + "}"));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    assertThat(output, equalsMultiLine(file("not-found-diff-sample_ascii.txt")));
  }

  @Test
  public void rendersWithDifferingCookies() {
    Diff diff =
        new Diff(
            post("/thing")
                .withName("Cookie diff")
                .withCookie("Cookie_1", containing("one value"))
                .withCookie("Second_Cookie", matching("cookie two value [0-9]*"))
                .build(),
            mockRequest()
                .method(POST)
                .url("/thing")
                .cookie("Cookie_1", "zero value")
                .cookie("Second_Cookie", "cookie two value 123"));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    assertThat(output, equalsMultiLine(file("not-found-diff-sample_cookies.txt")));
  }

  @Test
  public void rendersWithDifferingQueryParameters() {
    Diff diff =
        new Diff(
            get(urlPathEqualTo("/thing"))
                .withName("Query params diff")
                .withQueryParam("one", equalTo("1"))
                .withQueryParam("two", containing("two things"))
                .withQueryParam("three", matching("[a-z]{5}"))
                .build(),
            mockRequest().method(GET).url("/thing?one=2&two=wrong%20things&three=abcde"));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    assertThat(output, equalsMultiLine(file("not-found-diff-sample_query.txt")));
  }

  @Test
  public void wrapsLargeJsonBodiesAppropriately() {
    Diff diff =
        new Diff(
            post("/thing")
                .withName(
                    "The post stub with a really long name that ought to wrap and let us see exactly how that looks when it is done")
                .withHeader("Accept", equalTo("text/plain"))
                .withRequestBody(
                    equalToJson(
                        "{\n"
                            + "  \"one\": {\n"
                            + "    \"two\": {\n"
                            + "      \"three\": {\n"
                            + "        \"four\": {\n"
                            + "          \"five\": {\n"
                            + "            \"six\": \"superduperlongvaluethatshouldwrapokregardless_superduperlongvaluethatshouldwrapokregardless_superduperlongvaluethatshouldwrapokregardless_superduperlongvaluethatshouldwrapokregardless\"\n"
                            + "          }\n"
                            + "        }\n"
                            + "      }\n"
                            + "    }\n"
                            + "  }\n"
                            + "}"))
                .build(),
            mockRequest()
                .method(POST)
                .url("/thing")
                .header("Accept", "text/plain")
                .body(
                    prettyPrint(
                        "{\n"
                            + "  \"one\": {\n"
                            + "    \"two\": {\n"
                            + "      \"three\": {\n"
                            + "        \"four\": {\n"
                            + "          \"five\": {\n"
                            + "            \"six\": \"totally_the_wrong_value\"\n"
                            + "          }\n"
                            + "        }\n"
                            + "      }\n"
                            + "    }\n"
                            + "  }\n"
                            + "}")));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    // Ugh. The joys of Microsoft's line ending innovations.
    String expected =
        SystemUtils.IS_OS_WINDOWS
            ? file("not-found-diff-sample_large_json_windows.txt")
            : file("not-found-diff-sample_large_json.txt");
    assertThat(output, equalsMultiLine(expected));
  }

  @Test
  public void wrapsLargeXmlBodiesAppropriately() {
    Diff diff =
        new Diff(
            post("/thing")
                .withName(
                    "The post stub with a really long name that ought to wrap and let us see exactly how that looks when it is done")
                .withRequestBody(
                    equalToXml(
                        "<deep-things>\n"
                            + "  <thing id=\"1\">\n"
                            + "    <thing id=\"2\">\n"
                            + "      <thing id=\"3\">\n"
                            + "        <thing id=\"4\">\n"
                            + "          <thing id=\"5\">\n"
                            + "            <thing id=\"6\">\n"
                            + "              Super wrong bit of text that should push it way over the length limit!\n"
                            + "            </thing>\n"
                            + "          </thing>\n"
                            + "        </thing>\n"
                            + "      </thing>\n"
                            + "    </thing>\n"
                            + "  </thing>\n"
                            + "</deep-things>"))
                .build(),
            mockRequest()
                .method(POST)
                .url("/thing")
                .body(
                    "<deep-things>\n"
                        + "  <thing id=\"1\">\n"
                        + "    <thing id=\"2\">\n"
                        + "      <thing id=\"3\">\n"
                        + "        <thing id=\"4\">\n"
                        + "          <thing id=\"5\">\n"
                        + "            <thing id=\"6\">\n"
                        + "              Super long bit of text that should push it way over the length limit!\n"
                        + "            </thing>\n"
                        + "          </thing>\n"
                        + "        </thing>\n"
                        + "      </thing>\n"
                        + "    </thing>\n"
                        + "  </thing>\n"
                        + "</deep-things>"));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    assertThat(output, equalsMultiLine(file("not-found-diff-sample_large_xml.txt")));
  }

  @Test
  public void showsMissingHeaderMessage() {
    Diff diff =
        new Diff(
            post("/thing")
                .withName("Missing header stub")
                .withHeader("X-My-Header", equalTo("correct value"))
                .build(),
            mockRequest().method(POST).url("/thing"));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    assertThat(output, equalsMultiLine(file("not-found-diff-sample_missing_header.txt")));
  }

  @Test
  public void showsJsonPathMismatch() {
    Diff diff =
        new Diff(
            post("/thing")
                .withRequestBody(matchingJsonPath("$..six"))
                .withRequestBody(matchingJsonPath("$..seven"))
                .build(),
            mockRequest()
                .method(POST)
                .url("/thing")
                .body(
                    prettyPrint(
                        "{\n"
                            + "  \"one\": {\n"
                            + "    \"two\": {\n"
                            + "      \"three\": {\n"
                            + "        \"four\": {\n"
                            + "          \"five\": {\n"
                            + "            \"six\": \"match this\"\n"
                            + "          }\n"
                            + "        }\n"
                            + "      }\n"
                            + "    }\n"
                            + "  }\n"
                            + "}")));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    String expected = file("not-found-diff-sample_json-path.txt");
    assertThat(output, equalsMultiLine(expected));
  }

  @Test
  public void showsXPathWithSubMatchMismatch() {
    Diff diff =
        new Diff(
            post("/thing").withRequestBody(matchingXPath("//thing/text()", equalTo("two"))).build(),
            mockRequest()
                .method(POST)
                .url("/thing")
                .body("<stuff>\n" + "    <thing>one</thing>\n" + "</stuff>"));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    String expected = file("not-found-diff-sample_xpath-with-submatch.txt");
    assertThat(output, equalsMultiLine(expected));
  }

  @Test
  public void showsUrlRegexUnescapedMessage() {
    Diff diff =
        new Diff(
            get(urlMatching("thing?query=value")).build(), mockRequest().method(GET).url("/thing"));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    assertThat(output, equalsMultiLine(file("not-found-diff-sample_url-pattern.txt")));
  }

  @Test
  public void showsMultipartDifference() {
    Diff diff =
        new Diff(
            post("/thing")
                .withName("Multipart request body stub")
                .withMultipartRequestBody(
                    aMultipart()
                        .withName("part_one")
                        .withHeader("X-My-Stuff", containing("stuff_parts"))
                        .withBody(matching("Some expected text.*")))
                .withMultipartRequestBody(
                    aMultipart()
                        .withHeader("X-More", containing("stuff_parts"))
                        .withBody(equalTo("Correct body")))
                .build(),
            mockRequest()
                .method(POST)
                .url("/thing")
                .header("Content-Type", "multipart/form-data")
                .part(
                    mockPart()
                        .name("part_one")
                        .header("X-My-Stuff", "wrong value")
                        .body("Wrong body"))
                .part(mockPart().name("part_two").body("Correct body")));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    assertThat(output, equalsMultiLine(file("not-found-diff-sample_multipart.txt")));
  }

  @Test
  public void showsErrorInDiffWhenMultipartExpectedButNotSent() {
    Diff diff =
        new Diff(
            post("/thing")
                .withName("Multipart request body stub")
                .withMultipartRequestBody(
                    aMultipart()
                        .withHeader("X-My-Stuff", containing("stuff_parts"))
                        .withBody(matching("Some expected text.*")))
                .build(),
            mockRequest().method(POST).url("/thing").body("Non-multipart body"));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    assertThat(output, equalsMultiLine(file("not-found-diff-sample_no-multipart.txt")));
  }

  @Test
  public void showsErrorInDiffWhenInlineCustomMatcherNotSatisfiedInMixedStub() {
    Diff diff =
        new Diff(
            post("/thing")
                .withName("Standard and custom matched stub")
                .andMatching(
                    new ValueMatcher<Request>() {
                      @Override
                      public MatchResult match(Request value) {
                        return MatchResult.noMatch();
                      }
                    })
                .build(),
            mockRequest().method(POST).url("/thing"));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    assertThat(output, equalsMultiLine(file("not-found-diff-sample_mixed-matchers.txt")));
  }

  @Test
  public void showsErrorInDiffWhenNamedCustomMatcherNotSatisfiedInMixedStub() {
    Diff diff =
        new Diff(
            post("/thing")
                .withName("Standard and custom matched stub")
                .andMatching("my-custom-matcher", Parameters.one("myVal", "present"))
                .build(),
            mockRequest().method(POST).url("/thing"));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    assertThat(
        output, equalsMultiLine(file("not-found-diff-sample_mixed-matchers-named-custom.txt")));
  }

  @Test
  public void showsAppropriateErrorInDiffWhenCustomMatcherIsUsedExclusively() {
    Diff diff =
        new Diff(
            requestMatching(
                    new ValueMatcher<Request>() {
                      @Override
                      public MatchResult match(Request value) {
                        return MatchResult.noMatch();
                      }
                    })
                .build(),
            mockRequest().method(POST).url("/thing"));

    String output = diffRenderer.render(diff);
    System.out.println(output);

    assertThat(output, equalsMultiLine(file("not-found-diff-sample_only-custom_matcher.txt")));
  }

  @Test
  public void handlesUrlsWithQueryStringAndNoPath() {
    Diff diff =
        new Diff(
            newRequestPattern(GET, urlMatching("/?q=correct")).build(),
            mockRequest().method(GET).url("/q=wrong"));

    String output = diffRenderer.render(diff);
    System.out.println(output);
  }

  public static class MyCustomMatcher extends RequestMatcherExtension {

    @Override
    public MatchResult match(Request request, Parameters parameters) {
      parameters.getString("myVal"); // Ensure we're getting passed parameters as expcted
      return MatchResult.noMatch();
    }

    @Override
    public String getName() {
      return "my-custom-matcher";
    }
  }
}
