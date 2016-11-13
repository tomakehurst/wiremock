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

import com.github.tomakehurst.wiremock.testsupport.TestHttpHeader;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class OptionalMatchersAcceptanceTest extends AcceptanceTestBase {

    @Test
    public void optionalRequestBodyMatchingTest() {
        final String url = "/optional-body";
        final String emailJsonPath = "$[?(@.email =~ /([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4})?/)]";
        final String nameJsonPath = "$[?(@.name =~ /([a-zA-Z0-9._%+-]+)?/)]";
        final String surnameJsonPath = "$[?(@.surname =~ /([a-zA-Z0-9._%+-]+)?/)]";
        final String jsonWithEmail =
                "{                                      \n" +
                "  \"name\":\"John\",                   \n" +
                "  \"surname\":\"Doe\",                 \n" +
                "  \"email\":\"john.doe@example.com\"   \n" +
                "}";

        final String jsonWithoutEmail =
                "{                                      \n" +
                "  \"name\":\"John\",                   \n" +
                "  \"surname\":\"Doe\",                 \n" +
                "}";

        final String jsonWithInvalidEmail =
                "{                                      \n" +
                "  \"name\":\"John\",                   \n" +
                "  \"surname\":\"Doe\",                 \n" +
                "  \"email\":\"john.doe\"   \n" +
                "}";

        stubFor(post(urlEqualTo(url))
                .withRequestBody(optionalMatchingJsonPath(emailJsonPath))
                .withRequestBody(optionalMatchingJsonPath(nameJsonPath))
                .withRequestBody(optionalMatchingJsonPath(surnameJsonPath))
                .willReturn(aResponse().withStatus(201)));

        WireMockResponse response = testClient.postJson(url, jsonWithEmail);
        assertThat(response.statusCode(), is(201));

        response = testClient.postJson(url, jsonWithoutEmail);
        assertThat(response.statusCode(), is(201));

        response = testClient.postJson(url, jsonWithInvalidEmail);
        assertThat(response.statusCode(), is(404));
    }

    @Test
    public void optionalRequestParametersMatchingTest() {
        final String url = "/optional-query-params";

        stubFor(get(urlPathEqualTo(url))
                .withQueryParam("id", matching("[0-9]+"))
                .withQueryParam("name", optionalMatching("[a-zA-Z]+"))
                .willReturn(aResponse().withStatus(200)));

        WireMockResponse response = testClient.get(url);
        assertThat(response.statusCode(), is(404));

        response = testClient.get(url + "?id=1");
        assertThat(response.statusCode(), is(200));

        response = testClient.get(url + "?id=1&name=John");
        assertThat(response.statusCode(), is(200));

        response = testClient.get(url + "?id=1&name=_J0hn_");
        assertThat(response.statusCode(), is(404));

        response = testClient.get(url + "?name=John&id=1");
        assertThat(response.statusCode(), is(200));
    }

    @Test
    public void optionalHeadersMatchingTest() {
        final String url = "/optional-headers-params";
        final TestHttpHeader contentTypeTextPlainHeader = new TestHttpHeader("Content-Type", "text/plain");
        final TestHttpHeader acceptTextPlainHeader = new TestHttpHeader("Accept", "text/plain");
        final TestHttpHeader acceptApplicationJsonHeader = new TestHttpHeader("Accept", "application/json");

        stubFor(get(urlEqualTo(url))
                .withHeader("Content-Type", equalTo("text/plain"))
                .withHeader("Accept", optionalEqualTo("text/plain"))
                .willReturn(aResponse().withStatus(200)));

        WireMockResponse response = testClient.get(url);
        assertThat(response.statusCode(), is(404));

        response = testClient.get(url, contentTypeTextPlainHeader);
        assertThat(response.statusCode(), is(200));

        response = testClient.get(url, contentTypeTextPlainHeader, acceptTextPlainHeader);
        assertThat(response.statusCode(), is(200));

        response = testClient.get(url, acceptTextPlainHeader, contentTypeTextPlainHeader);
        assertThat(response.statusCode(), is(200));

        response = testClient.get(url, acceptApplicationJsonHeader, contentTypeTextPlainHeader);
        assertThat(response.statusCode(), is(404));
    }
}
