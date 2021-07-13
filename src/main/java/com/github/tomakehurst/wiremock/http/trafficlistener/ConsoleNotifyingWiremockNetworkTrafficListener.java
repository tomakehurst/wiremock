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

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class ConsoleNotifyingWiremockNetworkTrafficListener
    implements WiremockNetworkTrafficListener {
  private static final ConsoleNotifier CONSOLE_NOTIFIER = new ConsoleNotifier(true);

  private final Charset charset = Charset.forName("UTF-8");
  private final CharsetDecoder decoder = charset.newDecoder();

  @Override
  public void opened(Socket socket) {
    CONSOLE_NOTIFIER.info("Opened " + socket);
  }

  @Override
  public void incoming(Socket socket, ByteBuffer bytes) {
    try {
      CONSOLE_NOTIFIER.info("Incoming bytes: " + decoder.decode(bytes));
    } catch (CharacterCodingException e) {
      CONSOLE_NOTIFIER.error("Problem decoding network traffic", e);
    }
  }

  @Override
  public void outgoing(Socket socket, ByteBuffer bytes) {
    try {
      CONSOLE_NOTIFIER.info("Outgoing bytes: " + decoder.decode(bytes));
    } catch (CharacterCodingException e) {
      CONSOLE_NOTIFIER.error("Problem decoding network traffic", e);
    }
  }

  @Override
  public void closed(Socket socket) {
    CONSOLE_NOTIFIER.info("Closed " + socket);
  }
}
