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
package com.github.tomakehurst.wiremock.recording;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.Test;

public class SnapshotRecordResultDeserialiserTest {

  @Test
  public void supportsFullResponse() {
    SnapshotRecordResult result =
        Json.read(
            "{                                     \n"
                + "    \"mappings\": [                    \n"
                + "        {                              \n"
                + "            \"request\": {             \n"
                + "                \"url\": \"/hello\",   \n"
                + "                \"method\": \"GET\"    \n"
                + "            },                         \n"
                + "            \"response\": {            \n"
                + "                \"status\": 201        \n"
                + "            }                          \n"
                + "        },                             \n"
                + "        {}                             \n"
                + "    ]                                  \n"
                + "}",
            SnapshotRecordResult.class);

    assertThat(result, instanceOf(SnapshotRecordResult.Full.class));
    assertThat(result.getStubMappings().size(), is(2));
    assertThat(result.getStubMappings().get(0), instanceOf(StubMapping.class));
  }

  @Test
  public void supportsIdsOnlyResponse() {
    SnapshotRecordResult result =
        Json.read(
            "{                                                     \n"
                + "    \"ids\": [                                         \n"
                + "        \"d3f32721-ab5e-479c-9f7a-fda76ed5d803\",      \n"
                + "        \"162d0567-4baf-408b-ad7f-41a779638082\",      \n"
                + "        \"02ee46c3-0b49-40ca-a424-8298c099b6db\"       \n"
                + "    ]                                                  \n"
                + "}",
            SnapshotRecordResult.class);

    assertThat(result, instanceOf(SnapshotRecordResult.Ids.class));

    SnapshotRecordResult.Ids idsResult = (SnapshotRecordResult.Ids) result;
    assertThat(idsResult.getIds().size(), is(3));
  }
}
