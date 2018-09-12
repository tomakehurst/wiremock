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
package com.github.tomakehurst.wiremock.client;

import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.extension.AbstractTransformer;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Arrays.asList;

public class ResponseDefinitionBuilder {

	protected int status = HTTP_OK;
	protected String statusMessage;
	protected byte[] binaryBody;
	protected String stringBody;
	protected String base64Body;
	protected String bodyFileName;
	protected List<HttpHeader> headers = newArrayList();
	protected Integer fixedDelayMilliseconds;
	protected DelayDistribution delayDistribution;
	protected ChunkedDribbleDelay chunkedDribbleDelay;
	protected String proxyBaseUrl;
	protected Fault fault;
	protected List<String> responseTransformerNames;
	protected List<AbstractTransformer> responseTransformers = Collections.emptyList();
	protected Map<String, Object> transformerParameters = newHashMap();
	protected Boolean wasConfigured = true;

	public static ResponseDefinitionBuilder like(ResponseDefinition responseDefinition) {
		ResponseDefinitionBuilder builder = new ResponseDefinitionBuilder();
		builder.status = responseDefinition.getStatus();
		builder.statusMessage = responseDefinition.getStatusMessage();
		builder.headers = responseDefinition.getHeaders() != null ?
				newArrayList(responseDefinition.getHeaders().all()) :
				Lists.<HttpHeader>newArrayList();
		builder.binaryBody = responseDefinition.getByteBodyIfBinary();
		builder.stringBody = responseDefinition.getBody();
		builder.base64Body = responseDefinition.getBase64Body();
		builder.bodyFileName = responseDefinition.getBodyFileName();
		builder.fixedDelayMilliseconds = responseDefinition.getFixedDelayMilliseconds();
		builder.delayDistribution = responseDefinition.getDelayDistribution();
		builder.chunkedDribbleDelay = responseDefinition.getChunkedDribbleDelay();
		builder.proxyBaseUrl = responseDefinition.getProxyBaseUrl();
		builder.fault = responseDefinition.getFault();
		builder.responseTransformerNames = responseDefinition.getTransformers();
		builder.responseTransformers = responseDefinition.getTransformerInstances();
		builder.transformerParameters = responseDefinition.getTransformerParameters() != null ?
			Parameters.from(responseDefinition.getTransformerParameters()) :
			Parameters.empty();
		builder.wasConfigured = responseDefinition.isFromConfiguredStub();
		return builder;
	}

    public static ResponseDefinition jsonResponse(Object body) {
        return jsonResponse(body, HTTP_OK);
    }

	public static ResponseDefinition jsonResponse(Object body, int status) {
		return new ResponseDefinitionBuilder()
				.withBody(Json.write(body))
				.withStatus(status)
				.withHeader("Content-Type", "application/json")
				.build();
	}

	public ResponseDefinitionBuilder but() {
		return this;
	}

	public ResponseDefinitionBuilder withStatus(int status) {
		this.status = status;
		return this;
	}

	public ResponseDefinitionBuilder withHeader(String key, String... values) {
		headers.add(new HttpHeader(key, values));
		return this;
	}

	public ResponseDefinitionBuilder withBodyFile(String fileName) {
		this.bodyFileName = fileName;
		return this;
	}

	public ResponseDefinitionBuilder withBody(String body) {
		this.stringBody = body;
		return this;
	}

	public ResponseDefinitionBuilder withBody(byte[] body) {
		this.binaryBody = body;
		return this;
	}

	public ResponseDefinitionBuilder withFixedDelay(Integer milliseconds) {
		this.fixedDelayMilliseconds = milliseconds;
		return this;
	}

	public ResponseDefinitionBuilder withRandomDelay(DelayDistribution distribution) {
		this.delayDistribution = distribution;
		return this;
	}

	public ResponseDefinitionBuilder withLogNormalRandomDelay(double medianMilliseconds, double sigma) {
		return withRandomDelay(new LogNormal(medianMilliseconds, sigma));
	}

	public ResponseDefinitionBuilder withUniformRandomDelay(int lowerMilliseconds, int upperMilliseconds) {
		return withRandomDelay(new UniformDistribution(lowerMilliseconds, upperMilliseconds));
	}

	public ResponseDefinitionBuilder withChunkedDribbleDelay(int numberOfChunks, int totalDuration) {
		this.chunkedDribbleDelay = new ChunkedDribbleDelay(numberOfChunks, totalDuration);
		return this;
	}

