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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.http.Cookie;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.matching.RequestMatcherExtension.NEVER;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static com.github.tomakehurst.wiremock.matching.WeightedMatchResult.weight;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;

public class RequestPattern implements NamedValueMatcher<Request> {

    private final UrlPattern url;
    private final RequestMethod method;
    private final Map<String, MultiValuePattern> headers;
    private final Map<String, MultiValuePattern> queryParams;
    private final Map<String, StringValuePattern> cookies;
    private final BasicCredentials basicAuthCredentials;
    private final List<ContentPattern<?>> bodyPatterns;
    private final List<MultipartValuePattern> multipartPatterns;

    private CustomMatcherDefinition customMatcherDefinition;
    private ValueMatcher<Request> matcher;

    private final RequestMatcher defaultMatcher = new RequestMatcher() {
        @Override
        public MatchResult match(Request request) {
            return MatchResult.aggregateWeighted(
                weight(url.match(request.getUrl()), 10.0),
                weight(method.match(request.getMethod()), 3.0),

                weight(allHeadersMatchResult(request)),
                weight(allQueryParamsMatch(request)),
                weight(allCookiesMatch(request)),
                weight(allBodyPatternsMatch(request)),
                weight(allMultipartPatternsMatch(request))
            );
        }

        @Override
        public String getName() {
            return "default";
        }

    };

    public RequestPattern(UrlPattern url,
                          RequestMethod method,
                          Map<String, MultiValuePattern> headers,
                          Map<String, MultiValuePattern> queryParams,
                          Map<String, StringValuePattern> cookies,
                          BasicCredentials basicAuthCredentials,
                          List<ContentPattern<?>> bodyPatterns,
                          CustomMatcherDefinition customMatcherDefinition,
                          List<MultipartValuePattern> multiPattern) {
        this.url = url;
        this.method = firstNonNull(method, RequestMethod.ANY);
        this.headers = headers;
        this.queryParams = queryParams;
        this.cookies = cookies;
        this.basicAuthCredentials = basicAuthCredentials;
        this.bodyPatterns = bodyPatterns;
        this.matcher = defaultMatcher;
        this.customMatcherDefinition = customMatcherDefinition;
        this.multipartPatterns = multiPattern;
    }

