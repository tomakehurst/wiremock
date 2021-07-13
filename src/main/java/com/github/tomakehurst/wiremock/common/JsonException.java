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

import static com.google.common.collect.Lists.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class JsonException extends InvalidInputException {

  protected JsonException(Errors errors) {
    super(errors);
  }

  public static JsonException fromJackson(JsonProcessingException processingException) {
    Throwable rootCause = getRootCause(processingException);

    String message = rootCause.getMessage();
    if (rootCause instanceof PatternSyntaxException) {
      PatternSyntaxException patternSyntaxException = (PatternSyntaxException) rootCause;
      message = patternSyntaxException.getMessage();
    } else if (rootCause instanceof JsonMappingException) {
      message = ((JsonMappingException) rootCause).getOriginalMessage();
    } else if (rootCause instanceof InvalidInputException) {
      message = ((InvalidInputException) rootCause).getErrors().first().getDetail();
    }

    String pointer = null;
    if (processingException instanceof JsonMappingException) {
      List<String> nodes =
          transform(((JsonMappingException) processingException).getPath(), TO_NODE_NAMES);
      pointer = '/' + Joiner.on('/').join(nodes);
    }

    return new JsonException(Errors.single(10, pointer, "Error parsing JSON", message));
  }

  private static Throwable getRootCause(Throwable e) {
    if (e.getCause() != null) {
      return getRootCause(e.getCause());
    }

    return e;
  }

  private static final Function<JsonMappingException.Reference, String> TO_NODE_NAMES =
      new Function<JsonMappingException.Reference, String>() {
        @Override
        public String apply(JsonMappingException.Reference input) {
          if (input.getFieldName() != null) {
            return input.getFieldName();
          }

          return String.valueOf(input.getIndex());
        }
      };
}
