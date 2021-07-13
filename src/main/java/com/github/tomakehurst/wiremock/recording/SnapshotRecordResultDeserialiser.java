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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class SnapshotRecordResultDeserialiser extends StdDeserializer<SnapshotRecordResult> {

  protected SnapshotRecordResultDeserialiser() {
    super(SnapshotRecordResult.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public SnapshotRecordResult deserialize(
      JsonParser parser, DeserializationContext deserializationContext)
      throws IOException, JsonProcessingException {
    String name = parser.nextFieldName();
    parser.nextToken();

    if (name.equals("mappings")) {
      JavaType listType =
          deserializationContext
              .getTypeFactory()
              .constructType(new TypeReference<List<StubMapping>>() {});
      List<StubMapping> mappings = deserializationContext.readValue(parser, listType);
      return SnapshotRecordResult.full(mappings);
    } else if (name.equals("ids")) {
      JavaType listType =
          deserializationContext.getTypeFactory().constructType(new TypeReference<List<UUID>>() {});
      List<UUID> ids = deserializationContext.readValue(parser, listType);
      return SnapshotRecordResult.ids(ids);
    }

    throw new JsonParseException(
        parser, "Snapshot result must contain either mappings or ids element");
  }
}
