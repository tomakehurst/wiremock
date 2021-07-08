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
package com.github.tomakehurst.wiremock;

import com.github.tomakehurst.wiremock.admin.model.*;
import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FatalStartupException;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.common.ProxySettings;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.core.Container;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockApp;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.global.GlobalSettingsHolder;
import com.github.tomakehurst.wiremock.http.HttpServer;
import com.github.tomakehurst.wiremock.http.HttpServerFactory;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.StubRequestHandler;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.recording.RecordingStatusResult;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.recording.RecordSpec;
import com.github.tomakehurst.wiremock.recording.RecordSpecBuilder;
import com.github.tomakehurst.wiremock.standalone.MappingsLoader;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubImport;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.stubbing.StubMappingJsonRecorder;
import com.github.tomakehurst.wiremock.verification.*;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.base.Preconditions.checkState;

public class WireMockServer implements Container, Stubbing, Admin {

    private final WireMockApp wireMockApp;
    private final StubRequestHandler stubRequestHandler;

	private final HttpServer httpServer;
	private final Notifier notifier;

    protected final Options options;

    protected final WireMock client;

    public WireMockServer(Options options) {
        this.options = options;
        this.notifier = options.notifier();

        wireMockApp = new WireMockApp(options, this);

        this.stubRequestHandler = wireMockApp.buildStubRequestHandler();
        HttpServerFactory httpServerFactory = options.httpServerFactory();
        httpServer = httpServerFactory.buildHttpServer(
                options,
                wireMockApp.buildAdminRequestHandler(),
                stubRequestHandler
        );

        client = new WireMock(wireMockApp);
    }

    public WireMockServer(int port, Integer httpsPort, FileSource fileSource, boolean enableBrowserProxying, ProxySettings proxySettings, Notifier notifier) {
        this(wireMockConfig()
                .port(port)
                .httpsPort(httpsPort)
                .fileSource(fileSource)
                .enableBrowserProxying(enableBrowserProxying)
                .proxyVia(proxySettings)
                .notifier(notifier));
    }

	public WireMockServer(int port, FileSource fileSource, boolean enableBrowserProxying, ProxySettings proxySettings) {
        this(wireMockConfig()
                .port(port)
                .fileSource(fileSource)
                .enableBrowserProxying(enableBrowserProxying)
                .proxyVia(proxySettings));
	}

    public WireMockServer(int port, FileSource fileSource, boolean enableBrowserProxying) {
        this(wireMockConfig()
                .port(port)
                .fileSource(fileSource)
                .enableBrowserProxying(enableBrowserProxying));
    }

    public WireMockServer(int port) {
		this(wireMockConfig().port(port));
	}

    public WireMockServer(int port, Integer httpsPort) {
        this(wireMockConfig().port(port).httpsPort(httpsPort));
    }

    public WireMockServer() {
		this(wireMockConfig());
	}

	public void loadMappingsUsing(final MappingsLoader mappingsLoader) {
        wireMockApp.loadMappingsUsing(mappingsLoader);
	}

    public GlobalSettingsHolder getGlobalSettingsHolder() {
        return wireMockApp.getGlobalSettingsHolder();
    }

    public void addMockServiceRequestListener(RequestListener listener) {
		stubRequestHandler.addRequestListener(listener);
	}

	public void enableRecordMappings(FileSource mappingsFileSource, FileSource filesFileSource) {
	    addMockServiceRequestListener(
                new StubMappingJsonRecorder(mappingsFileSource, filesFileSource, wireMockApp, options.matchingHeaders()));
        notifier.info("Recording mappings to " + mappingsFileSource.getPath());
	}

    public void stop() {
        httpServer.stop();
	}

	public void start() {
        try {
		    httpServer.start();
        } catch (Exception e) {
            throw new FatalStartupException(e);
        }
	}

