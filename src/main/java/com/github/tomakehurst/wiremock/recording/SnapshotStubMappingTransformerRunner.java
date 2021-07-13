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

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.StubMappingTransformer;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.base.Function;
import java.util.List;

/**
 * Applies all registered StubMappingTransformer extensions against a stub mapping when applicable,
 * passing them any supplied Parameters.
 */
public class SnapshotStubMappingTransformerRunner implements Function<StubMapping, StubMapping> {
  private final FileSource filesRoot;
  private final Parameters parameters;
  private final Iterable<StubMappingTransformer> registeredTransformers;
  private final List<String> requestedTransformers;

  public SnapshotStubMappingTransformerRunner(
      Iterable<StubMappingTransformer> registeredTransformers) {
    this(registeredTransformers, null, null, null);
  }

  public SnapshotStubMappingTransformerRunner(
      Iterable<StubMappingTransformer> registeredTransformers,
      List<String> requestedTransformers,
      Parameters parameters,
      FileSource filesRoot) {
    this.requestedTransformers = requestedTransformers;
    this.registeredTransformers = registeredTransformers;
    this.parameters = parameters;
    this.filesRoot = filesRoot;
  }

  @Override
  public StubMapping apply(StubMapping stubMapping) {
    for (StubMappingTransformer transformer : registeredTransformers) {
      if (transformer.applyGlobally()
          || (requestedTransformers != null
              && requestedTransformers.contains(transformer.getName()))) {
        stubMapping = transformer.transform(stubMapping, filesRoot, parameters);
      }
    }

    return stubMapping;
  }
}
