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
package com.github.tomakehurst.wiremock.verification;

import static com.github.tomakehurst.wiremock.http.HttpHeader.httpHeader;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.github.tomakehurst.wiremock.testsupport.MockRequestBuilder.aRequest;
import static com.github.tomakehurst.wiremock.verification.LoggedRequest.createFrom;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.common.Dates;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.http.Cookie;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(JMock.class)
public class LoggedRequestTest {

  public static final String REQUEST_BODY = "some text 形声字形聲字";
  public static final String REQUEST_BODY_AS_BASE64 = "c29tZSB0ZXh0IOW9ouWjsOWtl+W9ouiBsuWtlw==";

  private Mockery context;

  @Before
  public void init() {
    context = new Mockery();
    System.out.println(TimeZone.getDefault());
  }

  @Test
  public void headerMatchingIsCaseInsensitive() {
    LoggedRequest loggedRequest =
        createFrom(
            aRequest(context)
                .withUrl("/for/logging")
                .withMethod(POST)
                .withClientIp("14.07.17.89")
                .withBody(REQUEST_BODY)
                .withBodyAsBase64(REQUEST_BODY_AS_BASE64)
                .withHeader("Content-Type", "text/plain")
                .withHeader("ACCEPT", "application/json")
                .build());

    assertTrue(loggedRequest.containsHeader("content-type"));
    assertNotNull(loggedRequest.getHeader("content-type"));
    assertTrue(loggedRequest.containsHeader("CONTENT-TYPE"));
    assertNotNull(loggedRequest.getHeader("CONTENT-TYPE"));
    assertTrue(loggedRequest.containsHeader("Accept"));
    assertNotNull(loggedRequest.getHeader("Accept"));
  }

  static final String DATE = "2012-06-07T16:39:41Z";
  static final String JSON_EXAMPLE =
      "{\n"
          + "      \"url\" : \"/my/url\",\n"
          + "      \"absoluteUrl\" : \"http://mydomain.com/my/url\",\n"
          + "      \"method\" : \"GET\",\n"
          + "      \"clientIp\" : \"25.10.18.11\",\n"
          + "      \"headers\" : {\n"
          + "        \"Accept-Language\" : \"en-us,en;q=0.5\"\n"
          + "      },\n"
          + "      \"cookies\" : {\n"
          + "        \"first_cookie\"   : \"yum\",\n"
          + "        \"monster_cookie\" : \"COOKIIIEESS\"\n"
          + "      },\n"
          + "      \"browserProxyRequest\" : true,\n"
          + "      \"loggedDate\" : %d,\n"
          + "      \"bodyAsBase64\" : \""
          + REQUEST_BODY_AS_BASE64
          + "\",\n"
          + "      \"body\" : \""
          + REQUEST_BODY
          + "\",\n"
          + "      \"loggedDateString\" : \""
          + DATE
          + "\",\n"
          + "    }";

  @Test
  public void jsonRepresentation() throws Exception {
    HttpHeaders headers = new HttpHeaders(httpHeader("Accept-Language", "en-us,en;q=0.5"));
    Map<String, Cookie> cookies =
        ImmutableMap.of(
            "first_cookie", new Cookie("yum"),
            "monster_cookie", new Cookie("COOKIIIEESS"));

    Date loggedDate = Dates.parse(DATE);

    LoggedRequest loggedRequest =
        new LoggedRequest(
            "/my/url",
            "http://mydomain.com/my/url",
            RequestMethod.GET,
            "25.10.18.11",
            headers,
            cookies,
            true,
            loggedDate,
            REQUEST_BODY_AS_BASE64,
            null,
            null);

    String expectedJson = String.format(JSON_EXAMPLE, loggedDate.getTime());

    JSONAssert.assertEquals(expectedJson, Json.write(loggedRequest), false);
  }

  @Test
  public void bodyEncodedAsUTF8() throws Exception {
    LoggedRequest loggedRequest =
        new LoggedRequest(
            "/my/url",
            "http://mydomain.com/my/url",
            RequestMethod.GET,
            null,
            null,
            null,
            true,
            null,
            REQUEST_BODY_AS_BASE64,
            null,
            null);

    assertThat(loggedRequest.getBodyAsString(), is(equalTo(REQUEST_BODY)));
  }

  static final String JSON_PARAMS_EXAMPLE =
      "{\n"
          + "  \"url\" : \"/sample/path?test-param-1=value1&test-param-2=value2\",\n"
          + "  \"absoluteUrl\" : \"http://ex.ample/sample/path?test-param-1=value1&test-param-2=value2\",\n"
          + "  \"method\" : \"GET\",\n"
          + "  \"clientIp\" : \"0.0.0.0\",\n"
          + "  \"browserProxyRequest\" : true,\n"
          + "  \"loggedDate\" : 0,\n"
          + "  \"loggedDateString\" : \"1970-01-01T00:00:00Z\",\n"
          + "  \"queryParams\" : {\n"
          + "    \"test-param-1\" : {\n"
          + "      \"key\" : \"test-param-1\",\n"
          + "      \"values\" : [ \"value-1\" ]\n"
          + "    },\n"
          + "    \"test-param-2\" : {\n"
          + "      \"key\" : \"test-param-2\",\n"
          + "      \"values\" : [ \"value-2\" ]\n"
          + "    }\n"
          + "  }\n"
          + "}";

  @Test
  public void queryParametersAreSerialized() {
    LoggedRequest req =
        new LoggedRequest(
            "/sample/path?test-param-1=value-1&test-param-2=value-2",
            "http://ex.ample/sample/path?test-param-1=value-1&test-param-2=value-2",
            RequestMethod.GET,
            "0.0.0.0",
            null,
            null,
            true,
            new Date(0),
            null,
            null,
            null);

    Map<String, Object> reqMap = Json.objectToMap(req);

    assertTrue(reqMap.containsKey("queryParams"));
    assertEquals(
        "value-1",
        ((List) ((Map) ((Map) reqMap.get("queryParams")).get("test-param-1")).get("values"))
            .get(0));
    assertEquals(
        "value-2",
        ((List) ((Map) ((Map) reqMap.get("queryParams")).get("test-param-2")).get("values"))
            .get(0));
  }

  @Test
  public void queryParametersAreDeserialized() throws IOException {
    LoggedRequest req = new ObjectMapper().readValue(JSON_PARAMS_EXAMPLE, LoggedRequest.class);

    assertEquals("test-param-1", req.queryParameter("test-param-1").key());
    assertEquals("value-1", req.queryParameter("test-param-1").firstValue());

    assertEquals("test-param-2", req.queryParameter("test-param-2").key());
    assertEquals("value-2", req.queryParameter("test-param-2").firstValue());
  }
}
