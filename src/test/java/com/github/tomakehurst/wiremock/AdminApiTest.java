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
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.equalsMultiLine;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.matches;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonPartEquals;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.github.tomakehurst.wiremock.common.Errors;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.common.TextFile;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.http.DelayDistribution;
import com.github.tomakehurst.wiremock.http.UniformDistribution;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.google.common.collect.ImmutableMap;
import com.toomuchcoding.jsonassert.JsonAssertion;
import com.toomuchcoding.jsonassert.JsonVerifiable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.http.entity.StringEntity;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class AdminApiTest extends AcceptanceTestBase {

  static Stubbing dsl = wireMockServer;

  @After
  public void tearDown() throws Exception {
    deleteAllBodyFiles();
  }

  private void deleteAllBodyFiles() throws IOException {
    FileSource filesRoot = wireMockServer.getOptions().filesRoot().child(FILES_ROOT);
    if (filesRoot.exists()) {
      List<TextFile> textFiles = filesRoot.listFilesRecursively();
      for (TextFile textFile : textFiles) {
        Files.delete(Paths.get(textFile.getPath()));
      }
    }
  }

  @Test
  public void getAllStubMappings() throws Exception {
    StubMapping stubMapping =
        dsl.stubFor(get(urlEqualTo("/my-test-url")).willReturn(aResponse().withStatus(418)));

    String body = testClient.get("/__admin/mappings").content();

    JSONAssert.assertEquals(
        "{                                              \n"
            + "  \"mappings\" : [ {                           \n"
            + "    \"id\" : \""
            + stubMapping.getId()
            + "\",  \n"
            + "    \"uuid\" : \""
            + stubMapping.getId()
            + "\",\n"
            + "    \"request\" : {                            \n"
            + "      \"url\" : \"/my-test-url\",              \n"
            + "      \"method\" : \"GET\"                     \n"
            + "    },                                         \n"
            + "    \"response\" : {                           \n"
            + "      \"status\" : 418                         \n"
            + "    }                                          \n"
            + "  } ],                                         \n"
            + "                                               \n"
            + "  \"meta\": {                                  \n"
            + "    \"total\": 1                               \n"
            + "  }                                            \n"
            + "}",
        body,
        true);
  }

  @Test
  public void getAllStubMappingsWithLimitedResults() throws Exception {
    for (int i = 1; i <= 20; i++) {
      dsl.stubFor(get(urlEqualTo("/things/" + i)).willReturn(aResponse().withStatus(418)));
    }

    String allBody = testClient.get("/__admin/mappings").content();
    String limitedBody = testClient.get("/__admin/mappings?limit=7").content();

    JsonAssertion.assertThat(allBody).field("mappings").array().hasSize(20);
    JsonAssertion.assertThat(limitedBody).field("mappings").array().hasSize(7);
  }

  @Test
  public void getAllStubMappingsWithLimitedAndOffsetResults() throws Exception {
    for (int i = 1; i <= 20; i++) {
      dsl.stubFor(get(urlEqualTo("/things/" + i)).willReturn(aResponse().withStatus(418)));
    }

    String limitedBody = testClient.get("/__admin/mappings?limit=4&offset=3").content();

    JsonAssertion.assertThat(limitedBody).field("mappings").array().hasSize(4);
    JsonAssertion.assertThat(limitedBody)
        .field("mappings")
        .elementWithIndex(0)
        .field("request")
        .field("url")
        .isEqualTo("/things/17");
    JsonAssertion.assertThat(limitedBody)
        .field("mappings")
        .elementWithIndex(3)
        .field("request")
        .field("url")
        .isEqualTo("/things/14");
  }

  @Test
  public void deprecatedGetAllStubMappings() throws Exception {
    dsl.stubFor(get(urlEqualTo("/my-test-url")).willReturn(aResponse().withStatus(418)));

    String body = testClient.get("/__admin/").content();
    System.out.println(body);
    JSONAssert.assertEquals(
        "{\n"
            + "  \"mappings\" : [ {\n"
            + "    \"request\" : {\n"
            + "      \"url\" : \"/my-test-url\",\n"
            + "      \"method\" : \"GET\"\n"
            + "    },\n"
            + "    \"response\" : {\n"
            + "      \"status\" : 418\n"
            + "    }\n"
            + "  } ]\n"
            + "}",
        body,
        false);
  }

  @Test
  public void getStubMappingById() throws Exception {
    UUID id = UUID.randomUUID();

    dsl.stubFor(
        trace(urlEqualTo("/my-addressable-stub"))
            .withId(id)
            .willReturn(aResponse().withStatus(451)));

    String body = testClient.get("/__admin/mappings/" + id).content();

    JSONAssert.assertEquals(
        "{                                          \n"
            + "    \"id\": \""
            + id
            + "\",              \n"
            + "    \"uuid\": \""
            + id
            + "\",              \n"
            + "    \"request\" : {                        \n"
            + "      \"url\" : \"/my-addressable-stub\",  \n"
            + "      \"method\" : \"TRACE\"               \n"
            + "    },                                     \n"
            + "    \"response\" : {                       \n"
            + "      \"status\" : 451                     \n"
            + "    }                                      \n"
            + "}",
        body,
        true);
  }

  @Test
  public void getLoggedRequests() throws Exception {
    dsl.stubFor(get(urlPathEqualTo("/received-request/4")).willReturn(aResponse()));

    for (int i = 1; i <= 5; i++) {
      testClient.get("/received-request/" + i);
    }

    String body = testClient.get("/__admin/requests").content();

    System.out.println(body);
    JsonVerifiable check = JsonAssertion.assertThat(body);
    check.field("meta").field("total").isEqualTo(5);
    check
        .field("requests")
        .elementWithIndex(2)
        .field("request")
        .field("url")
        .isEqualTo("/received-request/3");
    check.field("requests").hasSize(5);
    check.field("requests").elementWithIndex(1).field("wasMatched").isEqualTo(true);
    check.field("requests").elementWithIndex(3).field("wasMatched").isEqualTo(false);
  }

  @Test
  public void getLoggedRequestsWithLimit() throws Exception {
    dsl.stubFor(
        get(urlPathEqualTo("/received-request/7"))
            .willReturn(aResponse().withStatus(200).withBody("This was matched")));

    for (int i = 1; i <= 7; i++) {
      testClient.get("/received-request/" + i);
    }

    String body = testClient.get("/__admin/requests?limit=2").content();

    JsonVerifiable check = JsonAssertion.assertThat(body);
    check.field("meta").field("total").isEqualTo(7);
    check
        .field("requests")
        .elementWithIndex(0)
        .field("request")
        .field("url")
        .isEqualTo("/received-request/7");
    check
        .field("requests")
        .elementWithIndex(1)
        .field("request")
        .field("url")
        .isEqualTo("/received-request/6");
    check.field("requests").hasSize(2);
  }

  @Test
  public void getLoggedRequestsWithLimitAndSinceDate() throws Exception {
    for (int i = 1; i <= 5; i++) {
      testClient.get("/received-request/" + i);
    }

    String midPoint = new ISO8601DateFormat().format(new Date());

    for (int i = 6; i <= 9; i++) {
      testClient.get("/received-request/" + i);
    }

    String body = testClient.get("/__admin/requests?since=" + midPoint + "&limit=3").content();

    JsonVerifiable check = JsonAssertion.assertThat(body);
    check.field("meta").field("total").isEqualTo(9);
    check.field("requests").hasSize(3);
    check
        .field("requests")
        .elementWithIndex(0)
        .field("request")
        .field("url")
        .isEqualTo("/received-request/9");
    check
        .field("requests")
        .elementWithIndex(2)
        .field("request")
        .field("url")
        .isEqualTo("/received-request/7");
  }

  @Test
  public void getLoggedRequestsWithLimitLargerThanResults() throws Exception {
    for (int i = 1; i <= 3; i++) {
      testClient.get("/received-request/" + i);
    }

    String body = testClient.get("/__admin/requests?limit=3000").content();

    JsonVerifiable check = JsonAssertion.assertThat(body);
    check.field("meta").field("total").isEqualTo(3);
    check.field("requests").hasSize(3);
  }

  @Test
  public void getLoggedRequestById() throws Exception {
    for (int i = 1; i <= 3; i++) {
      testClient.get("/received-request/" + i);
    }

    List<ServeEvent> serveEvents = dsl.getAllServeEvents();
    UUID servedStubId = serveEvents.get(1).getId();

    WireMockResponse response = testClient.get("/__admin/requests/" + servedStubId);
    String body = response.content();
    System.out.println("BODY:" + body);

    assertThat(response.statusCode(), is(200));
    JsonVerifiable check = JsonAssertion.assertThat(body);
    check.field("id").isEqualTo(servedStubId);
    check.field("request").field("url").isEqualTo("/received-request/2");
  }

  @Test
  public void deleteStubMappingById() throws Exception {
    StubMapping stubMapping =
        dsl.stubFor(get(urlPathEqualTo("/delete/this")).willReturn(aResponse().withStatus(200)));

    assertThat(testClient.get("/delete/this").statusCode(), is(200));

    WireMockResponse response = testClient.delete("/__admin/mappings/" + stubMapping.getId());

    assertThat(response.content(), is("{}"));
    assertThat(testClient.get("/delete/this").statusCode(), is(404));
  }

  @Test
  public void returns404WhenAttemptingToDeleteNonExistentStubMapping() {
    assertThat(testClient.delete("/__admin/mappings/" + UUID.randomUUID()).statusCode(), is(404));
  }

  @Test
  public void editStubMappingById() throws Exception {
    StubMapping stubMapping =
        dsl.stubFor(get(urlPathEqualTo("/put/this")).willReturn(aResponse().withStatus(200)));

    assertThat(testClient.get("/put/this").statusCode(), is(200));

    String requestBody =
        "{                                  \n"
            + "    \"request\": {                 \n"
            + "        \"method\": \"GET\",       \n"
            + "        \"url\": \"/put/this\"     \n"
            + "    },                             \n"
            + "    \"response\": {                \n"
            + "        \"status\": 418            \n"
            + "    }                              \n"
            + "}";

    WireMockResponse response =
        testClient.putWithBody(
            "/__admin/mappings/" + stubMapping.getId(), requestBody, "application/json");

    JSONAssert.assertEquals(requestBody, response.content(), false);
    assertThat(testClient.get("/put/this").statusCode(), is(418));
  }

  @Test
  public void returns404WhenAttemptingToEditNonExistentStubMapping() {
    assertThat(
        testClient
            .putWithBody("/__admin/mappings/" + UUID.randomUUID(), "{}", "application/json")
            .statusCode(),
        is(404));
  }

  @Test
  public void createStubMappingReturnsTheCreatedMapping() {
    WireMockResponse response =
        testClient.postJson(
            "/__admin/mappings",
            "{                                \n"
                + "    \"name\": \"Teapot putter\",   \n"
                + "    \"request\": {                 \n"
                + "        \"method\": \"PUT\",       \n"
                + "        \"url\": \"/put/this\"     \n"
                + "    },                             \n"
                + "    \"response\": {                \n"
                + "        \"status\": 418            \n"
                + "    }                              \n"
                + "}");

    assertThat(response.statusCode(), is(201));
    assertThat(response.firstHeader("Content-Type"), is("application/json"));
    String body = response.content();
    JsonAssertion.assertThat(body).field("id").matches("[a-z0-9\\-]{36}");
    JsonAssertion.assertThat(body).field("name").isEqualTo("Teapot putter");
  }

  @Test
  public void resetStubMappingsViaDELETE() {
    dsl.stubFor(get(urlEqualTo("/reset-this")).willReturn(aResponse().withStatus(200)));
    dsl.stubFor(get(urlEqualTo("/reset-this/too")).willReturn(aResponse().withStatus(200)));

    assertThat(testClient.get("/reset-this").statusCode(), is(200));
    assertThat(testClient.get("/reset-this/too").statusCode(), is(200));

    WireMockResponse response = testClient.delete("/__admin/mappings");

    assertThat(response.content(), is("{}"));
    assertThat(testClient.get("/reset-this").statusCode(), is(404));
    assertThat(testClient.get("/reset-this/too").statusCode(), is(404));
  }

  @Test
  public void resetRequestJournalViaDELETE() {
    testClient.get("/one");
    testClient.get("/two");
    testClient.get("/three");

    assertThat(dsl.getAllServeEvents().size(), is(3));

    WireMockResponse response = testClient.delete("/__admin/requests");

    assertThat(response.firstHeader("Content-Type"), is("application/json"));
    assertThat(response.content(), is("{}"));
    assertThat(response.statusCode(), is(200));
    assertThat(dsl.getAllServeEvents().size(), is(0));
  }

  @Test
  public void resetScenariosViaPOST() {
    dsl.stubFor(
        get(urlEqualTo("/stateful"))
            .inScenario("changing-states")
            .whenScenarioStateIs(STARTED)
            .willSetStateTo("final")
            .willReturn(aResponse().withBody("Initial")));

    dsl.stubFor(
        get(urlEqualTo("/stateful"))
            .inScenario("changing-states")
            .whenScenarioStateIs("final")
            .willReturn(aResponse().withBody("Final")));

    assertThat(testClient.get("/stateful").content(), is("Initial"));
    assertThat(testClient.get("/stateful").content(), is("Final"));

    WireMockResponse response =
        testClient.post("/__admin/scenarios/reset", new StringEntity("", TEXT_PLAIN));

    assertThat(response.content(), is("{}"));
    assertThat(response.firstHeader("Content-Type"), is("application/json"));
    assertThat(testClient.get("/stateful").content(), is("Initial"));
  }

  @Test
  public void defaultsUnspecifiedStubMappingAttributes() {
    WireMockResponse response = testClient.postJson("/__admin/mappings", "{}");

    assertThat(response.statusCode(), is(201));

    String body = response.content();
    JsonAssertion.assertThat(body).field("request").field("method").isEqualTo("ANY");
    JsonAssertion.assertThat(body).field("response").field("status").isEqualTo(200);

    assertThat(testClient.get("/").statusCode(), is(200));
  }

  @Test
  public void returnsBadEntityStatusWhenInvalidRegexUsedInUrl() {
    WireMockResponse response =
        testClient.postJson(
            "/__admin/mappings",
            "{                                      \n"
                + "    \"request\": {                            \n"
                + "        \"urlPattern\": \"/@$&%*[[^^£$&%\"    \n"
                + "    }                                         \n"
                + "}");

    assertThat(response.statusCode(), is(422));

    Errors errors = Json.read(response.content(), Errors.class);
    assertThat(
        errors.first().getDetail(),
        equalsMultiLine(
            "Unclosed character class near index 13\n" + "/@$&%*[[^^£$&%\n" + "             ^"));
    assertThat(errors.first().getSource().getPointer(), is("/request"));
  }

  @Test
  public void returnsBadEntityStatusWhenInvalidRegexUsedInHeader() {
    WireMockResponse response =
        testClient.postJson(
            "/__admin/mappings",
            "{\n"
                + "    \"request\": {\n"
                + "        \"headers\": {\n"
                + "            \"Accept\": {\n"
                + "                \"matches\": \"%[[json[[\"\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}");

    assertThat(response.statusCode(), is(422));

    Errors errors = Json.read(response.content(), Errors.class);
    assertThat(
        errors.first().getDetail(),
        equalsMultiLine("Unclosed character class near index 8\n" + "%[[json[[\n" + "        ^"));
    assertThat(errors.first().getSource().getPointer(), is("/request/headers/Accept"));
  }

  @Test
  public void returnsBadEntityStatusWhenInvalidRegexUsedInBodyPattern() {
    WireMockResponse response =
        testClient.postJson(
            "/__admin/mappings",
            "{\n"
                + "    \"request\": {\n"
                + "        \"bodyPatterns\": [\n"
                + "            {\n"
                + "                \"equalTo\": \"fine\"\n"
                + "            },\n"
                + "            {\n"
                + "                \"matches\": \"somebad]]][[stuff\"\n"
                + "            }\n"
                + "        ]\n"
                + "    }\n"
                + "}");

    assertThat(response.statusCode(), is(422));

    Errors errors = Json.read(response.content(), Errors.class);
    assertThat(errors.first().getSource().getPointer(), is("/request/bodyPatterns/1"));
    assertThat(errors.first().getTitle(), is("Error parsing JSON"));
    assertThat(
        errors.first().getDetail(),
        equalsMultiLine(
            "Unclosed character class near index 16\n"
                + "somebad]]][[stuff\n"
                + "                ^"));
  }

  @Test
  public void returnsBadEntityStatusWhenInvalidMatchOperator() {
    WireMockResponse response =
        testClient.postJson(
            "/__admin/mappings",
            "{\n"
                + "    \"request\": {\n"
                + "        \"bodyPatterns\": [\n"
                + "            {\n"
                + "                \"matching\": \"somebad]]][[stuff\"\n"
                + "            }\n"
                + "        ]\n"
                + "    }\n"
                + "}");

    assertThat(response.statusCode(), is(422));

    Errors errors = Json.read(response.content(), Errors.class);
    assertThat(errors.first().getSource().getPointer(), is("/request/bodyPatterns/0"));
    assertThat(
        errors.first().getDetail(),
        is("{\"matching\":\"somebad]]][[stuff\"} is not a valid match operation"));
  }

  @Test
  public void returnsBadEntityStatusWhenInvalidMatchOperatorManyBodyPatterns() {
    WireMockResponse response =
        testClient.postJson(
            "/__admin/mappings",
            "{\n"
                + "    \"request\": {\n"
                + "        \"bodyPatterns\": [\n"
                + "            {\n"
                + "                \"equalTo\": \"fine\"\n"
                + "            },\n"
                + "            {\n"
                + "                \"matching\": \"somebad]]][[stuff\"\n"
                + "            }\n"
                + "        ]\n"
                + "    }\n"
                + "}");

    assertThat(response.statusCode(), is(422));

    Errors errors = Json.read(response.content(), Errors.class);
    assertThat(errors.first().getSource().getPointer(), is("/request/bodyPatterns/1"));
    assertThat(
        errors.first().getDetail(),
        is("{\"matching\":\"somebad]]][[stuff\"} is not a valid match operation"));
  }

  @Test
  public void returnsBadEntityStatusOnEqualToJsonOperand() {
    WireMockResponse response =
        testClient.postJson(
            "/__admin/mappings",
            "{\n"
                + "    \"request\": {\n"
                + "        \"bodyPatterns\": [\n"
                + "            {\n"
                + "                \"equalToJson\": \"(wrong)\"\n"
                + "            }\n"
                + "        ]\n"
                + "    }\n"
                + "}");

    assertThat(response.statusCode(), is(422));

    Errors errors = Json.read(response.content(), Errors.class);
    assertThat(errors.first().getSource().getPointer(), is("/request/bodyPatterns/0"));
    assertThat(
        errors.first().getDetail(),
        allOf(
            containsString(
                "Unexpected character ('(' (code 40)): expected a valid value (JSON String, Number, Array, Object or token 'null', 'true' or 'false')"),
            containsString("line: 1, column: 2")));
  }

  @Test
  public void returnsBadEntityStatusWhenInvalidEqualToXmlSpecified() {
    WireMockResponse response =
        testClient.postJson(
            "/__admin/mappings",
            "{\n"
                + "    \"request\": {\n"
                + "        \"bodyPatterns\": [\n"
                + "            {\n"
                + "                \"equalToXml\": \"(wrong)\"\n"
                + "            }\n"
                + "        ]\n"
                + "    }\n"
                + "}");

    assertThat(response.statusCode(), is(422));

    Errors errors = Json.read(response.content(), Errors.class);
    assertThat(errors.first().getSource().getPointer(), is("/request/bodyPatterns/0"));
    assertThat(errors.first().getTitle(), is("Error parsing JSON"));
    assertThat(
        errors.first().getDetail(), is("Content is not allowed in prolog.; line 1; column 1"));
  }

  @Test
  public void returnsBadEntityStatusWhenContainsOperandIsNull() {
    WireMockResponse response =
        testClient.postJson(
            "/__admin/mappings",
            "{\n"
                + "    \"request\": {\n"
                + "        \"bodyPatterns\": [\n"
                + "            {\n"
                + "                \"contains\": null\n"
                + "            }\n"
                + "        ]\n"
                + "    }\n"
                + "}");

    assertThat(response.statusCode(), is(422));

    Errors errors = Json.read(response.content(), Errors.class);
    assertThat(errors.first().getSource().getPointer(), is("/request/bodyPatterns/0"));
    assertThat(errors.first().getTitle(), is("Error parsing JSON"));
    assertThat(errors.first().getDetail(), is("contains operand must be a non-null string"));
  }

  @Test
  public void returnsBadEntityStatusWhenEqualToOperandIsWrongType() {
    WireMockResponse response =
        testClient.postJson(
            "/__admin/mappings",
            "{\n"
                + "    \"request\": {\n"
                + "        \"bodyPatterns\": [\n"
                + "            {\n"
                + "                \"equalTo\": 12\n"
                + "            }\n"
                + "        ]\n"
                + "    }\n"
                + "}");

    assertThat(response.statusCode(), is(422));

    Errors errors = Json.read(response.content(), Errors.class);
    assertThat(errors.first().getSource().getPointer(), is("/request/bodyPatterns/0"));
    assertThat(errors.first().getTitle(), is("Error parsing JSON"));
    assertThat(errors.first().getDetail(), is("equalTo operand must be a non-null string"));
  }

  @Test
  public void returnsBadEntityStatusWhenContainsOperandIsWrongType() {
    WireMockResponse response =
        testClient.postJson(
            "/__admin/mappings",
            "{\n"
                + "    \"request\": {\n"
                + "        \"bodyPatterns\": [\n"
                + "            {\n"
                + "                \"contains\": 12\n"
                + "            }\n"
                + "        ]\n"
                + "    }\n"
                + "}");

    assertThat(response.statusCode(), is(422));

    Errors errors = Json.read(response.content(), Errors.class);
    assertThat(errors.first().getSource().getPointer(), is("/request/bodyPatterns/0"));
    assertThat(errors.first().getTitle(), is("Error parsing JSON"));
    assertThat(errors.first().getDetail(), is("contains operand must be a non-null string"));
  }

  @Test
  public void returnsBadEntityStatusWhenMatchesOperandIsWrongType() {
    WireMockResponse response =
        testClient.postJson(
            "/__admin/mappings",
            "{\n"
                + "    \"request\": {\n"
                + "        \"bodyPatterns\": [\n"
                + "            {\n"
                + "                \"matches\": 12\n"
                + "            }\n"
                + "        ]\n"
                + "    }\n"
                + "}");

    assertThat(response.statusCode(), is(422));

    Errors errors = Json.read(response.content(), Errors.class);
    assertThat(errors.first().getSource().getPointer(), is("/request/bodyPatterns/0"));
    assertThat(errors.first().getTitle(), is("Error parsing JSON"));
    assertThat(errors.first().getDetail(), is("matches operand must be a non-null string"));
  }

  @Test
  public void servesSwaggerSpec() {
    WireMockResponse response = testClient.get("/__admin/docs/swagger");
    assertThat(response.statusCode(), is(200));
    assertThat(response.content(), containsString("\"openapi\": \"3.0.0\""));
  }

  @Test
  public void servesSwaggerUiHtml() {
    WireMockResponse response = testClient.get("/__admin/swagger-ui/");
    assertThat(response.statusCode(), is(200));
  }

  @Test
  public void servesDocIndex() {
    WireMockResponse response = testClient.get("/__admin/docs");
    assertThat(response.statusCode(), is(200));
    assertThat(response.content(), containsString("<html"));
  }

  @Test
  public void deleteStubFile() throws Exception {
    String fileName = "bar.txt";
    FileSource fileSource = wireMockServer.getOptions().filesRoot().child(FILES_ROOT);
    fileSource.createIfNecessary();
    fileSource.writeTextFile(fileName, "contents");

    int statusCode = testClient.delete("/__admin/files/bar.txt").statusCode();

    assertEquals(200, statusCode);
    assertFalse(
        "File should have been deleted",
        Paths.get(fileSource.getTextFileNamed(fileName).getPath()).toFile().exists());
  }

  @Test
  public void editStubFileContent() throws Exception {
    String fileName = "bar.txt";
    FileSource fileSource = wireMockServer.getOptions().filesRoot().child(FILES_ROOT);
    fileSource.createIfNecessary();
    fileSource.writeTextFile(fileName, "AAA");

    int statusCode =
        testClient.putWithBody("/__admin/files/bar.txt", "BBB", "text/plain").statusCode();

    assertEquals(200, statusCode);
    assertEquals(
        "File should have been changed",
        "BBB",
        fileSource.getTextFileNamed(fileName).readContentsAsString());
  }

  @Test
  public void listStubFiles() throws Exception {
    FileSource fileSource = wireMockServer.getOptions().filesRoot().child(FILES_ROOT);
    fileSource.createIfNecessary();
    fileSource.writeTextFile("bar.txt", "contents");
    fileSource.writeTextFile("zoo.txt", "contents");

    WireMockResponse response = testClient.get("/__admin/files");

    assertEquals(200, response.statusCode());
    String pathSeparatorRegex = File.separator;
    if (File.separator.equals("\\")) {
      pathSeparatorRegex = "\\\\";
    }
    assertThat(
        new String(response.binaryContent()),
        matches("\\[ \".*" + pathSeparatorRegex + "bar.txt\", \".*zoo.*txt\" ]"));
  }

  @Test
  public void fetchStubWithMetadata() {
    UUID id = UUID.randomUUID();
    wireMockServer.stubFor(
        get("/with-metadata")
            .withId(id)
            .withMetadata(
                ImmutableMap.<String, Object>of(
                    "one",
                    1,
                    "two",
                    "2",
                    "three",
                    true,
                    "four",
                    ImmutableMap.of("five", "55555"))));

    WireMockResponse response = testClient.get("/__admin/mappings/" + id);

    JsonAssertion.assertThat(response.content()).field("metadata").field("one").isEqualTo(1);
    JsonAssertion.assertThat(response.content()).field("metadata").field("two").isEqualTo("2");
    JsonAssertion.assertThat(response.content()).field("metadata").field("three").isEqualTo(true);
    JsonAssertion.assertThat(response.content())
        .field("metadata")
        .field("four")
        .field("five")
        .isEqualTo("55555");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void stubMetadataIsAbsentWhenNoneSpecified() {
    UUID id = UUID.randomUUID();
    wireMockServer.stubFor(get("/without-metadata").withId(id));

    WireMockResponse response = testClient.get("/__admin/mappings/" + id);
    Map<String, ?> data = Json.read(response.content(), Map.class);

    assertThat(data, not(hasKey("metadata")));
  }

  static final String IMPORT_JSON =
      "{\n"
          + "  \"mappings\": [\n"
          + "    {\n"
          + "      \"request\": {\n"
          + "        \"method\": \"GET\",\n"
          + "        \"url\": \"/one\"\n"
          + "      },\n"
          + "      \"response\": {\n"
          + "        \"status\": 200\n"
          + "      }\n"
          + "    },\n"
          + "    {\n"
          + "      \"id\": \"8c5db8b0-2db4-4ad7-a99f-38c9b00da3f7\",\n"
          + "      \"request\": {\n"
          + "        \"url\": \"/two\"\n"
          + "      },\n"
          + "      \"response\": {\n"
          + "        \"body\": \"Updated\"\n"
          + "      }\n"
          + "    }\n"
          + "  ],\n"
          + "  \n"
          + "  \"importOptions\": {\n"
          + "    \"duplicatePolicy\": \"IGNORE\",\n"
          + "    \"deleteAllNotInImport\": true\n"
          + "  }\n"
          + "}";

  @Test
  public void importStubs() {
    UUID id2 = UUID.fromString("8c5db8b0-2db4-4ad7-a99f-38c9b00da3f7");
    wm.stubFor(get("/two").withId(id2).willReturn(ok("Original")));
    wm.stubFor(get("/three").willReturn(ok()));

    testClient.postJson("/__admin/mappings/import", IMPORT_JSON);

    List<StubMapping> stubs = wireMockServer.listAllStubMappings().getMappings();
    assertThat(stubs.get(1).getResponse().getBody(), is("Original"));
    assertThat(stubs.size(), is(2));
  }

  static final String EMPTY_ID_IMPORT_JSON =
      "{\n"
          + "  \"mappings\": [\n"
          + "    {\n"
          + "      \"id\": \"\",\n"
          + "      \"name\": \"Empty ID\",\n"
          + "      \"request\": {\n"
          + "        \"url\": \"/empty-id\"\n"
          + "      },\n"
          + "      \"response\": {\n"
          + "        \"status\": 204\n"
          + "      }\n"
          + "    },\n"
          + "    {\n"
          + "      \"id\": null,\n"
          + "      \"name\": \"Null ID\",\n"
          + "      \"request\": {\n"
          + "        \"url\": \"/null-id\"\n"
          + "      },\n"
          + "      \"response\": {\n"
          + "        \"status\": 204\n"
          + "      }\n"
          + "    }\n"
          + "  ]\n"
          + "}";

  @Test
  public void treatsEmptyOrNullIdFieldsAsNotPresent() {
    WireMockResponse response =
        testClient.postJson("/__admin/mappings/import", EMPTY_ID_IMPORT_JSON);
    assertThat(response.statusCode(), is(200));

    List<StubMapping> stubs = wireMockServer.listAllStubMappings().getMappings();
    assertThat(stubs, everyItem(hasIdAndUuid()));
  }

  static final String EMPTY_UUID_IMPORT_JSON =
      "{\n"
          + "  \"mappings\": [\n"
          + "    {\n"
          + "      \"id\": \"27d7818b-4df6-4630-a6ab-c50e87e384e1\",\n"
          + "      \"uuid\": \"\",\n"
          + "      \"name\": \"Empty UUID\",\n"
          + "      \"request\": {\n"
          + "        \"url\": \"/empty-id\"\n"
          + "      },\n"
          + "      \"response\": {\n"
          + "        \"status\": 204\n"
          + "      }\n"
          + "    },\n"
          + "    {\n"
          + "      \"id\": \"95b5c478-eb39-4bad-ba55-a336dbfeaa53\",\n"
          + "      \"uuid\": null,\n"
          + "      \"name\": \"Null ID\",\n"
          + "      \"request\": {\n"
          + "        \"url\": \"/null-id\"\n"
          + "      },\n"
          + "      \"response\": {\n"
          + "        \"status\": 204\n"
          + "      }\n"
          + "    }\n"
          + "  ]\n"
          + "}";

  @Test
  public void treatsEmptyOrNullUuidFieldsAsNotPresent() {
    WireMockResponse response =
        testClient.postJson("/__admin/mappings/import", EMPTY_UUID_IMPORT_JSON);
    assertThat(response.statusCode(), is(200));

    List<StubMapping> stubs = wireMockServer.listAllStubMappings().getMappings();
    assertThat(stubs, everyItem(hasIdAndUuid()));
  }

  private static final Matcher<StubMapping> hasIdAndUuid() {
    return new TypeSafeMatcher<StubMapping>() {
      @Override
      protected boolean matchesSafely(StubMapping stub) {
        return stub.getId() != null && stub.getUuid() != null;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a stub with a non-null ID and UUID");
      }
    };
  }

  final String SETTINGS_JSON =
      "{\n" + "  \"extended\": {\n" + "    \"mySetting\": 123\n" + "  }\n" + "}";

  @Test
  public void updateGlobalSettingsViaPut() {
    WireMockResponse response =
        testClient.putWithBody("/__admin/settings", SETTINGS_JSON, "application/json");

    assertThat(response.statusCode(), is(200));
    assertThat(
        wireMockServer.getGlobalSettings().getSettings().getExtended().getInt("mySetting"),
        is(123));
  }

  final String WRAPPED_SETTINGS_JSON =
      "{\n"
          + "  \"settings\": {\n"
          + "    \"delayDistribution\": {\n"
          + "      \"type\": \"uniform\",\n"
          + "      \"lower\": 100,\n"
          + "      \"upper\": 300\n"
          + "    },\n"
          + "\n"
          + "    \"extended\": {\n"
          + "      \"one\": 1,\n"
          + "      \"two\": {\n"
          + "        \"name\": \"abc\"\n"
          + "      }\n"
          + "    }\n"
          + "  }\n"
          + "}";

  @Test
  public void updateGlobalSettingsViaPutWithWrapper() {
    WireMockResponse response =
        testClient.putWithBody("/__admin/settings", WRAPPED_SETTINGS_JSON, "application/json");

    assertThat(response.statusCode(), is(200));

    GlobalSettings settings = wireMockServer.getGlobalSettings().getSettings();
    assertThat(
        settings.getDelayDistribution(),
        Matchers.<DelayDistribution>instanceOf(UniformDistribution.class));
    assertThat(settings.getExtended().getInt("one"), is(1));
    assertThat(
        settings.getExtended().getMetadata("two").as(TestExtendedSettingsData.class).name,
        is("abc"));
  }

  final String EXTENDED_JSON =
      "{\n" + "  \"extended\": {\n" + "    \"one\": 11,\n" + "    \"three\": 3\n" + "  }\n" + "}";

  @Test
  public void patchExtendedGlobalSettings() {
    wireMockServer.updateGlobalSettings(
        GlobalSettings.builder().extended(Parameters.one("two", 2)).build());

    WireMockResponse response =
        testClient.patchWithBody("/__admin/settings/extended", EXTENDED_JSON, "application/json");
    assertThat(response.statusCode(), is(200));

    Parameters extended = wireMockServer.getGlobalSettings().getSettings().getExtended();
    assertThat(extended.getInt("one"), is(11));
    assertThat(extended.getInt("two"), is(2));
    assertThat(extended.getInt("three"), is(3));
  }

  static final String STUB_IMPORT_JSON =
      "{\n"
          + "  \"mappings\": [\n"
          + "    {\n"
          + "      \"request\": {\n"
          + "        \"url\": \"/one\",\n"
          + "        \"method\": \"GET\"\n"
          + "      },\n"
          + "      \"response\": {\n"
          + "        \"status\": 200\n"
          + "      }\n"
          + "    },\n"
          + "    {\n"
          + "      \"request\": {\n"
          + "        \"url\": \"/two\",\n"
          + "        \"method\": \"GET\"\n"
          + "      },\n"
          + "      \"response\": {\n"
          + "        \"status\": 200\n"
          + "      }\n"
          + "    }\n"
          + "  ],\n"
          + "  \"meta\" : {\n"
          + "    \"total\" : 2\n"
          + "  }\n"
          + "}";

  @Test
  public void importMultipleStubsWithDefaultParameters() {
    WireMockResponse response = testClient.postJson("/__admin/mappings/import", STUB_IMPORT_JSON);

    assertThat(response.statusCode(), is(200));

    List<StubMapping> allStubs = wm.getStubMappings();
    assertThat(allStubs.size(), is(2));
    assertThat(allStubs.get(0).getRequest().getUrl(), is("/one"));
    assertThat(allStubs.get(1).getRequest().getUrl(), is("/two"));
  }

  @Test
  public void findsNearMissesByRequest() {
    wm.stubFor(post("/things").willReturn(ok()));
    testClient.postJson("/anything", "{}");

    String nearMissRequestJson =
        "{\n" + "  \"method\": \"GET\",\n" + "  \"url\": \"/thing\"\n" + "}";
    WireMockResponse response =
        testClient.postJson("/__admin/near-misses/request", nearMissRequestJson);

    assertThat(response.statusCode(), is(200));
    assertThat(response.content(), jsonPartEquals("nearMisses[0].request.url", "/thing"));
  }

  public static class TestExtendedSettingsData {
    public String name;
  }
}
