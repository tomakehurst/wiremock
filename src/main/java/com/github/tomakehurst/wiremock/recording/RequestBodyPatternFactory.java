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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.ContentPattern;

/** Factory for the StringValuePattern to use in a recorded stub mapping to match request bodies */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "matcher",
    defaultImpl = RequestBodyAutomaticPatternFactory.class)
@JsonSubTypes({
  @JsonSubTypes.Type(value = RequestBodyAutomaticPatternFactory.class, name = "auto"),
  @JsonSubTypes.Type(value = RequestBodyEqualToPatternFactory.class, name = "equalTo"),
  @JsonSubTypes.Type(value = RequestBodyEqualToJsonPatternFactory.class, name = "equalToJson"),
  @JsonSubTypes.Type(value = RequestBodyEqualToXmlPatternFactory.class, name = "equalToXml")
})
public interface RequestBodyPatternFactory {
  ContentPattern<?> forRequest(Request request);
}
