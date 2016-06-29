package com.github.tomakehurst.wiremock.matching;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.github.tomakehurst.wiremock.http.RequestMethod.PUT;
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RequestPatternTest {

    @Test
    public void matchesExactlyWith0DistanceWhenUrlAndMethodAreExactMatch() {
        RequestPattern requestPattern =
            newRequestPattern(PUT, WireMock.urlPathEqualTo("/my/url"))
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest().method(PUT).url("/my/url"));
        assertThat(matchResult.getDistance(), is(0.0));
        assertTrue(matchResult.isExactMatch());
    }

    @Test
    public void returnsNon0DistanceWhenUrlDoesNotMatch() {
        RequestPattern requestPattern =
            newRequestPattern(PUT, WireMock.urlPathEqualTo("/my/url"))
            .withUrl("/my/url")
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest().url("/totally/other/url"));
        assertThat(matchResult.getDistance(), greaterThan(0.0));
        assertFalse(matchResult.isExactMatch());
    }

    @Test
    public void matchesExactlyWith0DistanceWhenAllRequiredHeadersMatch() {
        RequestPattern requestPattern =
            newRequestPattern(PUT, WireMock.urlPathEqualTo("/my/url"))
            .withHeader("My-Header", equalTo("my-expected-header-val"))
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
            .method(PUT)
            .header("My-Header", "my-expected-header-val")
            .url("/my/url"));
        assertThat(matchResult.getDistance(), is(0.0));
        assertTrue(matchResult.isExactMatch());
    }

    @Test
    public void doesNotMatchWhenHeaderDoesNotMatch() {
        RequestPattern requestPattern =
            newRequestPattern(GET, WireMock.urlPathEqualTo("/my/url"))
            .withHeader("My-Header", equalTo("my-expected-header-val"))
            .withHeader("My-Other-Header", equalTo("my-other-expected-header-val"))
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
            .method(GET)
            .header("My-Header", "my-expected-header-val")
            .header("My-Other-Header", "wrong")
            .url("/my/url"));

        assertFalse(matchResult.isExactMatch());
    }

    @Test
    public void matchesExactlyWhenRequiredAbsentHeaderIsAbsent() {
        RequestPattern requestPattern =
            newRequestPattern(GET, WireMock.urlPathEqualTo("/my/url"))
            .withHeader("My-Header", absent())
            .withHeader("My-Other-Header", equalTo("my-other-expected-header-val"))
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
            .method(GET)
            .header("My-Other-Header", "my-other-expected-header-val")
            .url("/my/url"));

        assertTrue(matchResult.isExactMatch());
    }

    @Test
    public void doesNotMatchWhenRequiredAbsentHeaderIsPresent() {
        RequestPattern requestPattern =
            newRequestPattern(GET, WireMock.urlPathEqualTo("/my/url"))
            .withHeader("My-Header", absent())
            .withHeader("My-Other-Header", equalTo("my-other-expected-header-val"))
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
            .method(GET)
            .header("My-Header", "my-expected-header-val")
            .header("My-Other-Header", "wrong")
            .url("/my/url"));

        assertFalse(matchResult.isExactMatch());
    }

    @Test
    public void bindsToJsonCompatibleWithOriginalRequestPatternForUrl() throws Exception {
        RequestPattern requestPattern =
            newRequestPattern(GET, WireMock.urlEqualTo("/my/url"))
            .build();

        String actualJson = Json.write(requestPattern);

        JSONAssert.assertEquals(
            "{									                \n" +
                "		\"method\": \"GET\",						\n" +
                "		\"url\": \"/my/url\"                		\n" +
                "}												    ",
            actualJson,
            true);
    }

    @Test
    public void bindsToJsonCompatibleWithOriginalRequestPatternForUrlPattern() throws Exception {
        RequestPattern requestPattern =
            newRequestPattern(GET, WireMock.urlMatching("/my/url"))
            .build();

        String actualJson = Json.write(requestPattern);

        JSONAssert.assertEquals(
            "{									                \n" +
            "		\"method\": \"GET\",						\n" +
            "		\"urlPattern\": \"/my/url\"           		\n" +
            "}												    ",
            actualJson,
            true);
    }

    @Test
    public void bindsToJsonCompatibleWithOriginalRequestPatternForUrlPathPattern() throws Exception {
        RequestPattern requestPattern =
            newRequestPattern(GET, WireMock.urlPathMatching("/my/url"))
            .build();

        String actualJson = Json.write(requestPattern);

        JSONAssert.assertEquals(
            "{									                \n" +
            "		\"method\": \"GET\",						\n" +
            "		\"urlPathPattern\": \"/my/url\"             \n" +
            "}												    ",
            actualJson,
            true);
    }

    @Test
    public void bindsToJsonCompatibleWithOriginalRequestPatternForUrlPathAndHeaders() throws Exception {
        RequestPattern requestPattern =
            newRequestPattern(GET, WireMock.urlPathEqualTo("/my/url"))
            .withHeader("Accept", matching("(.*)xml(.*)"))
            .withHeader("If-None-Match", matching("([a-z0-9]*)"))
            .build();

        String actualJson = Json.write(requestPattern);

        JSONAssert.assertEquals(
            URL_PATH_AND_HEADERS_EXAMPLE,
            actualJson,
            true);
    }

    static final String URL_PATH_AND_HEADERS_EXAMPLE =
        "{									                \n" +
        "		\"method\": \"GET\",						\n" +
        "		\"urlPath\": \"/my/url\",             		\n" +
        "		\"headers\": {								\n" +
        "			\"Accept\": {							\n" +
        "				\"matches\": \"(.*)xml(.*)\"		\n" +
        "			},										\n" +
        "			\"If-None-Match\": {					\n" +
        "				\"matches\": \"([a-z0-9]*)\"		\n" +
        "			}										\n" +
        "		}											\n" +
        "}												    ";

    @Test
    public void matchesExactlyWith0DistanceWhenAllRequiredQueryParametersMatch() {
        RequestPattern requestPattern =
            newRequestPattern(PUT, WireMock.urlPathEqualTo("/my/url"))
            .withQueryParam("param1", equalTo("1"))
            .withQueryParam("param2", equalTo("2"))
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
            .method(PUT)
            .url("/my/url?param1=1&param1=555&param2=2"));
        assertThat(matchResult.getDistance(), is(0.0));
        assertTrue(matchResult.isExactMatch());
    }

    @Test
    public void returnsNon0DistanceWhenRequiredQueryParameterMatchDoesNotMatch() {
        RequestPattern requestPattern =
            newRequestPattern(PUT, WireMock.urlPathEqualTo("/my/url"))
            .withQueryParam("param1", equalTo("1"))
            .withQueryParam("param2", equalTo("2"))
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
            .method(PUT)
            .url("/my/url?param1=555&param2=2"));
        assertThat(matchResult.getDistance(), greaterThan(0.0));
        assertFalse(matchResult.isExactMatch());
    }

    @Test
    public void bindsToJsonCompatibleWithOriginalRequestPatternWithQueryParams() throws Exception {
        RequestPattern requestPattern =
            newRequestPattern(GET, WireMock.urlPathEqualTo("/my/url"))
            .withQueryParam("param1", equalTo("1"))
            .withQueryParam("param2", matching("2"))
            .build();

        String actualJson = Json.write(requestPattern);

        JSONAssert.assertEquals(
            "{                              \n" +
            "    \"method\": \"GET\",       \n" +
            "    \"urlPath\": \"/my/url\",  \n" +
            "    \"queryParameters\": {     \n" +
            "        \"param1\": {          \n" +
            "            \"equalTo\": \"1\" \n" +
            "        },                     \n" +
            "        \"param2\": {          \n" +
            "            \"matches\": \"2\" \n" +
            "        }                      \n" +
            "    }                          \n" +
            "}",
            actualJson,
            true);
    }

    @Test
    public void matchesExactlyWith0DistanceWhenBodyPatternsAllMatch() {
        RequestPattern requestPattern =
            newRequestPattern(PUT, WireMock.urlPathEqualTo("/my/url"))
            .withRequestBody(WireMock.equalTo("exactwordone approxwordtwo blah blah"))
            .withRequestBody(WireMock.containing("two"))
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
            .method(PUT)
            .url("/my/url")
            .body("exactwordone approxwordtwo blah blah"));
        assertThat(matchResult.getDistance(), is(0.0));
        assertTrue(matchResult.isExactMatch());
    }

    @Test
    public void doesNotMatchExactlyWhenOneBodyPatternDoesNotMatch() {
        RequestPattern requestPattern =
            newRequestPattern(PUT, WireMock.urlPathEqualTo("/my/url"))
            .withRequestBody(WireMock.equalTo("exactwordone approxwordtwo blah blah"))
            .withRequestBody(WireMock.containing("three"))
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
            .method(PUT)
            .url("/my/url")
            .body("exactwordone approxwordtwo blah blah"));

        assertFalse(matchResult.isExactMatch());
    }

    @Test
    public void doesNotMatchExactlyWhenThereIsNoBody() {
        RequestPattern requestPattern =
                newRequestPattern(PUT, WireMock.urlPathEqualTo("/my/url"))
                        .withRequestBody(WireMock.equalToXml("<xml></xml>"))
                        .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
                .method(PUT)
                .url("/my/url")
                .body(""));

        assertFalse(matchResult.isExactMatch());
    }

    @Test
    public void matchesExactlyWhenAllCookiesMatch() {
        RequestPattern requestPattern =
            newRequestPattern(POST, WireMock.urlPathEqualTo("/my/url"))
            .withCookie("my_cookie", WireMock.equalTo("my-cookie-value"))
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
            .method(POST)
            .cookie("my_cookie", "my-cookie-value")
            .url("/my/url"));

        assertThat(matchResult.getDistance(), is(0.0));
        assertTrue(matchResult.isExactMatch());
    }

    @Test
    public void doesNotMatchWhenARequiredCookieIsMissing() {
        RequestPattern requestPattern =
            newRequestPattern(POST, WireMock.urlPathEqualTo("/my/url"))
            .withCookie("my_cookie", WireMock.equalTo("my-cookie-value"))
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
            .method(POST)
            .url("/my/url"));

        assertFalse(matchResult.isExactMatch());
    }

    @Test
    public void doesNotMatchWhenRequiredCookieValueIsWrong() {
        RequestPattern requestPattern =
            newRequestPattern(POST, WireMock.urlPathEqualTo("/my/url"))
            .withCookie("my_cookie", WireMock.equalTo("my-cookie-value"))
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
            .method(POST)
            .cookie("my_cookie", "wrong-value")
            .url("/my/url"));

        assertFalse(matchResult.isExactMatch());
    }

    @Test
    public void doesNotMatchWhenRequiredAbsentCookieIsPresent() {
        RequestPattern requestPattern =
            newRequestPattern(POST, WireMock.urlPathEqualTo("/my/url"))
            .withCookie("my_cookie", absent())
            .build();

        MatchResult matchResult = requestPattern.match(mockRequest()
            .method(POST)
            .cookie("my_cookie", "any-value")
            .url("/my/url"));

        assertFalse(matchResult.isExactMatch());
    }

    static final String ALL_BODY_PATTERNS_EXAMPLE =
        "{                                                      \n" +
        "    \"url\" : \"/all/body/patterns\",                  \n" +
        "    \"method\" : \"PUT\",                              \n" +
        "    \"bodyPatterns\" : [                               \n" +
        "        { \"equalTo\": \"thing\" },                    \n" +
        "        { \"equalToJson\": \"{ \\\"thing\\\": 1 }\" }, \n" +
        "        { \"matchesJsonPath\": \"@.*\" },              \n" +
        "        { \"equalToXml\": \"<thing />\" },             \n" +
        "        { \"matchesXPath\": \"//thing\" },             \n" +
        "        { \"contains\": \"thin\" },                  \n" +
        "        { \"matches\": \".*thing.*\" },                \n" +
        "        { \"doesNotMatch\": \"^stuff.+\" }             \n" +
        "    ]                                                  \n" +
        "}";

    @SuppressWarnings("unchecked")
    @Test
    public void correctlyDeserialisesBodyPatterns() {
        RequestPattern pattern = Json.read(ALL_BODY_PATTERNS_EXAMPLE, RequestPattern.class);
        assertThat(pattern.getBodyPatterns(), hasItems(
            valuePattern(EqualToPattern.class, "thing"),
            valuePattern(EqualToJsonPattern.class, "{ \"thing\": 1 }"),
            valuePattern(MatchesJsonPathPattern.class, "@.*"),
            valuePattern(EqualToXmlPattern.class, "<thing />"),
            valuePattern(MatchesXPathPattern.class, "//thing"),
            valuePattern(ContainsPattern.class, "thin"),
            valuePattern(RegexPattern.class, ".*thing.*"),
            valuePattern(NegativeRegexPattern.class, "^stuff.+")
        ));
    }

    @Test
    public void correctlySerialisesBodyPatterns() throws Exception {
        RequestPattern requestPattern = newRequestPattern(RequestMethod.PUT, WireMock.urlEqualTo("/all/body/patterns"))
            .withRequestBody(WireMock.equalTo("thing"))
            .withRequestBody(WireMock.equalToJson("{ \"thing\": 1 }"))
            .withRequestBody(WireMock.matchingJsonPath("@.*"))
            .withRequestBody(WireMock.equalToXml("<thing />"))
            .withRequestBody(WireMock.matchingXPath("//thing"))
            .withRequestBody(WireMock.containing("thin"))
            .withRequestBody(WireMock.matching(".*thing.*"))
            .withRequestBody(WireMock.notMatching("^stuff.+"))
            .build();

        String json = Json.write(requestPattern);
        System.out.println(json);
        JSONAssert.assertEquals(ALL_BODY_PATTERNS_EXAMPLE, json, true);
    }

    static Matcher<StringValuePattern> valuePattern(final Class<? extends StringValuePattern> patternClass, final String expectedValue) {
        return new TypeSafeDiagnosingMatcher<StringValuePattern>() {
            @Override
            protected boolean matchesSafely(StringValuePattern item, Description mismatchDescription) {
                return item.getClass().equals(patternClass) && item.getValue().equals(expectedValue);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a value pattern of type " + patternClass.getSimpleName() + " with expected value " + expectedValue);
            }
        };
    }
}


