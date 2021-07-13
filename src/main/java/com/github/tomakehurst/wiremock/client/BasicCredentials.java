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
package com.github.tomakehurst.wiremock.client;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToIgnoreCase;
import static com.github.tomakehurst.wiremock.common.Encoding.encodeBase64;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;

public class BasicCredentials {

  public final String username;
  public final String password;

  @JsonCreator
  public BasicCredentials(
      @JsonProperty("username") String username, @JsonProperty("password") String password) {
    this.username = username;
    this.password = password;
  }

  public boolean present() {
    return username != null && password != null;
  }

  public MultiValuePattern asAuthorizationMultiValuePattern() {
    return MultiValuePattern.of(equalToIgnoreCase(asAuthorizationHeaderValue()));
  }

  public String asAuthorizationHeaderValue() {
    byte[] usernameAndPassword = (username + ":" + password).getBytes();
    return "Basic " + encodeBase64(usernameAndPassword);
  }
}
