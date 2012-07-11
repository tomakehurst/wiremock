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
package com.github.tomakehurst.wiremock.mapping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.http.RequestMethod.OPTIONS;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.github.tomakehurst.wiremock.http.RequestMethod.PUT;
import static com.github.tomakehurst.wiremock.mapping.Scenario.STARTED;
import static com.github.tomakehurst.wiremock.testsupport.MockRequestBuilder.aRequest;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.common.Notifier;

@RunWith(JMock.class)
public class InMemoryMappingsTest {

	private InMemoryMappings mappings;
	private Mockery context;
	private Notifier notifier;
	
	@Before
	public void init() {
		mappings = new InMemoryMappings();
		context = new Mockery();
		
		notifier = context.mock(Notifier.class);
	}
	
	@After
    public void cleanUp() {
        LocalNotifier.set(null);
    }
	
	@Test
	public void correctlyAcceptsMappingAndReturnsCorrespondingResponse() {
		mappings.addMapping(new RequestResponseMapping(
				new RequestPattern(PUT, "/some/resource"),
				new ResponseDefinition(204, "")));
		
		Request request = aRequest(context).withMethod(PUT).withUrl("/some/resource").build();
		ResponseDefinition response = mappings.serveFor(request);
		
		assertThat(response.getStatus(), is(204));
	}
	
	@Test
	public void returnsNotFoundWhenMethodIncorrect() {
		mappings.addMapping(new RequestResponseMapping(
				new RequestPattern(PUT, "/some/resource"),
				new ResponseDefinition(204, "")));
		
		Request request = aRequest(context).withMethod(POST).withUrl("/some/resource").build();
		ResponseDefinition response = mappings.serveFor(request);
		
		assertThat(response.getStatus(), is(HTTP_NOT_FOUND));
	}
	
	@Test
	public void returnsNotFoundWhenUrlIncorrect() {
		mappings.addMapping(new RequestResponseMapping(
				new RequestPattern(PUT, "/some/resource"),
				new ResponseDefinition(204, "")));
		
		Request request = aRequest(context).withMethod(PUT).withUrl("/some/bad/resource").build();
		ResponseDefinition response = mappings.serveFor(request);
		
		assertThat(response.getStatus(), is(HTTP_NOT_FOUND));
	}
	
	@Test
	public void returnsNotConfiguredResponseForUnmappedRequest() {
		Request request = aRequest(context).withMethod(OPTIONS).withUrl("/not/mapped").build();
		ResponseDefinition response = mappings.serveFor(request);
		assertThat(response.getStatus(), is(HTTP_NOT_FOUND));
		assertThat(response.wasConfigured(), is(false));
	}
	
	@Test
	public void returnsMostRecentlyInsertedResponseIfTwoOrMoreMatch() {
		mappings.addMapping(new RequestResponseMapping(
				new RequestPattern(GET, "/duplicated/resource"),
				new ResponseDefinition(204, "Some content")));
		
		mappings.addMapping(new RequestResponseMapping(
				new RequestPattern(GET, "/duplicated/resource"),
				new ResponseDefinition(201, "Desired content")));
		
		ResponseDefinition response = mappings.serveFor(aRequest(context).withMethod(GET).withUrl("/duplicated/resource").build());
		
		assertThat(response.getStatus(), is(201));
		assertThat(response.getBody(), is("Desired content"));
	}
	
	@Test
	public void returnsMappingInScenarioOnlyWhenStateIsCorrect() {
		RequestResponseMapping firstGetMapping = new RequestResponseMapping(
				new RequestPattern(GET, "/scenario/resource"),
				new ResponseDefinition(204, "Initial content"));
		firstGetMapping.setScenarioName("TestScenario");
		firstGetMapping.setRequiredScenarioState(STARTED);
		mappings.addMapping(firstGetMapping);
		
		RequestResponseMapping putMapping = new RequestResponseMapping(
				new RequestPattern(PUT, "/scenario/resource"),
				new ResponseDefinition(204, ""));
		putMapping.setScenarioName("TestScenario");
		putMapping.setRequiredScenarioState(STARTED);
		putMapping.setNewScenarioState("Modified");
		mappings.addMapping(putMapping);
		
		RequestResponseMapping secondGetMapping = new RequestResponseMapping(
				new RequestPattern(GET, "/scenario/resource"),
				new ResponseDefinition(204, "Modified content"));
		secondGetMapping.setScenarioName("TestScenario");
		secondGetMapping.setRequiredScenarioState("Modified");
		mappings.addMapping(secondGetMapping);
		
		
		Request firstGet = aRequest(context, "firstGet").withMethod(GET).withUrl("/scenario/resource").build();
		Request put = aRequest(context, "put").withMethod(PUT).withUrl("/scenario/resource").build();
		Request secondGet = aRequest(context, "secondGet").withMethod(GET).withUrl("/scenario/resource").build();
		
		assertThat(mappings.serveFor(firstGet).getBody(), is("Initial content"));
		mappings.serveFor(put);
		assertThat(mappings.serveFor(secondGet).getBody(), is("Modified content"));
	}
	
