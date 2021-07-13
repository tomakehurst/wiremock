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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LogNormalTest {

  // To test properly we would need something like a normality test.
  // For our purposes, a simple verification is sufficient.
  @Test
  public void samplingLogNormalHasExpectedMean() {
    LogNormal distribution = new LogNormal(90.0, 0.39);
    int n = 10000;

    long sum = 0;
    for (int i = 0; i < n; i++) {
      sum += distribution.sampleMillis();
    }

    assertEquals(97.1115, sum / (double) n, 5.0);
  }
}
