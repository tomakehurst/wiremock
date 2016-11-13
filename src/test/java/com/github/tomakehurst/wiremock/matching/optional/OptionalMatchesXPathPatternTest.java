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
package com.github.tomakehurst.wiremock.matching.optional;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.MatchesXPathPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.google.common.collect.ImmutableMap;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class OptionalMatchesXPathPatternTest {

    @Test
    public void shouldReturnsExactMatchWhenOptionalXPathMatches() {
        //given
        final String mySolarSystemXML = "<solar-system>"
                + "<planet name='Earth' position='3' supportsLife='yes'/>"
                + "<planet name='Venus' position='4'/></solar-system>";

        final StringValuePattern pattern = WireMock.optionalMatchingXPath("//planet[@name='Earth']");

        //when
        final MatchResult match = pattern.match(mySolarSystemXML);

        //then
        assertTrue("Expected XPath match", match.isExactMatch());
        assertThat(match.getDistance(), is(0.0));
    }

    @Test
    public void shouldReturnsNoExactMatchWhenXPathDoesNotMatch() {
        //given
        final String mySolarSystemXML = "<solar-system>"
                + "<planet name='Earth' position='3' supportsLife='yes'/>"
                + "<planet name='Venus' position='4'/></solar-system>";

        final StringValuePattern pattern = WireMock.optionalMatchingXPath("//star[@name='alpha centauri']");

        //when
        final MatchResult match = pattern.match(mySolarSystemXML);

        //then
        assertFalse("Expected XPath non-match", match.isExactMatch());
        assertThat(match.getDistance(), is(1.0));
    }

    @Test
    public void shouldReturnsNoExactMatchWhenXPathExpressionIsInvalid() {
        //given
        final String mySolarSystemXML = "<solar-system>"
                + "<planet name='Earth' position='3' supportsLife='yes'/>"
                + "<planet name='Venus' position='4'/></solar-system>";

        final StringValuePattern pattern = WireMock.optionalMatchingXPath("//\\\\&&&&&");

        //when
        final MatchResult match = pattern.match(mySolarSystemXML);

        //then
        assertFalse("Expected XPath non-match", match.isExactMatch());
        assertThat(match.getDistance(), is(1.0));
    }

    @Test
    public void shouldReturnsNoExactMatchWhenXmlIsBadlyFormed() {
        //given
        final String mySolarSystemXML = "solar-system>"
                + "<planet name='Earth' position='3' supportsLife='yes'/>"
                + "<planet name='Venus' position='4'/></solar-system>";

        final StringValuePattern pattern = WireMock.optionalMatchingXPath("//star[@name='alpha centauri']");

        //when
        final MatchResult match = pattern.match(mySolarSystemXML);

        //then
        assertFalse("Expected XPath non-match", match.isExactMatch());
        assertThat(match.getDistance(), is(1.0));
    }


    @Test
    public void shouldMatchOnNullValue() {
        assertThat(WireMock.optionalMatchingXPath("//*").match(null).isExactMatch(), is(true));
    }

    @Test
    public void shouldDeserializesCorrectlyWithoutNamespaces() {
        //given
        final String json = "{ \"matchesOrAbsentXPath\" : \"/stuff:outer/stuff:inner[.=111]\" }";

        //when
        final OptionalMatchesXPathPattern pattern = Json.read(json, OptionalMatchesXPathPattern.class);

        //then
        assertThat(pattern.getMatchesOrAbsentXPath(), is("/stuff:outer/stuff:inner[.=111]"));
        assertThat(pattern.getXPathNamespaces(), nullValue());
    }

    @Test
    public void shouldDeserializesCorrectlyWithNamespaces() {
        String json =
                "{ \"matchesOrAbsentXPath\" : \"/stuff:outer/stuff:inner[.=111]\" ,   \n" +
                        "  \"xPathNamespaces\" : {                                    \n" +
                        "      \"one\" : \"http://one.com/\",                         \n" +
                        "      \"two\" : \"http://two.com/\"                          \n" +
                        "  }                                                          \n" +
                        "}";

        OptionalMatchesXPathPattern pattern = Json.read(json, OptionalMatchesXPathPattern.class);

        assertThat(pattern.getXPathNamespaces(), hasEntry("one", "http://one.com/"));
        assertThat(pattern.getXPathNamespaces(), hasEntry("two", "http://two.com/"));
    }

    @Test
    public void serialisesCorrectlyWithNamspaces() throws JSONException {
        OptionalMatchesXPathPattern pattern = new OptionalMatchesXPathPattern("//*", ImmutableMap.of(
                "one", "http://one.com/",
                "two", "http://two.com/"
        ));

        final String json = Json.write(pattern);

        JSONAssert.assertEquals(
                "{ \"matchesOrAbsentXPath\" : \"//*\" ,   \n" +
                        "  \"xPathNamespaces\" : {                                    \n" +
                        "      \"one\" : \"http://one.com/\",                         \n" +
                        "      \"two\" : \"http://two.com/\"                          \n" +
                        "  }                                                          \n" +
                        "}",
                json, false);
    }

    @Test
    public void serialisesCorrectlyWithoutNamspaces() throws JSONException {
        OptionalMatchesXPathPattern pattern = new OptionalMatchesXPathPattern("//*", Collections.<String, String>emptyMap());

        final String json = Json.write(pattern);

        JSONAssert.assertEquals(
                "{ \"matchesOrAbsentXPath\" : \"//*\" }",
                json, false);
    }


}