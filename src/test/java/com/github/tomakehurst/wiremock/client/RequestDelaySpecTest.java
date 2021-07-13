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
package com.github.tomakehurst.wiremock.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.global.RequestDelaySpec;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class RequestDelaySpecTest {

  @Test
  public void canSerialiseToJson() throws Exception {
    String json = Json.write(new RequestDelaySpec(500));
    JSONAssert.assertEquals("{ \"milliseconds\": 500 }", json, false);
  }

  @Test
  public void canDeserialiseFromJson() {
    RequestDelaySpec spec = Json.read("{ \"milliseconds\": 80 }", RequestDelaySpec.class);

    assertThat(spec.milliseconds(), is(80));
  }
}
