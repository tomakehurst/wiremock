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

import static org.apache.commons.lang3.JavaVersion.JAVA_1_8;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class EqualToJsonTest {

  @Test
  public void returns0DistanceForExactMatchForSingleLevelObject() {
    assertThat(
        WireMock.equalToJson(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"three\":  3,  \n"
                    + "   \"four\":   4   \n"
                    + "}                  \n")
            .match(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"three\":  3,  \n"
                    + "   \"four\":   4   \n"
                    + "}                  \n")
            .getDistance(),
        is(0.0));
  }

  @Test
  public void returnsNon0DistanceForPartialMatchForSingleLevelObject() {
    assertThat(
        WireMock.equalToJson(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"three\":  3,  \n"
                    + "   \"four\":   4   \n"
                    + "}                  \n")
            .match(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"three\":  7,  \n"
                    + "   \"four\":   8   \n"
                    + "}                  \n")
            .getDistance(),
        is(0.4));
  }

  @Test
  public void returnsLargeDistanceForTotallyDifferentDocuments() {
    assertThat(
        WireMock.equalToJson(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"three\":  3,  \n"
                    + "   \"four\":   4   \n"
                    + "}                  \n")
            .match("[1, 2, 3]")
            .getDistance(),
        is(1.0));
  }

  @Test
  public void returnsLargeDistanceWhenActualDocIsAnEmptyObject() {
    assertThat(
        WireMock.equalToJson(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"three\":  3,  \n"
                    + "   \"four\":   4   \n"
                    + "}                  \n")
            .match("{}")
            .getDistance(),
        is(0.8));
  }

  @Test
  public void returnsLargeDistanceWhenActualDocIsAnEmptyArray() {
    assertThat(
        WireMock.equalToJson(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"three\":  3,  \n"
                    + "   \"four\":   4   \n"
                    + "}                  \n")
            .match("[]")
            .getDistance(),
        is(1.0));
  }

  @Test
  public void returnsLargeDistanceWhenExpectedDocIsAnEmptyObject() {
    assertThat(
        WireMock.equalToJson("{}")
            .match(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"three\":  3,  \n"
                    + "   \"four\":   4   \n"
                    + "}                  \n")
            .getDistance(),
        is(0.8));
  }

  @Test
  public void returnsLargeDistanceWhenExpectedDocIsAnEmptyArray() {
    assertThat(
        WireMock.equalToJson("[]")
            .match(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"three\":  3,  \n"
                    + "   \"four\":   4   \n"
                    + "}                  \n")
            .getDistance(),
        is(1.0));
  }

  @Test
  public void returnsMediumDistanceWhenSubtreeIsMissingFromActual() {
    assertThat(
        WireMock.equalToJson(
                "{\n"
                    + "    \"one\": \"GET\",          \n"
                    + "    \"two\": 2,                \n"
                    + "    \"three\": {               \n"
                    + "        \"four\": \"FOUR\",    \n"
                    + "        \"five\": [            \n"
                    + "            {                  \n"
                    + "                \"six\": 6,    \n"
                    + "                \"seven\": 7   \n"
                    + "            },                 \n"
                    + "            {                  \n"
                    + "                \"eight\": 8,  \n"
                    + "                \"nine\": 9    \n"
                    + "            }                  \n"
                    + "        ]                      \n"
                    + "    }                          \n"
                    + "}")
            .match(
                "{                          \n"
                    + "   \"one\":    \"GET\",    \n"
                    + "   \"two\":    2,          \n"
                    + "   \"three\":  {           \n"
                    + "       \"four\":   \"FOUR\"\n"
                    + "   }                       \n"
                    + "}                          \n")
            .getDistance(),
        closeTo(0.56, 0.01));
  }

  @Test
  public void returnsExactMatchWhenObjectPropertyOrderDiffers() {
    assertTrue(
        WireMock.equalToJson(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"three\":  3,  \n"
                    + "   \"four\":   4   \n"
                    + "}                  \n")
            .match(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"three\":  3,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"four\":   4   \n"
                    + "}                  \n")
            .isExactMatch());
  }

  @Test
  public void returnsNonMatchWhenArrayOrderDiffers() {
    assertFalse(WireMock.equalToJson("[1, 2, 3, 4]").match("[1, 3, 2, 4]").isExactMatch());
  }

  @Test
  public void ignoresArrayOrderDifferenceWhenConfigured() {
    assertTrue(
        WireMock.equalToJson("[1, 2, 3, 4]", true, false).match("[1, 3, 2, 4]").isExactMatch());
  }

  @Test
  public void ignoresNestedArrayOrderDifferenceWhenConfigured() {
    assertTrue(
        WireMock.equalToJson(
                "{\n"
                    + "    \"one\": 1,\n"
                    + "    \"two\": [\n"
                    + "        { \"val\": 1 },\n"
                    + "        { \"val\": 2 },\n"
                    + "        { \"val\": 3 }\n"
                    + "    ]\n"
                    + "}",
                true,
                false)
            .match(
                "{\n"
                    + "    \"one\": 1,\n"
                    + "    \"two\": [\n"
                    + "        { \"val\": 3 },\n"
                    + "        { \"val\": 2 },\n"
                    + "        { \"val\": 1 }\n"
                    + "    ]\n"
                    + "}")
            .isExactMatch());
  }

  @Test
  public void ignoresExtraObjectAttributesWhenConfigured() {
    assertTrue(
        WireMock.equalToJson(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"three\":  3,  \n"
                    + "   \"four\":   4   \n"
                    + "}                  \n",
                false,
                true)
            .match(
                "{                  \n"
                    + "   \"one\":    1,  \n"
                    + "   \"three\":  3,  \n"
                    + "   \"two\":    2,  \n"
                    + "   \"four\":   4,  \n"
                    + "   \"five\":   5,  \n"
                    + "   \"six\":    6   \n"
                    + "}                  \n")
            .isExactMatch());
  }

  @Test
  public void ignoresExtraObjectAttributesAndArrayOrderWhenConfigured() {
    assertTrue(
        WireMock.equalToJson(
                "{                          \n"
                    + "   \"one\":    1,          \n"
                    + "   \"two\":    2,          \n"
                    + "   \"three\":  3,          \n"
                    + "   \"four\":   [1, 2, 3]   \n"
                    + "}                  \n",
                true,
                true)
            .match(
                "{                          \n"
                    + "   \"one\":    1,          \n"
                    + "   \"three\":  3,          \n"
                    + "   \"two\":    2,          \n"
                    + "   \"four\":   [2, 1, 3],  \n"
                    + "   \"five\":   5,          \n"
                    + "   \"six\":    6           \n"
                    + "}                          \n")
            .isExactMatch());
  }

  @Test
  public void correctlyDeserialisesFromJsonStringWhenAdditionalParamsPresent() {
    StringValuePattern pattern =
        Json.read(
            "{\n"
                + "    \"equalToJson\": \"2\",\n"
                + "    \"ignoreArrayOrder\": true,\n"
                + "    \"ignoreExtraElements\": true\n"
                + "}",
            StringValuePattern.class);

    assertThat(pattern, instanceOf(EqualToJsonPattern.class));
    assertThat(((EqualToJsonPattern) pattern).isIgnoreArrayOrder(), is(true));
    assertThat(((EqualToJsonPattern) pattern).isIgnoreExtraElements(), is(true));
    assertThat(pattern.getExpected(), is("2"));
  }

  @Test
  public void correctlyDeserialisesFromJsonValueWhenAdditionalParamsPresent() throws JSONException {
    String expectedJson = "{ \"someKey\": \"someValue\" }";
    String serializedJson =
        "{                                           \n"
            + "    \"equalToJson\": "
            + expectedJson
            + ",  \n"
            + "    \"ignoreArrayOrder\": true,             \n"
            + "    \"ignoreExtraElements\": true           \n"
            + "}                                             ";
    StringValuePattern pattern = Json.read(serializedJson, StringValuePattern.class);

    assertThat(pattern, instanceOf(EqualToJsonPattern.class));
    assertThat(((EqualToJsonPattern) pattern).isIgnoreArrayOrder(), is(true));
    assertThat(((EqualToJsonPattern) pattern).isIgnoreExtraElements(), is(true));
    JSONAssert.assertEquals(pattern.getExpected(), expectedJson, false);
  }

  @Test
  public void correctlySerialisesToJsonValueWhenAdditionalParamsPresentAndConstructedWithJsonValue()
      throws JSONException {
    String expectedJson = "{ \"someKey\": \"someValue\" }";
    EqualToJsonPattern pattern = new EqualToJsonPattern(Json.node(expectedJson), true, true);

    String serialised = Json.write(pattern);
    String expected =
        "{                                           \n"
            + "    \"equalToJson\": "
            + expectedJson
            + ",  \n"
            + "    \"ignoreArrayOrder\": true,             \n"
            + "    \"ignoreExtraElements\": true           \n"
            + "}                                             ";
    JSONAssert.assertEquals(expected, serialised, false);
  }

  @Test
  public void correctlySerialisesToJsonWhenAdditionalParamsPresentAndConstructedWithString()
      throws JSONException {
    EqualToJsonPattern pattern = new EqualToJsonPattern("4444", true, true);

    String serialised = Json.write(pattern);
    JSONAssert.assertEquals(
        "{\n"
            + "    \"equalToJson\": \"4444\",\n"
            + "    \"ignoreArrayOrder\": true,\n"
            + "    \"ignoreExtraElements\": true\n"
            + "}",
        serialised,
        false);
  }

  @Test
  public void correctlyDeserialisesFromJsonStringWhenAdditionalParamsAbsent() {
    StringValuePattern pattern =
        Json.read("{\n" + "    \"equalToJson\": \"2\"\n" + "}", StringValuePattern.class);

    assertThat(pattern, instanceOf(EqualToJsonPattern.class));
    assertThat(((EqualToJsonPattern) pattern).isIgnoreArrayOrder(), is(nullValue()));
    assertThat(((EqualToJsonPattern) pattern).isIgnoreExtraElements(), is(nullValue()));
    assertThat(pattern.getExpected(), is("2"));
  }

  @Test
  public void correctlyDeserialisesFromJsonValueWhenAdditionalParamsAbsent() throws JSONException {
    String expectedJson = "[ 1, 2, \"value\" ]";
    StringValuePattern pattern =
        Json.read("{ \"equalToJson\": " + expectedJson + " }", StringValuePattern.class);

    assertThat(pattern, instanceOf(EqualToJsonPattern.class));
    assertThat(((EqualToJsonPattern) pattern).isIgnoreArrayOrder(), is(nullValue()));
    assertThat(((EqualToJsonPattern) pattern).isIgnoreExtraElements(), is(nullValue()));
    JSONAssert.assertEquals(pattern.getExpected(), expectedJson, false);
  }

  @Test
  public void correctlySerialisesToJsonWhenAdditionalParamsAbsentAndConstructedWithJsonValue()
      throws JSONException {
    String expectedJson = "[ 1, 2, \"value\" ]";
    EqualToJsonPattern pattern = new EqualToJsonPattern(Json.node(expectedJson), null, null);

    String serialised = Json.write(pattern);
    JSONAssert.assertEquals("{ \"equalToJson\": " + expectedJson + " }", serialised, false);
  }

  @Test
  public void correctlySerialisesToJsonWhenAdditionalParamsAbsent() throws JSONException {
    EqualToJsonPattern pattern = new EqualToJsonPattern("4444", null, null);

    String serialised = Json.write(pattern);
    JSONAssert.assertEquals("{\n" + "    \"equalToJson\": \"4444\"\n" + "}", serialised, false);
  }

  @Test
  public void returnsNoExactMatchForVerySimilarNestedDocs() {
    assertFalse(
        new EqualToJsonPattern(
                "{\n"
                    + "    \"outer\": {\n"
                    + "        \"inner:\": {\n"
                    + "            \"wrong\": 1\n"
                    + "        }\n"
                    + "    }\n"
                    + "}",
                false,
                false)
            .match(
                "{\n"
                    + "    \"outer\": {\n"
                    + "        \"inner:\": {\n"
                    + "            \"thing\": 1\n"
                    + "        }\n"
                    + "    }\n"
                    + "}")
            .isExactMatch());
  }

  @Test
  public void doesNotMatchWhenValueIsNull() {
    MatchResult match =
        new EqualToJsonPattern(
                "{\n"
                    + "    \"outer\": {\n"
                    + "        \"inner:\": {\n"
                    + "            \"wrong\": 1\n"
                    + "        }\n"
                    + "    }\n"
                    + "}",
                false,
                false)
            .match(null);

    assertFalse(match.isExactMatch());
    assertThat(match.getDistance(), is(1.0));
  }

  @Test
  public void doesNotMatchWhenValueIsEmptyString() {
    MatchResult match =
        new EqualToJsonPattern(
                "{\n"
                    + "    \"outer\": {\n"
                    + "        \"inner:\": {\n"
                    + "            \"wrong\": 1\n"
                    + "        }\n"
                    + "    }\n"
                    + "}",
                false,
                false)
            .match("");

    assertFalse(match.isExactMatch());
    assertThat(match.getDistance(), is(1.0));
  }

  @Test
  public void doesNotMatchWhenValueIsNotJson() {
    MatchResult match =
        new EqualToJsonPattern(
                "{\n"
                    + "    \"outer\": {\n"
                    + "        \"inner:\": {\n"
                    + "            \"wrong\": 1\n"
                    + "        }\n"
                    + "    }\n"
                    + "}",
                false,
                false)
            .match("<some-xml />");

    assertFalse(match.isExactMatch());
    assertThat(match.getDistance(), is(1.0));
  }

  @Test
  public void doesNotBreakWhenComparingNestedArraysOfDifferentSizes() {
    String expected =
        "{\"columns\": [{\"name\": \"agreementnumber\",\"a\": 1},{\"name\": \"utilizerstatus\",\"b\": 2}]}";
    String actual =
        "{\"columns\": [{\"name\": \"x\",\"y\": 3},{\"name\": \"agreementnumber\",\"a\": 1},{\"name\": \"agreementstatus\",\"b\": 2}]}";

    MatchResult match = new EqualToJsonPattern(expected, false, false).match(actual);

    assertFalse(match.isExactMatch());
  }

  @Test
  public void doesNotBreakWhenComparingTopLevelArraysOfDifferentSizesWithCommonElements() {
    String expected =
        "[    \n"
            + "  { \"one\": 1 },  \n"
            + "  { \"two\": 2 },  \n"
            + "  { \"three\": 3 } \n"
            + "]";
    String actual =
        "[      \n"
            + "  { \"zero\": 0 }, \n"
            + "  { \"one\": 1 },  \n"
            + "  { \"two\": 2 },  \n"
            + "  { \"four\": 4 }  \n"
            + "]";

    MatchResult match = new EqualToJsonPattern(expected, false, false).match(actual);

    assertFalse(match.isExactMatch());
  }

  @Test
  public void ignoresExtraElementsWhenParameterIsPresentsWithoutIgnoreArrayOrder() {
    StringValuePattern pattern =
        Json.read(
            "{\n"
                + "    \"equalToJson\": { \"one\": 1 },\n"
                + "    \"ignoreExtraElements\": true\n"
                + "}",
            StringValuePattern.class);

    assertThat(
        pattern.match("{\n" + "    \"one\": 1,\n" + "    \"two\": 2\n" + "}").isExactMatch(),
        is(true));
  }

  @Test
  public void doesNotMatchEmptyArraysWhenNotIgnoringExtraElements() {
    String expected = "{\"client\":\"AAA\",\"name\":\"BBB\"}";
    String actual = "{\"client\":\"AAA\", \"name\":\"BBB\", \"addresses\": [ ]}";

    MatchResult match = new EqualToJsonPattern(expected, false, false).match(actual);

    assertFalse(match.isExactMatch());
  }

  @Test
  public void doesNotMatchEmptyArrayWhenIgnoringExtraArrayElementsAndNotIgnoringExtraElements() {
    String expected = "{\"client\":\"AAA\",\"name\":\"BBB\"}";
    String actual = "{\"client\":\"AAA\", \"name\":\"BBB\", \"addresses\": [ ]}";

    MatchResult match = new EqualToJsonPattern(expected, true, false).match(actual);

    assertFalse(match.isExactMatch());
  }

  @Test
  public void doesNotMatchEmptyObjectWhenIgnoringExtraArrayElementsAndNotIgnoringExtraElements() {
    String expected = "{\"client\":\"AAA\",\"name\":\"BBB\"}";
    String actual = "{\"client\":\"AAA\", \"name\":\"BBB\", \"addresses\": { }}";

    MatchResult match = new EqualToJsonPattern(expected, true, false).match(actual);

    assertFalse(match.isExactMatch());
  }

  @Test
  public void treatsTwoTopLevelsArraysWithDifferingOrderAsSameWhenIgnoringOrder() {
    assumeJava8OrHigher();

    String expected = "[\"a\",\"b\", \"c\",\"d\",\"e\",\"f\",\"g\",\"h\"]";
    String actual = "[\"b\",\"a\", \"d\",\"c\",\"e\",\"f\",\"g\",\"h\"]";

    EqualToJsonPattern pattern = new EqualToJsonPattern(expected, true, true);
    MatchResult result = pattern.match(actual);

    assertTrue(result.isExactMatch());
  }

  @Test
  public void supportsPlaceholders() {
    assumeJava8OrHigher();

    String expected =
        "{\n" + "  \"id\": \"${json-unit.any-string}\",\n" + "  \"name\": \"Tom\"\n" + "}";

    String actual = "{\n" + "  \"id\": \"abc123\",\n" + "  \"name\": \"Tom\"\n" + "}";

    MatchResult match = new EqualToJsonPattern(expected, false, false).match(actual);
    assertThat(match.isExactMatch(), is(true));
  }

  @Test
  public void supportsRegexPlaceholders() {
    assumeJava8OrHigher();

    String expected =
        "{\n" + "  \"id\": \"${json-unit.regex}[a-z]+\",\n" + "  \"name\": \"Tom\"\n" + "}";

    String actualMatching = "{\n" + "  \"id\": \"abc\",\n" + "  \"name\": \"Tom\"\n" + "}";
    MatchResult match = new EqualToJsonPattern(expected, false, false).match(actualMatching);
    assertThat(match.isExactMatch(), is(true));

    String actualNonMatching = "{\n" + "  \"id\": \"123\",\n" + "  \"name\": \"Tom\"\n" + "}";
    MatchResult nonMatch = new EqualToJsonPattern(expected, false, false).match(actualNonMatching);
    assertThat(nonMatch.isExactMatch(), is(false));
  }

  private static void assumeJava8OrHigher() {
    assumeThat(isJavaVersionAtLeast(JAVA_1_8), is(true));
  }
}
