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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.admin.Paginator;
import java.util.List;

@JsonInclude(NON_NULL)
public abstract class PaginatedResult<T> {

  private final List<T> selection;
  private final Meta meta;

  protected PaginatedResult(Paginator<T> paginator) {
    selection = paginator.select();
    meta = new Meta(paginator.getTotal());
  }

  protected PaginatedResult(List<T> source, Meta meta) {
    this.selection = source;
    this.meta = meta;
  }

  public Meta getMeta() {
    return meta;
  }

  protected List<T> select() {
    return selection;
  }

  public static class Meta {

    public final int total;

    @JsonCreator
    public Meta(@JsonProperty("total") int total) {
      this.total = total;
    }
  }
}
