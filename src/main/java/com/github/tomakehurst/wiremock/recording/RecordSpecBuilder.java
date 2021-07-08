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
package com.github.tomakehurst.wiremock.recording;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Arrays.asList;

public class RecordSpecBuilder {

    private String targetBaseUrl;
    private RequestPatternBuilder filterRequestPatternBuilder;
    private List<UUID> filterIds;
    private Map<String, CaptureHeadersSpec> headers = newLinkedHashMap();
    private RequestBodyPatternFactory requestBodyPatternFactory;
    private long maxTextBodySize = ResponseDefinitionBodyMatcher.DEFAULT_MAX_TEXT_SIZE;
    private long maxBinaryBodySize = ResponseDefinitionBodyMatcher.DEFAULT_MAX_BINARY_SIZE;
    private boolean persistentStubs = true;
    private boolean repeatsAsScenarios = true;
    private List<String> transformerNames;
    private Parameters transformerParameters;
    private boolean allowNonProxied;
    private boolean captureAllHeaders = false;

    public RecordSpecBuilder forTarget(String targetBaseUrl) {
        this.targetBaseUrl = targetBaseUrl;
        return this;
    }

    public RecordSpecBuilder onlyRequestsMatching(RequestPatternBuilder filterRequestPattern) {
        this.filterRequestPatternBuilder = filterRequestPattern;
        return this;
    }

    public RecordSpecBuilder onlyRequestIds(List<UUID> filterIds) {
        this.filterIds = filterIds;
        return this;
    }

    public RecordSpecBuilder extractTextBodiesOver(long size) {
        this.maxTextBodySize = size;
        return this;
    }

    public RecordSpecBuilder extractBinaryBodiesOver(long size) {
        this.maxBinaryBodySize = size;
        return this;
    }

    public RecordSpecBuilder makeStubsPersistent(boolean persistent) {
        this.persistentStubs = persistent;
        return this;
    }

    public RecordSpecBuilder ignoreRepeatRequests() {
        this.repeatsAsScenarios = false;
        return this;
    }

    public RecordSpecBuilder transformers(String... transformerName) {
        return transformers(asList(transformerName));
    }

    public RecordSpecBuilder transformers(List<String> transformerName) {
        this.transformerNames = transformerName;
        return this;
    }

    public RecordSpecBuilder transformerParameters(Parameters parameters) {
        this.transformerParameters = parameters;
        return this;
    }

    public RecordSpecBuilder captureHeader(String key) {
        return captureHeader(key, null);
    }

    public RecordSpecBuilder captureHeader(String key, Boolean caseInsensitive) {
        headers.put(key, new CaptureHeadersSpec(caseInsensitive));
        return this;
    }

    public RecordSpecBuilder chooseBodyMatchTypeAutomatically() {
        return chooseBodyMatchTypeAutomatically(null, null, null);
    }

    public RecordSpecBuilder chooseBodyMatchTypeAutomatically(Boolean ignoreArrayOrder, Boolean ignoreExtraElements, Boolean caseInsensitive) {
        this.requestBodyPatternFactory = new RequestBodyAutomaticPatternFactory(ignoreArrayOrder, ignoreExtraElements, caseInsensitive);
        return this;
    }

    public RecordSpecBuilder matchRequestBodyWithEqualToJson() {
        return matchRequestBodyWithEqualToJson(null, null);
    }

    public RecordSpecBuilder matchRequestBodyWithEqualToJson(Boolean ignoreArrayOrder, Boolean ignoreExtraElements) {
        this.requestBodyPatternFactory = new RequestBodyEqualToJsonPatternFactory(ignoreArrayOrder, ignoreExtraElements);
        return this;
    }

    public RecordSpecBuilder matchRequestBodyWithEqualToXml() {
        this.requestBodyPatternFactory = new RequestBodyEqualToXmlPatternFactory();
        return this;
    }

    public RecordSpecBuilder matchRequestBodyWithEqualTo() {
        return matchRequestBodyWithEqualTo(null);
    }

    public RecordSpecBuilder matchRequestBodyWithEqualTo(Boolean caseInsensitive) {
        this.requestBodyPatternFactory = new RequestBodyEqualToPatternFactory(caseInsensitive);
        return this;
    }

    public RecordSpecBuilder allowNonProxied(boolean allowNonProxied) {
        this.allowNonProxied = allowNonProxied;
        return this;
    }

    public RecordSpecBuilder captureAllRequestHeaders(boolean captureAllHeaders){
        this.captureAllHeaders = captureAllHeaders;
        return this;
    }

    public RecordSpec build() {
        RequestPattern filterRequestPattern = filterRequestPatternBuilder != null ?
            filterRequestPatternBuilder.build() :
            null;
        ProxiedServeEventFilters filters = filterRequestPatternBuilder != null || filterIds != null || allowNonProxied ?
            new ProxiedServeEventFilters(filterRequestPattern, filterIds, allowNonProxied) :
            null;

        ResponseDefinitionBodyMatcher responseDefinitionBodyMatcher = new ResponseDefinitionBodyMatcher(maxTextBodySize, maxBinaryBodySize);

        return new RecordSpec(
            targetBaseUrl,
            filters,
            headers.isEmpty() ? null : headers,
            requestBodyPatternFactory,
            responseDefinitionBodyMatcher,
            SnapshotOutputFormatter.FULL,
            persistentStubs,
            repeatsAsScenarios,
            transformerNames,
            transformerParameters,
            captureAllHeaders
        );
    }
}
