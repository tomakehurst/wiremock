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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Metadata extends LinkedHashMap<String, Object> {

  public Metadata() {}

  public Metadata(Map<? extends String, ?> data) {
    super(data);
  }

  public Integer getInt(String key) {
    return checkPresenceValidityAndCast(key, Integer.class);
  }

  public Integer getInt(String key, Integer defaultValue) {
    return returnIfValidOrDefaultIfNot(key, Integer.class, defaultValue);
  }

  public Boolean getBoolean(String key) {
    return checkPresenceValidityAndCast(key, Boolean.class);
  }

  public Boolean getBoolean(String key, Boolean defaultValue) {
    return returnIfValidOrDefaultIfNot(key, Boolean.class, defaultValue);
  }

  public String getString(String key) {
    return checkPresenceValidityAndCast(key, String.class);
  }

  public String getString(String key, String defaultValue) {
    return returnIfValidOrDefaultIfNot(key, String.class, defaultValue);
  }

  public List<?> getList(String key) {
    return checkPresenceValidityAndCast(key, List.class);
  }

  @SuppressWarnings("unchecked")
  public Metadata getMetadata(String key) {
    checkKeyPresent(key);
    checkArgument(Map.class.isAssignableFrom(get(key).getClass()), key + " is not a map");
    return new Metadata((Map<String, ?>) get(key));
  }

  @SuppressWarnings("unchecked")
  private <T> T checkPresenceValidityAndCast(String key, Class<T> type) {
    checkKeyPresent(key);
    checkArgument(
        type.isAssignableFrom(get(key).getClass()),
        key + " is not of type " + type.getSimpleName());
    return (T) get(key);
  }

  @SuppressWarnings("unchecked")
  private <T> T returnIfValidOrDefaultIfNot(String key, Class<T> type, T defaultValue) {
    if (!containsKey(key) || !type.isAssignableFrom(get(key).getClass())) {
      return defaultValue;
    }

    return (T) get(key);
  }

  private void checkKeyPresent(String key) {
    checkArgument(containsKey(key), key + "' not present");
  }

  public static <T> Metadata from(T myData) {
    return new Metadata(Json.objectToMap(myData));
  }

  public static Builder metadata() {
    return new Builder();
  }

  public <T> T as(Class<T> myDataClass) {
    return Json.mapToObject(this, myDataClass);
  }

  public static class Builder {

    private final ImmutableMap.Builder<String, Object> mapBuilder;

    public Builder() {
      this.mapBuilder = ImmutableMap.builder();
    }

    public Builder attr(String key, Object value) {
      mapBuilder.put(key, value);
      return this;
    }

    public Builder list(String key, Object... values) {
      mapBuilder.put(key, ImmutableList.copyOf(values));
      return this;
    }

    public Builder attr(String key, Metadata.Builder metadataBuilder) {
      mapBuilder.put(key, metadataBuilder.build());
      return this;
    }

    public Metadata build() {
      return new Metadata(mapBuilder.build());
    }
  }
}
