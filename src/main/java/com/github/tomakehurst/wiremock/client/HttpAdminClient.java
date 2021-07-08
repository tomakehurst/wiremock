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

import com.github.tomakehurst.wiremock.admin.*;
import com.github.tomakehurst.wiremock.admin.model.*;
import com.github.tomakehurst.wiremock.admin.tasks.*;
import com.github.tomakehurst.wiremock.common.*;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpStatus;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.recording.RecordSpecBuilder;
import com.github.tomakehurst.wiremock.recording.RecordingStatusResult;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.recording.RecordSpec;
import com.github.tomakehurst.wiremock.security.ClientAuthenticator;
import com.github.tomakehurst.wiremock.security.NotAuthorisedException;
import com.github.tomakehurst.wiremock.stubbing.StubImport;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.common.HttpClientUtils.getEntityAsStringAndCloseStream;
import static com.github.tomakehurst.wiremock.security.NoClientAuthenticator.noClientAuthenticator;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.http.HttpHeaders.HOST;

public class HttpAdminClient implements Admin {

    private static final String ADMIN_URL_PREFIX = "%s://%s:%d%s/__admin";

    private final String scheme;
    private final String host;
    private final int port;
    private final String urlPathPrefix;
    private final String hostHeader;
    private final ClientAuthenticator authenticator;

    private final AdminRoutes adminRoutes;

    private final CloseableHttpClient httpClient;

    public HttpAdminClient(String scheme, String host, int port) {
        this(scheme, host, port, "");
    }

    public HttpAdminClient(String host, int port, String urlPathPrefix) {
        this("http", host, port, urlPathPrefix);
    }

    public HttpAdminClient(String scheme, String host, int port, String urlPathPrefix) {
        this(scheme, host, port, urlPathPrefix, null, null, 0, noClientAuthenticator());
    }

    public HttpAdminClient(String scheme, String host, int port, String urlPathPrefix, String hostHeader) {
        this(scheme, host, port, urlPathPrefix, hostHeader, null, 0, noClientAuthenticator());
    }

    public HttpAdminClient(String scheme,
                           String host,
                           int port,
                           String urlPathPrefix,
                           String hostHeader,
                           String proxyHost,
                           int proxyPort) {
        this(scheme, host, port, urlPathPrefix, hostHeader, proxyHost, proxyPort, noClientAuthenticator());
    }

    public HttpAdminClient(String scheme,
                           String host,
                           int port,
                           String urlPathPrefix,
                           String hostHeader,
                           String proxyHost,
                           int proxyPort,
                           ClientAuthenticator authenticator) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.urlPathPrefix = urlPathPrefix;
        this.hostHeader = hostHeader;
        this.authenticator = authenticator;

        adminRoutes = AdminRoutes.defaults();