    /**
     * Gracefully shutdown the server.
     *
     * This method assumes it is being called as the result of an incoming HTTP request.
     */
    @Override
    public void shutdown() {
        final WireMockServer server = this;
        Thread shutdownThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // We have to sleep briefly to finish serving the shutdown request before stopping the server, as
                    // there's no support in Jetty for shutting down after the current request.
                    // See http://stackoverflow.com/questions/4650713
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                server.stop();
            }
        });
        shutdownThread.start();
    }

    public int port() {
        checkState(
            isRunning() && !options.getHttpDisabled(),
            "Not listening on HTTP port. Either HTTP is not enabled or the WireMock server is stopped."
        );
        return httpServer.port();
    }

    public int httpsPort() {
        checkState(
                isRunning() && options.httpsSettings().enabled(),
                "Not listening on HTTPS port. Either HTTPS is not enabled or the WireMock server is stopped."
        );
        return httpServer.httpsPort();
    }

    public String url(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return String.format("%s%s", baseUrl(), path);
    }

    public String baseUrl() {
        boolean https = options.httpsSettings().enabled();
        String protocol = https ? "https" : "http";
        int port = https ? httpsPort() : port();

        return String.format("%s://localhost:%d", protocol, port);
    }

    public boolean isRunning() {
        return httpServer.isRunning();
    }

    @Override
    public StubMapping givenThat(MappingBuilder mappingBuilder) {
        return client.register(mappingBuilder);
    }

    @Override
    public StubMapping stubFor(MappingBuilder mappingBuilder) {
        return givenThat(mappingBuilder);
    }

    @Override
    public void editStub(MappingBuilder mappingBuilder) {
        client.editStubMapping(mappingBuilder);
    }

    @Override
    public void removeStub(MappingBuilder mappingBuilder) {
        client.removeStubMapping(mappingBuilder);
    }

    @Override
    public void removeStub(StubMapping stubMapping) {
        client.removeStubMapping(stubMapping);
    }

    @Override
    public List<StubMapping> getStubMappings() {
        return client.allStubMappings().getMappings();
    }

    @Override
    public StubMapping getSingleStubMapping(UUID id) {
        return client.getStubMapping(id).getItem();
    }

    @Override
    public List<StubMapping> findStubMappingsByMetadata(StringValuePattern pattern) {
        return client.findAllStubsByMetadata(pattern);
    }

    @Override
    public void removeStubMappingsByMetadata(StringValuePattern pattern) {
        client.removeStubsByMetadataPattern(pattern);
    }

    @Override
    public void removeStubMapping(StubMapping stubMapping){
        wireMockApp.removeStubMapping(stubMapping);
    }

    @Override
    public void verify(RequestPatternBuilder requestPatternBuilder) {
        client.verifyThat(requestPatternBuilder);
    }

    @Override
    public void verify(int count, RequestPatternBuilder requestPatternBuilder) {
        client.verifyThat(count, requestPatternBuilder);
    }

    @Override
    public void verify(CountMatchingStrategy countMatchingStrategy, RequestPatternBuilder requestPatternBuilder) {
        client.verifyThat(countMatchingStrategy, requestPatternBuilder);
    }

    @Override
    public List<LoggedRequest> findAll(RequestPatternBuilder requestPatternBuilder) {
        return client.find(requestPatternBuilder);
    }

    @Override
    public List<ServeEvent> getAllServeEvents() {
        return client.getServeEvents();
    }
    
    @Override
    public List<ServeEvent> getAllServeEvents(UUID stubMappingUUID) {
        return client.getServeEvents(stubMappingUUID);
    }

    @Override
    public void setGlobalFixedDelay(int milliseconds) {
        client.setGlobalFixedDelayVariable(milliseconds);
    }

    @Override
    public List<LoggedRequest> findAllUnmatchedRequests() {
        return client.findAllUnmatchedRequests();
    }

    @Override
    public List<NearMiss> findNearMissesForAllUnmatchedRequests() {
        return client.findNearMissesForAllUnmatchedRequests();
    }

    @Override
    public List<NearMiss> findAllNearMissesFor(RequestPatternBuilder requestPatternBuilder) {
        return client.findAllNearMissesFor(requestPatternBuilder);
    }

    @Override
    public List<NearMiss> findNearMissesFor(LoggedRequest loggedRequest) {
        return client.findTopNearMissesFor(loggedRequest);
    }

    @Override
    public void addStubMapping(StubMapping stubMapping) {
        wireMockApp.addStubMapping(stubMapping);
    }

    @Override
    public void editStubMapping(StubMapping stubMapping) {
        wireMockApp.editStubMapping(stubMapping);
    }

    @Override
    public ListStubMappingsResult listAllStubMappings() {
        return wireMockApp.listAllStubMappings();
    }

    @Override
    public SingleStubMappingResult getStubMapping(UUID id) {
        return wireMockApp.getStubMapping(id);
    }

    @Override
    public void saveMappings() {
        wireMockApp.saveMappings();
    }

    @Override
    public void resetAll() {
        wireMockApp.resetAll();
    }

    @Override
    public void resetRequests() {
        wireMockApp.resetRequests();
    }

    @Override
    public void resetToDefaultMappings() {
        wireMockApp.resetToDefaultMappings();
    }

    @Override
    public GetServeEventsResult getServeEvents() {
        return wireMockApp.getServeEvents();
    }
    
    @Override
    public GetServeEventsResult getServeEvents(UUID stubMappingUUID) {
        return wireMockApp.getServeEvents(stubMappingUUID);
    }

    @Override
    public SingleServedStubResult getServedStub(UUID id) {
        return wireMockApp.getServedStub(id);
    }

    @Override
    public void resetScenarios() {
        wireMockApp.resetScenarios();
    }

    @Override
    public void resetMappings() {
        wireMockApp.resetMappings();
    }

    @Override
    public VerificationResult countRequestsMatching(RequestPattern requestPattern) {
        return wireMockApp.countRequestsMatching(requestPattern);
    }

    @Override
    public FindRequestsResult findRequestsMatching(RequestPattern requestPattern) {
        return wireMockApp.findRequestsMatching(requestPattern);
    }

    @Override
    public FindRequestsResult findUnmatchedRequests() {
        return wireMockApp.findUnmatchedRequests();
    }

    @Override
    public void removeServeEvent(UUID eventId) {
        wireMockApp.removeServeEvent(eventId);
    }

    @Override
    public FindServeEventsResult removeServeEventsMatching(RequestPattern requestPattern) {
        return wireMockApp.removeServeEventsMatching(requestPattern);
    }

    @Override
    public FindServeEventsResult removeServeEventsForStubsMatchingMetadata(StringValuePattern metadataPattern) {
        return wireMockApp.removeServeEventsForStubsMatchingMetadata(metadataPattern);
    }

    @Override
    public void updateGlobalSettings(GlobalSettings newSettings) {
        wireMockApp.updateGlobalSettings(newSettings);
    }

    @Override
    public FindNearMissesResult findNearMissesForUnmatchedRequests() {
        return wireMockApp.findNearMissesForUnmatchedRequests();
    }

    @Override
    public GetScenariosResult getAllScenarios() {
        return wireMockApp.getAllScenarios();
    }

    @Override
    public FindNearMissesResult findTopNearMissesFor(LoggedRequest loggedRequest) {
        return wireMockApp.findTopNearMissesFor(loggedRequest);
    }

    @Override
    public FindNearMissesResult findTopNearMissesFor(RequestPattern requestPattern) {
        return wireMockApp.findTopNearMissesFor(requestPattern);
    }

    @Override
    public void startRecording(String targetBaseUrl) {
        wireMockApp.startRecording(targetBaseUrl);
    }

    @Override
    public void startRecording(RecordSpec spec) {
        wireMockApp.startRecording(spec);
    }

    @Override
    public void startRecording(RecordSpecBuilder recordSpec) {
        wireMockApp.startRecording(recordSpec);
    }

    @Override
    public SnapshotRecordResult stopRecording() {
        return wireMockApp.stopRecording();
    }

    @Override
    public RecordingStatusResult getRecordingStatus() {
        return wireMockApp.getRecordingStatus();
    }

    @Override
    public SnapshotRecordResult snapshotRecord() {
        return wireMockApp.snapshotRecord();
    }

    @Override
    public SnapshotRecordResult snapshotRecord(RecordSpecBuilder spec) {
        return wireMockApp.snapshotRecord(spec);
    }

    @Override
    public SnapshotRecordResult snapshotRecord(RecordSpec spec) {
        return wireMockApp.snapshotRecord(spec);
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public void shutdownServer() {
        shutdown();
    }

    @Override
    public ListStubMappingsResult findAllStubsByMetadata(StringValuePattern pattern) {
        return wireMockApp.findAllStubsByMetadata(pattern);
    }

    @Override
    public void removeStubsByMetadata(StringValuePattern pattern) {
        wireMockApp.removeStubsByMetadata(pattern);
    }

    @Override
    public void importStubs(StubImport stubImport) {
        wireMockApp.importStubs(stubImport);
    }

    @Override
    public GetGlobalSettingsResult getGlobalSettings() {
        return wireMockApp.getGlobalSettings();
    }
}
