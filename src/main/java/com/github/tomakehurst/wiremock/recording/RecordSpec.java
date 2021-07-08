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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.extension.Parameters;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates options for generating and outputting StubMappings
 */
public class RecordSpec {

    // Target URL when using the recording API. Not applicable to snapshotting
    private final String targetBaseUrl;

    // Whitelist requests to generate StubMappings for
    private final ProxiedServeEventFilters filters;

    // Headers from the request to include in the stub mapping, if they match the corresponding matcher
    // If captureAllHeaders is enabled, this is treated as a BlackList instead of a WhiteList
    private final Map<String, CaptureHeadersSpec> captureHeaders;

    // Factory for the StringValuePattern that will be used to match request bodies
    private final RequestBodyPatternFactory requestBodyPatternFactory;

    // Criteria for extracting body from responses
    private final ResponseDefinitionBodyMatcher extractBodyCriteria;

    // How to format StubMappings in the response body
    private final SnapshotOutputFormatter outputFormat;

    // Whether to persist stub mappings
    private final Boolean persist;

    // Whether duplicate requests should be recorded as scenarios or just discarded
    private final Boolean repeatsAsScenarios;

    // Stub mapping transformers
    private final List<String> transformers;

    // Parameters for stub mapping transformers
    private final Parameters transformerParameters;

    // If this is set to true, all headers will be captured for each request and must match exactly to be replayed
    // To ignore headers add them to the captureHeaders Map
    private final Boolean captureAllHeaders;

    @JsonCreator
    public RecordSpec(
        @JsonProperty("targetBaseUrl") String targetBaseUrl,
        @JsonProperty("filters") ProxiedServeEventFilters filters,
        @JsonProperty("captureHeaders") Map<String, CaptureHeadersSpec> captureHeaders,
        @JsonProperty("requestBodyPattern") RequestBodyPatternFactory requestBodyPatternFactory,
        @JsonProperty("extractBodyCriteria") ResponseDefinitionBodyMatcher extractBodyCriteria,
        @JsonProperty("outputFormat") SnapshotOutputFormatter outputFormat,
        @JsonProperty("persist") Boolean persist,
        @JsonProperty("repeatsAsScenarios") Boolean repeatsAsScenarios,
        @JsonProperty("transformers") List<String> transformers,
        @JsonProperty("transformerParameters") Parameters transformerParameters,
        @JsonProperty("captureAllHeaders") Boolean captureAllHeaders) {
        this.targetBaseUrl = targetBaseUrl;
        this.filters = filters == null ? ProxiedServeEventFilters.ALLOW_ALL : filters;
        this.captureHeaders = captureHeaders;
        this.requestBodyPatternFactory = requestBodyPatternFactory == null ? RequestBodyAutomaticPatternFactory.DEFAULTS : requestBodyPatternFactory;
        this.extractBodyCriteria = extractBodyCriteria;
        this.outputFormat = outputFormat == null ? SnapshotOutputFormatter.FULL : outputFormat;
        this.persist = persist == null ? true : persist;
        this.repeatsAsScenarios = repeatsAsScenarios;
        this.transformers = transformers;
        this.transformerParameters = transformerParameters;
        this.captureAllHeaders = captureAllHeaders  == null ? false: captureAllHeaders;
    }

    private RecordSpec() {
        this(null, null, null, null, null, null, null, null, null, null,null);
    }

    public static final RecordSpec DEFAULTS = new RecordSpec();

    public static RecordSpec forBaseUrl(String targetBaseUrl) {
        return new RecordSpec(targetBaseUrl, null, null, null, null, null, null, true, null, null, null);
    }

    public String getTargetBaseUrl() {
        return targetBaseUrl;
    }

    public ProxiedServeEventFilters getFilters() { return filters; }

    public Map<String, CaptureHeadersSpec> getCaptureHeaders() { return captureHeaders; }

    public SnapshotOutputFormatter getOutputFormat() { return outputFormat; }

    @JsonProperty("persist")
    public boolean shouldPersist() { return persist; }

    @JsonIgnore
    public boolean shouldRecordRepeatsAsScenarios() {
        return repeatsAsScenarios == null ? true : repeatsAsScenarios;
    }

    public Boolean getRepeatsAsScenarios() {
        return repeatsAsScenarios;
    }

    public List<String> getTransformers() { return transformers; }

    public Parameters getTransformerParameters() { return transformerParameters; }

    public ResponseDefinitionBodyMatcher getExtractBodyCriteria() { return extractBodyCriteria; }

    public RequestBodyPatternFactory getRequestBodyPatternFactory() { return requestBodyPatternFactory; }

    public Boolean shouldCaptureAllHeaders() {
        return captureAllHeaders;
    }
}
