package com.github.tomakehurst.wiremock.common;

import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.matching.MockMultipart;
import com.github.tomakehurst.wiremock.testsupport.MockRequestBuilder;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.core.Options.FileIdMethod.*;
import static com.github.tomakehurst.wiremock.http.HttpHeader.httpHeader;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.github.tomakehurst.wiremock.http.Response.response;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/***
 * We'll test with a series of requests that are different in some small regard; each request
 * should generate a new hash, different than any hash seen previously.  (requestTest)
 *
 * We'll test a series of different responses in the same manner.  (responseTest)
 *
 * We'll get hashes multiple times, against a new (but created in the same way) list of requests and responses
 * and verify that the hashes produced are consistent.  (consistentHashesTest)
 *
 * In REQUEST_HASH mode, with a constant request, the hash produced is the same as long as the request
 * doesn't change (responseTest), in RESPONSE_HASH mode the hash produced is the same as long as the response
 * doesn't change (requestTest), and in REQUEST_RESPONSE_HASH the hash changes when either changes (requestResponseTest).
 */
public class HashIdGeneratorTest {

    private Mockery context;
    private Request requestForResponseTest;

    @Before
    public void init() {
        context = new Mockery();
        requestForResponseTest = new MockRequestBuilder(context, "Request for response test")
                .withMethod(GET)
                .withUrl("/test/response").build();
    }

    List<Function<MockRequestBuilder, ImmutablePair<String,String>>> requestBuilders = List.of(
            (builder) -> {
                builder
                        .withMethod(GET)
                        .withUrl("/test/api");
                return ImmutablePair.of("3F49E892","{\r\n  \"request\" : {\r\n    \"body\" : \"\",\r\n    \"bodyHash\" : 1,\r\n    \"cookies\" : [ ],\r\n    \"headers\" : { },\r\n    \"method\" : \"GET\",\r\n    \"multiparts\" : [ ],\r\n    \"url\" : \"/test/api\"\r\n  }\r\n}");
            },
            (builder) -> {
                builder
                        .withMethod(POST);
                return ImmutablePair.of("06217964","{\r\n  \"request\" : {\r\n    \"body\" : \"\",\r\n    \"bodyHash\" : 1,\r\n    \"cookies\" : [ ],\r\n    \"headers\" : { },\r\n    \"method\" : \"POST\",\r\n    \"multiparts\" : [ ],\r\n    \"url\" : \"/test/api\"\r\n  }\r\n}");
            },
            (builder) -> {
                builder
                        .withUrl("/test/api2");
                return ImmutablePair.of("D330ACAE","{\r\n  \"request\" : {\r\n    \"body\" : \"\",\r\n    \"bodyHash\" : 1,\r\n    \"cookies\" : [ ],\r\n    \"headers\" : { },\r\n    \"method\" : \"POST\",\r\n    \"multiparts\" : [ ],\r\n    \"url\" : \"/test/api2\"\r\n  }\r\n}");
            },
            (builder) -> {
                builder
                        .withBody("k1=v1&k2=v2");
                return ImmutablePair.of("5D3F01AF","{\r\n  \"request\" : {\r\n    \"body\" : \"k1=v1&k2=v2\",\r\n    \"bodyHash\" : -1571478275,\r\n    \"cookies\" : [ ],\r\n    \"headers\" : { },\r\n    \"method\" : \"POST\",\r\n    \"multiparts\" : [ ],\r\n    \"url\" : \"/test/api2\"\r\n  }\r\n}");
            },
            (builder) -> {
                builder
                        .withHeader("Accept-Content", "text/plain");
                return ImmutablePair.of("6A5E8E9D","{\r\n  \"request\" : {\r\n    \"body\" : \"k1=v1&k2=v2\",\r\n    \"bodyHash\" : -1571478275,\r\n    \"cookies\" : [ ],\r\n    \"headers\" : {\r\n      \"Accept-Content\" : \"text/plain\"\r\n    },\r\n    \"method\" : \"POST\",\r\n    \"multiparts\" : [ ],\r\n    \"url\" : \"/test/api2\"\r\n  }\r\n}");
            },
            (builder) -> {
                builder
                        .withHeader("Accept-Content", "text/xml");
                return ImmutablePair.of("B8E04D7C","{\r\n  \"request\" : {\r\n    \"body\" : \"k1=v1&k2=v2\",\r\n    \"bodyHash\" : -1571478275,\r\n    \"cookies\" : [ ],\r\n    \"headers\" : {\r\n      \"Accept-Content\" : [ \"text/plain\", \"text/xml\" ]\r\n    },\r\n    \"method\" : \"POST\",\r\n    \"multiparts\" : [ ],\r\n    \"url\" : \"/test/api2\"\r\n  }\r\n}");
            },
            (builder) -> {
                builder
                        .withCookie("Color-Scheme", "blue");
                return ImmutablePair.of("AAA67DF3","{\r\n  \"request\" : {\r\n    \"body\" : \"k1=v1&k2=v2\",\r\n    \"bodyHash\" : -1571478275,\r\n    \"cookies\" : [ \"Color-Scheme\" ],\r\n    \"headers\" : {\r\n      \"Accept-Content\" : [ \"text/plain\", \"text/xml\" ]\r\n    },\r\n    \"method\" : \"POST\",\r\n    \"multiparts\" : [ ],\r\n    \"url\" : \"/test/api2\"\r\n  }\r\n}");
            },
            (builder) -> {
                Collection<Request.Part> parts = new ArrayList<>();
                MockMultipart part = new MockMultipart().name("favorite-cat-poem.txt").body("There was a cat who...");
                part.header("Content-Disposition", "attachment");
                parts.add(part);
                builder.withMultiparts(parts);
                return ImmutablePair.of("4625080E","{\r\n  \"request\" : {\r\n    \"body\" : \"k1=v1&k2=v2\",\r\n    \"bodyHash\" : -1571478275,\r\n    \"cookies\" : [ \"Color-Scheme\" ],\r\n    \"headers\" : {\r\n      \"Accept-Content\" : [ \"text/plain\", \"text/xml\" ]\r\n    },\r\n    \"method\" : \"POST\",\r\n    \"multiparts\" : [ {\r\n      \"bodyHash\" : 45440525,\r\n      \"headers\" : {\r\n        \"Content-Disposition\" : \"attachment\"\r\n      },\r\n      \"name\" : \"favorite-cat-poem.txt\"\r\n    } ],\r\n    \"url\" : \"/test/api2\"\r\n  }\r\n}");
            },
            (builder) -> {
                builder.withBody("{'data0':'0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999','data1':'0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999','data2':'0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999'}'data3':'0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999','data4':'0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999'}");
                return ImmutablePair.of("6A8D455B","{\r\n  \"request\" : {\r\n    \"body\" : \"{'data0':'0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999','data1':'0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999','data2':'0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999'}'data3':'0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999','data4':'0000000000111111111122222222223333333333444444... (56 characters have been truncated)\",\r\n    \"bodyHash\" : -763273856,\r\n    \"cookies\" : [ \"Color-Scheme\" ],\r\n    \"headers\" : {\r\n      \"Accept-Content\" : [ \"text/plain\", \"text/xml\" ]\r\n    },\r\n    \"method\" : \"POST\",\r\n    \"multiparts\" : [ {\r\n      \"bodyHash\" : 45440525,\r\n      \"headers\" : {\r\n        \"Content-Disposition\" : \"attachment\"\r\n      },\r\n      \"name\" : \"favorite-cat-poem.txt\"\r\n    } ],\r\n    \"url\" : \"/test/api2\"\r\n  }\r\n}");
            }
    );

