package com.github.tomakehurst.wiremock.http;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.MockRequest;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ResponseDefinitionTest {

    @Test
    public void getProxyUrlGivesBackRequestUrlIfBrowserProxyRequest() {
        ResponseDefinition response = ResponseDefinition.browserProxy(MockRequest.mockRequest()
                .host("http://my.domain").url("/path").isBrowserProxyRequest(true));

        assertThat(response.getProxyUrl(), equalTo("http://my.domain/path"));
    }

    @Test
    public void getProxyUrlGivesBackTheProxyUrlWhenNotBrowserProxy() {
        ResponseDefinition response = ResponseDefinitionBuilder.responseDefinition()
                .proxiedFrom("http://my.proxy.url")
                .build();

        response.setOriginalRequest(MockRequest.mockRequest().url("/path"));

        assertThat(response.getProxyUrl(), equalTo("http://my.proxy.url/path"));
    }

    @Test
    public void doesNotRemoveRequestPathPrefixWhenPrefixToRemoveDoesNotMatch() {
        ResponseDefinition response = ResponseDefinitionBuilder.responseDefinition()
                .proxiedFrom("http://my.proxy.url")
                .withProxyUrlPrefixToRemove("/no/match")
                .build();

        response.setOriginalRequest(MockRequest.mockRequest().url("/path"));

        assertThat(response.getProxyUrl(), equalTo("http://my.proxy.url/path"));
    }

    @Test
    public void removesRequestPathPrefixWhenPrefixToRemoveMatches() {
        ResponseDefinition response = ResponseDefinitionBuilder.responseDefinition()
                .proxiedFrom("http://my.proxy.url")
                .withProxyUrlPrefixToRemove("/path")
                .build();

        response.setOriginalRequest(MockRequest.mockRequest().url("/path"));

        assertThat(response.getProxyUrl(), equalTo("http://my.proxy.url"));
    }
}
