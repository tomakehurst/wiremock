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
package com.github.tomakehurst.wiremock.jetty9;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;

import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Dispatcher;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;

public class NotFoundHandler extends ErrorHandler {

  private final ErrorHandler DEFAULT_HANDLER = new ErrorHandler();

  private final ContextHandler mockServiceHandler;

  public NotFoundHandler(ContextHandler mockServiceHandler) {
    this.mockServiceHandler = mockServiceHandler;
  }

  @Override
  public boolean errorPageForMethod(String method) {
    return true;
  }

  @Override
  public void handle(
      String target,
      final Request baseRequest,
      final HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {
    if (response.getStatus() == 404) {

      ServletContext adminContext = mockServiceHandler.getServletContext().getContext("/__admin");
      Dispatcher requestDispatcher = (Dispatcher) adminContext.getRequestDispatcher("/not-matched");

      try {
        requestDispatcher.error(request, response);
      } catch (ServletException e) {
        throwUnchecked(e);
      }
    } else {
      try {
        DEFAULT_HANDLER.handle(target, baseRequest, request, response);
      } catch (Exception e) {
        if (e instanceof IOException) {
          throw (IOException) e;
        }

        throwUnchecked(e);
      }
    }
  }
}
