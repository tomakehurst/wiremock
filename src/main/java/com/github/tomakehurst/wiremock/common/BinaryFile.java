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

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class BinaryFile implements InputStreamSource {

  private URI uri;

  public BinaryFile(URI uri) {
    this.uri = uri;
  }

  public byte[] readContents() {
    try (InputStream stream = getStream()) {
      return ByteStreams.toByteArray(stream);
    } catch (final IOException ioe) {
      return throwUnchecked(ioe, byte[].class);
    }
  }

  protected URI getUri() {
    return uri;
  }

  public String name() {
    return uri.toString();
  }

  @Override
  public String toString() {
    return name();
  }

  @Override
  public InputStream getStream() {
    try {
      return uri.toURL().openStream();
    } catch (IOException e) {
      return throwUnchecked(e, InputStream.class);
    }
  }
}
