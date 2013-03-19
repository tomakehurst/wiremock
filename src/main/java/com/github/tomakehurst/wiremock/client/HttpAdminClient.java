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

import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.global.RequestDelaySpec;
import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.FindRequestsResult;
import com.github.tomakehurst.wiremock.verification.VerificationResult;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import static com.github.tomakehurst.wiremock.common.HttpClientUtils.getEntityAsStringAndCloseStream;
import static com.github.tomakehurst.wiremock.http.MimeType.JSON;
import static com.github.tomakehurst.wiremock.verification.VerificationResult.buildVerificationResultFrom;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

public class HttpAdminClient implements Admin {
	
	private static final String ADMIN_URL_PREFIX = "http://%s:%d%s/__admin";
	private static final String LOCAL_WIREMOCK_NEW_RESPONSE_URL = ADMIN_URL_PREFIX + "/mappings/new";
	private static final String LOCAL_WIREMOCK_RESET_URL = ADMIN_URL_PREFIX + "/reset";
    private static final String LOCAL_WIREMOCK_RESET_SCENARIOS_URL = ADMIN_URL_PREFIX + "/scenarios/reset";
    private static final String LOCAL_WIREMOCK_RESET_TO_DEFAULT_MAPPINGS_URL = ADMIN_URL_PREFIX + "/mappings/reset";
    private static final String LOCAL_WIREMOCK_COUNT_REQUESTS_URL = ADMIN_URL_PREFIX + "/requests/count";
    private static final String LOCAL_WIREMOCK_FIND_REQUESTS_URL = ADMIN_URL_PREFIX + "/requests/find";
	private static final String WIREMOCK_GLOBAL_SETTINGS_URL = ADMIN_URL_PREFIX + "/settings";
    private static final String SOCKET_ACCEPT_DELAY_URL = ADMIN_URL_PREFIX + "/socket-delay";
	
	private final String host;
	private final int port;
	private final String urlPathPrefix;
	
	private final HttpClient httpClient;
	
	public HttpAdminClient(String host, int port, String urlPathPrefix) {
		this.host = host;
		this.port = port;
		this.urlPathPrefix = urlPathPrefix;
		
		httpClient = HttpClientFactory.createClient();
	}
	
	public HttpAdminClient(String host, int port) {
		this(host, port, "");
	}

	@Override
	public void addStubMapping(StubMapping stubMapping) {
        String json = Json.write(stubMapping);
		int status = postJsonAndReturnStatus(newMappingUrl(), json);
		if (status != HTTP_CREATED) {
			throw new RuntimeException("Returned status code was " + status);
		}
	}
	
	@Override
	public void resetMappings() {
		int status = postEmptyBodyAndReturnStatus(resetUrl());
		assertStatusOk(status);
	}
	
	@Override
	public void resetScenarios() {
		int status = postEmptyBodyAndReturnStatus(resetScenariosUrl());
		assertStatusOk(status);
	}

    @Override
    public void resetToDefaultMappings() {
        int status = postEmptyBodyAndReturnStatus(resetToDefaultMappingsUrl());
        assertStatusOk(status);
    }

    private void assertStatusOk(int status) {
		if (status != HTTP_OK) {
			throw new RuntimeException("Returned status code was " + status);
		}
	}
	
	@Override
	public int countRequestsMatching(RequestPattern requestPattern) {
		String json = Json.write(requestPattern);
		String body = postJsonAssertOkAndReturnBody(requestsCountUrl(), json, HTTP_OK);
		VerificationResult verificationResult = buildVerificationResultFrom(body);
		return verificationResult.getCount();
	}

    @Override
    public FindRequestsResult findRequestsMatching(RequestPattern requestPattern) {
        String json = Json.write(requestPattern);
        String body = postJsonAssertOkAndReturnBody(findRequestsUrl(), json, HTTP_OK);
        return Json.read(body, FindRequestsResult.class);
    }

    @Override
	public void updateGlobalSettings(GlobalSettings settings) {
		String json = Json.write(settings);
		postJsonAssertOkAndReturnBody(globalSettingsUrl(), json, HTTP_OK);
	}

    @Override
    public void addSocketAcceptDelay(RequestDelaySpec spec) {
        String json = Json.write(spec);
        postJsonAssertOkAndReturnBody(socketAcceptDelayUrl(), json, HTTP_OK);
    }

    private int postJsonAndReturnStatus(String url, String json) {
		HttpPost post = new HttpPost(url);
		try {
			if (json != null) {
				post.setEntity(new StringEntity(json, JSON.toString(), "utf-8"));
			}
			HttpResponse response = httpClient.execute(post);
			int statusCode = response.getStatusLine().getStatusCode();
			getEntityAsStringAndCloseStream(response);
			
			return statusCode;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private String postJsonAssertOkAndReturnBody(String url, String json, int expectedStatus) {
		HttpPost post = new HttpPost(url);
		try {
			if (json != null) {
				post.setEntity(new StringEntity(json, JSON.toString(), "utf-8"));
			}
			HttpResponse response = httpClient.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != expectedStatus) {
				throw new VerificationException(
                        "Expected status " + expectedStatus + " for " + url + " but was " + statusCode);
			}
			
			String body = getEntityAsStringAndCloseStream(response);
			return body;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private int postEmptyBodyAndReturnStatus(String url) {
		return postJsonAndReturnStatus(url, null);
	}

	private String newMappingUrl() {
		return String.format(LOCAL_WIREMOCK_NEW_RESPONSE_URL, host, port, urlPathPrefix);
	}
	
	private String resetUrl() {
		return String.format(LOCAL_WIREMOCK_RESET_URL, host, port, urlPathPrefix);
	}
	
	private String resetScenariosUrl() {
		return String.format(LOCAL_WIREMOCK_RESET_SCENARIOS_URL, host, port, urlPathPrefix);
	}

    private String resetToDefaultMappingsUrl() {
        return String.format(LOCAL_WIREMOCK_RESET_TO_DEFAULT_MAPPINGS_URL, host, port, urlPathPrefix);
    }
	
	private String requestsCountUrl() {
		return String.format(LOCAL_WIREMOCK_COUNT_REQUESTS_URL, host, port, urlPathPrefix);
	}

    private String findRequestsUrl() {
        return String.format(LOCAL_WIREMOCK_FIND_REQUESTS_URL, host, port, urlPathPrefix);
    }

	private String globalSettingsUrl() {
		return String.format(WIREMOCK_GLOBAL_SETTINGS_URL, host, port, urlPathPrefix);
	}

    private String socketAcceptDelayUrl() {
        return String.format(SOCKET_ACCEPT_DELAY_URL, host, port, urlPathPrefix);
    }
}
