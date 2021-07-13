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
import static com.github.tomakehurst.wiremock.core.WireMockApp.FILES_ROOT;
import static com.github.tomakehurst.wiremock.core.WireMockApp.MAPPINGS_ROOT;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.hasFileContaining;
import static com.google.common.base.Charsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class StubMappingPersistenceAcceptanceTest {

  Path rootDir;
  Path mappingsDir;
  WireMockServer wireMockServer;
  WireMockTestClient testClient;
  Stubbing wm;

  @Before
  public void init() throws Exception {
    rootDir = Files.createTempDirectory("temp-filesource");
    mappingsDir = rootDir.resolve("mappings");
    FileSource fileSource = new SingleRootFileSource(rootDir.toAbsolutePath().toString());
    fileSource.createIfNecessary();
    FileSource filesFileSource = fileSource.child(FILES_ROOT);
    filesFileSource.createIfNecessary();
    FileSource mappingsFileSource = fileSource.child(MAPPINGS_ROOT);
    mappingsFileSource.createIfNecessary();

    wireMockServer = new WireMockServer(wireMockConfig().fileSource(fileSource).dynamicPort());
    wireMockServer.start();
    testClient = new WireMockTestClient(wireMockServer.port());
    WireMock.configureFor(wireMockServer.port());
    wm = wireMockServer;
  }

  @Test
  public void savesAllInMemoryStubMappings() {
    wm.stubFor(get(urlEqualTo("/1")).willReturn(aResponse().withBody("one")));
    wm.stubFor(get(urlEqualTo("/2")).willReturn(aResponse().withBody("two")));
    wm.stubFor(get(urlEqualTo("/3")).willReturn(aResponse().withBody("three")));

    wireMockServer.saveMappings();

    assertThat(mappingsDir, hasFileContaining("one"));
    assertThat(mappingsDir, hasFileContaining("two"));
    assertThat(mappingsDir, hasFileContaining("three"));
  }

  @Test
  public void savesEditedStubToTheFileItOriginatedFrom() throws Exception {
    UUID stubId = UUID.randomUUID();

    writeMappingFile(
        "mapping-to-edit.json",
        get(urlEqualTo("/edit")).withId(stubId).willReturn(aResponse().withBody("initial")));

    wireMockServer.resetToDefaultMappings(); // Loads from the file system

    assertThat(wm.getStubMappings().get(0).getId(), is(stubId));
    assertThat(wm.getStubMappings().get(0).getResponse().getBody(), is("initial"));

    wm.editStub(
        get(urlEqualTo("/edit")).withId(stubId).willReturn(aResponse().withBody("modified")));

    wireMockServer.saveMappings();

    assertMappingsDirContainsOneFile();
    assertThat(mappingsDir, hasFileContaining("modified"));
  }

  @Test
  public void savesSingleStubOnCreationIfFlaggedPersistent() {
    stubFor(get(urlEqualTo("/save-immediately")).persistent());
    assertThat(mappingsDir, hasFileContaining("/save-immediately"));
  }

  @Test
  public void doesNotSaveSingleStubOnCreationIfNotFlaggedPersistent() {
    stubFor(get(urlEqualTo("/save-immediately")));
    assertMappingsDirIsEmpty();
  }

  @Test
  public void savesSingleStubOnEditIfFlaggedPersistent() {
    UUID stubId = UUID.randomUUID();
    stubFor(
        get(urlEqualTo("/save-immediately"))
            .persistent()
            .withId(stubId)
            .willReturn(aResponse().withBody("initial")));

    assertThat(mappingsDir, hasFileContaining("/save-immediately", "initial"));

    editStub(
        get(urlEqualTo("/save-immediately"))
            .persistent()
            .withId(stubId)
            .willReturn(aResponse().withBody("modified")));

    assertMappingsDirContainsOneFile();
    assertThat(mappingsDir, hasFileContaining("/save-immediately", "modified"));
  }

  @Test
  public void doesNotSaveSingleStubOnEditIfNotFlaggedPersistent() {
    UUID stubId = UUID.randomUUID();
    stubFor(get(urlEqualTo("/no-save")).withId(stubId).willReturn(aResponse().withBody("initial")));

    editStub(
        get(urlEqualTo("/no-save")).withId(stubId).willReturn(aResponse().withBody("modified")));

    assertMappingsDirIsEmpty();
  }

  @Test
  public void deletesPersistentStubMappingIfFlaggedPersistent() {
    StubMapping stubMapping = stubFor(get(urlEqualTo("/to-delete")).persistent());
    assertMappingsDirContainsOneFile();

    removeStub(stubMapping);
    assertMappingsDirIsEmpty();
  }

  @Test
  public void doesNotDeleteStubMappingFromDiskIfNotFlaggedPersistent() throws Exception {
    UUID id = UUID.randomUUID();
    StubMapping stubMapping = get(urlEqualTo("/do-not-delete")).withId(id).build();
    Files.write(mappingsDir.resolve("do-not-delete.json"), Json.write(stubMapping).getBytes());
    resetToDefault();

    assertThat(getSingleStubMapping(id).getRequest().getUrl(), is("/do-not-delete"));
    assertMappingsDirContainsOneFile();

    removeStub(stubMapping);
    assertMappingsDirContainsOneFile();
  }

  @Test
  public void deletesAllPersistentStubMappingsOnReset() {
    stubFor(get(urlEqualTo("/to-delete/1")).persistent());
    stubFor(get(urlEqualTo("/to-delete/2")).persistent());
    stubFor(get(urlEqualTo("/to-delete/3")).persistent());

    assertMappingsDirSize(3);

    removeAllMappings();

    assertMappingsDirIsEmpty();
  }

  @Test
  public void deletesNestedPersistentStubMapping() throws IOException {
    UUID stubId = UUID.randomUUID();
    Path subDirectoryUnderMappingsRoot = Files.createDirectory(mappingsDir.resolve("sub-dir"));
    Path mappingFilePath = subDirectoryUnderMappingsRoot.resolve("mapping-to-delete.json");
    writeMappingFile(
        mappingFilePath.toString(), get(urlEqualTo("/to-delete")).withId(stubId).persistent());

    wireMockServer.resetToDefaultMappings(); // Loads from the file system
    assertThat(mappingFilePath.toFile().exists(), is(true));

    StubMapping stubMapping = wm.getStubMappings().get(0);
    assertThat(stubMapping.getId(), is(stubId));

    removeStub(stubMapping);
    assertThat(mappingFilePath.toFile().exists(), is(false));
  }

  private void writeMappingFile(String name, MappingBuilder stubBuilder) throws IOException {
    byte[] json = Json.write(stubBuilder.build()).getBytes(UTF_8);
    Files.write(mappingsDir.resolve(name), json);
  }

  private void assertMappingsDirIsEmpty() {
    assertMappingsDirSize(0);
  }

  private void assertMappingsDirContainsOneFile() {
    assertMappingsDirSize(1);
  }

  private void assertMappingsDirSize(int size) {
    assertThat(mappingsDir.toFile().list().length, is(size));
  }
}
