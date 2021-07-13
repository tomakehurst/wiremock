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
package com.github.tomakehurst.wiremock.recording;

import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.common.Json;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;

public class ResponseDefinitionBodyMatcherDeserializerTest {
  @Test
  public void correctlyParsesFileSize() {
    final Map<String, Long> testCases =
        ImmutableMap.<String, Long>builder()
            .put("100", 100L)
            .put("1KB", 1024L)
            .put("1 kb", 1024L)
            .put("1024 K", 1024L * 1024)
            .put("10 Mb", 10L * 1024 * 1024)
            .put("10.5 GB", Math.round(10.5 * 1024 * 1024 * 1024))
            .build();

    for (String input : testCases.keySet()) {
      Long expected = testCases.get(input);
      Long actual = ResponseDefinitionBodyMatcherDeserializer.parseFilesize(input);
      assertEquals("Failed with " + input, expected, actual);
    }
  }

  @Test
  public void correctlyDeserializesWithEmptyNode() {
    ResponseDefinitionBodyMatcher matcher = Json.read("{}", ResponseDefinitionBodyMatcher.class);
    assertEquals(new ResponseDefinitionBodyMatcher(Long.MAX_VALUE, Long.MAX_VALUE), matcher);
  }

  @Test
  public void correctlyDeserializesWithSingleValue() {
    ResponseDefinitionBodyMatcher matcher =
        Json.read("{ \"textSizeThreshold\": 100 }", ResponseDefinitionBodyMatcher.class);
    assertEquals(new ResponseDefinitionBodyMatcher(100, Long.MAX_VALUE), matcher);
  }

  @Test
  public void correctlyDeserializesWithBothValues() {
    ResponseDefinitionBodyMatcher matcher =
        Json.read(
            "{ \"textSizeThreshold\": 100, \"binarySizeThreshold\": 10 }",
            ResponseDefinitionBodyMatcher.class);
    assertEquals(new ResponseDefinitionBodyMatcher(100, 10), matcher);
  }
}
