package com.github.tomakehurst.wiremock.matching;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.http.Cookie;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.matching.RequestMatcherExtension.NEVER;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;

public class RequestPattern implements ValueMatcher<Request> {

    private final UrlPattern url;
    private final RequestMethod method;
    private final Map<String, MultiValuePattern> headers;
    private final Map<String, MultiValuePattern> queryParams;
	private final Map<String, MultiValuePattern> formParams;
    private final Map<String, StringValuePattern> cookies;
    private final BasicCredentials basicAuthCredentials;
    private final List<StringValuePattern> bodyPatterns;

    private CustomMatcherDefinition customMatcherDefinition;
    private RequestMatcher matcher;

    private final RequestMatcher defaultMatcher = new RequestMatcher() {
        @Override
        public MatchResult match(Request request) {
            return MatchResult.aggregate(
                url.match(request.getUrl()),
                method.match(request.getMethod()),
                allHeadersMatchResult(request),
                allQueryParamsMatch(request),
				allFormParamsMatch(request),
                allCookiesMatch(request),
                allBodyPatternsMatch(request)
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
						  Map<String, MultiValuePattern> formParams,
                          Map<String, StringValuePattern> cookies,
                          BasicCredentials basicAuthCredentials,
                          List<StringValuePattern> bodyPatterns,
                          CustomMatcherDefinition customMatcherDefinition) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.queryParams = queryParams;
		this.formParams = formParams;
        this.cookies = cookies;
        this.basicAuthCredentials = basicAuthCredentials;
        this.bodyPatterns = bodyPatterns;
        this.matcher = defaultMatcher;
        this.customMatcherDefinition = customMatcherDefinition;
    }

    @JsonCreator
    public RequestPattern(@JsonProperty("url") String url,
                          @JsonProperty("urlPattern") String urlPattern,
                          @JsonProperty("urlPath") String urlPath,
                          @JsonProperty("urlPathPattern") String urlPathPattern,
                          @JsonProperty("method") RequestMethod method,
                          @JsonProperty("headers") Map<String, MultiValuePattern> headers,
                          @JsonProperty("queryParameters") Map<String, MultiValuePattern> queryParams,
						  @JsonProperty("formParameters") Map<String, MultiValuePattern> formParams,
                          @JsonProperty("cookies") Map<String, StringValuePattern> cookies,
                          @JsonProperty("basicAuth") BasicCredentials basicAuthCredentials,
                          @JsonProperty("bodyPatterns") List<StringValuePattern> bodyPatterns,
                          @JsonProperty("customMatcher") CustomMatcherDefinition customMatcherDefinition) {

        this(
            UrlPattern.fromOneOf(url, urlPattern, urlPath, urlPathPattern),
            method,
            headers,
            queryParams,
			formParams,
            cookies,
            basicAuthCredentials,
            bodyPatterns,
            customMatcherDefinition
        );
    }

    public RequestPattern(RequestMatcher customMatcher) {
        this(null, null, null, null, null, null, null, null, null);
        this.matcher = customMatcher;
    }

    public RequestPattern(CustomMatcherDefinition customMatcherDefinition) {
        this(null, null, null, null, null, null, null, null, customMatcherDefinition);
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
                        public MatchResult apply(Map.Entry<String, StringValuePattern> cookiePattern) {
                            Cookie cookie = request.getCookies().get(cookiePattern.getKey());
                            return cookie != null ?
                                cookiePattern.getValue().match(cookie.getValue()) :
                                MatchResult.noMatch();

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

	private MatchResult allFormParamsMatch(final Request request) {
		if (formParams != null && !formParams.isEmpty()) {
			return MatchResult.aggregate(
					from(formParams.entrySet())
							.transform(new Function<Map.Entry<String, MultiValuePattern>, MatchResult>() {
								public MatchResult apply(Map.Entry<String, MultiValuePattern> formParamPattern) {
									return formParamPattern.getValue().match(request.formParameter(formParamPattern.getKey()));
								}
							}).toList()
			);
		}

		return MatchResult.exactMatch();
	}

    private MatchResult allBodyPatternsMatch(final Request request) {
        if (bodyPatterns != null && !bodyPatterns.isEmpty() && request.getBody() != null) {
            return MatchResult.aggregate(
                from(bodyPatterns).transform(new Function<StringValuePattern, MatchResult>() {
                    @Override
                    public MatchResult apply(StringValuePattern pattern) {
                        return pattern.match(request.getBodyAsString());
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

	public Map<String, MultiValuePattern> getFormParams() {
		return formParams;
	}

	public Map<String, StringValuePattern> getCookies() {
        return cookies;
    }

    public List<StringValuePattern> getBodyPatterns() {
        return bodyPatterns;
    }

    public CustomMatcherDefinition getCustomMatcher() {
        return customMatcherDefinition;
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
        return Objects.equal(url, that.url) &&
            Objects.equal(method, that.method) &&
            Objects.equal(headers, that.headers) &&
            Objects.equal(queryParams, that.queryParams) &&
			Objects.equal(formParams, that.formParams) &&
            Objects.equal(cookies, that.cookies) &&
            Objects.equal(basicAuthCredentials, that.basicAuthCredentials) &&
            Objects.equal(bodyPatterns, that.bodyPatterns) &&
            Objects.equal(customMatcherDefinition, that.customMatcherDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url, method, headers, queryParams, formParams, cookies, basicAuthCredentials, bodyPatterns, customMatcherDefinition, matcher, defaultMatcher);
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
}
