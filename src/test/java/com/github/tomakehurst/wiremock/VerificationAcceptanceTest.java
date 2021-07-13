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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.lessThan;
import static com.github.tomakehurst.wiremock.common.Metadata.metadata;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.forCustomMatcher;
import static com.github.tomakehurst.wiremock.testsupport.TestHttpHeader.withHeader;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.findServeEventWithUrl;
import static com.github.tomakehurst.wiremock.verification.diff.JUnitStyleDiffRenderer.junitStyleDiffMessage;
import static java.lang.System.lineSeparator;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcher;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.RequestJournalDisabledException;
import com.google.common.base.Optional;
import java.util.List;
import java.util.UUID;
import org.apache.http.entity.StringEntity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class VerificationAcceptanceTest {

  public static class JournalEnabled extends AcceptanceTestBase {

    @Test
    public void verifiesRequestBasedOnUrlOnly() {
      testClient.get("/this/got/requested?query");
      verify(getRequestedFor(urlEqualTo("/this/got/requested?query")));
    }

    @Test
    public void anyRequestedForMatchesAnyHttpMethod() {
      testClient.get("/this/got/requested?query");
      verify(anyRequestedFor(urlEqualTo("/this/got/requested?query")));
    }

    @Test(expected = VerificationException.class)
    public void throwsVerificationExceptionOnUrlEqualsWhenQueryMissing() {
      testClient.get("/this/got/requested?query");
      verify(getRequestedFor(urlEqualTo("/this/got/requested")));
    }

    @Test(expected = VerificationException.class)
    public void throwsVerificationExceptionOnUrlEqualsWhenPathShorter() {
      testClient.get("/this/got/requested?query");
      verify(getRequestedFor(urlEqualTo("/this/got/requeste?query")));
    }

    @Test(expected = VerificationException.class)
    public void throwsVerificationExceptionOnUrlEqualsWhenExtraPathPresent() {
      testClient.get("/this/got/requested?query");
      verify(getRequestedFor(urlEqualTo("/this/got/requested/?query")));
    }

    @Test
    public void verifiesRequestBasedOnUrlPathOnly() {
      testClient.get("/this/got/requested?query");
      verify(getRequestedFor(urlPathEqualTo("/this/got/requested")));
    }

    @Test(expected = VerificationException.class)
    public void throwsVerificationExceptionOnUrlPathEqualsWhenPathShorter() {
      testClient.get("/this/got/requested?query");
      verify(getRequestedFor(urlPathEqualTo("/this/got/requeste")));
    }

    @Test(expected = VerificationException.class)
    public void throwsVerificationExceptionOnUrlPathEqualsWhenExtraPathPresent() {
      testClient.get("/this/got/requested?query");
      verify(getRequestedFor(urlPathEqualTo("/this/got/requested/")));
    }

    @Test
    public void verifiesRequestBasedOnUrlPathPatternOnly() {
      testClient.get("/this/got/requested");
      verify(getRequestedFor(urlPathMatching("/(.*?)/got/.*")));
    }

    @Test(expected = VerificationException.class)
    public void throwsVerificationExceptionOnUrlPathPatternWhenOnlyPrefixMatching() {
      testClient.get("/this/got/requested");
      verify(getRequestedFor(urlPathMatching("/(.*?)/got/")));
    }

    @Test(expected = VerificationException.class)
    public void throwsVerificationExceptionOnUrlPathPatternWhenOnlySuffixMatching() {
      testClient.get("/this/got/requested");
      verify(getRequestedFor(urlPathMatching("/got/.*")));
    }

    @Test(expected = VerificationException.class)
    public void throwsVerificationExceptionWhenNoMatch() {
      testClient.get("/this/got/requested");
      verify(getRequestedFor(urlEqualTo("/this/did/not")));
    }

    @Test
    public void verifiesWithHeaders() {
      testClient.put(
          "/update/this",
          withHeader("Content-Type", "application/json"),
          withHeader("Encoding", "UTF-8"));
      verify(
          putRequestedFor(urlMatching("/[a-z]+/this"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("Encoding", notMatching("LATIN-1")));
    }

    @Test
    public void verifiesWithMultiValueHeaders() {
      testClient.get(
          "/multi/value/header",
          withHeader("X-Thing", "One"),
          withHeader("X-Thing", "Two"),
          withHeader("X-Thing", "Three"));

      verify(
          getRequestedFor(urlEqualTo("/multi/value/header"))
              .withHeader("X-Thing", equalTo("Two"))
              .withHeader("X-Thing", matching("Thr.*")));

      verify(
          getRequestedFor(urlEqualTo("/multi/value/header"))
              .withHeader("X-Thing", equalTo("Three")));
    }

    @Test
    public void findsRequestsWithMultiValueHeaders() {
      testClient.get(
          "/multi/value/header",
          withHeader("X-Thing", "One"),
          withHeader("X-Thing", "Two"),
          withHeader("X-Thing", "Three"));

      List<LoggedRequest> requests = findAll(getRequestedFor(urlEqualTo("/multi/value/header")));

      HttpHeaders headers = requests.get(0).getHeaders();
      assertThat(headers.getHeader("X-Thing").values().size(), is(3));
      assertThat(headers.getHeader("X-Thing").values().get(1), is("Two"));
    }

    @Test(expected = VerificationException.class)
    public void throwsVerificationExceptionWhenHeadersDoNotMatch() {
      testClient.put(
          "/to/modify",
          withHeader("Content-Type", "application/json"),
          withHeader("Encoding", "LATIN-1"));
      verify(
          putRequestedFor(urlEqualTo("/to/modify"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("Encoding", notMatching("LATIN-1")));
    }

    private static final String SAMPLE_JSON =
        "{ 													\n"
            + "	\"thing\": {									\n"
            + "		\"importantKey\": \"Important value\"		\n"
            + "	}												\n"
            + "}													";

    @Test
    public void verifiesWithBody() {
      testClient.postWithBody("/add/this", SAMPLE_JSON, "application/json", "utf-8");
      verify(
          postRequestedFor(urlEqualTo("/add/this"))
              .withRequestBody(matching(".*\"importantKey\": \"Important value\".*")));
    }

    @Test
    public void verifiesWithBodyContainingJson() {
      testClient.postWithBody("/body/contains", SAMPLE_JSON, "application/json", "utf-8");
      verify(
          postRequestedFor(urlEqualTo("/body/contains"))
              .withRequestBody(matchingJsonPath("$.thing"))
              .withRequestBody(
                  matchingJsonPath("$..thing[?(@.importantKey == 'Important value')]")));
    }

    @Test
    public void verifiesWithBodyEquallingJson() {
      testClient.postWithBody("/body/json", SAMPLE_JSON, "application/json", "utf-8");
      verify(postRequestedFor(urlEqualTo("/body/json")).withRequestBody(equalToJson(SAMPLE_JSON)));
    }

    @Test
    public void verifiesWithBodyEquallingJsonWithCompareMode() {
      testClient.postWithBody(
          "/body/json/lenient",
          "{ \"message\": \"Hello\", \"key\": \"welcome.message\" }",
          "application/json",
          "utf-8");
      verify(
          postRequestedFor(urlEqualTo("/body/json/lenient"))
              .withRequestBody(equalToJson("{ \"message\": \"Hello\" }", true, true)));
    }

    @Test
    public void verifiesWithBodyEquallingXml() {
      testClient.postWithBody(
          "/body/xml", "<thing><subThing>The stuff</subThing></thing>", "application/xml", "utf-8");
      verify(
          postRequestedFor(urlEqualTo("/body/xml"))
              .withRequestBody(
                  equalToXml("<thing>     <subThing>The stuff\n</subThing>\n\n    </thing>")));
    }

    @Test
    public void verifiesWithBodyEquallingXpath() {
      testClient.postWithBody(
          "/body/xml", "<thing><subThing>The stuff</subThing></thing>", "application/xml", "utf-8");
      verify(
          postRequestedFor(urlEqualTo("/body/xml"))
              .withRequestBody(matchingXPath("//subThing[.='The stuff']")));
    }

    @Test
    public void verifiesWithBodyEquallingNamespacedXpath() {
      testClient.postWithBody(
          "/namespaced/xml",
          "<t:thing xmlns:t='http://things' xmlns:s='http://subthings'><s:subThing>The stuff</s:subThing></t:thing>",
          "application/xml",
          "utf-8");

      verify(
          postRequestedFor(urlEqualTo("/namespaced/xml"))
              .withRequestBody(
                  matchingXPath("//s:subThing[.='The stuff']")
                      .withXPathNamespace("t", "http://things")
                      .withXPathNamespace("s", "http://subthings")));
    }

    @Test
    public void verifiesWithBodyContainingString() {
      testClient.postWithBody("/body/json", SAMPLE_JSON, "application/json", "utf-8");
      verify(
          postRequestedFor(urlEqualTo("/body/json"))
              .withRequestBody(containing("Important value")));
    }

    @Test
    public void verifiesWithQueryParam() {
      testClient.get("/query?param=my-value");
      verify(
          getRequestedFor(urlPathEqualTo("/query")).withQueryParam("param", equalTo("my-value")));
    }

    @Test
    public void queryParameterMatchingCopesWithSpaces() {
      testClient.get("/spacey-query?param=My%20Value");
      verify(
          getRequestedFor(urlPathEqualTo("/spacey-query"))
              .withQueryParam("param", equalTo("My Value")));
    }

    @Test(expected = VerificationException.class)
    public void verifyIsFalseWithQueryParamNotMatched() {
      testClient.get("/query?param=my-value");
      verify(
          getRequestedFor(urlPathEqualTo("/query"))
              .withQueryParam("param", equalTo("wrong-value")));
    }

    @Test(expected = VerificationException.class)
    public void verifyIsFalseWhenExpectedQueryParamMissing() {
      testClient.get("/query");
      verify(
          getRequestedFor(urlPathEqualTo("/query")).withQueryParam("param", equalTo("my-value")));
    }

    @Test(expected = VerificationException.class)
    public void resetErasesCounters() {
      testClient.get("/count/this");
      testClient.get("/count/this");
      testClient.get("/count/this");

      WireMock.reset();

      verify(getRequestedFor(urlEqualTo("/count/this")));
    }

    @Test
    public void verifiesArbitraryRequestCount() {
      testClient.get("/add/to/count");
      testClient.get("/add/to/count");
      testClient.get("/add/to/count");
      testClient.get("/add/to/count");

      verify(4, getRequestedFor(urlEqualTo("/add/to/count")));
    }

    private void getCountableRequests(int count) {
      for (int i = 0; i < count; i++) {
        testClient.get("/add/to/count");
      }
    }

    @Test
    public void verifiesLessThanCountWithLessRequests() {
      getCountableRequests(4);
      verify(lessThan(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test(expected = VerificationException.class)
    public void doesNotVerifyLessThanCountWithEqualRequests() {
      getCountableRequests(5);
      verify(lessThan(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test(expected = VerificationException.class)
    public void doesNotVerifyLessThanCountWithMoreRequests() {
      getCountableRequests(6);
      verify(lessThan(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test
    public void verifiesLessThanOrExactlyCountWithLessRequests() {
      getCountableRequests(4);
      verify(lessThanOrExactly(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test
    public void verifiesLessThanOrExactlyCountWithEqualRequests() {
      getCountableRequests(5);
      verify(lessThanOrExactly(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test(expected = VerificationException.class)
    public void doesNotVerifyLessThanOrExactlyCountWithMoreRequests() {
      getCountableRequests(6);
      verify(lessThanOrExactly(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test(expected = VerificationException.class)
    public void doesNotVerifyExactCountWithLessRequests() {
      getCountableRequests(4);
      verify(exactly(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test
    public void verifiesExactlyThanCountWithExactRequests() {
      getCountableRequests(5);
      verify(exactly(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test(expected = VerificationException.class)
    public void doesNotVerifyExactCountWithMoreRequests() {
      getCountableRequests(6);
      verify(exactly(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test(expected = VerificationException.class)
    public void doesNotVerifyMoreThanOrExactlyCountWithLessRequests() {
      getCountableRequests(4);
      verify(moreThanOrExactly(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test
    public void verifiesMoreThanOrExactlyCountWithEqualRequests() {
      getCountableRequests(5);
      verify(moreThanOrExactly(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test
    public void verifiesMoreThanOrExactlyCountWithMoreRequests() {
      getCountableRequests(6);
      verify(moreThanOrExactly(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test(expected = VerificationException.class)
    public void doesNotVerifyMoreThanCountWithLessRequests() {
      getCountableRequests(4);
      verify(moreThan(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test(expected = VerificationException.class)
    public void doesNotVerifyMoreThanCountWithEqualRequests() {
      getCountableRequests(5);
      verify(moreThan(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test
    public void verifiesMoreThanCountWithMoreRequests() {
      getCountableRequests(6);
      verify(moreThan(5), getRequestedFor(urlEqualTo("/add/to/count")));
    }

    @Test
    public void verifiesHeaderAbsent() {
      testClient.get("/without/header", withHeader("Content-Type", "application/json"));
      verify(
          getRequestedFor(urlEqualTo("/without/header"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withoutHeader("Accept"));
    }

    @Test(expected = VerificationException.class)
    public void failsVerificationWhenAbsentHeaderPresent() {
      testClient.get("/without/another/header", withHeader("Content-Type", "application/json"));
      verify(getRequestedFor(urlEqualTo("/without/another/header")).withoutHeader("Content-Type"));
    }

    @Test
    public void verifiesBodyAbsent() throws Exception {
      testClient.post("/no/body", new StringEntity(""));
      verify(postRequestedFor(urlEqualTo("/no/body")).withRequestBody(absent()));
    }

    @Test(expected = VerificationException.class)
    public void failsVerificationWhenAbsentBodyPresent() throws Exception {
      testClient.post("/no/body", new StringEntity("not absent"));
      verify(postRequestedFor(urlEqualTo("/no/body")).withRequestBody(absent()));
    }

    @Test
    public void showsDiffWithNearestMissWhenNoRequestsMatchedAndNearMissesAreAvailable() {
      testClient.get("/my-near-miss");
      testClient.get("/near-miss");

      try {
        verify(getRequestedFor(urlEqualTo("/a-near-miss")));
        fail();
      } catch (VerificationException e) {
        assertThat(
            e.getMessage(),
            containsString(
                junitStyleDiffMessage("GET\n" + "/a-near-miss\n", "GET\n" + "/my-near-miss\n")));
      }
    }

    @Test
    public void showsExpectedRequestAndCountShortfallWhenNotEnoughMatchingRequestsAreReceived() {
      testClient.get("/hit");
      testClient.get("/hit");

      try {
        verify(3, getRequestedFor(urlEqualTo("/hit")));
        fail();
      } catch (VerificationException e) {
        assertThat(
            e.getMessage(),
            is(
                "Expected exactly 3 requests matching the following pattern but received 2:\n"
                    + "{"
                    + lineSeparator()
                    + "  \"url\" : \"/hit\","
                    + lineSeparator()
                    + "  \"method\" : \"GET\""
                    + lineSeparator()
                    + "}"));
      }
    }

    @Test
    public void showsNearMissDiffWhenCountSpecifiedAndNoMatchingRequestsAreReceived() {
      testClient.get("/miss");
      testClient.get("/miss");

      try {
        verify(3, getRequestedFor(urlEqualTo("/hit")));
        fail();
      } catch (VerificationException e) {
        assertThat(
            e.getMessage(), containsString(junitStyleDiffMessage("GET\n/hit\n", "GET\n/miss\n")));
      }
    }

    @Test
    public void
        showsExpectedRequestAndCountShortfallWhenWrongNumberOfMatchingRequestsAreReceived() {
      testClient.get("/hit");
      testClient.get("/hit");
      testClient.get("/hit");
      testClient.get("/hit");

      try {
        verify(lessThan(2), getRequestedFor(urlEqualTo("/hit")));
        fail();
      } catch (VerificationException e) {
        assertThat(
            e.getMessage(),
            is(
                "Expected less than 2 requests matching the following pattern but received 4:\n"
                    + "{"
                    + lineSeparator()
                    + "  \"url\" : \"/hit\","
                    + lineSeparator()
                    + "  \"method\" : \"GET\""
                    + lineSeparator()
                    + "}"));
      }
    }

    @Test
    public void showsNearMissDiffWhenCountMatchSpecifiedAndNoMatchingRequestsAreReceived() {
      testClient.get("/miss");
      testClient.get("/miss");

      try {
        verify(moreThanOrExactly(4), getRequestedFor(urlEqualTo("/hit")));
        fail();
      } catch (VerificationException e) {
        assertThat(
            e.getMessage(),
            containsString(junitStyleDiffMessage("GET\n" + "/hit\n", "GET\n" + "/miss\n")));
      }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void showsExpectedAndReceivedRequestsOnVerificationExceptionForLessThan() {
      testClient.get("/some/request");
      testClient.get("/some/request");
      testClient.get("/some/request");

      try {
        verify(lessThan(2), getRequestedFor(urlEqualTo("/some/request")));
        fail();
      } catch (VerificationException e) {
        assertThat(
            e.getMessage(),
            allOf(
                containsString("Expected less than 2 requests matching"),
                containsString("/some/request")));
      }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void showsExpectedAndReceivedRequestsOnVerificationExceptionForLessThanOrExactly() {
      testClient.get("/some/request");
      testClient.get("/some/request");
      testClient.get("/some/request");

      try {
        verify(lessThanOrExactly(2), getRequestedFor(urlEqualTo("/some/request")));
        fail();
      } catch (VerificationException e) {
        assertThat(
            e.getMessage(),
            allOf(
                containsString("Expected less than or exactly 2 requests matching"),
                containsString("/some/request")));
      }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void showsExpectedAndReceivedRequestsOnVerificationExceptionForExactly() {
      testClient.get("/some/request");

      try {
        verify(exactly(12), getRequestedFor(urlEqualTo("/some/request")));
        fail();
      } catch (VerificationException e) {
        assertThat(
            e.getMessage(),
            allOf(
                containsString("Expected exactly 12 requests matching"),
                containsString("/some/request")));
      }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void showsExpectedAndReceivedRequestsOnVerificationExceptionForMoreThan() {
      testClient.get("/some/request");

      try {
        verify(moreThan(12), getRequestedFor(urlEqualTo("/some/request")));
        fail();
      } catch (VerificationException e) {
        assertThat(
            e.getMessage(),
            allOf(
                containsString("Expected more than 12 requests matching"),
                containsString("/some/request")));
      }
    }

    @Test
    public void verifiesPatchRequests() {
      testClient.patchWithBody("/patch/this", SAMPLE_JSON, "application/json");
      verify(
          patchRequestedFor(urlEqualTo("/patch/this"))
              .withRequestBody(matching(".*\"importantKey\": \"Important value\".*")));
    }

    @Test
    public void verifiesRequestsWithCountMatchingStrategy() {
      testClient.get("/custom-match-this");
      testClient.get("/custom-match-this");

      wireMockServer.verify(exactly(2), getRequestedFor(urlEqualTo("/custom-match-this")));
    }

    @Test
    public void verifiesRequestsViaCustomMatcher() {
      testClient.get("/custom-match-this");
      testClient.get("/custom-match-that");

      wireMockServer.verify(
          2,
          requestMadeFor(
              new RequestMatcher() {
                @Override
                public MatchResult match(Request request) {
                  return MatchResult.of(request.getUrl().contains("custom-match"));
                }

                @Override
                public String getName() {
                  return "inline";
                }
              }));
    }

    @Test
    public void verifiesRequestsViaCustomMatcherRemotely() {
      testClient.get("/remote-custom-match-this");
      testClient.get("/remote-custom-match-that");

      verify(
          2,
          requestMadeFor(
              new ValueMatcher<Request>() {
                @Override
                public MatchResult match(Request value) {
                  return MatchResult.of(value.getUrl().contains("remote-custom-match"));
                }
              }));
    }

    @Test
    public void copesWithAttemptedXmlBodyMatchWhenRequestHasNoXmlBody() {
      testClient.post("/missing-xml", new StringEntity("", TEXT_PLAIN));

      try {
        verify(
            postRequestedFor(urlEqualTo("/missing-xml"))
                .withRequestBody(equalToXml("<my-stuff />")));
        fail();
      } catch (VerificationException e) {
        assertThat(e.getMessage(), containsString("No requests exactly matched."));
      }
    }

    @Test
    public void verifiesWithCustomMatcherViaStaticDsl() {
      testClient.get("/custom-verify");

      verify(
          forCustomMatcher(
              new RequestMatcherExtension() {
                @Override
                public MatchResult match(Request request, Parameters parameters) {
                  return MatchResult.of(request.getUrl().equals("/custom-verify"));
                }
              }));
    }

    @Test
    public void removesEventsById() {
      stubFor(get(anyUrl()).willReturn(ok()));

      testClient.get("/one", withHeader("My-Header", "one"));
      testClient.get("/one", withHeader("My-Header", "two"));
      testClient.get("/two");

      List<ServeEvent> initialServeEvents = getAllServeEvents();
      assertThat(initialServeEvents.size(), is(3));

      removeServeEvent(initialServeEvents.get(0).getId());
      removeServeEvent(initialServeEvents.get(2).getId());

      List<ServeEvent> finalServeEvents = getAllServeEvents();

      assertThat(finalServeEvents.size(), is(1));
      assertThat(finalServeEvents.get(0).getRequest().header("My-Header").firstValue(), is("two"));
    }

    @Test
    public void doesNothingWhenAttemptingToRemoveANonExistentServeEvent() {
      stubFor(get(anyUrl()).willReturn(ok()));

      testClient.get("/one", withHeader("My-Header", "one"));
      testClient.get("/one", withHeader("My-Header", "two"));
      testClient.get("/two");

      List<ServeEvent> initialServeEvents = getAllServeEvents();
      assertThat(initialServeEvents.size(), is(3));

      removeServeEvent(UUID.randomUUID());

      List<ServeEvent> finalServeEvents = getAllServeEvents();

      assertThat(finalServeEvents.size(), is(3));
    }

    @Test
    public void removesEventsPerSuppliedFilter() {
      stubFor(get(anyUrl()).willReturn(ok()));

      testClient.get("/one", withHeader("My-Header", "one"));
      testClient.get("/one", withHeader("My-Header", "two"));
      testClient.get("/two");

      List<ServeEvent> removedEvents =
          removeServeEvents(
              getRequestedFor(urlPathEqualTo("/one")).withHeader("My-Header", equalTo("two")));

      assertThat(removedEvents.size(), is(1));
      assertThat(removedEvents.get(0).getRequest().header("My-Header").firstValue(), is("two"));

      List<ServeEvent> serveEvents = getAllServeEvents();
      assertThat(serveEvents.size(), is(2));

      ServeEvent event1 = findServeEventWithUrl(serveEvents, "/one");
      assertThat(event1.getRequest().header("My-Header").firstValue(), is("one"));

      ServeEvent event2 = findServeEventWithUrl(serveEvents, "/two");
      assertThat(event2, notNullValue());
    }

    @Test
    public void returnsEmptyListWhenNoEventsMatchedForRemoval() {
      stubFor(get(anyUrl()).willReturn(ok()));

      testClient.get("/one", withHeader("My-Header", "one"));
      testClient.get("/one", withHeader("My-Header", "two"));
      testClient.get("/two");

      List<ServeEvent> removedEvents =
          removeServeEvents(
              getRequestedFor(urlPathEqualTo("/one")).withHeader("My-Header", equalTo("wrong")));

      assertThat(removedEvents.size(), is(0));

      List<ServeEvent> serveEvents = getAllServeEvents();
      assertThat(serveEvents.size(), is(3));
    }

    @Test
    public void removesEventsAssociatedWithStubsMatchingMetadata() {
      stubFor(get("/with-metadata").withMetadata(metadata().list("tags", "delete-me")));
      stubFor(get("/without-metadata"));

      testClient.get("/with-metadata");
      testClient.get("/without-metadata");

      List<ServeEvent> removedServeEvents =
          removeEventsByStubMetadata(matchingJsonPath("$.tags[0]", equalTo("delete-me")));

      assertThat(removedServeEvents.size(), is(1));
      assertThat(removedServeEvents.get(0).getRequest().getUrl(), is("/with-metadata"));

      List<ServeEvent> serveEvents = getAllServeEvents();
      assertThat(serveEvents.size(), is(1));
      assertThat(serveEvents.get(0).getRequest().getUrl(), is("/without-metadata"));
    }
  }

  public static class JournalDisabled {

    @Rule
    public WireMockRule wireMockRule =
        new WireMockRule(wireMockConfig().dynamicPort().disableRequestJournal(), false);

    @Test(expected = RequestJournalDisabledException.class)
    public void verifyThrowsExceptionWhenVerificationAttemptedAndRequestJournalDisabled() {
      verify(getRequestedFor(urlEqualTo("/whatever")));
    }

    @Test(expected = RequestJournalDisabledException.class)
    public void findAllThrowsExceptionWhenVerificationAttemptedAndRequestJournalDisabled() {
      findAll(getRequestedFor(urlEqualTo("/whatever")));
    }
  }

  public static class JournalMaxEntriesRestricted {
    @Rule
    public WireMockRule wireMockRule =
        new WireMockRule(options().dynamicPort().maxRequestJournalEntries(Optional.of(2)), false);

    @Test
    public void maxLengthIs2() {
      WireMockTestClient testClient = new WireMockTestClient(wireMockRule.port());
      testClient.get("/request1");
      testClient.get("/request2");
      testClient.get("/request3");
      verify(0, getRequestedFor(urlEqualTo("/request1")));
      verify(1, getRequestedFor(urlEqualTo("/request2")));
      verify(1, getRequestedFor(urlEqualTo("/request3")));
    }
  }
}
