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
package com.github.tomakehurst.wiremock.matching;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.equalToJson;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.common.JsonException;
import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.common.Notifier;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MatchesJsonPathPatternTest {

  private Mockery context;

  @Before
  public void init() {
    context = new Mockery();
  }

  @Test
  public void matchesABasicJsonPathWhenTheExpectedElementIsPresent() {
    StringValuePattern pattern = WireMock.matchingJsonPath("$.one");
    assertTrue(
        "Expected match when JSON attribute is present",
        pattern.match("{ \"one\": 1 }").isExactMatch());
  }

  @Test
  public void doesNotMatchABasicJsonPathWhenTheExpectedElementIsNotPresent() {
    StringValuePattern pattern = WireMock.matchingJsonPath("$.one");
    assertFalse(
        "Expected no match when JSON attribute is absent",
        pattern.match("{ \"two\": 2 }").isExactMatch());
  }

  @Test
  public void matchesOnJsonPathsWithFilters() {
    StringValuePattern pattern = WireMock.matchingJsonPath("$.numbers[?(@.number == '2')]");

    assertTrue(
        "Expected match when JSON attribute is present",
        pattern.match("{ \"numbers\": [ {\"number\": 1}, {\"number\": 2} ]}").isExactMatch());
    assertFalse(
        "Expected no match when JSON attribute is absent",
        pattern.match("{ \"numbers\": [{\"number\": 7} ]}").isExactMatch());
  }

  @Test
  public void matchesOnJsonPathsWithRegexFilter() {
    StringValuePattern pattern = WireMock.matchingJsonPath("$.numbers[?(@.number =~ /2/i)]");

    assertTrue(
        "Expected match when JSON attribute is present",
        pattern.match("{ \"numbers\": [ {\"number\": 1}, {\"number\": 2} ]}").isExactMatch());
    assertFalse(
        "Expected no match when JSON attribute is absent",
        pattern.match("{ \"numbers\": [{\"number\": 7} ]}").isExactMatch());
  }

  @Test
  public void matchesOnJsonPathsWithSizeFilter() {
    StringValuePattern pattern = WireMock.matchingJsonPath("$[?(@.numbers.size() == 2)]");

    assertTrue(
        "Expected match when JSON attribute is present",
        pattern.match("{ \"numbers\": [ {\"number\": 1}, {\"number\": 2} ]}").isExactMatch());
    assertFalse(
        "Expected no match when JSON attribute is absent",
        pattern.match("{ \"numbers\": [{\"number\": 7} ]}").isExactMatch());
  }

  @Test
  public void matchesOnJsonPathsWithFiltersOnNestedObjects() {
    StringValuePattern pattern = WireMock.matchingJsonPath("$..thingOne[?(@.innerOne == 11)]");
    assertTrue(
        "Expected match",
        pattern
            .match("{ \"things\": { \"thingOne\": { \"innerOne\": 11 }, \"thingTwo\": 2 }}")
            .isExactMatch());
  }

  @Test
  public void providesSensibleNotificationWhenJsonMatchFailsDueToInvalidJson() {
    expectInfoNotification(
        "Warning: JSON path expression '$.something' failed to match document 'Not a JSON document' because of error 'Expected to find an object with property ['something'] in path $ but found 'java.lang.String'. This is not a json object according to the JsonProvider: 'com.jayway.jsonpath.spi.json.JsonSmartJsonProvider'.'");

    StringValuePattern pattern = WireMock.matchingJsonPath("$.something");
    assertFalse("Expected the match to fail", pattern.match("Not a JSON document").isExactMatch());
  }

  @Test
  public void providesSensibleNotificationWhenJsonMatchFailsDueToMissingAttributeJson() {
    expectInfoNotification(
        "Warning: JSON path expression '$.something' failed to match document '{ \"nothing\": 1 }' because of error 'No results for path: $['something']'");

    StringValuePattern pattern = WireMock.matchingJsonPath("$.something");
    assertFalse("Expected the match to fail", pattern.match("{ \"nothing\": 1 }").isExactMatch());
  }

  @Test
  public void doesNotMatchWhenJsonPathWouldResolveToEmptyArray() {
    String json =
        "{\n" + "  \"RequestDetail\" : {\n" + "    \"ClientTag\" : \"test111\"\n" + "  }\n" + "}";

    StringValuePattern pattern = WireMock.matchingJsonPath("$.RequestDetail.?(@=='test222')");
    MatchResult match = pattern.match(json);
    assertFalse(match.isExactMatch());
  }

  @Test
  public void noMatchOnNullValue() {
    assertThat(WireMock.matchingJsonPath("$..*").match(null).isExactMatch(), is(false));
  }

  @Test
  public void matchesNumericExpressionResultAgainstValuePatternWhenSpecified() {
    String json = "{\n" + "    \"volumeControl\": {\n" + "        \"max\": 11\n" + "    }\n" + "}";

    StringValuePattern pattern = WireMock.matchingJsonPath("$.volumeControl.max", equalTo("11"));
    MatchResult match = pattern.match(json);
    assertTrue(match.isExactMatch());
  }

  @Test
  public void matchesStringExpressionResultAgainstValuePatternWhenSpecified() {
    String json =
        "{\n" + "    \"volumeControl\": {\n" + "        \"max\": \"eleven\"\n" + "    }\n" + "}";

    StringValuePattern pattern =
        WireMock.matchingJsonPath("$.volumeControl.max", equalTo("eleven"));
    MatchResult match = pattern.match(json);
    assertTrue(match.isExactMatch());
  }

  @Test
  public void matchesBooleanExpressionResultAgainstValuePatternWhenSpecified() {
    String json =
        "{\n" + "    \"volumeControl\": {\n" + "        \"max\": true\n" + "    }\n" + "}";

    StringValuePattern pattern = WireMock.matchingJsonPath("$.volumeControl.max", equalTo("true"));
    MatchResult match = pattern.match(json);
    assertTrue(match.isExactMatch());
  }

  @Test
  public void matchesObjectExpressionResultAgainstValuePatternWhenSpecified() {
    String json = "{\n" + "    \"volumeControl\": {\n" + "        \"max\": 11\n" + "    }\n" + "}";

    StringValuePattern pattern =
        WireMock.matchingJsonPath(
            "$.volumeControl", WireMock.equalToJson("{\n" + "    \"max\": 11\n" + "}"));
    MatchResult match = pattern.match(json);
    assertTrue(match.isExactMatch());
  }

  @Test
  public void matchesArrayExpressionResultAgainstValuePatternWhenSpecified() {
    String json =
        "{\n" + "    \"volumeControl\": {\n" + "        \"max\": [1, 2, 3, 11]\n" + "    }\n" + "}";

    StringValuePattern pattern =
        WireMock.matchingJsonPath("$.volumeControl.max", WireMock.equalToJson("[1,2,3,11]"));
    MatchResult match = pattern.match(json);
    assertTrue(match.isExactMatch());
  }

  @Test
  public void matchesNotPresentExpressionResultAgainstAbsentValuePattern() {
    String json =
        "{\n" + "    \"volumeControl\": {\n" + "        \"max\": true\n" + "    }\n" + "}";

    StringValuePattern pattern = WireMock.matchingJsonPath("$.volumeControl.min", absent());
    MatchResult match = pattern.match(json);
    assertTrue(match.isExactMatch());
  }

  @Test
  public void matchesNullExpressionResultAgainstAbsentValuePattern() {
    String json =
        "{\n" + "    \"volumeControl\": {\n" + "        \"max\": null\n" + "    }\n" + "}";

    StringValuePattern pattern = WireMock.matchingJsonPath("$.volumeControl.max", absent());
    MatchResult match = pattern.match(json);
    assertTrue(match.isExactMatch());
  }

  @Test
  public void returnsTheDistanceFromTheValueMatcherWhenNotAMatch() {
    String json =
        "{\n" + "    \"volumeControl\": {\n" + "        \"max\": \"eleven\"\n" + "    }\n" + "}";

    StringValuePattern pattern = WireMock.matchingJsonPath("$.volumeControl.max", equalTo("ele"));
    MatchResult match = pattern.match(json);
    assertFalse(match.isExactMatch());
    assertThat(match.getDistance(), is(0.5));
  }

  @Test
  public void correctlySerialises() {
    assertThat(
        Json.write(WireMock.matchingJsonPath("$..thing")),
        equalToJson(
            "{                                \n"
                + "  \"matchesJsonPath\": \"$..thing\"       \n"
                + "}"));
  }

  @Test
  public void correctlySerialisesWithValuePattern() {
    assertThat(
        Json.write(WireMock.matchingJsonPath("$..thing", containing("123"))),
        equalToJson(
            "{                                      \n"
                + "    \"matchesJsonPath\": {              \n"
                + "        \"expression\": \"$..thing\",   \n"
                + "        \"contains\": \"123\"           \n"
                + "    }                                   \n"
                + "}"));
  }

  @Test
  public void correctlyDeserialises() {
    StringValuePattern stringValuePattern =
        Json.read(
            "{                                         \n"
                + "  \"matchesJsonPath\": \"$..thing\"       \n"
                + "}",
            StringValuePattern.class);

    assertThat(stringValuePattern, instanceOf(MatchesJsonPathPattern.class));
    assertThat(stringValuePattern.getExpected(), is("$..thing"));
  }

  @Test
  public void correctlyDeserialisesWithValuePattern() {
    StringValuePattern stringValuePattern =
        Json.read(
            "{                                      \n"
                + "    \"matchesJsonPath\": {              \n"
                + "        \"expression\": \"$..thing\",   \n"
                + "        \"equalTo\": \"the value\"      \n"
                + "    }                                   \n"
                + "}",
            StringValuePattern.class);

    assertThat(stringValuePattern, instanceOf(MatchesJsonPathPattern.class));
    assertThat(stringValuePattern.getExpected(), is("$..thing"));

    ContentPattern<?> subMatcher = ((MatchesJsonPathPattern) stringValuePattern).getValuePattern();
    assertThat(subMatcher, instanceOf(EqualToPattern.class));
    assertThat(subMatcher.getExpected(), is("the value"));
  }

  @Test
  public void correctlyDeserialisesWithAbsentValuePattern() {
    StringValuePattern stringValuePattern =
        Json.read(
            "{                                      \n"
                + "    \"matchesJsonPath\": {              \n"
                + "        \"expression\": \"$..thing\",   \n"
                + "        \"absent\": \"(absent)\"        \n"
                + "    }                                   \n"
                + "}",
            StringValuePattern.class);

    assertThat(stringValuePattern, instanceOf(MatchesJsonPathPattern.class));
    assertThat(stringValuePattern.getExpected(), is("$..thing"));

    ContentPattern<?> subMatcher = ((MatchesJsonPathPattern) stringValuePattern).getValuePattern();
    assertThat(subMatcher, instanceOf(AbsentPattern.class));
    assertThat(((StringValuePattern) subMatcher).nullSafeIsAbsent(), is(true));
  }

  @Test
  public void correctlyDeserialisesWhenSubMatcherHasExtraParameters() {
    StringValuePattern stringValuePattern =
        Json.read(
            "{                                       \n"
                + "    \"matchesJsonPath\": {              \n"
                + "        \"expression\": \"$..thing\",   \n"
                + "        \"equalToJson\": \"{}\",        \n"
                + "        \"ignoreExtraElements\": true,  \n"
                + "        \"ignoreArrayOrder\": true   \n"
                + "    }                                   \n"
                + "}",
            StringValuePattern.class);

    assertThat(stringValuePattern, instanceOf(MatchesJsonPathPattern.class));

    ContentPattern<?> subMatcher = ((MatchesJsonPathPattern) stringValuePattern).getValuePattern();
    assertThat(subMatcher, instanceOf(EqualToJsonPattern.class));
    assertThat(subMatcher.getExpected(), jsonEquals("{}"));
    assertThat(((EqualToJsonPattern) subMatcher).isIgnoreExtraElements(), is(true));
    assertThat(((EqualToJsonPattern) subMatcher).isIgnoreArrayOrder(), is(true));
  }

  @Test
  public void correctlySerialisesWhenSubMatcherHasExtraParameters() {
    StringValuePattern matcher =
        new MatchesJsonPathPattern("$..thing", WireMock.equalToJson("{}", true, true));

    String json = Json.write(matcher);

    assertThat(
        json,
        jsonEquals(
            "{                                       \n"
                + "    \"matchesJsonPath\": {              \n"
                + "        \"expression\": \"$..thing\",   \n"
                + "        \"equalToJson\": \"{}\",        \n"
                + "        \"ignoreExtraElements\": true,  \n"
                + "        \"ignoreArrayOrder\": true      \n"
                + "    }                                   \n"
                + "}"));
  }

  @Test(expected = JsonException.class)
  public void throwsSensibleErrorOnDeserialisationWhenPatternIsBadlyFormedWithMissingExpression() {
    Json.read(
        "{                                      \n"
            + "    \"matchesJsonPath\": {              \n"
            + "        \"express\": \"$..thing\",      \n"
            + "        \"equalTo\": \"the value\"      \n"
            + "    }                                   \n"
            + "}",
        StringValuePattern.class);
  }

  @Test(expected = JsonException.class)
  public void
      throwsSensibleErrorOnDeserialisationWhenPatternIsBadlyFormedWithBadValuePatternName() {
    Json.read(
        "{                                      \n"
            + "    \"matchesJsonPath\": {              \n"
            + "        \"expression\": \"$..thing\",   \n"
            + "        \"badOperator\": \"the value\"  \n"
            + "    }                                   \n"
            + "}",
        StringValuePattern.class);
  }

  @Test
  public void equalsIncludesValuePattern() {
    StringValuePattern pattern1 = matchingJsonPath("$.LinkageDetails.AccountId", equalTo("1000"));
    StringValuePattern pattern2 = matchingJsonPath("$.LinkageDetails.AccountId", equalTo("1001"));
    StringValuePattern pattern3 = matchingJsonPath("$.LinkageDetails.AccountId", equalTo("1000"));

    assertThat(pattern1, not(Matchers.equalTo(pattern2)));
    assertThat(pattern1.hashCode(), not(Matchers.equalTo(pattern2.hashCode())));

    assertThat(pattern1, Matchers.equalTo(pattern3));
    assertThat(pattern1.hashCode(), Matchers.equalTo(pattern3.hashCode()));
  }

  @Test
  public void treatsAnEmptyArrayExpressionResultAsAbsent() {
    String json =
        "{\n"
            + "  \"Books\": [\n"
            + "    {\n"
            + "      \"Author\": {\n"
            + "        \"Name\": \"1234567\",\n"
            + "        \"Price\": \"2.2\"\n"
            + "      }\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    MatchResult result = matchingJsonPath("$..[?(@.Author.ISBN)]", absent()).match(json);

    assertTrue(result.isExactMatch());
  }

  private void expectInfoNotification(final String message) {
    final Notifier notifier = context.mock(Notifier.class);
    context.checking(
        new Expectations() {
          {
            one(notifier).info(message);
          }
        });
    LocalNotifier.set(notifier);
  }

  @After
  public void cleanUp() {
    LocalNotifier.set(null);
  }
}
