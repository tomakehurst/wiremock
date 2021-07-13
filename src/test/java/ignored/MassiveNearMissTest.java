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
package ignored;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MassiveNearMissTest {

  @Rule public WireMockRule wm = new WireMockRule(options().dynamicPort(), false);

  WireMockTestClient client;

  @Before
  public void setup() {
    client = new WireMockTestClient(wm.port());
  }

  @Test
  public void timeToCalculateBigNearMissDiffXml() {
    final int stubs = 1000;
    for (int i = 0; i < stubs; i++) {
      wm.stubFor(
          post(urlPathMatching("/things/.*/" + i))
              .withRequestBody(equalToXml(requestXml(i)))
              .willReturn(ok("i: " + i)));
    }

    final int drop = 2;
    final int reps = 10;
    List<Long> times = new ArrayList<>(reps);
    long sum = 0;
    for (int i = 0; i < reps; i++) {
      Stopwatch stopwatch = Stopwatch.createStarted();
      client.postXml(
          "/things/blah123/" + (stubs / 2), "<?xml version=\"1.0\"?><things id=\"" + i + "\"/>");
      stopwatch.stop();
      long time = stopwatch.elapsed(MILLISECONDS);
      times.add(time);
      if (i > drop) sum += time;
    }

    System.out.printf("Times:\n%s\n", Joiner.on("\n").join(times));
    long mean = sum / (reps - drop);
    System.out.printf("Mean: %dms\n", mean);
  }

  private static String requestXml(int i) {
    return String.format(
        "<?xml version=\"1.0\"?>\n"
            + "\n"
            + "<things id=\"%d\">\n"
            + "    <stuff id=\"1\"/>\n"
            + "    <fluff id=\"2\"/>\n"
            + "\n"
            + "    <inside>\n"
            + "        <deep-inside level=\"3\">\n"
            + "            <one/>\n"
            + "            <two/>\n"
            + "            <three/>\n"
            + "            <four/>\n"
            + "            <one/>\n"
            + "            <text subject=\"JWT\">\n"
            + "                JSON Web Token (JWT) is a compact, URL-safe means of representing claims to be transferred between two parties. The claims in a JWT are encoded as a JSON object that is used as the payload of a JSON Web Signature (JWS) structure or as the plaintext of a JSON Web Encryption (JWE) structure, enabling the claims to be digitally signed or integrity protected with a Message Authentication Code (MAC) and/or encrypted.\n"
            + "            </text>\n"
            + "        </deep-inside>\n"
            + "    </inside>\n"
            + "\n"
            + "</things>",
        i);
  }

  @Test
  public void timeToCalculateBigNearMissDiffJson() {
    final int stubs = 1000;
    for (int i = 0; i < stubs; i++) {
      wm.stubFor(
          post(urlPathMatching("/things/.*/" + i))
              .withRequestBody(equalToJson(requestJson(i)))
              .willReturn(ok("i: " + i)));
    }

    final int drop = 2;
    final int reps = 30;
    List<Long> times = new ArrayList<>(reps);
    long sum = 0;
    for (int i = 0; i < reps; i++) {
      Stopwatch stopwatch = Stopwatch.createStarted();
      client.postJson("/things/blah123/" + (stubs / 2), "{ \"wrong\": [1,2,3]}");
      stopwatch.stop();
      long time = stopwatch.elapsed(MILLISECONDS);
      times.add(time);
      if (i > drop) sum += time;
    }

    System.out.printf("Times:\n%s\n", Joiner.on("\n").join(times));
    long mean = sum / (reps - drop);
    System.out.printf("Mean: %dms\n", mean);
  }

  private String requestJson(int i) {
    return "{\n"
        + "  \"children\": [\n"
        + "    {\n"
        + "      \"id\": \"9010946\",\n"
        + "      \"age\": 1,\n"
        + "      \"isRegistered\": false\n"
        + "    },\n"
        + "    {\n"
        + "      \"id\": \"9405762\",\n"
        + "      \"age\": 1,\n"
        + "      \"isRegistered\": true\n"
        + "    },\n"
        + "    {\n"
        + "      \"id\": \"9166586\",\n"
        + "      \"age\": 1,\n"
        + "      \"isRegistered\": false\n"
        + "    },\n"
        + "    {\n"
        + "      \"id\": \"7537984\",\n"
        + "      \"age\": 1,\n"
        + "      \"isRegistered\": true\n"
        + "    }\n"
        + "  ],\n"
        + "  \"category\": \"060\",\n"
        + "  \"id\": \""
        + i
        + "\",\n"
        + "  \"isRegistered\": true,\n"
        + "  \"date\": \"20200312\"\n"
        + "}";
  }
}
