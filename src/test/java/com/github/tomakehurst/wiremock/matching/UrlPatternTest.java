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
package com.github.tomakehurst.wiremock.matching;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Test;

public class UrlPatternTest {

  @Test
  public void matchesExactUrlWithQuery() {
    UrlPattern urlPattern = WireMock.urlEqualTo("/my/exact/url?one=1&two=2&three=3333333");
    assertTrue(urlPattern.match("/my/exact/url?one=1&two=2&three=3333333").isExactMatch());
    assertFalse(urlPattern.match("/my/wrong/url?one=1&three=3333333").isExactMatch());
  }

  @Test
  public void matchesOnRegexWithQuery() {
    UrlPattern urlPattern =
        WireMock.urlMatching("/my/([a-z]*)/url\\?one=1&two=([0-9]*)&three=3333333");
    assertTrue(urlPattern.match("/my/regex/url?one=1&two=123456&three=3333333").isExactMatch());
    assertFalse(urlPattern.match("/my/BAD/url?one=1&two=123456&three=3333333").isExactMatch());
  }

  @Test
  public void matchesExactlyOnPathOnly() {
    UrlPathPattern urlPathPattern = WireMock.urlPathEqualTo("/the/exact/path");
    assertTrue(urlPathPattern.match("/the/exact/path").isExactMatch());
    assertFalse(urlPathPattern.match("/totally/incorrect/path").isExactMatch());
  }

  @Test
  public void matchesOnPathWithRegex() {
    UrlPathPattern urlPathPattern = WireMock.urlPathMatching("/my/([a-z]*)/path");
    assertTrue(urlPathPattern.match("/my/regex/path?one=not_looked_at").isExactMatch());
    assertFalse(urlPathPattern.match("/my/12345/path").isExactMatch());
  }

  @Test
  public void noMatchOnNullValueForUrlEquality() {
    assertThat(WireMock.urlEqualTo("/things").match(null).isExactMatch(), is(false));
  }

  @Test
  public void noMatchOnNullValueForUrlPathEquality() {
    assertThat(WireMock.urlPathEqualTo("/things").match(null).isExactMatch(), is(false));
  }

  @Test
  public void noMatchOnNullValueForUrlRegex() {
    assertThat(WireMock.urlMatching("/things/.*").match(null).isExactMatch(), is(false));
  }

  @Test
  public void noMatchOnNullValueForUrlPathRegex() {
    assertThat(WireMock.urlPathMatching("/things/.*").match(null).isExactMatch(), is(false));
  }
}
