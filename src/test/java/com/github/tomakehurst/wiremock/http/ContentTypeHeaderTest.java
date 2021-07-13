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
package com.github.tomakehurst.wiremock.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.common.Strings;
import com.github.tomakehurst.wiremock.testsupport.MockRequestBuilder;
import com.google.common.base.Optional;
import java.nio.charset.StandardCharsets;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class ContentTypeHeaderTest {

  private Mockery context;

  @Before
  public void init() {
    context = new Mockery();
  }

  @Test
  public void returnsMimeTypeAndCharsetWhenBothPresent() {
    ContentTypeHeader header = new ContentTypeHeader("text/plain; charset=utf-8");
    assertThat(header.mimeTypePart(), is("text/plain"));
    Optional<String> encoding = header.encodingPart();
    assertTrue(encoding.isPresent());
    assertThat(encoding.get(), is("utf-8"));
  }

  @Test
  public void returnsMimeTypeWhenNoCharsetPresent() {
    ContentTypeHeader header = new ContentTypeHeader("text/plain");
    assertThat(header.mimeTypePart(), is("text/plain"));
  }

  @Test
  public void returnsCharsetWhenNotFirstParameter() {
    ContentTypeHeader header = new ContentTypeHeader("text/plain; param=value; charset=utf-8");
    Optional<String> encoding = header.encodingPart();
    assertTrue(encoding.isPresent());
    assertThat(encoding.get(), is("utf-8"));
  }

  @Test
  public void returnsAbsentOptionalEncodingPartWhenNotPresent() {
    ContentTypeHeader header = new ContentTypeHeader("text/plain");
    assertFalse(header.encodingPart().isPresent());
  }

  @Test
  public void stripsDoubleQuotesFromEncodingPart() {
    ContentTypeHeader header = new ContentTypeHeader("application/json;charset=\"UTF-8\"");
    Optional<String> encoding = header.encodingPart();
    assertTrue(encoding.isPresent());
    assertThat(encoding.get(), is("UTF-8"));
  }

  @Test
  public void fetchesFromRequest() {
    Request request =
        new MockRequestBuilder(context).withHeader("Content-Type", "text/xml").build();

    ContentTypeHeader contentTypeHeader = request.contentTypeHeader();
    assertThat(contentTypeHeader.mimeTypePart(), is("text/xml"));
  }

  @Test(expected = NullPointerException.class)
  public void throwsExceptionOnAttemptToSetNullHeaderValue() {
    Request request = new MockRequestBuilder(context).withHeader("Content-Type", null).build();

    request.contentTypeHeader();
  }

  @Test
  public void returnsNullFromMimeTypePartWhenContentTypeIsAbsent() {
    ContentTypeHeader header = ContentTypeHeader.absent();
    assertThat(header.mimeTypePart(), is(nullValue()));
  }

  @Test
  public void returnsCharsetWhenPresent() {
    ContentTypeHeader header = new ContentTypeHeader("text/plain; charset=iso-8859-1");
    assertThat(header.charset(), is(StandardCharsets.ISO_8859_1));
  }

  @Test
  public void returnsDefaultCharsetWhenEncodingNotPresent() {
    ContentTypeHeader header = new ContentTypeHeader("text/plain");
    assertThat(header.charset(), is(Strings.DEFAULT_CHARSET));
  }

  @Test
  public void returnsDefaultCharsetWhenAbsent() {
    ContentTypeHeader header = ContentTypeHeader.absent();
    assertThat(header.charset(), is(Strings.DEFAULT_CHARSET));
  }
}
