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
import static org.hamcrest.Matchers.containsString;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.trafficlistener.CollectingNetworkTrafficListener;
import org.junit.BeforeClass;
import org.junit.Test;

public class NetworkTrafficListenerAcceptanceTest extends AcceptanceTestBase {
  private static CollectingNetworkTrafficListener networkTrafficListener =
      new CollectingNetworkTrafficListener();

  @BeforeClass
  public static void setupServer() {
    setupServer(new WireMockConfiguration().networkTrafficListener(networkTrafficListener));
  }

  @Test
  public void capturesRawTraffic() {
    testClient.get("/a/non-registered/resource");

    assertThat(
        networkTrafficListener.getAllRequests(),
        containsString("GET /a/non-registered/resource HTTP/1.1\r\n"));
    assertThat(networkTrafficListener.getAllRequests(), containsString("Content-Length: 0\r\n"));
    assertThat(
        networkTrafficListener.getAllResponses(), containsString("HTTP/1.1 404 Not Found\r\n"));
  }
}
