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
package com.github.tomakehurst.wiremock.stubbing;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.common.Json.write;
import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static com.google.common.collect.Iterables.filter;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.*;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.matching.*;
import com.github.tomakehurst.wiremock.verification.VerificationResult;
import com.google.common.base.Predicate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class StubMappingJsonRecorder implements RequestListener {

  private final FileSource mappingsFileSource;
  private final FileSource filesFileSource;
  private final Admin admin;
  private final List<CaseInsensitiveKey> headersToMatch;
  private IdGenerator idGenerator;

  public StubMappingJsonRecorder(
      FileSource mappingsFileSource,
      FileSource filesFileSource,
      Admin admin,
      List<CaseInsensitiveKey> headersToMatch) {
    this.mappingsFileSource = mappingsFileSource;
    this.filesFileSource = filesFileSource;
    this.admin = admin;
    this.headersToMatch = headersToMatch;
    idGenerator = new VeryShortIdGenerator();
  }

  @Override
  public void requestReceived(Request request, Response response) {
    RequestPattern requestPattern = buildRequestPatternFrom(request);

    if (requestNotAlreadyReceived(requestPattern) && response.isFromProxy()) {
      notifier().info(String.format("Recording mappings for %s", request.getUrl()));
      writeToMappingAndBodyFile(request, response, requestPattern);
    } else {
      notifier()
          .info(
              String.format(
                  "Not recording mapping for %s as this has already been received",
                  request.getUrl()));
    }
  }

  private RequestPattern buildRequestPatternFrom(Request request) {
    RequestPatternBuilder builder =
        newRequestPattern(request.getMethod(), urlEqualTo(request.getUrl()));

    if (!headersToMatch.isEmpty()) {
      for (HttpHeader header : request.getHeaders().all()) {
        if (headersToMatch.contains(header.caseInsensitiveKey())) {
          builder.withHeader(header.key(), equalTo(header.firstValue()));
        }
      }
    }

    if (request.isMultipart()) {
      for (Request.Part part : request.getParts()) {
        builder.withRequestBodyPart(valuePatternForPart(part));
      }
    } else {
      String body = request.getBodyAsString();
      if (!body.isEmpty()) {
        builder.withRequestBody(valuePatternForContentType(request));
      }
    }

    return builder.build();
  }

  private MultipartValuePattern valuePatternForPart(Request.Part part) {
    MultipartValuePatternBuilder builder =
        new MultipartValuePatternBuilder()
            .withName(part.getName())
            .matchingType(MultipartValuePattern.MatchingType.ALL);

    if (!headersToMatch.isEmpty()) {
      Collection<HttpHeader> all = part.getHeaders().all();

      for (HttpHeader httpHeader : all) {
        if (headersToMatch.contains(httpHeader.caseInsensitiveKey())) {
          builder.withHeader(httpHeader.key(), equalTo(httpHeader.firstValue()));
        }
      }
    }

    HttpHeader contentType = part.getHeader("Content-Type");

    if (!contentType.isPresent() || contentType.firstValue().contains("text")) {
      builder.withBody(equalTo(part.getBody().asString()));
    } else if (contentType.firstValue().contains("json")) {
      builder.withBody(equalToJson(part.getBody().asString(), true, true));
    } else if (contentType.firstValue().contains("xml")) {
      builder.withBody(equalToXml(part.getBody().asString()));
    } else {
      builder.withBody(binaryEqualTo(part.getBody().asBytes()));
    }

    return builder.build();
  }

  private StringValuePattern valuePatternForContentType(Request request) {
    String contentType = request.getHeader("Content-Type");
    if (contentType != null) {
      if (contentType.contains("json")) {
        return equalToJson(request.getBodyAsString(), true, true);
      } else if (contentType.contains("xml")) {
        return equalToXml(request.getBodyAsString());
      }
    }

    return equalTo(request.getBodyAsString());
  }

  private void writeToMappingAndBodyFile(
      Request request, Response response, RequestPattern requestPattern) {
    String fileId = idGenerator.generate();
    byte[] body = bodyDecompressedIfRequired(response);

    String mappingFileName = UniqueFilenameGenerator.generate(request.getUrl(), "mapping", fileId);
    String bodyFileName =
        UniqueFilenameGenerator.generate(
            request.getUrl(),
            "body",
            fileId,
            ContentTypes.determineFileExtension(
                request.getUrl(), response.getHeaders().getContentTypeHeader(), body));

    ResponseDefinitionBuilder responseDefinitionBuilder =
        responseDefinition().withStatus(response.getStatus()).withBodyFile(bodyFileName);
    if (response.getHeaders().size() > 0) {
      responseDefinitionBuilder.withHeaders(
          withoutContentEncodingAndContentLength(response.getHeaders()));
    }

    ResponseDefinition responseToWrite = responseDefinitionBuilder.build();

    StubMapping mapping = new StubMapping(requestPattern, responseToWrite);
    mapping.setUuid(UUID.nameUUIDFromBytes(fileId.getBytes()));

    filesFileSource.writeBinaryFile(bodyFileName, body);
    mappingsFileSource.writeTextFile(mappingFileName, write(mapping));
  }

  private HttpHeaders withoutContentEncodingAndContentLength(HttpHeaders httpHeaders) {
    return new HttpHeaders(
        filter(
            httpHeaders.all(),
            new Predicate<HttpHeader>() {
              public boolean apply(HttpHeader header) {
                return !header.keyEquals("Content-Encoding") && !header.keyEquals("Content-Length");
              }
            }));
  }

  private byte[] bodyDecompressedIfRequired(Response response) {
    if (response.getHeaders().getHeader("Content-Encoding").containsValue("gzip")) {
      return Gzip.unGzip(response.getBody());
    }

    return response.getBody();
  }

  private boolean requestNotAlreadyReceived(RequestPattern requestPattern) {
    VerificationResult verificationResult = admin.countRequestsMatching(requestPattern);
    verificationResult.assertRequestJournalEnabled();
    return (verificationResult.getCount() < 1);
  }

  public void setIdGenerator(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }
}
