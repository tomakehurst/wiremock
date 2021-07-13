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
import static com.github.tomakehurst.wiremock.common.Strings.DEFAULT_CHARSET;
import static com.github.tomakehurst.wiremock.common.Strings.bytesFromString;

import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Gzip {

  public static byte[] unGzip(byte[] gzippedContent) {
    if (gzippedContent.length == 0) {
      return new byte[0];
    }

    try {
      GZIPInputStream gzipInputStream =
          new GZIPInputStream(new ByteArrayInputStream(gzippedContent));
      return ByteStreams.toByteArray(gzipInputStream);
    } catch (IOException e) {
      return throwUnchecked(e, byte[].class);
    }
  }

  public static String unGzipToString(byte[] gzippedContent) {
    return new String(unGzip(gzippedContent));
  }

  public static byte[] gzip(String plainContent) {
    return gzip(plainContent, DEFAULT_CHARSET);
  }

  public static byte[] gzip(String plainContent, Charset charset) {
    return gzip(bytesFromString(plainContent, charset));
  }

  public static byte[] gzip(byte[] plainContent) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bytes);
      gzipOutputStream.write(plainContent);
      gzipOutputStream.close();
      return bytes.toByteArray();
    } catch (IOException e) {
      return throwUnchecked(e, byte[].class);
    }
  }

  public static boolean isGzipped(byte[] content) {
    return content.length >= 2
        && content[0] == (byte) GZIPInputStream.GZIP_MAGIC
        && content[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8);
  }
}
