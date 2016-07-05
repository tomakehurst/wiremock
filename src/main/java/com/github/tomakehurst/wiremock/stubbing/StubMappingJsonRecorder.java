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
package com.github.tomakehurst.wiremock.stubbing;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.Gzip;
import com.github.tomakehurst.wiremock.common.IdGenerator;
import com.github.tomakehurst.wiremock.common.UniqueFilenameGenerator;
import com.github.tomakehurst.wiremock.common.VeryShortIdGenerator;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.CaseInsensitiveKey;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.verification.VerificationResult;
import com.google.common.base.Predicate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToXml;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.common.Json.write;
import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static com.google.common.collect.Iterables.filter;

public class StubMappingJsonRecorder implements RequestListener {

    private final FileSource mappingsFileSource;
    private final FileSource filesFileSource;
    private final Admin admin;
    private final List<CaseInsensitiveKey> headersToMatch;
	private final Boolean recordRepeats;
    private IdGenerator idGenerator;
	private final Map<RequestPattern, String> receivedRequestsFileIds = new HashMap<RequestPattern, String>();

    public StubMappingJsonRecorder(FileSource mappingsFileSource, FileSource filesFileSource, Admin admin, List<CaseInsensitiveKey> headersToMatch, boolean recordRepeats) {
        this.mappingsFileSource = mappingsFileSource;
        this.filesFileSource = filesFileSource;
        this.admin = admin;
        this.headersToMatch = headersToMatch;
		this.recordRepeats = recordRepeats;
        idGenerator = new VeryShortIdGenerator();
    }

    @Override
    public void requestReceived(Request request, Response response) {
        RequestPattern requestPattern = buildRequestPatternFrom(request);

        if (recordRepeats) {
            if (response.isFromProxy()) {
                int requestReceivedCount = getRequestReceivedCount(requestPattern);
                notifier().info(
                        String.format("Recording mappings for %s for the %s time", request.getUrl(),
                                requestReceivedCount));
                writeRepeatsToMappingAndBodyFile(request, response, requestPattern, requestReceivedCount);
            }
        } else {
            if (requestNotAlreadyReceived(requestPattern) && response.isFromProxy()) {
                notifier().info(String.format("Recording mappings for %s", request.getUrl()));
                writeToMappingAndBodyFile(request, response, requestPattern);
            } else {
                notifier().info(String.format("Not recording mapping for %s as this has already been received and not recording repeats.", request.getUrl()));
            }
        }
    }

    private RequestPattern buildRequestPatternFrom(Request request) {
        RequestPatternBuilder builder = newRequestPattern(request.getMethod(), urlEqualTo(request.getUrl()));

        if (!headersToMatch.isEmpty()) {
            for (HttpHeader header: request.getHeaders().all()) {
                if (headersToMatch.contains(header.caseInsensitiveKey())) {
                    builder.withHeader(header.key(), equalTo(header.firstValue()));
                }
            }
        }

        String body = request.getBodyAsString();
        if (!body.isEmpty()) {
            builder.withRequestBody(valuePatternForContentType(request));
        }

        return builder.build();
    }

    private StringValuePattern valuePatternForContentType(Request request) {
        String contentType = request.getHeader("Content-Type");
        if (contentType != null) {
            if (contentType.contains("json")) {
                return equalToJson(request.getBodyAsString(), true, true);
            } else if (contentType.contains("xml")) {
                return equalToXml(request.getBodyAsString());
            }
        }

        return equalTo(request.getBodyAsString());
    }

    private void writeToMappingAndBodyFile(Request request, Response response, RequestPattern requestPattern) {
        String fileId = idGenerator.generate();
        String mappingFileName = UniqueFilenameGenerator.generate(request, "mapping", fileId);
        String bodyFileName = UniqueFilenameGenerator.generate(request, "body", fileId);

        ResponseDefinitionBuilder responseDefinitionBuilder = responseDefinition()
                .withStatus(response.getStatus())
                .withBodyFile(bodyFileName);
        if (response.getHeaders().size() > 0) {
            responseDefinitionBuilder.withHeaders(withoutContentEncodingAndContentLength(response.getHeaders()));
        }

        ResponseDefinition responseToWrite = responseDefinitionBuilder.build();

        StubMapping mapping = new StubMapping(requestPattern, responseToWrite);
        mapping.setUuid(UUID.nameUUIDFromBytes(fileId.getBytes()));

        filesFileSource.writeBinaryFile(bodyFileName, bodyDecompressedIfRequired(response));
        mappingsFileSource.writeTextFile(mappingFileName, write(mapping));
    }
	private void writeRepeatsToMappingAndBodyFile(Request request, Response response, RequestPattern requestPattern,
			Integer requestReceivedCount) {
		String fileId = receivedRequestsFileIds.get(requestPattern);
		if (fileId == null) {
			fileId = idGenerator.generate();
			receivedRequestsFileIds.put(requestPattern, fileId);
		}
		writeRepeatsToMappingAndBodyFile(request, response, requestPattern, fileId, requestReceivedCount);

	}

	private void writeRepeatsToMappingAndBodyFile(Request request, Response response, RequestPattern requestPattern,
			String fileId, Integer requestReceivedCount) {

		String fileIdSequence = fileId + "." + requestReceivedCount;
		String mappingFileName = UniqueFilenameGenerator.generate(request, "mapping", fileIdSequence);
		String bodyFileName = UniqueFilenameGenerator.generate(request, "body", fileIdSequence);

		ResponseDefinitionBuilder responseDefinitionBuilder = responseDefinition().withStatus(response.getStatus())
				.withBodyFile(bodyFileName);
		if (response.getHeaders().size() > 0) {
			responseDefinitionBuilder.withHeaders(withoutContentEncodingAndContentLength(response.getHeaders()));
		}

		ResponseDefinition responseToWrite = responseDefinitionBuilder.build();

		StubMapping mapping = new StubMapping(requestPattern, responseToWrite);
		mapping.setUuid(UUID.nameUUIDFromBytes(fileIdSequence.getBytes()));

		mapping.setScenarioName(fileId);
		mapping.setRequiredScenarioState("" + ((requestReceivedCount < 2) ? Scenario.STARTED : requestReceivedCount));
		mapping.setNewScenarioState("" + (requestReceivedCount + 1));
		filesFileSource.writeBinaryFile(bodyFileName, bodyDecompressedIfRequired(response));
		mappingsFileSource.writeTextFile(mappingFileName, write(mapping));
	}
    private HttpHeaders withoutContentEncodingAndContentLength(HttpHeaders httpHeaders) {
        return new HttpHeaders(filter(httpHeaders.all(), new Predicate<HttpHeader>() {
            public boolean apply(HttpHeader header) {
                return !header.keyEquals("Content-Encoding") && !header.keyEquals("Content-Length");
            }
        }));
    }

    private byte[] bodyDecompressedIfRequired(Response response) {
        if (response.getHeaders().getHeader("Content-Encoding").containsValue("gzip")) {
            return Gzip.unGzip(response.getBody());
        }

        return response.getBody();
    }

    private boolean requestNotAlreadyReceived(RequestPattern requestPattern) {
        VerificationResult verificationResult = admin.countRequestsMatching(requestPattern);
        verificationResult.assertRequestJournalEnabled();
        return (verificationResult.getCount() <= 1);
    }
	private int getRequestReceivedCount(RequestPattern requestPattern) {
		VerificationResult verificationResult = admin.countRequestsMatching(requestPattern);
		verificationResult.assertRequestJournalEnabled();
		return verificationResult.getCount();
	}
    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

}
