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

import static java.util.Arrays.asList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.NamedValueMatcher;

public class RequestMethod implements NamedValueMatcher<RequestMethod> {

  public static final RequestMethod GET = new RequestMethod("GET");
  public static final RequestMethod POST = new RequestMethod("POST");
  public static final RequestMethod PUT = new RequestMethod("PUT");
  public static final RequestMethod DELETE = new RequestMethod("DELETE");
  public static final RequestMethod PATCH = new RequestMethod("PATCH");
  public static final RequestMethod OPTIONS = new RequestMethod("OPTIONS");
  public static final RequestMethod HEAD = new RequestMethod("HEAD");
  public static final RequestMethod TRACE = new RequestMethod("TRACE");
  public static final RequestMethod ANY = new RequestMethod("ANY");

  private final String name;

  public RequestMethod(String name) {
    if (name == null) throw new NullPointerException("Method name cannot be null");
    this.name = name;
  }

  @JsonCreator
  public static RequestMethod fromString(String value) {
    return new RequestMethod(value);
  }

  @JsonValue
  public String value() {
    return name;
  }

  public boolean isOneOf(RequestMethod... methods) {
    return asList(methods).contains(this);
  }

  public MatchResult match(RequestMethod method) {
    return MatchResult.of(this.equals(ANY) || this.equals(method));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getExpected() {
    return getName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestMethod that = (RequestMethod) o;

    return name.equals(that.name);
  }

  public boolean hasEntity() {
    return (asList(PUT, PATCH, POST).contains(this));
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }

  public static RequestMethod[] values() {
    return new RequestMethod[] {GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD, TRACE, ANY};
  }
}
