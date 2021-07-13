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
package com.github.tomakehurst.wiremock.extension;

import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.common.Metadata;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Parameters extends Metadata {

  public static Parameters empty() {
    return new Parameters();
  }

  public static Parameters from(Map<String, Object> parameterMap) {
    Parameters parameters = new Parameters();
    parameters.putAll(parameterMap);
    return parameters;
  }

  public static Parameters one(String name, Object value) {
    return from(ImmutableMap.of(name, value));
  }

  public static <T> Parameters of(T myData) {
    return from(Json.objectToMap(myData));
  }

  public Parameters merge(Parameters other) {
    Map<String, Object> attributes = new LinkedHashMap<>(this);
    attributes.putAll(other);
    return Parameters.from(attributes);
  }
}
