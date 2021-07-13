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
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Test;

public class MathsHelperTest extends HandlebarsHelperTestBase {

  MathsHelper helper;

  @Before
  public void init() {
    helper = new MathsHelper();
  }

  @Test
  public void returnsAnErrorIfNotExactlyTwoParameters() throws Exception {
    String expectedError =
        "[ERROR: All maths functions require two operands and an operator as parameters e.g. 3 '+' 2]";

    assertThat(renderHelperValue(helper, 5, "+"), is(expectedError));
    assertThat(renderHelperValue(helper, 5, "+", "6", true, 1), is(expectedError));
    assertThat(renderHelperValue(helper, 5), is(expectedError));
  }

  @Test
  public void returnsAnErrorIfOperatorNotRecognised() throws Exception {
    assertThat(
        renderHelperValue(helper, 2, "&", 3),
        is("[ERROR: & is not a valid mathematical operator]"));
  }

  @Test
  public void addsTwoIntegers() throws Exception {
    assertThat(renderHelperValue(helper, 2, "+", 3), is(5));
  }

  @Test
  public void addsTwoLongs() throws Exception {
    long left = ((long) Integer.MAX_VALUE) + 1;
    long right = ((long) Integer.MAX_VALUE) + 1;
    long expected = (((long) Integer.MAX_VALUE) * 2) + 2;
    assertThat(renderHelperValue(helper, left, "+", right), is(expected));
  }

  @Test
  public void addsAStringAndInteger() throws Exception {
    assertThat(renderHelperValue(helper, "2", "+", 3), is(5));
  }

  @Test
  public void addsADoubleAndInteger() throws Exception {
    assertThat(renderHelperValue(helper, 0.5, "+", 3), is(3.5));
  }

  @Test
  public void addsAStringDoubleAndDouble() throws Exception {
    assertThat(renderHelperValue(helper, "0.25", "+", "0.34"), is(0.59));
  }

  @Test
  public void addsADoubleAndFloat() throws Exception {
    assertThat(renderHelperValue(helper, 0.25f, "+", 0.34f), closeTo(0.59, 0.01));
  }

  @Test
  public void subtractsTwoIntegers() throws Exception {
    assertThat(renderHelperValue(helper, 10, "-", 3), is(7));
  }

  @Test
  public void multipliesTwoIntegers() throws Exception {
    assertThat(renderHelperValue(helper, 10, "*", 3), is(30));
    assertThat(renderHelperValue(helper, 10, "x", 3), is(30));
  }

  @Test
  public void dividesTwoIntegers() throws Exception {
    assertThat(renderHelperValue(helper, 15, "/", 3), is(5));
  }

  @Test
  public void modsTwoIntegers() throws Exception {
    assertThat(renderHelperValue(helper, 11, "%", 3), is(2));
  }
}
