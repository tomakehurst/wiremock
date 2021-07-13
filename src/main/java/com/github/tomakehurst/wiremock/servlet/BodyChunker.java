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
package com.github.tomakehurst.wiremock.servlet;

import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import java.util.Arrays;

public class BodyChunker {

  private static final Notifier notifier = new Slf4jNotifier(false);

  public static byte[][] chunkBody(byte[] body, int numberOfChunks) {

    if (numberOfChunks < 1) {
      notifier.error("Number of chunks set to value less than 1: " + numberOfChunks);
      numberOfChunks = 1;
    }

    if (body.length < numberOfChunks) {
      notifier.error(
          "Number of chunks set to value greater then body length. Number of chunks: "
              + numberOfChunks
              + ". Body length: "
              + body.length
              + ". Overriding number of chunks to body length.");
      numberOfChunks = body.length;
    }

    int chunkSize = body.length / numberOfChunks;
    int excessSize = body.length % numberOfChunks;

    byte[][] chunkedBody = new byte[numberOfChunks][];

    for (int chunkIndex = 0; chunkIndex < numberOfChunks; chunkIndex++) {
      int chunkStart = chunkIndex * chunkSize;
      int chunkEnd = chunkStart + chunkSize;

      chunkedBody[chunkIndex] = Arrays.copyOfRange(body, chunkStart, chunkEnd);
    }

    if (excessSize > 0) {
      int lastChunkIndex = numberOfChunks - 1;

      int chunkStart = lastChunkIndex * chunkSize;
      int newChunkEnd = chunkStart + chunkSize + excessSize;

      chunkedBody[lastChunkIndex] = Arrays.copyOfRange(body, chunkStart, newChunkEnd);
    }

    return chunkedBody;
  }
}
