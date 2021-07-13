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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.base.Predicate;
import java.util.UUID;
import org.junit.Test;

public class EditStubMappingAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void canEditAnExistingStubMapping() {
    UUID id = UUID.randomUUID();

    wireMockServer.stubFor(
        get(urlEqualTo("/edit-this")).withId(id).willReturn(aResponse().withBody("Original")));

    assertThat(testClient.get("/edit-this").content(), is("Original"));

    wireMockServer.editStub(
        get(urlEqualTo("/edit-this")).withId(id).willReturn(aResponse().withBody("Modified")));

    assertThat(testClient.get("/edit-this").content(), is("Modified"));

    int editThisStubCount =
        from(wireMockServer.listAllStubMappings().getMappings())
            .filter(withUrl("/edit-this"))
            .size();

    assertThat(editThisStubCount, is(1));
  }

  private Predicate<StubMapping> withUrl(final String url) {
    return new Predicate<StubMapping>() {
      public boolean apply(StubMapping mapping) {
        return (mapping.getRequest().getUrl() != null && mapping.getRequest().getUrl().equals(url));
      }
    };
  }
}
