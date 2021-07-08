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

import com.github.tomakehurst.wiremock.admin.model.ListStubMappingsResult;
import com.github.tomakehurst.wiremock.admin.model.SingleStubMappingResult;
import com.github.tomakehurst.wiremock.common.*;
import com.github.tomakehurst.wiremock.stubbing.*;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.global.GlobalSettingsHolder;
import com.github.tomakehurst.wiremock.http.DelayDistribution;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.*;
import com.github.tomakehurst.wiremock.recording.RecordSpecBuilder;
import com.github.tomakehurst.wiremock.recording.RecordingStatusResult;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.security.ClientAuthenticator;
import com.github.tomakehurst.wiremock.standalone.RemoteMappingsLoader;
import com.github.tomakehurst.wiremock.verification.*;
import com.github.tomakehurst.wiremock.verification.diff.Diff;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.matching.RequestPattern.thatMatch;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.allRequests;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.LOCATION;


public class WireMock {

	private static final int DEFAULT_PORT = 8080;
	private static final String DEFAULT_HOST = "localhost";

	private final Admin admin;
	private final GlobalSettingsHolder globalSettingsHolder = new GlobalSettingsHolder();

	private static InheritableThreadLocal<WireMock> defaultInstance = new InheritableThreadLocal<WireMock>(){
            @Override
            protected WireMock initialValue() {
            	return WireMock.create().build();
            }
	};

	public static WireMockBuilder create() {
	    return new WireMockBuilder();
    }

    public WireMock(Admin admin) {
        this.admin = admin;
    }

    public WireMock(int port) {
        this(DEFAULT_HOST, port);
    }

    public WireMock(String host, int port) {
		admin = new HttpAdminClient(host, port);
	}

	public WireMock(String host, int port, String urlPathPrefix) {
		admin = new HttpAdminClient(host, port, urlPathPrefix);
	}

	public WireMock(String scheme, String host, int port) {
		admin = new HttpAdminClient(scheme, host, port);
	}

	public WireMock(String scheme, String host, int port, String urlPathPrefix) {
		admin = new HttpAdminClient(scheme, host, port, urlPathPrefix);
	}

    public WireMock(String scheme, String host, int port, String urlPathPrefix, String hostHeader, String proxyHost, int proxyPort, ClientAuthenticator authenticator) {
        admin = new HttpAdminClient(scheme, host, port, urlPathPrefix, hostHeader, proxyHost, proxyPort, authenticator);
    }

	public WireMock() {
		admin = new HttpAdminClient(DEFAULT_HOST, DEFAULT_PORT);
	}

	public static StubMapping givenThat(MappingBuilder mappingBuilder) {
		return defaultInstance.get().register(mappingBuilder);
	}

	public static StubMapping stubFor(MappingBuilder mappingBuilder) {
		return givenThat(mappingBuilder);
	}

	public static void editStub(MappingBuilder mappingBuilder) {
		defaultInstance.get().editStubMapping(mappingBuilder);
	}

	public static void removeStub(MappingBuilder mappingBuilder) {
		defaultInstance.get().removeStubMapping(mappingBuilder);
	}

    public static void removeStub(StubMapping stubMapping) {
        defaultInstance.get().removeStubMapping(stubMapping);
    }

    public static ListStubMappingsResult listAllStubMappings() {
        return defaultInstance.get().allStubMappings();
    }

    public static StubMapping getSingleStubMapping(UUID id) {
        return defaultInstance.get().getStubMapping(id).getItem();
    }

    public static void configureFor(int port) {
        defaultInstance.set(WireMock.create().port(port).build());
    }

	public static void configureFor(String host, int port) {
		defaultInstance.set(WireMock.create().host(host).port(port).build());
	}

	public static void configureFor(String host, int port, String urlPathPrefix) {
		defaultInstance.set(WireMock.create().host(host).port(port).urlPathPrefix(urlPathPrefix).build());
	}

	public static void configureFor(String scheme, String host, int port, String urlPathPrefix) {
		defaultInstance.set(WireMock.create().scheme(scheme).host(host).port(port).urlPathPrefix(urlPathPrefix).build());
	}

	public static void configureFor(String scheme, String host, int port) {
		defaultInstance.set(WireMock.create().scheme(scheme).host(host).port(port).build());
	}

