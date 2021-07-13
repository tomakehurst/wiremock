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

import static com.github.tomakehurst.wiremock.http.HttpHeader.httpHeader;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.util.Collections.singletonList;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import java.util.List;

public class ClientBasicAuthenticator implements ClientAuthenticator {

  private final String username;
  private final String password;

  public ClientBasicAuthenticator(String username, String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  public List<HttpHeader> generateAuthHeaders() {
    BasicCredentials basicCredentials = new BasicCredentials(username, password);
    return singletonList(httpHeader(AUTHORIZATION, basicCredentials.asAuthorizationHeaderValue()));
  }
}
