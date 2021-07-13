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

import com.github.tomakehurst.wiremock.common.Urls;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.jetty9.MultipartParser;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.common.Strings.bytesFromString;
import static com.github.tomakehurst.wiremock.http.HttpHeader.httpHeader;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;

public class MockRequest implements Request {

    private String scheme = "http";
    private String host = "my.domain";
    private int port = 80;
    private String url = "/";
    private RequestMethod method = RequestMethod.ANY;
    private HttpHeaders headers = new HttpHeaders();
    private Map<String, Cookie> cookies = newHashMap();
    private byte[] body;
    private String clientIp = "1.1.1.1";
    private Collection<Part> multiparts = null;
    private boolean isBrowserProxyRequest = false;

    public static MockRequest mockRequest() {
        return new MockRequest();
    }

    public MockRequest scheme(String scheme){
        this.scheme = scheme;
        return this;
    }

    public MockRequest host(String host) {
        this.host = host;
        return this;
    }

    public MockRequest port(int port) {
        this.port = port;
        return this;
    }

    public MockRequest url(String url) {
        this.url = url;
        return this;
    }

    public MockRequest method(RequestMethod method) {
        this.method = method;
        return this;
    }

    public MockRequest header(String key, String... values) {
        headers = headers.plus(httpHeader(key, values));
        return this;
    }

    public MockRequest cookie(String key, String... values) {
        cookies.put(key, new Cookie(asList(values)));
        return this;
    }

    public MockRequest body(String body) {
        this.body = body.getBytes(UTF_8);
        return this;
    }

    public MockRequest body(byte[] body) {
        this.body = body;
        return this;
    }

    public MockRequest clientIp(String clientIp) {
        this.clientIp = clientIp;
        return this;
    }

    public MockRequest parts(Collection<Part> multiparts) {
        this.multiparts = multiparts;
        return this;
    }

    public MockRequest part(MockMultipart part) {
        if (multiparts == null) {
            multiparts = newArrayList();
        }

        multiparts.add(part);
        return this;
    }

    public MockRequest isBrowserProxyRequest(boolean isBrowserProxyRequest) {
        this.isBrowserProxyRequest = isBrowserProxyRequest;
        return this;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getAbsoluteUrl() {
        return "http://my.domain" + url;
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
    public String getHeader(String key) {
        return header(key).firstValue();
    }

    @Override
    public HttpHeader header(final String key) {
        return tryFind(headers.all(), new Predicate<HttpHeader>() {
            public boolean apply(HttpHeader input) {
                return input.keyEquals(key);
            }
        }).or(HttpHeader.absent(key));
    }

    @Override
    public ContentTypeHeader contentTypeHeader() {
        return ContentTypeHeader.absent();
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public boolean containsHeader(String key) {
        return headers.getHeader(key).isPresent();
    }

    @Override
    public Set<String> getAllHeaderKeys() {
        return getHeaders().keys();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return cookies;
    }

    @Override
    public QueryParameter queryParameter(String key) {
        Map<String, QueryParameter> queryParams = Urls.splitQuery(URI.create(url));
        return queryParams.get(key);
    }

    @Override
    public byte[] getBody() {
        return body;
    }

    @Override
    public String getBodyAsString() {
        return body != null ? new String(body) : null;
    }

    @Override
    public String getBodyAsBase64() {
        return "";
    }

    @Override
    public boolean isBrowserProxyRequest() {
        return isBrowserProxyRequest;
    }

    @Override
    public Optional<Request> getOriginalRequest() {
        return Optional.absent();
    }

    public LoggedRequest asLoggedRequest() {
        return LoggedRequest.createFrom(this);
    }

    @Override
    public boolean isMultipart() {
        return getParts() != null;
    }

    @Override
    public Collection<Part> getParts() {
        return multiparts;
    }

    @Override
    public Part getPart(final String name) {
        return (getParts() != null && name != null) ? from(multiparts).firstMatch(new Predicate<Part>() {
            @Override
            public boolean apply(Part input) {
                if (name.equals(input.getName())) {
                    return true;
                }
                return false;
            }
        }).get() : null;
    }

    public MockRequest multipartBody(String body) {
        ContentTypeHeader contentTypeHeader = headers.getContentTypeHeader();
        String contentType = contentTypeHeader.isPresent() ? contentTypeHeader.firstValue() : "multipart/form-data; boundary=BOUNDARY";
        this.multiparts = MultipartParser.parse(bytesFromString(body), contentType);

        return this;
    }
}
