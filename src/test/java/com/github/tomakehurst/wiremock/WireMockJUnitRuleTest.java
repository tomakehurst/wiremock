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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;

@RunWith(Enclosed.class)
public class WireMockJUnitRuleTest {

    public static class BasicWireMockRule {
    
    	@Rule
    	public WireMockRule wireMockRule = new WireMockRule(8089);
	
    	@Test
    	public void canRegisterStubAndFetchOnCorrectPort() {
    		givenThat(get(urlEqualTo("/rule/test")).willReturn(aResponse().withBody("Rule test body")));
    		
    		WireMockTestClient testClient = new WireMockTestClient(8089);
    		
    		assertThat(testClient.get("/rule/test").content(), is("Rule test body"));
    	}
    	
    }
    
    public static class WireMockRuleFailThenPass {
        
        @Ignore("Generates a failure to illustrate a Rule bug whereby a failed test would cause BindExceptions on subsequent (otherwise passing) tests")
        @Test
        public void fail() {
            assertTrue(false);
        }
    
        @Test
        public void succeed() {
            assertTrue(true);
        }
        
    }
    
    public static class WireMockRuleAsClassRule {
        
        @ClassRule
        @Rule
        public static WireMockRule wireMockRule = new WireMockRule(8089);
        
        @Test
        public void testStubAndFetchOnce() {
            assertCanRegisterStubAndFetchOnCorrectPort();
        }
        
        @Test
        public void testStubAndFetchAgain() {
            assertCanRegisterStubAndFetchOnCorrectPort();
        }
    
        public void assertCanRegisterStubAndFetchOnCorrectPort() {
            givenThat(get(urlEqualTo("/rule/test")).willReturn(aResponse().withBody("Rule test body")));
            
            WireMockTestClient testClient = new WireMockTestClient(8089);
            
            assertThat(testClient.get("/rule/test").content(), is("Rule test body"));
        }

    }
}
