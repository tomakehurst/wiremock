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
package com.github.tomakehurst.wiremock.global;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.DelayDistribution;
import java.util.Objects;

public class GlobalSettings {

  private final Integer fixedDelay;
  private final DelayDistribution delayDistribution;
  private final Parameters extended;

  public static GlobalSettings.Builder builder() {
    return new Builder();
  }

  public static GlobalSettings defaults() {
    return new Builder().build();
  }

  public GlobalSettings(
      @JsonProperty("fixedDelay") Integer fixedDelay,
      @JsonProperty("delayDistribution") DelayDistribution delayDistribution,
      @JsonProperty("extended") Parameters extended) {
    this.fixedDelay = fixedDelay;
    this.delayDistribution = delayDistribution;
    this.extended = extended;
  }

  public Integer getFixedDelay() {
    return fixedDelay;
  }

  public DelayDistribution getDelayDistribution() {
    return delayDistribution;
  }

  public Parameters getExtended() {
    return extended;
  }

  public GlobalSettings.Builder copy() {
    return new Builder()
        .fixedDelay(fixedDelay)
        .delayDistribution(delayDistribution)
        .extended(extended);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GlobalSettings that = (GlobalSettings) o;
    return Objects.equals(getFixedDelay(), that.getFixedDelay())
        && Objects.equals(getDelayDistribution(), that.getDelayDistribution())
        && Objects.equals(getExtended(), that.getExtended());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getFixedDelay(), getDelayDistribution(), getExtended());
  }

  public static class Builder {
    private Integer fixedDelay;
    private DelayDistribution delayDistribution;
    private Parameters extended;

    public Builder fixedDelay(Integer fixedDelay) {
      this.fixedDelay = fixedDelay;
      return this;
    }

    public Builder delayDistribution(DelayDistribution delayDistribution) {
      this.delayDistribution = delayDistribution;
      return this;
    }

    public Builder extended(Parameters extended) {
      this.extended = extended;
      return this;
    }

    public GlobalSettings build() {
      return new GlobalSettings(fixedDelay, delayDistribution, extended);
    }
  }
}
