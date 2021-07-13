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

import com.google.common.base.Optional;

/**
 * Exposed Jetty tuning options. See:
 * http://download.eclipse.org/jetty/stable-7/apidocs/org/eclipse/jetty/server/AbstractConnector.html
 */
public class JettySettings {
  private final Optional<Integer> acceptors;
  private final Optional<Integer> acceptQueueSize;
  private final Optional<Integer> requestHeaderSize;
  private final Optional<Long> stopTimeout;
  private final Optional<Long> idleTimeout;

  private JettySettings(
      Optional<Integer> acceptors,
      Optional<Integer> acceptQueueSize,
      Optional<Integer> requestHeaderSize,
      Optional<Long> stopTimeout,
      Optional<Long> idleTimeout) {
    this.acceptors = acceptors;
    this.acceptQueueSize = acceptQueueSize;
    this.requestHeaderSize = requestHeaderSize;
    this.stopTimeout = stopTimeout;
    this.idleTimeout = idleTimeout;
  }

  public Optional<Integer> getAcceptors() {
    return acceptors;
  }

  public Optional<Integer> getAcceptQueueSize() {
    return acceptQueueSize;
  }

  public Optional<Integer> getRequestHeaderSize() {
    return requestHeaderSize;
  }

  public Optional<Long> getStopTimeout() {
    return stopTimeout;
  }

  public Optional<Long> getIdleTimeout() {
    return idleTimeout;
  }

  @Override
  public String toString() {
    return "JettySettings{"
        + "acceptors="
        + acceptors
        + ", acceptQueueSize="
        + acceptQueueSize
        + ", requestHeaderSize="
        + requestHeaderSize
        + '}';
  }

  public static class Builder {
    private Integer acceptors;
    private Integer acceptQueueSize;
    private Integer requestHeaderSize;
    private Long stopTimeout;
    private Long idleTimeout;

    private Builder() {}

    public static Builder aJettySettings() {
      return new Builder();
    }

    public Builder withAcceptors(Integer acceptors) {
      this.acceptors = acceptors;
      return this;
    }

    public Builder withAcceptQueueSize(Integer acceptQueueSize) {
      this.acceptQueueSize = acceptQueueSize;
      return this;
    }

    public Builder withRequestHeaderSize(Integer requestHeaderSize) {
      this.requestHeaderSize = requestHeaderSize;
      return this;
    }

    public Builder withStopTimeout(Long stopTimeout) {
      this.stopTimeout = stopTimeout;
      return this;
    }

    public Builder withIdleTimeout(Long idleTimeout) {
      this.idleTimeout = idleTimeout;
      return this;
    }

    public JettySettings build() {
      return new JettySettings(
          Optional.fromNullable(acceptors),
          Optional.fromNullable(acceptQueueSize),
          Optional.fromNullable(requestHeaderSize),
          Optional.fromNullable(stopTimeout),
          Optional.fromNullable(idleTimeout));
    }
  }
}
