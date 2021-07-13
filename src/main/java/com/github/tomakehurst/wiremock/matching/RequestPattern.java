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
package com.github.tomakehurst.wiremock.matching;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.matching.RequestMatcherExtension.NEVER;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static com.github.tomakehurst.wiremock.matching.WeightedMatchResult.weight;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.util.Arrays.asList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.http.Cookie;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import java.util.*;
import org.apache.commons.lang3.StringUtils;

public class RequestPattern implements NamedValueMatcher<Request> {

  private final String scheme;
  private final StringValuePattern host;
  private final Integer port;
  private final UrlPattern url;
  private final RequestMethod method;
  private final Map<String, MultiValuePattern> headers;
  private final Map<String, MultiValuePattern> queryParams;
  private final Map<String, StringValuePattern> cookies;
  private final BasicCredentials basicAuthCredentials;
  private final List<ContentPattern<?>> bodyPatterns;
  private final List<MultipartValuePattern> multipartPatterns;

  private final CustomMatcherDefinition customMatcherDefinition;
  private final ValueMatcher<Request> matcher;
  private final boolean hasInlineCustomMatcher;

  public RequestPattern(
      final String scheme,
      final StringValuePattern host,
      final Integer port,
      final UrlPattern url,
      final RequestMethod method,
      final Map<String, MultiValuePattern> headers,
      final Map<String, MultiValuePattern> queryParams,
      final Map<String, StringValuePattern> cookies,
      final BasicCredentials basicAuthCredentials,
      final List<ContentPattern<?>> bodyPatterns,
      final CustomMatcherDefinition customMatcherDefinition,
      final ValueMatcher<Request> customMatcher,
      final List<MultipartValuePattern> multiPattern) {
    this.scheme = scheme;
    this.host = host;
    this.port = port;
    this.url = firstNonNull(url, UrlPattern.ANY);
    this.method = firstNonNull(method, RequestMethod.ANY);
    this.headers = headers;
    this.queryParams = queryParams;
    this.cookies = cookies;
    this.basicAuthCredentials = basicAuthCredentials;
    this.bodyPatterns = bodyPatterns;
    this.customMatcherDefinition = customMatcherDefinition;
    this.multipartPatterns = multiPattern;
    this.hasInlineCustomMatcher = customMatcher != null;

    this.matcher =
        new RequestMatcher() {
          @Override
          public MatchResult match(Request request) {
            List<WeightedMatchResult> matchResults =
                new ArrayList<>(
                    asList(
                        weight(schemeMatches(request), 3.0),
                        weight(hostMatches(request), 10.0),
                        weight(portMatches(request), 10.0),
                        weight(RequestPattern.this.url.match(request.getUrl()), 10.0),
                        weight(RequestPattern.this.method.match(request.getMethod()), 3.0),
                        weight(allHeadersMatchResult(request)),
                        weight(allQueryParamsMatch(request)),
                        weight(allCookiesMatch(request)),
                        weight(allBodyPatternsMatch(request)),
                        weight(allMultipartPatternsMatch(request))));

            if (hasInlineCustomMatcher) {
              matchResults.add(weight(customMatcher.match(request)));
            }

            return MatchResult.aggregateWeighted(matchResults);
          }

          @Override
          public String getName() {
            return "request-matcher";
          }
        };
  }

  @JsonCreator
  public RequestPattern(
      @JsonProperty("scheme") String scheme,
      @JsonProperty("host") StringValuePattern host,
      @JsonProperty("port") Integer port,
      @JsonProperty("url") String url,
      @JsonProperty("urlPattern") String urlPattern,
      @JsonProperty("urlPath") String urlPath,
      @JsonProperty("urlPathPattern") String urlPathPattern,
      @JsonProperty("method") RequestMethod method,
      @JsonProperty("headers") Map<String, MultiValuePattern> headers,
      @JsonProperty("queryParameters") Map<String, MultiValuePattern> queryParams,
      @JsonProperty("cookies") Map<String, StringValuePattern> cookies,
      @JsonProperty("basicAuth") BasicCredentials basicAuthCredentials,
      @JsonProperty("bodyPatterns") List<ContentPattern<?>> bodyPatterns,
      @JsonProperty("customMatcher") CustomMatcherDefinition customMatcherDefinition,
      @JsonProperty("multipartPatterns") List<MultipartValuePattern> multiPattern) {

    this(
        scheme,
        host,
        port,
        UrlPattern.fromOneOf(url, urlPattern, urlPath, urlPathPattern),
        method,
        headers,
        queryParams,
        cookies,
        basicAuthCredentials,
        bodyPatterns,
        customMatcherDefinition,
        null,
        multiPattern);
  }

  public static RequestPattern ANYTHING =
      new RequestPattern(
          null,
          null,
          null,
          WireMock.anyUrl(),
          RequestMethod.ANY,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null);

  public RequestPattern(ValueMatcher<Request> customMatcher) {
    this(
        null,
        null,
        null,
        UrlPattern.ANY,
        RequestMethod.ANY,
        null,
        null,
        null,
        null,
        null,
        null,
        customMatcher,
        null);
  }