    public static void configureFor(String scheme, String host, int port, String proxyHost, int proxyPort) {
        defaultInstance.set(WireMock.create().scheme(scheme).host(host).port(port).urlPathPrefix("").hostHeader(null).proxyHost(proxyHost).proxyPort(proxyPort).build());
    }

    public static void configureFor(WireMock client) {
	    defaultInstance.set(client);
    }

	public static void configure() {
		defaultInstance.set(WireMock.create().build());
	}

    public static StringValuePattern equalTo(String value) {
        return new EqualToPattern(value);
    }

    public static BinaryEqualToPattern binaryEqualTo(byte[] content) {
        return new BinaryEqualToPattern(content);
    }

    public static BinaryEqualToPattern binaryEqualTo(String content) {
        return new BinaryEqualToPattern(content);
    }

	public static StringValuePattern equalToIgnoreCase(String value) {
		return new EqualToPattern(value, true);
	}

    public static StringValuePattern equalToJson(String value) {
        return new EqualToJsonPattern(value, null, null);
    }

    public static StringValuePattern equalToJson(String value, boolean ignoreArrayOrder, boolean ignoreExtraElements) {
        return new EqualToJsonPattern(value, ignoreArrayOrder, ignoreExtraElements);
    }

    public static StringValuePattern matchingJsonPath(String value) {
        return new MatchesJsonPathPattern(value);
    }

    public static StringValuePattern matchingJsonPath(String value, StringValuePattern valuePattern) {
        return new MatchesJsonPathPattern(value, valuePattern);
    }

    public static EqualToXmlPattern equalToXml(String value) {
        return new EqualToXmlPattern(value);
    }

    public static EqualToXmlPattern equalToXml(String value, boolean enablePlaceholders) {
        return new EqualToXmlPattern(value, enablePlaceholders, null, null, null);
    }

    public static EqualToXmlPattern equalToXml(String value, boolean enablePlaceholders, String placeholderOpeningDelimiterRegex, String placeholderClosingDelimiterRegex) {
	    return new EqualToXmlPattern(value, enablePlaceholders, placeholderOpeningDelimiterRegex, placeholderClosingDelimiterRegex, null);
    }

    public static MatchesXPathPattern matchingXPath(String value) {
        return new MatchesXPathPattern(value, Collections.<String, String>emptyMap());
    }

    public static StringValuePattern matchingXPath(String value, Map<String, String> namespaces) {
        return new MatchesXPathPattern(value, namespaces);
    }

    public static StringValuePattern matchingXPath(String value, StringValuePattern valuePattern) {
        return new MatchesXPathPattern(value, valuePattern);
    }

    public static StringValuePattern containing(String value) {
        return new ContainsPattern(value);
    }

    public static StringValuePattern matching(String regex) {
        return new RegexPattern(regex);
    }

    public static StringValuePattern notMatching(String regex) {
        return new NegativeRegexPattern(regex);
    }

    public static BeforeDateTimePattern before(String dateTimeSpec) {
	    return new BeforeDateTimePattern(dateTimeSpec);
    }

    public static BeforeDateTimePattern before(ZonedDateTime dateTime) {
	    return new BeforeDateTimePattern(dateTime);
    }

    public static BeforeDateTimePattern before(LocalDateTime dateTime) {
	    return new BeforeDateTimePattern(dateTime);
    }

    public static BeforeDateTimePattern beforeNow() {
	    return new BeforeDateTimePattern("now");
    }

    public static EqualToDateTimePattern equalToDateTime(String dateTimeSpec) {
	    return new EqualToDateTimePattern(dateTimeSpec);
    }

    public static EqualToDateTimePattern equalToDateTime(ZonedDateTime dateTime) {
	    return new EqualToDateTimePattern(dateTime);
    }

    public static EqualToDateTimePattern equalToDateTime(LocalDateTime dateTime) {
	    return new EqualToDateTimePattern(dateTime);
    }

    public static EqualToDateTimePattern isNow() {
        return new EqualToDateTimePattern("now");
    }

    public static AfterDateTimePattern after(String dateTimeSpec) {
        return new AfterDateTimePattern(dateTimeSpec);
    }

