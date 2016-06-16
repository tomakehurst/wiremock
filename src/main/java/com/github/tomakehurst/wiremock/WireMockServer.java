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

import com.github.tomakehurst.wiremock.client.LocalMappingBuilder;
import com.github.tomakehurst.wiremock.client.RemoteMappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FatalStartupException;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.common.ProxySettings;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.core.Container;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockApp;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.global.GlobalSettingsHolder;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.junit.LocalStubbing;
import com.github.tomakehurst.wiremock.matching.LocalRequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource;
import com.github.tomakehurst.wiremock.standalone.MappingsLoader;
import com.github.tomakehurst.wiremock.stubbing.ListStubMappingsResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.stubbing.StubMappingJsonRecorder;
import com.github.tomakehurst.wiremock.verification.*;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.base.Preconditions.checkState;

public class WireMockServer implements Container, LocalStubbing, Admin {

	public static final String FILES_ROOT = "__files";
    public static final String MAPPINGS_ROOT = "mappings";

	private final WireMockApp wireMockApp;
    private final StubRequestHandler stubRequestHandler;

	private final HttpServer httpServer;
    private final FileSource fileSource;
	private final Notifier notifier;

    private final Options options;

    protected final WireMock client;

    public WireMockServer(Options options) {
        this.options = options;
        this.fileSource = options.filesRoot();
        this.notifier = options.notifier();

        JsonFileMappingsSource mappingsSource = new JsonFileMappingsSource(fileSource.child(MAPPINGS_ROOT));

        wireMockApp = new WireMockApp(
                options.browserProxyingEnabled(),
                mappingsSource,
                mappingsSource,
                options.requestJournalDisabled(),
                options.maxRequestJournalEntries(),
                options.extensionsOfType(ResponseDefinitionTransformer.class),
                options.extensionsOfType(RequestMatcherExtension.class),
                fileSource,
                this
        );

        AdminRequestHandler adminRequestHandler = new AdminRequestHandler(
                wireMockApp,
                new BasicResponseRenderer()
        );
        stubRequestHandler = new StubRequestHandler(
                wireMockApp,
                new StubResponseRenderer(
                        fileSource.child(FILES_ROOT),
                        wireMockApp.getGlobalSettingsHolder(),
                        new ProxyResponseRenderer(
                                options.proxyVia(),
                                options.httpsSettings().trustStore(),
                                options.shouldPreserveHostHeader(),
                                options.proxyHostHeader()
                        ),
                        ImmutableList.copyOf(options.extensionsOfType(ResponseTransformer.class).values()))
        );
        HttpServerFactory httpServerFactory = options.httpServerFactory();
        httpServer = httpServerFactory.buildHttpServer(
                options,
                adminRequestHandler,
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
                isRunning(),
                "Not listening on HTTP port. The WireMock server is most likely stopped"
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

    public boolean isRunning() {
        return httpServer.isRunning();
    }

    @Override
    public void givenThat(RemoteMappingBuilder mappingBuilder) {
        client.register(mappingBuilder);
    }

    @Override
    public void stubFor(RemoteMappingBuilder mappingBuilder) {
        givenThat(mappingBuilder);
    }

    @Override
    public void editStub(RemoteMappingBuilder mappingBuilder) {
        client.editStubMapping(mappingBuilder);
    }

    @Override
    public void removeStubMapping(RemoteMappingBuilder mappingBuilder) {
        client.removeStubMapping(mappingBuilder);
    }

    @Override
    public void givenThat(LocalMappingBuilder mappingBuilder) {
        stubFor(mappingBuilder);
    }

    @Override
    public void stubFor(LocalMappingBuilder mappingBuilder) {
        client.register(mappingBuilder.build());
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
    public void verify(LocalRequestPatternBuilder requestPatternBuilder) {
        verify(requestPatternBuilder.getUnderlyingBuilder());
    }

    @Override
    public void verify(int count, LocalRequestPatternBuilder requestPatternBuilder) {
        verify(count, requestPatternBuilder.getUnderlyingBuilder());
    }

    @Override
    public List<LoggedRequest> findAll(RequestPatternBuilder requestPatternBuilder) {
        return client.find(requestPatternBuilder);
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
    public void removeStubMapping(StubMapping stubbMapping) {
        wireMockApp.removeStubMapping(stubbMapping);
    }

    @Override
    public ListStubMappingsResult listAllStubMappings() {
        return wireMockApp.listAllStubMappings();
    }

    @Override
    public void saveMappings() {
        wireMockApp.saveMappings();
    }

    @Override
    public void resetMappings() {
        wireMockApp.resetMappings();
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
    public void resetScenarios() {
        wireMockApp.resetScenarios();
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
    public void updateGlobalSettings(GlobalSettings newSettings) {
        wireMockApp.updateGlobalSettings(newSettings);
    }

    @Override
    public FindNearMissesResult findNearMissesForUnmatchedRequests() {
        return wireMockApp.findNearMissesForUnmatchedRequests();
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
    public void shutdownServer() {
        shutdown();
    }
}
