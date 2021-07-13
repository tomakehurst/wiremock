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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jNotifier implements Notifier {

  private static final Logger log = LoggerFactory.getLogger("WireMock");

  private final boolean verbose;

  public Slf4jNotifier(boolean verbose) {
    this.verbose = verbose;
  }

  @Override
  public void info(String message) {
    if (verbose) {
      log.info(message);
    }
  }

  @Override
  public void error(String message) {
    log.error(message);
  }

  @Override
  public void error(String message, Throwable t) {
    log.error(message, t);
  }
}
