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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.tomakehurst.wiremock.common.ContentTypes;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import java.util.Objects;

// Matches the size of the body of a ResponseDefinition, for use by the Snapshot API when
// determining if the body
// should be extracted to a file.
@JsonDeserialize(using = ResponseDefinitionBodyMatcherDeserializer.class)
public class ResponseDefinitionBodyMatcher implements ValueMatcher<ResponseDefinition> {

  public static final long DEFAULT_MAX_TEXT_SIZE = 10240;
  public static final long DEFAULT_MAX_BINARY_SIZE = 0;

  private final long textSizeThreshold;
  private final long binarySizeThreshold;

  public ResponseDefinitionBodyMatcher(long textSizeThreshold, long binarySizeThreshold) {
    this.textSizeThreshold = textSizeThreshold;
    this.binarySizeThreshold = binarySizeThreshold;
  }

  public String getTextSizeThreshold() {
    return String.valueOf(textSizeThreshold);
  }

  public String getBinarySizeThreshold() {
    return String.valueOf(binarySizeThreshold);
  }

  @Override
  public MatchResult match(ResponseDefinition responseDefinition) {
    if (!responseDefinition.specifiesBodyContent()) {
      return MatchResult.noMatch();
    } else if (responseDefinition.getHeaders() != null
        && ContentTypes.determineIsTextFromMimeType(
            responseDefinition.getHeaders().getContentTypeHeader().mimeTypePart())) {
      if (responseDefinition.getTextBody().length() > textSizeThreshold) {
        return MatchResult.exactMatch();
      } else {
        return MatchResult.noMatch();
      }
    } else {
      if (responseDefinition.getByteBody().length > binarySizeThreshold) {
        return MatchResult.exactMatch();
      } else {
        return MatchResult.noMatch();
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ResponseDefinitionBodyMatcher that = (ResponseDefinitionBodyMatcher) o;

    return Objects.equals(textSizeThreshold, that.textSizeThreshold)
        && Objects.equals(binarySizeThreshold, that.binarySizeThreshold);
  }

  @Override
  public int hashCode() {
    return Objects.hash(textSizeThreshold, binarySizeThreshold);
  }
}
