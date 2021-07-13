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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class WireMockClassRule extends WireMockServer implements MethodRule, TestRule {

  public WireMockClassRule(Options options) {
    super(options);
  }

  public WireMockClassRule(int port, Integer httpsPort) {
    this(wireMockConfig().port(port).httpsPort(httpsPort));
  }

  public WireMockClassRule(int port) {
    this(wireMockConfig().port(port));
  }

  public WireMockClassRule() {
    this(wireMockConfig());
  }

  @Override
  public Statement apply(final Statement base, FrameworkMethod method, Object target) {
    return apply(base, null);
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        if (isRunning()) {
          try {
            before();
            base.evaluate();
          } finally {
            after();
            client.resetMappings();
          }
        } else {
          start();
          if (options.getHttpDisabled()) {
            WireMock.configureFor("https", "localhost", httpsPort());
          } else {
            WireMock.configureFor("http", "localhost", port());
          }

          try {
            before();
            base.evaluate();
          } finally {
            after();
            stop();
          }
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
