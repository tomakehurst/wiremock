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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import org.junit.Test;

public class UrlMatchingAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void mappingMatchedWithRegexUrl() {
    String REGEX_URL_MAPPING_REQUEST =
        "{ 													\n"
            + "	\"request\": {									\n"
            + "		\"method\": \"GET\",						\n"
            + "		\"urlPattern\": \"/one/(.*?)/three\"		\n"
            + "	},												\n"
            + "	\"response\": {									\n"
            + "		\"body\": \"Matched!\"						\n"
            + "	}												\n"
            + "}													  ";

    testClient.addResponse(REGEX_URL_MAPPING_REQUEST);
    WireMockResponse response = testClient.get("/one/two/three");

    assertThat(response.statusCode(), is(200));
    assertThat(response.content(), is("Matched!"));
  }
}
