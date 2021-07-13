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
import static com.github.tomakehurst.wiremock.common.Gzip.gzip;
import static com.github.tomakehurst.wiremock.common.Strings.DEFAULT_CHARSET;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.testsupport.TestHttpHeader.withHeader;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.findMappingWithUrl;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.InvalidInputException;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.recording.NotRecordingException;
import com.github.tomakehurst.wiremock.recording.RecordingStatus;
import com.github.tomakehurst.wiremock.recording.RecordingStatusResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import java.io.File;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.entity.StringEntity;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

public class RecordingDslAcceptanceTest extends AcceptanceTestBase {

  private WireMockServer targetService;
  private WireMockServer proxyingService;
  private WireMockTestClient client;
  private WireMock adminClient;
  private String targetBaseUrl;
  private File fileRoot;

  public void init() {
    fileRoot = setupTempFileRoot();
    proxyingService =
        new WireMockServer(
            wireMockConfig().dynamicPort().withRootDirectory(fileRoot.getAbsolutePath()));
    proxyingService.start();

    targetService = wireMockServer;
    targetBaseUrl = "http://localhost:" + targetService.port();

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
  public void
      startsRecordingWithDefaultSpecFromTheSpecifiedProxyBaseUrlWhenServeEventsAlreadyExist() {
    targetService.stubFor(get("/record-this").willReturn(okForContentType("text/plain", "Got it")));
    targetService.stubFor(get(urlPathMatching("/do-not-record-this/.*")).willReturn(noContent()));

    client.get("/do-not-record-this/1");
    client.get("/do-not-record-this/2");
    client.get("/do-not-record-this/3");

    startRecording(targetBaseUrl);

    client.get("/record-this");

    List<StubMapping> returnedMappings = stopRecording().getStubMappings();
    client.get("/do-not-record-this/4");

    assertThat(returnedMappings.size(), is(1));
    assertThat(returnedMappings.get(0).getRequest().getUrl(), is("/record-this"));

    StubMapping mapping = findMappingWithUrl(proxyingService.getStubMappings(), "/record-this");
    assertThat(mapping.getResponse().getBody(), is("Got it"));
  }

  @Test
  public void generatesStubNameFromUrlPath() {
    targetService.stubFor(get(urlPathMatching("/record-this/.*")).willReturn(ok("Fine")));

    startRecording(targetBaseUrl);

    String url = "/record-this/with$!/safe/ŃaMe?ignore=this";
    client.get(url);

    List<StubMapping> mappings = stopRecording().getStubMappings();

    StubMapping mapping = mappings.get(0);
    assertThat(mapping.getName(), is("record-this_with_safe_name"));
  }

  @Test
  public void
      startsRecordingWithDefaultSpecFromTheSpecifiedProxyBaseUrlWhenNoServeEventsAlreadyExist() {
    targetService.stubFor(get("/record-this").willReturn(okForContentType("text/plain", "Got it")));

    startRecording(targetBaseUrl);

    client.get("/record-this");

    List<StubMapping> returnedMappings = stopRecording().getStubMappings();

    assertThat(returnedMappings.size(), is(1));
    assertThat(returnedMappings.get(0).getRequest().getUrl(), is("/record-this"));

    StubMapping mapping = findMappingWithUrl(proxyingService.getStubMappings(), "/record-this");
    assertThat(mapping.getResponse().getBody(), is("Got it"));
  }

  @Test
  public void recordsNothingWhenNoServeEventsAreRecievedDuringRecording() {
    targetService.stubFor(get(urlPathMatching("/do-not-record-this/.*")).willReturn(noContent()));

    client.get("/do-not-record-this/1");
    client.get("/do-not-record-this/2");

    startRecording(targetBaseUrl);
    List<StubMapping> returnedMappings = stopRecording().getStubMappings();
    client.get("/do-not-record-this/3");

    assertThat(returnedMappings.size(), is(0));
    assertThat(proxyingService.getStubMappings(), Matchers.<StubMapping>empty());
  }

  @Test
  public void recordsNothingWhenNoServeEventsAreRecievedAtAll() {
    startRecording(targetBaseUrl);
    List<StubMapping> returnedMappings = stopRecording().getStubMappings();

    assertThat(returnedMappings.size(), is(0));
    assertThat(proxyingService.getStubMappings(), Matchers.<StubMapping>empty());
  }

  @Test
  public void honoursRecordSpecWhenPresent() {
    targetService.stubFor(get("/record-this-with-header").willReturn(ok()));

    startRecording(recordSpec().forTarget(targetBaseUrl).captureHeader("Accept"));

    client.get("/record-this", withHeader("Accept", "text/plain"));

    List<StubMapping> returnedMappings = stopRecording().getStubMappings();

    assertThat(
        returnedMappings.get(0).getRequest().getHeaders().get("Accept").getExpected(),
        is("text/plain"));
  }

  @Test
  public void supportsInstanceClientWithDefaultSpec() {
    targetService.stubFor(get("/record-this").willReturn(okForContentType("text/plain", "Got it")));

    adminClient.startStubRecording(targetBaseUrl);

    client.get("/record-this");

    List<StubMapping> returnedMappings = adminClient.stopStubRecording().getStubMappings();

    assertThat(returnedMappings.size(), is(1));
    assertThat(returnedMappings.get(0).getRequest().getUrl(), is("/record-this"));

    StubMapping mapping = findMappingWithUrl(proxyingService.getStubMappings(), "/record-this");
    assertThat(mapping.getResponse().getBody(), is("Got it"));
  }

  @Test
  public void supportsInstanceClientWithSpec() {
    targetService.stubFor(post("/record-this-with-body").willReturn(ok()));

    adminClient.startStubRecording(
        recordSpec().forTarget(targetBaseUrl).matchRequestBodyWithEqualToJson(true, true));

    client.postJson("/record-this-with-body", "{}");

    List<StubMapping> returnedMappings = adminClient.stopStubRecording().getStubMappings();

    EqualToJsonPattern bodyPattern =
        (EqualToJsonPattern) returnedMappings.get(0).getRequest().getBodyPatterns().get(0);
    assertThat(bodyPattern.isIgnoreArrayOrder(), is(true));
    assertThat(bodyPattern.isIgnoreExtraElements(), is(true));
  }

  @Test
  public void supportsDirectDslCallsWithSpec() {
    targetService.stubFor(post("/record-this-with-body").willReturn(ok()));

    proxyingService.startRecording(
        recordSpec().forTarget(targetBaseUrl).matchRequestBodyWithEqualToJson(true, true));

    client.postJson("/record-this-with-body", "{}");

    List<StubMapping> returnedMappings = proxyingService.stopRecording().getStubMappings();

    EqualToJsonPattern bodyPattern =
        (EqualToJsonPattern) returnedMappings.get(0).getRequest().getBodyPatterns().get(0);
    assertThat(bodyPattern.isIgnoreArrayOrder(), is(true));
    assertThat(bodyPattern.isIgnoreExtraElements(), is(true));
  }

  @Test
  public void returnsTheRecordingStatus() {
    proxyingService.startRecording(targetBaseUrl);

    RecordingStatusResult result = getRecordingStatus();

    assertThat(result.getStatus(), is(RecordingStatus.Recording));
  }

  @Test
  public void returnsTheRecordingStatusViaInstanceClient() {
    proxyingService.startRecording(targetBaseUrl);
    proxyingService.stopRecording();

    RecordingStatusResult result = adminClient.getStubRecordingStatus();

    assertThat(result.getStatus(), is(RecordingStatus.Stopped));
  }

  @Test
  public void returnsTheRecordingStatusViaDirectDsl() {
    proxyingService.startRecording(targetBaseUrl);

    RecordingStatusResult result = proxyingService.getRecordingStatus();

    assertThat(result.getStatus(), is(RecordingStatus.Recording));
  }

  @Test
  public void recordsIntoPlainTextWhenRequestIsGZipped() {
    proxyingService.startRecording(targetBaseUrl);
    targetService.stubFor(post("/gzipped").willReturn(ok("Zippy")));

    HttpEntity compressedBody =
        new GzipCompressingEntity(new StringEntity("expected body", TEXT_PLAIN));
    client.post("/gzipped", compressedBody);

    StubMapping mapping = proxyingService.stopRecording().getStubMappings().get(0);
    assertThat(mapping.getRequest().getBodyPatterns().get(0).getExpected(), is("expected body"));
  }

  @Test
  public void recordsIntoPlainTextWhenResponseIsGZipped() {
    proxyingService.startRecording(targetBaseUrl);

    byte[] gzippedBody = gzip("Zippy");
    targetService.stubFor(
        get("/gzipped-response")
            .willReturn(
                aResponse()
                    .withHeader("Content-Encoding", "gzip")
                    .withHeader("Content-Type", "text/plain")
                    .withBody(gzippedBody)));

    client.get("/gzipped-response");

    StubMapping mapping = proxyingService.stopRecording().getStubMappings().get(0);
    assertThat(mapping.getResponse().getBody(), is("Zippy"));
  }

  @Test
  public void recordsIntoPlainBinaryWhenResponseIsGZipped() {
    proxyingService.startRecording(targetBaseUrl);

    byte[] originalBody = "sdkfnslkdjfsjdf".getBytes(DEFAULT_CHARSET);
    byte[] gzippedBody = gzip(originalBody);
    targetService.stubFor(
        get("/gzipped-response")
            .willReturn(
                aResponse()
                    .withHeader("Content-Encoding", "gzip")
                    .withHeader("Content-Type", APPLICATION_OCTET_STREAM.getMimeType())
                    .withBody(gzippedBody)));

    client.get("/gzipped-response");

    StubMapping mapping = proxyingService.stopRecording().getStubMappings().get(0);
    assertThat(mapping.getResponse().getByteBody(), is(originalBody));
  }

  static final String IMAGE_CONTENT_BASE64 =
      "iVBORw0KGgoAAAANSUhEUgAAACAAAAAPCAYAAACFgM0XAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAABcSAAAXEgFnn9JSAAAAB3RJTUUH4AYeEQ8RFdqRVAAAA/VJREFUOMvFk9+LVHUYxj/v9/yYmbVVN1NKDVvDH6yBiuiWrG6LelG33lQQ1EVIUNA/YBARRNHVFgRWF93URV4VJGQm/l5KFNfaQNnN1a3NH+vOzM6cOed8v9+3i5mdGcLb6IUDhwfe532+D88D//PIpi8n3pIwDFAENPNp42huzMy0tdHauPBCjPYDihgkin/wNr8wdfxrYfvIcy6It7zoT8moOxLHWBfi0AVz+ePDh75/86NPwWgf8BLQiyBY+cPPmm8w5A99MAVAKGH4ikSFrQCoqnhfy4Pwc5fMrUniwjs9xSX9SFvv+sFt/WOTMzuXShQfliB6esD9Sa8keAx48HU5+/prn51DKUvIIPAhUALwVZnSRC4QMLXIaNTZb0G1yS8iQbD31t4nEZFttTxd61BAWh87r0zMPkJc2qAm2NTHAsP+alufWgHLFrPcD2hFAPYtHkdBG/K4FHVQIqUjIEuP4f29DhLs3Pjd2KMiMpQ5F6XWdgyAfuvsFoJwl5egb0Bvsllv4pGmvgwIWC5FHTar3TJguL1pASWUkh4INzhTe/eJ5jlXnRtX78bbJhuzLotLe4BBr0rd5nT00uOc3U8QDAPs9eMso9aKD2guSKxIUYfJZDewaXFRc0EChRK7fVUek0KT1fQOjlTU2p+6ctljlVdFdbMCSZ7htSMhz/ODxoRDS0kY1vGOO761XVAosR3hELC0veiAAkhR+yXWHVJQ5kfXYJJrE2iensT7KqqQ5eRODyCyQoDUWTJnmwqsJUuzzc6EqzfoDE/5G037AXV4CRSKIKGuAp6nW5w0xUmkBQz7I2OICTCuMoevV66qsxPkFl9PaDgXait1XrVsbX4e69B6gyTLUGDI/8oKKk37QRHOUaAqkYIgQNRV92lC7km8yMqeHL9SAJP9NUXfrqH7Wq+f1FqdvJaQtt8FCr8Fzo9qPam5WkKijh5SRvQKwaLvUJeAIxLrZHdiO8HmqMQ6hmkjG4GtAGb6vZeZPzuGmy+f8OWFJM0yrHRYBM6UGva4VmrXs6RBQwz9Oss2nWx2f/GFwo8IFx9wvgEcw3CqC+sBRgSaDHb2Fu7u7Ut+vnwtwaOd+wlw4lpl7q6Wq6cbLscZwzP6O6v0fpdP/CLCDHCmE8f23ACuAOeBhS78WYWHDYC7M8vk+wdv59WF043I0GXjdeDSykqKrSwcr4cmLZCxz18mwrUbBpxqFeVn4M6/BFxUuA1MAJNd+ACwIwSY/uIN+t4+Rlgpf2VFi2S5iGLUmPPau+ROKkpeii404uCTFfl83zr/t9aJfSwub1l8okU6CYwC6zvZ56iAV7gn8DGwo1VKAHlQZP7zSUbXtf//AaFX9LL7Nh3cAAAAJXRFWHRkYXRlOmNyZWF0ZQAyMDE2LTA2LTMwVDE3OjE1OjE3KzAxOjAwsKT/BwAAACV0RVh0ZGF0ZTptb2RpZnkAMjAxNi0wNi0zMFQxNzoxNToxNyswMTowMMH5R7sAAAAASUVORK5CYII=";

  @Test
  public void defaultsToWritingBinaryResponseFilesOfAnySize() {
    targetService.stubFor(
        get("/myimage.png").willReturn(aResponse().withBase64Body(IMAGE_CONTENT_BASE64)));

    proxyingService.startRecording(recordSpec().forTarget(targetBaseUrl));

    client.get("/myimage.png");

    List<StubMapping> mappings = proxyingService.stopRecording().getStubMappings();
    StubMapping mapping = mappings.get(0);
    String bodyFileName = mapping.getResponse().getBodyFileName();

    assertThat(bodyFileName, is("myimagepng-" + mapping.getId() + ".png"));
    File bodyFile = new File(fileRoot, "__files/" + bodyFileName);
    assertThat(bodyFile.exists(), is(true));
  }

  @Test
  public void defaultsToWritingTextResponseFilesOver1Kb() {
    targetService.stubFor(
        get("/large.txt")
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE, "text/plain")
                    .withBody(StringUtils.rightPad("", 10241, 'a'))));

