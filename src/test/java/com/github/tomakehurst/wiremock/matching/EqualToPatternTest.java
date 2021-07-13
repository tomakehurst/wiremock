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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.common.JsonException;
import org.junit.Test;

public class EqualToPatternTest {

  @Test
  public void returnsANonZeroScoreForPartialMatchOnEquals() {
    StringValuePattern pattern = WireMock.equalTo("matchthis");
    assertThat(pattern.match("matchthisbadlydone").getDistance(), is(0.5));
  }

  @Test
  public void returns1ForNoMatchOnEquals() {
    StringValuePattern pattern = WireMock.equalTo("matchthis");
    assertThat(pattern.match("924387348975923").getDistance(), is(1.0));
  }

  @Test
  public void returns0ForExactMatchOnEquals() {
    StringValuePattern pattern = WireMock.equalTo("matchthis");
    assertThat(pattern.match("matchthis").getDistance(), is(0.0));
  }

  @Test
  public void matchesCaseInsensitive() {
    StringValuePattern pattern = WireMock.equalToIgnoreCase("MaTchtHis");
    assertThat(pattern.match("matchthis").isExactMatch(), is(true));
  }

  @Test
  public void correctlyDeserialisesEqualToFromJson() {
    StringValuePattern stringValuePattern =
        Json.read(
            "{                               \n" + "  \"equalTo\": \"something\"    \n" + "}",
            StringValuePattern.class);

    assertThat(stringValuePattern, instanceOf(EqualToPattern.class));
    assertThat(stringValuePattern.getValue(), is("something"));
  }

  @Test
  public void correctlyDeserialisesEqualToFromJsonWithIgnoreCase() {
    StringValuePattern stringValuePattern =
        Json.read(
            "{                              \n"
                + "  \"equalTo\": \"something\",   \n"
                + "  \"caseInsensitive\": true     \n"
                + "}",
            StringValuePattern.class);

    assertThat(stringValuePattern, instanceOf(EqualToPattern.class));
    assertThat(stringValuePattern.getValue(), is("something"));
    assertThat(((EqualToPattern) stringValuePattern).getCaseInsensitive(), is(true));
  }

  @Test
  public void correctlySerialisesToJson() throws Exception {
    assertEquals(
        "{                               \n" + "  \"equalTo\": \"something\"    \n" + "}",
        Json.write(new EqualToPattern("something")),
        false);
  }

  @Test
  public void failsWithMeaningfulErrorWhenOperatorNotRecognised() {
    try {
      Json.read(
          "{                               \n" + "  \"munches\": \"something\"    \n" + "}",
          StringValuePattern.class);

      fail();
    } catch (Exception e) {
      assertThat(e, instanceOf(JsonException.class));
      JsonException jsonException = (JsonException) e;
      assertThat(
          jsonException.getErrors().first().getDetail(),
          containsString("{\"munches\":\"something\"} is not a valid match operation"));
    }
  }

  @Test
  public void noMatchOnNullValue() {
    assertThat(WireMock.equalTo("this_thing").match(null).isExactMatch(), is(false));
  }
}
