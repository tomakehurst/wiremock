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

import com.github.tomakehurst.wiremock.admin.model.*;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.recording.RecordSpec;
import com.github.tomakehurst.wiremock.recording.RecordSpecBuilder;
import com.github.tomakehurst.wiremock.recording.RecordingStatusResult;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubImport;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.FindNearMissesResult;
import com.github.tomakehurst.wiremock.verification.FindRequestsResult;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.VerificationResult;

import java.util.UUID;

public interface Admin {

	void addStubMapping(StubMapping stubMapping);
	void editStubMapping(StubMapping stubMapping);
	void removeStubMapping(StubMapping stubbMapping);
    ListStubMappingsResult listAllStubMappings();
    SingleStubMappingResult getStubMapping(UUID id);
    void saveMappings();

	void resetRequests();
    void resetSelectedRequests(RequestPattern requestPattern);
    void resetScenarios();
    void resetMappings();
    void resetAll();
    void resetToDefaultMappings();

    GetServeEventsResult getServeEvents();
    SingleServedStubResult getServedStub(UUID id);
    VerificationResult countRequestsMatching(RequestPattern requestPattern);
    FindRequestsResult findRequestsMatching(RequestPattern requestPattern);
    FindRequestsResult findUnmatchedRequests();

    FindNearMissesResult findTopNearMissesFor(LoggedRequest loggedRequest);
    FindNearMissesResult findTopNearMissesFor(RequestPattern requestPattern);
    FindNearMissesResult findNearMissesForUnmatchedRequests();

    GetScenariosResult getAllScenarios();

    void updateGlobalSettings(GlobalSettings settings);

    SnapshotRecordResult snapshotRecord();
    SnapshotRecordResult snapshotRecord(RecordSpec spec);
    SnapshotRecordResult snapshotRecord(RecordSpecBuilder spec);

    void startRecording(String targetBaseUrl);
    void startRecording(RecordSpec spec);
    void startRecording(RecordSpecBuilder recordSpec);
    SnapshotRecordResult stopRecording();
    RecordingStatusResult getRecordingStatus();

    Options getOptions();

    void shutdownServer();

    ListStubMappingsResult findAllStubsByMetadata(StringValuePattern pattern);
    void removeStubsByMetadata(StringValuePattern pattern);

    void importStubs(StubImport stubImport);

    GetGlobalSettingsResult getGlobalSettings();
}