	public ResponseDefinitionBuilder withTransformers(String... responseTransformerNames) {
		this.responseTransformerNames = asList(responseTransformerNames);
		return this;
	}

	public ResponseDefinitionBuilder withTransformers(AbstractTransformer... responseTransformers) {
		this.responseTransformers = asList(responseTransformers);
		return this;
	}

	public ResponseDefinitionBuilder withTransformerParameter(String name, Object value) {
		transformerParameters.put(name, value);
		return this;
	}

	public ResponseDefinitionBuilder withTransformer(String transformerName, String parameterKey, Object parameterValue) {
		withTransformers(transformerName);
		withTransformerParameter(parameterKey, parameterValue);
		return this;
	}

	public ProxyResponseDefinitionBuilder proxiedFrom(String proxyBaseUrl) {
		this.proxyBaseUrl = proxyBaseUrl;
		return new ProxyResponseDefinitionBuilder(this);
	}

	public static ResponseDefinitionBuilder responseDefinition() {
		return new ResponseDefinitionBuilder();
	}

	public static <T> ResponseDefinitionBuilder okForJson(T body) {
        return responseDefinition()
            .withStatus(HTTP_OK)
            .withBody(Json.write(body))
            .withHeader("Content-Type", "application/json");
    }

	public static <T> ResponseDefinitionBuilder okForEmptyJson() {
		return responseDefinition()
			.withStatus(HTTP_OK)
			.withBody("{}")
			.withHeader("Content-Type", "application/json");
	}

	public ResponseDefinitionBuilder withHeaders(HttpHeaders headers) {
		this.headers = ImmutableList.copyOf(headers.all());
		return this;
	}

	public ResponseDefinitionBuilder withBase64Body(String base64Body) {
		this.base64Body = base64Body;
		return this;
	}

	public ResponseDefinitionBuilder withStatusMessage(String message) {
		this.statusMessage = message;
		return this;
	}

	public static class ProxyResponseDefinitionBuilder extends ResponseDefinitionBuilder {

		private List<HttpHeader> additionalRequestHeaders = newArrayList();

		public ProxyResponseDefinitionBuilder(ResponseDefinitionBuilder from) {
			this.status = from.status;
			this.headers = from.headers;
			this.binaryBody = from.binaryBody;
			this.stringBody = from.stringBody;
			this.base64Body = from.base64Body;
			this.bodyFileName = from.bodyFileName;
			this.fault = from.fault;
			this.fixedDelayMilliseconds = from.fixedDelayMilliseconds;
			this.proxyBaseUrl = from.proxyBaseUrl;
			this.responseTransformerNames = from.responseTransformerNames;
			this.responseTransformers = from.responseTransformers;
		}

		public ProxyResponseDefinitionBuilder withAdditionalRequestHeader(String key, String value) {
			additionalRequestHeaders.add(new HttpHeader(key, value));
			return this;
		}

		@Override
		public ResponseDefinition build() {
			return !additionalRequestHeaders.isEmpty() ?
					super.build(new HttpHeaders(additionalRequestHeaders)) :
					super.build();
		}
	}

	public ResponseDefinitionBuilder withFault(Fault fault) {
		this.fault = fault;
		return this;
	}

	public ResponseDefinition build() {
		return build(null);
	}

	private boolean isBinaryBody() {
		return binaryBody != null;
	}

	protected ResponseDefinition build(HttpHeaders additionalProxyRequestHeaders) {
		HttpHeaders httpHeaders = headers == null || headers.isEmpty() ? null : new HttpHeaders(headers);
		Parameters transformerParameters = this.transformerParameters == null || this.transformerParameters.isEmpty() ? null : Parameters.from(this.transformerParameters);
		return isBinaryBody() ?
				new ResponseDefinition(
						status,
						statusMessage,
						binaryBody,
						null,
						base64Body,
						bodyFileName,
						httpHeaders,
						additionalProxyRequestHeaders,
						fixedDelayMilliseconds,
						delayDistribution,
						chunkedDribbleDelay,
						proxyBaseUrl,
						fault,
						responseTransformerNames,
						responseTransformers,
						transformerParameters,
						wasConfigured) :
				new ResponseDefinition(
						status,
						statusMessage,
						stringBody,
						base64Body,
						bodyFileName,
						httpHeaders,
						additionalProxyRequestHeaders,
						fixedDelayMilliseconds,
						delayDistribution,
						chunkedDribbleDelay,
						proxyBaseUrl,
						fault,
						responseTransformerNames,
						responseTransformers,
						transformerParameters,
						wasConfigured
				);
	}
}
