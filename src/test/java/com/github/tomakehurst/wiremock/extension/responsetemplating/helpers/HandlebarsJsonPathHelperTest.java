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
package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static com.github.tomakehurst.wiremock.testsupport.NoFileSource.noFileSource;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.equalToJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class HandlebarsJsonPathHelperTest extends HandlebarsHelperTestBase {

  private HandlebarsJsonPathHelper helper;

  @Before
  public void init() {
    helper = new HandlebarsJsonPathHelper();
    LocalNotifier.set(new ConsoleNotifier(true));
  }

  @Test
  public void mergesASimpleValueFromRequestIntoResponseBody() {
    final ResponseDefinition responseDefinition =
        this.transformer.transform(
            mockRequest().url("/json").body("{\"a\": {\"test\": \"success\"}}"),
            aResponse().withBody("{\"test\": \"{{jsonPath request.body '$.a.test'}}\"}").build(),
            noFileSource(),
            Parameters.empty());

    assertThat(responseDefinition.getBody(), is("{\"test\": \"success\"}"));
  }

  @Test
  public void incluesAnErrorInTheResponseBodyWhenTheJsonPathIsInvalid() {
    final ResponseDefinition responseDefinition =
        this.transformer.transform(
            mockRequest().url("/json").body("{\"a\": {\"test\": \"success\"}}"),
            aResponse().withBody("{\"test\": \"{{jsonPath request.body '$![bbb'}}\"}").build(),
            noFileSource(),
            Parameters.empty());

    assertThat(
        responseDefinition.getBody(), startsWith("{\"test\": \"" + HandlebarsHelper.ERROR_PREFIX));
  }

  @Test
  public void listResultFromJsonPathQueryCanBeUsedByHandlebarsEachHelper() {
    final ResponseDefinition responseDefinition =
        this.transformer.transform(
            mockRequest()
                .url("/json")
                .body(
                    "{\n"
                        + "    \"items\": [\n"
                        + "        {\n"
                        + "            \"name\": \"One\"\n"
                        + "        },\n"
                        + "        {\n"
                        + "            \"name\": \"Two\"\n"
                        + "        },\n"
                        + "        {\n"
                        + "            \"name\": \"Three\"\n"
                        + "        }\n"
                        + "    ]\n"
                        + "}"),
            aResponse()
                .withBody(
                    "{{#each (jsonPath request.body '$.items') as |item|}}{{item.name}} {{/each}}")
                .build(),
            noFileSource(),
            Parameters.empty());

    assertThat(responseDefinition.getBody(), is("One Two Three "));
  }

  @Test
  public void mapResultFromJsonPathQueryCanBeUsedByHandlebarsEachHelper() {
    final ResponseDefinition responseDefinition =
        this.transformer.transform(
            mockRequest()
                .url("/json")
                .body(
                    "{\n"
                        + "    \"items\": {\n"
                        + "        \"one\": 1,\n"
                        + "        \"two\": 2,\n"
                        + "        \"three\": 3\n"
                        + "    }\n"
                        + "}"),
            aResponse()
                .withBody(
                    ""
                        + "{{#each (jsonPath request.body '$.items') as |value key|}}{{key}}: {{value}} {{/each}}")
                .build(),
            noFileSource(),
            Parameters.empty());

    assertThat(responseDefinition.getBody(), is("one: 1 two: 2 three: 3 "));
  }

  @Test
  public void singleValueResultFromJsonPathQueryCanBeUsedByHandlebarsIfHelper() {
    final ResponseDefinition responseDefinition =
        this.transformer.transform(
            mockRequest()
                .url("/json")
                .body(
                    "{\n"
                        + "    \"items\": {\n"
                        + "        \"one\": true,\n"
                        + "        \"two\": false,\n"
                        + "        \"three\": true\n"
                        + "    }\n"
                        + "}"),
            aResponse()
                .withBody(
                    ""
                        + "{{#if (jsonPath request.body '$.items.one')}}One{{/if}}\n"
                        + "{{#if (jsonPath request.body '$.items.two')}}Two{{/if}}")
                .build(),
            noFileSource(),
            Parameters.empty());

    assertThat(responseDefinition.getBody(), containsString("One"));
    assertThat(responseDefinition.getBody(), not(containsString("Two")));
  }

  @Test
  public void extractsASingleStringValueFromTheInputJson() throws IOException {
    testHelper(helper, "{\"test\":\"success\"}", "$.test", "success");
  }

  @Test
  public void extractsASingleNumberValueFromTheInputJson() throws IOException {
    testHelper(helper, "{\"test\": 1.2}", "$.test", "1.2");
  }

  @Test
  public void extractsASingleBooleanValueFromTheInputJson() throws IOException {
    testHelper(helper, "{\"test\": false}", "$.test", "false");
  }

  @Test
  public void extractsAJsonObjectFromTheInputJson() throws IOException {
    testHelper(
        helper,
        "{                          \n"
            + "    \"outer\": {               \n"
            + "        \"inner\": \"Sanctum\" \n"
            + "    }                          \n"
            + "}",
        "$.outer",
        equalToJson(
            "{                         \n" + "        \"inner\": \"Sanctum\" \n" + "    }"));
  }

  @Test
  public void extractsAJsonArrayFromTheInputJson() throws IOException {
    testHelper(
        helper, "{\n" + "    \"things\": [1, 2, 3]\n" + "}", "$.things", equalToJson("[1, 2, 3]"));
  }

  @Test
  public void rendersAMeaningfulErrorWhenInputJsonIsInvalid() {
    testHelperError(
        helper,
        "{\"test\":\"success}",
        "$.test",
        is("[ERROR: {\"test\":\"success} is not valid JSON]"));
  }

  @Test
  public void rendersAMeaningfulErrorWhenJsonPathIsInvalid() {
    testHelperError(
        helper,
        "{\"test\":\"success\"}",
        "$==test",
        is("[ERROR: $==test is not a valid JSONPath expression]"));
  }

  @Test
  public void rendersAnEmptyStringWhenJsonValueUndefined() {
    testHelperError(helper, "{\"test\":\"success\"}", "$.test2", is(""));
  }

  @Test
  public void rendersAnEmptyStringWhenJsonValueUndefinedAndOptionsEmpty() throws Exception {
    Map<String, Object> options = ImmutableMap.<String, Object>of();
    String output = render("{\"test\":\"success\"}", "$.test2", options);
    assertThat(output, is(""));
  }

  @Test
  public void rendersDefaultValueWhenShallowJsonValueUndefined() throws Exception {
    Map<String, Object> options = ImmutableMap.<String, Object>of("default", "0");
    String output = render("{}", "$.test", options);
    assertThat(output, is("0"));
  }

  @Test
  public void rendersDefaultValueWhenDeepJsonValueUndefined() throws Exception {
    Map<String, Object> options = ImmutableMap.<String, Object>of("default", "0");
    String output = render("{}", "$.outer.inner[0]", options);
    assertThat(output, is("0"));
  }

  @Test
  public void rendersDefaultValueWhenJsonValueNull() throws Exception {
    Map<String, Object> options = ImmutableMap.<String, Object>of("default", "0");
    String output = render("{\"test\":null}", "$.test", options);
    assertThat(output, is("0"));
  }

  @Test
  public void ignoresDefaultWhenJsonValueEmpty() throws Exception {
    Map<String, Object> options = ImmutableMap.<String, Object>of("default", "0");
    String output = render("{\"test\":\"\"}", "$.test", options);
    assertThat(output, is(""));
  }

  @Test
  public void ignoresDefaultWhenJsonValueZero() throws Exception {
    Map<String, Object> options = ImmutableMap.<String, Object>of("default", "1");
    String output = render("{\"test\":0}", "$.test", options);
    assertThat(output, is("0"));
  }

  private String render(String content, String path, Map<String, Object> options)
      throws IOException {
    return helper
        .apply(
            content,
            new Options.Builder(null, null, null, createContext(), null)
                .setParams(new Object[] {path})
                .setHash(options)
                .build())
        .toString();
  }

  @Test
  public void rendersAnEmptyStringWhenJsonIsNull() {
    testHelperError(helper, null, "$.test", is(""));
  }

  @Test
  public void rendersAMeaningfulErrorWhenJsonPathIsNull() {
    testHelperError(
        helper, "{\"test\":\"success}", null, is("[ERROR: The JSONPath cannot be empty]"));
  }

  @Test
  public void extractsValueFromAMap() {
    ResponseTemplateTransformer transformer =
        new ResponseTemplateTransformer(true) {
          @Override
          protected Map<String, Object> addExtraModelElements(
              Request request,
              ResponseDefinition responseDefinition,
              FileSource files,
              Parameters parameters) {
            return ImmutableMap.<String, Object>of("mapData", ImmutableMap.of("things", "abc"));
          }
        };

    final ResponseDefinition responseDefinition =
        transformer.transform(
            mockRequest(),
            aResponse().withBody("{{jsonPath mapData '$.things'}}").build(),
            noFileSource(),
            Parameters.empty());

    assertThat(responseDefinition.getBody(), is("abc"));
  }

  @Test
  public void returnsCorrectResultWhenSameExpressionUsedTwiceOnIdenticalDocuments()
      throws Exception {
    String one = renderHelperValue(helper, "{\"test\": \"one\"}", "$.test");
    String two = renderHelperValue(helper, "{\"test\": \"one\"}", "$.test");

    assertThat(one, is("one"));
    assertThat(two, is("one"));
  }

  @Test
  public void returnsCorrectResultWhenSameExpressionUsedTwiceOnDifferentDocuments()
      throws Exception {
    String one = renderHelperValue(helper, "{\"test\": \"one\"}", "$.test");
    String two = renderHelperValue(helper, "{\"test\": \"two\"}", "$.test");

    assertThat(one, is("one"));
    assertThat(two, is("two"));
  }

  @Test
  public void returnsCorrectResultWhenDifferentExpressionsUsedOnSameDocument() throws Exception {
    int one =
        renderHelperValue(
            helper,
            "{\n" + "  \"test\": {\n" + "    \"one\": 1,\n" + "    \"two\": 2\n" + "  }\n" + "}",
            "$.test.one");
    int two =
        renderHelperValue(
            helper,
            "{\n" + "  \"test\": {\n" + "    \"one\": 1,\n" + "    \"two\": 2\n" + "  }\n" + "}",
            "$.test.two");

    assertThat(one, is(1));
    assertThat(two, is(2));
  }

  @Test
  public void helperCanBeCalledDirectlyWithoutSupplyingRenderCache() throws Exception {
    Context context = Context.newBuilder(null).build();
    Options options =
        new Options(
            null,
            null,
            null,
            context,
            null,
            null,
            new Object[] {"$.stuff"},
            null,
            new ArrayList<String>(0));

    Object result = helper.apply("{\"stuff\":1}", options);

    assertThat(result, instanceOf(Integer.class));
    assertThat((Integer) result, is(1));
  }
}
