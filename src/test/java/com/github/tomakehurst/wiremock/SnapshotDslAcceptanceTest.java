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
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.testsupport.TestHttpHeader.withHeader;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.StubMappingTransformer;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.*;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.testsupport.WireMatchers;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class SnapshotDslAcceptanceTest extends AcceptanceTestBase {

  private WireMockServer targetService;
  private WireMockServer proxyingService;
  private WireMockTestClient client;
  private WireMock adminClient;
  private StubMapping proxyStub;

  public void init() {
    proxyingService =
        new WireMockServer(
            wireMockConfig()
                .dynamicPort()
                .extensions(new TestParameterisedTransformer())
                .withRootDirectory(setupTempFileRoot().getAbsolutePath()));
    proxyingService.start();
    proxyingService.stubFor(proxyAllTo("http://localhost:" + wireMockServer.port()));

    targetService = wireMockServer;
    proxyStub = targetService.stubFor(any(anyUrl()).willReturn(ok()));

    client = new WireMockTestClient(proxyingService.port());
    WireMock.configureFor(proxyingService.port());
    adminClient = WireMock.create().port(proxyingService.port()).build();
  }

  @After
  public void proxyServerShutdown() {
    proxyingService.resetMappings();
    proxyingService.stop();
  }

  @Test
  public void snapshotRecordsAllLoggedRequestsWhenNoParametersPassed() throws Exception {
    targetService.stubFor(
        get("/one")
            .willReturn(
                aResponse().withHeader("Content-Type", "text/plain").withBody("Number one")));

    client.get("/one");
    client.get("/two");
    client.postJson("/three", "{ \"counter\": 55 }");

    List<StubMapping> returnedMappings = proxyingService.snapshotRecord().getStubMappings();
    List<StubMapping> serverMappings = proxyingService.getStubMappings();

    assertTrue(
        "All of the returned mappings should be present in the server",
        serverMappings.containsAll(returnedMappings));
    assertThat(returnedMappings.size(), is(3));

    assertThat(returnedMappings.get(0).getRequest().getUrl(), is("/one"));
    assertThat(returnedMappings.get(0).getRequest().getHeaders(), nullValue());
    assertThat(returnedMappings.get(0).getRequest().getMethod(), is(RequestMethod.GET));
    assertThat(
        returnedMappings.get(0).getResponse().getHeaders().getHeader("Content-Type").firstValue(),
        is("text/plain"));
    assertThat(returnedMappings.get(0).getResponse().getBody(), is("Number one"));

    assertThat(returnedMappings.get(1).getRequest().getUrl(), is("/two"));

    assertThat(returnedMappings.get(2).getRequest().getUrl(), is("/three"));

    ContentPattern bodyPattern = returnedMappings.get(2).getRequest().getBodyPatterns().get(0);
    assertThat(bodyPattern, instanceOf(EqualToJsonPattern.class));
    JSONAssert.assertEquals("{ \"counter\": 55 }", bodyPattern.getExpected(), true);

    EqualToJsonPattern equalToJsonPattern = (EqualToJsonPattern) bodyPattern;
    assertThat(equalToJsonPattern.isIgnoreArrayOrder(), is(true));
    assertThat(equalToJsonPattern.isIgnoreExtraElements(), is(true));
  }

  @Test
  public void supportsFilteringByCriteria() throws Exception {
    client.get("/things/1");
    client.get("/things/2");
    client.get("/stuff/1");
    client.get("/things/3");
    client.get("/stuff/2");

    List<StubMapping> mappings =
        proxyingService
            .snapshotRecord(
                recordSpec().onlyRequestsMatching(getRequestedFor(urlPathMatching("/things/.*"))))
            .getStubMappings();

    assertThat(mappings.size(), is(3));
    assertThat(mappings, everyItem(WireMatchers.stubMappingWithUrl(urlPathMatching("/things.*"))));
    assertThat(
        mappings, not(hasItem(WireMatchers.stubMappingWithUrl(urlPathMatching("/stuff.*")))));
  }

  @Test
  public void supportsFilteringByServeEventId() throws Exception {
    client.get("/1");
    client.get("/2");
    client.get("/3");

    UUID serveEventId =
        WireMatchers.findServeEventWithUrl(proxyingService.getAllServeEvents(), "/2").getId();

    List<StubMapping> mappings =
        adminClient.takeSnapshotRecording(recordSpec().onlyRequestIds(singletonList(serveEventId)));

    assertThat(mappings.size(), is(1));
    assertThat(mappings.get(0).getRequest().getUrl(), is("/2"));
  }

  @Test
  public void willAllowNonProxiedEventsIfSpecified() throws Exception {
    proxyingService.removeStub(proxyStub);

    client.postJson("/record-this-anyway", "{ \"things\": 123 }");

    List<StubMapping> mappings =
        adminClient.takeSnapshotRecording(recordSpec().allowNonProxied(true));

    assertThat(mappings.size(), is(1));
    assertThat(mappings.get(0).getRequest().getUrl(), is("/record-this-anyway"));
    assertThat(
        mappings.get(0).getRequest().getBodyPatterns().get(0).getExpected(),
        WireMatchers.equalToJson("{ \"things\": 123 }"));
  }

  @Test
  public void supportsRequestHeaderCriteria() {
    client.get("/one", withHeader("Yes", "1"), withHeader("No", "1"));
    client.get("/two", withHeader("Yes", "2"), withHeader("Also-Yes", "BBB"));

    List<StubMapping> mappings =
        snapshotRecord(recordSpec().captureHeader("Yes").captureHeader("Also-Yes", true));

    StringValuePattern yesValuePattern =
        mappings.get(0).getRequest().getHeaders().get("Yes").getValuePattern();
    assertThat(yesValuePattern, instanceOf(EqualToPattern.class));
    assertThat(((EqualToPattern) yesValuePattern).getCaseInsensitive(), nullValue());
    assertFalse(mappings.get(0).getRequest().getHeaders().containsKey("No"));

    StringValuePattern alsoYesValuePattern =
        mappings.get(1).getRequest().getHeaders().get("Also-Yes").getValuePattern();
    assertThat(alsoYesValuePattern, instanceOf(EqualToPattern.class));
    assertThat(((EqualToPattern) alsoYesValuePattern).getCaseInsensitive(), is(true));
  }

  @Test
  public void supportsBodyExtractCriteria() throws Exception {
    targetService.stubFor(
        get("/small/text")
            .willReturn(aResponse().withHeader("Content-Type", "text/plain").withBody("123")));
    targetService.stubFor(
        get("/large/text")
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "text/plain")
                    .withBody("12345678901234567")));
    targetService.stubFor(
        get("/small/binary")
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/octet-stream")
                    .withBody(new byte[] {1, 2, 3})));
    targetService.stubFor(
        get("/large/binary")
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/octet-stream")
                    .withBody(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9})));

    client.get("/small/text");
    client.get("/large/text");
    client.get("/small/binary");
    client.get("/large/binary");

    List<StubMapping> mappings =
        snapshotRecord(recordSpec().extractTextBodiesOver(10).extractBinaryBodiesOver(5));

    assertThat(mappings.size(), is(4));
    assertThat(
        WireMatchers.findMappingWithUrl(mappings, "/small/text").getResponse().getBodyFileName(),
        nullValue());
    assertThat(
        WireMatchers.findMappingWithUrl(mappings, "/large/text").getResponse().getBodyFileName(),
        containsString("large_text"));
    assertThat(
        WireMatchers.findMappingWithUrl(mappings, "/small/binary").getResponse().getBodyFileName(),
        nullValue());
    assertThat(
        WireMatchers.findMappingWithUrl(mappings, "/large/binary").getResponse().getBodyFileName(),
        containsString("large_binary"));
  }

  @Test
  public void supportsDisablingRecordedStubPersistence() {
    client.get("/transient");

    List<StubMapping> mappings = snapshotRecord(recordSpec().makeStubsPersistent(false));

    assertThat(WireMatchers.findMappingWithUrl(mappings, "/transient").isPersistent(), nullValue());
  }

  @Test
  public void buildsAScenarioForRepeatedIdenticalRequests() {
    targetService.stubFor(get("/stateful").willReturn(ok("One")));
    client.get("/stateful");

    targetService.stubFor(get("/stateful").willReturn(ok("Two")));
    client.get("/stateful");

    targetService.stubFor(get("/stateful").willReturn(ok("Three")));
    client.get("/stateful");

    // Scenario creation is the default
    List<StubMapping> mappings = snapshotRecord();

    assertThat(client.get("/stateful").content(), is("One"));
    assertThat(client.get("/stateful").content(), is("Two"));
    assertThat(client.get("/stateful").content(), is("Three"));

    assertThat(mappings, everyItem(WireMatchers.isInAScenario()));
    assertThat(mappings.get(0).getRequiredScenarioState(), is(Scenario.STARTED));
    assertThat(mappings.get(1).getRequiredScenarioState(), is("scenario-1-stateful-2"));
    assertThat(mappings.get(2).getRequiredScenarioState(), is("scenario-1-stateful-3"));
  }

  @Test
  public void appliesTransformerWithParameters() {
    client.get("/transform-this");

    List<StubMapping> mappings =
        snapshotRecord(
            recordSpec()
                .transformers("test-transformer")
                .transformerParameters(
                    Parameters.from(
                        ImmutableMap.<String, Object>of(
                            "headerKey", "X-Key",
                            "headerValue", "My value"))));

    assertThat(
        mappings.get(0).getResponse().getHeaders().getHeader("X-Key").firstValue(), is("My value"));
  }

  @Test
  public void supportsConfigurationOfAutoRequestBodyPatternFactory() {
    client.postJson("/some-json", "{}");
    client.postWithBody("/some-json", "<foo/>", "application/xml", "utf-8");
    client.postWithBody("/some-json", "foo", "application/text", "utf-8");

    List<StubMapping> mappings =
        snapshotRecord(recordSpec().chooseBodyMatchTypeAutomatically(false, false, true));

    EqualToJsonPattern jsonBodyPattern =
        (EqualToJsonPattern) mappings.get(0).getRequest().getBodyPatterns().get(0);
    assertThat(jsonBodyPattern.getEqualToJson(), is("{}"));
    assertThat(jsonBodyPattern.isIgnoreArrayOrder(), is(false));
    assertThat(jsonBodyPattern.isIgnoreExtraElements(), is(false));

    EqualToXmlPattern xmlBodyPattern =
        (EqualToXmlPattern) mappings.get(1).getRequest().getBodyPatterns().get(0);
    assertThat(xmlBodyPattern.getEqualToXml(), is("<foo/>"));

    EqualToPattern textBodyPattern =
        (EqualToPattern) mappings.get(2).getRequest().getBodyPatterns().get(0);
    assertThat(textBodyPattern.getEqualTo(), is("foo"));
    assertThat(textBodyPattern.getCaseInsensitive(), is(true));
  }

  @Test
  public void supportsConfigurationOfRequestBodyPatternFactoryWithEqualToJsonPattern() {
    client.postJson("/some-json", "{}");

    List<StubMapping> mappings =
        snapshotRecord(recordSpec().matchRequestBodyWithEqualToJson(false, true));

    EqualToJsonPattern bodyPattern =
        (EqualToJsonPattern) mappings.get(0).getRequest().getBodyPatterns().get(0);
    assertThat(bodyPattern.isIgnoreArrayOrder(), is(false));
    assertThat(bodyPattern.isIgnoreExtraElements(), is(true));
  }

  @Test
  public void supportsConfigurationOfRequestBodyPatternFactoryWithEqualToXmlPattern() {
    client.postWithBody("/some-json", "<foo/>", "application/xml", "utf-8");

    List<StubMapping> mappings = snapshotRecord(recordSpec().matchRequestBodyWithEqualToXml());

    assertThat(
        mappings.get(0).getRequest().getBodyPatterns().get(0), instanceOf(EqualToXmlPattern.class));
  }

  @Test
  public void supportsConfigurationOfRequestBodyPatternFactoryWithEqualToPattern() {
    client.postWithBody("/some-json", "foo", "application/text", "utf-8");

    List<StubMapping> mappings = snapshotRecord(recordSpec().matchRequestBodyWithEqualTo(true));

    EqualToPattern bodyPattern =
        (EqualToPattern) mappings.get(0).getRequest().getBodyPatterns().get(0);
    assertThat(bodyPattern.getCaseInsensitive(), is(true));
  }

  @Test
  public void defaultsToAutomaticRequestBodyPattern() {
    client.postJson("/some-json", "{}");

    List<StubMapping> mappings = snapshotRecord(recordSpec());

    EqualToJsonPattern bodyPattern =
        (EqualToJsonPattern) mappings.get(0).getRequest().getBodyPatterns().get(0);
    assertThat(bodyPattern, is(new EqualToJsonPattern("{}", true, true)));
  }

  @Test
  public void staticClientIsSupportedWithDefaultSpec() {
    client.get("/get-this");

    snapshotRecord();

    List<StubMapping> serverMappings = proxyingService.getStubMappings();
    assertThat(serverMappings, hasItem(WireMatchers.stubMappingWithUrl("/get-this")));
  }

  @Test
  public void staticClientIsSupportedWithSpecProvided() {
    client.get("/get-this");
    client.get("/but-not-this");

    snapshotRecord(recordSpec().onlyRequestsMatching(getRequestedFor(urlEqualTo("/get-this"))));

    List<StubMapping> serverMappings = proxyingService.getStubMappings();
    assertThat(serverMappings, hasItem(WireMatchers.stubMappingWithUrl("/get-this")));
    assertThat(serverMappings, not(hasItem(WireMatchers.stubMappingWithUrl("/but-not-this"))));
  }

  @Test
  public void instanceClientIsSupportedWithDefaultSpec() {
    client.get("/get-this-too");
    adminClient.takeSnapshotRecording();

    List<StubMapping> serverMappings = proxyingService.getStubMappings();
    assertThat(serverMappings, hasItem(WireMatchers.stubMappingWithUrl("/get-this-too")));
  }

  @Test
  public void instanceClientIsSupportedWithSpecProvided() {
    client.get("/get-this");
    client.get("/but-not-this");

    adminClient.takeSnapshotRecording(
        recordSpec().onlyRequestsMatching(getRequestedFor(urlEqualTo("/get-this"))));

    List<StubMapping> serverMappings = proxyingService.getStubMappings();
    assertThat(serverMappings, hasItem(WireMatchers.stubMappingWithUrl("/get-this")));
    assertThat(serverMappings, not(hasItem(WireMatchers.stubMappingWithUrl("/but-not-this"))));
  }

  public static class TestParameterisedTransformer extends StubMappingTransformer {

    @Override
    public StubMapping transform(StubMapping stubMapping, FileSource files, Parameters parameters) {
      ResponseDefinition newResponse =
          ResponseDefinitionBuilder.like(stubMapping.getResponse())
              .but()
              .withHeader(parameters.getString("headerKey"), parameters.getString("headerValue"))
              .build();
      stubMapping.setResponse(newResponse);
      return stubMapping;
    }

    @Override
    public boolean applyGlobally() {
      return false;
    }

    @Override
    public String getName() {
      return "test-transformer";
    }
  }
}
