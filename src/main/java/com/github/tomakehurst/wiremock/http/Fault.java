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

import com.github.tomakehurst.wiremock.core.FaultInjector;

public enum Fault {
  CONNECTION_RESET_BY_PEER {
    @Override
    public void apply(FaultInjector faultInjector) {
      faultInjector.connectionResetByPeer();
    }
  },

  EMPTY_RESPONSE {
    @Override
    public void apply(FaultInjector faultInjector) {
      faultInjector.emptyResponseAndCloseConnection();
    }
  },

  MALFORMED_RESPONSE_CHUNK {
    @Override
    public void apply(FaultInjector faultInjector) {
      faultInjector.malformedResponseChunk();
    }
  },

  RANDOM_DATA_THEN_CLOSE {
    @Override
    public void apply(FaultInjector faultInjector) {
      faultInjector.randomDataAndCloseConnection();
    }
  };

  public abstract void apply(FaultInjector faultInjector);
}
