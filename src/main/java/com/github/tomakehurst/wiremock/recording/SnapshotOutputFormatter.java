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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.util.List;

/** Wraps a list of generated StubMappings into a SnapshotRecordResult object */
public enum SnapshotOutputFormatter {
  FULL {
    @Override
    public SnapshotRecordResult format(List<StubMapping> stubMappings) {
      return SnapshotRecordResult.full(stubMappings);
    }
  },
  IDS {
    @Override
    public SnapshotRecordResult format(List<StubMapping> stubMappings) {
      return SnapshotRecordResult.idsFromMappings(stubMappings);
    }
  };

  public abstract SnapshotRecordResult format(List<StubMapping> stubMappings);

  @JsonCreator
  public static SnapshotOutputFormatter fromString(String value) {
    if (value == null || value.equalsIgnoreCase("full")) {
      return FULL;
    } else if (value.equalsIgnoreCase("ids")) {
      return IDS;
    } else {
      throw new IllegalArgumentException("Invalid output format");
    }
  }
}