  public RequestPattern(CustomMatcherDefinition customMatcherDefinition) {
    this(
        null,
        null,
        null,
        UrlPattern.ANY,
        RequestMethod.ANY,
        null,
        null,
        null,
        null,
        null,
        customMatcherDefinition,
        null,
        null);
  }

  @Override
  public MatchResult match(Request request) {
    return match(request, Collections.<String, RequestMatcherExtension>emptyMap());
  }

  public static RequestPattern everything() {
    return newRequestPattern(RequestMethod.ANY, anyUrl()).build();
  }

  public MatchResult match(Request request, Map<String, RequestMatcherExtension> customMatchers) {
    if (customMatcherDefinition != null) {
      RequestMatcherExtension requestMatcher =
          firstNonNull(customMatchers.get(customMatcherDefinition.getName()), NEVER);

      MatchResult standardMatchResult = matcher.match(request);
      MatchResult customMatchResult =
          requestMatcher.match(request, customMatcherDefinition.getParameters());

      return MatchResult.aggregate(standardMatchResult, customMatchResult);
    }

    return matcher.match(request);
  }

  private MatchResult allCookiesMatch(final Request request) {
    if (cookies != null && !cookies.isEmpty()) {
      return MatchResult.aggregate(
          from(cookies.entrySet())
              .transform(
                  new Function<Map.Entry<String, StringValuePattern>, MatchResult>() {
                    public MatchResult apply(
                        final Map.Entry<String, StringValuePattern> cookiePattern) {
                      Cookie cookie = request.getCookies().get(cookiePattern.getKey());
                      if (cookie == null) {
                        return cookiePattern.getValue().nullSafeIsAbsent()
                            ? MatchResult.exactMatch()
                            : MatchResult.noMatch();
                      }

                      return from(cookie.getValues())
                          .transform(
                              new Function<String, MatchResult>() {
                                @Override
                                public MatchResult apply(String cookieValue) {
                                  return cookiePattern.getValue().match(cookieValue);
                                }
                              })
                          .toSortedList(
                              new Comparator<MatchResult>() {
                                @Override
                                public int compare(MatchResult o1, MatchResult o2) {
                                  return o2.compareTo(o1);
                                }
                              })
                          .get(0);
                    }
                  })
              .toList());
    }

    return MatchResult.exactMatch();
  }

  private MatchResult schemeMatches(final Request request) {
    return scheme != null
        ? MatchResult.of(scheme.equals(request.getScheme()))
        : MatchResult.exactMatch();
  }

  private MatchResult hostMatches(final Request request) {
    return host != null ? host.match(request.getHost()) : MatchResult.exactMatch();
  }

  private MatchResult portMatches(final Request request) {
    return port != null ? MatchResult.of(request.getPort() == port) : MatchResult.exactMatch();
  }

  private MatchResult allHeadersMatchResult(final Request request) {
    Map<String, MultiValuePattern> combinedHeaders = combineBasicAuthAndOtherHeaders();

    if (combinedHeaders != null && !combinedHeaders.isEmpty()) {
      return MatchResult.aggregate(
          from(combinedHeaders.entrySet())
              .transform(
                  new Function<Map.Entry<String, MultiValuePattern>, MatchResult>() {
                    public MatchResult apply(Map.Entry<String, MultiValuePattern> headerPattern) {
                      return headerPattern.getValue().match(request.header(headerPattern.getKey()));
                    }
                  })
              .toList());
    }

    return MatchResult.exactMatch();
  }

  public Map<String, MultiValuePattern> combineBasicAuthAndOtherHeaders() {
    if (basicAuthCredentials == null) {
      return headers;
    }

    Map<String, MultiValuePattern> combinedHeaders = headers;
    ImmutableMap.Builder<String, MultiValuePattern> allHeadersBuilder =
        ImmutableMap.<String, MultiValuePattern>builder()
            .putAll(
                firstNonNull(combinedHeaders, Collections.<String, MultiValuePattern>emptyMap()));
    allHeadersBuilder.put(AUTHORIZATION, basicAuthCredentials.asAuthorizationMultiValuePattern());
    combinedHeaders = allHeadersBuilder.build();
    return combinedHeaders;
  }

  private MatchResult allQueryParamsMatch(final Request request) {
    if (queryParams != null && !queryParams.isEmpty()) {
      return MatchResult.aggregate(
          from(queryParams.entrySet())
              .transform(
                  new Function<Map.Entry<String, MultiValuePattern>, MatchResult>() {
                    public MatchResult apply(
                        Map.Entry<String, MultiValuePattern> queryParamPattern) {
                      return queryParamPattern
                          .getValue()
                          .match(request.queryParameter(queryParamPattern.getKey()));
                    }
                  })
              .toList());
    }

    return MatchResult.exactMatch();
  }