    @JsonCreator
    public RequestPattern(@JsonProperty("url") String url,
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
            UrlPattern.fromOneOf(url, urlPattern, urlPath, urlPathPattern),
            method,
            headers,
            queryParams,
            cookies,
            basicAuthCredentials,
            bodyPatterns,
            customMatcherDefinition,
            multiPattern
        );
    }

    public static RequestPattern ANYTHING = new RequestPattern(
        WireMock.anyUrl(),
        RequestMethod.ANY,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    public RequestPattern(ValueMatcher<Request> customMatcher) {
        this(null, null, null, null, null, null, null, null, null);
        this.matcher = customMatcher;
    }

    public RequestPattern(CustomMatcherDefinition customMatcherDefinition) {
        this(null, null, null, null, null, null, null, customMatcherDefinition, null);
    }

    @Override
    public MatchResult match(Request request) {
        return match(request, Collections.<String, RequestMatcherExtension>emptyMap());
    }

    public static RequestPattern everything() {
        return newRequestPattern(RequestMethod.ANY, anyUrl()).build();
    }

    public MatchResult match(Request request,  Map<String, RequestMatcherExtension> customMatchers) {
        if (customMatcherDefinition != null) {
            RequestMatcherExtension requestMatcher =
                firstNonNull(customMatchers.get(customMatcherDefinition.getName()), NEVER);
            return requestMatcher.match(request, customMatcherDefinition.getParameters());
        }

        return matcher.match(request);
    }

    private MatchResult allCookiesMatch(final Request request) {
        if (cookies != null && !cookies.isEmpty()) {
            return MatchResult.aggregate(
                from(cookies.entrySet())
                    .transform(new Function<Map.Entry<String, StringValuePattern>, MatchResult>() {
                        public MatchResult apply(final Map.Entry<String, StringValuePattern> cookiePattern) {
                            Cookie cookie = request.getCookies().get(cookiePattern.getKey());
                            if (cookie == null) {
                                return cookiePattern.getValue().nullSafeIsAbsent() ?
                                    MatchResult.exactMatch() :
                                    MatchResult.noMatch();
                            }

                            return from(cookie.getValues()).transform(new Function<String, MatchResult>() {
                                @Override
                                public MatchResult apply(String cookieValue) {
                                    return cookiePattern.getValue().match(cookieValue);
                                }
                            }).toSortedList(new Comparator<MatchResult>() {
                                @Override
                                public int compare(MatchResult o1, MatchResult o2) {
                                    return o2.compareTo(o1);
                                }
                            }).get(0);
                        }
                    }).toList()
            );
        }

        return MatchResult.exactMatch();
    }

    private MatchResult allHeadersMatchResult(final Request request) {
        Map<String, MultiValuePattern> combinedHeaders = combineBasicAuthAndOtherHeaders();

        if (combinedHeaders != null && !combinedHeaders.isEmpty()) {
            return MatchResult.aggregate(
                from(combinedHeaders.entrySet())
                    .transform(new Function<Map.Entry<String, MultiValuePattern>, MatchResult>() {
                        public MatchResult apply(Map.Entry<String, MultiValuePattern> headerPattern) {
                            return headerPattern.getValue().match(request.header(headerPattern.getKey()));
                        }
                    }).toList()
            );
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
                .putAll(firstNonNull(combinedHeaders, Collections.<String, MultiValuePattern>emptyMap()));
        allHeadersBuilder.put(AUTHORIZATION, basicAuthCredentials.asAuthorizationMultiValuePattern());
        combinedHeaders = allHeadersBuilder.build();
        return combinedHeaders;
    }

    private MatchResult allQueryParamsMatch(final Request request) {
        if (queryParams != null && !queryParams.isEmpty()) {
            return MatchResult.aggregate(
                from(queryParams.entrySet())
                    .transform(new Function<Map.Entry<String, MultiValuePattern>, MatchResult>() {
                        public MatchResult apply(Map.Entry<String, MultiValuePattern> queryParamPattern) {
                            return queryParamPattern.getValue().match(request.queryParameter(queryParamPattern.getKey()));
                        }
                    }).toList()
            );
        }

        return MatchResult.exactMatch();
    }

    @SuppressWarnings("unchecked")
    private MatchResult allBodyPatternsMatch(final Request request) {
        if (bodyPatterns != null && !bodyPatterns.isEmpty() && request.getBody() != null) {
            return MatchResult.aggregate(
                from(bodyPatterns).transform(new Function<ContentPattern, MatchResult>() {
                    @Override
                    public MatchResult apply(ContentPattern pattern) {
                        if (StringValuePattern.class.isAssignableFrom(pattern.getClass())) {
                            return pattern.match(request.getBodyAsString());
                        }

                        return ((BinaryEqualToPattern) pattern).match(request.getBody());
                    }


                }).toList()
            );
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
                            .transform(new Function<MultipartValuePattern, MatchResult>() {
                                public MatchResult apply(MultipartValuePattern pattern) {
                                    return pattern.match(request);
                                }
                            }).toList()
            );
        }

        return MatchResult.exactMatch();
    }

    public boolean isMatchedBy(Request request, Map<String, RequestMatcherExtension> customMatchers) {
        return match(request, customMatchers).isExactMatch();
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
        return (url != null && url.getClass().equals(clazz) && url.isRegex() == regex && url.isSpecified()) ? url.getPattern().getValue() : null;
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

    public boolean hasCustomMatcher() {
        return matcher != defaultMatcher;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestPattern that = (RequestPattern) o;
        return Objects.equals(url, that.url) &&
            Objects.equals(method, that.method) &&
            Objects.equals(headers, that.headers) &&
            Objects.equals(queryParams, that.queryParams) &&
            Objects.equals(cookies, that.cookies) &&
            Objects.equals(basicAuthCredentials, that.basicAuthCredentials) &&
            Objects.equals(bodyPatterns, that.bodyPatterns) &&
            Objects.equals(customMatcherDefinition, that.customMatcherDefinition) &&
            Objects.equals(matcher, that.matcher) &&
            Objects.equals(multipartPatterns, that.multipartPatterns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method, headers, queryParams, cookies, basicAuthCredentials, bodyPatterns, customMatcherDefinition, matcher, multipartPatterns);
    }

    @Override
    public String toString() {
        return Json.write(this);
    }

    public static Predicate<Request> thatMatch(final RequestPattern pattern, final Map<String, RequestMatcherExtension> customMatchers) {
        return new Predicate<Request>() {
            @Override
            public boolean apply(Request request) {
                return pattern.match(request, customMatchers).isExactMatch();
            }
        };
    }

    public static Predicate<Request> thatMatch(final RequestPattern pattern) {
        return thatMatch(pattern, Collections.<String, RequestMatcherExtension>emptyMap());
    }
}
