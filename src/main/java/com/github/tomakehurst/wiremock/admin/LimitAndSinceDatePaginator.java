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
package com.github.tomakehurst.wiremock.admin;

import static com.github.tomakehurst.wiremock.admin.Conversions.toDate;
import static com.github.tomakehurst.wiremock.admin.Conversions.toInt;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import java.util.Date;
import java.util.List;

public class LimitAndSinceDatePaginator implements Paginator<ServeEvent> {

  private final List<ServeEvent> source;
  private final Integer limit;
  private final Date since;

  public LimitAndSinceDatePaginator(List<ServeEvent> source, Integer limit, Date since) {
    checkArgument(limit == null || limit >= 0, "limit must be 0 or greater");
    this.source = source;
    this.limit = limit;
    this.since = since;
  }

  public static LimitAndSinceDatePaginator fromRequest(List<ServeEvent> source, Request request) {
    return new LimitAndSinceDatePaginator(
        source, toInt(request.queryParameter("limit")), toDate(request.queryParameter("since")));
  }

  @Override
  public List<ServeEvent> select() {
    FluentIterable<ServeEvent> chain = FluentIterable.from(source);
    return chain
        .filter(
            new Predicate<ServeEvent>() {
              @Override
              public boolean apply(ServeEvent input) {
                return since == null || input.getRequest().getLoggedDate().after(since);
              }
            })
        .limit(firstNonNull(limit, source.size()))
        .toList();
  }

  @Override
  public int getTotal() {
    return source.size();
  }
}
