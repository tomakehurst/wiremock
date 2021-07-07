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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.testsupport.MappingJsonSamples;
import com.github.tomakehurst.wiremock.testsupport.Network;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PortNumberTest {

    private List<WireMockServer> createdServers;
    private final int HTTP_UNAUTHORIZED = 401;

    @Before
    public void setup() {
        createdServers = new ArrayList<WireMockServer>();
    }

    @After
    public void stopServers() {
        for (WireMockServer wireMockServer : createdServers) {
            if(wireMockServer.isRunning()) {
                wireMockServer.stop();
            }
        }
    }

    @Test
    public void canRunOnAnotherPortThan8080() {
        int port = Network.findFreePort();
        WireMockServer wireMockServer = createServer(wireMockConfig().port(port));
        wireMockServer.start();
        WireMockTestClient wireMockClient = new WireMockTestClient(port);

        wireMockClient.addResponse(MappingJsonSamples.BASIC_MAPPING_REQUEST_WITH_RESPONSE_HEADER);
        WireMockResponse response = wireMockClient.get("/a/registered/resource");
        assertThat(response.statusCode(), is(HTTP_UNAUTHORIZED));
    }


    @Test
    public void configuredPortIsReportedListeningPort() {
        int port = Network.findFreePort();
        int httpsPort = Network.findFreePort();
        WireMockServer wireMockServer = createServer(wireMockConfig().port(port).httpsPort(httpsPort));
        wireMockServer.start();

        assertThat(wireMockServer.port(), is(port));
        assertThat(wireMockServer.httpsPort(), is(httpsPort));
    }

    @Test(expected = IllegalStateException.class)
    public void unstartedServerThrowsExceptionWhenAttemptingToRetrievePort() {
        createServer(wireMockConfig().port(Network.findFreePort())).port();
    }

    @Test(expected = IllegalStateException.class)
    public void unstartedServerThrowsExceptionWhenAttemptingToRetrieveHttpsPort() {
        createServer(wireMockConfig().httpsPort(Network.findFreePort())).httpsPort();
    }

    @Test(expected = IllegalStateException.class)
    public void serverWithoutHttpsThrowsExceptionWhenAttemptingToRetrieveHttpsPort() {
        WireMockServer wireMockServer = createServer(wireMockConfig().port(Network.findFreePort()));
        wireMockServer.start();
        wireMockServer.httpsPort();
    }

    @Test
    public void configuringPortZeroPicksArbitraryPort() {
        WireMockServer wireMockServer = createServer(wireMockConfig().port(0).httpsPort(0));
        wireMockServer.start();
        assertThat(wireMockServer.port(), greaterThan(0));
        assertThat(wireMockServer.httpsPort(), greaterThan(0));
    }

    private WireMockServer createServer(WireMockConfiguration configuration) {
        final WireMockServer wireMockServer = new WireMockServer(configuration);
        createdServers.add(wireMockServer);
        return wireMockServer;
    }
}
