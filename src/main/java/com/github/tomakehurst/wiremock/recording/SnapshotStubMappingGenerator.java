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

import com.github.tomakehurst.wiremock.common.SafeNames;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.base.Function;
import java.net.URI;
import java.util.Map;

/**
 * Transforms ServeEvents to StubMappings using RequestPatternTransformer and
 * LoggedResponseDefinitionTransformer
 */
public class SnapshotStubMappingGenerator implements Function<ServeEvent, StubMapping> {
  private final RequestPatternTransformer requestTransformer;
  private final LoggedResponseDefinitionTransformer responseTransformer;

  public SnapshotStubMappingGenerator(
      RequestPatternTransformer requestTransformer,
      LoggedResponseDefinitionTransformer responseTransformer) {
    this.requestTransformer = requestTransformer;
    this.responseTransformer = responseTransformer;
  }

  public SnapshotStubMappingGenerator(
      Map<String, CaptureHeadersSpec> captureHeaders,
      RequestBodyPatternFactory requestBodyPatternFactory) {
    this(
        new RequestPatternTransformer(captureHeaders, requestBodyPatternFactory),
        new LoggedResponseDefinitionTransformer());
  }

  @Override
  public StubMapping apply(ServeEvent event) {
    final RequestPattern requestPattern = requestTransformer.apply(event.getRequest()).build();
    final ResponseDefinition responseDefinition = responseTransformer.apply(event.getResponse());
    StubMapping stubMapping = new StubMapping(requestPattern, responseDefinition);

    URI uri = URI.create(event.getRequest().getUrl());
    stubMapping.setName(SafeNames.makeSafeNameFromUrl(uri.getPath()));

    return stubMapping;
  }
}
