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

import static java.util.Collections.singletonList;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Errors {

  private final List<Error> errors;

  public Errors(@JsonProperty("errors") List<Error> errors) {
    this.errors = errors;
  }

  public static Errors single(Integer code, String sourcePointer, String title) {
    return new Errors(singletonList(new Error(code, new Error.Source(sourcePointer), title)));
  }

  public static Errors single(Integer code, String sourcePointer, String title, String detail) {
    return new Errors(
        singletonList(new Error(code, new Error.Source(sourcePointer), title, detail)));
  }

  public static Errors single(Integer code, String title) {
    return new Errors(singletonList(new Error(code, title)));
  }

  public static Errors singleWithDetail(Integer code, String title, String detail) {
    return new Errors(singletonList(new Error(code, null, title, detail)));
  }

  public static Errors notRecording() {
    return single(30, "Not currently recording.");
  }

  public static Errors validation(String pointer, String message) {
    return single(10, pointer, message);
  }

  public static Errors notPermitted(String reason) {
    return single(50, reason);
  }

  public Error first() {
    if (errors.isEmpty()) {
      throw new IllegalStateException("No errors are present");
    }

    return errors.get(0);
  }

  public List<Error> getErrors() {
    return errors;
  }

  public static class Error {

    private final Integer code;
    private final Source source;
    private final String title;
    private final String detail;

    public Error(
        @JsonProperty("code") Integer code,
        @JsonProperty("source") Source source,
        @JsonProperty("title") String title,
        @JsonProperty("detail") String detail) {
      this.code = code;
      this.source = source;
      this.title = title;
      this.detail = detail;
    }

    public Error(int code, Source source, String title) {
      this(code, source, title, null);
    }

    public Error(int code, String title) {
      this(code, null, title, null);
    }

    public Integer getCode() {
      return code;
    }

    public Source getSource() {
      return source;
    }

    public String getTitle() {
      return title;
    }

    public String getDetail() {
      return detail;
    }

    public static class Source {

      private final String pointer;

      public Source(@JsonProperty("pointer") String pointer) {
        this.pointer = pointer;
      }

      public String getPointer() {
        return pointer;
      }
    }
  }
}
