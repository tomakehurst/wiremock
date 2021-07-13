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

import static com.github.tomakehurst.wiremock.common.ContentTypes.determineIsTextFromMimeType;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.net.HttpHeaders.*;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Gzip;
import com.github.tomakehurst.wiremock.common.Strings;
import com.github.tomakehurst.wiremock.http.*;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Transforms a LoggedResponse into a ResponseDefinition, which will be used to construct a
 * StubMapping
 */
public class LoggedResponseDefinitionTransformer
    implements Function<LoggedResponse, ResponseDefinition> {

  private static final List<CaseInsensitiveKey> EXCLUDED_HEADERS =
      ImmutableList.of(
          CaseInsensitiveKey.from(CONTENT_ENCODING),
          CaseInsensitiveKey.from(CONTENT_LENGTH),
          CaseInsensitiveKey.from(TRANSFER_ENCODING));

  @Override
  public ResponseDefinition apply(LoggedResponse response) {
    final ResponseDefinitionBuilder responseDefinitionBuilder =
        new ResponseDefinitionBuilder().withStatus(response.getStatus());

    if (response.getBody() != null && response.getBody().length > 0) {

      byte[] body = bodyDecompressedIfRequired(response);
      String mimeType = response.getMimeType();
      Charset charset = response.getCharset();
      if (determineIsTextFromMimeType(mimeType)) {
        responseDefinitionBuilder.withBody(Strings.stringFromBytes(body, charset));
      } else {
        responseDefinitionBuilder.withBody(body);
      }
    }

    if (response.getHeaders() != null) {
      responseDefinitionBuilder.withHeaders(withoutContentEncodingAndContentLength(response));
    }

    return responseDefinitionBuilder.build();
  }

  private byte[] bodyDecompressedIfRequired(LoggedResponse response) {
    if (response.getHeaders() != null
        && response.getHeaders().getHeader(CONTENT_ENCODING).containsValue("gzip")) {
      return Gzip.unGzip(response.getBody());
    }
    return response.getBody();
  }

  private HttpHeaders withoutContentEncodingAndContentLength(LoggedResponse response) {
    return new HttpHeaders(
        filter(
            response.getHeaders().all(),
            new Predicate<HttpHeader>() {
              public boolean apply(HttpHeader header) {
                return !EXCLUDED_HEADERS.contains(header.caseInsensitiveKey());
              }
            }));
  }
}