    proxyingService.startRecording(recordSpec().forTarget(targetBaseUrl));

    client.get("/large.txt");

    List<StubMapping> mappings = proxyingService.stopRecording().getStubMappings();
    StubMapping mapping = mappings.get(0);
    String bodyFileName = mapping.getResponse().getBodyFileName();

    assertThat(bodyFileName, is("largetxt-" + mapping.getId() + ".txt"));
    File bodyFile = new File(fileRoot, "__files/" + bodyFileName);
    assertThat(bodyFile.exists(), is(true));
  }

  @Test
  public void doesNotWriteTextResponseFilesUnder1KbByDefault() {
    targetService.stubFor(
        get("/small.txt")
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE, "text/plain")
                    .withBody(StringUtils.rightPad("", 10239, 'a'))));

    proxyingService.startRecording(recordSpec().forTarget(targetBaseUrl));

    client.get("/small.txt");

    List<StubMapping> mappings = proxyingService.stopRecording().getStubMappings();
    String bodyFileName = mappings.get(0).getResponse().getBodyFileName();

    assertThat(bodyFileName, nullValue());
  }

  @Test(expected = NotRecordingException.class)
  public void throwsAnErrorIfAttemptingToStopViaStaticRemoteDslWhenNotRecording() {
    stopRecording();
  }

  @Test(expected = NotRecordingException.class)
  public void throwsAnErrorIfAttemptingToStopViaInstanceRemoteDslWhenNotRecording() {
    adminClient.stopStubRecording();
  }

  @Test(expected = NotRecordingException.class)
  public void throwsAnErrorIfAttemptingToStopViaDirectDslWhenNotRecording() {
    proxyingService.stopRecording();
  }

  @Test(expected = InvalidInputException.class)
  public void throwsValidationErrorWhenAttemptingToStartRecordingViaStaticDslWithNoTargetUrl() {
    startRecording(recordSpec());
  }

  @Test(expected = InvalidInputException.class)
  public void throwsValidationErrorWhenAttemptingToStartRecordingViaDirectDslWithNoTargetUrl() {
    proxyingService.startRecording(recordSpec());
  }
}
