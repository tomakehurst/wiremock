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
package com.github.tomakehurst.wiremock.verification.diff;

import com.github.tomakehurst.wiremock.http.*;
import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class EmptyToStringRequestWrapper implements Request {

  private final Request target;

  public EmptyToStringRequestWrapper(Request target) {
    this.target = target;
  }

  @Override
  public String getUrl() {
    return target.getUrl();
  }

  @Override
  public String getAbsoluteUrl() {
    return target.getAbsoluteUrl();
  }

  @Override
  public RequestMethod getMethod() {
    return target.getMethod();
  }

  @Override
  public String getScheme() {
    return target.getScheme();
  }

  @Override
  public String getHost() {
    return target.getHost();
  }

  @Override
  public int getPort() {
    return target.getPort();
  }

  @Override
  public String getClientIp() {
    return target.getClientIp();
  }

  @Override
  public String getHeader(String key) {
    return target.getHeader(key);
  }

  @Override
  public HttpHeader header(String key) {
    return target.header(key);
  }

  @Override
  public ContentTypeHeader contentTypeHeader() {
    return target.contentTypeHeader();
  }

  @Override
  public HttpHeaders getHeaders() {
    return target.getHeaders();
  }

  @Override
  public boolean containsHeader(String key) {
    return target.containsHeader(key);
  }

  @Override
  public Set<String> getAllHeaderKeys() {
    return target.getAllHeaderKeys();
  }

  @Override
  public Map<String, Cookie> getCookies() {
    return target.getCookies();
  }

  @Override
  public QueryParameter queryParameter(String key) {
    return target.queryParameter(key);
  }

  @Override
  public byte[] getBody() {
    return target.getBody();
  }

  @Override
  public String getBodyAsString() {
    return target.getBodyAsString();
  }

  @Override
  public String getBodyAsBase64() {
    return target.getBodyAsBase64();
  }

  @Override
  public boolean isMultipart() {
    return target.isMultipart();
  }

  @Override
  public Collection<Part> getParts() {
    return target.getParts();
  }

  @Override
  public Part getPart(String name) {
    return target.getPart(name);
  }

  @Override
  public boolean isBrowserProxyRequest() {
    return target.isBrowserProxyRequest();
  }

  @Override
  public Optional<Request> getOriginalRequest() {
    return target.getOriginalRequest();
  }

  @Override
  public String toString() {
    return " ";
  }
}