    public static AfterDateTimePattern after(ZonedDateTime dateTime) {
        return new AfterDateTimePattern(dateTime);
    }

    public static AfterDateTimePattern after(LocalDateTime dateTime) {
        return new AfterDateTimePattern(dateTime);
    }

    public static AfterDateTimePattern afterNow() {
        return new AfterDateTimePattern("now");
    }

    public static StringValuePattern absent() {
        return AbsentPattern.ABSENT;
    }

    public static StringValuePattern and(StringValuePattern... matchers) {
	    return new LogicalAnd(matchers);
    }

    public static StringValuePattern or(StringValuePattern... matchers) {
	    return new LogicalOr(matchers);
    }

    public void saveMappings() {
        admin.saveMappings();
    }

    public static void saveAllMappings() {
        defaultInstance.get().saveMappings();
    }

    public void removeMappings() {
        admin.resetMappings();
    }

    public static void removeAllMappings() {
        defaultInstance.get().removeMappings();
    }

	public void resetMappings() {
		admin.resetAll();
	}

	public static void reset() {
		defaultInstance.get().resetMappings();
	}

	public static void resetAllRequests() {
		defaultInstance.get().resetRequests();
	}

	public void resetRequests() {
		admin.resetRequests();
	}

	public void resetScenarios() {
		admin.resetScenarios();
	}

    public static List<Scenario> getAllScenarios() {
        return defaultInstance.get().getScenarios();
    }

    private List<Scenario> getScenarios() {
        return admin.getAllScenarios().getScenarios();
    }

    public static void resetAllScenarios() {
		defaultInstance.get().resetScenarios();
	}

    public void resetToDefaultMappings() {
        admin.resetToDefaultMappings();
    }

    public static void resetToDefault() {
        defaultInstance.get().resetToDefaultMappings();
    }

	public StubMapping register(MappingBuilder mappingBuilder) {
		StubMapping mapping = mappingBuilder.build();
		register(mapping);
		return mapping;
	}

    public void register(StubMapping mapping) {
        admin.addStubMapping(mapping);
    }

	public void editStubMapping(MappingBuilder mappingBuilder) {
		admin.editStubMapping(mappingBuilder.build());
	}

	public void removeStubMapping(MappingBuilder mappingBuilder) {
		admin.removeStubMapping(mappingBuilder.build());
	}

	public void removeStubMapping(StubMapping stubMapping) {
		admin.removeStubMapping(stubMapping);
	}

    public ListStubMappingsResult allStubMappings() {
        return admin.listAllStubMappings();
    }

    public SingleStubMappingResult getStubMapping(UUID id) {
        return admin.getStubMapping(id);
    }

    public static UrlPattern urlEqualTo(String testUrl) {
        return new UrlPattern(equalTo(testUrl), false);
    }

    public static UrlPattern urlMatching(String urlRegex) {
        return new UrlPattern(matching(urlRegex), true);
    }

    public static UrlPathPattern urlPathEqualTo(String testUrl) {
        return new UrlPathPattern(equalTo(testUrl), false);
    }

    public static UrlPathPattern urlPathMatching(String urlRegex) {
        return new UrlPathPattern(matching(urlRegex), true);
    }

    public static UrlPattern anyUrl() {
        return UrlPattern.ANY;
    }

	public static CountMatchingStrategy lessThan(int expected) {
		return new CountMatchingStrategy(CountMatchingStrategy.LESS_THAN, expected);
	}

	public static CountMatchingStrategy lessThanOrExactly(int expected) {
		return new CountMatchingStrategy(CountMatchingStrategy.LESS_THAN_OR_EQUAL, expected);
	}

