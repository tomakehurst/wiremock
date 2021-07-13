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
import static com.github.tomakehurst.wiremock.common.Metadata.metadata;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.stubMappingWithUrl;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.github.tomakehurst.wiremock.common.Metadata;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class StubMetadataAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void createAndRetrieveStubMetadata() {
    UUID id = UUID.randomUUID();
    stubFor(
        get("/with-metadata")
            .withId(id)
            .withMetadata(
                metadata()
                    .attr("one", 1)
                    .attr("two", "2")
                    .attr("three", true)
                    .attr("four", metadata().attr("five", "55555"))
                    .list("six", 1, 2, 3)));

    StubMapping retrievedStub = getSingleStubMapping(id);
    Metadata metadata = retrievedStub.getMetadata();

    assertThat(metadata.getInt("one"), is(1));
    assertThat(metadata.getString("two"), is("2"));
    assertThat(metadata.getBoolean("three"), is(true));

    Metadata four = metadata.getMetadata("four");

    assertThat(four.getString("five"), is("55555"));

    List<?> six = metadata.getList("six");
    assertThat((Integer) six.get(0), is(1));
  }

  @Test
  public void canFindStubsByMetadata() {
    UUID id = UUID.randomUUID();
    stubFor(
        get("/with-metadata")
            .withId(id)
            .withMetadata(
                metadata().attr("four", metadata().attr("five", "55555")).list("six", 1, 2, 3)));
    stubFor(get("/without-metadata"));

    List<StubMapping> stubs =
        findStubsByMetadata(matchingJsonPath("$..four.five", containing("55555")));
    StubMapping retrievedStub = stubs.get(0);
    assertThat(retrievedStub.getId(), is(id));
  }

  @Test
  public void canRemoveStubsByMetadata() {
    UUID id = UUID.randomUUID();
    stubFor(
        get("/with-metadata")
            .withId(id)
            .withMetadata(
                metadata().attr("four", metadata().attr("five", "55555")).list("six", 1, 2, 3)));
    stubFor(get("/without-metadata"));

    removeStubsByMetadata(matchingJsonPath("$..four.five", containing("55555")));

    assertThat(
        listAllStubMappings().getMappings(), not(hasItem(stubMappingWithUrl("/with-metadata"))));
  }
}
