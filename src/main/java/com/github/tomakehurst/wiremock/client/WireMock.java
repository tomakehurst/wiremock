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

import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.global.GlobalSettingsHolder;
import com.github.tomakehurst.wiremock.http.DelayDistribution;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.*;
import com.github.tomakehurst.wiremock.stubbing.ListStubMappingsResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.allRequests;


public class WireMock {

	private static final int DEFAULT_PORT = 8080;
	private static final String DEFAULT_HOST = "localhost";

	private final Admin admin;
	private final GlobalSettingsHolder globalSettingsHolder = new GlobalSettingsHolder();

	private static ThreadLocal<WireMock> defaultInstance = new ThreadLocal<WireMock>(){
            @Override
            protected WireMock initialValue() {
            	return new WireMock();
            }
	};

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

	public WireMock() {
		admin = new HttpAdminClient(DEFAULT_HOST, DEFAULT_PORT);
	}

	public static void givenThat(RemoteMappingBuilder mappingBuilder) {
		defaultInstance.get().register(mappingBuilder);
	}

	public static void stubFor(RemoteMappingBuilder mappingBuilder) {
		givenThat(mappingBuilder);
	}

	public static void editStub(MappingBuilder mappingBuilder) {
		defaultInstance.get().editStubMapping(mappingBuilder);
	}

    public static ListStubMappingsResult listAllStubMappings() {
        return defaultInstance.get().allStubMappings();
    }

    public static void configureFor(int port) {
        defaultInstance.set(new WireMock(port));
    }

	public static void configureFor(String host, int port) {
		defaultInstance.set(new WireMock(host, port));
	}

	public static void configureFor(String host, int port, String urlPathPrefix) {
		defaultInstance.set(new WireMock(host, port, urlPathPrefix));
	}

	public static void configureFor(String scheme, String host, int port, String urlPathPrefix) {
		defaultInstance.set(new WireMock(scheme, host, port, urlPathPrefix));
	}

	public static void configureFor(String scheme, String host, int port) {
		defaultInstance.set(new WireMock(scheme, host, port));
	}

	public static void configure() {
		defaultInstance.set(new WireMock());
	}

