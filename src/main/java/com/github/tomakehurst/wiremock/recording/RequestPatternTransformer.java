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

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.*;
import com.google.common.base.Function;
import java.util.Map;

/**
 * Creates a RequestPatternBuilder from a Request's URL, method, body (if present), and optionally
 * headers from a whitelist.
 */
public class RequestPatternTransformer implements Function<Request, RequestPatternBuilder> {
  private final Map<String, CaptureHeadersSpec> headers;
  private final RequestBodyPatternFactory bodyPatternFactory;

  public RequestPatternTransformer(
      Map<String, CaptureHeadersSpec> headers, RequestBodyPatternFactory bodyPatternFactory) {
    this.headers = headers;
    this.bodyPatternFactory = bodyPatternFactory;
  }

  /** Returns a RequestPatternBuilder matching a given Request */
  @Override
  public RequestPatternBuilder apply(Request request) {
    final RequestPatternBuilder builder =
        new RequestPatternBuilder(request.getMethod(), urlEqualTo(request.getUrl()));

    if (headers != null && !headers.isEmpty()) {
      for (Map.Entry<String, CaptureHeadersSpec> header : headers.entrySet()) {
        String headerName = header.getKey();
        if (request.containsHeader(headerName)) {
          CaptureHeadersSpec spec = header.getValue();
          StringValuePattern headerMatcher =
              new EqualToPattern(request.getHeader(headerName), spec.getCaseInsensitive());
          builder.withHeader(headerName, headerMatcher);
        }
      }
    }

    byte[] body = request.getBody();
    if (bodyPatternFactory != null && body != null && body.length > 0) {
      builder.withRequestBody(bodyPatternFactory.forRequest(request));
    }

    return builder;
  }
}
