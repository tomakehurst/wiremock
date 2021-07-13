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
package com.github.tomakehurst.wiremock.client;

/** Matches the number of requests made using relational predicates. */
public class CountMatchingStrategy {

  public static final CountMatchingMode LESS_THAN =
      new CountMatchingMode() {
        @Override
        public String getFriendlyName() {
          return "Less than";
        }

        @Override
        public boolean test(Integer actual, Integer expected) {
          return actual < expected;
        }
      };

  public static final CountMatchingMode LESS_THAN_OR_EQUAL =
      new CountMatchingMode() {
        @Override
        public String getFriendlyName() {
          return "Less than or exactly";
        }

        @Override
        public boolean test(Integer actual, Integer expected) {
          return actual <= expected;
        }
      };

  public static final CountMatchingMode EQUAL_TO =
      new CountMatchingMode() {
        @Override
        public String getFriendlyName() {
          return "Exactly";
        }

        @Override
        public boolean test(Integer actual, Integer expected) {
          return actual.equals(expected);
        }
      };

  public static final CountMatchingMode GREATER_THAN_OR_EQUAL =
      new CountMatchingMode() {
        @Override
        public String getFriendlyName() {
          return "More than or exactly";
        }

        @Override
        public boolean test(Integer actual, Integer expected) {
          return actual >= expected;
        }
      };

  public static final CountMatchingMode GREATER_THAN =
      new CountMatchingMode() {
        @Override
        public String getFriendlyName() {
          return "More than";
        }

        @Override
        public boolean test(Integer actual, Integer expected) {
          return actual > expected;
        }
      };

  private CountMatchingMode mode;
  private int expected;

  public CountMatchingStrategy(CountMatchingMode mode, int expected) {
    this.mode = mode;
    this.expected = expected;
  }

  public boolean match(int actual) {
    return mode.test(actual, expected);
  }

  @Override
  public String toString() {
    return String.format("%s %d", mode.getFriendlyName(), expected);
  }
}
