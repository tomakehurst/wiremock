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

import static java.util.Collections.emptyList;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import java.util.List;

public class NoClientAuthenticator implements ClientAuthenticator {

  public static NoClientAuthenticator noClientAuthenticator() {
    return new NoClientAuthenticator();
  }

  @Override
  public List<HttpHeader> generateAuthHeaders() {
    return emptyList();
  }
}
