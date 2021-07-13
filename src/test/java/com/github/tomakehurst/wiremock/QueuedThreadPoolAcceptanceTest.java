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
package com.github.tomakehurst.wiremock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ThreadPoolFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueuedThreadPoolAcceptanceTest extends AcceptanceTestBase {

  @BeforeClass
  public static void setupServer() {
    setupServer(new WireMockConfiguration().threadPoolFactory(new InstrumentedThreadPoolFactory()));
  }

  @Test
  public void serverUseCustomInstrumentedQueuedThreadPool() {
    assertThat(InstrumentedQueuedThreadPool.flag, is(true));
  }

  public static class InstrumentedQueuedThreadPool extends QueuedThreadPool {
    public static boolean flag = false;

    public InstrumentedQueuedThreadPool(int maxThreads) {
      this(maxThreads, 8);
    }

    public InstrumentedQueuedThreadPool(int maxThreads, int minThreads) {
      this(maxThreads, minThreads, 60000);
    }

    public InstrumentedQueuedThreadPool(int maxThreads, int minThreads, int idleTimeout) {
      super(maxThreads, minThreads, idleTimeout, null);
    }

    @Override
    protected void doStart() throws Exception {
      super.doStart();
      flag = true;
    }
  }

  public static class InstrumentedThreadPoolFactory implements ThreadPoolFactory {
    @Override
    public ThreadPool buildThreadPool(Options options) {
      return new InstrumentedQueuedThreadPool(options.containerThreads());
    }
  }
}
