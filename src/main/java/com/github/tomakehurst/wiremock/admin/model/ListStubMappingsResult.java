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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.admin.LimitAndOffsetPaginator;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.util.List;

public class ListStubMappingsResult extends PaginatedResult<StubMapping> {

  @JsonCreator
  public ListStubMappingsResult(
      @JsonProperty("mappings") List<StubMapping> mappings, @JsonProperty("meta") Meta meta) {
    super(mappings, meta);
  }

  public ListStubMappingsResult(LimitAndOffsetPaginator<StubMapping> paginator) {
    super(paginator);
  }

  public List<StubMapping> getMappings() {
    return select();
  }
}
