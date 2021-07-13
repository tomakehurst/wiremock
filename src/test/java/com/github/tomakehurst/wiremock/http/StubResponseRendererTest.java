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
package com.github.tomakehurst.wiremock.http;

import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.global.GlobalSettingsHolder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.ArrayList;
import java.util.List;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class StubResponseRendererTest {
  private static final int TEST_TIMEOUT = 500;

  private Mockery context;
  private FileSource fileSource;
  private GlobalSettingsHolder globalSettingsHolder;
  private List<ResponseTransformer> responseTransformers;
  private StubResponseRenderer stubResponseRenderer;

  @Before
  public void init() {
    context = new Mockery();
    fileSource = context.mock(FileSource.class);
    globalSettingsHolder = new GlobalSettingsHolder();
    responseTransformers = new ArrayList<>();
    stubResponseRenderer =
        new StubResponseRenderer(fileSource, globalSettingsHolder, null, responseTransformers);
  }

  @Test(timeout = TEST_TIMEOUT)
  public void endpointFixedDelayShouldOverrideGlobalDelay() throws Exception {
    globalSettingsHolder.replaceWith(GlobalSettings.builder().fixedDelay(1000).build());

    Response response = stubResponseRenderer.render(createServeEvent(100));

    assertThat(response.getInitialDelay(), is(100L));
  }

  @Test(timeout = TEST_TIMEOUT)
  public void globalFixedDelayShouldNotBeOverriddenIfNoEndpointDelaySpecified() throws Exception {
    globalSettingsHolder.replaceWith(GlobalSettings.builder().fixedDelay(1000).build());

    Response response = stubResponseRenderer.render(createServeEvent(null));

    assertThat(response.getInitialDelay(), is(1000L));
  }

  @Test(timeout = TEST_TIMEOUT)
  public void shouldSetGlobalFixedDelayOnResponse() throws Exception {
    globalSettingsHolder.replaceWith(GlobalSettings.builder().fixedDelay(1000).build());

    Response response = stubResponseRenderer.render(createServeEvent(null));

    assertThat(response.getInitialDelay(), is(1000L));
  }

  @Test
  public void shouldSetEndpointFixedDelayOnResponse() throws Exception {
    Response response = stubResponseRenderer.render(createServeEvent(2000));

    assertThat(response.getInitialDelay(), is(2000L));
  }

  @Test(timeout = TEST_TIMEOUT)
  public void shouldSetEndpointDistributionDelayOnResponse() throws Exception {
    globalSettingsHolder.replaceWith(
        GlobalSettings.builder()
            .delayDistribution(
                new DelayDistribution() {
                  @Override
                  public long sampleMillis() {
                    return 123;
                  }
                })
            .build());

    Response response = stubResponseRenderer.render(createServeEvent(null));

    assertThat(response.getInitialDelay(), is(123L));
  }

  @Test(timeout = TEST_TIMEOUT)
  public void shouldCombineFixedDelayDistributionDelay() throws Exception {
    globalSettingsHolder.replaceWith(
        GlobalSettings.builder()
            .delayDistribution(
                new DelayDistribution() {
                  @Override
                  public long sampleMillis() {
                    return 123;
                  }
                })
            .build());
    Response response = stubResponseRenderer.render(createServeEvent(2000));
    assertThat(response.getInitialDelay(), is(2123L));
  }

  private ServeEvent createServeEvent(Integer fixedDelayMillis) {
    return ServeEvent.of(
        LoggedRequest.createFrom(mockRequest()),
        new ResponseDefinition(
            0,
            "",
            "",
            null,
            "",
            "",
            null,
            null,
            fixedDelayMillis,
            null,
            null,
            null,
            null,
            null,
            null,
            true));
  }
}