    public static StringValuePattern equalTo(String value) {
        return new EqualToPattern(value);
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

    public static StringValuePattern equalToXml(String value) {
        return new EqualToXmlPattern(value);
    }

    public static MatchesXPathPattern matchingXPath(String value) {
        return new MatchesXPathPattern(value, Collections.<String, String>emptyMap());
    }

    public static StringValuePattern matchingXPath(String value, Map<String, String> namespaces) {
        return new MatchesXPathPattern(value, namespaces);
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

    public static StringValuePattern absent() {
        return StringValuePattern.ABSENT;
    }

    public void saveMappings() {
        admin.saveMappings();
    }

    public static void saveAllMappings() {
        defaultInstance.get().saveMappings();
    }

	public void resetMappings() {
		admin.resetMappings();
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

	public static void resetAllScenarios() {
		defaultInstance.get().resetScenarios();
	}

    public void resetToDefaultMappings() {
        admin.resetToDefaultMappings();
    }

    public static void resetToDefault() {
        defaultInstance.get().resetToDefaultMappings();
    }

	public void register(RemoteMappingBuilder mappingBuilder) {
		StubMapping mapping = mappingBuilder.build();
		register(mapping);
	}

    public void register(StubMapping mapping) {
        admin.addStubMapping(mapping);
    }

	public void editStubMapping(RemoteMappingBuilder mappingBuilder) {
		admin.editStubMapping(mappingBuilder.build());
	}

	public void removeStubMapping(RemoteMappingBuilder mappingBuilder) {
		admin.removeStubMapping(mappingBuilder.build());
	}
    public ListStubMappingsResult allStubMappings() {
        return admin.listAllStubMappings();
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
        return new UrlPattern(new AnythingPattern(), false);
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

	public static RemoteMappingBuilder get(UrlPattern urlPattern) {
		return new MappingBuilder(RequestMethod.GET, urlPattern);
	}

	public static RemoteMappingBuilder post(UrlPattern urlPattern) {
		return new MappingBuilder(RequestMethod.POST, urlPattern);
	}

	public static RemoteMappingBuilder put(UrlPattern urlPattern) {
		return new MappingBuilder(RequestMethod.PUT, urlPattern);
	}

	public static RemoteMappingBuilder delete(UrlPattern urlPattern) {
		return new MappingBuilder(RequestMethod.DELETE, urlPattern);
	}

	public static RemoteMappingBuilder patch(UrlPattern urlPattern) {
		return new MappingBuilder(RequestMethod.PATCH, urlPattern);
	}

	public static RemoteMappingBuilder head(UrlPattern urlPattern) {
		return new MappingBuilder(RequestMethod.HEAD, urlPattern);
	}

	public static RemoteMappingBuilder options(UrlPattern urlPattern) {
		return new MappingBuilder(RequestMethod.OPTIONS, urlPattern);
	}

	public static RemoteMappingBuilder trace(UrlPattern urlPattern) {
		return new MappingBuilder(RequestMethod.TRACE, urlPattern);
	}

	public static RemoteMappingBuilder any(UrlPattern urlPattern) {
		return new MappingBuilder(RequestMethod.ANY, urlPattern);
	}

    public static RemoteMappingBuilder request(String method, UrlPattern urlPattern) {
        return new MappingBuilder(RequestMethod.fromString(method), urlPattern);
    }

	public static MappingBuilder requestMatching(String customRequestMatcherName) {
		return new MappingBuilder(customRequestMatcherName, Parameters.empty());
	}

	public static RemoteMappingBuilder requestMatching(String customRequestMatcherName, Parameters parameters) {
		return new MappingBuilder(customRequestMatcherName, parameters);
	}

	public static LocalMappingBuilder requestMatching(RequestMatcher requestMatcher) {
		return new MappingBuilder(requestMatcher);
	}

	public static ResponseDefinitionBuilder aResponse() {
		return new ResponseDefinitionBuilder();
	}

	public void verifyThat(RequestPatternBuilder requestPatternBuilder) {
		RequestPattern requestPattern = requestPatternBuilder.build();
        VerificationResult result = admin.countRequestsMatching(requestPattern);
        result.assertRequestJournalEnabled();

		if (result.getCount() < 1) {
            List<NearMiss> nearMisses = findAllNearMissesFor(requestPatternBuilder);
            if (nearMisses.size() > 0) {
                Diff diff = new Diff(requestPattern, nearMisses.get(0).getRequest());
                throw VerificationException.forUnmatchedRequestPattern(diff);
            }

            throw new VerificationException(requestPattern, find(allRequests()));
		}
	}

	public void verifyThat(int expectedCount, RequestPatternBuilder requestPatternBuilder) {
		RequestPattern requestPattern = requestPatternBuilder.build();
        VerificationResult result = admin.countRequestsMatching(requestPattern);
        result.assertRequestJournalEnabled();

        int actualCount = result.getCount();
        if (actualCount != expectedCount) {
            throw actualCount == 0 ?
                verificationExceptionForNearMisses(requestPatternBuilder, requestPattern) :
                new VerificationException(requestPattern, expectedCount, actualCount);
		}
	}

	public void verifyThat(CountMatchingStrategy expectedCount, RequestPatternBuilder requestPatternBuilder) {
		RequestPattern requestPattern = requestPatternBuilder.build();
		VerificationResult result = admin.countRequestsMatching(requestPattern);
		result.assertRequestJournalEnabled();

        int actualCount = result.getCount();
        if (!expectedCount.match(actualCount)) {
            throw actualCount == 0 ?
                verificationExceptionForNearMisses(requestPatternBuilder, requestPattern) :
			    new VerificationException(requestPattern, expectedCount, actualCount);
		}
	}

    private VerificationException verificationExceptionForNearMisses(RequestPatternBuilder requestPatternBuilder, RequestPattern requestPattern) {
        List<NearMiss> nearMisses = findNearMissesFor(requestPatternBuilder);
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

    public static RequestPatternBuilder requestMadeFor(String customMatcherName, Parameters parameters) {
        return RequestPatternBuilder.forCustomMatcher(customMatcherName, parameters);
    }

	public static LocalRequestPatternBuilder requestMadeFor(RequestMatcher requestMatcher) {
		return LocalRequestPatternBuilder.forCustomMatcher(requestMatcher);
	}

	public static void setGlobalFixedDelay(int milliseconds) {
		defaultInstance.get().setGlobalFixedDelayVariable(milliseconds);
	}

	public void setGlobalFixedDelayVariable(int milliseconds) {
		GlobalSettings settings = globalSettingsHolder.get().copy();
		settings.setFixedDelay(milliseconds);
		updateGlobalSettings(settings);
	}

	public static void setGlobalRandomDelay(DelayDistribution distribution) {
		defaultInstance.get().setGlobalRandomDelayVariable(distribution);
	}

	public void setGlobalRandomDelayVariable(DelayDistribution distribution) {
		GlobalSettings settings = globalSettingsHolder.get().copy();
		settings.setDelayDistribution(distribution);
		updateGlobalSettings(settings);
	}

	private void updateGlobalSettings(GlobalSettings settings) {
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
}
