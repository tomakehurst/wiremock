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

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.Test;

public class SafeNamesTest {

  @Test
  public void generatesNameFromStubNameWhenPresent() {
    StubMapping mapping =
        WireMock.get("/named").withName("This is a NAMED stub").willReturn(ok()).build();

    assertThat(
        SafeNames.makeSafeFileName(mapping),
        is("this-is-a-named-stub-" + mapping.getId() + ".json"));
  }

  @Test
  public void generatesNameFromStubUrlWhenNameNotPresent() {
    StubMapping mapping =
        WireMock.get(urlMatching("/named/([0-9]*)/things")).willReturn(ok()).build();

    assertThat(
        SafeNames.makeSafeFileName(mapping), is("named0-9things-" + mapping.getId() + ".json"));
  }

  @Test
  public void generatesNameWhenStubUrlIsAnyAndNameNotPresent() {
    StubMapping mapping = WireMock.get(anyUrl()).willReturn(ok()).build();

    assertThat(SafeNames.makeSafeFileName(mapping), is(mapping.getId() + ".json"));
  }

  @Test
  public void generatesNameFromNameWithCharactersSafeForFilenames() {
    String output = SafeNames.makeSafeName("ẄǏŔe mȎČǨs it!");
    assertThat(output, is("wire-mocks-it"));
  }

  @Test
  public void doesNothingWhenAlreadySafe() {
    String input = "wire-mocks__it--123-4";
    String output = SafeNames.makeSafeName(input);
    assertThat(output, is(input));
  }

  @Test
  public void generatesNameFromUrlPathWithCharactersSafeForFilenames() {
    String output = SafeNames.makeSafeNameFromUrl("/hello/1/2/3__!/ẮČĖ--ace/¥$$/$/and/¿?");
    assertThat(output, is("hello_1_2_3___ace--ace___and"));
  }

  @Test
  public void truncatesWhenResultingNameOver200Chars() {
    String output =
        SafeNames.makeSafeNameFromUrl(
            "/hello/1/2/3__!/ẮČĖ--ace/¥$$/$/andverylongstuffandverylongstuffandverylongstuffandverylongstuffandverylongstuffandverylongstuffandverylongstuffandverylongstuffandverylongstuffandverylongstuffandverylongstuffandverylongstuffandverylongstuff/¿?");
    assertThat(output.length(), is(200));
  }
}
