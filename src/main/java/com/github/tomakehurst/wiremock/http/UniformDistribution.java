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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Distribution that returns values uniformally distributed across a range.
 *
 * <p>That is, given a uniform distribution of 50 to 60 ms, there will be an equal spread of delays
 * between 50 and 60. This would useful for representing an average delay of 55ms with a +/- 5ms
 * jitter.
 */
public final class UniformDistribution implements DelayDistribution {

  @JsonProperty("lower")
  private final int lower;

  @JsonProperty("upper")
  private final int upper;

  /**
   * @param lower lower bound inclusive
   * @param upper upper bound inclusive
   */
  public UniformDistribution(@JsonProperty("lower") int lower, @JsonProperty("upper") int upper) {
    this.lower = lower;
    this.upper = upper;
  }

  @Override
  public long sampleMillis() {
    return ThreadLocalRandom.current().nextLong(lower, upper + 1);
  }
}
