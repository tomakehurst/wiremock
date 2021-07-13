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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.common.HttpClientUtils.getEntityAsStringAndCloseStream;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class HttpClientFactoryAcceptsTrustedCertificatesTest
    extends HttpClientFactoryCertificateVerificationTest {

  @Parameters(name = "{index}: trusted={0}, certificateCN={1}, validCertificate={2}")
  public static Collection<Object[]> data() {
    return asList(
        new Object[][] {
          // trusted                     certificateCN validCertificate?
          {TRUST_NOBODY, "localhost", true},
          {singletonList("other.com"), "localhost", true},
          {singletonList("localhost"), "other.com", true},
          {singletonList("localhost"), "other.com", false},
          {singletonList("localhost"), "localhost", true},
          {singletonList("localhost"), "localhost", false},
        });
  }

  @Test
  public void certificatesAreAccepted() throws Exception {

    server.stubFor(get("/whatever").willReturn(aResponse().withBody("Hello World")));

    HttpResponse response = client.execute(new HttpGet(server.url("/whatever")));

    String result = getEntityAsStringAndCloseStream(response);

    assertEquals("Hello World", result);
  }

  public HttpClientFactoryAcceptsTrustedCertificatesTest(
      List<String> trustedHosts, String certificateCN, boolean validCertificate) {
    super(trustedHosts, certificateCN, validCertificate);
  }
}
