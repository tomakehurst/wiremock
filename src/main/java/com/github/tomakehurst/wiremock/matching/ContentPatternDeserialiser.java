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
package com.github.tomakehurst.wiremock.matching;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Map;

public class ContentPatternDeserialiser extends JsonDeserializer<ContentPattern<?>> {

  @Override
  public ContentPattern<?> deserialize(JsonParser parser, DeserializationContext context)
      throws IOException, JsonProcessingException {
    JsonNode rootNode = parser.readValueAsTree();

    if (isAbsent(rootNode)) {
      return AbsentPattern.ABSENT;
    }

    if (rootNode.has("binaryEqualTo")) {
      return deserializeBinaryEqualTo(rootNode);
    }

    return new StringValuePatternJsonDeserializer().buildStringValuePattern(rootNode);
  }

  private BinaryEqualToPattern deserializeBinaryEqualTo(JsonNode rootNode)
      throws JsonMappingException {
    String operand = rootNode.findValue("binaryEqualTo").textValue();

    return new BinaryEqualToPattern(operand);
  }

  private static boolean isAbsent(JsonNode rootNode) {
    for (Map.Entry<String, JsonNode> node : ImmutableList.copyOf(rootNode.fields())) {
      if (node.getKey().equals("absent")) {
        return true;
      }
    }

    return false;
  }
}
