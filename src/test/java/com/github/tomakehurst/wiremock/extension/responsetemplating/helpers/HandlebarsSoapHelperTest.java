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
package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static com.github.tomakehurst.wiremock.testsupport.NoFileSource.noFileSource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class HandlebarsSoapHelperTest extends HandlebarsHelperTestBase {

  private HandlebarsSoapHelper helper;
  private ResponseTemplateTransformer transformer;

  @Before
  public void init() {
    this.helper = new HandlebarsSoapHelper();
    this.transformer = new ResponseTemplateTransformer(true);
  }

  @Test
  public void extractsASimpleBodyValue() throws IOException {
    testHelper(
        this.helper,
        "<Envelope><Body><test>success</test></Body></Envelope>",
        "/test/text()",
        "success");
  }

  @Test
  public void rendersASimpleValue() {
    final ResponseDefinition responseDefinition =
        this.transformer.transform(
            mockRequest()
                .url("/soap")
                .body(
                    "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope/\"><soap:Body><m:a><m:test>success</m:test></m:a></soap:Body></soap:Envelope>"),
            aResponse()
                .withBody("<test>{{soapXPath request.body '/a/test/text()'}}</test>")
                .build(),
            noFileSource(),
            Parameters.empty());

    assertThat(responseDefinition.getBody(), is("<test>success</test>"));
  }

  @Test
  public void negativeTestResponseTemplate() {
    final ResponseDefinition responseDefinition =
        this.transformer.transform(
            mockRequest()
                .url("/soap")
                .body(
                    "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope/\"><soap:Body><m:a><m:test>success</m:test></m:a></soap:Body></soap:Envelope>"),
            aResponse().withBody("<test>{{soapXPath request.body '/b/test'}}</test>").build(),
            noFileSource(),
            Parameters.empty());

    assertThat(responseDefinition.getBody(), is("<test></test>"));
  }
}