    List<Function<Response.Builder, ImmutablePair<String,String>>> responseBuilders = List.of(
            (builder) -> {
                builder
                        .status(200);
                return ImmutablePair.of("9EDFE657","{\r\n  \"response\" : {\r\n    \"body\" : \"\",\r\n    \"bodyHash\" : 1,\r\n    \"headers\" : { },\r\n    \"status\" : 200\r\n  }\r\n}");
            },
            (builder) -> {
                builder
                        .statusMessage("OK");
                return ImmutablePair.of("342D4AB9","{\r\n  \"response\" : {\r\n    \"body\" : \"\",\r\n    \"bodyHash\" : 1,\r\n    \"headers\" : { },\r\n    \"message\" : \"OK\",\r\n    \"status\" : 200\r\n  }\r\n}");
            },
            (builder) -> {
                builder
                        .body("Lorem Ipsum");
                return ImmutablePair.of("916A38EF","{\r\n  \"response\" : {\r\n    \"body\" : \"Lorem Ipsum\",\r\n    \"bodyHash\" : -358969414,\r\n    \"headers\" : { },\r\n    \"message\" : \"OK\",\r\n    \"status\" : 200\r\n  }\r\n}");
            },
            (builder) -> {
                builder
                        .body("Lorem Ipsum -- what the heck is that?");
                return ImmutablePair.of("1B121734","{\r\n  \"response\" : {\r\n    \"body\" : \"Lorem Ipsum -- what the heck is that?\",\r\n    \"bodyHash\" : 1158347372,\r\n    \"headers\" : { },\r\n    \"message\" : \"OK\",\r\n    \"status\" : 200\r\n  }\r\n}");
            },
            (builder) -> {
                builder
                        .headers(new HttpHeaders(
                                httpHeader("Content-Type", "text/plain"),
                                httpHeader("Cache-Control", "no-cache")));
                return ImmutablePair.of("516A332B","{\r\n  \"response\" : {\r\n    \"body\" : \"Lorem Ipsum -- what the heck is that?\",\r\n    \"bodyHash\" : 1158347372,\r\n    \"headers\" : {\r\n      \"Content-Type\" : \"text/plain\",\r\n      \"Cache-Control\" : \"no-cache\"\r\n    },\r\n    \"message\" : \"OK\",\r\n    \"status\" : 200\r\n  }\r\n}");
            },
            (builder) -> {
                builder
                        .status(404)
                        .statusMessage("Not found")
                        .body("We couldn't find the thing you were looking for.");
                return ImmutablePair.of("928C5090","{\r\n  \"response\" : {\r\n    \"body\" : \"We couldn't find the thing you were looking for.\",\r\n    \"bodyHash\" : 109342669,\r\n    \"headers\" : {\r\n      \"Content-Type\" : \"text/plain\",\r\n      \"Cache-Control\" : \"no-cache\"\r\n    },\r\n    \"message\" : \"Not found\",\r\n    \"status\" : 404\r\n  }\r\n}");
            }
    );

