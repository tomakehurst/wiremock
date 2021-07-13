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
package com.github.tomakehurst.wiremock.http;

import static com.google.common.collect.Iterables.transform;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class HttpHeadersJsonDeserializer extends JsonDeserializer<HttpHeaders> {

  @Override
  public HttpHeaders deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode rootNode = parser.readValueAsTree();
    return new HttpHeaders(transform(all(rootNode.fields()), toHttpHeaders()));
  }

  private static Function<Map.Entry<String, JsonNode>, HttpHeader> toHttpHeaders() {
    return new Function<Map.Entry<String, JsonNode>, HttpHeader>() {
      @Override
      public HttpHeader apply(Map.Entry<String, JsonNode> field) {
        String key = field.getKey();
        if (field.getValue().isArray()) {
          return new HttpHeader(
              key,
              ImmutableList.copyOf(transform(all(field.getValue().elements()), toStringValues())));
        } else {
          return new HttpHeader(key, field.getValue().textValue());
        }
      }
    };
  }

  private static Function<JsonNode, String> toStringValues() {
    return new Function<JsonNode, String>() {
      public String apply(JsonNode node) {
        return node.textValue();
      }
    };
  }

  public static <T> Iterable<T> all(final Iterator<T> underlyingIterator) {
    return new Iterable<T>() {
      public Iterator<T> iterator() {
        return underlyingIterator;
      }
    };
  }
}