	public static CountMatchingStrategy exactly(int expected) {
		return new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, expected);
	}

	public static CountMatchingStrategy moreThanOrExactly(int expected) {
		return new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN_OR_EQUAL, expected);
	}

	public static CountMatchingStrategy moreThan(int expected) {
		return new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN, expected);
	}

	public static MappingBuilder get(UrlPattern urlPattern) {
		return new BasicMappingBuilder(RequestMethod.GET, urlPattern);
	}

	public static MappingBuilder post(UrlPattern urlPattern) {
		return new BasicMappingBuilder(RequestMethod.POST, urlPattern);
	}

	public static MappingBuilder put(UrlPattern urlPattern) {
		return new BasicMappingBuilder(RequestMethod.PUT, urlPattern);
	}

	public static MappingBuilder delete(UrlPattern urlPattern) {
		return new BasicMappingBuilder(RequestMethod.DELETE, urlPattern);
	}

	public static MappingBuilder patch(UrlPattern urlPattern) {
		return new BasicMappingBuilder(RequestMethod.PATCH, urlPattern);
	}

	public static MappingBuilder head(UrlPattern urlPattern) {
		return new BasicMappingBuilder(RequestMethod.HEAD, urlPattern);
	}

	public static MappingBuilder options(UrlPattern urlPattern) {
		return new BasicMappingBuilder(RequestMethod.OPTIONS, urlPattern);
	}

	public static MappingBuilder trace(UrlPattern urlPattern) {
		return new BasicMappingBuilder(RequestMethod.TRACE, urlPattern);
	}

	public static MappingBuilder any(UrlPattern urlPattern) {
		return new BasicMappingBuilder(RequestMethod.ANY, urlPattern);
	}

    public static MappingBuilder request(String method, UrlPattern urlPattern) {
        return new BasicMappingBuilder(RequestMethod.fromString(method), urlPattern);
    }

	public static MappingBuilder requestMatching(String customRequestMatcherName) {
		return new BasicMappingBuilder(customRequestMatcherName, Parameters.empty());
	}

	public static MappingBuilder requestMatching(String customRequestMatcherName, Parameters parameters) {
		return new BasicMappingBuilder(customRequestMatcherName, parameters);
	}

	public static MappingBuilder requestMatching(ValueMatcher<Request> requestMatcher) {
		return new BasicMappingBuilder(requestMatcher);
	}

	public static ResponseDefinitionBuilder aResponse() {
		return new ResponseDefinitionBuilder();
	}

    public static ResponseDefinitionBuilder ok() {
        return aResponse().withStatus(200);
    }

    public static ResponseDefinitionBuilder ok(String body) {
        return aResponse().withStatus(200).withBody(body);
    }

    public static ResponseDefinitionBuilder okForContentType(String contentType, String body) {
        return aResponse()
            .withStatus(200)
            .withHeader(CONTENT_TYPE, contentType)
            .withBody(body);
    }

    public static ResponseDefinitionBuilder okJson(String body) {
        return okForContentType("application/json", body);
    }

    public static ResponseDefinitionBuilder okXml(String body) {
        return okForContentType("application/xml", body);
    }

    public static ResponseDefinitionBuilder okTextXml(String body) {
        return okForContentType("text/xml", body);
    }

    public static MappingBuilder proxyAllTo(String url) {
        return any(anyUrl()).willReturn(aResponse().proxiedFrom(url));
    }

    public static MappingBuilder get(String url) {
        return get(urlEqualTo(url));
    }

    public static MappingBuilder post(String url) {
        return post(urlEqualTo(url));
    }

    public static MappingBuilder put(String url) {
        return put(urlEqualTo(url));
    }

    public static MappingBuilder delete(String url) {
        return delete(urlEqualTo(url));
    }

    public static ResponseDefinitionBuilder created() {
        return aResponse().withStatus(201);
    }

    public static ResponseDefinitionBuilder noContent() {
        return aResponse().withStatus(204);
    }

    public static ResponseDefinitionBuilder permanentRedirect(String location) {
        return aResponse().withStatus(301).withHeader(LOCATION, location);
    }

    public static ResponseDefinitionBuilder temporaryRedirect(String location) {
        return aResponse().withStatus(302).withHeader(LOCATION, location);
    }

    public static ResponseDefinitionBuilder seeOther(String location) {
        return aResponse().withStatus(303).withHeader(LOCATION, location);
    }

    public static ResponseDefinitionBuilder badRequest() {
        return aResponse().withStatus(400);
    }

    public static ResponseDefinitionBuilder badRequestEntity() {
        return aResponse().withStatus(422);
    }

    public static ResponseDefinitionBuilder unauthorized() {
        return aResponse().withStatus(401);
    }

    public static ResponseDefinitionBuilder unauthorizedJson(String body) {
	    return aResponse().withStatus(401).withJsonBody(body);
    }

    public static ResponseDefinitionBuilder forbidden() {
        return aResponse().withStatus(403);
    }

    public static ResponseDefinitionBuilder notFound() {
        return aResponse().withStatus(404);
    }

    public static ResponseDefinitionBuilder serverError() {
        return aResponse().withStatus(500);
    }

    public static ResponseDefinitionBuilder serviceUnavailable() {
        return aResponse().withStatus(503);
    }

    public static ResponseDefinitionBuilder status(int status) {
        return aResponse().withStatus(status);
    }

	public void verifyThat(RequestPatternBuilder requestPatternBuilder) {
		verifyThat(moreThanOrExactly(1), requestPatternBuilder);
	}

	public void verifyThat(int expectedCount, RequestPatternBuilder requestPatternBuilder) {
		verifyThat(exactly(expectedCount), requestPatternBuilder);
	}

	public void verifyThat(CountMatchingStrategy expectedCount, RequestPatternBuilder requestPatternBuilder) {
		final RequestPattern requestPattern = requestPatternBuilder.build();

		int actualCount;
		if (requestPattern.hasInlineCustomMatcher()) {
            List<LoggedRequest> requests = admin.findRequestsMatching(RequestPattern.everything()).getRequests();
            actualCount = from(requests).filter(thatMatch(requestPattern)).size();
        } else {
            VerificationResult result = admin.countRequestsMatching(requestPattern);
            result.assertRequestJournalEnabled();
            actualCount = result.getCount();
        }

        if (!expectedCount.match(actualCount)) {
            throw actualCount == 0 ?
                verificationExceptionForNearMisses(requestPatternBuilder, requestPattern) :
			    new VerificationException(requestPattern, expectedCount, actualCount);
		}
	}

    private VerificationException verificationExceptionForNearMisses(RequestPatternBuilder requestPatternBuilder, RequestPattern requestPattern) {
        List<NearMiss> nearMisses = findAllNearMissesFor(requestPatternBuilder);
        if (nearMisses.size() > 0) {
            Diff diff = new Diff(requestPattern, nearMisses.get(0).getRequest());
            return VerificationException.forUnmatchedRequestPattern(diff);
        }

        return new VerificationException(requestPattern, find(allRequests()));
    }

	public static void verify(RequestPatternBuilder requestPatternBuilder) {
		defaultInstance.get().verifyThat(requestPatternBuilder);
	}

	public static void verify(int count, RequestPatternBuilder requestPatternBuilder) {
		defaultInstance.get().verifyThat(count, requestPatternBuilder);
	}

	public static void verify(CountMatchingStrategy countMatchingStrategy, RequestPatternBuilder requestPatternBuilder) {
		defaultInstance.get().verifyThat(countMatchingStrategy, requestPatternBuilder);
	}

    public List<LoggedRequest> find(RequestPatternBuilder requestPatternBuilder) {
        FindRequestsResult result = admin.findRequestsMatching(requestPatternBuilder.build());
        result.assertRequestJournalEnabled();
        return result.getRequests();
    }

    public static List<LoggedRequest> findAll(RequestPatternBuilder requestPatternBuilder) {
        return defaultInstance.get().find(requestPatternBuilder);
    }

	public static List<ServeEvent> getAllServeEvents() {
        return defaultInstance.get().getServeEvents();
    }

    public List<ServeEvent> getServeEvents() {
        return admin.getServeEvents().getRequests();
    }

    public static void removeServeEvent(UUID eventId) {
	    defaultInstance.get().removeEvent(eventId);
    }

    public void removeEvent(UUID eventId) {
	    admin.removeServeEvent(eventId);
    }

    public List<ServeEvent> removeEvents(RequestPatternBuilder requestPatternBuilder) {
        return admin.removeServeEventsMatching(requestPatternBuilder.build()).getServeEvents();
    }

    public static List<ServeEvent> removeServeEvents(RequestPatternBuilder requestPatternBuilder) {
	    return defaultInstance.get().removeEvents(requestPatternBuilder);
    }

    public static List<ServeEvent> removeEventsByStubMetadata(StringValuePattern pattern) {
	    return defaultInstance.get().removeEventsByMetadata(pattern);
    }

    public List<ServeEvent> removeEventsByMetadata(StringValuePattern pattern) {
	    return admin.removeServeEventsForStubsMatchingMetadata(pattern).getServeEvents();
    }

    public static RequestPatternBuilder getRequestedFor(UrlPattern urlPattern) {
		return new RequestPatternBuilder(RequestMethod.GET, urlPattern);
	}

	public static RequestPatternBuilder postRequestedFor(UrlPattern urlPattern) {
		return new RequestPatternBuilder(RequestMethod.POST, urlPattern);
	}

	public static RequestPatternBuilder putRequestedFor(UrlPattern urlPattern) {
		return new RequestPatternBuilder(RequestMethod.PUT, urlPattern);
	}

	public static RequestPatternBuilder deleteRequestedFor(UrlPattern urlPattern) {
		return new RequestPatternBuilder(RequestMethod.DELETE, urlPattern);
	}

	public static RequestPatternBuilder patchRequestedFor(UrlPattern urlPattern) {
		return new RequestPatternBuilder(RequestMethod.PATCH, urlPattern);
	}

	public static RequestPatternBuilder headRequestedFor(UrlPattern urlPattern) {
		return new RequestPatternBuilder(RequestMethod.HEAD, urlPattern);
	}

	public static RequestPatternBuilder optionsRequestedFor(UrlPattern urlPattern) {
		return new RequestPatternBuilder(RequestMethod.OPTIONS, urlPattern);
	}

	public static RequestPatternBuilder traceRequestedFor(UrlPattern urlPattern) {
		return new RequestPatternBuilder(RequestMethod.TRACE, urlPattern);
	}

	public static RequestPatternBuilder anyRequestedFor(UrlPattern urlPattern) {
		return new RequestPatternBuilder(RequestMethod.ANY, urlPattern);
	}

    public static RequestPatternBuilder requestMadeFor(String customMatcherName, Parameters parameters) {
        return RequestPatternBuilder.forCustomMatcher(customMatcherName, parameters);
    }

	public static RequestPatternBuilder requestMadeFor(ValueMatcher<Request> requestMatcher) {
		return RequestPatternBuilder.forCustomMatcher(requestMatcher);
	}

	public static void setGlobalFixedDelay(int milliseconds) {
		defaultInstance.get().setGlobalFixedDelayVariable(milliseconds);
	}

	public void setGlobalFixedDelayVariable(int milliseconds) {
		GlobalSettings settings = globalSettingsHolder.get()
                .copy()
                .fixedDelay(milliseconds)
                .build();
		updateGlobalSettings(settings);
	}

	public static void setGlobalRandomDelay(DelayDistribution distribution) {
		defaultInstance.get().setGlobalRandomDelayVariable(distribution);
	}

	public void setGlobalRandomDelayVariable(DelayDistribution distribution) {
		GlobalSettings settings = globalSettingsHolder.get()
                .copy()
                .delayDistribution(distribution)
                .build();
		updateGlobalSettings(settings);
	}

	public static void updateSettings(GlobalSettings settings) {
	    defaultInstance.get().updateGlobalSettings(settings);
    }

	public void updateGlobalSettings(GlobalSettings settings) {
		globalSettingsHolder.replaceWith(settings);
		admin.updateGlobalSettings(settings);
	}

    public void shutdown() {
        admin.shutdownServer();
    }

    public static void shutdownServer() {
        defaultInstance.get().shutdown();
    }

    public static List<NearMiss> findNearMissesForAllUnmatched() {
        return defaultInstance.get().findNearMissesForAllUnmatchedRequests();
    }

    public List<NearMiss> findNearMissesForAllUnmatchedRequests() {
        FindNearMissesResult nearMissesResult = admin.findNearMissesForUnmatchedRequests();
        return nearMissesResult.getNearMisses();
    }

    public static List<LoggedRequest> findUnmatchedRequests() {
        return defaultInstance.get().findAllUnmatchedRequests();
    }

    public List<LoggedRequest> findAllUnmatchedRequests() {
        FindRequestsResult unmatchedResult = admin.findUnmatchedRequests();
        return unmatchedResult.getRequests();
    }

    public static List<NearMiss> findNearMissesFor(LoggedRequest loggedRequest) {
        return defaultInstance.get().findTopNearMissesFor(loggedRequest);
    }

    public List<NearMiss> findTopNearMissesFor(LoggedRequest loggedRequest) {
        FindNearMissesResult nearMissesResult = admin.findTopNearMissesFor(loggedRequest);
        return nearMissesResult.getNearMisses();
    }

    public static List<NearMiss> findNearMissesFor(RequestPatternBuilder requestPatternBuilder) {
        return defaultInstance.get().findAllNearMissesFor(requestPatternBuilder);
    }

    public List<NearMiss> findAllNearMissesFor(RequestPatternBuilder requestPatternBuilder) {
        FindNearMissesResult nearMissesResult = admin.findTopNearMissesFor(requestPatternBuilder.build());
        return nearMissesResult.getNearMisses();
    }

    public void loadMappingsFrom(String rootDir) {
        loadMappingsFrom(new File(rootDir));
    }

	public void loadMappingsFrom(File rootDir) {
		FileSource mappingsSource = new SingleRootFileSource(rootDir);
		new RemoteMappingsLoader(mappingsSource, this).load();
	}

    public static List<StubMapping> snapshotRecord() {
        return defaultInstance.get().takeSnapshotRecording();
    }

    public static List<StubMapping> snapshotRecord(RecordSpecBuilder spec) {
        return defaultInstance.get().takeSnapshotRecording(spec);
    }

    public List<StubMapping> takeSnapshotRecording() {
        return admin.snapshotRecord().getStubMappings();
    }

    public List<StubMapping> takeSnapshotRecording(RecordSpecBuilder spec) {
        return admin.snapshotRecord(spec.build()).getStubMappings();
    }

    public static MultipartValuePatternBuilder aMultipart() {
        return new MultipartValuePatternBuilder();
    }

    public static MultipartValuePatternBuilder aMultipart(String name) {
        return new MultipartValuePatternBuilder(name);
    }

    public static void startRecording(String targetBaseUrl) {
        defaultInstance.get().startStubRecording(targetBaseUrl);
    }

    public static void startRecording(RecordSpecBuilder spec) {
        defaultInstance.get().startStubRecording(spec);
    }

    public void startStubRecording(String targetBaseUrl) {
        admin.startRecording(targetBaseUrl);
    }

    public void startStubRecording(RecordSpecBuilder spec) {
        admin.startRecording(spec.build());
    }

    public static SnapshotRecordResult stopRecording() {
        return defaultInstance.get().stopStubRecording();
    }

    public SnapshotRecordResult stopStubRecording() {
        return admin.stopRecording();
    }

    public static RecordingStatusResult getRecordingStatus() {
        return defaultInstance.get().getStubRecordingStatus();
    }

    public RecordingStatusResult getStubRecordingStatus() {
        return admin.getRecordingStatus();
    }

    public static RecordSpecBuilder recordSpec() {
        return new RecordSpecBuilder();
    }

    public List<StubMapping> findAllStubsByMetadata(StringValuePattern pattern) {
	    return admin.findAllStubsByMetadata(pattern).getMappings();
    }

    public static List<StubMapping> findStubsByMetadata(StringValuePattern pattern) {
	    return defaultInstance.get().findAllStubsByMetadata(pattern);
    }

    public void removeStubsByMetadataPattern(StringValuePattern pattern) {
	    admin.removeStubsByMetadata(pattern);
    }

    public static void removeStubsByMetadata(StringValuePattern pattern) {
	    defaultInstance.get().removeStubsByMetadataPattern(pattern);
    }

    public void importStubMappings(StubImport stubImport) {
        admin.importStubs(stubImport);
    }

    public void importStubMappings(StubImportBuilder stubImport) {
	    importStubMappings(stubImport.build());
    }

    public static void importStubs(StubImportBuilder stubImport) {
	    importStubs(stubImport.build());
    }

    public static void importStubs(StubImport stubImport) {
        defaultInstance.get().importStubMappings(stubImport);
    }

    public GlobalSettings getGlobalSettings() {
	    return admin.getGlobalSettings().getSettings();
    }

    public static GlobalSettings getSettings() {
        return defaultInstance.get().getGlobalSettings();
    }
}
