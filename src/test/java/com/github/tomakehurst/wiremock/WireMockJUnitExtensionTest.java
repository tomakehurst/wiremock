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
import com.github.tomakehurst.wiremock.junit5.ConfigurationFactory;
import com.github.tomakehurst.wiremock.junit5.ServerCustomizer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.Wiremock;
import com.github.tomakehurst.wiremock.junit5.Wiremock.Customize;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.temporaryRedirect;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WireMockJUnitExtensionTest {

    static class SampleCustomConfigurationFactory implements ConfigurationFactory {

        @Override
        public WireMockConfiguration createOptions(Wiremock wiremock) {
            return new WireMockConfiguration().port(9876);
        }
    }

    static class SampleCustomizer1 implements ServerCustomizer {
        @Override
        public void customize(WireMockServer server) {
            server.stubFor(get("/extension").willReturn(temporaryRedirect("/extension/test")));
        }
    }

    static class SampleCustomizer2 implements ServerCustomizer {
        @Override
        public void customize(WireMockServer server) {
            server.stubFor(get("/extension/test").willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")));
        }
    }

    static class SampleCustomizer3 implements ServerCustomizer {
        @Override
        public void customize(WireMockServer server) {
            server.stubFor(get("/extension/failed").willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"down\"}")));
        }
    }

    @Nested
    @ExtendWith(WireMockExtension.class)
    class ExtensionMethodInjectionTest {

        @Test
        void shouldRunOnDefinedPort(@Wiremock(port = 8123) WireMockServer server) {
            // GIVEN
            server.stubFor(get(urlEqualTo("/extension")).willReturn(temporaryRedirect("/extension/test")));
            final WireMockTestClient testClient = new WireMockTestClient(server.port());

            // WHEN
            final WireMockResponse response = testClient.get("/extension");

            // THEN
            assertEquals(8123, server.port());
            assertEquals(302, response.statusCode());
            assertEquals("/extension/test", response.firstHeader("Location"));
        }

        @Test
        void shouldRunOnDynamicPort(@Wiremock WireMockServer server) {
            // GIVEN
            server.stubFor(get("/extension/test").willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")));
            final WireMockTestClient testClient = new WireMockTestClient(server.port());

            // WHEN
            final WireMockResponse response = testClient.get("/extension/test");

            // THEN
            assertEquals(200, response.statusCode());
            assertEquals("{\"status\": \"ok\"}", response.content());
        }

        @Test
        void shouldRunWithCustomizers(
                @Wiremock(customizers = {SampleCustomizer1.class, SampleCustomizer2.class, SampleCustomizer3.class})
                final WireMockServer server) {
            // GIVEN
            final WireMockTestClient testClient = new WireMockTestClient(server.port());

            // WHEN
            final WireMockResponse response1 = testClient.get("/extension");
            final WireMockResponse response2 = testClient.get("/extension/test");
            final WireMockResponse response3 = testClient.get("/extension/failed");

            // THEN
            assertEquals(302, response1.statusCode());
            assertEquals("/extension/test", response1.firstHeader("Location"));
            assertEquals(200, response2.statusCode());
            assertEquals("{\"status\": \"ok\"}", response2.content());
            assertEquals(500, response3.statusCode());
            assertEquals("{\"status\": \"down\"}", response3.content());
        }

        @Test
        void shouldNotFailedOnUnmatchedRequests(@Wiremock(failOnUnmatchedRequests = false) WireMockServer server) {
            // GIVEN
            final WireMockTestClient testClient = new WireMockTestClient(server.port());

            // WHEN
            final WireMockResponse response = testClient.get("/extension/unmatched");

            // THEN
            assertEquals(404, response.statusCode());
        }

        @Test
        void shouldHaveVerboseModeEnable(@Wiremock(verbose = true) WireMockServer server) {
            // GIVEN
            server.stubFor(get("/extension/test").willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")));
            final WireMockTestClient testClient = new WireMockTestClient(server.port());

            // WHEN
            final WireMockResponse response = testClient.get("/extension/test");

            // THEN
            assertEquals(200, response.statusCode());
            assertEquals("{\"status\": \"ok\"}", response.content());
        }

        @Test
        void shouldUseCustomFactory(@Wiremock(factory = SampleCustomConfigurationFactory.class) WireMockServer server) {
            // GIVEN
            server.stubFor(get("/extension/test").willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")));
            final WireMockTestClient testClient = new WireMockTestClient(server.port());

            // WHEN
            final WireMockResponse response = testClient.get("/extension/test");

            // THEN
            assertEquals(9876, server.port());
            assertEquals(200, response.statusCode());
            assertEquals("{\"status\": \"ok\"}", response.content());
        }

    }

    @Nested
    @ExtendWith(WireMockExtension.class)
    class ExtensionConstructorInjectionTest {

        private final WireMockServer server;

        ExtensionConstructorInjectionTest(
                @Wiremock(customizers = {SampleCustomizer1.class, SampleCustomizer2.class, SampleCustomizer3.class})
                final WireMockServer server) {
            this.server = server;
        }

        @Test
        void shouldServerBeConfigurable() {
            // GIVEN
            server.stubFor(get("/extension/test-constructor").willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")));
            final WireMockTestClient testClient = new WireMockTestClient(server.port());

            // WHEN
            final WireMockResponse response = testClient.get("/extension/test");

            // THEN
            assertEquals(200, response.statusCode());
            assertEquals("{\"status\": \"ok\"}", response.content());
        }

        @Test
        void shouldUseTheCustomizer() {
            // GIVEN
            final WireMockTestClient testClient = new WireMockTestClient(server.port());

            // WHEN
            final WireMockResponse response1 = testClient.get("/extension");
            final WireMockResponse response2 = testClient.get("/extension/test");
            final WireMockResponse response3 = testClient.get("/extension/failed");

            // THEN
            assertEquals(302, response1.statusCode());
            assertEquals("/extension/test", response1.firstHeader("Location"));
            assertEquals(200, response2.statusCode());
            assertEquals("{\"status\": \"ok\"}", response2.content());
            assertEquals(500, response3.statusCode());
            assertEquals("{\"status\": \"down\"}", response3.content());
        }
    }

    @Nested
    @ExtendWith(WireMockExtension.class)
    class ExtensionFieldInjectionTest {

        @Wiremock(customizers = {SampleCustomizer1.class, SampleCustomizer2.class, SampleCustomizer3.class})
        private WireMockServer server;

        @Test
        void shouldServerBeConfigurable() {
            // GIVEN
            server.stubFor(get("/extension/test-constructor").willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")));
            final WireMockTestClient testClient = new WireMockTestClient(server.port());

            // WHEN
            final WireMockResponse response = testClient.get("/extension/test");

            // THEN
            assertEquals(200, response.statusCode());
            assertEquals("{\"status\": \"ok\"}", response.content());
        }

        @Test
        void shouldUseTheCustomizer() {
            // GIVEN
            final WireMockTestClient testClient = new WireMockTestClient(server.port());

            // WHEN
            final WireMockResponse response1 = testClient.get("/extension");
            final WireMockResponse response2 = testClient.get("/extension/test");
            final WireMockResponse response3 = testClient.get("/extension/failed");

            // THEN
            assertEquals(302, response1.statusCode());
            assertEquals("/extension/test", response1.firstHeader("Location"));
            assertEquals(200, response2.statusCode());
            assertEquals("{\"status\": \"ok\"}", response2.content());
            assertEquals(500, response3.statusCode());
            assertEquals("{\"status\": \"down\"}", response3.content());
        }
    }

    @Nested
    @ExtendWith(WireMockExtension.class)
    class DeepInjectionTest {

        @Wiremock(customizers = {SampleCustomizer1.class, SampleCustomizer2.class})
        private WireMockServer server;

        @Nested
        class SampleDeepNestedTest {

            @Test
            void shouldServerBeAvailable() {
                // GIVEN
                final WireMockTestClient testClient = new WireMockTestClient(server.port());

                // WHEN
                final WireMockResponse response = testClient.get("/extension/test");

                // THEN
                assertEquals(200, response.statusCode());
                assertEquals("{\"status\": \"ok\"}", response.content());
            }

        }

        @Nested
        @Customize(SampleCustomizer2.class)
        class SampleDeepCustomization {

            @Test
            @Disabled("Not implemented")
            @Customize(SampleCustomizer3.class)
            void shouldUseCustomizer() {
                Assertions.fail("Not implemented - require to set name on server");
            }
        }

    }

}
