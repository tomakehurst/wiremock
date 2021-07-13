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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;

public class BodyChunkerTest {

  @Test
  public void returnsBodyAsSingleChunkWhenChunkSizeIsOne() {
    byte[] body = "1234".getBytes();
    int numberOfChunks = 1;

    byte[][] chunkedBody = BodyChunker.chunkBody(body, numberOfChunks);

    assertThat(chunkedBody, arrayWithSize(numberOfChunks));
    assertThat(chunkedBody[0], equalTo(body));
  }

  @Test
  public void returnsEvenlyChunkedBody() {
    byte[] body = "1234".getBytes();
    int numberOfChunks = 2;

    byte[][] chunkedBody = BodyChunker.chunkBody(body, numberOfChunks);

    assertThat(chunkedBody, arrayWithSize(numberOfChunks));
    assertThat(chunkedBody[0], equalTo("12".getBytes()));
    assertThat(chunkedBody[1], equalTo("34".getBytes()));
  }

  @Test
  public void addsExcessBytesToLastChunk() {
    byte[] body = "1234".getBytes();
    int numberOfChunks = 3;

    byte[][] chunkedBody = BodyChunker.chunkBody(body, numberOfChunks);

    assertThat(chunkedBody, arrayWithSize(numberOfChunks));
    assertThat(chunkedBody[0], equalTo("1".getBytes()));
    assertThat(chunkedBody[1], equalTo("2".getBytes()));
    assertThat(chunkedBody[2], equalTo("34".getBytes()));
  }

  @Test
  public void defaultsChunkSizeToOneIfNumberOfChunksGreaterThenBodyLength() {
    byte[] body = "1234".getBytes();
    int numberOfChunks = 5;

    byte[][] chunkedBody = BodyChunker.chunkBody(body, numberOfChunks);

    assertThat(chunkedBody, arrayWithSize(body.length));
    assertThat(chunkedBody[0], equalTo("1".getBytes()));
    assertThat(chunkedBody[1], equalTo("2".getBytes()));
    assertThat(chunkedBody[2], equalTo("3".getBytes()));
    assertThat(chunkedBody[3], equalTo("4".getBytes()));
  }

  @Test
  public void defaultsChunkSizeToOneIfNumberOfChunksLessThanOne() {
    byte[] body = "1234".getBytes();
    int numberOfChunks = -1;

    byte[][] chunkedBody = BodyChunker.chunkBody(body, numberOfChunks);

    assertThat(chunkedBody, arrayWithSize(1));
    assertThat(chunkedBody[0], equalTo(body));
  }
}
