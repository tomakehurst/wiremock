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

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.http.RequestMethod.DELETE;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static com.github.tomakehurst.wiremock.testsupport.MockRequestBuilder.aRequest;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.equalToJson;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.admin.AdminRoutes;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilter;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.security.NoAuthenticator;
import com.github.tomakehurst.wiremock.testsupport.MockHttpResponder;
import com.github.tomakehurst.wiremock.verification.VerificationResult;
import java.util.Collections;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class AdminRequestHandlerTest {
  private Mockery context;
  private Admin admin;
  private MockHttpResponder httpResponder;

  private AdminRequestHandler handler;

  @Before
  public void init() {
    context = new Mockery();
    admin = context.mock(Admin.class);
    httpResponder = new MockHttpResponder();

    handler =
        new AdminRequestHandler(
            AdminRoutes.defaults(),
            admin,
            new BasicResponseRenderer(),
            new NoAuthenticator(),
            false,
            Collections.<RequestFilter>emptyList());
  }

  @Test
  public void shouldSaveMappingsWhenSaveCalled() {
    Request request = aRequest(context).withUrl("/mappings/save").withMethod(POST).build();

    context.checking(
        new Expectations() {
          {
            one(admin).saveMappings();
          }
        });

    handler.handle(request, httpResponder);
    Response response = httpResponder.response;

    assertThat(response.getStatus(), is(HTTP_OK));
  }

  @Test
  public void shouldClearMappingsJournalAndRequestDelayWhenResetCalled() {
    Request request = aRequest(context).withUrl("/reset").withMethod(POST).build();

    context.checking(
        new Expectations() {
          {
            one(admin).resetAll();
          }
        });

    handler.handle(request, httpResponder);
    Response response = httpResponder.response;

    assertThat(response.getStatus(), is(HTTP_OK));
  }

  @Test
  public void shouldClearJournalWhenResetRequestsCalled() {
    Request request = aRequest(context).withUrl("/requests/reset").withMethod(POST).build();

    context.checking(
        new Expectations() {
          {
            one(admin).resetRequests();
          }
        });

    handler.handle(request, httpResponder);
    Response response = httpResponder.response;

    assertThat(response.getStatus(), is(HTTP_OK));
  }

  private static final String REQUEST_PATTERN_SAMPLE =
      "{												\n"
          + "	\"method\": \"DELETE\",						\n"
          + "	\"url\": \"/some/resource\"					\n"
          + "}												";

  @Test
  public void shouldReturnCountOfMatchingRequests() {
    context.checking(
        new Expectations() {
          {
            RequestPattern requestPattern =
                newRequestPattern(DELETE, urlEqualTo("/some/resource")).build();
            allowing(admin).countRequestsMatching(requestPattern);
            will(returnValue(VerificationResult.withCount(5)));
          }
        });

    handler.handle(
        aRequest(context)
            .withUrl("/requests/count")
            .withMethod(POST)
            .withBody(REQUEST_PATTERN_SAMPLE)
            .build(),
        httpResponder);
    Response response = httpResponder.response;

    assertThat(response.getStatus(), is(HTTP_OK));
    assertThat(
        response.getBodyAsString(),
        equalToJson("{ \"count\": 5, \"requestJournalDisabled\" : false}"));
  }

  private static final String GLOBAL_SETTINGS_JSON =
      "{												\n" + "	\"fixedDelay\": 2000						\n" + "}												";

  @Test
  public void shouldUpdateGlobalSettings() {
    context.checking(
        new Expectations() {
          {
            GlobalSettings expectedSettings = GlobalSettings.builder().fixedDelay(2000).build();
            allowing(admin).updateGlobalSettings(expectedSettings);
          }
        });

    handler.handle(
        aRequest(context)
            .withUrl("/settings")
            .withMethod(POST)
            .withBody(GLOBAL_SETTINGS_JSON)
            .build(),
        httpResponder);
  }
}
