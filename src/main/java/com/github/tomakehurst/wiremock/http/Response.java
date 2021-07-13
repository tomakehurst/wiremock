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

import static com.github.tomakehurst.wiremock.http.HttpHeaders.noHeaders;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

import com.github.tomakehurst.wiremock.common.BinaryFile;
import com.github.tomakehurst.wiremock.common.InputStreamSource;
import com.github.tomakehurst.wiremock.common.StreamSources;
import com.github.tomakehurst.wiremock.common.Strings;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;

public class Response {

  private final int status;
  private final String statusMessage;
  private final InputStreamSource bodyStreamSource;
  private final HttpHeaders headers;
  private final boolean configured;
  private final Fault fault;
  private final boolean fromProxy;
  private final long initialDelay;
  private final ChunkedDribbleDelay chunkedDribbleDelay;

  public static Response notConfigured() {
    return new Response(
        HTTP_NOT_FOUND, null, (byte[]) null, noHeaders(), false, null, 0, null, false);
  }

  public static Builder response() {
    return new Builder();
  }

  public Response(
      int status,
      String statusMessage,
      byte[] body,
      HttpHeaders headers,
      boolean configured,
      Fault fault,
      long initialDelay,
      ChunkedDribbleDelay chunkedDribbleDelay,
      boolean fromProxy) {
    this.status = status;
    this.statusMessage = statusMessage;
    this.bodyStreamSource = StreamSources.forBytes(body);
    this.headers = headers;
    this.configured = configured;
    this.fault = fault;
    this.initialDelay = initialDelay;
    this.chunkedDribbleDelay = chunkedDribbleDelay;
    this.fromProxy = fromProxy;
  }

  public Response(
      int status,
      String statusMessage,
      InputStreamSource streamSource,
      HttpHeaders headers,
      boolean configured,
      Fault fault,
      long initialDelay,
      ChunkedDribbleDelay chunkedDribbleDelay,
      boolean fromProxy) {
    this.status = status;
    this.statusMessage = statusMessage;
    this.bodyStreamSource = streamSource;
    this.headers = headers;
    this.configured = configured;
    this.fault = fault;
    this.initialDelay = initialDelay;
    this.chunkedDribbleDelay = chunkedDribbleDelay;
    this.fromProxy = fromProxy;
  }

  public Response(
      int status,
      String statusMessage,
      String body,
      HttpHeaders headers,
      boolean configured,
      Fault fault,
      long initialDelay,
      ChunkedDribbleDelay chunkedDribbleDelay,
      boolean fromProxy) {
    this.status = status;
    this.statusMessage = statusMessage;
    this.headers = headers;
    this.bodyStreamSource = StreamSources.forString(body, headers.getContentTypeHeader().charset());
    this.configured = configured;
    this.fault = fault;
    this.initialDelay = initialDelay;
    this.chunkedDribbleDelay = chunkedDribbleDelay;
    this.fromProxy = fromProxy;
  }

