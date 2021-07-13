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

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.junit.Before;
import org.junit.Test;

public class RegexExtractHelperTest extends HandlebarsHelperTestBase {

  private RegexExtractHelper helper;

  @Before
  public void init() {
    helper = new RegexExtractHelper();
    LocalNotifier.set(new ConsoleNotifier(true));
  }

  @Test
  public void canExtractSingleRegexMatch() {
    final ResponseDefinition responseDefinition =
        this.transformer.transform(
            mockRequest().url("/api/abc,def,ghi"),
            aResponse()
                .withBody("{\"test\": \"{{regexExtract request.path.[1] '([A-Za-z]+)'}}\"}")
                .build(),
            noFileSource(),
            Parameters.empty());

    assertThat(responseDefinition.getBody(), is("{\"test\": \"abc\"}"));
  }

  @Test
  public void canExtractMultipleRegexMatches() {
    final ResponseDefinition responseDefinition =
        this.transformer.transform(
            mockRequest().url("/api/abc,def,ghi"),
            aResponse()
                .withBody(
                    "{\"test\": \"{{regexExtract request.path.[1] '([A-Za-z]+)' 'parts'}}{{#each parts}}{{this}} {{/each}}\"}")
                .build(),
            noFileSource(),
            Parameters.empty());

    assertThat(responseDefinition.getBody(), is("{\"test\": \"abc def ghi \"}"));
  }

  @Test
  public void noMatchErrorWhenNoRegexMatch() {
    testHelperError(
        helper, "/123/456,789,900", "([A-Za-z]+)", is("[ERROR: Nothing matched ([A-Za-z]+)]"));
  }

  @Test
  public void invalidRegExErrorWhenRegexStringIsInvalid() {
    testHelperError(
        helper,
        "/123/456,789,900",
        "(([A-Za-z]+)",
        is("[ERROR: Invalid regex string (([A-Za-z]+)]"));
  }
}
