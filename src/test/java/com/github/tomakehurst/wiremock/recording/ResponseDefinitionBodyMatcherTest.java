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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.junit.Test;

public class ResponseDefinitionBodyMatcherTest {
  @Test
  public void noThresholds() {
    ResponseDefinitionBodyMatcher matcher = new ResponseDefinitionBodyMatcher(0, 0);
    assertFalse(matcher.match(new ResponseDefinition()).isExactMatch());
    assertTrue(matcher.match(textResponseDefinition("a")).isExactMatch());
    assertTrue(matcher.match(binaryResponseDefinition(new byte[] {0x1})).isExactMatch());
  }

  @Test
  public void textBodyMatchingWithThreshold() {
    ResponseDefinitionBodyMatcher matcher = new ResponseDefinitionBodyMatcher(2, 0);
    assertFalse(matcher.match(textResponseDefinition("f")).isExactMatch());
    assertFalse(matcher.match(textResponseDefinition("fo")).isExactMatch());
    assertTrue(matcher.match(textResponseDefinition("foo")).isExactMatch());
  }

  @Test
  public void binaryBodyMatchingWithThreshold() {
    ResponseDefinitionBodyMatcher matcher = new ResponseDefinitionBodyMatcher(0, 2);
    assertFalse(matcher.match(binaryResponseDefinition(new byte[] {0x1})).isExactMatch());
    assertFalse(matcher.match(binaryResponseDefinition(new byte[] {0x1, 0xc})).isExactMatch());
    assertTrue(matcher.match(binaryResponseDefinition(new byte[] {0x1, 0xc, 0xf})).isExactMatch());
  }

  private static ResponseDefinition textResponseDefinition(String body) {
    return new ResponseDefinitionBuilder()
        .withHeader("Content-Type", "text/plain")
        .withBody(body)
        .build();
  }

  private static ResponseDefinition binaryResponseDefinition(byte[] body) {
    return new ResponseDefinitionBuilder().withBody(body).build();
  }
}