        httpClient = HttpClientFactory.createClient(createProxySettings(proxyHost, proxyPort));
    }

    public HttpAdminClient(String host, int port) {
        this(host, port, "");
    }

    private static StringEntity jsonStringEntity(String json) {
        return new StringEntity(json, UTF_8.name());
    }

    @Override
    public void addStubMapping(StubMapping stubMapping) {
        if (stubMapping.getRequest().hasInlineCustomMatcher()) {
            throw new AdminException("Custom matchers can't be used when administering a remote WireMock server. " +
                    "Use WireMockRule.stubFor() or WireMockServer.stubFor() to administer the local instance.");
        }

        executeRequest(
                adminRoutes.requestSpecForTask(CreateStubMappingTask.class),
                PathParams.empty(),
                stubMapping,
                Void.class
        );
    }

    @Override
    public void editStubMapping(StubMapping stubMapping) {
        postJsonAssertOkAndReturnBody(
                urlFor(OldEditStubMappingTask.class),
                Json.write(stubMapping));
    }

    @Override
    public void removeStubMapping(StubMapping stubbMapping) {
        postJsonAssertOkAndReturnBody(
                urlFor(OldRemoveStubMappingTask.class),
                Json.write(stubbMapping));
    }

    @Override
    public ListStubMappingsResult listAllStubMappings() {
        return executeRequest(
                adminRoutes.requestSpecForTask(GetAllStubMappingsTask.class),
                ListStubMappingsResult.class
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public SingleStubMappingResult getStubMapping(UUID id) {
        return executeRequest(
                adminRoutes.requestSpecForTask(GetStubMappingTask.class),
                PathParams.single("id", id),
                SingleStubMappingResult.class
        );
    }

    @Override
    public void saveMappings() {
        postJsonAssertOkAndReturnBody(urlFor(SaveMappingsTask.class), null);
    }

    @Override
    public void resetAll() {
        postJsonAssertOkAndReturnBody(urlFor(ResetTask.class), null);
    }

    @Override
    public void resetRequests() {
        executeRequest(adminRoutes.requestSpecForTask(ResetRequestsTask.class));
    }

    @Override
    public void resetScenarios() {
        executeRequest(adminRoutes.requestSpecForTask(ResetScenariosTask.class));
    }

    @Override
    public void resetMappings() {
        executeRequest(adminRoutes.requestSpecForTask(ResetStubMappingsTask.class));
    }

    @Override
    public void resetToDefaultMappings() {
        postJsonAssertOkAndReturnBody(urlFor(ResetToDefaultMappingsTask.class), null);
    }

    @Override
    public GetServeEventsResult getServeEvents() {
        return executeRequest(
                adminRoutes.requestSpecForTask(GetAllRequestsTask.class),
                GetServeEventsResult.class
        );
    }
    
    @Override
    public GetServeEventsResult getServeEvents(UUID stubMappingUUID) {
        return executeRequest(
            adminRoutes.requestSpecForTask(FindRequestsByStubMappingUUIDTask.class),
            GetServeEventsResult.class
        );
    }

    @Override
    public SingleServedStubResult getServedStub(UUID id) {
        return executeRequest(
                adminRoutes.requestSpecForTask(GetServedStubTask.class),
                PathParams.single("id", id),
                SingleServedStubResult.class
        );
    }

    @Override
    public VerificationResult countRequestsMatching(RequestPattern requestPattern) {
        String body = postJsonAssertOkAndReturnBody(
                urlFor(GetRequestCountTask.class),
                Json.write(requestPattern));
        return VerificationResult.from(body);
    }

    @Override
    public FindRequestsResult findRequestsMatching(RequestPattern requestPattern) {
        String body = postJsonAssertOkAndReturnBody(
                urlFor(FindRequestsTask.class),
                Json.write(requestPattern));
        return Json.read(body, FindRequestsResult.class);
    }

    @Override
    public FindRequestsResult findUnmatchedRequests() {
        String body = getJsonAssertOkAndReturnBody(
                urlFor(FindUnmatchedRequestsTask.class));
        return Json.read(body, FindRequestsResult.class);
    }

    @Override
    public void removeServeEvent(UUID eventId) {
        executeRequest(
                adminRoutes.requestSpecForTask(RemoveServeEventTask.class),
                PathParams.single("id", eventId),
                Void.class
        );
    }

    @Override
    public FindServeEventsResult removeServeEventsMatching(RequestPattern requestPattern) {
        String body = postJsonAssertOkAndReturnBody(
                urlFor(RemoveServeEventsByRequestPatternTask.class),
                Json.write(requestPattern));
        return Json.read(body, FindServeEventsResult.class);
    }

    @Override
    public FindServeEventsResult removeServeEventsForStubsMatchingMetadata(StringValuePattern metadataPattern) {
        String body = postJsonAssertOkAndReturnBody(
                urlFor(RemoveServeEventsByStubMetadataTask.class),
                Json.write(metadataPattern));
        return Json.read(body, FindServeEventsResult.class);
    }

    @Override
    public FindNearMissesResult findNearMissesForUnmatchedRequests() {
        String body = getJsonAssertOkAndReturnBody(urlFor(FindNearMissesForUnmatchedTask.class));
        return Json.read(body, FindNearMissesResult.class);
    }

    @Override
    public GetScenariosResult getAllScenarios() {
        return executeRequest(
            adminRoutes.requestSpecForTask(GetAllScenariosTask.class),
            GetScenariosResult.class
        );
    }

    @Override
    public FindNearMissesResult findTopNearMissesFor(LoggedRequest loggedRequest) {
        String body = postJsonAssertOkAndReturnBody(
                urlFor(FindNearMissesForRequestTask.class),
                Json.write(loggedRequest)
        );

        return Json.read(body, FindNearMissesResult.class);
    }

    @Override
    public FindNearMissesResult findTopNearMissesFor(RequestPattern requestPattern) {
        String body = postJsonAssertOkAndReturnBody(
                urlFor(FindNearMissesForRequestPatternTask.class),
                Json.write(requestPattern)
        );

        return Json.read(body, FindNearMissesResult.class);
    }

    @Override
    public void updateGlobalSettings(GlobalSettings settings) {
        postJsonAssertOkAndReturnBody(
                urlFor(GlobalSettingsUpdateTask.class),
                Json.write(settings));
    }

    @Override
    public SnapshotRecordResult snapshotRecord() {
        String body = postJsonAssertOkAndReturnBody(
            urlFor(SnapshotTask.class),
            "");

        return Json.read(body, SnapshotRecordResult.class);
    }

    @Override
    public SnapshotRecordResult snapshotRecord(RecordSpecBuilder spec) {
        return snapshotRecord(spec.build());
    }

    @Override
    public SnapshotRecordResult snapshotRecord(RecordSpec spec) {
        String body = postJsonAssertOkAndReturnBody(
            urlFor(SnapshotTask.class),
            Json.write(spec));

        return Json.read(body, SnapshotRecordResult.class);
    }

    @Override
    public void startRecording(String targetBaseUrl) {
        startRecording(RecordSpec.forBaseUrl(targetBaseUrl));
    }

    @Override
    public void startRecording(RecordSpec recordSpec) {
        postJsonAssertOkAndReturnBody(
            urlFor(StartRecordingTask.class),
            Json.write(recordSpec));
    }

    @Override
    public void startRecording(RecordSpecBuilder recordSpec) {
        startRecording(recordSpec.build());
    }

    @Override
    public SnapshotRecordResult stopRecording() {
        String body = postJsonAssertOkAndReturnBody(
            urlFor(StopRecordingTask.class),
            "");

        return Json.read(body, SnapshotRecordResult.class);
    }

    @Override
    public RecordingStatusResult getRecordingStatus() {
        return executeRequest(adminRoutes.requestSpecForTask(GetRecordingStatusTask.class), RecordingStatusResult.class);
    }

    @Override
    public Options getOptions() {
        return new WireMockConfiguration().port(port).bindAddress(host);
    }

    @Override
    public void shutdownServer() {
        postJsonAssertOkAndReturnBody(urlFor(ShutdownServerTask.class), null);
    }

    @Override
    public ListStubMappingsResult findAllStubsByMetadata(StringValuePattern pattern) {
        return executeRequest(
            adminRoutes.requestSpecForTask(FindStubMappingsByMetadataTask.class),
            pattern,
            ListStubMappingsResult.class
        );
    }

    @Override
    public void removeStubsByMetadata(StringValuePattern pattern) {
        executeRequest(
            adminRoutes.requestSpecForTask(RemoveStubMappingsByMetadataTask.class),
            pattern,
            Void.class
        );
    }

    @Override
    public void importStubs(StubImport stubImport) {
        executeRequest(
                adminRoutes.requestSpecForTask(ImportStubMappingsTask.class),
                stubImport,
                Void.class
        );
    }

    @Override
    public GetGlobalSettingsResult getGlobalSettings() {
        return executeRequest(adminRoutes.requestSpecForTask(GetGlobalSettingsTask.class), GetGlobalSettingsResult.class);
    }

    public int port() {
        return port;
    }

    private ProxySettings createProxySettings(String proxyHost, int proxyPort) {
        if(StringUtils.isNotBlank(proxyHost)) {
            return new ProxySettings(proxyHost, proxyPort);
        }
        return ProxySettings.NO_PROXY;
    }

    private String postJsonAssertOkAndReturnBody(String url, String json) {
        HttpPost post = new HttpPost(url);
        if (json != null) {
            post.setEntity(jsonStringEntity(json));
        }

        return safelyExecuteRequest(url, post);
    }

    protected String getJsonAssertOkAndReturnBody(String url) {
        HttpGet get = new HttpGet(url);
        return safelyExecuteRequest(url, get);
    }

    private void executeRequest(RequestSpec requestSpec) {
        executeRequest(requestSpec, PathParams.empty(), null, Void.class);
    }

    private <B, R> R executeRequest(RequestSpec requestSpec, B requestBody, Class<R> responseType) {
        return executeRequest(requestSpec, PathParams.empty(), requestBody, responseType);
    }

    private <B, R> R executeRequest(RequestSpec requestSpec, Class<R> responseType) {
        return executeRequest(requestSpec, PathParams.empty(), null, responseType);
    }

    private <B, R> R executeRequest(RequestSpec requestSpec, PathParams pathParams, Class<R> responseType) {
        return executeRequest(requestSpec, pathParams, null, responseType);
    }

    private <B, R> R executeRequest(RequestSpec requestSpec, PathParams pathParams, B requestBody, Class<R> responseType) {
        String url = String.format(ADMIN_URL_PREFIX + requestSpec.path(pathParams), scheme, host, port, urlPathPrefix);
        RequestBuilder requestBuilder = RequestBuilder
                .create(requestSpec.method().getName())
                .setUri(url);

        if (requestBody != null) {
            requestBuilder.setEntity(jsonStringEntity(Json.write(requestBody)));
        }

        String responseBodyString = safelyExecuteRequest(url, requestBuilder.build());

        return responseType == Void.class ?
                null :
                Json.read(responseBodyString, responseType);
    }

    private String safelyExecuteRequest(String url, HttpUriRequest request) {
        if (hostHeader != null) {
            request.addHeader(HOST, hostHeader);
        }

        List<HttpHeader> httpHeaders = authenticator.generateAuthHeaders();
        for (HttpHeader header: httpHeaders) {
            for (String value: header.values()) {
                request.addHeader(header.key(), value);
            }
        }

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpStatus.isServerError(statusCode)) {
                throw new VerificationException(
                        "Expected status 2xx for " + url + " but was " + statusCode);
            }

            if (statusCode == 401) {
                throw new NotAuthorisedException();
            }

            String body = getEntityAsStringAndCloseStream(response);
            if (HttpStatus.isClientError(statusCode)) {
                Errors errors = Json.read(body, Errors.class);
                throw ClientError.fromErrors(errors);
            }

            return body;
        } catch (Exception e) {
            return throwUnchecked(e, String.class);
        }
    }

    private String urlFor(Class<? extends AdminTask> taskClass) {
        RequestSpec requestSpec = adminRoutes.requestSpecForTask(taskClass);
        checkNotNull(requestSpec, "No admin task URL is registered for " + taskClass.getSimpleName());
        return String.format(ADMIN_URL_PREFIX + requestSpec.path(), scheme, host, port, urlPathPrefix);
    }
}
