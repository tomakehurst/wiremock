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
package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.RenderCache;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import java.io.IOException;

public class HandlebarsJsonPathHelper extends HandlebarsHelper<Object> {

  private final Configuration config =
      Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

  private final ParseContext parseContext = JsonPath.using(config);

  @Override
  public Object apply(final Object input, final Options options) throws IOException {
    if (input == null) {
      return "";
    }

    if (options == null || options.param(0, null) == null) {
      return this.handleError("The JSONPath cannot be empty");
    }

    final String jsonPathString = options.param(0);

    try {
      final DocumentContext jsonDocument = getJsonDocument(input, options);
      final JsonPath jsonPath = JsonPath.compile(jsonPathString);
      Object result = getValue(jsonPath, jsonDocument, options);
      return JsonData.create(result);
    } catch (InvalidJsonException e) {
      return this.handleError(input + " is not valid JSON", e.getJson(), e);
    } catch (InvalidPathException e) {
      return this.handleError(jsonPathString + " is not a valid JSONPath expression", e);
    }
  }

  private Object getValue(JsonPath jsonPath, DocumentContext jsonDocument, Options options) {
    RenderCache renderCache = getRenderCache(options);
    RenderCache.Key cacheKey = RenderCache.Key.keyFor(Object.class, jsonPath, jsonDocument);
    Object value = renderCache.get(cacheKey);
    if (value == null) {
      Object defaultValue = options.hash != null ? options.hash("default") : null;
      try {
        value = jsonDocument.read(jsonPath);
      } catch (Exception e) {
        value = defaultValue;
      }

      if (value == null) {
        value = firstNonNull(defaultValue, "");
      }

      renderCache.put(cacheKey, value);
    }

    return value;
  }

  private DocumentContext getJsonDocument(Object json, Options options) {
    RenderCache renderCache = getRenderCache(options);
    RenderCache.Key cacheKey = RenderCache.Key.keyFor(DocumentContext.class, json);
    DocumentContext document = renderCache.get(cacheKey);
    if (document == null) {
      document =
          json instanceof String ? parseContext.parse((String) json) : parseContext.parse(json);
      renderCache.put(cacheKey, document);
    }

    return document;
  }
}
