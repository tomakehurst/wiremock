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

import static com.github.tomakehurst.wiremock.testsupport.MappingJsonSamples.BINARY_COMPRESSED_CONTENT;
import static com.github.tomakehurst.wiremock.testsupport.MappingJsonSamples.MAPPING_REQUEST_FOR_BINARY_BYTE_BODY;
import static com.github.tomakehurst.wiremock.testsupport.MappingJsonSamples.MAPPING_REQUEST_FOR_BYTE_BODY;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import com.github.tomakehurst.wiremock.testsupport.MappingJsonSamples;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import org.junit.BeforeClass;
import org.junit.Test;

public class MappingsAcceptanceTest extends AcceptanceTestBase {

  @BeforeClass
  public static void setupServer() {
    setupServerWithMappingsInFileRoot();
  }

  @Test
  public void basicMappingCheckNonUtf8() {
    testClient.addResponse(MappingJsonSamples.MAPPING_REQUEST_FOR_NON_UTF8, "GB2312");

    WireMockResponse response = testClient.get("/test/nonutf8/");

    assertThat(response.statusCode(), is(200));
    assertThat(response.content(), is("国家标准"));
  }

  @Test
  public void basicMappingCheckCharsetMismatch() {
    testClient.addResponse(MappingJsonSamples.MAPPING_REQUEST_FOR_NON_UTF8, "ISO-8859-8");

    WireMockResponse response = testClient.get("/test/nonutf8/");

    assertThat(response.statusCode(), is(200));
    assertThat(response.content(), is("????")); // charset in request doesn't match body content
  }

  @Test
  public void basicMappingWithExactUrlAndMethodMatchIsCreatedAndReturned() {
    testClient.addResponse(MappingJsonSamples.BASIC_MAPPING_REQUEST_WITH_RESPONSE_HEADER);

    WireMockResponse response = testClient.get("/a/registered/resource");

    assertThat(response.statusCode(), is(401));
    assertThat(response.content(), is("Not allowed!"));
    assertThat(response.firstHeader("Content-Type"), is("text/plain"));
  }

  @Test
  public void mappingWithStatusOnlyResponseIsCreatedAndReturned() {
    testClient.addResponse(MappingJsonSamples.STATUS_ONLY_MAPPING_REQUEST);

    WireMockResponse response = testClient.put("/status/only");

    assertThat(response.statusCode(), is(204));
    assertNull(response.content());
  }

  @Test
  public void notFoundResponseIsReturnedForUnregisteredUrl() {
    WireMockResponse response = testClient.get("/non-existent/resource");
    assertThat(response.statusCode(), is(HTTP_NOT_FOUND));
  }

  @Test
  public void multipleMappingsSupported() {
    add200ResponseFor("/resource/1");
    add200ResponseFor("/resource/2");
    add200ResponseFor("/resource/3");

    getResponseAndAssert200Status("/resource/1");
    getResponseAndAssert200Status("/resource/2");
    getResponseAndAssert200Status("/resource/3");
  }

  @Test
  public void multipleInvocationsSupported() {
    add200ResponseFor("/resource/100");
    getResponseAndAssert200Status("/resource/100");
    getResponseAndAssert200Status("/resource/100");
    getResponseAndAssert200Status("/resource/100");
  }

  @Test
  public void loadsDefaultMappingsOnStart() {
    getResponseAndAssert200Status("/testmapping");
  }

  @Test
  public void resetToDefaultRemovesAllButDefault() {
    add200ResponseFor("/resource/11");

    testClient.resetDefaultMappings();

    getResponseAndAssert404Status("/resource/11");
    getResponseAndAssert200Status("/testmapping");
  }

  @Test
  public void resetToDefaultRestoresOldMeaningOfDefault() {
    add200ResponseFor("/testmapping");
    WireMockResponse response1 = testClient.get("/testmapping");
    assertThat(response1.content(), is(""));

    testClient.resetDefaultMappings();

    WireMockResponse response2 = testClient.get("/testmapping");
    assertThat(response2.content(), is("default test mapping"));
  }

  @Test
  public void readsMapppingForByteBody() {
    testClient.addResponse(MAPPING_REQUEST_FOR_BYTE_BODY);
    assertThat(testClient.get("/byte/resource/from/file").content(), is("ABC"));
  }

  @Test
  public void readsMapppingForByteBodyReturnsByteArray() {
    testClient.addResponse(MAPPING_REQUEST_FOR_BINARY_BYTE_BODY);
    assertThat(
        testClient.get("/bytecompressed/resource/from/file").binaryContent(),
        is(BINARY_COMPRESSED_CONTENT));
  }

  @Test
  public void readsJsonMapping() {
    WireMockResponse response = testClient.get("/testjsonmapping");
    assertThat(response.statusCode(), is(200));
    assertThat(response.content(), is("{\"key\":\"value\",\"array\":[1,2,3]}"));
  }

  @Test
  public void appendsTransferEncodingHeaderIfNoContentLengthHeaderIsPresentInMapping()
      throws Exception {
    testClient.addResponse(
        "{ 													\n"
            + "	\"request\": {									\n"
            + "		\"method\": \"GET\",						\n"
            + "		\"url\": \"/with/body\"						\n"
            + "	},												\n"
            + "	\"response\": {									\n"
            + "		\"status\": 200,							\n"
            + "		\"body\": \"Some content\"					\n"
            + "	}												\n"
            + "}													");

    WireMockResponse response = testClient.get("/with/body");

    assertThat(response.firstHeader("Transfer-Encoding"), is("chunked"));
  }

  @Test
  public void responseContainsContentLengthAndChunkedEncodingHeadersIfItIsDefinedInTheMapping()
      throws Exception {
    testClient.addResponse(
        "{ 													\n"
            + "	\"request\": {									\n"
            + "		\"method\": \"GET\",						\n"
            + "		\"url\": \"/with/body\"						\n"
            + "	},												\n"
            + "	\"response\": {									\n"
            + "		\"status\": 200,							\n"
            + "		\"headers\": {								\n"
            + "			\"Content-Length\": \"12\"		        \n"
            + "		},											\n"
            + "		\"body\": \"Some content\"					\n"
            + "	}												\n"
            + "}													");
    WireMockResponse response = testClient.get("/with/body");

    assertThat(response.firstHeader("Content-Length"), is("12"));
    assertFalse(
        "expected Transfer-Encoding head to be absent",
        response.headers().containsKey("Transfer-Encoding"));
  }

  private void getResponseAndAssert200Status(String url) {
    WireMockResponse response = testClient.get(url);
    assertThat(response.statusCode(), is(200));
  }

  private void getResponseAndAssert404Status(String url) {
    WireMockResponse response = testClient.get(url);
    assertThat(response.statusCode(), is(404));
  }

  private void add200ResponseFor(String url) {
    testClient.addResponse(String.format(MappingJsonSamples.STATUS_ONLY_GET_MAPPING_TEMPLATE, url));
  }
}
