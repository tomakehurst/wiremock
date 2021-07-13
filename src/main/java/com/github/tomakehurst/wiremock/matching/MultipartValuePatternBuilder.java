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
/*
 * Copyright (C) 2017 Arjan Duijzer
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
package com.github.tomakehurst.wiremock.matching;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MultipartValuePatternBuilder {

  private String name = null;
  private Map<String, MultiValuePattern> headerPatterns = newLinkedHashMap();
  private List<ContentPattern<?>> bodyPatterns = new LinkedList<>();
  private MultipartValuePattern.MatchingType matchingType = MultipartValuePattern.MatchingType.ANY;

  public MultipartValuePatternBuilder() {}

  public MultipartValuePatternBuilder(String name) {
    withName(name);
  }

  public MultipartValuePatternBuilder matchingType(MultipartValuePattern.MatchingType type) {
    matchingType = type;
    return this;
  }

  public MultipartValuePatternBuilder withName(String name) {
    this.name = name;
    return withHeader("Content-Disposition", containing("name=\"" + name + "\""));
  }

  public MultipartValuePatternBuilder withHeader(String name, StringValuePattern headerPattern) {
    headerPatterns.put(name, MultiValuePattern.of(headerPattern));
    return this;
  }

  public MultipartValuePatternBuilder withBody(ContentPattern<?> bodyPattern) {
    bodyPatterns.add(bodyPattern);
    return this;
  }

  public MultipartValuePattern build() {
    return headerPatterns.isEmpty() && bodyPatterns.isEmpty()
        ? null
        : headerPatterns.isEmpty()
            ? new MultipartValuePattern(name, matchingType, null, bodyPatterns)
            : new MultipartValuePattern(name, matchingType, headerPatterns, bodyPatterns);
  }
}
