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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.http.HttpHeader.httpHeader;
import static com.github.tomakehurst.wiremock.http.ResponseDefinition.copyOf;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class ResponseDefinitionTest {

    @Test
    public void copyProducesEqualObject() {
        ResponseDefinition response = new ResponseDefinition(
                222,
                null,
                "blah",
                null,
                null,
                "name.json",
                new HttpHeaders(httpHeader("thing", "thingvalue")),
                null,
                1112,
                null,
                null,
                "http://base.com",
                null,
                Fault.EMPTY_RESPONSE,
                ImmutableList.of("transformer-1"),
                Parameters.one("name", "Jeff"),
                true
        );

        ResponseDefinition copiedResponse = copyOf(response);
        
        assertTrue(response.equals(copiedResponse));
    }
    
    @Test
    public void copyPreservesConfiguredFlag() {
        ResponseDefinition response = ResponseDefinition.notConfigured();
        ResponseDefinition copiedResponse = copyOf(response);
        assertFalse("Should be not configured", copiedResponse.wasConfigured());
    }

    private static final String STRING_BODY =
            "{	        								\n" +
            "		\"status\": 200,    				\n" +
            "		\"body\": \"String content\" 		\n" +
            "}											";

    @Test
    public void correctlyUnmarshalsFromJsonWhenBodyIsAString() {
        ResponseDefinition responseDef = Json.read(STRING_BODY, ResponseDefinition.class);
        assertThat(responseDef.getBase64Body(), is(nullValue()));
        assertThat(responseDef.getJsonBody(), is(nullValue()));
        assertThat(responseDef.getBody(), is("String content"));
    }

    private static final String JSON_BODY =
            "{	        								\n" +
                    "		\"status\": 200,    				\n" +
                    "		\"jsonBody\": {\"name\":\"wirmock\",\"isCool\":true} \n" +
                    "}											";

    @Test
    public void correctlyUnmarshalsFromJsonWhenBodyIsJson() {
        ResponseDefinition responseDef = Json.read(JSON_BODY, ResponseDefinition.class);
        assertThat(responseDef.getBase64Body(), is(nullValue()));
        assertThat(responseDef.getBody(), is(nullValue()));

        JsonNode jsonNode = Json.node("{\"name\":\"wirmock\",\"isCool\":true}");
        assertThat(responseDef.getJsonBody(), is(jsonNode));

    }

    @Test
    public void correctlyMarshalsToJsonWhenBodyIsAString() throws Exception {
        ResponseDefinition responseDef = responseDefinition()
                .withStatus(200)
                .withBody("String content")
                .build();

        JSONAssert.assertEquals(STRING_BODY, Json.write(responseDef), false);
    }

    private static final byte[] BODY = new byte[] {1, 2, 3};
    private static final String BASE64_BODY = "AQID";
    private static final String BINARY_BODY =
            "{	        								        \n" +
            "		\"status\": 200,    				        \n" +
            "		\"base64Body\": \"" + BASE64_BODY + "\"     \n" +
            "}											        ";

    @Test
    public void correctlyUnmarshalsFromJsonWhenBodyIsBinary() {
        ResponseDefinition responseDef = Json.read(BINARY_BODY, ResponseDefinition.class);
        assertThat(responseDef.getBody(), is(nullValue()));
        assertThat(responseDef.getByteBody(), is(BODY));
    }

    @Test
    public void correctlyMarshalsToJsonWhenBodyIsBinary() throws Exception {
        ResponseDefinition responseDef = responseDefinition().withStatus(200).withBase64Body(BASE64_BODY).build();

        String actualJson = Json.write(responseDef);
        JSONAssert.assertEquals(actualJson,
                BINARY_BODY, false);
    }

    @Test
    public void indicatesBodyFileIfBodyContentIsNotAlsoSpecified() {
        ResponseDefinition responseDefinition = responseDefinition().withBodyFile("my-file").build();

        assertTrue(responseDefinition.specifiesBodyFile());
        assertFalse(responseDefinition.specifiesBodyContent());
    }

    @Test
    public void doesNotIndicateBodyFileIfBodyContentIsAlsoSpecified() {
        ResponseDefinition responseDefinition = responseDefinition().withBodyFile("my-file").withBody("hello").build();

        assertFalse(responseDefinition.specifiesBodyFile());
        assertTrue(responseDefinition.specifiesBodyContent());
    }

    @Test
    public void omitsResponseTransformerAttributesFromJsonWhenEmpty() {
        String json = Json.write(new ResponseDefinition(200, ""));

        assertThat(json, not(containsString("transformers")));
        assertThat(json, not(containsString("transformerParameters")));
    }

}
