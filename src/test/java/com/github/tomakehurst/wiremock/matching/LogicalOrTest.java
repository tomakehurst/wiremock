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

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import org.junit.Test;

public class LogicalOrTest {

  @Test
  public void matchesWhenAnyContainedMatchersMatch() {
    StringValuePattern matcher =
        WireMock.or(
            WireMock.before("2020-01-01T00:00:00Z"), WireMock.after("2021-01-01T00:00:00Z"));

    assertThat(
        matcher.getExpected(), is("before 2020-01-01T00:00:00Z OR after 2021-01-01T00:00:00Z"));

    assertTrue(matcher.match("2022-06-01T11:22:33Z").isExactMatch());
    assertTrue(matcher.match("2019-06-01T11:22:33Z").isExactMatch());
    assertFalse(matcher.match("2020-06-01T11:22:33Z").isExactMatch());
  }

  @Test
  public void serialisesCorrectlyToJson() {
    StringValuePattern matcher =
        WireMock.or(
            WireMock.before("2020-01-01T00:00:00Z"), WireMock.after("2021-01-01T00:00:00Z"));

    assertThat(
        Json.write(matcher),
        jsonEquals(
            "{\n"
                + "  \"or\": [\n"
                + "    {\n"
                + "      \"before\": \"2020-01-01T00:00:00Z\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"after\": \"2021-01-01T00:00:00Z\"\n"
                + "    }\n"
                + "  ]\n"
                + "}"));
  }

  @Test
  public void deserialisesCorrectlyFromJson() {
    LogicalOr matcher =
        Json.read(
            "{\n"
                + "  \"or\": [\n"
                + "    {\n"
                + "      \"before\": \"2020-01-01T00:00:00Z\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"after\": \"2021-01-01T00:00:00Z\"\n"
                + "    }\n"
                + "  ]\n"
                + "}",
            LogicalOr.class);

    ContentPattern<?> first = matcher.getOr().get(0);
    ContentPattern<?> second = matcher.getOr().get(1);

    assertThat(first, instanceOf(BeforeDateTimePattern.class));
    assertThat(first.getExpected(), is("2020-01-01T00:00:00Z"));

    assertThat(second, instanceOf(AfterDateTimePattern.class));
  }

  @Test
  public void returnsDistanceFromClosestMatchWhenNotAnExactMatch() {
    LogicalOr matcher =
        WireMock.equalTo("abcde").or(WireMock.equalTo("defgh")).or(WireMock.equalTo("hijkl"));

    MatchResult matchResult = matcher.match("efgh");

    assertFalse(matchResult.isExactMatch());

    double expectedDistance = WireMock.equalTo("defgh").match("efgh").getDistance();
    assertThat(matchResult.getDistance(), is(expectedDistance));
  }
}
