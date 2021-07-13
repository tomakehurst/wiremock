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
package com.github.tomakehurst.wiremock.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.github.tomakehurst.wiremock.http.QueryParameter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class UrlsTest {

  private Map<String, QueryParameter> params;

  @Test
  public void copesWithEqualsInParamValues() {
    params = Urls.splitQuery(URI.create("/thing?param1=one&param2=one==two=three"));
    assertThat(params.get("param1").firstValue(), is("one"));
    assertThat(params.get("param2").firstValue(), is("one==two=three"));
  }

  @Test
  public void returnsEmptyStringsAsValuesWhenOnlyKeysArePresent() {
    params = Urls.splitQuery(URI.create("/thing?param1&param2&param3"));
    assertThat(params.get("param1").firstValue(), is(""));
    assertThat(params.get("param2").firstValue(), is(""));
    assertThat(params.get("param3").firstValue(), is(""));
  }

  @Test
  public void supportsMultiValuedParameters() {
    params = Urls.splitQuery(URI.create("/thing?param1=1&param2=two&param1=2&param1=3"));
    assertThat(params.size(), is(2));
    assertThat(params.get("param1").isSingleValued(), is(false));
    assertThat(params.get("param1").values(), hasItems("1", "2", "3"));
  }

  @Test
  public void doesNotAttemptToDoubleDecodeSplitQueryString() {
    URI url = URI.create("/thing?q=a%25b");
    Map<String, QueryParameter> query = Urls.splitQuery(url);
    assertThat(query.get("q").firstValue(), is("a%b"));
  }

  @Test
  public void returnsEmptyStringForEmptyUrlPathParts() {
    assertThat(Urls.urlToPathParts(URI.create("/")), is(""));
    assertThat(Urls.urlToPathParts(URI.create("http://www.wiremock.org/")), is(""));
  }

  @Test
  public void returnsNonDelimitedStringForUrlWithOnePathPart() {
    String pathParts = Urls.urlToPathParts(URI.create("/foo?param=value"));
    assertThat(pathParts, is("foo"));
  }

  @Test
  public void returnsDelimitedStringForUrlWithTwoPathParts() {
    String pathParts = Urls.urlToPathParts(URI.create("/foo/bar/?param=value"));
    assertThat(pathParts, is("foo-bar"));
  }

  @Test
  public void returnsNonDelimitedStringForUrlWithMoreThanTwoPathParts() {
    String pathParts = Urls.urlToPathParts(URI.create("/foo/bar/zoo/wire/mock?param=value"));
    assertThat(pathParts, is("foo-bar-zoo-wire-mock"));
  }

  @Test
  public void splitsQueryFromUrl() {
    Map<String, QueryParameter> query = Urls.splitQueryFromUrl("/a/b?one=1&one=11&two=2");

    List<String> oneValues = query.get("one").values();
    assertThat(oneValues, hasItems("1", "11"));
    assertThat(oneValues, hasItem("11"));
    assertThat(query.get("two").firstValue(), is("2"));
  }

  @Test
  public void splitsQueryFromUrlWithTrailingSlash() {
    Map<String, QueryParameter> query = Urls.splitQueryFromUrl("/a/b/?one=1&one=11&two=2");

    List<String> oneValues = query.get("one").values();
    assertThat(oneValues, hasItems("1", "11"));
    assertThat(oneValues, hasItem("11"));
    assertThat(query.get("two").firstValue(), is("2"));
  }

  @Test
  public void splitQueryFromUrlHandlesUrlThatEndsWithQuestionMark() {
    Map<String, QueryParameter> query = Urls.splitQueryFromUrl("/a/b/?");
    assertThat(query.isEmpty(), is(true));
  }

  @Test
  public void splitQueryFromUrlReturnsEmptyWhenNoQuery() {
    Map<String, QueryParameter> query = Urls.splitQueryFromUrl("/a/b");
    assertThat(query.isEmpty(), is(true));
  }

  @Test
  public void splitQueryFromUrlReturnsEmptyWhenTrailingSlashAndNoQuery() {
    Map<String, QueryParameter> query = Urls.splitQueryFromUrl("/a/b/");
    assertThat(query.isEmpty(), is(true));
  }

  @Test
  public void getsThePathFromAUrlWithAQueryString() {
    assertThat(Urls.getPath("/one/two/3?q=a"), is("/one/two/3"));
  }

  @Test
  public void getsThePathFromAUrlWithoutAQueryString() {
    assertThat(Urls.getPath("/one/two/3"), is("/one/two/3"));
  }

  @Test
  public void getsThePathFromAUrlWithATrailingSlashAndQueryString() {
    assertThat(Urls.getPath("/one/two/3/?q=a"), is("/one/two/3/"));
  }

  @Test
  public void getsThePathFromAUrlWithRootPathAndQuery() {
    assertThat(Urls.getPath("/?q=a"), is("/"));
  }

  @Test
  public void getsThePathFromAUrlWithJustAQuery() {
    assertThat(Urls.getPath("?q=a"), is(""));
  }
}
