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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.google.common.io.BaseEncoding;
import org.junit.Test;

public class BinaryEqualToPatternPatternTest {

  @Test
  public void returns1ForNonMatch() {
    ValueMatcher<byte[]> pattern = WireMock.binaryEqualTo(new byte[] {1, 2, 3});
    byte[] actual = {4, 5, 6};

    MatchResult match = pattern.match(actual);

    assertThat(match.getDistance(), is(1.0));
    assertThat(match.isExactMatch(), is(false));
  }

  @Test
  public void returns0WhenExactlyEqual() {
    ValueMatcher<byte[]> pattern = WireMock.binaryEqualTo(new byte[] {1, 2, 3});
    byte[] actual = {1, 2, 3};

    MatchResult match = pattern.match(actual);

    assertThat(match.getDistance(), is(0.0));
    assertThat(match.isExactMatch(), is(true));
  }

  @Test
  public void returnsNonMatchWheActualIsNull() {
    ValueMatcher<byte[]> pattern = WireMock.binaryEqualTo(new byte[] {1, 2, 3});
    byte[] actual = null;

    MatchResult match = pattern.match(actual);

    assertThat(match.getDistance(), is(1.0));
    assertThat(match.isExactMatch(), is(false));
  }

  @Test
  public void serialisesCorrectly() throws Exception {
    byte[] expected = {5, 5, 5, 5};
    String base64Expected = BaseEncoding.base64().encode(expected);
    String expectedJson =
        "{                                                   \n"
            + "  \"binaryEqualTo\": \""
            + base64Expected
            + "\"     \n"
            + "}";
    assertEquals(expectedJson, Json.write(new BinaryEqualToPattern(expected)), true);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void deserialisesCorrectly() {
    String base64Expected = BaseEncoding.base64().encode(new byte[] {1, 2, 3});

    ContentPattern<byte[]> pattern =
        Json.read(
            "{                                              \n"
                + "  \"binaryEqualTo\": \""
                + base64Expected
                + "\"    \n"
                + "}",
            ContentPattern.class);

    assertThat(pattern, instanceOf(BinaryEqualToPattern.class));
    assertThat(pattern.getExpected(), is(base64Expected));
  }
}
