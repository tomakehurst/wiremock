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
/*
 * Copyright (C) 2017 Arjan Duijzer
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
package com.github.tomakehurst.wiremock.matching;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.testsupport.WireMatchers;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class MultipartValuePatternTest {

  @Test
  public void deserialisesCorrectlyWhenNoBodyOrHeaderMatchersPresent() {
    String serializedPattern = "{\n" + "    \"matchingType\": \"ANY\"" + "}";

    MultipartValuePattern pattern = Json.read(serializedPattern, MultipartValuePattern.class);
    assertTrue(pattern.isMatchAny());
    assertFalse(pattern.isMatchAll());
  }

  @Test
  public void deserialisesCorrectlyWithTypeAllAndSingleHeaderMatcher() {
    String serializedPattern =
        "{                                           \n"
            + "    \"matchingType\": \"ALL\",              \n"
            + "    \"headers\": {                          \n"
            + "        \"Content-Disposition\": {\n        \n"
            + "            \"contains\": \"name=\\\"part1\\\"\"\n"
            + "        }\n"
            + "    }"
            + "}";

    MultipartValuePattern pattern = Json.read(serializedPattern, MultipartValuePattern.class);

    StringValuePattern headerPattern =
        pattern.getHeaders().get("Content-Disposition").getValuePattern();
    assertThat(headerPattern, instanceOf(ContainsPattern.class));
    assertThat(headerPattern.getValue(), is("name=\"part1\""));

    assertNull(pattern.getBodyPatterns());
    assertTrue(pattern.isMatchAll());
    assertFalse(pattern.isMatchAny());
  }

  @Test
  public void deserialisesCorrectlyWithSingleJsonBodyMatcer() throws JSONException {
    String expectedJson = "{ \"someKey\": \"someValue\" }";
    String serializedPattern =
        "{                                                \n"
            + "    \"matchingType\": \"ANY\",                   \n"
            + "    \"bodyPatterns\": [                          \n"
            + "        { \"equalToJson\": "
            + expectedJson
            + " }\n"
            + "    ]\n"
            + "}";

    MultipartValuePattern pattern = Json.read(serializedPattern, MultipartValuePattern.class);

    JSONAssert.assertEquals(pattern.getBodyPatterns().get(0).getExpected(), expectedJson, false);

    assertNull(pattern.getHeaders());
    assertTrue(pattern.isMatchAny());
    assertFalse(pattern.isMatchAll());
  }

  @Test
  public void deserialisesCorrectlyWithANYMatchTypeWithMultipleHeaderAndBodyMatchers()
      throws JSONException {
    String expectedJson = "{ \"someKey\": \"someValue\" }";
    String serializedPattern =
        "{\n"
            + "    \"matchingType\": \"ANY\",\n"
            + "    \"headers\": {\n"
            + "        \"Content-Disposition\": \n"
            + "            {\n"
            + "                \"contains\": \"name=\\\"part1\\\"\"\n"
            + "            }\n"
            + "        ,\n"
            + "        \"Content-Type\": \n"
            + "            {\n"
            + "                \"contains\": \"application/json\"\n"
            + "            }"
            + "        \n"
            + "    },\n"
            + "    \"bodyPatterns\": [\n"
            + "        {\n"
            + "            \"equalToJson\": "
            + expectedJson
            + "\n"
            + "        }\n"
            + "    ]\n"
            + "}";

    MultipartValuePattern pattern = Json.read(serializedPattern, MultipartValuePattern.class);

    assertThat(
        pattern.getBodyPatterns().get(0).getExpected(), WireMatchers.equalToJson(expectedJson));

    MultiValuePattern contentTypeHeaderPattern = pattern.getHeaders().get("Content-Type");
    assertThat(contentTypeHeaderPattern.getValuePattern(), instanceOf(ContainsPattern.class));
    assertThat(contentTypeHeaderPattern.getValuePattern().getExpected(), is("application/json"));
    assertTrue(pattern.isMatchAny());
    assertFalse(pattern.isMatchAll());
  }

  @Test
  public void deserialisesCorrectlyWithHeadersAndBinaryBody() {
    String expectedBinary = "RG9jdW1lbnQgYm9keSBjb250ZW50cw==";
    String serializedPattern =
        "{\n"
            + "    \"name\": \"my_part_name\",\n"
            + "    \"matchingType\": \"ALL\",\n"
            + "    \"headers\": {\n"
            + "        \"Content-Disposition\": \n"
            + "            {\n"
            + "                \"contains\": \"name=\\\"file\\\"\"\n"
            + "            }\n"
            + "        ,\n"
            + "        \"Content-Type\": \n"
            + "            {\n"
            + "                \"equalTo\": \"application/octet-stream\"\n"
            + "            }\n"
            + "        \n"
            + "    },\n"
            + "    \"bodyPatterns\": [\n"
            + "        {\n"
            + "            \"binaryEqualTo\": \""
            + expectedBinary
            + "\"\n"
            + "        }\n"
            + "    ]\n"
            + "}";

    MultipartValuePattern pattern = Json.read(serializedPattern, MultipartValuePattern.class);

    assertThat(pattern.getName(), is("my_part_name"));
    assertEquals(pattern.getBodyPatterns().get(0).getExpected(), expectedBinary);
    assertThat(
        pattern.getHeaders().get("Content-Type").getValuePattern().getExpected(),
        is("application/octet-stream"));
    assertTrue(pattern.isMatchAll());
    assertFalse(pattern.isMatchAny());
  }

  @Test
  public void serialisesCorrectlyWithMultipleHeaderAndBodyMatchers() {
    MultipartValuePattern pattern =
        aMultipart()
            .withName("title")
            .withHeader("X-First-Header", equalTo("One"))
            .withHeader("X-Second-Header", matching(".*2"))
            .withBody(equalToJson("{ \"thing\": 123 }"))
            .build();

    String json = Json.write(pattern);

    assertThat(
        json,
        WireMatchers.equalToJson(
            "{\n"
                + "  \"name\" : \"title\",\n"
                + "  \"matchingType\" : \"ANY\",\n"
                + "  \"headers\" : {\n"
                + "    \"Content-Disposition\" : {\n"
                + "      \"contains\" : \"name=\\\"title\\\"\"\n"
                + "    },\n"
                + "    \"X-First-Header\" : {\n"
                + "      \"equalTo\" : \"One\"\n"
                + "    },\n"
                + "    \"X-Second-Header\" : {\n"
                + "      \"matches\" : \".*2\"\n"
                + "    }\n"
                + "  },\n"
                + "  \"bodyPatterns\" : [ {\n"
                + "    \"equalToJson\" : \"{ \\\"thing\\\": 123 }\"\n"
                + "  } ]\n"
                + "}"));
  }
}
