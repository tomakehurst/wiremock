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
package com.github.tomakehurst.wiremock.recording;

import com.github.tomakehurst.wiremock.common.Timing;
import com.github.tomakehurst.wiremock.http.LoggedResponse;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.testsupport.MockRequestBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.HashMap;
import java.util.Map;
import org.jmock.Mockery;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static com.github.tomakehurst.wiremock.testsupport.MockRequestBuilder.aRequest;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SnapshotStubMappingGeneratorTest {
    @Test
    public void apply() {
        final RequestPatternBuilder requestPatternBuilder = newRequestPattern().withUrl("/foo");
        final ResponseDefinition responseDefinition = ResponseDefinition.ok();

        SnapshotStubMappingGenerator stubMappingTransformer = new SnapshotStubMappingGenerator(
            requestPatternTransformer(requestPatternBuilder),
            responseDefinitionTransformer(responseDefinition)
        );

        StubMapping actual = stubMappingTransformer.apply(serveEvent());
        StubMapping expected = new StubMapping(requestPatternBuilder.build(), responseDefinition);
        expected.setId(actual.getId());
        assertThat(actual, is(expected));
    }

    @Test
    public void applyCaptureHeadersTest() {
        Map<String,String> testRequestHeaders = new HashMap<>();
        testRequestHeaders.put("capture", "test");
        testRequestHeaders.put("noCapture", "test");

        Map<String,CaptureHeadersSpec> headersToCapture = new HashMap<>();
        headersToCapture.put("capture",new CaptureHeadersSpec(true));

        final RequestPatternBuilder expectedRequestPatternBuilder = newRequestPattern().withHeader("capture",new EqualToPattern("test",true));
        final ResponseDefinition responseDefinition = ResponseDefinition.ok();

        SnapshotStubMappingGenerator stubMappingTransformer = new SnapshotStubMappingGenerator(
            new RequestPatternTransformer(headersToCapture,null),
            responseDefinitionTransformer(responseDefinition)
        );

        StubMapping actual = stubMappingTransformer.apply(serveEventWithRequestHeaders(testRequestHeaders));
        StubMapping expected = new StubMapping(expectedRequestPatternBuilder.build(), responseDefinition);

        assertThat(actual.getRequest().getHeaders(), is(expected.getRequest().getHeaders()));
    }

    @Test
    public void applyCaptureAllHeadersTestNoBlackList() {
        Map<String,String> testRequestHeaders = new HashMap<>();
        testRequestHeaders.put("capture", "test");
        testRequestHeaders.put("alsoCapture", "test");

        final RequestPatternBuilder expectedRequestPatternBuilder = newRequestPattern();
        for(String s : testRequestHeaders.keySet()){
            expectedRequestPatternBuilder.withHeader(s, new EqualToPattern(testRequestHeaders.get(s),true));
        }

        final ResponseDefinition responseDefinition = ResponseDefinition.ok();

        SnapshotStubMappingGenerator stubMappingTransformer = new SnapshotStubMappingGenerator(
            new CaptureAllHeadersRequestTransformer(null,null),
            responseDefinitionTransformer(responseDefinition)
        );

        StubMapping actual = stubMappingTransformer.apply(serveEventWithRequestHeaders(testRequestHeaders));
        StubMapping expected = new StubMapping(expectedRequestPatternBuilder.build(), responseDefinition);

        assertThat(actual.getRequest().getHeaders(), is(expected.getRequest().getHeaders()));
    }

    @Test
    public void applyCaptureAllHeadersTestWithBlacklist() {
        Map<String,String> testRequestHeaders = new HashMap<>();
        testRequestHeaders.put("capture", "test");
        testRequestHeaders.put("alsoCapture", "test");
        testRequestHeaders.put("ignore", "test");

        Map<String,CaptureHeadersSpec> headersToIgnore = new HashMap<>();
        headersToIgnore.put("Ignore",new CaptureHeadersSpec(true));

        final RequestPatternBuilder expectedRequestPatternBuilder = newRequestPattern();
        expectedRequestPatternBuilder.withHeader("capture", new EqualToPattern("test",true));
        expectedRequestPatternBuilder.withHeader("alsoCapture", new EqualToPattern("test",true));


        final ResponseDefinition responseDefinition = ResponseDefinition.ok();

        SnapshotStubMappingGenerator stubMappingTransformer = new SnapshotStubMappingGenerator(
            new CaptureAllHeadersRequestTransformer(headersToIgnore,null),
            responseDefinitionTransformer(responseDefinition)
        );

        StubMapping actual = stubMappingTransformer.apply(serveEventWithRequestHeaders(testRequestHeaders));
        StubMapping expected = new StubMapping(expectedRequestPatternBuilder.build(), responseDefinition);

        assertThat(actual.getRequest().getHeaders(), is(expected.getRequest().getHeaders()));
    }



    private static RequestPatternTransformer requestPatternTransformer(final RequestPatternBuilder requestPatternBuilder) {
        return new RequestPatternTransformer(null, null) {
            @Override
            public RequestPatternBuilder apply(Request request) {
                return requestPatternBuilder;
            }
        };
    }

    private static LoggedResponseDefinitionTransformer responseDefinitionTransformer(final ResponseDefinition responseDefinition) {
        return new LoggedResponseDefinitionTransformer() {
            @Override
            public ResponseDefinition apply(LoggedResponse response) {
                return responseDefinition;
            }
        };
    }

    private static ServeEvent serveEvent() {
        return new ServeEvent(
            null,
            LoggedRequest.createFrom(aRequest(new Mockery()).build()),
            null,
            null,
            LoggedResponse.from(Response.notConfigured()),
            false,
            Timing.UNTIMED);
    }
    private static ServeEvent serveEventWithRequestHeaders(Map<String,String> headers) {
        MockRequestBuilder request = aRequest(new Mockery());
        for(String s : headers.keySet()){
            request.withHeader(s, headers.get(s));
        }
        return new ServeEvent(
            null,
            LoggedRequest.createFrom(request.build()),
            null,
            null,
            LoggedResponse.from(Response.notConfigured()),
            false,
            Timing.UNTIMED);
    }
}