  public int getStatus() {
    return status;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public byte[] getBody() {
    try (InputStream stream = bodyStreamSource == null ? null : getBodyStream()) {
      return stream == null ? null : ByteStreams.toByteArray(stream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String getBodyAsString() {
    return Strings.stringFromBytes(getBody(), headers.getContentTypeHeader().charset());
  }

  public InputStream getBodyStream() {
    return bodyStreamSource == null ? null : bodyStreamSource.getStream();
  }

  public boolean hasInlineBody() {
    return !BinaryFile.class.isAssignableFrom(bodyStreamSource.getClass());
  }

  public HttpHeaders getHeaders() {
    return headers;
  }

  public Fault getFault() {
    return fault;
  }

  public long getInitialDelay() {
    return initialDelay;
  }

  public ChunkedDribbleDelay getChunkedDribbleDelay() {
    return chunkedDribbleDelay;
  }

  public boolean shouldAddChunkedDribbleDelay() {
    return chunkedDribbleDelay != null;
  }

  public boolean wasConfigured() {
    return configured;
  }

  public boolean isFromProxy() {
    return fromProxy;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("HTTP/1.1 ").append(status).append("\n");
    sb.append(headers).append("\n");
    // no longer printing body
    return sb.toString();
  }

  public static class Builder {
    private int status = HTTP_OK;
    private String statusMessage;
    private byte[] bodyBytes;
    private String bodyString;
    private InputStreamSource bodyStream;
    private HttpHeaders headers = new HttpHeaders();
    private boolean configured = true;
    private Fault fault;
    private boolean fromProxy;
    private long initialDelay;
    private ChunkedDribbleDelay chunkedDribbleDelay;

    public static Builder like(Response response) {
      Builder responseBuilder = new Builder();
      responseBuilder.status = response.getStatus();
      responseBuilder.statusMessage = response.getStatusMessage();
      responseBuilder.bodyStream = response.bodyStreamSource;
      responseBuilder.headers = response.getHeaders();
      responseBuilder.configured = response.wasConfigured();
      responseBuilder.fault = response.getFault();
      responseBuilder.initialDelay = response.getInitialDelay();
      responseBuilder.chunkedDribbleDelay = response.getChunkedDribbleDelay();
      responseBuilder.fromProxy = response.isFromProxy();
      return responseBuilder;
    }

    public Builder but() {
      return this;
    }

    public Builder status(int status) {
      this.status = status;
      return this;
    }

    public Builder statusMessage(String statusMessage) {
      this.statusMessage = statusMessage;
      return this;
    }

    public Builder body(byte[] body) {
      this.bodyBytes = body;
      this.bodyString = null;
      this.bodyStream = null;
      return this;
    }

    public Builder body(String body) {
      this.bodyBytes = null;
      this.bodyString = body;
      this.bodyStream = null;
      return this;
    }

    public Builder body(InputStreamSource bodySource) {
      this.bodyBytes = null;
      this.bodyString = null;
      this.bodyStream = bodySource;
      return this;
    }

    public Builder headers(HttpHeaders headers) {
      this.headers = headers == null ? noHeaders() : headers;
      return this;
    }

    public Builder configured(boolean configured) {
      this.configured = configured;
      return this;
    }

    public Builder fault(Fault fault) {
      this.fault = fault;
      return this;
    }

    public Builder configureDelay(
        Integer globalFixedDelay,
        DelayDistribution globalDelayDistribution,
        Integer fixedDelay,
        DelayDistribution delayDistribution) {
      addDelayIfSpecifiedGloballyOrIn(fixedDelay, globalFixedDelay);
      addRandomDelayIfSpecifiedGloballyOrIn(delayDistribution, globalDelayDistribution);
      return this;
    }

    private void addDelayIfSpecifiedGloballyOrIn(Integer fixedDelay, Integer globalFixedDelay) {
      Optional<Integer> optionalDelay =
          getDelayFromResponseOrGlobalSetting(fixedDelay, globalFixedDelay);
      if (optionalDelay.isPresent()) {
        incrementInitialDelay(optionalDelay.get());
      }
    }

    private Optional<Integer> getDelayFromResponseOrGlobalSetting(
        Integer fixedDelay, Integer globalFixedDelay) {
      Integer delay = fixedDelay != null ? fixedDelay : globalFixedDelay;

      return Optional.fromNullable(delay);
    }

    private void addRandomDelayIfSpecifiedGloballyOrIn(
        DelayDistribution localDelayDistribution, DelayDistribution globalDelayDistribution) {
      DelayDistribution delayDistribution;

      if (localDelayDistribution != null) {
        delayDistribution = localDelayDistribution;
      } else {
        delayDistribution = globalDelayDistribution;
      }

      if (delayDistribution != null) {
        incrementInitialDelay(delayDistribution.sampleMillis());
      }
    }

    public Builder incrementInitialDelay(long amountMillis) {
      this.initialDelay += amountMillis;
      return this;
    }

    public Builder chunkedDribbleDelay(ChunkedDribbleDelay chunkedDribbleDelay) {
      this.chunkedDribbleDelay = chunkedDribbleDelay;
      return this;
    }

    public Builder fromProxy(boolean fromProxy) {
      this.fromProxy = fromProxy;
      return this;
    }

    public Response build() {
      if (bodyBytes != null) {
        return new Response(
            status,
            statusMessage,
            bodyBytes,
            headers,
            configured,
            fault,
            initialDelay,
            chunkedDribbleDelay,
            fromProxy);
      } else if (bodyString != null) {
        return new Response(
            status,
            statusMessage,
            bodyString,
            headers,
            configured,
            fault,
            initialDelay,
            chunkedDribbleDelay,
            fromProxy);
      } else if (bodyStream != null) {
        return new Response(
            status,
            statusMessage,
            bodyStream,
            headers,
            configured,
            fault,
            initialDelay,
            chunkedDribbleDelay,
            fromProxy);
      } else {
        return new Response(
            status,
            statusMessage,
            new byte[0],
            headers,
            configured,
            fault,
            initialDelay,
            chunkedDribbleDelay,
            fromProxy);
      }
    }
  }
}
