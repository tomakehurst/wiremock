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

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import java.util.*;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static com.github.tomakehurst.wiremock.core.WireMockApp.FILES_ROOT;
import static com.github.tomakehurst.wiremock.http.ResponseDefinition.copyOf;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Iterables.tryFind;


public class InMemoryStubMappings implements StubMappings {
	
	private final SortedConcurrentMappingSet mappings = new SortedConcurrentMappingSet();
	private final Scenarios scenarios = new Scenarios();
	private final Map<String, RequestMatcherExtension> customMatchers;
    private final Map<String, ResponseDefinitionTransformer> transformers;
    private final FileSource rootFileSource;

	public InMemoryStubMappings(Map<String, RequestMatcherExtension> customMatchers, Map<String, ResponseDefinitionTransformer> transformers, FileSource rootFileSource) {
		this.customMatchers = customMatchers;
        this.transformers = transformers;
        this.rootFileSource = rootFileSource;
    }

	public InMemoryStubMappings() {
		this(Collections.<String, RequestMatcherExtension>emptyMap(),
             Collections.<String, ResponseDefinitionTransformer>emptyMap(),
             new SingleRootFileSource("."));
	}

	@Override
	public ServeEvent serveFor(Request request) {
		StubMapping matchingMapping = find(
				mappings,
				mappingMatchingAndInCorrectScenarioState(request),
				StubMapping.NOT_CONFIGURED);
		
		scenarios.onStubServed(matchingMapping);

        ResponseDefinition response = matchingMapping.getResponse();

		for (ResponseDefinitionTransformer transformer : response.selectApplicableResponseDefinitionTransformers(this.transformers.values())) {
			response = transformer.transform(request, response, rootFileSource.child(FILES_ROOT), response.getTransformerParameters());
		}

		return ServeEvent.of(
            LoggedRequest.createFrom(request),
            copyOf(response),
            matchingMapping
        );
	}

	@Override
	public void addMapping(StubMapping mapping) {
		mappings.add(mapping);
		scenarios.onStubMappingAddedOrUpdated(mapping, mappings);
	}

	@Override
	public void removeMapping(StubMapping mapping) {
		mappings.remove(mapping);
		scenarios.onStubMappingRemoved(mapping, mappings);
	}

	@Override
	public void editMapping(StubMapping stubMapping) {
		final Optional<StubMapping> optionalExistingMapping = tryFind(
				mappings,
				mappingMatchingUuid(stubMapping.getUuid())
		);

		if (!optionalExistingMapping.isPresent()) {
			String msg = "StubMapping with UUID: " + stubMapping.getUuid() + " not found";
			notifier().error(msg);
			throw new RuntimeException(msg);
		}

		final StubMapping existingMapping = optionalExistingMapping.get();

		stubMapping.setInsertionIndex(existingMapping.getInsertionIndex());
		stubMapping.setDirty(true);

		mappings.replace(existingMapping, stubMapping);
		scenarios.onStubMappingAddedOrUpdated(stubMapping, mappings);
	}


	@Override
	public void reset() {
		mappings.clear();
        scenarios.clear();
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
		return tryFind(mappings, new Predicate<StubMapping>() {
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
        return from(mappings).filter(new Predicate<StubMapping>() {
            @Override
            public boolean apply(StubMapping stub) {
                String metadataJson = Json.write(stub.getMetadata());
                return pattern.match(metadataJson).isExactMatch();
            }
        }).toList();
	}

    private Predicate<StubMapping> mappingMatchingAndInCorrectScenarioState(final Request request) {
		return mappingMatchingAndInCorrectScenarioStateNew(request);
    }

    private Predicate<StubMapping> mappingMatchingAndInCorrectScenarioStateNew(final Request request) {
		return new Predicate<StubMapping>() {
			public boolean apply(StubMapping mapping) {
				return mapping.getRequest().match(request, customMatchers).isExactMatch() &&
				(mapping.isIndependentOfScenarioState() || scenarios.mappingMatchesScenarioState(mapping));
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
