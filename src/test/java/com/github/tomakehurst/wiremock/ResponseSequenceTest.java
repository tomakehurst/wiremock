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

import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ResponseSequenceTest {

    private WireMockServer wm;
    private WireMockTestClient client;
    private String url;

    @Test
    public void sequenceWorksAsExpected() {
        wm.stubFor(get(url)
                .thenReturn(serverError())
                .thenReturn(noContent()));

        assertNextResponseIs(500);
        assertNextResponseIs(204);

        // after sequence is exhausted, falls back to default response
        assertNextResponseIs(200);
        assertNextResponseIs(200);
        assertNextResponseIs(200);
    }

    @Test
    public void loopingSequenceWorksAsExpected() {
        wm.stubFor(get(url)
                .thenReturn(serverError())
                .thenReturn(unauthorized())
                .thenReturn(noContent())
                .loopResponseSequence());

        assertNextResponseIs(500);
        assertNextResponseIs(401);
        assertNextResponseIs(204);
        assertNextResponseIs(500);
        assertNextResponseIs(401);
        assertNextResponseIs(204);
    }

    private void assertNextResponseIs(int statusCode) {
        WireMockResponse response = client.get(url);
        assertThat(response.statusCode(), is(statusCode));
    }

    @Before
    public void init() {
        url = "/" + RandomStringUtils.randomAlphabetic(5);
        wm = new WireMockServer(wireMockConfig().dynamicPort());
        wm.start();
        client = new WireMockTestClient(wm.port());
    }

    @After
    public void stopServer() {
        wm.stop();
    }

}
