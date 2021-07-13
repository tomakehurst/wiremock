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
package com.github.tomakehurst.wiremock.http.trafficlistener;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class CollectingNetworkTrafficListener implements WiremockNetworkTrafficListener {
  private final StringBuilder requestBuilder = new StringBuilder();
  private final StringBuilder responseBuilder = new StringBuilder();

  private final Charset charset = Charset.forName("UTF-8");
  private final CharsetDecoder decoder = charset.newDecoder();

  @Override
  public void opened(Socket socket) {}

  @Override
  public void incoming(Socket socket, ByteBuffer bytes) {
    try {
      requestBuilder.append(decoder.decode(bytes));
    } catch (CharacterCodingException e) {
      notifier().error("Problem decoding network traffic", e);
    }
  }

  @Override
  public void outgoing(Socket socket, ByteBuffer bytes) {
    try {
      responseBuilder.append(decoder.decode(bytes));
    } catch (CharacterCodingException e) {
      notifier().error("Problem decoding network traffic", e);
    }
  }

  @Override
  public void closed(Socket socket) {}

  public String getAllRequests() {
    return requestBuilder.toString();
  }

  public String getAllResponses() {
    return responseBuilder.toString();
  }
}
