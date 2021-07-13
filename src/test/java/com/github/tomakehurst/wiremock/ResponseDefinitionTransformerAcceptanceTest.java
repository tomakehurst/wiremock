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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.testsupport.TestFiles.defaultTestFilesRoot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.junit.After;
import org.junit.Test;

public class ResponseDefinitionTransformerAcceptanceTest {

  WireMockServer wm;
  WireMockTestClient client;

  @Test
  public void transformerSpecifiedByClassTransformsHeadersStatusAndBody() {
    startWithExtensions(
        "com.github.tomakehurst.wiremock.ResponseDefinitionTransformerAcceptanceTest$ExampleTransformer");
    createStub("/to-transform");

    WireMockResponse response = client.get("/to-transform");
    assertThat(response.statusCode(), is(200));
    assertThat(response.firstHeader("MyHeader"), is("Transformed"));
    assertThat(response.content(), is("Transformed body"));
  }

  @Test
  public void supportsMultipleTransformers() {
    startWithExtensions(
        "com.github.tomakehurst.wiremock.ResponseDefinitionTransformerAcceptanceTest$MultiTransformer1",
        "com.github.tomakehurst.wiremock.ResponseDefinitionTransformerAcceptanceTest$MultiTransformer2");
    createStub("/to-multi-transform");

    WireMockResponse response = client.get("/to-multi-transform");
    assertThat(response.statusCode(), is(201));
    assertThat(response.content(), is("Expect this"));
  }

  @Test
  public void supportsSpecifiyingExtensionsByClass() {
    wm =
        new WireMockServer(
            wireMockConfig()
                .dynamicPort()
                .extensions(ExampleTransformer.class, MultiTransformer1.class));
    wm.start();
    client = new WireMockTestClient(wm.port());
    createStub("/to-class-transform");

    WireMockResponse response = client.get("/to-class-transform");
    assertThat(response.statusCode(), is(201));
    assertThat(response.content(), is("Transformed body"));
  }

  @Test
  public void supportsSpecifiyingExtensionsByInstance() {
    wm =
        new WireMockServer(
            wireMockConfig()
                .dynamicPort()
                .extensions(new ExampleTransformer(), new MultiTransformer2()));
    wm.start();
    client = new WireMockTestClient(wm.port());
    createStub("/to-instance-transform");

    WireMockResponse response = client.get("/to-instance-transform");
    assertThat(response.statusCode(), is(200));
    assertThat(response.content(), is("Expect this"));
  }

  @Test
  public void doesNotApplyNonGlobalExtensionsWhenNotExplicitlySpecfiedByStub() {
    wm =
        new WireMockServer(
            wireMockConfig()
                .dynamicPort()
                .extensions(new ExampleTransformer(), new NonGlobalTransformer()));
    wm.start();
    client = new WireMockTestClient(wm.port());
    createStub("/non-global-transform");

    WireMockResponse response = client.get("/non-global-transform");
    assertThat(response.content(), is("Transformed body"));
  }

