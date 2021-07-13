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

import com.fasterxml.jackson.annotation.JsonProperty;

public class Timing {

  public static final Timing UNTIMED = new Timing(-1, -1);

  private final int addedDelay;
  private final int processTime;
  private final int responseSendTime;

  public Timing(int addedDelay, int processTime) {
    this(addedDelay, processTime, -1, -1, -1);
  }

  private Timing(
      @JsonProperty("addedDelay") int addedDelay,
      @JsonProperty("processTime") int processTime,
      @JsonProperty("responseSendTime") int responseSendTime,
      @JsonProperty("serveTime") int ignored1,
      @JsonProperty("totalTime") int ignored2) {
    this.addedDelay = addedDelay;
    this.processTime = processTime;
    this.responseSendTime = responseSendTime;
  }

  /** The delay added to the response via the stub or global configuration */
  public int getAddedDelay() {
    return addedDelay;
  }

  /** The amount of time spent handling the stub request */
  public int getProcessTime() {
    return processTime;
  }

  /** The amount of time taken to send the response to the client */
  public int getResponseSendTime() {
    return responseSendTime;
  }

  /** The total request time from start to finish, minus added delay */
  public int getServeTime() {
    return processTime + responseSendTime;
  }

  /** The total request time including added delay */
  public int getTotalTime() {
    return getServeTime() + addedDelay;
  }

  public Timing withResponseSendTime(int responseSendTimeMillis) {
    return new Timing(addedDelay, processTime, responseSendTimeMillis, -1, -1);
  }
}
