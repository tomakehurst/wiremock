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

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Timing;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.MockRequest;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class ProxiedServeEventFiltersTest {
  @Test
  public void applyWithUniversalRequestPattern() {
    ServeEvent serveEvent = proxiedServeEvent(mockRequest());
    ProxiedServeEventFilters filters =
        new ProxiedServeEventFilters(RequestPattern.ANYTHING, null, false);
    assertTrue(filters.apply(serveEvent));

    // Should default to RequestPattern.ANYTHING when passing null for filters
    filters = new ProxiedServeEventFilters(null, null, false);
    assertTrue(filters.apply(serveEvent));
  }

  @Test
  public void applyWithUnproxiedServeEvent() {
    ServeEvent serveEvent = toServeEvent(null, null, ResponseDefinition.ok());
    ProxiedServeEventFilters filters = new ProxiedServeEventFilters(null, null, false);
    assertFalse(filters.apply(serveEvent));
  }

  @Test
  public void applyWithMethodPattern() {
    ProxiedServeEventFilters filters =
        new ProxiedServeEventFilters(newRequestPattern(GET, anyUrl()).build(), null, false);
    MockRequest request = mockRequest().method(GET).url("/foo");

    assertTrue(filters.apply(proxiedServeEvent(request)));
    assertTrue(filters.apply(proxiedServeEvent(request.url("/bar"))));
    assertFalse(filters.apply(proxiedServeEvent(request.method(POST))));
  }

  @Test
  public void applyWithIds() {
    List<UUID> ids =
        Arrays.asList(
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            UUID.fromString("00000000-0000-0000-0000-000000000001"));
    ProxiedServeEventFilters filters = new ProxiedServeEventFilters(null, ids, false);

    assertTrue(filters.apply(proxiedServeEvent(ids.get(0))));
    assertTrue(filters.apply(proxiedServeEvent(ids.get(1))));
    assertFalse(
        filters.apply(proxiedServeEvent(UUID.fromString("00000000-0000-0000-0000-000000000002"))));
  }

  @Test
  public void applyWithMethodAndUrlPattern() {
    ProxiedServeEventFilters filters =
        new ProxiedServeEventFilters(
            newRequestPattern(GET, urlEqualTo("/foo")).build(), null, false);
    MockRequest request = mockRequest().method(GET).url("/foo");

    assertTrue(filters.apply(proxiedServeEvent(request)));
    assertFalse(filters.apply(proxiedServeEvent(request.url("/bar"))));
    assertFalse(filters.apply(proxiedServeEvent(request.method(POST))));
  }

  @Test
  public void applyWithIdsAndMethodPattern() {
    MockRequest request = mockRequest().method(GET).url("/foo");
    List<UUID> ids =
        Arrays.asList(
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            UUID.fromString("00000000-0000-0000-0000-000000000001"));
    ProxiedServeEventFilters filters =
        new ProxiedServeEventFilters(newRequestPattern(GET, anyUrl()).build(), ids, false);

    assertTrue(filters.apply(proxiedServeEvent(ids.get(0), request)));
    assertFalse(
        filters.apply(
            proxiedServeEvent(UUID.fromString("00000000-0000-0000-0000-000000000002"), request)));
    assertFalse(filters.apply(proxiedServeEvent(ids.get(0), request.method(POST))));
  }

  private ServeEvent toServeEvent(
      UUID id, MockRequest request, ResponseDefinition responseDefinition) {
    return new ServeEvent(
        id,
        request != null ? request.asLoggedRequest() : null,
        null,
        responseDefinition,
        null,
        true,
        Timing.UNTIMED);
  }

  private ServeEvent proxiedServeEvent(UUID id, MockRequest request) {
    return toServeEvent(
        id, request, new ResponseDefinitionBuilder().proxiedFrom("http://localhost").build());
  }

  private ServeEvent proxiedServeEvent(MockRequest request) {
    return proxiedServeEvent(null, request);
  }

  private ServeEvent proxiedServeEvent(UUID id) {
    return proxiedServeEvent(id, null);
  }
}
