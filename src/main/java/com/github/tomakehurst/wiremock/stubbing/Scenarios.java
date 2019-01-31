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

import com.github.tomakehurst.wiremock.common.ClientError;
import com.github.tomakehurst.wiremock.common.Errors;
import com.github.tomakehurst.wiremock.common.InvalidInputException;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.FluentIterable.from;

public class Scenarios {

    private final ConcurrentHashMap<String, Scenario> scenarioMap = new ConcurrentHashMap<>();

    public Scenario getByName(String name) {
        return scenarioMap.get(name);
    }

    public List<Scenario> getAll() {
        return ImmutableList.copyOf(scenarioMap.values());
    }

    public void onStubMappingAdded(StubMapping mapping) {
        if (mapping.isInScenario()) {
            String scenarioName = mapping.getScenarioName();
            Scenario scenario =
                firstNonNull(scenarioMap.get(scenarioName), Scenario.inStartedState(scenarioName))
                .withStubMapping(mapping);
            scenarioMap.put(scenarioName, scenario);
        }
    }

    public void onStubMappingUpdated(StubMapping oldMapping, StubMapping newMapping) {
        if (oldMapping.isInScenario() && !newMapping.getScenarioName().equals(oldMapping.getScenarioName())) {
            Scenario scenarioForOldMapping =
                scenarioMap.get(oldMapping.getScenarioName())
                    .withoutStubMapping(oldMapping);

            if (scenarioForOldMapping.getMappings().isEmpty()) {
                scenarioMap.remove(scenarioForOldMapping.getName());
            } else {
                scenarioMap.put(oldMapping.getScenarioName(), scenarioForOldMapping);
            }
        }

        if (newMapping.isInScenario()) {
            String scenarioName = newMapping.getScenarioName();
            Scenario scenario =
                firstNonNull(scenarioMap.get(scenarioName), Scenario.inStartedState(scenarioName))
                .withStubMapping(newMapping);
            scenarioMap.put(scenarioName, scenario);
        }
    }

    public void onStubMappingRemoved(StubMapping mapping) {
        if (mapping.isInScenario()) {
            final String scenarioName = mapping.getScenarioName();
            Scenario scenario =
                scenarioMap.get(scenarioName)
                .withoutStubMapping(mapping);

            if (scenario.getMappings().isEmpty()) {
                scenarioMap.remove(scenarioName);
            } else {
                scenarioMap.put(scenarioName, scenario);
            }
        }
    }

    public void onStubServed(StubMapping mapping) {
        if (mapping.isInScenario()) {
            final String scenarioName = mapping.getScenarioName();
            Scenario scenario = scenarioMap.get(scenarioName);
            if (mapping.modifiesScenarioState() &&
                (mapping.getRequiredScenarioState() == null || scenario.getState().equals(mapping.getRequiredScenarioState()))) {
                Scenario newScenario = scenario.setState(mapping.getNewScenarioState());
                scenarioMap.put(scenarioName, newScenario);
            }
        }
    }

    public void moveScenarioToState(Scenario scenario, String newState) {
        if (scenarioMap.contains(scenario)){
            if (scenario.getPossibleStates().contains(newState)) {
                final Scenario newScenario = scenario.setState(newState);
                scenarioMap.put(newScenario.getName(), newScenario);
            } else {
                final Errors error = Errors.single(70, "Could not find the given scenario state");
                throw new InvalidInputException(error);
            }
        } else {
            final Errors error = Errors.single(70, "Could not find the given scenario");
            throw new InvalidInputException(error);
        }
    }

    public void reset() {
        scenarioMap.putAll(Maps.transformValues(scenarioMap, new Function<Scenario, Scenario>() {
            @Override
            public Scenario apply(Scenario input) {
                return input.reset();
            }
        }));
    }

    public void clear() {
        scenarioMap.clear();
    }

    public boolean mappingMatchesScenarioState(StubMapping mapping) {
        String currentScenarioState = getByName(mapping.getScenarioName()).getState();
        return mapping.getRequiredScenarioState().equals(currentScenarioState);
    }

}
