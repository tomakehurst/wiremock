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
package com.github.tomakehurst.wiremock.security;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.Request;
import java.util.List;

public class SingleHeaderAuthenticator implements Authenticator {

  private final String key;
  private final String value;

  public SingleHeaderAuthenticator(String key, String value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public boolean authenticate(Request request) {
    HttpHeader requestHeader = request.header(key);
    if (requestHeader == null || !requestHeader.isPresent()) {
      return false;
    }

    List<String> headerValues = requestHeader.values();
    return request.containsHeader(AUTHORIZATION) && headerValues.contains(value);
  }
}
