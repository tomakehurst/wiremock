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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import org.junit.Test;

public class EditMappingAcceptanceTest extends AcceptanceTestBase {

  public static final String MAPPING_REQUEST_WITH_UUID =
      "{ 	"
          + "	\"uuid\":\"bff18359-a74e-4c3e-95f0-dab304cd3a5a\",	\n"
          + "	\"request\": {										\n"
          + "		\"method\": \"GET\",							\n"
          + "		\"url\": \"/a/registered/resource\"				\n"
          + "	},													\n"
          + "	\"response\": {										\n"
          + "		\"status\": 401,								\n"
          + "		\"headers\": {									\n"
          + "			\"Content-Type\": \"text/plain\"			\n"
          + "		},												\n"
          + "		\"body\": \"Not allowed!\"						\n"
          + "	}													\n"
          + "}														";

  public static final String MODIFY_MAPPING_REQUEST_WITH_UUID =
      "{ 	"
          + "	\"uuid\":\"bff18359-a74e-4c3e-95f0-dab304cd3a5a\",	\n"
          + "	\"request\": {										\n"
          + "		\"method\": \"GET\",							\n"
          + "		\"url\": \"/a/registered/resource\"				\n"
          + "	},													\n"
          + "	\"response\": {										\n"
          + "		\"status\": 200,								\n"
          + "		\"headers\": {									\n"
          + "			\"Content-Type\": \"text/html\"				\n"
          + "		},												\n"
          + "		\"body\": \"OK\"								\n"
          + "	}													\n"
          + "}														";

  @Test
  public void editMappingViaTheJsonApi() {

    testClient.addResponse(MAPPING_REQUEST_WITH_UUID);
    WireMockResponse response = testClient.get("/a/registered/resource");

    assertThat(response.statusCode(), is(401));
    assertThat(response.content(), is("Not allowed!"));
    assertThat(response.firstHeader("Content-Type"), is("text/plain"));

    testClient.editMapping(MODIFY_MAPPING_REQUEST_WITH_UUID);

    response = testClient.get("/a/registered/resource");

    assertThat(response.statusCode(), is(200));
    assertThat(response.content(), is("OK"));
    assertThat(response.firstHeader("Content-Type"), is("text/html"));
  }

  @Test
  public void editMappingViaTheDsl() {
    StubMapping stubMapping =
        stubFor(get(urlEqualTo("/edit/this")).willReturn(aResponse().withStatus(200)));

    assertThat(testClient.get("/edit/this").statusCode(), is(200));

    editStub(
        get(urlEqualTo("/edit/this"))
            .withId(stubMapping.getId())
            .willReturn(aResponse().withStatus(418)));

    assertThat(testClient.get("/edit/this").statusCode(), is(418));
  }
}
