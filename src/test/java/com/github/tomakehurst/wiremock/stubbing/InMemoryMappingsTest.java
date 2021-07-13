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

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.http.RequestMethod.OPTIONS;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.github.tomakehurst.wiremock.http.RequestMethod.PUT;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static com.github.tomakehurst.wiremock.testsupport.MockRequestBuilder.aRequest;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class InMemoryMappingsTest {

  private InMemoryStubMappings mappings;
  private Mockery context;
  private Notifier notifier;

  @Before
  public void init() {
    mappings = new InMemoryStubMappings();
    context = new Mockery();

    notifier = context.mock(Notifier.class);
  }

  @After
  public void cleanUp() {
    LocalNotifier.set(null);
  }

  @Test
  public void correctlyAcceptsMappingAndReturnsCorrespondingResponse() {
    mappings.addMapping(
        new StubMapping(
            newRequestPattern(PUT, urlEqualTo("/some/resource")).build(),
            new ResponseDefinition(204, "")));

    Request request = aRequest(context).withMethod(PUT).withUrl("/some/resource").build();
    ResponseDefinition response = mappings.serveFor(request).getResponseDefinition();

    assertThat(response.getStatus(), is(204));
  }

  @Test
  public void returnsNotFoundWhenMethodIncorrect() {
    mappings.addMapping(
        new StubMapping(
            newRequestPattern(PUT, urlEqualTo("/some/resource")).build(),
            new ResponseDefinition(204, "")));

    Request request = aRequest(context).withMethod(POST).withUrl("/some/resource").build();
    ResponseDefinition response = mappings.serveFor(request).getResponseDefinition();

    assertThat(response.getStatus(), is(HTTP_NOT_FOUND));
  }

  @Test
  public void returnsNotFoundWhenUrlIncorrect() {
    mappings.addMapping(
        new StubMapping(
            newRequestPattern(PUT, urlEqualTo("/some/resource")).build(),
            new ResponseDefinition(204, "")));

    Request request = aRequest(context).withMethod(PUT).withUrl("/some/bad/resource").build();
    ResponseDefinition response = mappings.serveFor(request).getResponseDefinition();

    assertThat(response.getStatus(), is(HTTP_NOT_FOUND));
  }

  @Test
  public void returnsNotConfiguredResponseForUnmappedRequest() {
    Request request = aRequest(context).withMethod(OPTIONS).withUrl("/not/mapped").build();
    ResponseDefinition response = mappings.serveFor(request).getResponseDefinition();
    assertThat(response.getStatus(), is(HTTP_NOT_FOUND));
    assertThat(response.wasConfigured(), is(false));
  }

  @Test
  public void returnsMostRecentlyInsertedResponseIfTwoOrMoreMatch() {
    mappings.addMapping(
        new StubMapping(
            newRequestPattern(GET, urlEqualTo("/duplicated/resource")).build(),
            new ResponseDefinition(204, "Some content")));

    mappings.addMapping(
        new StubMapping(
            newRequestPattern(GET, urlEqualTo("/duplicated/resource")).build(),
            new ResponseDefinition(201, "Desired content")));

    ResponseDefinition response =
        mappings
            .serveFor(aRequest(context).withMethod(GET).withUrl("/duplicated/resource").build())
            .getResponseDefinition();

    assertThat(response.getStatus(), is(201));
    assertThat(response.getBody(), is("Desired content"));
  }

  @Test
  public void returnsMappingInScenarioOnlyWhenStateIsCorrect() {
    StubMapping firstGetMapping =
        new StubMapping(
            newRequestPattern(GET, urlEqualTo("/scenario/resource")).build(),
            new ResponseDefinition(204, "Initial content"));
    firstGetMapping.setScenarioName("TestScenario");
    firstGetMapping.setRequiredScenarioState(STARTED);
    mappings.addMapping(firstGetMapping);

    StubMapping putMapping =
        new StubMapping(
            newRequestPattern(PUT, urlEqualTo("/scenario/resource")).build(),
            new ResponseDefinition(204, ""));
    putMapping.setScenarioName("TestScenario");
    putMapping.setRequiredScenarioState(STARTED);
    putMapping.setNewScenarioState("Modified");
    mappings.addMapping(putMapping);

    StubMapping secondGetMapping =
        new StubMapping(
            newRequestPattern(GET, urlEqualTo("/scenario/resource")).build(),
            new ResponseDefinition(204, "Modified content"));
    secondGetMapping.setScenarioName("TestScenario");
    secondGetMapping.setRequiredScenarioState("Modified");
    mappings.addMapping(secondGetMapping);

    Request firstGet =
        aRequest(context, "firstGet").withMethod(GET).withUrl("/scenario/resource").build();
    Request put = aRequest(context, "put").withMethod(PUT).withUrl("/scenario/resource").build();
    Request secondGet =
        aRequest(context, "secondGet").withMethod(GET).withUrl("/scenario/resource").build();

    assertThat(
        mappings.serveFor(firstGet).getResponseDefinition().getBody(), is("Initial content"));
    mappings.serveFor(put);
    assertThat(
        mappings.serveFor(secondGet).getResponseDefinition().getBody(), is("Modified content"));
  }

  @Test
  public void returnsMappingInScenarioWithNoRequiredState() {
    StubMapping firstGetMapping =
        new StubMapping(
            newRequestPattern(GET, urlEqualTo("/scenario/resource")).build(),
            new ResponseDefinition(200, "Expected content"));
    firstGetMapping.setScenarioName("TestScenario");
    mappings.addMapping(firstGetMapping);

    Request request = aRequest(context).withMethod(GET).withUrl("/scenario/resource").build();

    assertThat(
        mappings.serveFor(request).getResponseDefinition().getBody(), is("Expected content"));
  }

  @Test
  public void supportsResetOfAllScenariosState() {
    StubMapping firstGetMapping =
        new StubMapping(
            newRequestPattern(GET, urlEqualTo("/scenario/resource")).build(),
            new ResponseDefinition(204, "Desired content"));
    firstGetMapping.setScenarioName("TestScenario");
    firstGetMapping.setRequiredScenarioState(STARTED);
    mappings.addMapping(firstGetMapping);

    StubMapping putMapping =
        new StubMapping(
            newRequestPattern(PUT, urlEqualTo("/scenario/resource")).build(),
            new ResponseDefinition(204, ""));
    putMapping.setScenarioName("TestScenario");
    putMapping.setRequiredScenarioState(STARTED);
    putMapping.setNewScenarioState("Modified");
    mappings.addMapping(putMapping);

    mappings.serveFor(
        aRequest(context, "put /scenario/resource")
            .withMethod(PUT)
            .withUrl("/scenario/resource")
            .build());
    ResponseDefinition response =
        mappings
            .serveFor(
                aRequest(context, "1st get /scenario/resource")
                    .withMethod(GET)
                    .withUrl("/scenario/resource")
                    .build())
            .getResponseDefinition();

    assertThat(response.wasConfigured(), is(false));

    mappings.resetScenarios();
    response =
        mappings
            .serveFor(
                aRequest(context, "2nd get /scenario/resource")
                    .withMethod(GET)
                    .withUrl("/scenario/resource")
                    .build())
            .getResponseDefinition();
    assertThat(response.getBody(), is("Desired content"));
  }

  @Test
  public void scenariosShouldBeResetWhenMappingsAreReset() {
    StubMapping firstMapping = aBasicMappingInScenario("Starting content");
    firstMapping.setRequiredScenarioState(Scenario.STARTED);
    firstMapping.setNewScenarioState("modified");
    mappings.addMapping(firstMapping);

    StubMapping secondMapping = aBasicMappingInScenario("Modified content");
    secondMapping.setRequiredScenarioState("modified");
    mappings.addMapping(secondMapping);

    Request request = aRequest(context).withMethod(POST).withUrl("/scenario/resource").build();
    mappings.serveFor(request);
    assertThat(
        mappings.serveFor(request).getResponseDefinition().getBody(), is("Modified content"));

    mappings.reset();

    StubMapping thirdMapping = aBasicMappingInScenario("Starting content");
    thirdMapping.setRequiredScenarioState(Scenario.STARTED);
    mappings.addMapping(thirdMapping);

    assertThat(
        mappings.serveFor(request).getResponseDefinition().getBody(), is("Starting content"));
  }

  private StubMapping aBasicMappingInScenario(String body) {
    StubMapping mapping =
        new StubMapping(
            newRequestPattern(POST, urlEqualTo("/scenario/resource")).build(),
            new ResponseDefinition(200, body));
    mapping.setScenarioName("TestScenario");
    return mapping;
  }
}
