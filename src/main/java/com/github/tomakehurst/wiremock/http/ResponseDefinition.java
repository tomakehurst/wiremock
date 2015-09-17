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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.net.HttpURLConnection.*;

@JsonSerialize(include=Inclusion.NON_NULL)
public class ResponseDefinition {

	private final int status;
	private final Body body;
	private final String bodyFileName;
	private final HttpHeaders headers;
	private final HttpHeaders additionalProxyRequestHeaders;
	private final Integer fixedDelayMilliseconds;
	private final String proxyBaseUrl;
	private final Fault fault;
	private final List<String> transformers;

	private String browserProxyUrl;
	private boolean wasConfigured = true;
	private Request originalRequest;

	@JsonCreator
	public ResponseDefinition(@JsonProperty("status") int status,
							  @JsonProperty("body") String body,
							  @JsonProperty("jsonBody") JsonNode jsonBody,
							  @JsonProperty("base64Body") String base64Body,
							  @JsonProperty("bodyFileName") String bodyFileName,
							  @JsonProperty("headers") HttpHeaders headers,
							  @JsonProperty("additionalProxyRequestHeaders") HttpHeaders additionalProxyRequestHeaders,
							  @JsonProperty("fixedDelayMilliseconds") Integer fixedDelayMilliseconds,
							  @JsonProperty("proxyBaseUrl") String proxyBaseUrl,
							  @JsonProperty("fault") Fault fault,
							  @JsonProperty("transformers") List<String> transformers) {
		this(status, Body.fromOneOf(null, body, jsonBody, base64Body), bodyFileName, headers, additionalProxyRequestHeaders, fixedDelayMilliseconds, proxyBaseUrl, fault, transformers);
	}

	public ResponseDefinition(int status,
							  byte[] body,
							  JsonNode jsonBody,
							  String base64Body,
							  String bodyFileName,
							  HttpHeaders headers,
							  HttpHeaders additionalProxyRequestHeaders,
							  Integer fixedDelayMilliseconds,
							  String proxyBaseUrl,
							  Fault fault,
							  List<String> transformers) {
		this(status, Body.fromOneOf(body, null, jsonBody, base64Body), bodyFileName, headers, additionalProxyRequestHeaders, fixedDelayMilliseconds, proxyBaseUrl, fault, transformers);
	}

	private ResponseDefinition(int status,
							   Body body,
							   String bodyFileName,
							   HttpHeaders headers,
							   HttpHeaders additionalProxyRequestHeaders,
							   Integer fixedDelayMilliseconds,
							   String proxyBaseUrl,
							   Fault fault,
							   List<String> transformers) {
		this.status = status > 0 ? status : 200;

		this.body = body;
		this.bodyFileName = bodyFileName;

		this.headers = headers;
		this.additionalProxyRequestHeaders = additionalProxyRequestHeaders;
		this.fixedDelayMilliseconds = fixedDelayMilliseconds;
		this.proxyBaseUrl = proxyBaseUrl;
		this.fault = fault;
		this.transformers = transformers;
	}

	public ResponseDefinition(final int statusCode, final String bodyContent) {
		this(statusCode, Body.fromString(bodyContent), null, null, null, null, null, null, Collections.<String>emptyList());
	}

	public ResponseDefinition(final int statusCode, final byte[] bodyContent) {
		this(statusCode, Body.fromBytes(bodyContent), null, null, null, null, null, null, Collections.<String>emptyList());
	}

	public ResponseDefinition() {
		this(HTTP_OK, Body.none(), null, null, null, null, null, null, Collections.<String>emptyList());
	}

	public static ResponseDefinition notFound() {
		return new ResponseDefinition(HTTP_NOT_FOUND, (byte[])null);
	}

	public static ResponseDefinition ok() {
		return new ResponseDefinition(HTTP_OK, (byte[])null);
	}

	public static ResponseDefinition created() {
		return new ResponseDefinition(HTTP_CREATED, (byte[])null);
	}

	public static ResponseDefinition redirectTo(String path) {
		return new ResponseDefinitionBuilder()
				.withHeader("Location", path)
				.withStatus(HTTP_MOVED_TEMP)
				.build();
	}

	public static ResponseDefinition notConfigured() {
		final ResponseDefinition response = new ResponseDefinition(HTTP_NOT_FOUND, (byte[])null);
		response.wasConfigured = false;
		return response;
	}

	public static ResponseDefinition browserProxy(Request originalRequest) {
		final ResponseDefinition response = new ResponseDefinition();
		response.browserProxyUrl = originalRequest.getAbsoluteUrl();
		return response;
	}

	public static ResponseDefinition copyOf(ResponseDefinition original) {
		ResponseDefinition newResponseDef = new ResponseDefinition(
				original.status,
				original.body,
				original.bodyFileName,
				original.headers,
				original.additionalProxyRequestHeaders,
				original.fixedDelayMilliseconds,
				original.proxyBaseUrl,
				original.fault,
				original.transformers
		);
		newResponseDef.wasConfigured = original.wasConfigured;
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

	public String getBody() {
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

	public String getBodyFileName() {
		return bodyFileName;
	}

	public boolean wasConfigured() {
		return wasConfigured;
	}

	public Integer getFixedDelayMilliseconds() {
		return fixedDelayMilliseconds;
	}

	@JsonIgnore
	public String getProxyUrl() {
		if (browserProxyUrl != null) {
			return browserProxyUrl;
		}

		return proxyBaseUrl + originalRequest.getUrl();
	}

	public String getProxyBaseUrl() {
		return proxyBaseUrl;
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
	public boolean specifiesBinaryBodyContent() {
		return (body.isPresent() && body.isBinary());
	}

	@JsonIgnore
	public boolean isProxyResponse() {
		return browserProxyUrl != null || proxyBaseUrl != null;
	}

	public Request getOriginalRequest() {
		return originalRequest;
	}

	public void setOriginalRequest(final Request originalRequest) {
		this.originalRequest = originalRequest;
	}

	public Fault getFault() {
		return fault;
	}

	public List<String> getTransformers() {
		return transformers;
	}

	public boolean hasTransformer(ResponseTransformer transformer) {
		return transformers != null && transformers.contains(transformer.name());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ResponseDefinition that = (ResponseDefinition) o;
		return Objects.equals(status, that.status) &&
				Objects.equals(wasConfigured, that.wasConfigured) &&
				Objects.equals(body, that.body) &&
				Objects.equals(bodyFileName, that.bodyFileName) &&
				Objects.equals(headers, that.headers) &&
				Objects.equals(additionalProxyRequestHeaders, that.additionalProxyRequestHeaders) &&
				Objects.equals(fixedDelayMilliseconds, that.fixedDelayMilliseconds) &&
				Objects.equals(proxyBaseUrl, that.proxyBaseUrl) &&
				Objects.equals(browserProxyUrl, that.browserProxyUrl) &&
				Objects.equals(fault, that.fault) &&
				Objects.equals(originalRequest, that.originalRequest) &&
				Objects.equals(transformers, that.transformers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(status, body, bodyFileName, headers, additionalProxyRequestHeaders, fixedDelayMilliseconds, proxyBaseUrl, browserProxyUrl, fault, wasConfigured, originalRequest, transformers);
	}

	@Override
	public String toString() {
		return Json.write(this);
	}
}
