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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Errors;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.extension.AbstractTransformer;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.google.common.net.MediaType;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.*;

public class ResponseDefinition {

    private final int status;
    private final String statusMessage;
    private final Body body;
    private final String bodyFileName;
    private final HttpHeaders headers;
    private final HttpHeaders additionalProxyRequestHeaders;
    private final Integer fixedDelayMilliseconds;
    private final DelayDistribution delayDistribution;
    private final ChunkedDribbleDelay chunkedDribbleDelay;
    private final String proxyBaseUrl;
    private final String proxyUrlPrefixToRemove;
    private final Fault fault;
    private final List<String> transformers;
    private final Parameters transformerParameters;

    private String browserProxyUrl;
    private Boolean wasConfigured = true;
    private Request originalRequest;

    @JsonCreator
    public ResponseDefinition(@JsonProperty("status") int status,
                              @JsonProperty("statusMessage") String statusMessage,
                              @JsonProperty("body") String body,
                              @JsonProperty("jsonBody") JsonNode jsonBody,
                              @JsonProperty("base64Body") String base64Body,
                              @JsonProperty("bodyFileName") String bodyFileName,
                              @JsonProperty("headers") HttpHeaders headers,
                              @JsonProperty("additionalProxyRequestHeaders") HttpHeaders additionalProxyRequestHeaders,
                              @JsonProperty("fixedDelayMilliseconds") Integer fixedDelayMilliseconds,
                              @JsonProperty("delayDistribution") DelayDistribution delayDistribution,
                              @JsonProperty("chunkedDribbleDelay") ChunkedDribbleDelay chunkedDribbleDelay,
                              @JsonProperty("proxyBaseUrl") String proxyBaseUrl,
                              @JsonProperty("proxyUrlPrefixToRemove") String proxyUrlPrefixToRemove,
                              @JsonProperty("fault") Fault fault,
                              @JsonProperty("transformers") List<String> transformers,
                              @JsonProperty("transformerParameters") Parameters transformerParameters,
                              @JsonProperty("fromConfiguredStub") Boolean wasConfigured) {
        this(status, statusMessage, Body.fromOneOf(null, body, jsonBody, base64Body), bodyFileName, headers, additionalProxyRequestHeaders, fixedDelayMilliseconds, delayDistribution, chunkedDribbleDelay, proxyBaseUrl, proxyUrlPrefixToRemove, fault, transformers, transformerParameters, wasConfigured);
    }

    public ResponseDefinition(int status,
                              String statusMessage,
                              byte[] body,
                              JsonNode jsonBody,
                              String base64Body,
                              String bodyFileName,
                              HttpHeaders headers,
                              HttpHeaders additionalProxyRequestHeaders,
                              Integer fixedDelayMilliseconds,
                              DelayDistribution delayDistribution,
                              ChunkedDribbleDelay chunkedDribbleDelay,
                              String proxyBaseUrl,
                              String proxyUrlPrefixToRemove,
                              Fault fault,
                              List<String> transformers,
                              Parameters transformerParameters,
                              Boolean wasConfigured) {
        this(status, statusMessage, Body.fromOneOf(body, null, jsonBody, base64Body), bodyFileName, headers, additionalProxyRequestHeaders, fixedDelayMilliseconds, delayDistribution, chunkedDribbleDelay, proxyBaseUrl, proxyUrlPrefixToRemove, fault, transformers, transformerParameters, wasConfigured);
    }

    private ResponseDefinition(int status,
                               String statusMessage,
                               Body body,
                               String bodyFileName,
                               HttpHeaders headers,
                               HttpHeaders additionalProxyRequestHeaders,
                               Integer fixedDelayMilliseconds,
                               DelayDistribution delayDistribution,
                               ChunkedDribbleDelay chunkedDribbleDelay,
                               String proxyBaseUrl,
                               String proxyUrlPrefixToRemove,
                               Fault fault,
                               List<String> transformers,
                               Parameters transformerParameters,
                               Boolean wasConfigured) {
        this.status = status > 0 ? status : 200;
        this.statusMessage = statusMessage;

        this.body = body;
        this.bodyFileName = bodyFileName;

        this.headers = headers;
        this.additionalProxyRequestHeaders = additionalProxyRequestHeaders;
        this.fixedDelayMilliseconds = fixedDelayMilliseconds;
        this.delayDistribution = delayDistribution;
        this.chunkedDribbleDelay = chunkedDribbleDelay;
        this.proxyBaseUrl = proxyBaseUrl;
        this.proxyUrlPrefixToRemove = proxyUrlPrefixToRemove;
        this.fault = fault;
        this.transformers = transformers;
        this.transformerParameters = transformerParameters;
        this.wasConfigured = wasConfigured == null ? true : wasConfigured;
    }