    static class HashableJson<ITEM_TYPE> {
        public final ITEM_TYPE item;
        public final String expectedHash;
        public final String expectedJson;
        public HashableJson(ITEM_TYPE item, String expectedHash, String expectedJson) {
            this.item = item;
            this.expectedHash = expectedHash;
            this.expectedJson = expectedJson;
        }
    }

    static int itemCount = 0;

    static <BUILDER, ITEM_TYPE> List<HashableJson<ITEM_TYPE>> build(
            Function<Integer, BUILDER> builderFactory,
            Function<BUILDER, ITEM_TYPE> converter,
            List<Function<BUILDER, ImmutablePair<String, String>>> builders)
    {
        List<HashableJson<ITEM_TYPE>> items = new ArrayList<>();
        for (int countToApply = 1; countToApply <= builders.size(); countToApply++) {
            BUILDER builder = builderFactory.apply(itemCount);
            itemCount++;
            ImmutablePair<String, String> expectedId = null;
            // Each request/response builds on the one before with one additional change.
            for (int i = 0; i < countToApply; i++) {
                expectedId = builders.get(i).apply(builder);
            }
            items.add(new HashableJson(converter.apply(builder), expectedId.getLeft(), expectedId.getRight()));
        }
        return items;
    }

    @Test
    public void requestTest() {
        final HashIdGenerator generator = new HashIdGenerator(REQUEST_HASH);
        final HashIdGenerator responseHashGenerator = new HashIdGenerator(RESPONSE_HASH);

        Response response = response()
                .status(200)
                .body("Lorem Ipsum")
                .headers(new HttpHeaders(
                        httpHeader("Content-Type", "text/plain"),
                        httpHeader("Cache-Control", "no-cache")))
                .build();

        List<HashableJson<Request>> requests = build(
                (count) -> new MockRequestBuilder(context, "Request #"+ count),
                (builder) -> builder.build(),
                requestBuilders);
        Set<String> uniqueHashes = new HashSet<>();
        Set<String> responseHashes = new HashSet<>();

        for (HashableJson<Request> entry : requests) {
            Request request = entry.item;
            HashIdGenerator.HashRequestResponseId id = generator.generate(request, response, response.getBody());
            assertThat("Request with expected hash " + entry.expectedHash + " produced the wrong JSON", Json.write(id.hashDetails), equalTo(entry.expectedJson));
            assertThat("Request with expected hash " + entry.expectedHash + " produced the wrong hash", id.value(), equalTo(entry.expectedHash));

            boolean isNew = uniqueHashes.add(id.value());
            assertThat("Each request should generate a new hash", isNew, equalTo(true));

            final HashIdGenerator.HashRequestResponseId responseHashId = responseHashGenerator.generate(request, response, response.getBody());
            assertThat(responseHashId.value(), not(equalTo(id.value())));
            responseHashes.add(responseHashId.value());

            // The same response is used each time so there should only be one hash returned by hash generator
            // with mode = RESPONSE_HASH.
            assertThat(responseHashes.size(), equalTo(1));
        }
    }

