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
package com.github.tomakehurst.wiremock.standalone;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.testsupport.TestFiles.filePath;
import static com.google.common.base.Charsets.UTF_8;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.NotWritableException;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.stubbing.InMemoryStubMappings;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.io.Files;
import java.io.File;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JsonFileMappingsSourceTest {

  @Rule public final TemporaryFolder tempDir = new TemporaryFolder();

  InMemoryStubMappings stubMappings;
  JsonFileMappingsSource source;
  File stubMappingFile;

  @Before
  public void init() throws Exception {
    stubMappings = new InMemoryStubMappings();
  }

  private void configureWithMultipleMappingFile() throws Exception {
    stubMappingFile = tempDir.newFile("multi.json");
    Files.copy(new File(filePath("multi-stub/multi.json")), stubMappingFile);
    load();
  }

  private void configureWithSingleMappingFile() throws Exception {
    stubMappingFile = tempDir.newFile("single.json");
    Files.copy(new File(filePath("multi-stub/single.json")), stubMappingFile);
    load();
  }

  private void load() {
    source = new JsonFileMappingsSource(new SingleRootFileSource(tempDir.getRoot()));
    source.loadMappingsInto(stubMappings);
  }

  @Test
  public void loadsMappingsViaClasspathFileSource() {
    ClasspathFileSource fileSource = new ClasspathFileSource("jar-filesource");
    JsonFileMappingsSource source = new JsonFileMappingsSource(fileSource);
    InMemoryStubMappings stubMappings = new InMemoryStubMappings();

    source.loadMappingsInto(stubMappings);

    List<StubMapping> allMappings = stubMappings.getAll();
    assertThat(allMappings, hasSize(2));

    List<String> mappingRequestUrls =
        asList(allMappings.get(0).getRequest().getUrl(), allMappings.get(1).getRequest().getUrl());
    assertThat(mappingRequestUrls, is(asList("/second_test", "/test")));
  }

  @Test
  public void stubMappingFilesAreWrittenWithInsertionIndex() throws Exception {
    JsonFileMappingsSource source =
        new JsonFileMappingsSource(new SingleRootFileSource(tempDir.getRoot()));

    StubMapping stub = get("/saveable").willReturn(ok()).build();
    source.save(stub);

    File savedFile = tempDir.getRoot().listFiles()[0];
    String savedStub = Files.toString(savedFile, UTF_8);

    assertThat(savedStub, containsString("\"insertionIndex\" : 0"));
  }

  @Test
  public void refusesToRemoveStubMappingContainedInMultiFile() throws Exception {
    configureWithMultipleMappingFile();

    StubMapping firstStub = stubMappings.getAll().get(0);

    try {
      source.remove(firstStub);
      fail("Expected an exception to be thrown");
    } catch (Exception e) {
      assertThat(e, Matchers.instanceOf(NotWritableException.class));
      assertThat(
          e.getMessage(),
          is(
              "Stubs loaded from multi-mapping files are read-only, and therefore cannot be removed"));
    }

    assertThat(stubMappingFile.exists(), is(true));
  }

  @Test
  public void refusesToRemoveAllWhenMultiMappingFilesArePresent() throws Exception {
    configureWithMultipleMappingFile();

    try {
      source.removeAll();
      fail("Expected an exception to be thrown");
    } catch (Exception e) {
      assertThat(e, Matchers.instanceOf(NotWritableException.class));
      assertThat(
          e.getMessage(),
          is(
              "Some stubs were loaded from multi-mapping files which are read-only, so remove all cannot be performed"));
    }

    assertThat(stubMappingFile.exists(), is(true));
  }

  @Test
  public void refusesToSaveStubMappingOriginallyLoadedFromMultiMappingFile() throws Exception {
    configureWithMultipleMappingFile();

    StubMapping firstStub = stubMappings.getAll().get(0);

    try {
      source.save(firstStub);
      fail("Expected an exception to be thrown");
    } catch (Exception e) {
      assertThat(e, Matchers.instanceOf(NotWritableException.class));
      assertThat(
          e.getMessage(),
          is("Stubs loaded from multi-mapping files are read-only, and therefore cannot be saved"));
    }

    assertThat(stubMappingFile.exists(), is(true));
  }

  @Test
  public void savesStubMappingOriginallyLoadedFromSingleMappingFile() throws Exception {
    configureWithSingleMappingFile();

    StubMapping firstStub = stubMappings.getAll().get(0);
    firstStub.setName("New name");
    source.save(firstStub);

    assertThat(Files.toString(stubMappingFile, UTF_8), containsString("New name"));
  }

  @Test
  public void removesStubMappingOriginallyLoadedFromSingleMappingFile() throws Exception {
    configureWithSingleMappingFile();

    StubMapping firstStub = stubMappings.getAll().get(0);
    source.remove(firstStub);

    assertThat(stubMappingFile.exists(), is(false));
  }
}