  @SuppressWarnings("unchecked")
  private MatchResult allBodyPatternsMatch(final Request request) {
    if (bodyPatterns != null && !bodyPatterns.isEmpty() && request.getBody() != null) {
      return MatchResult.aggregate(
          from(bodyPatterns)
              .transform(
                  new Function<ContentPattern, MatchResult>() {
                    @Override
                    public MatchResult apply(ContentPattern pattern) {
                      if (StringValuePattern.class.isAssignableFrom(pattern.getClass())) {
                        String body =
                            StringUtils.isEmpty(request.getBodyAsString())
                                ? null
                                : request.getBodyAsString();
                        return pattern.match(body);
                      }

                      return pattern.match(request.getBody());
                    }
                  })
              .toList());
    }

    return MatchResult.exactMatch();
  }

  @SuppressWarnings("unchecked")
  private MatchResult allMultipartPatternsMatch(final Request request) {
    if (multipartPatterns != null && !multipartPatterns.isEmpty()) {
      if (!request.isMultipart()) {
        return MatchResult.noMatch();
      }
      return MatchResult.aggregate(
          from(multipartPatterns)
              .transform(
                  new Function<MultipartValuePattern, MatchResult>() {
                    public MatchResult apply(MultipartValuePattern pattern) {
                      return pattern.match(request);
                    }
                  })
              .toList());
    }

    return MatchResult.exactMatch();
  }

  public boolean isMatchedBy(Request request, Map<String, RequestMatcherExtension> customMatchers) {
    return match(request, customMatchers).isExactMatch();
  }

  public String getScheme() {
    return scheme;
  }

  public StringValuePattern getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }

  public String getUrl() {
    return urlPatternOrNull(UrlPattern.class, false);
  }

  public String getUrlPattern() {
    return urlPatternOrNull(UrlPattern.class, true);
  }

  public String getUrlPath() {
    return urlPatternOrNull(UrlPathPattern.class, false);
  }

  public String getUrlPathPattern() {
    return urlPatternOrNull(UrlPathPattern.class, true);
  }

  @JsonIgnore
  public UrlPattern getUrlMatcher() {
    return url;
  }

  private String urlPatternOrNull(Class<? extends UrlPattern> clazz, boolean regex) {
    return (url != null
            && url.getClass().equals(clazz)
            && url.isRegex() == regex
            && url.isSpecified())
        ? url.getPattern().getValue()
        : null;
  }

  public RequestMethod getMethod() {
    return method;
  }

  public Map<String, MultiValuePattern> getHeaders() {
    return headers;
  }

  public BasicCredentials getBasicAuthCredentials() {
    return basicAuthCredentials;
  }

  public Map<String, MultiValuePattern> getQueryParameters() {
    return queryParams;
  }

  public Map<String, StringValuePattern> getCookies() {
    return cookies;
  }

  public List<ContentPattern<?>> getBodyPatterns() {
    return bodyPatterns;
  }

  public CustomMatcherDefinition getCustomMatcher() {
    return customMatcherDefinition;
  }

  public List<MultipartValuePattern> getMultipartPatterns() {
    return multipartPatterns;
  }

  @JsonIgnore
  public ValueMatcher<Request> getMatcher() {
    return matcher;
  }

  @Override
  public String getName() {
    return "requestMatching";
  }

  @Override
  public String getExpected() {
    return toString();
  }

  public boolean hasInlineCustomMatcher() {
    return hasInlineCustomMatcher;
  }

  public boolean hasNamedCustomMatcher() {
    return customMatcherDefinition != null;
  }

  public boolean hasCustomMatcher() {
    return hasInlineCustomMatcher() || hasNamedCustomMatcher();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RequestPattern that = (RequestPattern) o;
    return hasInlineCustomMatcher == that.hasInlineCustomMatcher
        && Objects.equals(scheme, that.scheme)
        && Objects.equals(host, that.host)
        && Objects.equals(port, that.port)
        && Objects.equals(url, that.url)
        && Objects.equals(method, that.method)
        && Objects.equals(headers, that.headers)
        && Objects.equals(queryParams, that.queryParams)
        && Objects.equals(cookies, that.cookies)
        && Objects.equals(basicAuthCredentials, that.basicAuthCredentials)
        && Objects.equals(bodyPatterns, that.bodyPatterns)
        && Objects.equals(multipartPatterns, that.multipartPatterns)
        && Objects.equals(customMatcherDefinition, that.customMatcherDefinition)
        && Objects.equals(matcher, that.matcher);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        scheme,
        host,
        port,
        url,
        method,
        headers,
        queryParams,
        cookies,
        basicAuthCredentials,
        bodyPatterns,
        multipartPatterns,
        customMatcherDefinition,
        matcher,
        hasInlineCustomMatcher);
  }

  @Override
  public String toString() {
    return Json.write(this);
  }

  public static Predicate<Request> thatMatch(final RequestPattern pattern) {
    return new Predicate<Request>() {
      @Override
      public boolean apply(Request request) {
        return pattern.match(request).isExactMatch();
      }
    };
  }

  public static Predicate<ServeEvent> withRequstMatching(final RequestPattern pattern) {
    return new Predicate<ServeEvent>() {
      @Override
      public boolean apply(ServeEvent serveEvent) {
        return pattern.match(serveEvent.getRequest()).isExactMatch();
      }
    };
  }
}
