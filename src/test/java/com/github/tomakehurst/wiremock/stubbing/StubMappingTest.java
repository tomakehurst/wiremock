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
package com.github.tomakehurst.wiremock.stubbing;

import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class StubMappingTest {

    @Test
    public void excludesInsertionIndexFromPublicView() {
        StubMapping stub = get("/saveable").willReturn(ok()).build();

        String json = Json.write(stub);
        System.out.println(json);

        assertThat(json, not(containsString("insertionIndex")));
    }

    @Test
    public void includedInsertionIndexInPrivateView() {
        StubMapping stub = get("/saveable").willReturn(ok()).build();

        String json = Json.writePrivate(stub);
        System.out.println(json);

        assertThat(json, containsString("insertionIndex"));
    }

    @Test
    public void deserialisesInsertionIndex() {
        String json =
            "{\n" +
            "    \"request\": {\n" +
            "        \"method\": \"ANY\",\n" +
            "        \"url\": \"/\"\n" +
            "    },\n" +
            "    \"response\": {\n" +
            "        \"status\": 200\n" +
            "    },\n" +
            "    \"insertionIndex\": 42\n" +
            "}";

        StubMapping stub = Json.read(json, StubMapping.class);

        assertThat(stub.getInsertionIndex(), is(42L));
    }

    @Test
    public void deserialisesSample() throws IOException {
        URL url = Resources.getResource("sample.json");
        StubMapping stub = Json.read(Resources.toString(url, Charsets.UTF_8), StubMapping.class);

        assertThat(stub.getRequest().getMethod(), is(RequestMethod.GET));
        assertThat(stub.getRequest().getUrl(), is("/my/other/resource"));
        assertThat(stub.getResponse().getStatus(), is(200));
        assertThat(stub.getResponse().getBody(), is("YES INDEED!"));
    }

    @Test
    public void deserialisesSequenceSample() throws IOException {
        URL url = Resources.getResource("sequence_sample.json");
        StubMapping stub = Json.read(Resources.toString(url, Charsets.UTF_8), StubMapping.class);

        assertThat(stub.getResponseSequence().getResponses().size(), is(2));
        assertThat(stub.getResponseSequence().isLoopResponseSequence(), is(true));
        assertThat(stub.getResponseSequence().getResponses().get(0).getStatus(), is(200));
        assertThat(stub.getResponseSequence().getResponses().get(0).getBody(), is("YES INDEED!"));
        assertThat(stub.getResponseSequence().getResponses().get(1).getStatus(), is(500));
        assertThat(stub.getResponseSequence().getResponses().get(1).getBody(), is("PERHAPS NOT!"));
    }
}