    public ResponseDefinition(final int statusCode, final String bodyContent) {
        this(statusCode, null, Body.fromString(bodyContent), null, null, null, null, null, null, null, null, null, Collections.<String>emptyList(), Parameters.empty(), true);
    }

    public ResponseDefinition(final int statusCode, final byte[] bodyContent) {
        this(statusCode, null, Body.fromBytes(bodyContent), null, null, null, null, null, null, null, null, null, Collections.<String>emptyList(), Parameters.empty(), true);
    }

    public ResponseDefinition() {
        this(HTTP_OK, null, Body.none(), null, null, null, null, null, null, null, null, null, Collections.<String>emptyList(), Parameters.empty(), true);
    }

    public static ResponseDefinition notFound() {
        return new ResponseDefinition(HTTP_NOT_FOUND, (byte[]) null);
    }

    public static ResponseDefinition ok() {
        return new ResponseDefinition(HTTP_OK, (byte[]) null);
    }

    public static ResponseDefinition okEmptyJson() {
        return ResponseDefinitionBuilder.okForEmptyJson().build();
    }

    public static <T> ResponseDefinition okForJson(T body) {
        return ResponseDefinitionBuilder.okForJson(body).build();
    }

    public static ResponseDefinition created() {
        return new ResponseDefinition(HTTP_CREATED, (byte[]) null);
    }

    public static ResponseDefinition noContent() {
        return new ResponseDefinition(HTTP_NO_CONTENT, (byte[]) null);
    }

    public static ResponseDefinition badRequest(Errors errors) {
        return ResponseDefinitionBuilder.responseDefinition()
            .withStatus(422)
            .withHeader(CONTENT_TYPE, "application/json")
            .withBody(Json.write(errors))
            .build();
    }

    public static ResponseDefinition redirectTo(String path) {
        return new ResponseDefinitionBuilder()
            .withHeader("Location", path)
            .withStatus(HTTP_MOVED_TEMP)
            .build();
    }

    public static ResponseDefinition notConfigured() {
        final ResponseDefinition response = new ResponseDefinition(HTTP_NOT_FOUND, (byte[]) null);
        response.wasConfigured = false;
        return response;
    }

    public static ResponseDefinition notAuthorised() {
        return new ResponseDefinition(HTTP_UNAUTHORIZED, (byte[]) null);
    }

    public static ResponseDefinition notPermitted(String message) {
        return notPermitted(Errors.single(40, message));
    }

    public static ResponseDefinition notPermitted(Errors errors) {
        return ResponseDefinitionBuilder
                .jsonResponse(errors, HTTP_FORBIDDEN);
    }

    public static ResponseDefinition browserProxy(Request originalRequest) {
        final ResponseDefinition response = new ResponseDefinition();
        response.browserProxyUrl = originalRequest.getAbsoluteUrl();
        return response;
    }

    public static ResponseDefinition copyOf(ResponseDefinition original) {
        return original.copy();
    }

