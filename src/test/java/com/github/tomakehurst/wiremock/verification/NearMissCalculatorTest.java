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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.http.RequestMethod.*;
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static com.github.tomakehurst.wiremock.verification.NearMissCalculator.NEAR_MISS_COUNT;
import static com.google.common.collect.FluentIterable.from;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.Scenarios;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.stubbing.StubMappings;
import com.google.common.base.Function;
import java.util.List;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class NearMissCalculatorTest {

  private Mockery context;

  NearMissCalculator nearMissCalculator;

  StubMappings stubMappings;
  RequestJournal requestJournal;
  Scenarios scenarios;

  @Before
  public void init() {
    context = new Mockery();

    stubMappings = context.mock(StubMappings.class);
    requestJournal = context.mock(RequestJournal.class);
    scenarios = new Scenarios();
    nearMissCalculator = new NearMissCalculator(stubMappings, requestJournal, scenarios);
  }

  @Test
  public void returnsNearest3MissesForSingleRequest() {
    givenStubMappings(
        get(urlEqualTo("/righ")).willReturn(aResponse()),
        get(urlEqualTo("/totally-wrong1")).willReturn(aResponse()),
        get(urlEqualTo("/totally-wrong222")).willReturn(aResponse()),
        get(urlEqualTo("/almost-right")).willReturn(aResponse()),
        get(urlEqualTo("/rig")).willReturn(aResponse()),
        get(urlEqualTo("/totally-wrong33333")).willReturn(aResponse()));

    List<NearMiss> nearest =
        nearMissCalculator.findNearestTo(mockRequest().url("/right").asLoggedRequest());

    assertThat(nearest.size(), is(NEAR_MISS_COUNT));
    assertThat(nearest.get(0).getStubMapping().getRequest().getUrl(), is("/righ"));
    assertThat(nearest.get(1).getStubMapping().getRequest().getUrl(), is("/rig"));
    assertThat(nearest.get(2).getStubMapping().getRequest().getUrl(), is("/almost-right"));
  }

  @Test
  public void returns0NearMissesForSingleRequestWhenNoStubsPresent() {
    givenStubMappings();

    List<NearMiss> nearest =
        nearMissCalculator.findNearestTo(mockRequest().url("/right").asLoggedRequest());

    assertThat(nearest.size(), is(0));
  }

  @Test
  public void returns3NearestMissesForTheGivenRequestPattern() {
    givenRequests(
        mockRequest().method(DELETE).url("/rig"),
        mockRequest().method(DELETE).url("/righ"),
        mockRequest().method(DELETE).url("/almost-right"),
        mockRequest().method(POST).url("/almost-right"));

    List<NearMiss> nearest =
        nearMissCalculator.findNearestTo(newRequestPattern(DELETE, urlEqualTo("/right")).build());

    assertThat(nearest.size(), is(NEAR_MISS_COUNT));
    assertThat(nearest.get(0).getRequest().getUrl(), is("/righ"));
    assertThat(nearest.get(1).getRequest().getUrl(), is("/rig"));
    assertThat(nearest.get(2).getRequest().getUrl(), is("/almost-right"));
    assertThat(nearest.get(2).getRequest().getMethod(), is(DELETE));
  }

  @Test
  public void returns1NearestMissForTheGivenRequestPatternWhenOnlyOneRequestLogged() {
    givenRequests(mockRequest().method(DELETE).url("/righ"));

    List<NearMiss> nearest =
        nearMissCalculator.findNearestTo(newRequestPattern(DELETE, urlEqualTo("/right")).build());

    assertThat(nearest.size(), is(1));
    assertThat(nearest.get(0).getRequest().getUrl(), is("/righ"));
  }

  @Test
  public void returns0NearMissesForSingleRequestPatternWhenNoRequestsLogged() {
    givenRequests();

    List<NearMiss> nearest =
        nearMissCalculator.findNearestTo(newRequestPattern(DELETE, urlEqualTo("/right")).build());

    assertThat(nearest.size(), is(0));
  }

  @Test
  public void
      stubMappingsWithIdenticalMethodAndUrlWillRankHigherDespiteOtherParametersBeingAbsent() {
    givenStubMappings(
        post("/the-correct-path")
            .withName("Correct")
            .withHeader("Accept", equalTo("text/plain"))
            .withHeader("X-My-Header", matching("[0-9]*"))
            .withQueryParam("search", containing("somethings"))
            .withRequestBody(equalToJson("[1, 2, 3]"))
            .withRequestBody(matchingJsonPath("$..*"))
            .willReturn(ok()),
        post("/another-path").withName("Another 1").willReturn(ok()),
        get("/yet-another-path").withName("Yet another").willReturn(ok()));

    List<NearMiss> nearestForCorrectMethodAndUrl =
        nearMissCalculator.findNearestTo(
            mockRequest().method(POST).url("/the-correct-path").asLoggedRequest());
    assertThat(nearestForCorrectMethodAndUrl.get(0).getStubMapping().getName(), is("Correct"));

    List<NearMiss> nearestForIncorrectMethodAndCorrectUrl =
        nearMissCalculator.findNearestTo(
            mockRequest().method(POST).url("/the-incorrect-path").asLoggedRequest());
    assertThat(
        nearestForIncorrectMethodAndCorrectUrl.get(0).getStubMapping().getName(), is("Correct"));

    List<NearMiss> nearestForIncorrectMethodAndUrl =
        nearMissCalculator.findNearestTo(
            mockRequest().method(PUT).url("/the-incorrect-path").asLoggedRequest());
    assertThat(nearestForIncorrectMethodAndUrl.get(0).getStubMapping().getName(), is("Correct"));
  }

  private void givenStubMappings(final MappingBuilder... mappingBuilders) {
    final List<StubMapping> mappings =
        from(mappingBuilders)
            .transform(
                new Function<MappingBuilder, StubMapping>() {
                  @Override
                  public StubMapping apply(MappingBuilder input) {
                    return input.build();
                  }
                })
            .toList();
    context.checking(
        new Expectations() {
          {
            allowing(stubMappings).getAll();
            will(returnValue(mappings));
          }
        });
  }

  private void givenRequests(final Request... requests) {
    final List<ServeEvent> serveEvents =
        from(requests)
            .transform(
                new Function<Request, ServeEvent>() {
                  @Override
                  public ServeEvent apply(Request request) {
                    return ServeEvent.of(
                        LoggedRequest.createFrom(request), new ResponseDefinition());
                  }
                })
            .toList();

    context.checking(
        new Expectations() {
          {
            allowing(requestJournal).getAllServeEvents();
            will(returnValue(serveEvents));
          }
        });
  }
}
