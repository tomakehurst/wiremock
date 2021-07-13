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

import static com.github.tomakehurst.wiremock.http.Response.response;
import static com.google.common.base.MoreObjects.firstNonNull;

import com.github.tomakehurst.wiremock.common.BinaryFile;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.global.GlobalSettingsHolder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.util.List;

public class StubResponseRenderer implements ResponseRenderer {

  private final FileSource fileSource;
  private final GlobalSettingsHolder globalSettingsHolder;
  private final ProxyResponseRenderer proxyResponseRenderer;
  private final List<ResponseTransformer> responseTransformers;

  public StubResponseRenderer(
      FileSource fileSource,
      GlobalSettingsHolder globalSettingsHolder,
      ProxyResponseRenderer proxyResponseRenderer,
      List<ResponseTransformer> responseTransformers) {
    this.fileSource = fileSource;
    this.globalSettingsHolder = globalSettingsHolder;
    this.proxyResponseRenderer = proxyResponseRenderer;
    this.responseTransformers = responseTransformers;
  }

  @Override
  public Response render(ServeEvent serveEvent) {
    ResponseDefinition responseDefinition = serveEvent.getResponseDefinition();
    if (!responseDefinition.wasConfigured()) {
      return Response.notConfigured();
    }

    Response response = buildResponse(serveEvent);
    return applyTransformations(
        responseDefinition.getOriginalRequest(),
        responseDefinition,
        response,
        responseTransformers);
  }

  private Response buildResponse(ServeEvent serveEvent) {
    if (serveEvent.getResponseDefinition().isProxyResponse()) {
      return proxyResponseRenderer.render(serveEvent);
    } else {
      Response.Builder responseBuilder = renderDirectly(serveEvent);
      return responseBuilder.build();
    }
  }

  private Response applyTransformations(
      Request request,
      ResponseDefinition responseDefinition,
      Response response,
      List<ResponseTransformer> transformers) {
    if (transformers.isEmpty()) {
      return response;
    }

    ResponseTransformer transformer = transformers.get(0);
    Response newResponse =
        transformer.applyGlobally() || responseDefinition.hasTransformer(transformer)
            ? transformer.transform(
                request, response, fileSource, responseDefinition.getTransformerParameters())
            : response;

    return applyTransformations(
        request, responseDefinition, newResponse, transformers.subList(1, transformers.size()));
  }

  private Response.Builder renderDirectly(ServeEvent serveEvent) {
    ResponseDefinition responseDefinition = serveEvent.getResponseDefinition();

    HttpHeaders headers = responseDefinition.getHeaders();
    StubMapping stubMapping = serveEvent.getStubMapping();
    if (serveEvent.getWasMatched() && stubMapping != null) {
      headers =
          firstNonNull(headers, new HttpHeaders())
              .plus(new HttpHeader("Matched-Stub-Id", stubMapping.getId().toString()));

      if (stubMapping.getName() != null) {
        headers = headers.plus(new HttpHeader("Matched-Stub-Name", stubMapping.getName()));
      }
    }

    Response.Builder responseBuilder =
        response()
            .status(responseDefinition.getStatus())
            .statusMessage(responseDefinition.getStatusMessage())
            .headers(headers)
            .fault(responseDefinition.getFault())
            .configureDelay(
                globalSettingsHolder.get().getFixedDelay(),
                globalSettingsHolder.get().getDelayDistribution(),
                responseDefinition.getFixedDelayMilliseconds(),
                responseDefinition.getDelayDistribution())
            .chunkedDribbleDelay(responseDefinition.getChunkedDribbleDelay());

    if (responseDefinition.specifiesBodyFile()) {
      BinaryFile bodyFile = fileSource.getBinaryFileNamed(responseDefinition.getBodyFileName());
      responseBuilder.body(bodyFile);
    } else if (responseDefinition.specifiesBodyContent()) {
      if (responseDefinition.specifiesBinaryBodyContent()) {
        responseBuilder.body(responseDefinition.getByteBody());
      } else {
        responseBuilder.body(responseDefinition.getByteBody());
      }
    }

    return responseBuilder;
  }
}
