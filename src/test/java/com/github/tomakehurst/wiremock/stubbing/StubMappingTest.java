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
package com.github.tomakehurst.wiremock.stubbing;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.github.tomakehurst.wiremock.common.Json;
import org.junit.Test;

public class StubMappingTest {

  @Test
  public void excludesInsertionIndexFromPublicView() {
    StubMapping stub = get("/saveable").willReturn(ok()).build();

    String json = Json.write(stub);
    System.out.println(json);

    assertThat(json, not(containsString("insertionIndex")));
  }

  @Test
  public void includedInsertionIndexInPrivateView() {
    StubMapping stub = get("/saveable").willReturn(ok()).build();

    String json = Json.writePrivate(stub);
    System.out.println(json);

    assertThat(json, containsString("insertionIndex"));
  }

  @Test
  public void deserialisesInsertionIndex() {
    String json =
        "{\n"
            + "    \"request\": {\n"
            + "        \"method\": \"ANY\",\n"
            + "        \"url\": \"/\"\n"
            + "    },\n"
            + "    \"response\": {\n"
            + "        \"status\": 200\n"
            + "    },\n"
            + "    \"insertionIndex\": 42\n"
            + "}";

    StubMapping stub = Json.read(json, StubMapping.class);

    assertThat(stub.getInsertionIndex(), is(42L));
  }
}
