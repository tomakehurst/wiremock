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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import org.junit.Test;

public class ContentTypesTest {

  @Test
  public void detectsTextTypesCorrectlyFromFileExtension() {
    assertTrue(ContentTypes.determineIsTextFromExtension("txt"));
    assertTrue(ContentTypes.determineIsTextFromExtension("json"));
    assertTrue(ContentTypes.determineIsTextFromExtension("xml"));
    assertTrue(ContentTypes.determineIsTextFromExtension("html"));
    assertTrue(ContentTypes.determineIsTextFromExtension("htm"));
    assertTrue(ContentTypes.determineIsTextFromExtension("yaml"));
    assertTrue(ContentTypes.determineIsTextFromExtension("csv"));

    assertFalse(ContentTypes.determineIsTextFromExtension("jpg"));
    assertFalse(ContentTypes.determineIsTextFromExtension("png"));
    assertFalse(ContentTypes.determineIsTextFromExtension(null));
    assertFalse(ContentTypes.determineIsTextFromExtension(""));
  }

  @Test
  public void detectsTextTypesCorrectlyFromMimeType() {
    assertTrue(ContentTypes.determineIsTextFromMimeType("text/plain"));
    assertTrue(ContentTypes.determineIsTextFromMimeType("text/html"));
    assertTrue(ContentTypes.determineIsTextFromMimeType("application/json"));
    assertTrue(ContentTypes.determineIsTextFromMimeType("some-preamble; application/json"));
    assertTrue(ContentTypes.determineIsTextFromMimeType("application/blah.something+json"));
    assertTrue(ContentTypes.determineIsTextFromMimeType("application/xml"));
    assertTrue(ContentTypes.determineIsTextFromMimeType("text/xml"));
    assertTrue(ContentTypes.determineIsTextFromMimeType("text/xml; utf-8"));
    assertTrue(ContentTypes.determineIsTextFromMimeType("application/csv"));
    assertTrue(ContentTypes.determineIsTextFromMimeType("application/x-www-form-urlencoded"));

    assertFalse(ContentTypes.determineIsTextFromMimeType("application/octet-stream"));
    assertFalse(ContentTypes.determineIsTextFromMimeType("image/jpeg"));
    assertFalse(ContentTypes.determineIsTextFromMimeType("application/pdf"));
  }

  @Test
  public void detectsTextTypesCorrectlyFromExtensionOrMimeType() {
    assertTrue(ContentTypes.determineIsText("txt", "text/plain"));
    assertTrue(ContentTypes.determineIsText("xml", ""));
    assertTrue(ContentTypes.determineIsText("json", null));

    assertFalse(ContentTypes.determineIsText("png", null));
    assertFalse(ContentTypes.determineIsText(null, "image/jpeg"));
  }

  @Test
  public void correctlyDeterminesFileExtensionWhenDotsInPath() {
    String fileExtension =
        ContentTypes.determineFileExtension(
            "http://some.host/path.with.dots/and/several/segments",
            ContentTypeHeader.absent(),
            new byte[] {});

    assertThat(fileExtension, is("txt"));
  }

  @Test
  public void correctlyDeterminesFileExtensionFromUrl() {
    String fileExtension =
        ContentTypes.determineFileExtension(
            "http://some.host/path.with.dots/image.png", ContentTypeHeader.absent(), new byte[] {});

    assertThat(fileExtension, is("png"));
  }
}
