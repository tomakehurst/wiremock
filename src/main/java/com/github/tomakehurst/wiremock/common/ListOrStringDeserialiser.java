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

import static com.google.common.collect.Lists.newArrayList;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class ListOrStringDeserialiser<T> extends JsonDeserializer<ListOrSingle<T>> {

  @Override
  @SuppressWarnings("unchecked")
  public ListOrSingle<T> deserialize(JsonParser parser, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    JsonNode rootNode = parser.readValueAsTree();
    if (rootNode.isArray()) {
      ArrayNode arrayNode = (ArrayNode) rootNode;
      List<T> items = newArrayList();
      for (Iterator<JsonNode> i = arrayNode.elements(); i.hasNext(); ) {
        JsonNode node = i.next();
        Object value = getValue(node);
        items.add((T) value);
      }

      return new ListOrSingle<>(items);
    }

    return new ListOrSingle<>((T) getValue(rootNode));
  }

  private static Object getValue(JsonNode node) {
    return node.isTextual()
        ? node.textValue()
        : node.isNumber()
            ? node.numberValue()
            : node.isBoolean() ? node.booleanValue() : node.textValue();
  }
}
