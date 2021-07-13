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

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import java.util.ArrayList;
import java.util.List;

public class StubImportBuilder {

  private List<StubMapping> mappings = new ArrayList<>();
  private StubImport.Options.DuplicatePolicy duplicatePolicy =
      StubImport.Options.DuplicatePolicy.OVERWRITE;
  private Boolean deleteAllNotInImport = false;

  StubImportBuilder() {}

  public StubImportBuilder stub(MappingBuilder stubMappingBuilder) {
    mappings.add(stubMappingBuilder.build());
    return this;
  }

  public StubImportBuilder stub(StubMapping stubMapping) {
    mappings.add(stubMapping);
    return this;
  }

  public StubImportBuilder ignoreExisting() {
    duplicatePolicy = StubImport.Options.DuplicatePolicy.IGNORE;
    return this;
  }

  public StubImportBuilder overwriteExisting() {
    duplicatePolicy = StubImport.Options.DuplicatePolicy.OVERWRITE;
    return this;
  }

  public StubImportBuilder deleteAllExistingStubsNotInImport() {
    deleteAllNotInImport = true;
    return this;
  }

  public StubImportBuilder doNotDeleteExistingStubs() {
    deleteAllNotInImport = false;
    return this;
  }

  public StubImport build() {
    return new StubImport(mappings, new StubImport.Options(duplicatePolicy, deleteAllNotInImport));
  }
}
