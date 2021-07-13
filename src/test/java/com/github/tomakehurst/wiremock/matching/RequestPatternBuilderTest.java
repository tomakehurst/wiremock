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
/*
 * Copyright (C) 2017 Thomas Akehurst
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
package com.github.tomakehurst.wiremock.matching;

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class RequestPatternBuilderTest {
  @Test
  public void likeRequestPatternWithDifferentUrl() {
    RequestPattern requestPattern = RequestPattern.everything();

    RequestPattern newRequestPattern =
        RequestPatternBuilder.like(requestPattern).but().withUrl("/foo").build();

    assertThat(newRequestPattern.getUrl(), is("/foo"));
    assertThat(newRequestPattern, not(equalTo(requestPattern)));
  }

  @Test
  public void likeRequestPatternWithoutCustomMatcher() {
    // Use a RequestPattern with everything defined except a custom matcher to ensure all fields are
    // set properly
    RequestPattern requestPattern =
        new RequestPattern(
            "https",
            WireMock.equalTo("my.wiremock.org"),
            1234,
            WireMock.urlEqualTo("/foo"),
            RequestMethod.POST,
            ImmutableMap.of("X-Header", MultiValuePattern.of(WireMock.equalTo("bar"))),
            ImmutableMap.of("query_param", MultiValuePattern.of(WireMock.equalTo("bar"))),
            ImmutableMap.of("cookie", WireMock.equalTo("yum")),
            new BasicCredentials("user", "pass"),
            ImmutableList.<ContentPattern<?>>of(WireMock.equalTo("BODY")),
            null,
            null,
            null);

    RequestPattern newRequestPattern = RequestPatternBuilder.like(requestPattern).build();
    assertThat(newRequestPattern, is(requestPattern));
  }

  @Test
  public void likeRequestPatternWithCustomMatcher() {
    RequestMatcher customRequestMatcher =
        new RequestMatcherExtension() {
          @Override
          public MatchResult match(Request request, Parameters parameters) {
            return MatchResult.noMatch();
          }
        };
    RequestPattern requestPattern = new RequestPattern(customRequestMatcher);

    RequestPattern newRequestPattern = RequestPatternBuilder.like(requestPattern).build();
    assertThat(newRequestPattern, is(requestPattern));
  }

  @Test
  public void likeRequestPatternWithMultipartMatcher() {
    MultipartValuePattern multipartValuePattern = aMultipart().withBody(equalToJson("[]")).build();

    RequestPattern requestPattern = RequestPattern.everything();
    RequestPattern newRequestPattern =
        RequestPatternBuilder.like(requestPattern)
            .but()
            .withRequestBodyPart(multipartValuePattern)
            .build();

    assertThat(
        newRequestPattern.getMultipartPatterns(), everyItem(isIn(asList(multipartValuePattern))));
    assertThat(newRequestPattern, not(equalTo(requestPattern)));
  }

  @Test
  public void likeRequestPatternWithoutMultipartMatcher() {
    MultipartValuePattern multipartPattern = aMultipart().withBody(equalToJson("[]")).build();

    // Use a RequestPattern with everything defined except a custom matcher to ensure all fields are
    // set properly
    RequestPattern requestPattern =
        new RequestPattern(
            "https",
            WireMock.equalTo("my.wiremock.org"),
            1234,
            WireMock.urlEqualTo("/foo"),
            RequestMethod.POST,
            ImmutableMap.of("X-Header", MultiValuePattern.of(WireMock.equalTo("bar"))),
            ImmutableMap.of("query_param", MultiValuePattern.of(WireMock.equalTo("bar"))),
            ImmutableMap.of("cookie", WireMock.equalTo("yum")),
            new BasicCredentials("user", "pass"),
            ImmutableList.<ContentPattern<?>>of(WireMock.equalTo("BODY")),
            null,
            null,
            asList(multipartPattern));

    RequestPattern newRequestPattern = RequestPatternBuilder.like(requestPattern).build();
    assertThat(newRequestPattern, is(requestPattern));
  }

  @Test
  public void likeRequestPatternWithCustomMatcherDefinition() {
    CustomMatcherDefinition customMatcherDefinition =
        new CustomMatcherDefinition("foo", Parameters.empty());
    RequestPattern requestPattern = new RequestPattern(customMatcherDefinition);

    RequestPattern newRequestPattern = RequestPatternBuilder.like(requestPattern).build();
    assertThat(newRequestPattern, is(requestPattern));
  }
}
