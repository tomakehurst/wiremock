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
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.hasExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import java.util.Iterator;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

public class SortedConcurrentMappingSetTest {

  private SortedConcurrentMappingSet mappingSet;

  @Before
  public void init() {
    mappingSet = new SortedConcurrentMappingSet();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void returnsMappingsInPriorityThenInsertionOrder() {
    mappingSet.add(aMapping(3, "/priority3/1"));
    mappingSet.add(aMapping(3, "/priority3/2"));
    mappingSet.add(aMapping(6, "/priority6/1"));
    mappingSet.add(aMapping(1, "/priority1/1"));
    mappingSet.add(aMapping(1, "/priority1/2"));
    mappingSet.add(aMapping(1, "/priority1/3"));

    assertThat(
        mappingSet,
        hasExactly(
            requestUrlIs("/priority1/3"),
            requestUrlIs("/priority1/2"),
            requestUrlIs("/priority1/1"),
            requestUrlIs("/priority3/2"),
            requestUrlIs("/priority3/1"),
            requestUrlIs("/priority6/1")));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void supportsNullPriority() {
    mappingSet.add(aMapping(null, "/1"));
    mappingSet.add(aMapping(null, "/2"));
    mappingSet.add(aMapping(null, "/3"));
    mappingSet.add(aMapping(null, "/4"));

    assertThat(
        mappingSet,
        hasExactly(requestUrlIs("/4"), requestUrlIs("/3"), requestUrlIs("/2"), requestUrlIs("/1")));
  }

  @Test
  public void clearsCorrectly() {
    mappingSet.add(aMapping(3, "/priority3/1"));
    mappingSet.add(aMapping(3, "/priority3/2"));
    mappingSet.add(aMapping(6, "/priority6/1"));
    mappingSet.add(aMapping(1, "/priority1/1"));

    mappingSet.clear();

    assertThat("Mapping set should be empty", mappingSet.iterator().hasNext(), is(false));
  }

  @Test
  public void testRemove() throws Exception {

    StubMapping stubMapping = aMapping(1, "/priority1/1");

    mappingSet.add(stubMapping);
    assertThat(mappingSet.iterator().hasNext(), is(true));

    mappingSet.remove(stubMapping);
    assertThat(mappingSet.iterator().hasNext(), is(false));
  }

  @Test
  public void testReplace() throws Exception {

    StubMapping existingMapping = aMapping(1, "/priority1/1");
    mappingSet.add(existingMapping);

    existingMapping.setNewScenarioState("New Scenario State");

    StubMapping newMapping = aMapping(2, "/priority2/1");
    boolean result = mappingSet.replace(existingMapping, newMapping);

    Iterator<StubMapping> it = mappingSet.iterator();

    assertThat(result, is(true));
    assertThat(it.hasNext(), is(true));
    assertThat(it.next(), is(newMapping));
    assertThat(it.hasNext(), is(false));
  }

  @Test
  public void testReplaceNotExists() throws Exception {

    StubMapping existingMapping = aMapping(1, "/priority1/1");
    mappingSet.add(existingMapping);

    StubMapping newMapping = aMapping(2, "/priority2/1");
    boolean result = mappingSet.replace(aMapping(2, "/priority2/2"), newMapping);

    Iterator<StubMapping> it = mappingSet.iterator();

    assertThat(result, is(false));
    assertThat(it.hasNext(), is(true));
    assertThat(it.next(), is(existingMapping));
    assertThat(it.hasNext(), is(false));
  }

  private StubMapping aMapping(Integer priority, String url) {
    RequestPattern requestPattern = newRequestPattern(ANY, urlEqualTo(url)).build();
    StubMapping mapping = new StubMapping(requestPattern, new ResponseDefinition());
    mapping.setPriority(priority);
    return mapping;
  }

  private Matcher<StubMapping> requestUrlIs(final String expectedUrl) {
    return new TypeSafeMatcher<StubMapping>() {

      @Override
      public void describeTo(Description desc) {}

      @Override
      public boolean matchesSafely(StubMapping actualMapping) {
        return actualMapping.getRequest().getUrl().equals(expectedUrl);
      }
    };
  }
}
