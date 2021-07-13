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

import static com.google.common.net.MediaType.OCTET_STREAM;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.common.Encoding;
import com.github.tomakehurst.wiremock.common.Strings;
import java.nio.charset.Charset;

public class LoggedResponse {

  private final int status;
  private final HttpHeaders headers;
  private final byte[] body;
  private final Fault fault;

  public LoggedResponse(
      @JsonProperty("status") int status,
      @JsonProperty("headers") HttpHeaders headers,
      @JsonProperty("bodyAsBase64") String bodyAsBase64,
      @JsonProperty("fault") Fault fault,
      @JsonProperty("body") String ignoredBodyOnlyUsedForBinding) {
    this(status, headers, Encoding.decodeBase64(bodyAsBase64), fault);
  }

  private LoggedResponse(int status, HttpHeaders headers, byte[] body, Fault fault) {
    this.status = status;
    this.headers = headers;
    this.body = body;
    this.fault = fault;
  }

  public static LoggedResponse from(Response response) {
    return new LoggedResponse(
        response.getStatus(),
        response.getHeaders() == null || response.getHeaders().all().isEmpty()
            ? null
            : response.getHeaders(),
        response.getBody(),
        response.getFault());
  }

  public int getStatus() {
    return status;
  }

  public HttpHeaders getHeaders() {
    return headers;
  }

  /**
   * Retrieve body as a String encoded in the charset in the "Content-Type" header, or, if that's
   * not present, the default character set (UTF-8)
   *
   * @return Encoded string
   */
  @JsonProperty("body")
  public String getBodyAsString() {
    if (body == null) {
      return "";
    }

    return Strings.stringFromBytes(body, getCharset());
  }

  @JsonIgnore
  public String getMimeType() {
    return headers == null || headers.getContentTypeHeader() == null
        ? OCTET_STREAM.toString()
        : headers.getContentTypeHeader().mimeTypePart();
  }

  @JsonIgnore
  public Charset getCharset() {
    return headers == null ? Strings.DEFAULT_CHARSET : headers.getContentTypeHeader().charset();
  }

  @JsonIgnore
  public byte[] getBody() {
    return body;
  }

  @JsonProperty("bodyAsBase64")
  public String getBodyAsBase64() {
    return Encoding.encodeBase64(body);
  }

  public Fault getFault() {
    return fault;
  }
}
