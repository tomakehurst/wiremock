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
import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.base.Predicate;
import java.util.UUID;
import org.junit.Test;

public class RemoveStubMappingAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void removeStubThatExistsUsingUUID() {

    UUID id1 = UUID.randomUUID();

    stubFor(get(urlEqualTo("/stub-1")).withId(id1).willReturn(aResponse().withBody("Stub-1-Body")));

    assertThat(testClient.get("/stub-1").content(), is("Stub-1-Body"));

    UUID id2 = UUID.randomUUID();
    stubFor(get(urlEqualTo("/stub-2")).withId(id2).willReturn(aResponse().withBody("Stub-2-Body")));

    assertThat(testClient.get("/stub-2").content(), is("Stub-2-Body"));

    assertThat(getMatchingStubCount("/stub-1", "/stub-2"), is(2));

    removeStub(
        get(urlEqualTo("/stub-2")).withId(id2).willReturn(aResponse().withBody("Stub-2-Body")));

    assertThat(getMatchingStubCount("/stub-1", "/stub-2"), is(1));

    removeStub(
        get(urlEqualTo("/stub-1")).withId(id1).willReturn(aResponse().withBody("Stub-1-Body")));

    assertThat(getMatchingStubCount("/stub-1", "/stub-2"), is(0));
  }

  @Test
  public void removeStubThatExistsUsingRequestMatchUUIDNotMatch() {

    UUID id1 = UUID.randomUUID();

    stubFor(
        get(urlEqualTo("/stub-11")).withId(id1).willReturn(aResponse().withBody("Stub-11-Body")));

    assertThat(testClient.get("/stub-11").content(), is("Stub-11-Body"));

    UUID id2 = UUID.randomUUID();
    stubFor(
        get(urlEqualTo("/stub-22")).withId(id2).willReturn(aResponse().withBody("Stub-22-Body")));

    assertThat(testClient.get("/stub-22").content(), is("Stub-22-Body"));

    assertThat(getMatchingStubCount("/stub-11", "/stub-22"), is(2));

    UUID id3 = UUID.randomUUID();
    removeStub(
        get(urlEqualTo("/stub-22")).withId(id3).willReturn(aResponse().withBody("Stub-22-Body")));

    assertThat(getMatchingStubCount("/stub-11", "/stub-22"), is(1));

    UUID id4 = UUID.randomUUID();
    removeStub(
        get(urlEqualTo("/stub-11")).withId(id4).willReturn(aResponse().withBody("Stub-11-Body")));

    assertThat(getMatchingStubCount("/stub-11", "/stub-22"), is(0));
  }

  @Test
  public void removeStubThatExistsWithRequestMatchNoUUIDPresent() {

    UUID id1 = UUID.randomUUID();

    stubFor(
        get(urlEqualTo("/stub-111")).withId(id1).willReturn(aResponse().withBody("Stub-111-Body")));

    assertThat(testClient.get("/stub-111").content(), is("Stub-111-Body"));

    UUID id2 = UUID.randomUUID();
    stubFor(
        get(urlEqualTo("/stub-222")).withId(id2).willReturn(aResponse().withBody("Stub-222-Body")));

    assertThat(testClient.get("/stub-222").content(), is("Stub-222-Body"));

    assertThat(getMatchingStubCount("/stub-111", "/stub-222"), is(2));

    removeStub(get(urlEqualTo("/stub-222")).willReturn(aResponse().withBody("Stub-222-Body")));

    assertThat(getMatchingStubCount("/stub-111", "/stub-222"), is(1));

    removeStub(get(urlEqualTo("/stub-111")).willReturn(aResponse().withBody("Stub-111-Body")));

    assertThat(getMatchingStubCount("/stub-111", "/stub-222"), is(0));
  }

  @Test
  public void removeStubThatDoesNotExists() {

    UUID id1 = UUID.randomUUID();

    stubFor(get(urlEqualTo("/stb-1")).withId(id1).willReturn(aResponse().withBody("Stb-1-Body")));

    assertThat(testClient.get("/stb-1").content(), is("Stb-1-Body"));

    UUID id2 = UUID.randomUUID();
    stubFor(get(urlEqualTo("/stb-2")).withId(id2).willReturn(aResponse().withBody("Stb-2-Body")));

    assertThat(testClient.get("/stb-2").content(), is("Stb-2-Body"));

    assertThat(getMatchingStubCount("/stb-1", "/stb-2"), is(2));

    UUID id3 = UUID.randomUUID();
    removeStub(
        get(urlEqualTo("/stb-3")).withId(id3).willReturn(aResponse().withBody("Stb-3-Body")));

    assertThat(getMatchingStubCount("/stb-1", "/stb-2"), is(2));
  }

  private Predicate<StubMapping> withAnyOf(final String... urls) {
    return new Predicate<StubMapping>() {
      public boolean apply(StubMapping mapping) {
        return mapping.getRequest().getUrl() != null
            && asList(urls).contains(mapping.getRequest().getUrl());
      }
    };
  }

  private synchronized int getMatchingStubCount(String url1, String url2) {
    return from(listAllStubMappings().getMappings()).filter(withAnyOf(url1, url2)).size();
  }
}
