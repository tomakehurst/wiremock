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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;

public class RequestPatternTransformerTest {
  @Test
  public void applyIncludesMethodAndUrlMatchers() {
    Request request =
        mockRequest()
            .url("/foo")
            .method(RequestMethod.GET)
            .header("User-Agent", "foo")
            .header("X-Foo", "bar");

    RequestPatternBuilder expected =
        new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/foo"));

    assertEquals(
        expected.build(), new RequestPatternTransformer(null, null).apply(request).build());
  }

  @Test
  public void applyWithHeaders() {
    Request request =
        mockRequest()
            .url("/")
            .method(RequestMethod.POST)
            .header("X-CaseSensitive", "foo")
            .header("X-Ignored", "ignored")
            .header("X-CaseInsensitive", "Baz");

    RequestPatternBuilder expected =
        new RequestPatternBuilder(RequestMethod.POST, urlEqualTo("/"))
            .withHeader("X-CaseSensitive", equalTo("foo"))
            .withHeader("X-CaseInsensitive", equalToIgnoreCase("Baz"));

    Map<String, CaptureHeadersSpec> headers =
        ImmutableMap.of(
            "X-CaseSensitive", new CaptureHeadersSpec(false),
            "X-CaseInsensitive", new CaptureHeadersSpec(true));

    assertEquals(
        expected.build(), new RequestPatternTransformer(headers, null).apply(request).build());
  }
}
