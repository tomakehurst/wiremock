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
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class OptionalMatchesJsonPathPatternTest {

    /**
     * OptionalMatchesJsonPathPattern should match when there:
     * - exists json path and it matches to pattern,
     * - json path does not exist (is absent),
     * <p>
     * Only when it exists AND does not match to pattern it should return no match
     */

    private Mockery context;

    @Before
    public void init() {
        context = new Mockery();
    }

    @Test
    public void shouldMatchesWhenExpectedElementIsPresent() {
        //given
        final StringValuePattern pattern = WireMock.optionalMatchingJsonPath("$.one");
        final String json = "{ \"one\": 1 }";

        //when
        final MatchResult matchResult = pattern.match(json);

        //then
        assertTrue("Expected match when JSON attribute is present", matchResult.isExactMatch());
    }

    @Test
    public void shouldMatchesWhenJsonIsEmpty() {
        //given
        final StringValuePattern pattern = WireMock.optionalMatchingJsonPath("$.one");
        final String json = "{}";

        //when
        final MatchResult matchResult = pattern.match(json);

        //then
        assertTrue("Expected match when JSON attribute is present", matchResult.isExactMatch());
    }

    @Test
    public void shouldMatchesWhenExpectedElementIsAbsent() {
        //given
        final StringValuePattern pattern = WireMock.optionalMatchingJsonPath("$.one");
        final String json =
                "{\n" +
                        "  \"two\":2,\n" +
                        "  \"three\":3\n" +
                        "}";

        //when
        final MatchResult matchResult = pattern.match(json);

        //then
        assertTrue("Expected match when JSON attribute is present", matchResult.isExactMatch());
    }

    @Test
    public void shouldMatchesWhenJsonIsNull() {
        //given
        final StringValuePattern pattern = WireMock.optionalMatchingJsonPath("$.one");

        //when
        final MatchResult matchResult = pattern.match(null);

        //then
        assertTrue("Expected match when JSON attribute is present", matchResult.isExactMatch());
    }

    @Test
    public void shouldMatchesWhenJsonPathExistsAndFilterMatches() {
        //given
        final StringValuePattern pattern = WireMock.optionalMatchingJsonPath("$.[?(@.number == '2')]");
        final String json = "{\"number\": 2}";

        //when
        final MatchResult matchResult = pattern.match(json);

        //then
        assertTrue("Expected match when JSON attribute is present", matchResult.isExactMatch());
    }

    @Test
    public void shouldNotMatchesWhenJsonPathExistsAndFiltersDoesNotMatch() {
        //given
        final StringValuePattern pattern = WireMock.optionalMatchingJsonPath("$.[?(@.number == '2')]");
        final String json = "{\"number\": 3}";

        //when
        final MatchResult matchResult = pattern.match(json);

        //then
        assertFalse("Expected no match when JSON attribute is absent", matchResult.isExactMatch());
    }
}