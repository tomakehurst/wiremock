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

import com.github.tomakehurst.wiremock.common.Urls;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ScenarioProcessor {

  public void putRepeatedRequestsInScenarios(List<StubMapping> stubMappings) {
    ImmutableListMultimap<RequestPattern, StubMapping> stubsGroupedByRequest =
        Multimaps.index(
            stubMappings,
            new Function<StubMapping, RequestPattern>() {
              @Override
              public RequestPattern apply(StubMapping mapping) {
                return mapping.getRequest();
              }
            });

    Map<RequestPattern, Collection<StubMapping>> groupsWithMoreThanOneStub =
        Maps.filterEntries(
            stubsGroupedByRequest.asMap(),
            new Predicate<Map.Entry<RequestPattern, Collection<StubMapping>>>() {
              @Override
              public boolean apply(Map.Entry<RequestPattern, Collection<StubMapping>> input) {
                return input.getValue().size() > 1;
              }
            });

    int scenarioIndex = 0;
    for (Map.Entry<RequestPattern, Collection<StubMapping>> entry :
        groupsWithMoreThanOneStub.entrySet()) {
      scenarioIndex++;
      putStubsInScenario(scenarioIndex, ImmutableList.copyOf(entry.getValue()));
    }
  }

  private void putStubsInScenario(int scenarioIndex, List<StubMapping> stubMappings) {
    StubMapping firstScenario = stubMappings.get(0);
    String scenarioName =
        "scenario-"
            + Integer.toString(scenarioIndex)
            + "-"
            + Urls.urlToPathParts(URI.create(firstScenario.getRequest().getUrl()));

    int count = 1;
    for (StubMapping stub : stubMappings) {
      stub.setScenarioName(scenarioName);
      if (count == 1) {
        stub.setRequiredScenarioState(Scenario.STARTED);
      } else {
        stub.setRequiredScenarioState(scenarioName + "-" + count);
      }

      if (count < stubMappings.size()) {
        stub.setNewScenarioState(scenarioName + "-" + (count + 1));
      }

      count++;
    }
  }
}
