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
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;

public class OptionalNegativeRegexPatternTest {

    @Test
    public void shouldMatchWhenCorrectValueIsPresent() {
        //given
        final String regex = "something";
        final OptionalNegativeRegexPattern pattern = new OptionalNegativeRegexPattern(regex);

        //when
        final MatchResult matchResult = pattern.match("else");

        //then
        assertThat(matchResult.isExactMatch(), is(true));
    }

    @Test
    public void shouldNotMatchWhenIncorrectValueIsPresent() {
        //given
        final String regex = "something";
        final OptionalNegativeRegexPattern pattern = new OptionalNegativeRegexPattern(regex);

        //when
        final MatchResult matchResult = pattern.match("something");

        //then
        assertThat(matchResult.isExactMatch(), is(false));
    }

    @Test
    public void shouldMatchWhenValueIsAbsent() {
        //given
        final String regex = "something";
        final OptionalRegexPattern pattern = new OptionalRegexPattern(regex);

        //when
        final MatchResult matchResult = pattern.match(null);

        //then
        assertThat(matchResult.isExactMatch(), is(true));
    }

    @Test
    public void shouldCorrectlySerializeToJson() throws Exception {
        //given

        //when
        final String actual = Json.write(WireMock.optionalNotMatching("something"));
        System.out.println(actual);

        //then
        JSONAssert.assertEquals(
                "{                                       \n" +
                        "  \"doesNotMatchOrAbsent\": \"something\"    \n" +
                        "}",
                actual,
                true
        );
    }

    @Test
    public void shouldCorrectlyDeserializeFromJson() {
        //given

        //when
        final StringValuePattern stringValuePattern = Json.read(
                "{                                   \n" +
                        "  \"doesNotMatchOrAbsent\": \"something\"\n" +
                        "}",
                StringValuePattern.class);

        //then
        assertThat(stringValuePattern, instanceOf(OptionalNegativeRegexPattern.class));
        assertThat(stringValuePattern.getValue(), Matchers.is("something"));
    }
}