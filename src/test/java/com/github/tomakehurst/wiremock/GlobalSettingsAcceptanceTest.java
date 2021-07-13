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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.http.LogNormal;
import org.junit.Test;

public class GlobalSettingsAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void settingGlobalFixedResponseDelay() {
    WireMock.setGlobalFixedDelay(500);
    givenThat(
        get(urlEqualTo("/globally/delayed/resource")).willReturn(aResponse().withStatus(200)));

    long start = System.currentTimeMillis();
    testClient.get("/globally/delayed/resource");
    int duration = (int) (System.currentTimeMillis() - start);

    assertThat(duration, greaterThanOrEqualTo(500));
  }

  @Test
  public void settingGlobalRandomDistributionDelayCausesADelay() {
    WireMock.setGlobalRandomDelay(new LogNormal(90, 0.1));
    givenThat(
        get(urlEqualTo("/globally/random/delayed/resource"))
            .willReturn(aResponse().withStatus(200)));

    long start = System.currentTimeMillis();
    testClient.get("/globally/random/delayed/resource");
    int duration = (int) (System.currentTimeMillis() - start);

    assertThat(duration, greaterThanOrEqualTo(60));
  }

  @Test
  public void canCombineFixedAndRandomDelays() {
    WireMock.setGlobalRandomDelay(new LogNormal(90, 0.1));
    WireMock.setGlobalFixedDelay(30);
    givenThat(
        get(urlEqualTo("/globally/random/delayed/resource"))
            .willReturn(aResponse().withStatus(200)));

    long start = System.currentTimeMillis();
    testClient.get("/globally/random/delayed/resource");
    int duration = (int) (System.currentTimeMillis() - start);

    assertThat(duration, greaterThanOrEqualTo(90));
  }

  @Test
  public void fetchSettings() {
    WireMock.setGlobalFixedDelay(30);

    GlobalSettings settings = WireMock.getSettings();

    assertThat(settings.getFixedDelay(), is(30));
  }

  @Test
  public void setAndRetrieveExtendedSettings() {
    WireMock.updateSettings(
        GlobalSettings.builder().extended(Parameters.one("mySetting", "setting-value")).build());

    GlobalSettings fetchedSettings = WireMock.getSettings();

    assertThat(fetchedSettings.getExtended().getString("mySetting"), is("setting-value"));
  }
}
