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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeadlockTest {

  private static final int READ_TIMEOUT = 500;

  private static WireMockServer wireMockServer;

  @BeforeClass
  public static void setUp() {
    wireMockServer = new WireMockServer(options().dynamicPort().dynamicHttpsPort());
    wireMockServer.start();
  }

  @AfterClass
  public static void tearDown() {
    wireMockServer.stop();
  }

  @Before
  public void reset() {
    System.out.println("reset");
    wireMockServer.resetAll();
  }

  @Test
  public void test1Timeout() throws IOException {
    System.out.println("test timeout start");

    wireMockServer.stubFor(
        get(urlEqualTo("/timeout"))
            .willReturn(aResponse().withFixedDelay(2 * READ_TIMEOUT).withBody("body1")));

    downloadContentAndMeasure("/timeout", null);

    System.out.println("test timeout end");
  }

  // This will fail with a timeout if acceptor count is < 3 and/or threads < 13
  @Test
  public void test2GetContent() throws IOException {
    System.out.println("test content start");

    wireMockServer.stubFor(get(urlEqualTo("/content")).willReturn(aResponse().withBody("body2")));
    System.out.println("test content stub");

    downloadContentAndMeasure("/content", "body2");

    System.out.println("test content end");
  }

  private void downloadContentAndMeasure(String urlDir, String expectedBody) throws IOException {
    System.out.printf("downloadContentAndMeasure urlDir=%s", urlDir);

    final long start = System.currentTimeMillis();

    boolean exceptionOccurred = false;
    try {
      final String url = "http://localhost:" + wireMockServer.port() + urlDir;
      final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setConnectTimeout(2000);
      connection.setReadTimeout(READ_TIMEOUT);
      connection.setDoInput(true);
      if (expectedBody == null) {
        try {
          httpGetContent(connection);
          fail("Expected SocketTimeoutException");
        } catch (Exception e) {
          assertThat(e, instanceOf(SocketTimeoutException.class));
        }
      } else {
        final String body = httpGetContent(connection);
        assertEquals(expectedBody, body);
      }
    } catch (Exception e) {
      exceptionOccurred = true;
      System.out.printf(
          "exception '%s' after ms %s",
          e.getMessage(), TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start));
      throw e;
    } finally {
      if (!exceptionOccurred) {
        System.out.printf(
            "downloaded at ms %s",
            TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start));
      }
    }
  }

  private String httpGetContent(HttpURLConnection connection) throws IOException {
    try (InputStream is = connection.getInputStream()) {
      return IOUtils.toString(is);
    }
  }
}
