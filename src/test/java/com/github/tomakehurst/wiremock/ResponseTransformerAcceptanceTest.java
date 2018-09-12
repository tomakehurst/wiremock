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

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Extension;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.HttpHeader.httpHeader;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ResponseTransformerAcceptanceTest {

    WireMockServer wm;
    WireMockTestClient client;

    @Test
    public void transformsStubResponse() {
        startWithExtensions(StubResponseTransformer.class);

        wm.stubFor(get(urlEqualTo("/response-transform")).willReturn(aResponse().withBody("Original body")));

        assertThat(client.get("/response-transform").content(), is("Modified body"));
    }

    @Test
    public void transformsLocalStubResponse() {
        startWithExtensions();

        wm.stubFor(get(urlEqualTo("/response-transform")).willReturn(aResponse().withBody("Original body")
                .withTransformers(new StubResponseTransformer())
        ));

        assertThat(client.get("/response-transform").content(), is("Modified body"));
    }

    @Test
    public void transformsNonGlobalLocalStubResponse() {
        startWithExtensions();

        wm.stubFor(get(urlEqualTo("/response-transform")).willReturn(aResponse().withBody("Original body")
                .withTransformers(new NonGlobalStubResponseTransformer())
        ));

        assertThat(client.get("/response-transform").content(), is("Modified body"));
    }

    @Test
    public void acceptsTransformerParameters() {
        startWithExtensions(StubResponseTransformerWithParams.class);

        wm.stubFor(get(urlEqualTo("/response-transform-with-params")).willReturn(
                aResponse()
                        .withTransformerParameter("name", "John")
                        .withTransformerParameter("number", 66)
                        .withTransformerParameter("flag", true)
                        .withBody("Original body")));

        assertThat(client.get("/response-transform-with-params").content(), is("John, 66, true"));
    }

    @Test
    public void globalTransformAppliedWithLocalParameters() {
        startWithExtensions(GlobalResponseTransformer.class);

        wm.stubFor(get(urlEqualTo("/global-response-transform")).willReturn(aResponse()));

        assertThat(client.get("/global-response-transform").firstHeader("X-Extra"), is("extra val"));
    }

    @SuppressWarnings("unchecked")
    private void startWithExtensions(Class<? extends Extension>... extensionClasses) {
        wm = new WireMockServer(wireMockConfig().dynamicPort().extensions(extensionClasses));
        wm.start();
        client = new WireMockTestClient(wm.port());
    }

    public static class StubResponseTransformer extends ResponseTransformer {

        @Override
        public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
            return Response.Builder.like(response)
                    .but().body("Modified body")
                    .build();
        }

        @Override
        public boolean applyGlobally() {
            return true;
        }

        @Override
        public String getName() {
            return "stub-transformer";
        }
    }

    public static class NonGlobalStubResponseTransformer extends ResponseTransformer {

        @Override
        public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
            return Response.Builder.like(response)
                    .but().body("Modified body")
                    .build();
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }

        @Override
        public String getName() {
            return "non-global-stub-transformer";
        }
    }

    public static class StubResponseTransformerWithParams extends ResponseTransformer {

        @Override
        public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
            return Response.Builder.like(response)
                    .but().body(parameters.getString("name") + ", "
                            + parameters.getInt("number") + ", "
                            + parameters.getBoolean("flag"))
                    .build();
        }

        @Override
        public boolean applyGlobally() {
            return true;
        }

        @Override
        public String getName() {
            return "stub-transformer-with-params";
        }
    }

    public static class GlobalResponseTransformer extends ResponseTransformer {

        @Override
        public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
            return Response.Builder.like(response).but()
                    .headers(response.getHeaders().plus(httpHeader("X-Extra", "extra val")))
                    .build();
        }

        @Override
        public String getName() {
            return "global-response-transformer";
        }

        @Override
        public boolean applyGlobally() {
            return true;
        }
    }
}
