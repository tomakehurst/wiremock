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
package com.github.tomakehurst.wiremock;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.withName;
import static com.google.common.collect.Iterables.find;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ScenarioAcceptanceTest extends AcceptanceTestBase {

	@Test
	public void createMappingsInScenarioAndChangeResponseWithStateChange() {
		givenThat(get(urlEqualTo("/some/resource"))
				.willReturn(aResponse().withBody("Initial"))
				.inScenario("SomeResourceUpdate")
				.whenScenarioStateIs(STARTED));
		
		givenThat(put(urlEqualTo("/some/resource"))
				.willReturn(aResponse().withStatus(HTTP_OK))
				.inScenario("SomeResourceUpdate")
				.willSetStateTo("BodyModified")
				.whenScenarioStateIs(STARTED));
		
		givenThat(get(urlEqualTo("/some/resource"))
				.willReturn(aResponse().withBody("Modified"))
				.inScenario("SomeResourceUpdate")
				.whenScenarioStateIs("BodyModified"));
		
		assertThat(testClient.get("/some/resource").content(), is("Initial"));
		testClient.put("/some/resource");
		assertThat(testClient.get("/some/resource").content(), is("Modified"));
	}
	
	@Test
	public void mappingInScenarioIndependentOfCurrentState() {
		givenThat(get(urlEqualTo("/state/independent/resource"))
				.willReturn(aResponse().withBody("Some content"))
				.inScenario("StateIndependent"));
		
		givenThat(put(urlEqualTo("/state/modifying/resource"))
				.willReturn(aResponse().withStatus(HTTP_OK))
				.inScenario("StateIndependent")
				.willSetStateTo("BodyModified"));
		
		WireMockResponse response = testClient.get("/state/independent/resource");
		assertThat(response.statusCode(), is(HTTP_OK));
		assertThat(response.content(), is("Some content"));
		
		testClient.put("/state/modifying/resource");
		
		response = testClient.get("/state/independent/resource");
		assertThat(response.statusCode(), is(HTTP_OK));
		assertThat(response.content(), is("Some content"));
	}
	
	@Test
	public void resetAllScenariosState() {
		givenThat(get(urlEqualTo("/stateful/resource"))
				.willReturn(aResponse().withBody("Expected content"))
				.inScenario("ResetScenario")
				.whenScenarioStateIs(STARTED));
		
		givenThat(put(urlEqualTo("/stateful/resource"))
				.willReturn(aResponse().withStatus(HTTP_OK))
				.inScenario("ResetScenario")
				.willSetStateTo("Changed"));
		
		testClient.put("/stateful/resource");
		WireMock.resetAllScenarios();
		
		assertThat(testClient.get("/stateful/resource").content(), is("Expected content"));
	}

	@Test
	public void resetScenarioByNameState() {
		givenThat(get(urlEqualTo("/stateful/resource"))
				.willReturn(aResponse().withBody("Expected content"))
				.inScenario("ResetScenario")
				.whenScenarioStateIs(STARTED));

		givenThat(put(urlEqualTo("/stateful/resource"))
				.willReturn(aResponse().withStatus(HTTP_OK))
				.inScenario("ResetScenario")
				.willSetStateTo("Changed"));

		testClient.put("/stateful/resource");
		WireMock.resetScenario("ResetScenario");

		assertThat(testClient.get("/stateful/resource").content(), is("Expected content"));
	}

    @Test(expected = IllegalArgumentException.class)
    public void settingScenarioNameToNullCausesException() {
        get(urlEqualTo("/some/resource"))
                .willReturn(aResponse())
                .inScenario(null);
    }

    @Test
	public void canGetAllScenarios() {
	    stubFor(get("/scenarios/1")
            .inScenario("scenario_one")
            .whenScenarioStateIs(STARTED)
            .willSetStateTo("state_2")
            .willReturn(ok("1:1")));

        stubFor(get("/scenarios/2")
            .inScenario("scenario_two")
            .whenScenarioStateIs(STARTED)
            .willReturn(ok("2:1")));

        testClient.get("/scenarios/1");

        List<Scenario> scenarios = getAllScenarios();

        Scenario scenario1 = find(scenarios, withName("scenario_one"));
        assertThat(scenario1.getId(), notNullValue(UUID.class));
        assertThat(scenario1.getPossibleStates(), hasItems(STARTED, "state_2"));
        assertThat(scenario1.getState(), is("state_2"));

        Scenario scenario2 = find(scenarios, withName("scenario_two"));
        assertThat(scenario2.getState(), is("Started"));
    }

    @Test
    public void scenarioIsRemovedWhenLastMappingReferringToItIsRemoved() {
	    final String NAME = "remove_this_scenario";

        StubMapping stub1 = stubFor(get("/scenarios/22")
            .inScenario(NAME)
            .whenScenarioStateIs(STARTED)
            .willSetStateTo("state_2")
            .willReturn(ok("1")));

        StubMapping stub2 = stubFor(get("/scenarios/22")
            .inScenario(NAME)
            .whenScenarioStateIs("state_2")
            .willSetStateTo("state_2")
            .willReturn(ok("2")));

        StubMapping stub3 = stubFor(get("/scenarios/22")
            .inScenario(NAME)
            .whenScenarioStateIs("state_2")
            .willSetStateTo("state_3")
            .willReturn(ok("3")));

        assertThat(getAllScenarios().size(), is(1));

        removeStub(stub1);
        removeStub(stub2);
        removeStub(stub3);

        assertThat(getAllScenarios().size(), is(0));
    }

    @Test
    public void scenarioIsRemovedWhenLastMappingReferringToHasItsScenarioNameChanged() {
	    final UUID ID1 = UUID.randomUUID();
	    final UUID ID2 = UUID.randomUUID();
        final String OLD_NAME = "old_scenario";
        final String NEW_NAME = "new_scenario";

        stubFor(get("/scenarios/33")
            .withId(ID1)
            .inScenario(OLD_NAME)
            .whenScenarioStateIs(STARTED)
            .willSetStateTo("state_2")
            .willReturn(ok("1")));

        stubFor(get("/scenarios/33")
            .withId(ID2)
            .inScenario(OLD_NAME)
            .whenScenarioStateIs("state_2")
            .willSetStateTo("state_2")
            .willReturn(ok("2")));

        assertThat(getAllScenarios().size(), is(1));
        assertThat(getAllScenarios().get(0).getName(), is(OLD_NAME));

        editStub(get("/scenarios/33")
            .withId(ID1)
            .inScenario(NEW_NAME)
            .whenScenarioStateIs(STARTED)
            .willSetStateTo("state_2")
            .willReturn(ok("1")));
        editStub(get("/scenarios/33")
            .withId(ID2)
            .inScenario(NEW_NAME)
            .whenScenarioStateIs("state_2")
            .willSetStateTo("state_2")
            .willReturn(ok("2")));

        assertThat(getAllScenarios().size(), is(1));
        assertThat(getAllScenarios().get(0).getName(), is(NEW_NAME));
    }

    @Test
    public void returnsEmptyMapOnGetAllScenariosWhenThereAreNone() {
        assertThat(getAllScenarios().size(), is(0));
    }

    @Test
    public void scenarioBuilderMethodsDoNotNeedToBeContiguous() {
        // This test has no assertions, but is here to ensure that the following compiles - i.e. that
        // whenScenarioStatesIs and willSetStateTo don't have to immediately follow inScenario() calls, but can have
        // other builder calls in between them.
        //
        // It should *not* be possible to call either before inScenario is called, however. We can't add a test for that
        // of course, as it doesn't compile!
        get(urlEqualTo("/"))
                .inScenario("Scenario")
                .willReturn(aResponse())
                .whenScenarioStateIs("Prior State")
                .atPriority(1)
                .willSetStateTo("Next State");
    }


}
