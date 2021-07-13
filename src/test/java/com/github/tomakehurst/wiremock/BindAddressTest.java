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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.*;

public class BindAddressTest {

  private String localhost = "127.0.0.1";
  private String nonBindAddress;
  private WireMockServer wireMockServer;

  final HttpClient client = HttpClientFactory.createClient();

  @Before
  public void prepare() throws Exception {
    nonBindAddress = getIpAddressOtherThan(localhost);

    assumeFalse(
        "Impossible to validate the binding address. This machine has only a one Ip address ["
            + localhost
            + "]",
        nonBindAddress == null);

    wireMockServer =
        new WireMockServer(
            wireMockConfig().bindAddress(localhost).dynamicPort().dynamicHttpsPort());
    wireMockServer.start();

    wireMockServer.stubFor(
        get(urlPathEqualTo("/bind-test")).willReturn(aResponse().withStatus(200)));
  }

  @After
  public void stop() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  @Test
  public void shouldRespondInTheBindAddressOnlyOnHttp() throws Exception {
    executeGetIn(localhost);
    try {
      executeGetIn(nonBindAddress);
      fail("Should not accept HTTP connection to [" + nonBindAddress + "]");
    } catch (Exception ex) {
    }
  }

  @Test
  public void shouldRespondInTheBindAddressOnlyOnHttps() throws Exception {
    int localhostStatus = getStatusViaHttps(localhost);
    assertThat(localhostStatus, is(200));

    try {
      getStatusViaHttps(nonBindAddress);
      fail("Should not accept HTTPS connection to [" + nonBindAddress + "]");
    } catch (Exception e) {
    }
  }

  private int getStatusViaHttps(String host) throws Exception {
    HttpResponse localhostResponse =
        client.execute(
            RequestBuilder.get("https://" + host + ":" + wireMockServer.httpsPort() + "/bind-test")
                .build());

    int status = localhostResponse.getStatusLine().getStatusCode();
    EntityUtils.consume(localhostResponse.getEntity());
    return status;
  }

  private void executeGetIn(String address) {
    WireMockTestClient wireMockClient = new WireMockTestClient(wireMockServer.port(), address);
    WireMockResponse response = wireMockClient.get("/bind-test");
    assertThat(response.statusCode(), is(200));
  }

  private String getIpAddressOtherThan(String lopbackAddress) throws SocketException {
    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
    for (NetworkInterface netInterface : Collections.list(networkInterfaces)) {
      Enumeration<InetAddress> inetAddresses = netInterface.getInetAddresses();
      for (InetAddress address : Collections.list(inetAddresses)) {
        if (address instanceof Inet4Address && !address.getHostAddress().equals(lopbackAddress)) {
          return address.getHostAddress();
        }
      }
    }
    return null;
  }
}
