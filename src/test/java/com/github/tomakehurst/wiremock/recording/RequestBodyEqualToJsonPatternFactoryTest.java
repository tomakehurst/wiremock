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
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import org.junit.Test;

public class RequestBodyEqualToJsonPatternFactoryTest {

  @Test
  public void withIgnoreArrayOrder() {
    RequestBodyEqualToJsonPatternFactory patternFactory =
        new RequestBodyEqualToJsonPatternFactory(true, false);
    EqualToJsonPattern pattern = patternFactory.forRequest(mockRequest().body("{}"));

    assertThat(pattern.getEqualToJson(), is("{}"));
    assertThat(pattern.isIgnoreExtraElements(), is(false));
    assertThat(pattern.isIgnoreArrayOrder(), is(true));
  }

  @Test
  public void withIgnoreExtraElements() {
    RequestBodyEqualToJsonPatternFactory patternFactory =
        new RequestBodyEqualToJsonPatternFactory(false, true);
    EqualToJsonPattern pattern = patternFactory.forRequest(mockRequest().body("{}"));

    assertThat(pattern.getEqualToJson(), is("{}"));
    assertThat(pattern.isIgnoreExtraElements(), is(true));
    assertThat(pattern.isIgnoreArrayOrder(), is(false));
  }
}
