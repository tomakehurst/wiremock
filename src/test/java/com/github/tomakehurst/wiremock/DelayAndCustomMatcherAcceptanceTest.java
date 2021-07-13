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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import com.google.common.base.Stopwatch;
import org.junit.Rule;
import org.junit.Test;

public class DelayAndCustomMatcherAcceptanceTest {

  @Rule
  public WireMockRule wireMockRule =
      new WireMockRule(options().dynamicPort().extensions(BodyChanger.class));

  @Test
  public void delayIsAddedWhenCustomResponseTransformerPresent() {
    stubFor(
        get(urlEqualTo("/delay-this"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withTransformers("response-body-changer")
                    .withUniformRandomDelay(500, 1000)));

    WireMockTestClient client = new WireMockTestClient(wireMockRule.port());

    Stopwatch stopwatch = Stopwatch.createStarted();
    WireMockResponse response = client.get("/delay-this");
    stopwatch.stop();

    assertThat(stopwatch.elapsed(MILLISECONDS), greaterThanOrEqualTo(500L));
    assertThat(response.statusCode(), is(200));
    assertThat(response.content(), is("Transformed body"));
  }

  public static class BodyChanger extends ResponseDefinitionTransformer {

    @Override
    public ResponseDefinition transform(
        Request request,
        ResponseDefinition responseDefinition,
        FileSource files,
        Parameters parameters) {
      return ResponseDefinitionBuilder.like(responseDefinition)
          .but()
          .withBody("Transformed body")
          .build();
    }

    @Override
    public boolean applyGlobally() {
      return false;
    }

    @Override
    public String getName() {
      return "response-body-changer";
    }
  }
}
