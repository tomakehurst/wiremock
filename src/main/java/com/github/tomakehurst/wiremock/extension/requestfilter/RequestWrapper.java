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
package com.github.tomakehurst.wiremock.extension.requestfilter;

import static com.github.tomakehurst.wiremock.common.Encoding.encodeBase64;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.apache.commons.lang3.StringUtils.ordinalIndexOf;

import com.github.tomakehurst.wiremock.http.*;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import java.util.*;

public class RequestWrapper implements Request {

  private final Request delegate;

  private final RequestMethod method;
  private final FieldTransformer<String> absoluteUrlTransformer;
  private final List<HttpHeader> addedHeaders;
  private final List<String> removedHeaders;
  private final Map<CaseInsensitiveKey, FieldTransformer<List<String>>> headerTransformers;
  private final Map<String, Cookie> additionalCookies;
  private final List<String> cookiesToRemove;
  private final Map<String, FieldTransformer<Cookie>> cookieTransformers;
  private final FieldTransformer<Body> bodyTransformer;
  private final FieldTransformer<Part> multipartTransformer;

  public RequestWrapper(Request delegate) {
    this(
        delegate,
        null,
        null,
        Collections.<HttpHeader>emptyList(),
        Collections.<String>emptyList(),
        Collections.<CaseInsensitiveKey, FieldTransformer<List<String>>>emptyMap(),
        Collections.<String, Cookie>emptyMap(),
        Collections.<String>emptyList(),
        Collections.<String, FieldTransformer<Cookie>>emptyMap(),
        null,
        null);
  }

  public RequestWrapper(
      Request delegate,
      RequestMethod method,
      FieldTransformer<String> absoluteUrlTransformer,
      List<HttpHeader> addedHeaders,
      List<String> removedHeaders,
      Map<CaseInsensitiveKey, FieldTransformer<List<String>>> headerTransformers,
      Map<String, Cookie> additionalCookies,
      List<String> cookiesToRemove,
      Map<String, FieldTransformer<Cookie>> cookieTransformers,
      FieldTransformer<Body> bodyTransformer,
      FieldTransformer<Part> multipartTransformer) {
    this.delegate = delegate;

    this.method = method;
    this.absoluteUrlTransformer = absoluteUrlTransformer;
    this.addedHeaders = addedHeaders;
    this.removedHeaders = removedHeaders;
    this.headerTransformers = headerTransformers;
    this.additionalCookies = additionalCookies;
    this.cookiesToRemove = cookiesToRemove;
    this.cookieTransformers = cookieTransformers;
    this.bodyTransformer = bodyTransformer;
    this.multipartTransformer = multipartTransformer;
  }

  public static Builder create() {
    return new Builder();
  }

  @Override
  public String getUrl() {
    String absoluteUrl = getAbsoluteUrl();
    int relativeStartIndex =
        countMatches(absoluteUrl, "/") >= 3
            ? ordinalIndexOf(absoluteUrl, "/", 3)
            : absoluteUrl.length();
    return absoluteUrl.substring(relativeStartIndex);
  }

  @Override
  public String getAbsoluteUrl() {
    if (absoluteUrlTransformer != null) {
      return absoluteUrlTransformer.transform(delegate.getAbsoluteUrl());
    }

    return delegate.getAbsoluteUrl();
  }

  @Override
  public RequestMethod getMethod() {
    return firstNonNull(method, delegate.getMethod());
  }

  @Override
  public String getScheme() {
    return delegate.getScheme();
  }

  @Override
  public String getHost() {
    return delegate.getHost();
  }

  @Override
  public int getPort() {
    return delegate.getPort();
  }

  @Override
  public String getClientIp() {
    return delegate.getClientIp();
  }

  @Override
  public String getHeader(String key) {
    return getHeaders().getHeader(key).firstValue();
  }

  @Override
  public HttpHeader header(String key) {
    return getHeaders().getHeader(key);
  }

  @Override
  public ContentTypeHeader contentTypeHeader() {
    return delegate.contentTypeHeader();
  }

  @Override
  public HttpHeaders getHeaders() {
    Collection<HttpHeader> existingHeaders = delegate.getHeaders().all();

    List<HttpHeader> combinedHeaders =
        from(existingHeaders)
            .append(addedHeaders)
            .filter(
                new Predicate<HttpHeader>() {
                  @Override
                  public boolean apply(HttpHeader httpHeader) {
                    return !removedHeaders.contains(httpHeader.key());
                  }
                })
            .transform(
                new Function<HttpHeader, HttpHeader>() {
                  @Override
                  public HttpHeader apply(HttpHeader httpHeader) {
                    if (headerTransformers.containsKey(httpHeader.caseInsensitiveKey())) {
                      FieldTransformer<List<String>> transformer =
                          headerTransformers.get(httpHeader.caseInsensitiveKey());
                      List<String> newValues = transformer.transform(httpHeader.values());
                      return new HttpHeader(httpHeader.key(), newValues);
                    }

                    return httpHeader;
                  }
                })
            .toList();
    return new HttpHeaders(combinedHeaders);
  }

