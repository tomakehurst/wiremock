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
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static com.github.tomakehurst.wiremock.core.WireMockApp.FILES_ROOT;
import static com.github.tomakehurst.wiremock.http.ResponseDefinition.copyOf;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Iterables.tryFind;


public class StubMappingsWithTransformersAndCustomMatchers implements StubMappings {
	private final MappingsRepository mappingsRepository;
	private final ConcurrentHashMap<String, Scenario> scenarioMap = new ConcurrentHashMap<>();
	private final Map<String, RequestMatcherExtension> customMatchers;
	private final Map<String, ResponseDefinitionTransformer> transformers;
	private final FileSource rootFileSource;
	private final StubMapping defaultMapping;

	public StubMappingsWithTransformersAndCustomMatchers(Map<String, RequestMatcherExtension> customMatchers, Map<String, ResponseDefinitionTransformer> transformers, FileSource rootFileSource, MappingsRepository mappingsRepository, StubMapping defaultMapping) {
		this.customMatchers = customMatchers;
		this.transformers = transformers;
		this.rootFileSource = rootFileSource;
		this.mappingsRepository = mappingsRepository;
		this.defaultMapping = defaultMapping;
	}

	public StubMappingsWithTransformersAndCustomMatchers(Map<String, RequestMatcherExtension> customMatchers, Map<String, ResponseDefinitionTransformer> transformers, FileSource rootFileSource, MappingsRepository mappingsRepository) {
		this(customMatchers, transformers, rootFileSource, mappingsRepository, StubMapping.NOT_CONFIGURED);
	}

	@VisibleForTesting
	public StubMappingsWithTransformersAndCustomMatchers() {
		this(Collections.<String, RequestMatcherExtension>emptyMap(),
				Collections.<String, ResponseDefinitionTransformer>emptyMap(),
				new SingleRootFileSource("."),
				new SortedConcurrentSetMappingsRepository());
	}

	@Override
	public ServeEvent serveFor(Request request) {
		StubMapping matchingMapping = find(
				mappingsRepository.getAll(),
				mappingMatchingAndInCorrectScenarioState(request),
				defaultMapping);

		matchingMapping.updateScenarioStateIfRequired();

		ResponseDefinition responseDefinition = applyTransformations(request,
				matchingMapping.getResponse(),
				ImmutableList.copyOf(transformers.values()));

		return ServeEvent.of(
				LoggedRequest.createFrom(request),
				copyOf(responseDefinition),
				matchingMapping
		);
	}

	private ResponseDefinition applyTransformations(Request request,
													ResponseDefinition responseDefinition,
													List<ResponseDefinitionTransformer> transformers) {
		if (transformers.isEmpty()) {
			return responseDefinition;
		}

		ResponseDefinitionTransformer transformer = transformers.get(0);
		ResponseDefinition newResponseDef =
				transformer.applyGlobally() || responseDefinition.hasTransformer(transformer) ?
						transformer.transform(request, responseDefinition, rootFileSource.child(FILES_ROOT), responseDefinition.getTransformerParameters()) :
						responseDefinition;

		return applyTransformations(request, newResponseDef, transformers.subList(1, transformers.size()));
	}

	@Override
	public void addMapping(StubMapping mapping) {
		updateSenarioMapIfPresent(mapping);
		mappingsRepository.add(mapping);
	}

	@Override
	public void removeMapping(StubMapping mapping) {
		removeFromSenarioMapIfPresent(mapping);
		mappingsRepository.remove(mapping);
	}

	@Override
	public void editMapping(StubMapping stubMapping) {
		final Optional<StubMapping> optionalExistingMapping = tryFind(
				mappingsRepository.getAll(),
				mappingMatchingUuid(stubMapping.getUuid())
		);

		if (!optionalExistingMapping.isPresent()) {
			String msg = "StubMapping with UUID: " + stubMapping.getUuid() + " not found";
			notifier().error(msg);
			throw new RuntimeException(msg);
		}

		final StubMapping existingMapping = optionalExistingMapping.get();

		updateSenarioMapIfPresent(stubMapping);
		stubMapping.setInsertionIndex(existingMapping.getInsertionIndex());
		stubMapping.setDirty(true);

		mappingsRepository.replace(existingMapping, stubMapping);
	}

	private void removeFromSenarioMapIfPresent(StubMapping mapping) {
		if (mapping.isInScenario()) {
			scenarioMap.remove(mapping.getScenarioName(), Scenario.inStartedState());
		}
	}

	private void updateSenarioMapIfPresent(StubMapping mapping) {
		if (mapping.isInScenario()) {
			scenarioMap.putIfAbsent(mapping.getScenarioName(), Scenario.inStartedState());
			Scenario scenario = scenarioMap.get(mapping.getScenarioName());
			mapping.setScenario(scenario);
		}
	}

	@Override
	public void reset() {
		mappingsRepository.clear();
		scenarioMap.clear();
	}

	@Override
	public void resetScenarios() {
		for (Scenario scenario : scenarioMap.values()) {
			scenario.reset();
		}
	}

	@Override
	public List<StubMapping> getAll() {
		return ImmutableList.copyOf(mappingsRepository.getAll());
	}

	@Override
	public Optional<StubMapping> get(final UUID id) {
		return tryFind(mappingsRepository.getAll(), new Predicate<StubMapping>() {
			@Override
			public boolean apply(StubMapping input) {
				return input.getUuid().equals(id);
			}
		});
	}

	private Predicate<StubMapping> mappingMatchingAndInCorrectScenarioState(final Request request) {
		return mappingMatchingAndInCorrectScenarioStateNew(request);
	}

	private Predicate<StubMapping> mappingMatchingAndInCorrectScenarioStateNew(final Request request) {
		return new Predicate<StubMapping>() {
			public boolean apply(StubMapping mapping) {
				return mapping.getRequest().match(request, customMatchers).isExactMatch() &&
						(mapping.isIndependentOfScenarioState() || mapping.requiresCurrentScenarioState());
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
