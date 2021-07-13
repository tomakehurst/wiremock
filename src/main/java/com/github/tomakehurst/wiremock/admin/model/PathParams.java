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
package com.github.tomakehurst.wiremock.admin.model;

import java.util.LinkedHashMap;

public class PathParams extends LinkedHashMap<String, String> {

  public static PathParams empty() {
    return new PathParams();
  }

  public PathParams add(String key, String value) {
    put(key, value);
    return this;
  }

  public static PathParams single(String key, Object value) {
    return new PathParams().add(key, value.toString());
  }
}
