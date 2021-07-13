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
package com.github.tomakehurst.wiremock.verification;

import static com.google.common.collect.FluentIterable.from;
import static java.lang.Math.min;

import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.MemoizingMatchResult;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.*;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import java.util.Comparator;
import java.util.List;

public class NearMissCalculator {

  public static final int NEAR_MISS_COUNT = 3;

  public static final Comparator<NearMiss> NEAR_MISS_ASCENDING_COMPARATOR =
      new Comparator<NearMiss>() {
        public int compare(NearMiss o1, NearMiss o2) {
          return o1.compareTo(o2);
        }
      };

  private final StubMappings stubMappings;
  private final RequestJournal requestJournal;
  private final Scenarios scenarios;

  public NearMissCalculator(
      StubMappings stubMappings, RequestJournal requestJournal, Scenarios scenarios) {
    this.stubMappings = stubMappings;
    this.requestJournal = requestJournal;
    this.scenarios = scenarios;
  }

  public List<NearMiss> findNearestTo(final LoggedRequest request) {
    List<StubMapping> allMappings = stubMappings.getAll();

    return sortAndTruncate(
        from(allMappings)
            .transform(
                new Function<StubMapping, NearMiss>() {
                  public NearMiss apply(StubMapping stubMapping) {
                    MatchResult matchResult =
                        new MemoizingMatchResult(stubMapping.getRequest().match(request));
                    String actualScenarioState = getScenarioStateOrNull(stubMapping);
                    return new NearMiss(request, stubMapping, matchResult, actualScenarioState);
                  }
                }),
        allMappings.size());
  }

  private String getScenarioStateOrNull(StubMapping stubMapping) {
    if (!stubMapping.isInScenario()) {
      return null;
    }

    Scenario scenario = scenarios.getByName(stubMapping.getScenarioName());
    return scenario != null ? scenario.getState() : null;
  }

  public List<NearMiss> findNearestTo(final RequestPattern requestPattern) {
    List<ServeEvent> serveEvents = requestJournal.getAllServeEvents();
    return sortAndTruncate(
        from(serveEvents)
            .transform(
                new Function<ServeEvent, NearMiss>() {
                  public NearMiss apply(ServeEvent serveEvent) {
                    MatchResult matchResult =
                        new MemoizingMatchResult(requestPattern.match(serveEvent.getRequest()));
                    return new NearMiss(serveEvent.getRequest(), requestPattern, matchResult);
                  }
                }),
        serveEvents.size());
  }

  private static List<NearMiss> sortAndTruncate(
      FluentIterable<NearMiss> nearMisses, int originalSize) {
    return nearMisses
        .toSortedList(NEAR_MISS_ASCENDING_COMPARATOR)
        .subList(0, min(NEAR_MISS_COUNT, originalSize));
  }
}
