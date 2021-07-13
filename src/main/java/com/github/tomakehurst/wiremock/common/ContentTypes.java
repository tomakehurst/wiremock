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

import static com.github.tomakehurst.wiremock.common.Strings.stringFromBytes;
import static com.github.tomakehurst.wiremock.common.TextType.JSON;
import static com.google.common.collect.Iterables.any;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.common.xml.Xml;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class ContentTypes {

  private static final Map<String, String> COMMON_MIME_TYPES =
      ImmutableMap.<String, String>builder()
          .put("image/jpeg", "jpeg")
          .put("image/gif", "gif")
          .put("image/tiff", "tiff")
          .put("image/png", "png")
          .put("image/x-icon", "ico")
          .put("image/svg+xml", "svg")
          .put("audio/x-aiff", "aiff")
          .put("video/x-ms-asf", "asf")
          .put("video/mpeg", "mp2")
          .put("audio/mpeg", "mp3")
          .put("video/quicktime", "mov")
          .put("application/pdf", "pdf")
          .build();

  public static final List<String> TEXT_FILE_EXTENSIONS =
      asList("txt", "json", "xml", "html", "htm", "yaml", "csv");

  public static final List<String> TEXT_MIME_TYPE_PATTERNS =
      asList(
          ".*text.*",
          ".*json.*",
          ".*xml.*",
          ".*html.*",
          ".*yaml.*",
          ".*csv.*",
          ".*x-www-form-urlencoded.*");

  public static String determineFileExtension(
      String url, ContentTypeHeader contentTypeHeader, byte[] responseBody) {
    if (contentTypeHeader.isPresent()) {
      if (contentTypeHeader.mimeTypePart().contains("json")) {
        return "json";
      }
      if (contentTypeHeader.mimeTypePart().contains("xml")) {
        return "xml";
      }
      if (contentTypeHeader.mimeTypePart().contains("text")) {
        return "txt";
      }

      String extension = COMMON_MIME_TYPES.get(contentTypeHeader.mimeTypePart());
      if (extension != null) {
        return extension;
      }
    }

    String path = URI.create(url).getPath();
    String lastPathSegment = substringAfterLast(path, "/");
    if (lastPathSegment.indexOf('.') != -1) {
      return substringAfterLast(lastPathSegment, ".");
    }

    return determineTextFileExtension(stringFromBytes(responseBody, contentTypeHeader.charset()));
  }

  public static TextType determineTextType(String content) {
    try {
      Json.read(content, JsonNode.class);
      return JSON;
    } catch (Exception e) {
      try {
        Xml.read(content);
        return TextType.XML;
      } catch (Exception e1) {
        return TextType.PLAIN_TEXT;
      }
    }
  }

  public static String determineTextFileExtension(String content) {
    TextType textType = determineTextType(content);
    switch (textType) {
      case JSON:
        return "json";
      case XML:
        return "xml";
      default:
        return "txt";
    }
  }

  public static boolean determineIsTextFromExtension(String extension) {
    return TEXT_FILE_EXTENSIONS.contains(extension);
  }

  public static boolean determineIsTextFromMimeType(final String mimeType) {
    return any(
        TEXT_MIME_TYPE_PATTERNS,
        new Predicate<String>() {
          @Override
          public boolean apply(String pattern) {
            return mimeType != null && mimeType.matches(pattern);
          }
        });
  }

  public static boolean determineIsText(String extension, String mimeType) {
    return determineIsTextFromExtension(extension) || determineIsTextFromMimeType(mimeType);
  }
}
