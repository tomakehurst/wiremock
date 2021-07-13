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

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface Request {

  interface Part {
    String getName();

    HttpHeader getHeader(String name);

    HttpHeaders getHeaders();

    Body getBody();
  }

  String getUrl();

  String getAbsoluteUrl();

  RequestMethod getMethod();

  String getScheme();

  String getHost();

  int getPort();

  String getClientIp();

  String getHeader(String key);

  HttpHeader header(String key);

  ContentTypeHeader contentTypeHeader();

  HttpHeaders getHeaders();

  boolean containsHeader(String key);

  Set<String> getAllHeaderKeys();

  Map<String, Cookie> getCookies();

  QueryParameter queryParameter(String key);

  byte[] getBody();

  String getBodyAsString();

  String getBodyAsBase64();

  boolean isMultipart();

  Collection<Part> getParts();

  Part getPart(String name);

  boolean isBrowserProxyRequest();

  Optional<Request> getOriginalRequest();
}
