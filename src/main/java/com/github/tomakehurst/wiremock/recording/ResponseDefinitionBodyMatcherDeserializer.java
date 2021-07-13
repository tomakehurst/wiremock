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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseDefinitionBodyMatcherDeserializer
    extends JsonDeserializer<ResponseDefinitionBodyMatcher> {
  @Override
  public ResponseDefinitionBodyMatcher deserialize(
      JsonParser parser, DeserializationContext context) throws IOException {
    JsonNode rootNode = parser.readValueAsTree();
    return new ResponseDefinitionBodyMatcher(
        parseJsonNode(rootNode.get("textSizeThreshold")),
        parseJsonNode(rootNode.get("binarySizeThreshold")));
  }

  public static long parseJsonNode(JsonNode node) {
    if (node == null || node.isNull()) {
      return Long.MAX_VALUE;
    } else if (node.isNumber()) {
      return node.asLong();
    } else {
      return parseFilesize(node.textValue());
    }
  }

  // Converts a human-readable file size string (e.g. "10,100 KB") to bytes
  // Partially based off https://stackoverflow.com/a/12090818
  public static long parseFilesize(String in) {
    String cleanedInput = in.trim().replaceAll(",", ".");

    final Matcher m =
        Pattern.compile("^([\\d.]+)\\s*(\\w)?b?$", Pattern.CASE_INSENSITIVE).matcher(cleanedInput);
    if (!m.find()) {
      throw new IllegalArgumentException("Invalid size string: \"" + in + "\"");
    }

    int scale = 1;
    if (m.group(2) != null) {
      switch (m.group(2).toUpperCase()) {
        case "G":
          scale *= 1024;
        case "M":
          scale *= 1024;
        case "K":
          scale *= 1024;
          break;
        default:
          throw new IllegalArgumentException("Invalid size unit: " + m.group(2));
      }
    }
    return Math.round(Double.parseDouble(m.group(1)) * scale);
  }
}
