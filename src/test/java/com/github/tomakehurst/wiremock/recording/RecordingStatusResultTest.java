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

import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.equalToJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.common.Json;
import org.junit.Test;

public class RecordingStatusResultTest {

  @Test
  public void deserialise() {
    RecordingStatusResult result =
        Json.read("{ \"status\": \"Recording\" }", RecordingStatusResult.class);

    assertThat(result.getStatus(), is(RecordingStatus.Recording));
  }

  @Test
  public void serialise() {
    RecordingStatusResult result = new RecordingStatusResult(RecordingStatus.Recording);

    String json = Json.write(result);

    assertThat(json, equalToJson("{ \"status\": \"Recording\" }"));
  }
}
