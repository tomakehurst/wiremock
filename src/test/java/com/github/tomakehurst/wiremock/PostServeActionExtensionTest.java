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

import static com.github.tomakehurst.wiremock.PostServeActionExtensionTest.CounterNameParameter.counterNameParameter;
import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.Router;
import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.AdminApiExtension;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Test;

public class PostServeActionExtensionTest {

  WireMockServer wm;
  WireMockTestClient client;

  void initWithOptions(Options options) {
    wm = new WireMockServer(options);
    wm.start();
    client = new WireMockTestClient(wm.port());
  }

  @After
  public void cleanup() {
    if (wm != null) {
      wm.stop();
    }
  }

  @Test
  public void triggersActionWhenAppliedToAStubMapping() throws Exception {
    initWithOptions(options().dynamicPort().extensions(new NamedCounterAction()));

    wm.stubFor(
        get(urlPathEqualTo("/count-me"))
            .withPostServeAction("count-request", counterNameParameter().withName("things"))
            .willReturn(aResponse()));

    client.get("/count-me");
    client.get("/count-me");
    client.get("/count-me");
    client.get("/count-me");

    await().atMost(5, SECONDS).until(getContent("/__admin/named-counter/things"), is("4"));
  }

  @Test
  public void continuesWithNoEffectIfANonExistentActionIsReferenced() {
    initWithOptions(options().dynamicPort());

    wm.stubFor(
        get(urlPathEqualTo("/as-normal"))
            .withPostServeAction("does-not-exist", counterNameParameter().withName("things"))
            .willReturn(aResponse().withStatus(200)));

    assertThat(client.get("/as-normal").statusCode(), is(200));
  }

  @Test
  public void providesServeEventWithResponseFieldPopulated() throws InterruptedException {
    final AtomicInteger finalStatus = new AtomicInteger();
    initWithOptions(
        options()
            .dynamicPort()
            .extensions(
                new PostServeAction() {
                  @Override
                  public String getName() {
                    return "response-field-test";
                  }

                  @Override
                  public void doGlobalAction(ServeEvent serveEvent, Admin admin) {
                    if (serveEvent.getResponse() != null) {
                      finalStatus.set(serveEvent.getResponse().getStatus());
                    }
                  }
                }));

    wm.stubFor(get(urlPathEqualTo("/response-status")).willReturn(aResponse().withStatus(418)));

    client.get("/response-status");

    await().atMost(5, SECONDS).until(getValue(finalStatus), is(418));
  }

  private Callable<Integer> getValue(final AtomicInteger value) {
    return new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return value.get();
      }
    };
  }

  private Callable<String> getContent(final String url) {
    return new Callable<String>() {
      @Override
      public String call() throws Exception {
        return client.get(url).content();
      }
    };
  }

  public static class NamedCounterAction extends PostServeAction implements AdminApiExtension {

    private final ConcurrentHashMap<String, Integer> counters = new ConcurrentHashMap<>();

    @Override
    public String getName() {
      return "count-request";
    }

    @Override
    public void contributeAdminApiRoutes(Router router) {
      router.add(
          GET,
          "/named-counter/{name}",
          new AdminTask() {
            @Override
            public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
              String name = pathParams.get("name");
              Integer count = firstNonNull(counters.get(name), 0);
              return responseDefinition().withStatus(200).withBody(String.valueOf(count)).build();
            }
          });
    }

    @Override
    public void doAction(ServeEvent serveEvent, Admin admin, Parameters parameters) {
      CounterNameParameter counterNameParam = parameters.as(CounterNameParameter.class);

      String counterName = counterNameParam.counterName;

      counters.putIfAbsent(counterName, 0);
      Integer oldValue;
      Integer newValue;

      do {
        oldValue = counters.get(counterName);
        newValue = oldValue + 1;
      } while (!counters.replace(counterName, oldValue, newValue));
    }
  }

  public static class CounterNameParameter {

    public String counterName;

    public CounterNameParameter(@JsonProperty("counterName") String counterName) {
      this.counterName = counterName;
    }

    public CounterNameParameter() {}

    public static CounterNameParameter counterNameParameter() {
      return new CounterNameParameter();
    }

    public CounterNameParameter withName(String name) {
      this.counterName = name;
      return this;
    }
  }
}
