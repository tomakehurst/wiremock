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
package com.github.tomakehurst.wiremock.core;

import com.github.tomakehurst.wiremock.admin.AdminRoutes;
import com.github.tomakehurst.wiremock.admin.LimitAndOffsetPaginator;
import com.github.tomakehurst.wiremock.admin.model.*;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.Xml;
import com.github.tomakehurst.wiremock.extension.*;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.global.GlobalSettingsHolder;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.recording.*;
import com.github.tomakehurst.wiremock.standalone.MappingsLoader;
import com.github.tomakehurst.wiremock.stubbing.*;
import com.github.tomakehurst.wiremock.verification.*;
import com.github.tomakehurst.wiremock.verification.diff.PlainTextDiffRenderer;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.jsonResponse;
import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static com.github.tomakehurst.wiremock.stubbing.ServeEvent.NOT_MATCHED;
import static com.github.tomakehurst.wiremock.stubbing.ServeEvent.TO_LOGGED_REQUEST;
import static com.google.common.collect.FluentIterable.from;

public class WireMockApp implements StubServer, Admin {

    public static final String FILES_ROOT = "__files";
    public static final String ADMIN_CONTEXT_ROOT = "/__admin";
    public static final String MAPPINGS_ROOT = "mappings";


    private final StubMappings stubMappings;
    private final RequestJournal requestJournal;
    private final GlobalSettingsHolder globalSettingsHolder;
    private final boolean browserProxyingEnabled;
    private final MappingsLoader defaultMappingsLoader;
    private final Container container;
    private final MappingsSaver mappingsSaver;
    private final NearMissCalculator nearMissCalculator;
    private final PlainTextDiffRenderer diffRenderer;

    private final Recorder recorder;

    private Options options;

    static {
        Xml.optimizeFactoriesLoading();
    }

    public WireMockApp(Options options, Container container) {
        this.options = options;

        FileSource fileSource = options.filesRoot();

        this.browserProxyingEnabled = options.browserProxyingEnabled();
        this.defaultMappingsLoader = options.mappingsLoader();
        this.mappingsSaver = options.mappingsSaver();
        globalSettingsHolder = new GlobalSettingsHolder();
        requestJournal = options.requestJournalDisabled() ? new DisabledRequestJournal() : new InMemoryRequestJournal(options.maxRequestJournalEntries());
        Map<String, RequestMatcherExtension> customMatchers = options.extensionsOfType(RequestMatcherExtension.class);
        stubMappings = new InMemoryStubMappings(
                customMatchers,
            options.extensionsOfType(ResponseDefinitionTransformer.class),
            fileSource);
        nearMissCalculator = new NearMissCalculator(stubMappings, requestJournal);
        diffRenderer = new PlainTextDiffRenderer(customMatchers);
        recorder = new Recorder(this);
        this.container = container;
        loadDefaultMappings();
    }

    public WireMockApp(
        boolean browserProxyingEnabled,
        MappingsLoader defaultMappingsLoader,
        MappingsSaver mappingsSaver,
        boolean requestJournalDisabled,
        Optional<Integer> maxRequestJournalEntries,
        Map<String, ResponseDefinitionTransformer> transformers,
        Map<String, RequestMatcherExtension> requestMatchers,
        FileSource rootFileSource,
        Container container) {

        this.browserProxyingEnabled = browserProxyingEnabled;
        this.defaultMappingsLoader = defaultMappingsLoader;
        this.mappingsSaver = mappingsSaver;
        globalSettingsHolder = new GlobalSettingsHolder();
        requestJournal = requestJournalDisabled ? new DisabledRequestJournal() : new InMemoryRequestJournal(maxRequestJournalEntries);
        stubMappings = new InMemoryStubMappings(requestMatchers, transformers, rootFileSource);
        this.container = container;
        nearMissCalculator = new NearMissCalculator(stubMappings, requestJournal);
        diffRenderer = new PlainTextDiffRenderer(requestMatchers);
        recorder = new Recorder(this);
        loadDefaultMappings();
    }

    public AdminRequestHandler buildAdminRequestHandler() {
        AdminRoutes adminRoutes = AdminRoutes.defaultsPlus(
            options.extensionsOfType(AdminApiExtension.class).values(),
            options.getNotMatchedRenderer()
        );
        return new AdminRequestHandler(
            adminRoutes,
            this,
            new BasicResponseRenderer(),
            options.getAdminAuthenticator(),
            options.getHttpsRequiredForAdminApi()
        );
    }

