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

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.base.Predicate;
import java.util.List;
import java.util.UUID;

/** A predicate to filter proxied ServeEvents against RequestPattern filters and IDs */
public class ProxiedServeEventFilters implements Predicate<ServeEvent> {

  @JsonUnwrapped private RequestPattern filters;

  @JsonUnwrapped private List<UUID> ids;

  @JsonUnwrapped private boolean allowNonProxied;

  public static final ProxiedServeEventFilters ALLOW_ALL =
      new ProxiedServeEventFilters(null, null, false);

  // For Jackson. This class needs to be mutable as @JsonUnwrapped doesn't yet do constructor based
  // serialisation
  public ProxiedServeEventFilters() {}

  public ProxiedServeEventFilters(RequestPattern filters, List<UUID> ids, boolean allowNonProxied) {
    this.filters = filters;
    this.ids = ids;
    this.allowNonProxied = allowNonProxied;
  }

  public RequestPattern getFilters() {
    return filters;
  }

  public void setFilters(RequestPattern filters) {
    this.filters = filters;
  }

  public List<UUID> getIds() {
    return ids;
  }

  public void setIds(List<UUID> ids) {
    this.ids = ids;
  }

  public boolean isAllowNonProxied() {
    return allowNonProxied;
  }

  public void setAllowNonProxied(boolean allowNonProxied) {
    this.allowNonProxied = allowNonProxied;
  }

  @Override
  public boolean apply(ServeEvent serveEvent) {
    if (!serveEvent.getResponseDefinition().isProxyResponse() && !allowNonProxied) {
      return false;
    }

    if (filters != null && !filters.match(serveEvent.getRequest()).isExactMatch()) {
      return false;
    }

    if (ids != null && !ids.contains(serveEvent.getId())) {
      return false;
    }

    return true;
  }
}
