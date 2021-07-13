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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.testsupport.TestHttpHeader.withHeader;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import java.io.UnsupportedEncodingException;
import org.apache.http.entity.StringEntity;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AdminRequestHandlerTest {
  private Mockery context;
  private WireMockServer wm;
  private WireMockTestClient client;

  @Before
  public void init() {
    context = new Mockery();
  }

  @After
  public void cleanup() {
    if (wm != null) {
      wm.stop();
    }
  }

  @Test
  public void shouldLogInfoOnRequest() throws UnsupportedEncodingException {
    final Notifier notifier = context.mock(Notifier.class);
    wm = new WireMockServer(options().dynamicPort().notifier(notifier));
    wm.start();
    client = new WireMockTestClient(wm.port());

    final String postHeaderABCName = "ABC";
    final String postHeaderABCValue = "abc123";
    final String postBody =
        "{\n"
            + "    \"request\": {\n"
            + "        \"method\": \"GET\",\n"
            + "        \"url\": \"/some/thing\"\n"
            + "    },\n"
            + "    \"response\": {\n"
            + "        \"status\": 200,\n"
            + "        \"body\": \"Hello world!\",\n"
            + "        \"headers\": {\n"
            + "            \"Content-Type\": \"text/plain\"\n"
            + "        }\n"
            + "    }\n"
            + "}";

    context.checking(
        new Expectations() {
          {
            one(notifier)
                .info(
                    with(
                        allOf(
                            containsString("Admin request received:\n127.0.0.1 - POST /mappings\n"),
                            containsString(postHeaderABCName + ": [" + postHeaderABCValue + "]\n"),
                            containsString(postBody))));
          }
        });

    client.post(
        "/__admin/mappings",
        new StringEntity(postBody),
        withHeader(postHeaderABCName, postHeaderABCValue));

    context.assertIsSatisfied();
  }
}
