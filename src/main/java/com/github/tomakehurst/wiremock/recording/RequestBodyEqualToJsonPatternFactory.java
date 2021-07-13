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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;

public class RequestBodyEqualToJsonPatternFactory implements RequestBodyPatternFactory {

  private final Boolean ignoreArrayOrder;
  private final Boolean ignoreExtraElements;

  @JsonCreator
  public RequestBodyEqualToJsonPatternFactory(
      @JsonProperty("ignoreArrayOrder") Boolean ignoreArrayOrder,
      @JsonProperty("ignoreExtraElements") Boolean ignoreExtraElements) {
    this.ignoreArrayOrder = ignoreArrayOrder;
    this.ignoreExtraElements = ignoreExtraElements;
  }

  public Boolean isIgnoreArrayOrder() {
    return ignoreArrayOrder;
  }

  public Boolean isIgnoreExtraElements() {
    return ignoreExtraElements;
  }

  @Override
  public EqualToJsonPattern forRequest(Request request) {
    return new EqualToJsonPattern(request.getBodyAsString(), ignoreArrayOrder, ignoreExtraElements);
  }
}
