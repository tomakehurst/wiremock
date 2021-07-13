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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import java.util.List;

public class JUnitStyleDiffRenderer {

  public String render(Diff diff) {
    List<DiffLine<?>> lines = diff.getLines();

    String expected = Joiner.on("\n").join(from(lines).transform(EXPECTED));
    String actual = Joiner.on("\n").join(from(lines).transform(ACTUAL));

    return lines.isEmpty() ? "" : junitStyleDiffMessage(expected, actual);
  }

  public static String junitStyleDiffMessage(Object expected, Object actual) {
    return String.format(" expected:<\n%s> but was:<\n%s>", expected, actual);
  }

  private static Function<DiffLine<?>, Object> EXPECTED =
      new Function<DiffLine<?>, Object>() {
        @Override
        public Object apply(DiffLine<?> line) {
          return line.isForNonMatch() ? line.getPrintedPatternValue() : line.getActual();
        }
      };

  private static Function<DiffLine<?>, Object> ACTUAL =
      new Function<DiffLine<?>, Object>() {
        @Override
        public Object apply(DiffLine<?> input) {
          return input.getActual();
        }
      };
}
