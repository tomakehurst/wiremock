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

import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.testsupport.TestFiles;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.core.WireMockApp.FILES_ROOT;
import static com.github.tomakehurst.wiremock.core.WireMockApp.MAPPINGS_ROOT;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class Http2BrowserProxyAcceptanceTest {

    private static final String CERTIFICATE_NOT_TRUSTED_BY_TEST_CLIENT = TestFiles.KEY_STORE_PATH;

    @ClassRule
    public static WireMockClassRule target = new WireMockClassRule(wireMockConfig()
            .httpDisabled(true)
            .keystorePath(CERTIFICATE_NOT_TRUSTED_BY_TEST_CLIENT)
            .dynamicHttpsPort()
    );

    @Rule
    public WireMockClassRule instanceRule = target;

    private WireMockServer proxy;
    private WireMockTestClient testClient;

    @Before
    public void addAResourceToProxy() {
        testClient = new WireMockTestClient(target.httpsPort());

        proxy = new WireMockServer(wireMockConfig()
                .dynamicPort()
                .fileSource(new SingleRootFileSource(setupTempFileRoot()))
                .enableBrowserProxying(true));
        proxy.start();
    }

    @After
    public void stopServer() {
        if (proxy.isRunning()) {
            proxy.stop();
        }
    }

    @Test
    public void canProxyHttpsInBrowserProxyMode() throws Exception {
        target.stubFor(get(urlEqualTo("/whatever")).willReturn(aResponse().withBody("Got it")));

        assertThat(testClient.getViaProxy(target.url("/whatever"), proxy.port()).content(), is("Got it"));
    }

    @Test
    public void canStubHttpsInBrowserProxyMode() throws Exception {
        target.stubFor(get(urlEqualTo("/stubbed")).willReturn(aResponse().withBody("Should Not Be Returned")));
        proxy.stubFor(get(urlEqualTo("/stubbed")).willReturn(aResponse().withBody("Stubbed Value")));
        target.stubFor(get(urlEqualTo("/not_stubbed")).willReturn(aResponse().withBody("Should be served from target")));

        assertThat(testClient.getViaProxy(target.url("/stubbed"), proxy.port()).content(), is("Stubbed Value"));
        assertThat(testClient.getViaProxy(target.url("/not_stubbed"), proxy.port()).content(), is("Should be served from target"));
    }

    @Test
    public void canRecordHttpsInBrowserProxyMode() throws Exception {

        // given
        proxy.startRecording(target.baseUrl());
        String recordedEndpoint = target.url("/record_me");

        // and
        target.stubFor(get(urlEqualTo("/record_me")).willReturn(aResponse().withBody("Target response")));

        // then
        assertThat(testClient.getViaProxy(recordedEndpoint, proxy.port()).content(), is("Target response"));

        // when
        proxy.stopRecording();

        // and
        target.stop();

        // then
        assertThat(testClient.getViaProxy(recordedEndpoint, proxy.port()).content(), is("Target response"));
    }

    private static File setupTempFileRoot() {
        try {
            File root = java.nio.file.Files.createTempDirectory("wiremock").toFile();
            new File(root, MAPPINGS_ROOT).mkdirs();
            new File(root, FILES_ROOT).mkdirs();
            return root;
        } catch (IOException e) {
            return throwUnchecked(e, File.class);
        }
    }
}