    @Test
    public void requestTestIgnoringHeaders()
    {
        final HashIdGenerator generator = new HashIdGenerator(REQUEST_HASH, ImmutableSet.of("Accept-Content"));

        Response response = response()
                .status(200)
                .body("Lorem Ipsum")
                .headers(new HttpHeaders(
                        httpHeader("Content-Type", "text/plain"),
                        httpHeader("Cache-Control", "no-cache")))
                .build();

        List<HashableJson<Request>> requests = build(
                (count) -> new MockRequestBuilder(context, "Request #"+ count),
                (builder) -> builder.build(),
                requestBuilders);

        Consumer<Integer> testRequest = (index) -> {
            HashableJson<Request> item = requests.get(index);
            Request request = item.item;
            HashIdGenerator.HashRequestResponseId id = generator.generate(request, response, response.getBody());

            String expectedJson = item.expectedJson;
            String expectedHash = item.expectedHash;

            // Strip out headers and updated the expected hash
            Pattern pattern = Pattern.compile("(.*?\"headers\" : )(\\{.*?\\})(.*)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(expectedJson);
            if (matcher.matches()) {
                expectedJson = matcher.replaceFirst("$1{ }$3");
                expectedHash = String.format("%08X", (long)expectedJson.hashCode() - Integer.MIN_VALUE);
            }

            assertThat("Request #" + index + " with expected hash " + item.expectedHash + " produced the wrong JSON", Json.write(id.hashDetails), equalTo(expectedJson));
            assertThat("Request #" + index + " with expected hash " + item.expectedHash + " produced the wrong hash", id.value(), equalTo(expectedHash));
        };

        for (int i = 0; i < requests.size(); i++)
            testRequest.accept(i);
    }

    @Test
    public void responseTest() {
        final HashIdGenerator generator = new HashIdGenerator(RESPONSE_HASH);
        final HashIdGenerator requestHashGenerator = new HashIdGenerator(REQUEST_HASH);

        List<HashableJson<Response>> responses = build(
                (count) -> response(),
                (builder) -> builder.build(),
                responseBuilders);
        Set<String> uniqueHashes = new HashSet<>();
        Set<String> requestHashes = new HashSet<>();

        for (HashableJson<Response> entry : responses) {
            Response response = entry.item;
            final HashIdGenerator.HashRequestResponseId id = generator.generate(requestForResponseTest, response, response.getBody());
            assertThat("Response with expected hash " + entry.expectedHash + " produced the wrong JSON", Json.write(id.hashDetails), equalTo(entry.expectedJson));
            assertThat("Response with expected hash " + entry.expectedHash + " produced the wrong hash", id.value(), equalTo(entry.expectedHash));

            boolean isNew = uniqueHashes.add(id.value());
            assertThat("Each response should generate a new hash", isNew, equalTo(true));

            final HashIdGenerator.HashRequestResponseId requestHashId = requestHashGenerator.generate(requestForResponseTest, response, response.getBody());
            assertThat(requestHashId.value(), not(equalTo(id.value())));
            requestHashes.add(requestHashId.value());

            // The same request is used each time so there should only be one hash returned by hash generator
            // with mode = REQUEST_HASH.
            assertThat(requestHashes.size(), equalTo(1));
        }
    }

    @Test
    public void consistentHashesTest() {
        // Run the same tests as before a number of times and validate hashes don't change.
        for (int i = 0; i < 10; i++) {
            requestTest();
            responseTest();
        }
    }

    @Test
    public void requestResponseTest() {
        List<HashableJson<Request>> requests = build(
                (count) -> new MockRequestBuilder(context, "Request #"+ count),
                (builder) -> builder.build(),
                requestBuilders);
        List<HashableJson<Response>> responses = build(
                (count) -> response(),
                (builder) -> builder.build(),
                responseBuilders);
        LinkedHashSet<String> uniqueHashes = new LinkedHashSet<>();
        ArrayList<String> hashDetails = new ArrayList<>();

        final HashIdGenerator generator = new HashIdGenerator(REQUEST_RESPONSE_HASH);

        for (HashableJson<Request> requestEntry : requests) {
            Request request = requestEntry.item;
            for (HashableJson<Response> responseEntry : responses) {
                Response response = responseEntry.item;
                final HashIdGenerator.HashRequestResponseId id = generator.generate(request, response, response.getBody());
                boolean isNew = uniqueHashes.add(id.value());
                String detailsJson = Json.write(id.hashDetails);
                int previousInstance = hashDetails.indexOf(detailsJson);
                hashDetails.add(detailsJson);
                assertThat("Each request/response combination should generate a new hash (previous instance = " + previousInstance +")", isNew, equalTo(true));
            }
        }
        assertThat(uniqueHashes.size(), equalTo(requests.size() * responses.size()));
    }

}
