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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties({"$schema", "meta"})
public class StubMappingCollection extends StubMapping {

  private List<StubMapping> mappings;

  @JsonIgnore
  public boolean isMulti() {
    return mappings != null;
  }

  @JsonIgnore
  public List<? extends StubMapping> getMappingOrMappings() {
    return isMulti() ? getMappings() : Collections.singletonList(this);
  }

  public List<StubMapping> getMappings() {
    return mappings;
  }

  public void setMappings(List<StubMapping> mappings) {
    this.mappings = mappings;
  }
}
