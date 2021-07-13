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
package com.github.tomakehurst.wiremock.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.IOException;
import java.util.List;

public class ListOrSingleSerialiser extends JsonSerializer<ListOrSingle<Object>> {

  @Override
  public void serialize(
      ListOrSingle<Object> value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException, JsonProcessingException {
    if (value.isEmpty()) {
      gen.writeStartArray();
      gen.writeEndArray();
      return;
    }

    Object firstValue = value.getFirst();
    if (value.isSingle()) {
      JsonSerializer<Object> serializer = serializers.findValueSerializer(firstValue.getClass());
      serializer.serialize(firstValue, gen, serializers);
    } else {
      CollectionType type =
          TypeFactory.defaultInstance().constructCollectionType(List.class, firstValue.getClass());
      JsonSerializer<Object> serializer = serializers.findValueSerializer(type);
      serializer.serialize(value, gen, serializers);
    }
  }
}
