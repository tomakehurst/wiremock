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
package com.github.tomakehurst.wiremock.verification;

import static com.github.tomakehurst.wiremock.common.Encoding.decodeBase64;
import static com.github.tomakehurst.wiremock.common.Encoding.encodeBase64;
import static com.github.tomakehurst.wiremock.common.Strings.stringFromBytes;
import static com.github.tomakehurst.wiremock.common.Urls.*;
import static com.github.tomakehurst.wiremock.http.HttpHeaders.copyOf;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.FluentIterable.from;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.common.Dates;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.http.*;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoggedRequest implements Request {

  private final String scheme;
  private final String host;
  private final int port;
  private final String url;
  private final String absoluteUrl;
  private final String clientIp;
  private final RequestMethod method;
  private final HttpHeaders headers;
  private final Map<String, Cookie> cookies;
  private final Map<String, QueryParameter> queryParams;
  private final byte[] body;
  private final boolean isBrowserProxyRequest;
  private final Date loggedDate;
  private final Collection<Part> multiparts;

  public static LoggedRequest createFrom(Request request) {
    return new LoggedRequest(
        request.getUrl(),
        request.getAbsoluteUrl(),
        request.getMethod(),
        request.getClientIp(),
        copyOf(request.getHeaders()),
        ImmutableMap.copyOf(request.getCookies()),
        request.isBrowserProxyRequest(),
        new Date(),
        request.getBody(),
        request.getParts());
  }

  @JsonCreator
  public LoggedRequest(
      @JsonProperty("url") String url,
      @JsonProperty("absoluteUrl") String absoluteUrl,
      @JsonProperty("method") RequestMethod method,
      @JsonProperty("clientIp") String clientIp,
      @JsonProperty("headers") HttpHeaders headers,
      @JsonProperty("cookies") Map<String, Cookie> cookies,
      @JsonProperty("browserProxyRequest") boolean isBrowserProxyRequest,
      @JsonProperty("loggedDate") Date loggedDate,
      @JsonProperty("bodyAsBase64") String bodyAsBase64,
      @JsonProperty("body") String ignoredBodyOnlyUsedForBinding,
      @JsonProperty("multiparts") Collection<Part> multiparts) {
    this(
        url,
        absoluteUrl,
        method,
        clientIp,
        headers,
        cookies,
        isBrowserProxyRequest,
        loggedDate,
        decodeBase64(bodyAsBase64),
        multiparts);
  }

  public LoggedRequest(
      String url,
      String absoluteUrl,
      RequestMethod method,
      String clientIp,
      HttpHeaders headers,
      Map<String, Cookie> cookies,
      boolean isBrowserProxyRequest,
      Date loggedDate,
      byte[] body,
      Collection<Part> multiparts) {
    this.url = url;

    this.absoluteUrl = absoluteUrl;
    if (absoluteUrl == null) {
      this.scheme = null;
      this.host = null;
      this.port = -1;
    } else {
      URL fullUrl = safelyCreateURL(absoluteUrl);
      this.scheme = fullUrl.getProtocol();
      this.host = fullUrl.getHost();
      this.port = fullUrl.getPort();
    }

    this.clientIp = clientIp;
    this.method = method;
    this.body = body;
    this.headers = headers;
    this.cookies = cookies;
    this.queryParams = splitQueryFromUrl(url);
    this.isBrowserProxyRequest = isBrowserProxyRequest;
    this.loggedDate = loggedDate;
    this.multiparts = multiparts;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public String getAbsoluteUrl() {
    return absoluteUrl;
  }

  @Override
  public RequestMethod getMethod() {
    return method;
  }

  @Override
  public String getScheme() {
    return scheme;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public String getClientIp() {
    return clientIp;
  }

  @Override
  @JsonIgnore
  public String getHeader(String key) {
    HttpHeader header = header(key);
    if (header.isPresent()) {
      return header.firstValue();
    }

    return null;
  }

  @Override
  public HttpHeader header(String key) {
    return headers.getHeader(key);
  }

  @Override
  public ContentTypeHeader contentTypeHeader() {
    if (headers != null) {
      return headers.getContentTypeHeader();
    }
    return null;
  }

  private Charset encodingFromContentTypeHeaderOrUtf8() {
    ContentTypeHeader contentTypeHeader = contentTypeHeader();
    if (contentTypeHeader != null) {
      return contentTypeHeader.charset();
    }
    return UTF_8;
  }

  @Override
  public boolean containsHeader(String key) {
    return getHeader(key) != null;
  }

  @Override
  public Map<String, Cookie> getCookies() {
    return cookies;
  }

  @Override
  public byte[] getBody() {
    return body;
  }

  @Override
  @JsonProperty("body")
  public String getBodyAsString() {
    return stringFromBytes(body, encodingFromContentTypeHeaderOrUtf8());
  }

  @Override
  @JsonProperty("bodyAsBase64")
  public String getBodyAsBase64() {
    return encodeBase64(body);
  }

  @Override
  @JsonIgnore
  public Set<String> getAllHeaderKeys() {
    return headers.keys();
  }

  @Override
  public QueryParameter queryParameter(String key) {
    return firstNonNull(queryParams.get(key), QueryParameter.absent(key));
  }

  @JsonProperty("queryParams")
  public Map<String, QueryParameter> getQueryParams() {
    return queryParams;
  }

  public HttpHeaders getHeaders() {
    return headers;
  }

  @Override
  public boolean isBrowserProxyRequest() {
    return isBrowserProxyRequest;
  }

  @JsonIgnore
  @Override
  public Optional<Request> getOriginalRequest() {
    return Optional.absent();
  }

  public Date getLoggedDate() {
    return loggedDate;
  }

  public String getLoggedDateString() {
    return loggedDate != null ? Dates.format(loggedDate) : null;
  }

  @Override
  public String toString() {
    return Json.write(this);
  }

  @JsonIgnore
  @Override
  public boolean isMultipart() {
    return (multiparts != null && multiparts.size() > 0);
  }

  @JsonIgnore
  @Override
  public Collection<Part> getParts() {
    return multiparts;
  }

  @JsonIgnore
  @Override
  public Part getPart(final String name) {
    return (multiparts != null && name != null)
        ? from(multiparts)
            .firstMatch(
                new Predicate<Part>() {
                  @Override
                  public boolean apply(Part input) {
                    return (name.equals(input.getName()));
                  }
                })
            .get()
        : null;
  }
}
