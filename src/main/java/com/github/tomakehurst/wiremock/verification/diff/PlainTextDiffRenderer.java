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

import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.rightPad;

import com.github.tomakehurst.wiremock.common.Strings;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import java.util.Map;

public class PlainTextDiffRenderer {

  private final String SEPARATOR = lineSeparator();

  private final int consoleWidth;
  private final Map<String, RequestMatcherExtension> customMatcherExtensions;

  public PlainTextDiffRenderer(Map<String, RequestMatcherExtension> customMatcherExtensions) {
    this(customMatcherExtensions, 119);
  }

  public PlainTextDiffRenderer(
      Map<String, RequestMatcherExtension> customMatcherExtensions, int consoleWidth) {
    this.customMatcherExtensions = customMatcherExtensions;
    this.consoleWidth = consoleWidth;
  }

  public String render(Diff diff) {
    StringBuilder sb = new StringBuilder();
    header(sb);

    if (diff.getStubMappingName() != null) {
      writeLine(sb, diff.getStubMappingName(), "", null);
      writeBlankLine(sb);
    }

    for (DiffLine<?> line : diff.getLines(customMatcherExtensions)) {
      boolean isBodyLine = line.getRequestAttribute().equals("Body");
      if (!isBodyLine || line.isForNonMatch()) {
        writeLine(
            sb, line.getPrintedPatternValue(), line.getActual().toString(), line.getMessage());
      }
    }

    writeBlankLine(sb);
    footer(sb);

    return sb.toString();
  }

  private void header(StringBuilder sb) {
    String titleLine = "Request was not matched";
    int middle = getMiddle();
    int titleLinePaddingLeft = middle - (titleLine.length() / 2);
    sb.append(SEPARATOR)
        .append(repeat(' ', titleLinePaddingLeft))
        .append(titleLine)
        .append(SEPARATOR)
        .append(repeat(' ', titleLinePaddingLeft))
        .append(repeat('=', titleLine.length()))
        .append(SEPARATOR)
        .append(SEPARATOR)
        .append(repeat('-', consoleWidth))
        .append(SEPARATOR)
        .append('|')
        .append(rightPad(" Closest stub", middle))
        .append('|')
        .append(rightPad(" Request", middle, ' '))
        .append('|')
        .append(SEPARATOR)
        .append(repeat('-', consoleWidth))
        .append(SEPARATOR);

    writeBlankLine(sb);
  }

  private void footer(StringBuilder sb) {
    sb.append(repeat('-', consoleWidth)).append(SEPARATOR);
  }

  private void writeLine(StringBuilder sb, String left, String right, String message) {
    String[] leftLines = wrap(left).split(SEPARATOR);
    String[] rightLines = wrap(right).split(SEPARATOR);

    int maxLines = Math.max(leftLines.length, rightLines.length);

    writeSingleLine(sb, leftLines[0], rightLines[0], message);

    if (maxLines > 1) {
      for (int i = 1; i < maxLines; i++) {
        String leftPart = leftLines.length > i ? leftLines[i] : "";
        String rightPart = rightLines.length > i ? rightLines[i] : "";
        writeSingleLine(sb, leftPart, rightPart, null);
      }
    }
  }

  private void writeBlankLine(StringBuilder sb) {
    writeSingleLine(sb, "", null, null);
  }

  private void writeSingleLine(StringBuilder sb, String left, String right) {
    writeSingleLine(sb, left, right, null);
  }

  private void writeSingleLine(StringBuilder sb, String left) {
    writeSingleLine(sb, left, null);
  }

  private void writeSingleLine(StringBuilder sb, String left, String right, String message) {
    sb.append("").append(rightPad(left, getMiddle() + 1, " ")).append("|");

    if (isNotEmpty(right)) {
      sb.append(" ");

      if (isNotEmpty(message)) {
        sb.append(rightPad(right, getMiddle() - 6, " ")).append("<<<<< ").append(message);
      } else {
        sb.append(right);
      }
    } else {
      if (isNotEmpty(message)) {
        sb.append(rightPad(right, getMiddle() - 5, " ")).append("<<<<< ").append(message);
      }
    }

    sb.append(SEPARATOR);
  }

  private String wrap(String s) {
    String safeString = s == null ? "" : s;
    return Strings.wrapIfLongestLineExceedsLimit(safeString, getColumnWidth());
  }

  private int getColumnWidth() {
    return (consoleWidth / 2) - 2;
  }

  private int getMiddle() {
    return (consoleWidth / 2) - 1;
  }
}