    public ResponseDefinition copy() {
        ResponseDefinition newResponseDef = new ResponseDefinition(
            this.status,
            this.statusMessage,
            this.body,
            this.bodyFileName,
            this.headers,
            this.additionalProxyRequestHeaders,
            this.fixedDelayMilliseconds,
            this.delayDistribution,
            this.chunkedDribbleDelay,
            this.proxyBaseUrl,
            this.proxyUrlPrefixToRemove,
            this.fault,
            this.transformers,
            this.transformerParameters,
            this.wasConfigured
        );
        return newResponseDef;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public HttpHeaders getAdditionalProxyRequestHeaders() {
        return additionalProxyRequestHeaders;
    }

    public int getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getBody() {
        return (!body.isBinary() && !body.isJson()) ? body.asString() : null;
    }

    @JsonIgnore
    public String getTextBody() {
        return !body.isBinary() ? body.asString() : null;
    }

    @JsonIgnore
    public byte[] getByteBody() {
        return body.asBytes();
    }

    @JsonIgnore
    public byte[] getByteBodyIfBinary() {
        return body.isBinary() ? body.asBytes() : null;
    }

    public String getBase64Body() {
        return body.isBinary() ? body.asBase64() : null;
    }

    public JsonNode getJsonBody() {

        return body.isJson() ? body.asJson(): null;
    }

    public String getBodyFileName() {
        return bodyFileName;
    }

    public boolean wasConfigured() {
        return wasConfigured == null || wasConfigured;
    }

    public Boolean isFromConfiguredStub() {
        return wasConfigured == null || wasConfigured ? null : false;
    }

    public Integer getFixedDelayMilliseconds() {
        return fixedDelayMilliseconds;
    }

    public DelayDistribution getDelayDistribution() {
        return delayDistribution;
    }

    public ChunkedDribbleDelay getChunkedDribbleDelay() {
        return chunkedDribbleDelay;
    }

    @JsonIgnore
    public String getProxyUrl() {
        if (browserProxyUrl != null) {
            return browserProxyUrl;
        }

        return proxyBaseUrl + StringUtils.removeStart(originalRequest.getUrl(), proxyUrlPrefixToRemove);
    }

    public String getProxyBaseUrl() {
        return proxyBaseUrl;
    }

    public String getProxyUrlPrefixToRemove() {
        return proxyUrlPrefixToRemove;
    }

    @JsonIgnore
    public boolean specifiesBodyFile() {
        return bodyFileName != null && body.isAbsent();
    }

    @JsonIgnore
    public boolean specifiesBodyContent() {
        return body.isPresent();
    }

    @JsonIgnore
    public boolean specifiesTextBodyContent() {
        return body.isPresent() && !body.isBinary();
    }

    @JsonIgnore
    public boolean specifiesBinaryBodyContent() {
        return (body.isPresent() && body.isBinary());
    }

    @JsonIgnore
    public boolean isProxyResponse() {
        return browserProxyUrl != null || proxyBaseUrl != null;
    }

    @JsonIgnore
    public Request getOriginalRequest() {
        return originalRequest;
    }

    public void setOriginalRequest(final Request originalRequest) {
        this.originalRequest = originalRequest;
    }

    public Fault getFault() {
        return fault;
    }

    @JsonInclude(NON_EMPTY)
    public List<String> getTransformers() {
        return transformers;
    }

    @JsonInclude(NON_EMPTY)
    public Parameters getTransformerParameters() {
        return transformerParameters;
    }

    public boolean hasTransformer(AbstractTransformer transformer) {
        return transformers != null && transformers.contains(transformer.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponseDefinition that = (ResponseDefinition) o;
        return status == that.status &&
            Objects.equals(statusMessage, that.statusMessage) &&
            Objects.equals(body, that.body) &&
            Objects.equals(bodyFileName, that.bodyFileName) &&
            Objects.equals(headers, that.headers) &&
            Objects.equals(additionalProxyRequestHeaders, that.additionalProxyRequestHeaders) &&
            Objects.equals(fixedDelayMilliseconds, that.fixedDelayMilliseconds) &&
            Objects.equals(delayDistribution, that.delayDistribution) &&
            Objects.equals(chunkedDribbleDelay, that.chunkedDribbleDelay) &&
            Objects.equals(proxyBaseUrl, that.proxyBaseUrl) &&
            Objects.equals(proxyUrlPrefixToRemove, that.proxyUrlPrefixToRemove) &&
            fault == that.fault &&
            Objects.equals(transformers, that.transformers) &&
            Objects.equals(transformerParameters, that.transformerParameters) &&
            Objects.equals(browserProxyUrl, that.browserProxyUrl) &&
            Objects.equals(wasConfigured, that.wasConfigured);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, statusMessage, body, bodyFileName, headers, additionalProxyRequestHeaders, fixedDelayMilliseconds, delayDistribution, chunkedDribbleDelay, proxyBaseUrl, proxyUrlPrefixToRemove, fault, transformers, transformerParameters, browserProxyUrl, wasConfigured);
    }

    @Override
    public String toString() {
        return this.wasConfigured ? Json.write(this) : "(no response definition configured)";
    }


}
