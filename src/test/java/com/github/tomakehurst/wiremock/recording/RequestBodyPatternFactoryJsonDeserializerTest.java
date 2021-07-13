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

import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.junit.Test;

public class RequestBodyPatternFactoryJsonDeserializerTest {
  @Test
  public void correctlyDeserializesWithEmptyObject() {
    RequestBodyPatternFactory bodyPatternFactory = deserializeJson("{}");
    assertThat(bodyPatternFactory, instanceOf(RequestBodyAutomaticPatternFactory.class));
  }

  @Test
  public void correctlyDeserializesWithAutoMatcher() {
    RequestBodyPatternFactory bodyPatternFactory = deserializeJson("{ \"matcher\": \"auto\" }");
    assertThat(bodyPatternFactory, instanceOf(RequestBodyAutomaticPatternFactory.class));
  }

  @Test
  public void correctlyDeserializesWithEqualToMatcher() {
    RequestBodyPatternFactory bodyPatternFactory =
        deserializeJson(
            "{                                    \n"
                + "    \"matcher\": \"equalTo\",        \n"
                + "    \"caseInsensitive\": true        \n"
                + "}                                      ");
    EqualToPattern bodyPattern =
        (EqualToPattern) bodyPatternFactory.forRequest(mockRequest().body("this body text"));
    assertThat(bodyPattern.getCaseInsensitive(), is(true));
    assertThat(bodyPattern.getExpected(), is("this body text"));
  }

  @Test
  public void correctlyDeserializesWithEqualToJsonMatcher() {
    RequestBodyPatternFactory bodyPatternFactory =
        deserializeJson(
            "{                                    \n"
                + "    \"matcher\": \"equalToJson\",    \n"
                + "    \"ignoreArrayOrder\": false,     \n"
                + "    \"ignoreExtraElements\": true    \n"
                + "}                                      ");
    EqualToJsonPattern bodyPattern =
        (EqualToJsonPattern) bodyPatternFactory.forRequest(mockRequest().body("1"));
    assertThat(bodyPattern.isIgnoreArrayOrder(), is(false));
    assertThat(bodyPattern.isIgnoreExtraElements(), is(true));
  }

  @Test
  public void correctlyDeserializesWithEqualToXmlMatcher() {
    RequestBodyPatternFactory bodyPatternFactory =
        deserializeJson("{ \"matcher\": \"equalToXml\" }");
    assertThat(bodyPatternFactory, instanceOf(RequestBodyEqualToXmlPatternFactory.class));
  }

  private static RequestBodyPatternFactory deserializeJson(String json) {
    return Json.read(json, RequestBodyPatternFactory.class);
  }
}
