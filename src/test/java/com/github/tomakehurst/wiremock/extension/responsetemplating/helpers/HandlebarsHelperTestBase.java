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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.RenderCache;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;

public abstract class HandlebarsHelperTestBase {

  protected ResponseTemplateTransformer transformer;
  protected RenderCache renderCache;

  @Before
  public void initRenderCache() {
    transformer = new ResponseTemplateTransformer(true);
    renderCache = new RenderCache();
  }

  protected static final String FAIL_GRACEFULLY_MSG =
      "Handlebars helper should fail gracefully and show the issue directly in the response.";

  protected <T> void testHelperError(
      Helper<T> helper, T content, String pathExpression, Matcher<String> expectation) {
    try {
      assertThat((String) renderHelperValue(helper, content, pathExpression), expectation);
    } catch (final IOException e) {
      Assert.fail(FAIL_GRACEFULLY_MSG);
    }
  }

  @SuppressWarnings("unchecked")
  protected <R, C> R renderHelperValue(Helper<C> helper, C content, Object... parameters)
      throws IOException {
    return (R) helper.apply(content, createOptions(parameters));
  }

  protected <T> void testHelper(Helper<T> helper, T content, String optionParam, String expected)
      throws IOException {
    testHelper(helper, content, optionParam, is(expected));
  }

  protected <T> void testHelper(
      Helper<T> helper, T content, String optionParam, Matcher<String> expected)
      throws IOException {
    assertThat(helper.apply(content, createOptions(map(), optionParam)).toString(), expected);
  }

  protected Options createOptions(Object... optionParams) {
    return createOptions(map(), optionParams);
  }

  protected Options createOptions(Map<String, Object> hash, Object... optionParams) {
    return createOptions(renderCache, hash, optionParams);
  }

  protected Options createOptions(
      RenderCache renderCache, Map<String, Object> hash, Object... optionParams) {
    Context context = createContext(renderCache);

    return new Options(
        null, null, null, context, null, null, optionParams, hash, new ArrayList<String>(0));
  }

  protected Context createContext() {
    return createContext(renderCache);
  }

  private Context createContext(RenderCache renderCache) {
    return Context.newBuilder(null).combine("renderCache", renderCache).build();
  }

  protected static Map<String, Object> map() {
    return new HashMap<>();
  }

  protected static Map<String, Object> map(String key, Object value) {
    final HashMap<String, Object> map = new HashMap<>();
    map.put(key, value);
    return map;
  }
}
