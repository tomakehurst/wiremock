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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StubImport {

  private final List<StubMapping> mappings;
  private final Options importOptions;

  public StubImport(
      @JsonProperty("mappings") List<StubMapping> mappings,
      @JsonProperty("importOptions") Options importOptions) {
    this.mappings = mappings;
    this.importOptions = importOptions;
  }

  public List<StubMapping> getMappings() {
    return mappings;
  }

  public Options getImportOptions() {
    return importOptions;
  }

  public static StubImportBuilder stubImport() {
    return new StubImportBuilder();
  }

  public static class Options {

    public enum DuplicatePolicy {
      OVERWRITE,
      IGNORE
    }

    private final DuplicatePolicy duplicatePolicy;
    private final Boolean deleteAllNotInImport;

    public Options(
        @JsonProperty("duplicatePolicy") DuplicatePolicy duplicatePolicy,
        @JsonProperty("deleteAllNotInImport") Boolean deleteAllNotInImport) {
      this.duplicatePolicy = duplicatePolicy;
      this.deleteAllNotInImport = deleteAllNotInImport;
    }

    public DuplicatePolicy getDuplicatePolicy() {
      return duplicatePolicy;
    }

    public Boolean getDeleteAllNotInImport() {
      return deleteAllNotInImport;
    }

    public static final Options DEFAULTS = new Options(DuplicatePolicy.OVERWRITE, false);
  }
}
