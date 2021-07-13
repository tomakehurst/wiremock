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
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.equalToJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class MatchesXPathPatternTest {

  @Test
  public void returnsExactMatchWhenXPathMatches() {
    String mySolarSystemXML =
        "<solar-system>"
            + "<planet name='Earth' position='3' supportsLife='yes'/>"
            + "<planet name='Venus' position='4'/></solar-system>";

    StringValuePattern pattern = WireMock.matchingXPath("//planet[@name='Earth']");

    MatchResult match = pattern.match(mySolarSystemXML);
    assertTrue("Expected XPath match", match.isExactMatch());
    assertThat(match.getDistance(), is(0.0));
  }

  @Test
  public void returnsNoExactMatchWhenXPathDoesNotMatch() {
    String mySolarSystemXML =
        "<solar-system>"
            + "<planet name='Earth' position='3' supportsLife='yes'/>"
            + "<planet name='Venus' position='4'/></solar-system>";

    StringValuePattern pattern = WireMock.matchingXPath("//star[@name='alpha centauri']");

    MatchResult match = pattern.match(mySolarSystemXML);
    assertFalse("Expected XPath non-match", match.isExactMatch());
    assertThat(match.getDistance(), is(1.0));
  }

  @Test
  public void returnsNoExactMatchWhenXPathExpressionIsInvalid() {
    String mySolarSystemXML =
        "<solar-system>"
            + "<planet name='Earth' position='3' supportsLife='yes'/>"
            + "<planet name='Venus' position='4'/></solar-system>";

    StringValuePattern pattern = WireMock.matchingXPath("//\\\\&&&&&");

    MatchResult match = pattern.match(mySolarSystemXML);
    assertFalse("Expected XPath non-match", match.isExactMatch());
    assertThat(match.getDistance(), is(1.0));
  }

  @Test
  public void returnsNoExactMatchWhenXmlIsBadlyFormed() {
    String mySolarSystemXML =
        "solar-system>"
            + "<planet name='Earth' position='3' supportsLife='yes'/>"
            + "<planet name='Venus' position='4'/></solar-system>";

    StringValuePattern pattern = WireMock.matchingXPath("//star[@name='alpha centauri']");

    MatchResult match = pattern.match(mySolarSystemXML);
    assertFalse("Expected XPath non-match", match.isExactMatch());
    assertThat(match.getDistance(), is(1.0));
  }

  @Test
  public void matchesNamespacedXmlWhenNamespacesSpecified() {
    String xml =
        "<t:thing xmlns:t='http://things' xmlns:s='http://subthings'><s:subThing>The stuff</s:subThing></t:thing>";

    StringValuePattern pattern =
        WireMock.matchingXPath(
            "//sub:subThing[.='The stuff']",
            ImmutableMap.of("sub", "http://subthings", "t", "http://things"));

    MatchResult match = pattern.match(xml);
    assertTrue(match.isExactMatch());
  }

  @Test
  public void matchesNamespacedXmlFromLocalNames() {
    String xml =
        "<t:thing xmlns:t='http://things' xmlns:s='http://subthings'><s:subThing>The stuff</s:subThing></t:thing>";

    StringValuePattern pattern = WireMock.matchingXPath("/thing/subThing[.='The stuff']");

    MatchResult match = pattern.match(xml);
    assertTrue(match.isExactMatch());
  }

  @Test
  public void matchesAgainstValuePatternWhenSingleElementReturnedFromXPath() {
    String xml = "<outer>\n" + "    <inner>stuff</inner>\n" + "</outer>";

    StringValuePattern pattern = WireMock.matchingXPath("//inner/text()", matching("[a-z]*"));

    assertThat(pattern.match(xml).isExactMatch(), is(true));
  }

  @Test
  public void matchesAgainstValuePatternWhenMultipleElementsReturnedFromXPath() {
    String xml =
        "<outer>\n"
            + "    <inner>stuffing</inner>\n"
            + "    <inner>stuffed</inner>\n"
            + "    <inner>stuff</inner>\n"
            + "    <inner>stuffable</inner>\n"
            + "</outer>";

    StringValuePattern pattern =
        WireMock.matchingXPath("//inner/text()", WireMock.equalTo("stuff"));

    assertThat(pattern.match(xml).isExactMatch(), is(true));
  }

  @Test
  public void returnsTheMatchFromTheClosestElementWhenNoneMatchExactly() {
    String xml =
        "<outer>\n"
            + "    <inner>stuffing</inner>\n"
            + "    <inner>stuffed</inner>\n"
            + "    <inner>stuffy</inner>\n"
            + "    <inner>stuffable</inner>\n"
            + "</outer>";

    StringValuePattern pattern =
        WireMock.matchingXPath("//inner/text()", WireMock.equalTo("stuff"));

    assertThat(pattern.match(xml).getDistance(), closeTo(0.16, 0.01));
  }

  @Test
  public void matchesAttributeAgainstValuePattern() {
    String xml = "<outer inner=\"stuff\"/>";

    StringValuePattern pattern =
        WireMock.matchingXPath("/outer/@inner", equalToIgnoreCase("Stuff"));

    assertThat(pattern.match(xml).isExactMatch(), is(true));
  }

  @Test
  public void returnsAMaxDistanceNoMatchWhenNoNodesReturnedAndValuePatternIsPresent() {
    String xml = "<outer inner=\"stuff\"/>";

    StringValuePattern pattern =
        WireMock.matchingXPath("/outer/@nothing", equalToIgnoreCase("Stuff"));

    assertThat(pattern.match(xml).isExactMatch(), is(false));
    assertThat(pattern.match(xml).getDistance(), is(1.0));
  }

  @Test
  public void matchesComplexElementAgainstValuePattern() {
    String xml = "<outer>\n" + "    <inner>stuff</inner>\n" + "</outer>";

    StringValuePattern pattern =
        WireMock.matchingXPath("/outer/inner", equalToXml("<inner>stuff</inner>"));

    assertThat(pattern.match(xml).isExactMatch(), is(true));
  }

  @Test
  public void deserialisesCorrectlyWithoutNamespaces() {
    String json = "{ \"matchesXPath\" : \"/stuff:outer/stuff:inner[.=111]\" }";

    MatchesXPathPattern pattern = Json.read(json, MatchesXPathPattern.class);

    assertThat(pattern.getMatchesXPath(), is("/stuff:outer/stuff:inner[.=111]"));
    assertThat(pattern.getXPathNamespaces(), nullValue());
  }

  @Test
  public void deserialisesCorrectlyWithNamespaces() {
    String json =
        "{ \"matchesXPath\" : \"/stuff:outer/stuff:inner[.=111]\" ,   \n"
            + "  \"xPathNamespaces\" : {                                    \n"
            + "      \"one\" : \"http://one.com/\",                         \n"
            + "      \"two\" : \"http://two.com/\"                          \n"
            + "  }                                                          \n"
            + "}";

    MatchesXPathPattern pattern = Json.read(json, MatchesXPathPattern.class);

    assertThat(pattern.getXPathNamespaces(), hasEntry("one", "http://one.com/"));
    assertThat(pattern.getXPathNamespaces(), hasEntry("two", "http://two.com/"));
  }

  @Test
  public void deserialisesCorrectlyWithValuePattern() {
    String json =
        "{                                      \n"
            + "    \"matchesXPath\": {                 \n"
            + "        \"expression\": \"/thing\",     \n"
            + "        \"matches\": \"[0-9]*\"         \n"
            + "    }                                   \n"
            + "}";

    MatchesXPathPattern pattern = Json.read(json, MatchesXPathPattern.class);

    assertThat(pattern.getValuePattern(), instanceOf(RegexPattern.class));
    assertThat(pattern.getExpected(), is("/thing"));
    assertThat(pattern.getValuePattern().getExpected(), is("[0-9]*"));
  }

  @Test
  public void serialisesCorrectlyWithNamspaces() throws JSONException {
    MatchesXPathPattern pattern =
        new MatchesXPathPattern(
            "//*",
            ImmutableMap.of(
                "one", "http://one.com/",
                "two", "http://two.com/"));

    String json = Json.write(pattern);

    JSONAssert.assertEquals(
        "{ \"matchesXPath\" : \"//*\" ,   \n"
            + "  \"xPathNamespaces\" : {                                    \n"
            + "      \"one\" : \"http://one.com/\",                         \n"
            + "      \"two\" : \"http://two.com/\"                          \n"
            + "  }                                                          \n"
            + "}",
        json,
        false);
  }

  @Test
  public void serialisesCorrectlyWithoutNamspaces() throws JSONException {
    MatchesXPathPattern pattern =
        new MatchesXPathPattern("//*", Collections.<String, String>emptyMap());

    String json = Json.write(pattern);

    JSONAssert.assertEquals("{ \"matchesXPath\" : \"//*\" }", json, false);
  }

  @Test
  public void serialisesCorrectlyWithValuePattern() {
    assertThat(
        Json.write(WireMock.matchingXPath("/thing", containing("123"))),
        equalToJson(
            "{                                      \n"
                + "    \"matchesXPath\": {                 \n"
                + "        \"expression\": \"/thing\",     \n"
                + "        \"contains\": \"123\"           \n"
                + "    }                                   \n"
                + "}"));
  }

  @Test
  public void noMatchOnNullValue() {
    assertThat(WireMock.matchingXPath("//*").match(null).isExactMatch(), is(false));
  }
}
