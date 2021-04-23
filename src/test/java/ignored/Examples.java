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
package ignored;

import com.github.tomakehurst.wiremock.AcceptanceTestBase;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.requestfilter.FieldTransformer;
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilterAction;
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestWrapper;
import com.github.tomakehurst.wiremock.extension.requestfilter.StubRequestFilter;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class Examples extends AcceptanceTestBase {

    @Test
    public void exactUrlOnly() {
        stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "text/plain")
                    .withBody("Hello world!")));

        assertThat(testClient.get("/some/thing").statusCode(), is(200));
        assertThat(testClient.get("/some/thing/else").statusCode(), is(404));
    }

    @Test
    public void urlRegexMatch() {
        stubFor(put(urlMatching("/thing/matching/[0-9]+"))
                .willReturn(aResponse().withStatus(200)));
    }

    @Test
    public void headerMatching() {
        stubFor(post(urlEqualTo("/with/headers"))
                .withHeader("Content-Type", equalTo("text/xml"))
                .withHeader("Accept", matching("text/.*"))
                .withHeader("etag", notMatching("abcd.*"))
                .withHeader("etag", containing("2134"))
                .willReturn(aResponse().withStatus(200)));
    }

    @Test
    public void bodyMatching() {
        stubFor(post(urlEqualTo("/with/body"))
                .withRequestBody(matching("<status>OK</status>"))
                .withRequestBody(notMatching("<status>ERROR</status>"))
                .willReturn(aResponse().withStatus(200)));
    }

    @Test
    public void binaryBodyMatchingByteArray() {
        stubFor(post(urlEqualTo("/with/body"))
            .withRequestBody(binaryEqualTo(new byte[] { 1, 2, 3 }))
            .willReturn(ok()));
    }

    @Test
    public void binaryBodyMatchingBase64() {
        stubFor(post(urlEqualTo("/with/body"))
            .withRequestBody(binaryEqualTo("AQID"))
            .willReturn(ok()));
    }

    @Test
    public void multipartBodyMatchingBase64() {
        stubFor(post(urlEqualTo("/with/multipart"))
                .withMultipartRequestBody(aMultipart()
                        .withBody(binaryEqualTo("Content")))
                .willReturn(ok()));
    }

    @Test
    public void priorities() {

        //Catch-all case
        stubFor(get(urlMatching("/api/.*")).atPriority(5)
                .willReturn(aResponse().withStatus(401)));

        //Specific case
        stubFor(get(urlEqualTo("/api/specific-resource")).atPriority(1) //1 is highest
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Resource state")));
    }

    @Test
    public void responseHeaders() {
        stubFor(get(urlEqualTo("/whatever"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Etag", "b13894794wb")));
    }

    @Test
    public void bodyFile() {
        stubFor(get(urlEqualTo("/body-file"))
                .willReturn(aResponse()
                        .withBodyFile("path/to/myfile.xml")));
    }

    @Test
    public void binaryBody() {
        stubFor(get(urlEqualTo("/binary-body"))
                .willReturn(aResponse()
                        .withBody(new byte[]{1, 2, 3, 4})));
    }

    @Test(expected=VerificationException.class)
    public void verifyAtLeastOnce() {
        verify(postRequestedFor(urlEqualTo("/verify/this"))
                .withHeader("Content-Type", equalTo("text/xml")));

        verify(3, postRequestedFor(urlEqualTo("/3/of/these")));
    }

    @Test(expected=VerificationException.class)
    public void verifyWithoutHeader() {
        verify(putRequestedFor(urlEqualTo("/without/header")).withoutHeader("Content-Type"));
    }

    @Test
    public void findingRequests() {
        List<LoggedRequest> requests = findAll(putRequestedFor(urlMatching("/api/.*")));
    }

    @Test
    public void proxying() {
        stubFor(get(urlMatching("/other/service/.*"))
                .willReturn(aResponse().proxiedFrom("http://otherhost.com/approot")));
    }

    @Test
    public void proxyIntercept() {
        // Low priority catch-all proxies to otherhost.com by default
        stubFor(get(urlMatching(".*")).atPriority(10)
                .willReturn(aResponse().proxiedFrom("http://otherhost.com")));


        // High priority stub will send a Service Unavailable response
        // if the specified URL is requested
        stubFor(get(urlEqualTo("/api/override/123")).atPriority(1)
                .willReturn(aResponse().withStatus(503)));
    }

    @Test
    public void toDoListScenario() {
        stubFor(get(urlEqualTo("/todo/items")).inScenario("To do list")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withBody("<items>" +
                                "   <item>Buy milk</item>" +
                                "</items>")));

        stubFor(post(urlEqualTo("/todo/items")).inScenario("To do list")
                .whenScenarioStateIs(STARTED)
                .withRequestBody(containing("Cancel newspaper subscription"))
                .willReturn(aResponse().withStatus(201))
                .willSetStateTo("Cancel newspaper item added"));

        stubFor(get(urlEqualTo("/todo/items")).inScenario("To do list")
                .whenScenarioStateIs("Cancel newspaper item added")
                .willReturn(aResponse()
                        .withBody("<items>" +
                                "   <item>Buy milk</item>" +
                                "   <item>Cancel newspaper subscription</item>" +
                                "</items>")));

        WireMockResponse response = testClient.get("/todo/items");
        assertThat(response.content(), containsString("Buy milk"));
        assertThat(response.content(), not(containsString("Cancel newspaper subscription")));

        response = testClient.postWithBody("/todo/items", "Cancel newspaper subscription", "text/plain", "UTF-8");
        assertThat(response.statusCode(), is(201));

        response = testClient.get("/todo/items");
        assertThat(response.content(), containsString("Buy milk"));
        assertThat(response.content(), containsString("Cancel newspaper subscription"));
    }

    @Test
    public void delay() {
        stubFor(get(urlEqualTo("/delayed")).willReturn(
                aResponse()
                        .withStatus(200)
                        .withFixedDelay(2000)));
    }

    @Test
    public void fault() {
        stubFor(get(urlEqualTo("/fault"))
                .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    }

    @Test
    public void xpath() {
        stubFor(put(urlEqualTo("/xpath"))
            .withRequestBody(matchingXPath("/todo-list[count(todo-item) = 3]"))
            .willReturn(aResponse().withStatus(200)));
    }

    @Test
    public void xpathWithNamespaces() {
        stubFor(put(urlEqualTo("/namespaced/xpath"))
                .withRequestBody(matchingXPath("/stuff:outer/stuff:inner[.=111]")
                        .withXPathNamespace("stuff", "http://foo.com"))
                .willReturn(aResponse().withStatus(200)));
    }

    @Test
    public void advancedXPathMatching() {
        stubFor(put(urlEqualTo("/xpath"))
            .withRequestBody(matchingXPath("//todo-item/text()", containing("wash")))
            .willReturn(aResponse().withStatus(200)));
    }

    @Test
    public void advancedJSONPathMatching() {
        stubFor(put(urlEqualTo("/jsonpath"))
            .withRequestBody(matchingJsonPath("$..todoItem", containing("wash")))
            .willReturn(aResponse().withStatus(200)));
    }

    @Test
    public void advancedJSONPathMatchingWithObject() {
        System.out.println(matchingJsonPath("$.outer",
            equalToJson(
            "{\n" +
            "        \"inner\": 42\n" +
            "    }"))
            .match(
            "{\n" +
            "    \"outer\": {\n" +
            "        \"inner\": 42\n" +
            "    }\n" +
            "}").isExactMatch());
    }

    @Test
    public void transformerParameters() {
        stubFor(get(urlEqualTo("/transform")).willReturn(
                aResponse()
                        .withTransformerParameter("newValue", 66)
                        .withTransformerParameter("inner", ImmutableMap.of("thing", "value"))));

        System.out.println(get(urlEqualTo("/transform")).willReturn(
                aResponse()
                        .withTransformerParameter("newValue", 66)
                        .withTransformerParameter("inner", ImmutableMap.of("thing", "value"))).build());
    }

    @Test
    public void transformerWithParameters() {
        stubFor(get(urlEqualTo("/transform")).willReturn(
                aResponse()
                        .withTransformer("body-transformer", "newValue", 66)));

        System.out.println(get(urlEqualTo("/transform")).willReturn(
                aResponse()
                        .withTransformer("body-transformer", "newValue", 66)).build());
    }

    @Test
    public void customMatcherName() {
        stubFor(requestMatching("body-too-long", Parameters.one("maxLemgth", 2048))
                .willReturn(aResponse().withStatus(422)));

        System.out.println(requestMatching("body-too-long", Parameters.one("maxLemgth", 2048))
                .willReturn(aResponse().withStatus(422)).build());
    }

    @Test
    public void customMatcher() {
        wireMockServer.stubFor(requestMatching(new RequestMatcherExtension() {
            @Override
            public MatchResult match(Request request, Parameters parameters) {
                return MatchResult.of(request.getBody().length > 2048);
            }
        }).willReturn(aResponse().withStatus(422)));
    }

    @Test
    public void tmp() {
        System.out.println(Json.write(
            any(urlPathEqualTo("/everything"))
            .withHeader("Accept", containing("xml"))
            .withCookie("session", matching(".*12345.*"))
            .withQueryParam("search_term", equalTo("WireMock"))
            .withBasicAuth("jeff@example.com", "jeffteenjefftyjeff")
            .withRequestBody(equalToXml("<search-results />"))
            .withRequestBody(matchingXPath("//search-results"))
            .withMultipartRequestBody(
                aMultipart()
                    .withName("info")
                    .withHeader("Content-Type", containing("charset"))
                    .withBody(equalToJson("{}"))
            )
            .willReturn(aResponse()).build()));

        stubFor(any(urlPathEqualTo("/everything"))
            .withHeader("Accept", containing("xml"))
            .withCookie("session", matching(".*12345.*"))
            .withQueryParam("search_term", equalTo("WireMock"))
            .withBasicAuth("jeff@example.com", "jeffteenjefftyjeff")
            .withRequestBody(equalToXml("<search-results />"))
            .withRequestBody(matchingXPath("//search-results"))
            .withMultipartRequestBody(
                aMultipart()
                    .withName("info")
                    .withHeader("Content-Type", containing("charset"))
                    .withBody(equalToJson("{}"))
            )
            .willReturn(aResponse()));
    }

    @Test
    public void removeStubMapping() {
        StubMapping stubMapping = stubFor(get(urlEqualTo("/delete-me")).willReturn(aResponse().withStatus(200)));
        assertThat(testClient.get("/delete-me").statusCode(), is(200));

        removeStub(stubMapping);
        assertThat(testClient.get("/delete-me").statusCode(), is(404));
    }

    @Test
    public void servedStubs() {
        List<ServeEvent> allServeEvents = getAllServeEvents();
    }

    @Test
    public void configuration() {
        WireMockConfiguration.options()
            // Statically set the HTTP port number. Defaults to 8080.
            .port(8000)

            // Disable HTTP listener.
            .httpDisabled(true)

            // Statically set the HTTPS port number. Defaults to 8443.
            .httpsPort(8001)

            // Randomly assign the HTTP port on startup
            .dynamicPort()

            // Randomly asssign the HTTPS port on startup
            .dynamicHttpsPort()

            // Bind the WireMock server to this IP address locally. Defaults to the loopback adaptor.
            .bindAddress("192.168.1.111")

            // Set the number of request handling threads in Jetty. Defaults to 10.
            .containerThreads(5)

            // Set the number of connection acceptor threads in Jetty. Defaults to 2.
            .jettyAcceptors(4)

            // Set the Jetty accept queue size. Defaults to Jetty's default of unbounded.
            .jettyAcceptQueueSize(100)

            // Deprecated. Set the size of Jetty's header buffer (to avoid exceptions when very large request headers are sent). Defaults to 8192.
            .jettyHeaderBufferSize(16834)

            // Set the size of Jetty's request header buffer (to avoid exceptions when very large request headers are sent). Defaults to 8192.
            .jettyHeaderRequestSize(16834)

            // Set the size of Jetty's response header buffer (to avoid exceptions when very large request headers are sent). Defaults to 8192.
            .jettyHeaderResponseSize(16834)

            // Set the timeout to wait for Jetty to stop in milliseconds. Defaults to 0 (no wait)
            .jettyStopTimeout(5000L)

            // Set the keystore containing the HTTPS certificate
            .keystorePath("/path/to/https-certs-keystore.jks")

            // Set the password to the keystore
            .keystorePassword("verysecret!")

            // Require a client calling WireMock to present a client certificate
            .needClientAuth(true)

            // Path to the trust store containing the client certificate required in by the previous parameter
            .trustStorePath("/path/to/trust-store.jks")

            // The password to the trust store
            .trustStorePassword("trustme")

            // Make WireMock behave as a forward proxy e.g. via browser proxy settings
            .enableBrowserProxying(true)

            // Send the Host header in the original request onwards to the system being proxied to
            .preserveHostHeader(false)

            // Override the Host header sent when reverse proxying to another system (this and the previous parameter are mutually exclusive)
            .proxyHostHeader("my.otherdomain.com")

            // When reverse proxying, also route via the specified forward proxy (useful inside corporate firewalls)
            .proxyVia("my.corporate.proxy", 8080)

            // Set the root of the filesystem WireMock will look under for files and mappings
            .usingFilesUnderDirectory("/path/to/files-and-mappings-root")

            // Set a path within the classpath as the filesystem root
            .usingFilesUnderClasspath("root/path/under/classpath")

            // Do not record received requests. Typically needed during load testing to avoid JVM heap exhaustion.
            .disableRequestJournal()

            // Limit the size of the request log (for the same reason as above).
            .maxRequestJournalEntries(Optional.of(100))

            // Provide an alternative notifier.
            .notifier(new ConsoleNotifier(true)
        );
    }

    @Test
    public void abbreviatedDsl() {
        stubFor(get("/some/thing").willReturn(aResponse().withStatus(200)));

        stubFor(delete("/fine").willReturn(ok()));
        stubFor(get("/json").willReturn(okJson("{ \"message\": \"Hello\" }")));
        stubFor(get("/xml").willReturn(okXml("<hello />")));     // application/xml
        stubFor(get("/xml").willReturn(okTextXml("<hello />"))); // text/xml
        stubFor(post("/things").willReturn(noContent()));

        stubFor(post("/temp-redirect").willReturn(temporaryRedirect("/new/place")));
        stubFor(post("/perm-redirect").willReturn(permanentRedirect("/new/place")));
        stubFor(post("/see-other").willReturn(seeOther("/new/place")));

        stubFor(post("/sorry-no").willReturn(unauthorized()));
        stubFor(post("/still-no").willReturn(forbidden()));

        stubFor(put("/dodgy").willReturn(badRequest()));
        stubFor(put("/dodgy-body").willReturn(badRequestEntity()));
        stubFor(put("/nothing-to-see-here").willReturn(notFound()));

        stubFor(put("/status-only").willReturn(status(418)));

        stubFor(get("/dead-server").willReturn(serviceUnavailable()));
        stubFor(put("/error").willReturn(serverError()));

        stubFor(proxyAllTo("http://my.example.com"));

    }

    @Test
    public void recordingDsl() {
        startRecording(
            recordSpec()
                .forTarget("http://example.mocklab.io")
                .onlyRequestsMatching(getRequestedFor(urlPathMatching("/api/.*")))
                .captureHeader("Accept")
                .captureHeader("Content-Type", true)
                .extractBinaryBodiesOver(10240)
                .extractTextBodiesOver(2048)
                .makeStubsPersistent(false)
                .ignoreRepeatRequests()
                .transformers("modify-response-header")
                .transformerParameters(Parameters.one("headerValue", "123"))
                .matchRequestBodyWithEqualToJson(false, true)
        );

        System.out.println(Json.write(recordSpec()
            .forTarget("http://example.mocklab.io")
            .onlyRequestsMatching(getRequestedFor(urlPathMatching("/api/.*")))
            .captureHeader("Accept")
            .captureHeader("Content-Type", true)
            .extractBinaryBodiesOver(10240)
            .extractTextBodiesOver(2048)
            .makeStubsPersistent(false)
            .ignoreRepeatRequests()
            .transformers("modify-response-header")
            .transformerParameters(Parameters.one("headerValue", "123"))
            .matchRequestBodyWithEqualToJson(false, true)
            .build()));
    }

    @Test
    public void snapshotDsl() {
        snapshotRecord(
            recordSpec()
                .onlyRequestsMatching(getRequestedFor(urlPathMatching("/api/.*")))
                .onlyRequestIds(singletonList(UUID.fromString("40a93c4a-d378-4e07-8321-6158d5dbcb29")))
                .captureHeader("Accept")
                .captureHeader("Content-Type", true)
                .extractBinaryBodiesOver(10240)
                .extractTextBodiesOver(2048)
                .makeStubsPersistent(false)
                .ignoreRepeatRequests()
                .transformers("modify-response-header")
                .transformerParameters(Parameters.one("headerValue", "123"))
                .chooseBodyMatchTypeAutomatically()
        );

        System.out.println(Json.write(recordSpec()
            .onlyRequestsMatching(getRequestedFor(urlPathMatching("/api/.*")))
            .onlyRequestIds(singletonList(UUID.fromString("40a93c4a-d378-4e07-8321-6158d5dbcb29")))
            .captureHeader("Accept")
            .captureHeader("Content-Type", true)
            .extractBinaryBodiesOver(10240)
            .extractTextBodiesOver(2048)
            .makeStubsPersistent(false)
            .ignoreRepeatRequests()
            .transformers("modify-response-header")
            .transformerParameters(Parameters.one("headerValue", "123"))
            .chooseBodyMatchTypeAutomatically()
            .build()));
    }

    @Test
    public void customAndStandardMatcherJson() {
        System.out.println(Json.write(get(urlPathMatching("/the/.*/one"))
                .andMatching("path-contains-param", Parameters.one("path", "correct"))
                .willReturn(ok())
                .build()));
    }

    public static class SimpleAuthRequestFilter extends StubRequestFilter {

        @Override
        public RequestFilterAction filter(Request request) {
            if (request.header("Authorization").firstValue().equals("Basic abc123")) {
                return RequestFilterAction.continueWith(request);
            }

            return RequestFilterAction.stopWith(ResponseDefinition.notAuthorised());
        }

        @Override
        public String getName() {
            return "simple-auth";
        }
    }

    public static class UrlAndHeadersModifyingFilter extends StubRequestFilter {

        @Override
        public RequestFilterAction filter(Request request) {
            Request wrappedRequest = RequestWrapper.create()
                    .transformAbsoluteUrl(new FieldTransformer<String>() {
                        @Override
                        public String transform(String url) {
                            return url + "extraparam=123";
                        }
                    })
                    .addHeader("X-Custom-Header", "headerval")
                    .wrap(request);

            return RequestFilterAction.continueWith(wrappedRequest);
        }

        @Override
        public String getName() {
            return "url-and-header-modifier";
        }
    }
}
