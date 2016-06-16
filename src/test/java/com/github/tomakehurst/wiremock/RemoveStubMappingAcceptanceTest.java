package com.github.tomakehurst.wiremock;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.base.Predicate;
import org.junit.Test;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.collect.FluentIterable.from;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RemoveStubMappingAcceptanceTest extends AcceptanceTestBase {


    private synchronized int getMatchingStubCount(String s1,String s2){
        return from(wireMockServer.listAllStubMappings().getMappings()).filter(withUrl(s1)).size()
                +
                from(wireMockServer.listAllStubMappings().getMappings()).filter(withUrl(s2)).size();
    }
    @Test
    public void removeStubThatExistsUsingUUID() {

        UUID id1 = UUID.randomUUID();

        wireMockServer.stubFor(get(urlEqualTo("/stub-1"))
                .withId(id1)
                .willReturn(aResponse()
                        .withBody("Stub-1-Body")));

        assertThat(testClient.get("/stub-1").content(), is("Stub-1-Body"));

        UUID id2 = UUID.randomUUID();
        wireMockServer.stubFor(get(urlEqualTo("/stub-2"))
                .withId(id2)
                .willReturn(aResponse()
                        .withBody("Stub-2-Body")));

        assertThat(testClient.get("/stub-2").content(), is("Stub-2-Body"));

        assertThat(getMatchingStubCount("/stub-1","/stub-2"), is(2));

        wireMockServer.removeStubMapping(get(urlEqualTo("/stub-2"))
                .withId(id2)
                .willReturn(aResponse().withBody("Stub-2-Body")));

        assertThat(getMatchingStubCount("/stub-1","/stub-2"), is(1));

        wireMockServer.removeStubMapping(get(urlEqualTo("/stub-1"))
                .withId(id1)
                .willReturn(aResponse().withBody("Stub-1-Body")));

        assertThat(getMatchingStubCount("/stub-1","/stub-2"), is(0));

    }
    @Test
    public void removeStubThatExistsUsingRequestMatchUUIDNotMatch() {

        UUID id1 = UUID.randomUUID();

        wireMockServer.stubFor(get(urlEqualTo("/stub-11"))
                .withId(id1)
                .willReturn(aResponse()
                        .withBody("Stub-11-Body")));

        assertThat(testClient.get("/stub-11").content(), is("Stub-11-Body"));

        UUID id2 = UUID.randomUUID();
        wireMockServer.stubFor(get(urlEqualTo("/stub-22"))
                .withId(id2)
                .willReturn(aResponse()
                        .withBody("Stub-22-Body")));

        assertThat(testClient.get("/stub-22").content(), is("Stub-22-Body"));

        assertThat(getMatchingStubCount("/stub-11","/stub-22"), is(2));

        UUID id3 = UUID.randomUUID();
        wireMockServer.removeStubMapping(get(urlEqualTo("/stub-22"))
                .withId(id3)
                .willReturn(aResponse().withBody("Stub-22-Body")));

        assertThat(getMatchingStubCount("/stub-11","/stub-22"), is(1));

        UUID id4 = UUID.randomUUID();
        wireMockServer.removeStubMapping(get(urlEqualTo("/stub-11"))
                .withId(id4)
                .willReturn(aResponse().withBody("Stub-11-Body")));

        assertThat(getMatchingStubCount("/stub-11","/stub-22"), is(0));

    }
    @Test
    public void removeStubThatExistsWithRequestMatchNoUUIDPresent() {

        UUID id1 = UUID.randomUUID();

        wireMockServer.stubFor(get(urlEqualTo("/stub-111"))
                .withId(id1)
                .willReturn(aResponse()
                        .withBody("Stub-111-Body")));

        assertThat(testClient.get("/stub-111").content(), is("Stub-111-Body"));

        UUID id2 = UUID.randomUUID();
        wireMockServer.stubFor(get(urlEqualTo("/stub-222"))
                .withId(id2)
                .willReturn(aResponse()
                        .withBody("Stub-222-Body")));

        assertThat(testClient.get("/stub-222").content(), is("Stub-222-Body"));

        assertThat(getMatchingStubCount("/stub-111","/stub-222"), is(2));

        wireMockServer.removeStubMapping(get(urlEqualTo("/stub-222"))
                .willReturn(aResponse().withBody("Stub-222-Body")));

        assertThat(getMatchingStubCount("/stub-111","/stub-222"), is(1));

        wireMockServer.removeStubMapping(get(urlEqualTo("/stub-111"))
                .willReturn(aResponse().withBody("Stub-111-Body")));

        assertThat(getMatchingStubCount("/stub-111","/stub-222"), is(0));

    }
    @Test
    public void removeStubThatDoesNotExists() {

        UUID id1 = UUID.randomUUID();

        wireMockServer.stubFor(get(urlEqualTo("/stb-1"))
                .withId(id1)
                .willReturn(aResponse()
                        .withBody("Stb-1-Body")));

        assertThat(testClient.get("/stb-1").content(), is("Stb-1-Body"));

        UUID id2 = UUID.randomUUID();
        wireMockServer.stubFor(get(urlEqualTo("/stb-2"))
                .withId(id2)
                .willReturn(aResponse()
                        .withBody("Stb-2-Body")));

        assertThat(testClient.get("/stb-2").content(), is("Stb-2-Body"));

        assertThat(getMatchingStubCount("/stb-1","/stb-2"), is(2));

        UUID id3 = UUID.randomUUID();
        wireMockServer.removeStubMapping(get(urlEqualTo("/stb-3"))
                .withId(id3)
                .willReturn(aResponse().withBody("Stb-3-Body")));

        assertThat(getMatchingStubCount("/stb-1","/stb-2"), is(2));

    }

    private Predicate<StubMapping> withUrl(final String url) {
        return new Predicate<StubMapping>() {
            public boolean apply(StubMapping mapping) {
                return (mapping.getRequest().getUrl() != null && mapping.getRequest().getUrl().equals(url));
            }
        };
    }


}
