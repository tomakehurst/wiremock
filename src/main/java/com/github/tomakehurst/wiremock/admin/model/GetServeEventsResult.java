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
package com.github.tomakehurst.wiremock.admin.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.admin.Paginator;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.util.List;

public class GetServeEventsResult extends RequestJournalDependentResult<ServeEvent> {

  @JsonCreator
  public GetServeEventsResult(
      @JsonProperty("requests") List<ServeEvent> source,
      @JsonProperty("meta") Meta meta,
      @JsonProperty("requestJournalDisabled") boolean requestJournalDisabled) {
    super(source, meta, requestJournalDisabled);
  }

  public GetServeEventsResult(Paginator<ServeEvent> paginator, boolean requestJournalDisabled) {
    super(paginator, requestJournalDisabled);
  }

  public static GetServeEventsResult requestJournalEnabled(Paginator<ServeEvent> paginator) {
    return new GetServeEventsResult(paginator, false);
  }

  public static GetServeEventsResult requestJournalDisabled(Paginator<ServeEvent> paginator) {
    return new GetServeEventsResult(paginator, true);
  }

  public List<ServeEvent> getRequests() {
    return getServeEvents();
  }

  @JsonIgnore
  public List<ServeEvent> getServeEvents() {
    return select();
  }
}
