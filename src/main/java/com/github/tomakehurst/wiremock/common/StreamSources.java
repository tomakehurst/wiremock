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

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;

public class StreamSources {
  private StreamSources() {}

  public static InputStreamSource forString(final String string, final Charset charset) {
    return new InputStreamSource() {
      @Override
      public InputStream getStream() {
        return string == null
            ? null
            : new ByteArrayInputStream(Strings.bytesFromString(string, charset));
      }
    };
  }

  public static InputStreamSource forBytes(final byte[] bytes) {
    return new InputStreamSource() {
      @Override
      public InputStream getStream() {
        return bytes == null ? null : new ByteArrayInputStream(bytes);
      }
    };
  }

  public static InputStreamSource forURI(final URI uri) {
    return new InputStreamSource() {
      @Override
      public InputStream getStream() {
        try {
          return uri == null ? null : new BufferedInputStream(uri.toURL().openStream());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }
}