  @Override
  public boolean containsHeader(String key) {
    return getHeaders().keys().contains(key);
  }

  @Override
  public Set<String> getAllHeaderKeys() {
    return getHeaders().keys();
  }

  @Override
  public Map<String, Cookie> getCookies() {
    ImmutableMap.Builder<String, Cookie> builder = ImmutableMap.builder();
    for (Map.Entry<String, Cookie> entry : delegate.getCookies().entrySet()) {
      Cookie newCookie =
          cookieTransformers.containsKey(entry.getKey())
              ? cookieTransformers.get(entry.getKey()).transform(entry.getValue())
              : entry.getValue();

      if (!cookiesToRemove.contains(entry.getKey())) {
        builder.put(entry.getKey(), newCookie);
      }
    }

    builder.putAll(additionalCookies);

    return builder.build();
  }

  @Override
  public QueryParameter queryParameter(String key) {
    return delegate.queryParameter(key);
  }

  @Override
  public byte[] getBody() {
    if (bodyTransformer != null) {
      return bodyTransformer.transform(new Body(delegate.getBody())).asBytes();
    }

    return delegate.getBody();
  }

  @Override
  public String getBodyAsString() {
    if (bodyTransformer != null) {
      return bodyTransformer.transform(new Body(delegate.getBodyAsString())).asString();
    }

    return delegate.getBodyAsString();
  }

  @Override
  public String getBodyAsBase64() {
    return encodeBase64(getBody());
  }

  @Override
  public boolean isMultipart() {
    return delegate.isMultipart();
  }

  @Override
  public Collection<Part> getParts() {
    if (delegate.getParts() == null || multipartTransformer == null) {
      return delegate.getParts();
    }

    return from(delegate.getParts())
        .transform(
            new Function<Part, Part>() {
              @Override
              public Part apply(Part part) {
                return multipartTransformer.transform(part);
              }
            })
        .toList();
  }

  @Override
  public Part getPart(String name) {
    if (multipartTransformer != null) {
      return multipartTransformer.transform(delegate.getPart(name));
    }

    return delegate.getPart(name);
  }

  @Override
  public boolean isBrowserProxyRequest() {
    return delegate.isBrowserProxyRequest();
  }

  @Override
  public Optional<Request> getOriginalRequest() {
    return delegate.getOriginalRequest();
  }

  public static class Builder {

    private RequestMethod requestMethod;
    private FieldTransformer<String> absoluteUrlTransformer;

    private final List<HttpHeader> additionalHeaders = newArrayList();
    private final List<String> headersToRemove = newArrayList();
    private final Map<CaseInsensitiveKey, FieldTransformer<List<String>>> headerTransformers =
        newHashMap();

    private final Map<String, Cookie> additionalCookies = newHashMap();
    private final List<String> cookiesToRemove = newArrayList();
    private final Map<String, FieldTransformer<Cookie>> cookieTransformers = newHashMap();

    private FieldTransformer<Body> bodyTransformer;
    private FieldTransformer<Part> mutlipartTransformer;

    public Builder addHeader(String key, String... values) {
      additionalHeaders.add(new HttpHeader(key, values));
      return this;
    }

    public Builder removeHeader(String key) {
      headersToRemove.add(key);
      return this;
    }

    public Builder transformHeader(String key, FieldTransformer<List<String>> transformer) {
      headerTransformers.put(CaseInsensitiveKey.from(key), transformer);
      return this;
    }

    public Builder setMethod(RequestMethod method) {
      requestMethod = method;
      return this;
    }

    public Builder transformAbsoluteUrl(FieldTransformer<String> transformer) {
      absoluteUrlTransformer = transformer;
      return this;
    }

    public Request wrap(Request request) {
      return new RequestWrapper(
          request,
          requestMethod,
          absoluteUrlTransformer,
          additionalHeaders,
          headersToRemove,
          headerTransformers,
          additionalCookies,
          cookiesToRemove,
          cookieTransformers,
          bodyTransformer,
          mutlipartTransformer);
    }

    public Builder transformBody(FieldTransformer<Body> transformer) {
      bodyTransformer = transformer;
      return this;
    }

    public Builder transformCookie(String name, FieldTransformer<Cookie> transformer) {
      cookieTransformers.put(name, transformer);
      return this;
    }

    public Builder transformParts(FieldTransformer<Part> transformer) {
      mutlipartTransformer = transformer;
      return this;
    }

    public Builder addCookie(String name, Cookie value) {
      additionalCookies.put(name, value);
      return this;
    }

    public Builder removeCookie(String name) {
      cookiesToRemove.add(name);
      return this;
    }
  }
}
