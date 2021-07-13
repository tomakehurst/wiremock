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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Returns log normally distributed values. Takes two parameters, the median (50th percentile) of
 * the lognormal and the standard deviation of the underlying normal distribution.
 *
 * <p>The larger the standard deviation the longer the tails.
 *
 * @see <a
 *     href="https://www.wolframalpha.com/input/?i=lognormaldistribution%28log%2890%29%2C+0.1%29">lognormal
 *     example</a>
 */
public final class LogNormal implements DelayDistribution {

  @JsonProperty("median")
  private final double median;

  @JsonProperty("sigma")
  private final double sigma;

  /**
   * @param median 50th percentile of the distribution in millis
   * @param sigma standard deviation of the distribution, a larger value produces a longer tail
   */
  @JsonCreator
  public LogNormal(@JsonProperty("median") double median, @JsonProperty("sigma") double sigma) {
    this.median = median;
    this.sigma = sigma;
  }

  @Override
  public long sampleMillis() {
    return Math.round(Math.exp(ThreadLocalRandom.current().nextGaussian() * sigma) * median);
  }
}