  @Test
  public void appliesNonGlobalExtensionsWhenSpecifiedByStub() {
    wm = new WireMockServer(wireMockConfig().dynamicPort().extensions(new NonGlobalTransformer()));
    wm.start();
    client = new WireMockTestClient(wm.port());

    wm.stubFor(
        get(urlEqualTo("/local-transform"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("Should not see this")
                    .withTransformers("local")));

    WireMockResponse response = client.get("/local-transform");
    assertThat(response.content(), is("Non-global transformed body"));
  }

  @Test(expected = IllegalArgumentException.class)
  @SuppressWarnings("unchecked")
  public void preventsMoreThanOneExtensionWithTheSameNameFromBeingAdded() {
    new WireMockServer(
        wireMockConfig()
            .dynamicPort()
            .extensions(ExampleTransformer.class, AnotherExampleTransformer.class));
  }

  @Test
  public void supportsAccessingTheFilesFileSource() {
    startWithExtensions(
        "com.github.tomakehurst.wiremock.ResponseDefinitionTransformerAcceptanceTest$FileAccessTransformer");
    createStub("/files-access-transform");

    WireMockResponse response = client.get("/files-access-transform");
    assertThat(response.content(), is("Some example test from a file"));
  }

  @Test
  public void supportsParameters() {
    startWithExtensions(
        "com.github.tomakehurst.wiremock.ResponseDefinitionTransformerAcceptanceTest$ParameterisedTransformer");

    wm.stubFor(
        get(urlEqualTo("/transform-with-params"))
            .willReturn(
                aResponse().withStatus(200).withTransformerParameter("newBody", "Use this body")));

    assertThat(client.get("/transform-with-params").content(), is("Use this body"));
  }

  private void startWithExtensions(String... extensions) {
    wm =
        new WireMockServer(
            wireMockConfig()
                .dynamicPort()
                .withRootDirectory(defaultTestFilesRoot())
                .extensions(extensions));
    wm.start();
    client = new WireMockTestClient(wm.port());
  }

  @After
  public void cleanup() {
    if (wm != null) {
      wm.stop();
    }
  }

  private void createStub(String url) {
    wm.stubFor(
        get(urlEqualTo(url))
            .willReturn(
                aResponse()
                    .withHeader("MyHeader", "Initial")
                    .withStatus(300)
                    .withBody("Should not see this")));
  }

  public static class ExampleTransformer extends ResponseDefinitionTransformer {

    @Override
    public ResponseDefinition transform(
        Request request,
        ResponseDefinition responseDefinition,
        FileSource files,
        Parameters parameters) {
      return new ResponseDefinitionBuilder()
          .withHeader("MyHeader", "Transformed")
          .withStatus(200)
          .withBody("Transformed body")
          .build();
    }

    @Override
    public String getName() {
      return "example";
    }
  }

  public static class MultiTransformer1 extends ResponseDefinitionTransformer {

    @Override
    public ResponseDefinition transform(
        Request request,
        ResponseDefinition responseDefinition,
        FileSource files,
        Parameters parameters) {
      return ResponseDefinitionBuilder.like(responseDefinition).but().withStatus(201).build();
    }

    @Override
    public String getName() {
      return "multi1";
    }
  }

  public static class MultiTransformer2 extends ResponseDefinitionTransformer {

    @Override
    public ResponseDefinition transform(
        Request request,
        ResponseDefinition responseDefinition,
        FileSource files,
        Parameters parameters) {
      return ResponseDefinitionBuilder.like(responseDefinition)
          .but()
          .withBody("Expect this")
          .build();
    }

    @Override
    public String getName() {
      return "multi2";
    }
  }

  public static class NonGlobalTransformer extends ResponseDefinitionTransformer {

    @Override
    public ResponseDefinition transform(
        Request request,
        ResponseDefinition responseDefinition,
        FileSource files,
        Parameters parameters) {
      return ResponseDefinitionBuilder.like(responseDefinition)
          .but()
          .withBody("Non-global transformed body")
          .build();
    }

    @Override
    public boolean applyGlobally() {
      return false;
    }

    @Override
    public String getName() {
      return "local";
    }
  }

  public static class AnotherExampleTransformer extends ResponseDefinitionTransformer {

    @Override
    public ResponseDefinition transform(
        Request request,
        ResponseDefinition responseDefinition,
        FileSource files,
        Parameters parameters) {
      return responseDefinition;
    }

    @Override
    public String getName() {
      return "example";
    }
  }

  public static class FileAccessTransformer extends ResponseDefinitionTransformer {

    @Override
    public ResponseDefinition transform(
        Request request,
        ResponseDefinition responseDefinition,
        FileSource files,
        Parameters parameters) {
      return ResponseDefinitionBuilder.like(responseDefinition)
          .but()
          .withBody(files.getBinaryFileNamed("plain-example.txt").readContents())
          .build();
    }

    @Override
    public String getName() {
      return "filesource";
    }
  }

  public static class ParameterisedTransformer extends ResponseDefinitionTransformer {

    @Override
    public ResponseDefinition transform(
        Request request,
        ResponseDefinition responseDefinition,
        FileSource files,
        Parameters parameters) {
      return ResponseDefinitionBuilder.like(responseDefinition)
          .but()
          .withBody(parameters.getString("newBody"))
          .build();
    }

    @Override
    public String getName() {
      return "params";
    }
  }
}
