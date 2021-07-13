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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.*;

public class RequestBodyAutomaticPatternFactory implements RequestBodyPatternFactory {

  private final Boolean caseInsensitive;
  private final Boolean ignoreArrayOrder;
  private final Boolean ignoreExtraElements;

  @JsonCreator
  public RequestBodyAutomaticPatternFactory(
      @JsonProperty("ignoreArrayOrder") Boolean ignoreArrayOrder,
      @JsonProperty("ignoreExtraElements") Boolean ignoreExtraElements,
      @JsonProperty("caseInsensitive") Boolean caseInsensitive) {
    this.ignoreArrayOrder = ignoreArrayOrder == null ? true : ignoreArrayOrder;
    this.ignoreExtraElements = ignoreExtraElements == null ? true : ignoreExtraElements;
    this.caseInsensitive = caseInsensitive == null ? false : caseInsensitive;
  }

  private RequestBodyAutomaticPatternFactory() {
    this(null, null, null);
  }

  public static final RequestBodyAutomaticPatternFactory DEFAULTS =
      new RequestBodyAutomaticPatternFactory();

  public Boolean isIgnoreArrayOrder() {
    return ignoreArrayOrder;
  }

  public Boolean isIgnoreExtraElements() {
    return ignoreExtraElements;
  }

  public Boolean isCaseInsensitive() {
    return caseInsensitive;
  }

  /**
   * If request body was JSON or XML, use "equalToJson" or "equalToXml" (respectively) in the
   * RequestPattern so it's easier to read. Otherwise, just use "equalTo"
   */
  @Override
  public ContentPattern<?> forRequest(Request request) {
    final String mimeType = request.getHeaders().getContentTypeHeader().mimeTypePart();
    if (mimeType != null) {
      if (mimeType.contains("json")) {
        return new EqualToJsonPattern(
            request.getBodyAsString(), ignoreArrayOrder, ignoreExtraElements);
      } else if (mimeType.contains("xml")) {
        return new EqualToXmlPattern(request.getBodyAsString());
      } else if (mimeType.equals("multipart/form-data")) {
        // TODO: Need to add a matcher that can handle multipart data properly. For now, just always
        // match
        return new AnythingPattern();
      } else if (!determineIsTextFromMimeType(mimeType)) {
        return new BinaryEqualToPattern(request.getBody());
      }
    }

    return new EqualToPattern(request.getBodyAsString(), caseInsensitive);
  }
}
