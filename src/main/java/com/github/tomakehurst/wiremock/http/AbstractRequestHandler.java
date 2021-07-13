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
package com.github.tomakehurst.wiremock.http;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static com.github.tomakehurst.wiremock.extension.requestfilter.FilterProcessor.processFilters;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.github.tomakehurst.wiremock.extension.requestfilter.*;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.base.Stopwatch;
import java.util.List;

public abstract class AbstractRequestHandler implements RequestHandler, RequestEventSource {

  protected List<RequestListener> listeners = newArrayList();
  protected final ResponseRenderer responseRenderer;
  protected final List<RequestFilter> requestFilters;

  public AbstractRequestHandler(
      ResponseRenderer responseRenderer, List<RequestFilter> requestFilters) {
    this.responseRenderer = responseRenderer;
    this.requestFilters = requestFilters;
  }

  @Override
  public void addRequestListener(RequestListener requestListener) {
    listeners.add(requestListener);
  }

  protected void beforeResponseSent(ServeEvent serveEvent, Response response) {}

  protected void afterResponseSent(ServeEvent serveEvent, Response response) {}

  @Override
  public void handle(Request request, HttpResponder httpResponder) {
    Stopwatch stopwatch = Stopwatch.createStarted();

    ServeEvent serveEvent;
    Request processedRequest = request;
    if (!requestFilters.isEmpty()) {
      RequestFilterAction requestFilterAction =
          processFilters(request, requestFilters, RequestFilterAction.continueWith(request));
      if (requestFilterAction instanceof ContinueAction) {
        processedRequest = ((ContinueAction) requestFilterAction).getRequest();
        serveEvent = handleRequest(processedRequest);
      } else {
        serveEvent =
            ServeEvent.of(
                LoggedRequest.createFrom(request),
                ((StopAction) requestFilterAction).getResponseDefinition());
      }
    } else {
      serveEvent = handleRequest(request);
    }

    ResponseDefinition responseDefinition = serveEvent.getResponseDefinition();
    responseDefinition.setOriginalRequest(processedRequest);
    Response response = responseRenderer.render(serveEvent);
    ServeEvent completedServeEvent =
        serveEvent.complete(response, (int) stopwatch.elapsed(MILLISECONDS));

    if (logRequests()) {
      notifier()
          .info(
              "Request received:\n"
                  + formatRequest(processedRequest)
                  + "\n\nMatched response definition:\n"
                  + responseDefinition
                  + "\n\nResponse:\n"
                  + response);
    }

    for (RequestListener listener : listeners) {
      listener.requestReceived(processedRequest, response);
    }

    beforeResponseSent(completedServeEvent, response);

    stopwatch.reset();
    stopwatch.start();
    httpResponder.respond(processedRequest, response);

    completedServeEvent.afterSend((int) stopwatch.elapsed(MILLISECONDS));
    afterResponseSent(completedServeEvent, response);
    stopwatch.stop();
  }

  protected String formatRequest(Request request) {
    StringBuilder sb = new StringBuilder();
    sb.append(request.getClientIp())
        .append(" - ")
        .append(request.getMethod())
        .append(" ")
        .append(request.getUrl());

    if (request.isBrowserProxyRequest()) {
      sb.append(" (via browser proxy request)");
    }

    sb.append("\n\n");
    sb.append(request.getHeaders());

    if (request.getBody() != null) {
      sb.append(request.getBodyAsString()).append("\n");
    }

    return sb.toString();
  }

  protected boolean logRequests() {
    return false;
  }

  protected abstract ServeEvent handleRequest(Request request);
}
