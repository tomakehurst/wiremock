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
package com.github.tomakehurst.wiremock.client;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.FluentIterable.from;

import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.NearMiss;
import com.github.tomakehurst.wiremock.verification.diff.Diff;
import com.google.common.base.Joiner;
import java.util.List;

public class VerificationException extends AssertionError {

  private static final long serialVersionUID = 5116216532516117538L;

  public VerificationException(String message) {
    super(message);
  }

  public static VerificationException forUnmatchedRequestPattern(Diff diff) {
    return new VerificationException(
        "No requests exactly matched. Most similar request was:", diff);
  }

  public static VerificationException forSingleUnmatchedRequest(Diff diff) {
    return new VerificationException(
        "A request was unmatched by any stub mapping. Closest stub mapping was:", diff);
  }

  public static VerificationException forUnmatchedNearMisses(List<NearMiss> nearMisses) {
    if (nearMisses.size() == 1) {
      return forSingleUnmatchedRequest(nearMisses.get(0).getDiff());
    }

    return new VerificationException(
        nearMisses.size()
            + " requests were unmatched by any stub mapping. Shown with closest stub mappings:\n"
            + renderList(nearMisses));
  }

  private static String renderList(List<?> list) {
    return Joiner.on("\n\n").join(from(list).transform(toStringFunction()));
  }

  private VerificationException(String messageStart, Diff diff) {
    super(messageStart + " " + diff.toString());
  }

  public VerificationException(RequestPattern expected, List<LoggedRequest> requests) {
    super(
        String.format(
            "Expected at least one request matching: %s\nRequests received: %s",
            expected.toString(), Json.write(requests)));
  }

  public VerificationException(RequestPattern expected, int expectedCount, int actualCount) {
    super(
        String.format(
            "Expected exactly %d requests matching the following pattern but received %d:\n%s",
            expectedCount, actualCount, expected.toString()));
  }

  public VerificationException(
      RequestPattern expected, CountMatchingStrategy expectedCount, int actualCount) {
    super(
        String.format(
            "Expected %s requests matching the following pattern but received %d:\n%s",
            expectedCount.toString().toLowerCase(), actualCount, expected.toString()));
  }

  public static VerificationException forUnmatchedRequests(List<LoggedRequest> unmatchedRequests) {
    if (unmatchedRequests.size() == 1) {
      return new VerificationException(
          String.format(
              "A request was unmatched by any stub " + "mapping. Request was: %s",
              unmatchedRequests.get(0)));
    }

    return new VerificationException(
        unmatchedRequests.size()
            + " requests were unmatched by any stub mapping. Requests are:\n"
            + renderList(unmatchedRequests));
  }
}