	@Test
	public void returnsMappingInScenarioWithNoRequiredState() {
		RequestResponseMapping firstGetMapping = new RequestResponseMapping(
				new RequestPattern(GET, "/scenario/resource"),
				new ResponseDefinition(200, "Expected content"));
		firstGetMapping.setScenarioName("TestScenario");
		mappings.addMapping(firstGetMapping);
		
		Request request = aRequest(context).withMethod(GET).withUrl("/scenario/resource").build();
		
		assertThat(mappings.serveFor(request).getBody(), is("Expected content"));
	}
	
	@Test
	public void supportsResetOfAllScenariosState() {
		RequestResponseMapping firstGetMapping = new RequestResponseMapping(
				new RequestPattern(GET, "/scenario/resource"),
				new ResponseDefinition(204, "Desired content"));
		firstGetMapping.setScenarioName("TestScenario");
		firstGetMapping.setRequiredScenarioState(STARTED);
		mappings.addMapping(firstGetMapping);
		
		RequestResponseMapping putMapping = new RequestResponseMapping(
				new RequestPattern(PUT, "/scenario/resource"),
				new ResponseDefinition(204, ""));
		putMapping.setScenarioName("TestScenario");
		putMapping.setRequiredScenarioState(STARTED);
		putMapping.setNewScenarioState("Modified");
		mappings.addMapping(putMapping);
		
		mappings.serveFor(
				aRequest(context, "put /scenario/resource")
				.withMethod(PUT).withUrl("/scenario/resource").build());
		ResponseDefinition response =
			mappings.serveFor(
					aRequest(context, "1st get /scenario/resource")
					.withMethod(GET).withUrl("/scenario/resource").build());
		
		assertThat(response.wasConfigured(), is(false));
		
		mappings.resetScenarios();
		response =
			mappings.serveFor(
					aRequest(context, "2nd get /scenario/resource")
					.withMethod(GET).withUrl("/scenario/resource").build());
		assertThat(response.getBody(), is("Desired content"));
	}
	
	@Test
	public void notifiesWhenNoMappingFound() {
	    context.checking(new Expectations() {{
            one(notifier).info("No mapping found matching URL /match/not/found");
        }});
	    
	    LocalNotifier.set(notifier);
        
        mappings.serveFor(aRequest(context).withMethod(GET).withUrl("/match/not/found").build());
	}
    
    @Test
    public void appliesGlobalHeadersForMatchedMappingsWhereNoneExist() {
        mappings.addMapping(new RequestResponseMapping(
                new RequestPattern(PUT, "/some/resource"),
                aResponse().withHeader("Cache-Control", "max-age=86400").build(), true));
        
        mappings.addMapping(new RequestResponseMapping(
                new RequestPattern(PUT, "/some/resource"),
                aResponse().withStatus(201).build()));
        
        Request request = aRequest(context).withMethod(PUT).withUrl("/some/resource").build();
        ResponseDefinition response = mappings.serveFor(request);
        
        assertThat(response.getStatus(), is(201));
        assertThat(response.getHeaders(), hasEntry("Cache-Control", "max-age=86400"));
    }
    
    @Test
    public void doesNotOverwriteExistingHeadersWhenMatchingGlobalSettings() {
        mappings.addMapping(new RequestResponseMapping(
                new RequestPattern(PUT, "/some/resource"),
                aResponse().withHeader("Cache-Control", "max-age=86400").build(), true));
        
        mappings.addMapping(new RequestResponseMapping(
                new RequestPattern(PUT, "/some/resource"),
                aResponse().withStatus(201).withHeader("Cache-Control", "max-age=100").build()));
        
        Request request = aRequest(context).withMethod(PUT).withUrl("/some/resource").build();
        ResponseDefinition response = mappings.serveFor(request);
        
        assertThat(response.getStatus(), is(201));
        assertThat(response.getHeaders(), hasEntry("Cache-Control", "max-age=100"));
    }
    
    @Test
    public void doesNotApplyGlobalSettingsForNonMatchedMappings() {
        mappings.addMapping(new RequestResponseMapping(
                new RequestPattern(PUT, "/some/resource"),
                aResponse().withHeader("Cache-Control", "max-age=86400").build(), true));
        
        mappings.addMapping(new RequestResponseMapping(
                new RequestPattern(PUT, "/some/other/resource"),
                aResponse().withStatus(201).build()));
        
        Request request = aRequest(context).withMethod(PUT).withUrl("/some/other/resource").build();
        ResponseDefinition response = mappings.serveFor(request);
        
        assertThat(response.getStatus(), is(201));
        assertThat(response.getHeaders(), is(nullValue()));
    }
}
