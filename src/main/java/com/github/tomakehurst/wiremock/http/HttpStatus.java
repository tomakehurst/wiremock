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

public class HttpStatus {

  public static boolean isSuccess(int code) {
    return ((200 <= code) && (code <= 299));
  }

  public static boolean isRedirection(int code) {
    return ((300 <= code) && (code <= 399));
  }

  public static boolean isClientError(int code) {
    return ((400 <= code) && (code <= 499));
  }

  public static boolean isServerError(int code) {
    return ((500 <= code) && (code <= 599));
  }
}