    public StubRequestHandler buildStubRequestHandler() {
        Map<String, PostServeAction> postServeActions = options.extensionsOfType(PostServeAction.class);
        return new StubRequestHandler(
            this,
            new StubResponseRenderer(
                options.filesRoot().child(FILES_ROOT),
                getGlobalSettingsHolder(),
                new ProxyResponseRenderer(
                    options.proxyVia(),
                    options.httpsSettings().trustStore(),
                    options.shouldPreserveHostHeader(),
                    options.proxyHostHeader(),
                    globalSettingsHolder),
                ImmutableList.copyOf(options.extensionsOfType(ResponseTransformer.class).values())
            ),
            this,
            postServeActions,
            requestJournal
        );
    }

    public GlobalSettingsHolder getGlobalSettingsHolder() {
        return globalSettingsHolder;
    }

    private void loadDefaultMappings() {
        loadMappingsUsing(defaultMappingsLoader);
    }

    public void loadMappingsUsing(final MappingsLoader mappingsLoader) {
        mappingsLoader.loadMappingsInto(stubMappings);
    }

    @Override
    public ServeEvent serveStubFor(Request request) {
        ServeEvent serveEvent = stubMappings.serveFor(request);

        if (serveEvent.isNoExactMatch()) {
            LoggedRequest loggedRequest = LoggedRequest.createFrom(request);
            if (request.isBrowserProxyRequest() && browserProxyingEnabled) {
                return ServeEvent.of(loggedRequest, ResponseDefinition.browserProxy(request));
            }

            logUnmatchedRequest(loggedRequest);
        }

        return serveEvent;
    }

    private void logUnmatchedRequest(LoggedRequest request) {
        List<NearMiss> nearest = nearMissCalculator.findNearestTo(request);
        String message;
        if (!nearest.isEmpty()) {
            message = diffRenderer.render(nearest.get(0).getDiff());
        } else {
            message = "Request was not matched as there were no stubs registered:\n" + request;
        }
        notifier().error(message);
    }

    @Override
    public void addStubMapping(StubMapping stubMapping) {
        stubMappings.addMapping(stubMapping);
        if (stubMapping.shouldBePersisted()) {
            mappingsSaver.save(stubMapping);
        }
    }

    @Override
    public void removeStubMapping(StubMapping stubMapping) {
        stubMappings.removeMapping(stubMapping);
        if (stubMapping.shouldBePersisted()) {
            mappingsSaver.remove(stubMapping);
        }
    }

    @Override
    public void editStubMapping(StubMapping stubMapping) {
        stubMappings.editMapping(stubMapping);
        if (stubMapping.shouldBePersisted()) {
            mappingsSaver.save(stubMapping);
        }
    }

    @Override
    public ListStubMappingsResult listAllStubMappings() {
        return new ListStubMappingsResult(LimitAndOffsetPaginator.none(stubMappings.getAll()));
    }

    @Override
    public SingleStubMappingResult getStubMapping(UUID id) {
        return SingleStubMappingResult.fromOptional(stubMappings.get(id));
    }

    @Override
    public void saveMappings() {
        mappingsSaver.save(stubMappings.getAll());
    }

    @Override
    public void resetAll() {
        resetToDefaultMappings();
    }

    @Override
    public void resetRequests() {
        requestJournal.reset();
    }

    @Override
    public void resetToDefaultMappings() {
        stubMappings.reset();
        resetRequests();
        loadDefaultMappings();
    }

    @Override
    public void resetScenarios() {
        stubMappings.resetScenarios();
    }

    @Override
    public void setScenarioState(Scenario scenario, String newState) {
        stubMappings.setScenarioState(scenario, newState);
    }

    @Override
    public void resetMappings() {
        mappingsSaver.removeAll();
        stubMappings.reset();
    }

    @Override
    public GetServeEventsResult getServeEvents() {
        try {
            return GetServeEventsResult.requestJournalEnabled(
                LimitAndOffsetPaginator.none(requestJournal.getAllServeEvents())
            );
        } catch (RequestJournalDisabledException e) {
            return GetServeEventsResult.requestJournalDisabled(
                LimitAndOffsetPaginator.none(requestJournal.getAllServeEvents())
            );
        }
    }

