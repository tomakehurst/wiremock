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
package com.github.tomakehurst.wiremock.verification.diff;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.verification.diff.SpacerLine.SPACER;
import static com.google.common.base.MoreObjects.firstNonNull;

import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.common.Urls;
import com.github.tomakehurst.wiremock.common.xml.Xml;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.matching.*;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Diff {

  private final String stubMappingName;
  private final RequestPattern requestPattern;
  private final Request request;
  private final String scenarioName;
  private final String scenarioState;
  private final String expectedScenarioState;

  public Diff(RequestPattern expected, Request actual) {
    this.requestPattern = expected;
    this.request = actual;
    this.stubMappingName = null;
    this.scenarioName = null;
    this.scenarioState = null;
    this.expectedScenarioState = null;
  }

  public Diff(StubMapping expected, Request actual) {
    this(expected, actual, null);
  }

  public Diff(StubMapping expected, Request actual, String scenarioState) {
    this.requestPattern = expected.getRequest();
    this.request = actual;
    this.stubMappingName = expected.getName();
    this.scenarioName = expected.getScenarioName();
    this.scenarioState = scenarioState;
    this.expectedScenarioState = expected.getRequiredScenarioState();
  }

  @Override
  public String toString() {
    return new JUnitStyleDiffRenderer().render(this);
  }

  public List<DiffLine<?>> getLines() {
    return getLines(Collections.<String, RequestMatcherExtension>emptyMap());
  }

  public List<DiffLine<?>> getLines(Map<String, RequestMatcherExtension> customMatcherExtensions) {
    ImmutableList.Builder<DiffLine<?>> builder = ImmutableList.builder();

    if (requestPattern.getHost() != null) {
      String hostOperator = generateOperatorString(requestPattern.getHost(), "");
      String printedHostPatternValue = hostOperator + requestPattern.getHost().getExpected();
      DiffLine<String> hostSection =
          new DiffLine<>(
              "Host", requestPattern.getHost(), request.getHost(), printedHostPatternValue.trim());
      builder.add(hostSection);
    }

    if (requestPattern.getPort() != null) {
      StringValuePattern expectedPort = equalTo(String.valueOf(requestPattern.getPort()));
      String actualPort = String.valueOf(request.getPort());
      DiffLine<String> portSection =
          new DiffLine<>("Port", expectedPort, actualPort, expectedPort.getExpected());
      builder.add(portSection);
    }

    if (requestPattern.getScheme() != null) {
      StringValuePattern expectedScheme = equalTo(String.valueOf(requestPattern.getScheme()));
      DiffLine<String> schemeSection =
          new DiffLine<>("Scheme", expectedScheme, request.getScheme(), requestPattern.getScheme());
      builder.add(schemeSection);
    }

    DiffLine<RequestMethod> methodSection =
        new DiffLine<>(
            "HTTP method",
            requestPattern.getMethod(),
            request.getMethod(),
            requestPattern.getMethod().getName());
    builder.add(methodSection);

    UrlPattern urlPattern = firstNonNull(requestPattern.getUrlMatcher(), anyUrl());
    String printedUrlPattern = generatePrintedUrlPattern(urlPattern);
    DiffLine<String> urlSection =
        new DiffLine<>("URL", urlPattern, request.getUrl(), printedUrlPattern);
    builder.add(urlSection);

    builder.add(SPACER);

    addHeaderSection(
        requestPattern.combineBasicAuthAndOtherHeaders(), request.getHeaders(), builder);

    boolean anyQueryParams = false;
    if (requestPattern.getQueryParameters() != null) {
      Map<String, QueryParameter> requestQueryParams =
          Urls.splitQuery(URI.create(request.getUrl()));

      for (Map.Entry<String, MultiValuePattern> entry :
          requestPattern.getQueryParameters().entrySet()) {
        String key = entry.getKey();
        MultiValuePattern pattern = entry.getValue();
        QueryParameter queryParameter =
            firstNonNull(requestQueryParams.get(key), QueryParameter.absent(key));

        String operator = generateOperatorString(pattern.getValuePattern(), " = ");
        DiffLine<MultiValue> section =
            new DiffLine<>(
                "Query",
                pattern,
                queryParameter,
                "Query: " + key + operator + pattern.getValuePattern().getValue());
        builder.add(section);
        anyQueryParams = true;
      }
    }

    if (anyQueryParams) {
      builder.add(SPACER);
    }

    boolean anyCookieSections = false;
    if (requestPattern.getCookies() != null) {
      Map<String, Cookie> cookies =
          firstNonNull(request.getCookies(), Collections.<String, Cookie>emptyMap());
      for (Map.Entry<String, StringValuePattern> entry : requestPattern.getCookies().entrySet()) {
        String key = entry.getKey();
        StringValuePattern pattern = entry.getValue();
        Cookie cookie = firstNonNull(cookies.get(key), Cookie.absent());

        String operator = generateOperatorString(pattern, "=");
        DiffLine<String> section =
            new DiffLine<>(
                "Cookie",
                pattern,
                cookie.isPresent() ? cookie.getValue() : "",
                "Cookie: " + key + operator + pattern.getValue());
        builder.add(section);
        anyCookieSections = true;
      }
    }

    if (anyCookieSections) {
      builder.add(SPACER);
    }

    List<ContentPattern<?>> bodyPatterns = requestPattern.getBodyPatterns();
    addBodySection(bodyPatterns, new Body(request.getBody()), builder);

    List<MultipartValuePattern> multipartPatterns = requestPattern.getMultipartPatterns();
    if (multipartPatterns != null && !multipartPatterns.isEmpty()) {

      for (MultipartValuePattern pattern : multipartPatterns) {
        if (!request.isMultipart()) {
          builder.add(new SectionDelimiter("[Multipart request body]", ""));
        } else if (!pattern.match(request).isExactMatch()) {
          for (Request.Part part : request.getParts()) {
            builder.add(SPACER);
            String patternPartName = pattern.getName() == null ? "" : ": " + pattern.getName();
            String partName = part.getName() == null ? "" : part.getName();
            builder.add(
                new SectionDelimiter("[Multipart" + patternPartName + "]", "[" + partName + "]"));
            builder.add(SPACER);

            if (!pattern.match(part).isExactMatch()) {
              addHeaderSection(pattern.getHeaders(), part.getHeaders(), builder);
              addBodySection(pattern.getBodyPatterns(), part.getBody(), builder);
              builder.add(SPACER);
            }

            builder.add(new SectionDelimiter("[/Multipart]", "[/" + partName + "]"));
            builder.add(SPACER);
          }
        }
      }
    }

    if (requestPattern.hasInlineCustomMatcher()) {
      InlineCustomMatcherLine customMatcherLine =
          new InlineCustomMatcherLine(requestPattern.getMatcher(), request);
      builder.add(customMatcherLine);
    }

    if (requestPattern.hasNamedCustomMatcher()) {
      RequestMatcherExtension customMatcher =
          customMatcherExtensions.get(requestPattern.getCustomMatcher().getName());
      if (customMatcher != null) {
        NamedCustomMatcherLine namedCustomMatcherLine =
            new NamedCustomMatcherLine(
                customMatcher, requestPattern.getCustomMatcher().getParameters(), request);
        builder.add(namedCustomMatcherLine);
      } else {
        builder.add(
            new SectionDelimiter(
                "[custom matcher: " + requestPattern.getCustomMatcher().getName() + "]"));
      }
    }

    if (scenarioName != null) {
      builder.add(
          new DiffLine<>(
              "Scenario",
              new EqualToPattern(expectedScenarioState),
              buildScenarioLine(scenarioName, scenarioState),
              buildScenarioLine(scenarioName, expectedScenarioState)));
    }

    return builder.build();
  }

  private static String buildScenarioLine(String scenarioName, String scenarioState) {
    return "[Scenario '" + scenarioName + "' state: " + scenarioState + "]";
  }

  private void addHeaderSection(
      Map<String, MultiValuePattern> headerPatterns,
      HttpHeaders headers,
      ImmutableList.Builder<DiffLine<?>> builder) {
    boolean anyHeaderSections = false;
    if (headerPatterns != null && !headerPatterns.isEmpty()) {
      anyHeaderSections = true;
      for (String key : headerPatterns.keySet()) {
        HttpHeader header = headers.getHeader(key);
        MultiValuePattern headerPattern = headerPatterns.get(header.key());

        String operator = generateOperatorString(headerPattern.getValuePattern(), "");
        String printedPatternValue = header.key() + operator + ": " + headerPattern.getExpected();

        DiffLine<MultiValue> section =
            new DiffLine<>("Header", headerPattern, header, printedPatternValue);
        builder.add(section);
      }
    }

    if (anyHeaderSections) {
      builder.add(SPACER);
    }
  }

  private void addBodySection(
      List<ContentPattern<?>> bodyPatterns, Body body, ImmutableList.Builder<DiffLine<?>> builder) {
    if (bodyPatterns != null && !bodyPatterns.isEmpty()) {
      for (ContentPattern<?> pattern : bodyPatterns) {
        String formattedBody = formatIfJsonOrXml(pattern, body);
        if (PathPattern.class.isAssignableFrom(pattern.getClass())) {
          PathPattern pathPattern = (PathPattern) pattern;
          if (!pathPattern.isSimple()) {
            String expressionResult = pathPattern.getExpressionResult(body.asString());
            String printedExpectedValue =
                pathPattern.getExpected()
                    + " ["
                    + pathPattern.getValuePattern().getName()
                    + "] "
                    + pathPattern.getValuePattern().getExpected();
            if (expressionResult != null) {
              builder.add(
                  new DiffLine<>(
                      "Body",
                      pathPattern.getValuePattern(),
                      expressionResult,
                      printedExpectedValue));
            } else {
              builder.add(new DiffLine<>("Body", pathPattern, formattedBody, printedExpectedValue));
            }
          } else {
            builder.add(new DiffLine<>("Body", pathPattern, formattedBody, pattern.getExpected()));
          }
        } else if (StringValuePattern.class.isAssignableFrom(pattern.getClass())) {
          StringValuePattern stringValuePattern = (StringValuePattern) pattern;
          builder.add(
              new DiffLine<>("Body", stringValuePattern, formattedBody, pattern.getExpected()));
        } else {
          BinaryEqualToPattern nonStringPattern = (BinaryEqualToPattern) pattern;
          builder.add(
              new DiffLine<>(
                  "Body", nonStringPattern, formattedBody.getBytes(), pattern.getExpected()));
        }
      }
    }
  }

  private String generatePrintedUrlPattern(UrlPattern urlPattern) {
    String matchPart =
        (urlPattern instanceof UrlPathPattern ? "path" : "")
            + (urlPattern.isRegex() ? " regex" : "");

    matchPart = matchPart.trim();

    return matchPart.isEmpty()
        ? urlPattern.getExpected()
        : "[" + matchPart + "] " + urlPattern.getExpected();
  }

  private String generateOperatorString(ContentPattern<?> pattern, String defaultValue) {
    return isAnEqualToPattern(pattern) ? defaultValue : " [" + pattern.getName() + "] ";
  }

  public String getStubMappingName() {
    return stubMappingName;
  }

  private static String formatIfJsonOrXml(ContentPattern<?> pattern, Body body) {
    if (body == null || body.isAbsent()) {
      return "";
    }

    try {
      return pattern.getClass().equals(EqualToJsonPattern.class)
          ? Json.prettyPrint(Json.write(body.asJson()))
          : pattern.getClass().equals(EqualToXmlPattern.class)
              ? Xml.prettyPrint(body.asString())
              : pattern.getClass().equals(BinaryEqualToPattern.class)
                  ? body.asBase64()
                  : body.asString();
    } catch (Exception e) {
      return body.asString();
    }
  }

  private static boolean isAnEqualToPattern(ContentPattern<?> pattern) {
    return pattern instanceof EqualToPattern
        || pattern instanceof EqualToJsonPattern
        || pattern instanceof EqualToXmlPattern
        || pattern instanceof BinaryEqualToPattern;
  }

  public boolean hasCustomMatcher() {
    return requestPattern.hasInlineCustomMatcher() || requestPattern.hasNamedCustomMatcher();
  }
}
