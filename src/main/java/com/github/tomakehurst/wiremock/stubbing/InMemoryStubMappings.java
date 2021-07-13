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
package com.github.tomakehurst.wiremock.stubbing;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static com.github.tomakehurst.wiremock.core.WireMockApp.FILES_ROOT;
import static com.github.tomakehurst.wiremock.http.ResponseDefinition.copyOf;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Iterables.tryFind;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.extension.StubLifecycleListener;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import java.util.*;

public class InMemoryStubMappings implements StubMappings {

  private final SortedConcurrentMappingSet mappings = new SortedConcurrentMappingSet();
  private final Scenarios scenarios;
  private final Map<String, RequestMatcherExtension> customMatchers;
  private final Map<String, ResponseDefinitionTransformer> transformers;
  private final FileSource rootFileSource;
  private final List<StubLifecycleListener> stubLifecycleListeners;

  public InMemoryStubMappings(
      Scenarios scenarios,
      Map<String, RequestMatcherExtension> customMatchers,
      Map<String, ResponseDefinitionTransformer> transformers,
      FileSource rootFileSource,
      List<StubLifecycleListener> stubLifecycleListeners) {
    this.scenarios = scenarios;
    this.customMatchers = customMatchers;
    this.transformers = transformers;
    this.rootFileSource = rootFileSource;
    this.stubLifecycleListeners = stubLifecycleListeners;
  }

  public InMemoryStubMappings() {
    this(
        new Scenarios(),
        Collections.<String, RequestMatcherExtension>emptyMap(),
        Collections.<String, ResponseDefinitionTransformer>emptyMap(),
        new SingleRootFileSource("."),
        Collections.<StubLifecycleListener>emptyList());
  }

  @Override
  public ServeEvent serveFor(Request request) {
    StubMapping matchingMapping =
        find(
            mappings,
            mappingMatchingAndInCorrectScenarioState(request),
            StubMapping.NOT_CONFIGURED);

    scenarios.onStubServed(matchingMapping);

    ResponseDefinition responseDefinition =
        applyTransformations(
            request, matchingMapping.getResponse(), ImmutableList.copyOf(transformers.values()));

    return ServeEvent.of(
        LoggedRequest.createFrom(request), copyOf(responseDefinition), matchingMapping);
  }

  private ResponseDefinition applyTransformations(
      Request request,
      ResponseDefinition responseDefinition,
      List<ResponseDefinitionTransformer> transformers) {
    if (transformers.isEmpty()) {
      return responseDefinition;
    }

    ResponseDefinitionTransformer transformer = transformers.get(0);
    ResponseDefinition newResponseDef =
        transformer.applyGlobally() || responseDefinition.hasTransformer(transformer)
            ? transformer.transform(
                request,
                responseDefinition,
                rootFileSource.child(FILES_ROOT),
                firstNonNull(responseDefinition.getTransformerParameters(), Parameters.empty()))
            : responseDefinition;

    return applyTransformations(
        request, newResponseDef, transformers.subList(1, transformers.size()));
  }

  @Override
  public void addMapping(StubMapping mapping) {
    for (StubLifecycleListener listener : stubLifecycleListeners) {
      listener.beforeStubCreated(mapping);
    }

    mappings.add(mapping);
    scenarios.onStubMappingAdded(mapping);

    for (StubLifecycleListener listener : stubLifecycleListeners) {
      listener.afterStubCreated(mapping);
    }
  }

  @Override
  public void removeMapping(StubMapping mapping) {
    for (StubLifecycleListener listener : stubLifecycleListeners) {
      listener.beforeStubRemoved(mapping);
    }

    mappings.remove(mapping);
    scenarios.onStubMappingRemoved(mapping);

    for (StubLifecycleListener listener : stubLifecycleListeners) {
      listener.afterStubRemoved(mapping);
    }
  }

  @Override
  public void editMapping(StubMapping stubMapping) {
    final Optional<StubMapping> optionalExistingMapping =
        tryFind(mappings, mappingMatchingUuid(stubMapping.getUuid()));

    if (!optionalExistingMapping.isPresent()) {
      String msg = "StubMapping with UUID: " + stubMapping.getUuid() + " not found";
      notifier().error(msg);
      throw new RuntimeException(msg);
    }

    final StubMapping existingMapping = optionalExistingMapping.get();
    for (StubLifecycleListener listener : stubLifecycleListeners) {
      listener.beforeStubEdited(existingMapping, stubMapping);
    }

    stubMapping.setInsertionIndex(existingMapping.getInsertionIndex());
    stubMapping.setDirty(true);

    mappings.replace(existingMapping, stubMapping);
    scenarios.onStubMappingUpdated(existingMapping, stubMapping);

    for (StubLifecycleListener listener : stubLifecycleListeners) {
      listener.afterStubEdited(existingMapping, stubMapping);
    }
  }

  @Override
  public void reset() {
    for (StubLifecycleListener listener : stubLifecycleListeners) {
      listener.beforeStubsReset();
    }

    mappings.clear();
    scenarios.clear();

    for (StubLifecycleListener listener : stubLifecycleListeners) {
      listener.afterStubsReset();
    }
  }

  @Override
  public void resetScenarios() {
    scenarios.reset();
  }

  @Override
  public List<StubMapping> getAll() {
    return ImmutableList.copyOf(mappings);
  }

  @Override
  public Optional<StubMapping> get(final UUID id) {
    return tryFind(
        mappings,
        new Predicate<StubMapping>() {
          @Override
          public boolean apply(StubMapping input) {
            return input.getUuid().equals(id);
          }
        });
  }

  @Override
  public List<Scenario> getAllScenarios() {
    return scenarios.getAll();
  }

  @Override
  public List<StubMapping> findByMetadata(final StringValuePattern pattern) {
    return from(mappings)
        .filter(
            new Predicate<StubMapping>() {
              @Override
              public boolean apply(StubMapping stub) {
                String metadataJson = Json.write(stub.getMetadata());
                return pattern.match(metadataJson).isExactMatch();
              }
            })
        .toList();
  }

  private Predicate<StubMapping> mappingMatchingAndInCorrectScenarioState(final Request request) {
    return mappingMatchingAndInCorrectScenarioStateNew(request);
  }

  private Predicate<StubMapping> mappingMatchingAndInCorrectScenarioStateNew(
      final Request request) {
    return new Predicate<StubMapping>() {
      public boolean apply(StubMapping mapping) {
        return mapping.getRequest().match(request, customMatchers).isExactMatch()
            && (mapping.isIndependentOfScenarioState()
                || scenarios.mappingMatchesScenarioState(mapping));
      }
    };
  }

  private Predicate<StubMapping> mappingMatchingUuid(final UUID uuid) {
    return new Predicate<StubMapping>() {
      @Override
      public boolean apply(StubMapping input) {
        return input.getUuid().equals(uuid);
      }
    };
  }
}