    @Override
    public SingleServedStubResult getServedStub(UUID id) {
        return SingleServedStubResult.fromOptional(requestJournal.getServeEvent(id));
    }

    @Override
    public VerificationResult countRequestsMatching(RequestPattern requestPattern) {
        try {
            return VerificationResult.withCount(requestJournal.countRequestsMatching(requestPattern));
        } catch (RequestJournalDisabledException e) {
            return VerificationResult.withRequestJournalDisabled();
        }
    }

    @Override
    public FindRequestsResult findRequestsMatching(RequestPattern requestPattern) {
        try {
            List<LoggedRequest> requests = requestJournal.getRequestsMatching(requestPattern);
            return FindRequestsResult.withRequests(requests);
        } catch (RequestJournalDisabledException e) {
            return FindRequestsResult.withRequestJournalDisabled();
        }
    }

    @Override
    public FindRequestsResult findUnmatchedRequests() {
        try {
            List<LoggedRequest> requests =
                from(requestJournal.getAllServeEvents())
                    .filter(NOT_MATCHED)
                    .transform(TO_LOGGED_REQUEST)
                    .toList();
            return FindRequestsResult.withRequests(requests);
        } catch (RequestJournalDisabledException e) {
            return FindRequestsResult.withRequestJournalDisabled();
        }
    }

    @Override
    public FindNearMissesResult findNearMissesForUnmatchedRequests() {
        ImmutableList.Builder<NearMiss> listBuilder = ImmutableList.builder();
        Iterable<ServeEvent> unmatchedServeEvents =
            from(requestJournal.getAllServeEvents())
                .filter(new Predicate<ServeEvent>() {
                    @Override
                    public boolean apply(ServeEvent input) {
                        return input.isNoExactMatch();
                    }
                });

        for (ServeEvent serveEvent : unmatchedServeEvents) {
            listBuilder.addAll(nearMissCalculator.findNearestTo(serveEvent.getRequest()));
        }

        return new FindNearMissesResult(listBuilder.build());
    }

    @Override
    public GetScenariosResult getAllScenarios() {
        return new GetScenariosResult(
            stubMappings.getAllScenarios()
        );
    }

    @Override
    public FindNearMissesResult findTopNearMissesFor(LoggedRequest loggedRequest) {
        return new FindNearMissesResult(nearMissCalculator.findNearestTo(loggedRequest));
    }

    @Override
    public FindNearMissesResult findTopNearMissesFor(RequestPattern requestPattern) {
        return new FindNearMissesResult(nearMissCalculator.findNearestTo(requestPattern));
    }

    @Override
    public void updateGlobalSettings(GlobalSettings newSettings) {
        globalSettingsHolder.replaceWith(newSettings);
    }

    public int port() {
        return container.port();
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public void shutdownServer() {
        container.shutdown();
    }

    public SnapshotRecordResult snapshotRecord() {
        return snapshotRecord(RecordSpec.DEFAULTS);
    }

    @Override
    public SnapshotRecordResult snapshotRecord(RecordSpecBuilder spec) {
        return snapshotRecord(spec.build());
    }

    public SnapshotRecordResult snapshotRecord(RecordSpec recordSpec) {
        return recorder.takeSnapshot(getServeEvents().getServeEvents(), recordSpec);
    }

    @Override
    public void startRecording(String targetBaseUrl) {
        recorder.startRecording(RecordSpec.forBaseUrl(targetBaseUrl));
    }

    @Override
    public void startRecording(RecordSpec recordSpec) {
        recorder.startRecording(recordSpec);
    }

    @Override
    public void startRecording(RecordSpecBuilder recordSpec) {
        recorder.startRecording(recordSpec.build());
    }

    @Override
    public SnapshotRecordResult stopRecording() {
        return recorder.stopRecording();
    }

    @Override
    public RecordingStatusResult getRecordingStatus() {
        return new RecordingStatusResult(recorder.getStatus().name());
    }

    @Override
    public ListStubMappingsResult findAllStubsByMetadata(StringValuePattern pattern) {
        return new ListStubMappingsResult(LimitAndOffsetPaginator.none(stubMappings.findByMetadata(pattern)));
    }

    @Override
    public void removeStubsByMetadata(StringValuePattern pattern) {
        List<StubMapping> foundMappings = stubMappings.findByMetadata(pattern);
        for (StubMapping mapping: foundMappings) {
            removeStubMapping(mapping);
        }
    }


}
