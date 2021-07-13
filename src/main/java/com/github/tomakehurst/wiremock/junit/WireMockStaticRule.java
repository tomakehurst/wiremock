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
package com.github.tomakehurst.wiremock.junit;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * @deprecated JUnit disallows this approach from version 4.11. Use {@link WireMockClassRule}
 *     instead
 */
@Deprecated
public class WireMockStaticRule implements MethodRule {

  private final WireMockServer wireMockServer;

  public WireMockStaticRule(int port) {
    wireMockServer = new WireMockServer(port);
    wireMockServer.start();
    WireMock.configureFor("localhost", port);
  }

  public WireMockStaticRule() {
    this(Options.DEFAULT_PORT);
  }

  public void stopServer() {
    wireMockServer.stop();
  }

  @Override
  public Statement apply(final Statement base, final FrameworkMethod method, Object target) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        try {
          before();
          base.evaluate();
        } finally {
          after();
          WireMock.reset();
        }
      }
    };
  }

  protected void before() {
    // NOOP
  }

  protected void after() {
    // NOOP
  }
}
