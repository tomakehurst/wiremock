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
import static com.github.tomakehurst.wiremock.http.RequestMethod.ANY;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class InMemoryStubMappingsTest {

  private InMemoryStubMappings inMemoryStubMappings;

  @Before
  public void setUp() throws Exception {
    inMemoryStubMappings = new InMemoryStubMappings();
  }

  @Test
  public void testEditMapping() throws Exception {

    StubMapping existingMapping = aMapping(1, "/priority1/1");
    inMemoryStubMappings.addMapping(existingMapping);

    StubMapping newMapping = aMapping(1, "/priority1/2");
    newMapping.setUuid(existingMapping.getUuid());

    inMemoryStubMappings.editMapping(newMapping);

    List<StubMapping> allMappings = inMemoryStubMappings.getAll();

    assertThat(allMappings, hasSize(1));
    assertThat(allMappings.get(0), is(newMapping));
    assertThat(newMapping.getInsertionIndex(), is(existingMapping.getInsertionIndex()));
  }

  @Test
  public void testRemoveMapping() throws Exception {

    List<StubMapping> allMappings = inMemoryStubMappings.getAll();
    assertThat(allMappings, hasSize(0));

    StubMapping existingMapping = aMapping(1, "priority1/1");
    inMemoryStubMappings.addMapping(existingMapping);
    existingMapping = aMapping(2, "priority2/2");
    StubMapping mappingToRemove = existingMapping;
    inMemoryStubMappings.addMapping(existingMapping);
    existingMapping = aMapping(3, "priority3/3");
    inMemoryStubMappings.addMapping(existingMapping);
    allMappings = inMemoryStubMappings.getAll();
    assertThat(allMappings, hasSize(3));

    inMemoryStubMappings.removeMapping(mappingToRemove);

    allMappings = inMemoryStubMappings.getAll();
    assertThat(allMappings, hasSize(2));
  }

  @Test
  public void testEditMappingNotPresent() throws Exception {

    StubMapping existingMapping = aMapping(1, "/priority1/1");
    inMemoryStubMappings.addMapping(existingMapping);

    StubMapping newMapping = aMapping(1, "/priority1/2");

    try {
      inMemoryStubMappings.editMapping(newMapping);
      fail("Expected Exception");
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), containsString(newMapping.getUuid().toString()));
    }
  }

  private StubMapping aMapping(Integer priority, String url) {
    RequestPattern requestPattern = newRequestPattern(ANY, urlEqualTo(url)).build();
    StubMapping mapping = new StubMapping(requestPattern, new ResponseDefinition());
    mapping.setPriority(priority);
    return mapping;
  }
}
