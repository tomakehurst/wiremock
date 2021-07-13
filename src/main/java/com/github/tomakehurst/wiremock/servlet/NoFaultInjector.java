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

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;

import com.github.tomakehurst.wiremock.core.FaultInjector;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

public class NoFaultInjector implements FaultInjector {

  private final HttpServletResponse httpServletResponse;

  public NoFaultInjector(HttpServletResponse httpServletResponse) {
    this.httpServletResponse = httpServletResponse;
  }

  @Override
  public void connectionResetByPeer() {
    sendError();
  }

  @Override
  public void emptyResponseAndCloseConnection() {
    sendError();
  }

  @Override
  public void malformedResponseChunk() {
    sendError();
  }

  @Override
  public void randomDataAndCloseConnection() {
    sendError();
  }

  private void sendError() {
    httpServletResponse.setStatus(418);
    try {
      httpServletResponse.getWriter().write("No fault injector is configured!");
    } catch (IOException e) {
      throwUnchecked(e);
    }
  }
}
