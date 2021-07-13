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
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.NearMiss;
import java.util.List;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class WireMockRule extends WireMockServer implements TestRule {

  private final boolean failOnUnmatchedRequests;

  public WireMockRule(Options options) {
    this(options, true);
  }

  public WireMockRule(Options options, boolean failOnUnmatchedRequests) {
    super(options);
    this.failOnUnmatchedRequests = failOnUnmatchedRequests;
  }

  public WireMockRule(int port) {
    this(wireMockConfig().port(port));
  }

  public WireMockRule(int port, Integer httpsPort) {
    this(wireMockConfig().port(port).httpsPort(httpsPort));
  }

  public WireMockRule() {
    this(wireMockConfig());
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        start();

        if (options.getHttpDisabled()) {
          WireMock.configureFor("https", "localhost", httpsPort());
        } else {
          WireMock.configureFor("localhost", port());
        }

        try {
          before();
          base.evaluate();
          checkForUnmatchedRequests();
        } finally {
          after();
          stop();
        }
      }
    };
  }

  private void checkForUnmatchedRequests() {
    if (failOnUnmatchedRequests) {
      List<LoggedRequest> unmatchedRequests = findAllUnmatchedRequests();
      if (!unmatchedRequests.isEmpty()) {
        List<NearMiss> nearMisses = findNearMissesForAllUnmatchedRequests();
        if (nearMisses.isEmpty()) {
          throw VerificationException.forUnmatchedRequests(unmatchedRequests);
        } else {
          throw VerificationException.forUnmatchedNearMisses(nearMisses);
        }
      }
    }
  }

  protected void before() {
    // NOOP
  }

  protected void after() {
    // NOOP
  }
}
