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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.testsupport.TestFiles.defaultTestFilesRoot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.testsupport.WireMatchers;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import java.net.InetAddress;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class ResponseTemplatingAcceptanceTest {

  public static class Local {

    WireMockTestClient client;

    @Rule
    public WireMockRule wm =
        new WireMockRule(
            options().dynamicPort().extensions(new ResponseTemplateTransformer(false)));

    @Before
    public void init() {
      client = new WireMockTestClient(wm.port());
    }

    @Test
    public void appliesResponseTemplateWhenAddedToStubMapping() {
      wm.stubFor(
          get(urlPathEqualTo("/templated"))
              .willReturn(
                  aResponse()
                      .withBody("{{request.path.[0]}}")
                      .withTransformers("response-template")));

      assertThat(client.get("/templated").content(), is("templated"));
    }

    @Test
    public void doesNotApplyResponseTemplateWhenNotAddedToStubMapping() {
      wm.stubFor(
          get(urlPathEqualTo("/not-templated"))
              .willReturn(aResponse().withBody("{{request.path.[0]}}")));

      assertThat(client.get("/not-templated").content(), is("{{request.path.[0]}}"));
    }
  }

  public static class Global {

    WireMockTestClient client;

    @Rule
    public WireMockRule wm =
        new WireMockRule(
            options()
                .dynamicPort()
                .withRootDirectory(defaultTestFilesRoot())
                .extensions(new ResponseTemplateTransformer(true)));

    @Before
    public void init() {
      client = new WireMockTestClient(wm.port());
    }

    @Test
    public void appliesResponseTemplate() {
      wm.stubFor(
          get(urlPathEqualTo("/templated"))
              .willReturn(aResponse().withBody("{{request.path.[0]}}")));

      assertThat(client.get("/templated").content(), is("templated"));
    }

    @Test
    public void appliesToResponseBodyFromFile() {
      wm.stubFor(
          get(urlPathEqualTo("/templated"))
              .willReturn(aResponse().withBodyFile("templated-example-1.txt")));

      assertThat(client.get("/templated").content(), is("templated"));
    }

    @Test
    public void copesWithBase64BodiesWithoutTemplateElements() {
      wm.stubFor(
          get(urlMatching("/documents/document/.+"))
              .willReturn(
                  ok().withBase64Body(
                          "JVBERi0xLjEKJcKlwrHDqwoKMSAwIG9iagogIDw8IC9UeXBlIC9DYXRhbG9nCiAgICAgL1BhZ2VzIDIgMCBSCiAgPj4KZW5kb2JqCgoyIDAgb2JqCiAgPDwgL1R5cGUgL1BhZ2VzCiAgICAgL0tpZHMgWzMgMCBSXQogICAgIC9Db3VudCAxCiAgICAgL01lZGlhQm94IFswIDAgMzAwIDE0NF0KICA+PgplbmRvYmoKCjMgMCBvYmoKICA8PCAgL1R5cGUgL1BhZ2UKICAgICAgL1BhcmVudCAyIDAgUgogICAgICAvUmVzb3VyY2VzCiAgICAgICA8PCAvRm9udAogICAgICAgICAgIDw8IC9GMQogICAgICAgICAgICAgICA8PCAvVHlwZSAvRm9udAogICAgICAgICAgICAgICAgICAvU3VidHlwZSAvVHlwZTEKICAgICAgICAgICAgICAgICAgL0Jhc2VGb250IC9UaW1lcy1Sb21hbgogICAgICAgICAgICAgICA+PgogICAgICAgICAgID4+CiAgICAgICA+PgogICAgICAvQ29udGVudHMgNCAwIFIKICA+PgplbmRvYmoKCjQgMCBvYmoKICA8PCAvTGVuZ3RoIDU1ID4+CnN0cmVhbQogIEJUCiAgICAvRjEgMTggVGYKICAgIDAgMCBUZAogICAgKEhlbGxvIFdvcmxkKSBUagogIEVUCmVuZHN0cmVhbQplbmRvYmoKCnhyZWYKMCA1CjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAwMDAxOCAwMDAwMCBuIAowMDAwMDAwMDc3IDAwMDAwIG4gCjAwMDAwMDAxNzggMDAwMDAgbiAKMDAwMDAwMDQ1NyAwMDAwMCBuIAp0cmFpbGVyCiAgPDwgIC9Sb290IDEgMCBSCiAgICAgIC9TaXplIDUKICA+PgpzdGFydHhyZWYKNTY1CiUlRU9GCg==")));

      WireMockResponse response = client.get("/documents/document/123");

      assertThat(response.statusCode(), is(200));
    }

    @Test
    public void supportsSelectionResponseBodyTemplateViaTemplate() {
      wm.stubFor(
          get(urlPathMatching("/templated/.*"))
              .willReturn(aResponse().withBodyFile("templated-example-{{request.path.1}}.txt")));

      assertThat(client.get("/templated/2").content(), is("templated"));
      assertThat(client.get("/templated/3").content(), is("3"));
    }

    @Test
    public void cacheIsClearedWhenStubEdited() {
      String url = "/templated/one/two";
      UUID id = UUID.randomUUID();

      wm.stubFor(
          get(urlPathEqualTo(url))
              .withId(id)
              .willReturn(
                  aResponse()
                      .withHeader("X-Value", "{{request.path.1}}")
                      .withBody("{{request.path.1}}")));

      WireMockResponse response = client.get(url);
      assertThat(response.content(), is("one"));
      assertThat(response.firstHeader("X-Value"), is("one"));

      wm.stubFor(
          get(urlPathEqualTo(url))
              .withId(id)
              .willReturn(
                  aResponse()
                      .withHeader("X-Value", "{{request.path.2}}")
                      .withBody("{{request.path.2}}")));

      response = client.get(url);
      assertThat(response.content(), is("two"));
      assertThat(response.firstHeader("X-Value"), is("two"));
    }

    @Test
    public void supportsDisablingTemplatingOfBodyFilesPerStub() {
      UUID id = UUID.randomUUID();
      wm.stubFor(
          get(urlPathEqualTo("/templated"))
              .withId(id)
              .willReturn(aResponse().withBodyFile("templated-example-1.txt")));

      assertThat(client.get("/templated").content(), is("templated"));

      wm.stubFor(
          get(urlPathEqualTo("/templated"))
              .withId(id)
              .willReturn(
                  aResponse()
                      .withBodyFile("templated-example-1.txt")
                      .withTransformerParameter("disableBodyFileTemplating", true)));

      assertThat(client.get("/templated").content(), is("{{request.path.[0]}}"));

      wm.stubFor(
          get(urlPathMatching("/templated/.*"))
              .withId(id)
              .willReturn(
                  aResponse()
                      .withBodyFile("templated-example-{{request.path.1}}.txt")
                      .withTransformerParameter("disableBodyFileTemplating", true)));

      assertThat(client.get("/templated/1").content(), is("{{request.path.[0]}}"));
    }

    @Test
    public void supportsJsonBodiesWithTemplating() {
      String stubJson =
          "{\n"
              + "  \"request\": {\n"
              + "    \"method\": \"POST\",\n"
              + "    \"url\" : \"/json-body-templating\"\n"
              + "  },\n"
              + "  \"response\": {\n"
              + "    \"status\": 200,\n"
              + "    \"jsonBody\": {\n"
              + "      \"modified\": \"{{jsonPath request.body '$.arrayprop.length()'}}\"\n"
              + "    },\n"
              + "    \"headers\": {\n"
              + "      \"Content-Type\": \"application/json\"\n"
              + "    }\n"
              + "  }\n"
              + "}";

      client.postJson("/__admin/mappings", stubJson);

      WireMockResponse response =
          client.postJson("/json-body-templating", "{ \"arrayprop\": [1,2,3] }");
      assertThat(response.content(), response.statusCode(), is(200));

      assertThat(response.content(), WireMatchers.equalToJson("{ \"modified\": \"3\" }"));
    }

    @Test
    public void jsonBodyTemplatesCanSpecifyRequestAttributes() {
      String stubJson =
          "{\n"
              + "  \"request\": {\n"
              + "    \"method\": \"GET\",\n"
              + "    \"urlPath\": \"/jsonBody/template\"\n"
              + "  },\n"
              + "  \"response\": {\n"
              + "    \"jsonBody\": {\n"
              + "      \"Key\": \"Hello world {{request.query.qp}}!\"\n"
              + "    },\n"
              + "    \"status\": 200,\n"
              + "    \"transformers\": [\n"
              + "      \"response-template\"\n"
              + "    ]\n"
              + "  }\n"
              + "}";
      client.postJson("/__admin/mappings", stubJson);

      WireMockResponse response = client.get("/jsonBody/template?qp=2");

      assertThat(response.content(), is("{\"Key\":\"Hello world 2!\"}"));
    }

    @Test
    public void canLookupSquareBracketedQueryParameters() {
      wm.stubFor(
          get(urlPathEqualTo("/squares"))
              .willReturn(ok("ID: {{lookup request.query 'filter[id]'}}")));

      assertThat(client.get("/squares?filter[id]=321").content(), is("ID: 321"));
      assertThat(client.get("/squares?filter%5Bid%5D=321").content(), is("ID: 321"));
    }
  }

  public static class RestrictedSystemPropertiesAndEnvVars {

    WireMockTestClient client;

    @Rule
    public WireMockRule wm =
        new WireMockRule(
            options()
                .dynamicPort()
                .withRootDirectory(defaultTestFilesRoot())
                .extensions(
                    new ResponseTemplateTransformer.Builder()
                        .global(true)
                        .permittedSystemKeys("allowed.*")
                        .build()));

    @Before
    public void init() {
      client = new WireMockTestClient(wm.port());
    }

    @Test
    public void appliesResponseTemplateWithHostname() throws Exception {
      wm.stubFor(
          get(urlPathEqualTo("/templated")).willReturn(aResponse().withBody("{{hostname}}")));

      String expectedHostname = InetAddress.getLocalHost().getHostName();

      assertThat(client.get("/templated").content(), is(expectedHostname));
    }

    @Test
    public void rendersPermittedSystemProperty() {
      System.setProperty("allowed.thing", "123");

      wm.stubFor(
          get(urlPathEqualTo("/templated"))
              .willReturn(
                  aResponse().withBody("{{systemValue type='PROPERTY' key='allowed.thing'}}")));

      assertThat(client.get("/templated").content(), is("123"));
    }

    @Test
    public void refusesToRenderForbiddenSystemProperty() {
      System.setProperty("forbidden.thing", "456");

      wm.stubFor(
          get(urlPathEqualTo("/templated"))
              .willReturn(
                  aResponse().withBody("{{systemValue type='PROPERTY' key='forbidden.thing'}}")));

      assertThat(
          client.get("/templated").content(), is("[ERROR: Access to forbidden.thing is denied]"));
    }

    @Test
    public void appliesResponseTemplateShouldNotEmptyWithExistingSystemValue() {
      wm.stubFor(
          get(urlPathEqualTo("/templated"))
              .willReturn(aResponse().withBody("{{systemValue type='ENVIRONMENT' key='PATH'}}")));

      assertThat(client.get("/templated").content(), notNullValue());
    }
  }
}
