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
import static com.github.tomakehurst.wiremock.stubbing.StubImport.stubImport;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.stubMappingWithUrl;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.util.List;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;

public class StubImportAcceptanceTest extends AcceptanceTestBase {

  private static Admin admin;

  @BeforeClass
  public static void setup() {
    admin = wireMockServer;
  }

  @Test
  public void importsAllStubsWhenNoneAreAlreadyPresent() {
    admin.importStubs(
        stubImport()
            .stub(get("/one").willReturn(ok()))
            .stub(post("/two").willReturn(ok()))
            .stub(put("/three").willReturn(ok()))
            .build());

    List<StubMapping> stubs = admin.listAllStubMappings().getMappings();

    assertThat(stubs, hasItem(stubMappingWithUrl("/one")));
    assertThat(stubs, hasItem(stubMappingWithUrl("/two")));
    assertThat(stubs, hasItem(stubMappingWithUrl("/three")));
  }

  @Test
  public void overwritesExistingStubsByDefault() {
    UUID id1 = UUID.randomUUID();
    wm.stubFor(get("/one").withId(id1).willReturn(ok("Original")));

    admin.importStubs(
        stubImport()
            .stub(get("/one").withId(id1).willReturn(ok("Updated")))
            .stub(post("/two").willReturn(ok()))
            .stub(put("/three").willReturn(ok()))
            .build());

    List<StubMapping> stubs = admin.listAllStubMappings().getMappings();
    assertThat(stubs.size(), is(3));
    assertThat(stubs.get(2).getResponse().getBody(), is("Updated"));
  }

  @Test
  public void ignoresExistingStubsIfConfigured() {
    UUID id1 = UUID.randomUUID();
    wm.stubFor(get("/one").withId(id1).willReturn(ok("Original")));

    WireMock wireMock = new WireMock(wireMockServer.port());
    wireMock.importStubMappings(
        stubImport()
            .stub(get("/one").withId(id1).willReturn(ok("Updated")))
            .stub(post("/two").willReturn(ok()))
            .stub(put("/three").willReturn(ok()))
            .ignoreExisting());

    List<StubMapping> stubs = admin.listAllStubMappings().getMappings();
    assertThat(stubs.size(), is(3));
    assertThat(stubs.get(2).getResponse().getBody(), is("Original"));
  }

  @Test
  public void deletesStubsNotInImportIfConfigured() {
    UUID id1 = UUID.randomUUID();
    wm.stubFor(get("/one").withId(id1).willReturn(ok("Original")));
    wm.stubFor(get("/four").willReturn(ok()));
    wm.stubFor(get("/five").willReturn(ok()));

    WireMock.importStubs(
        stubImport()
            .stub(get("/one").withId(id1).willReturn(ok("Updated")))
            .stub(post("/two").willReturn(ok()))
            .stub(put("/three").willReturn(ok()))
            .deleteAllExistingStubsNotInImport());

    List<StubMapping> stubs = admin.listAllStubMappings().getMappings();
    assertThat(stubs.size(), is(3));
    assertThat(stubs, hasItem(stubMappingWithUrl("/one")));
    assertThat(stubs, hasItem(stubMappingWithUrl("/two")));
    assertThat(stubs, hasItem(stubMappingWithUrl("/three")));
    assertThat(stubs.get(2).getResponse().getBody(), is("Updated"));
  }

  @Test
  public void doesNotDeleteStubsNotInImportIfNotConfigured() {
    UUID id1 = UUID.randomUUID();
    wm.stubFor(get("/one").withId(id1).willReturn(ok("Original")));
    wm.stubFor(get("/four").willReturn(ok()));
    wm.stubFor(get("/five").willReturn(ok()));

    WireMock.importStubs(
        stubImport()
            .stub(get("/one").withId(id1).willReturn(ok("Updated")))
            .stub(post("/two").willReturn(ok()))
            .stub(put("/three").willReturn(ok()))
            .overwriteExisting()
            .doNotDeleteExistingStubs());

    List<StubMapping> stubs = admin.listAllStubMappings().getMappings();
    assertThat(stubs.size(), is(5));
  }
}
