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
package com.github.tomakehurst.wiremock.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;

import org.junit.Test;

public class ProxySettingsTest {

  public static final String PROXYVIA_URL = "a.proxyvia.url";
  public static final int PROXYVIA_PORT = 8080;
  public static final String PROXYVIA_URL_WITH_PORT = PROXYVIA_URL + ":" + PROXYVIA_PORT;
  public static final int DEFAULT_PORT = 80;
  public static final String USER = "user";
  public static final String PASSWORD = "pass";

  @Test
  public void shouldRetrieveProxySettingsFromString() {
    ProxySettings proxySettings = ProxySettings.fromString(PROXYVIA_URL_WITH_PORT);
    assertThat(proxySettings.host(), is(PROXYVIA_URL));
    assertThat(proxySettings.port(), is(PROXYVIA_PORT));
  }

  @Test
  public void shouldUse80AsDefaultPort() {
    ProxySettings proxySettings = ProxySettings.fromString(PROXYVIA_URL);
    assertThat(proxySettings.host(), is(PROXYVIA_URL));
    assertThat(proxySettings.port(), is(DEFAULT_PORT));
  }

  @Test
  public void shouldRecognizeUrlWithTrailingSlashIsPresent() {
    ProxySettings proxySettings = ProxySettings.fromString(PROXYVIA_URL_WITH_PORT + "/");
    assertThat(proxySettings.host(), is(PROXYVIA_URL));
    assertThat(proxySettings.port(), is(PROXYVIA_PORT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionIfPortIsNotRecognized() {
    ProxySettings proxySettings = ProxySettings.fromString(PROXYVIA_URL + ":80a");
  }

  @Test
  public void shouldRetrieveProxyCredsFromUrl() {
    ProxySettings proxySettings =
        ProxySettings.fromString(USER + ":" + PASSWORD + "@" + PROXYVIA_URL);
    assertThat(proxySettings.host(), is(PROXYVIA_URL));
    assertThat(proxySettings.port(), is(DEFAULT_PORT));
    assertThat(proxySettings.getUsername(), is(USER));
    assertThat(proxySettings.getPassword(), is(PASSWORD));
  }

  @Test
  public void shouldRetrieveProxyCredsAndPortFromUrl() {
    ProxySettings proxySettings =
        ProxySettings.fromString(USER + ":" + PASSWORD + "@" + PROXYVIA_URL_WITH_PORT);
    assertThat(proxySettings.host(), is(PROXYVIA_URL));
    assertThat(proxySettings.port(), is(PROXYVIA_PORT));
    assertThat(proxySettings.getUsername(), is(USER));
    assertThat(proxySettings.getPassword(), is(PASSWORD));
  }

  @Test
  public void shouldRetrieveProxyCredsWithOnlyUserFromUrl() {
    ProxySettings proxySettings = ProxySettings.fromString(USER + "@" + PROXYVIA_URL);
    assertThat(proxySettings.host(), is(PROXYVIA_URL));
    assertThat(proxySettings.port(), is(DEFAULT_PORT));
    assertThat(proxySettings.getUsername(), is(USER));
    assertThat(proxySettings.getPassword(), isEmptyOrNullString());
  }

  @Test
  public void shouldAllowProtocol() {
    ProxySettings proxySettings = ProxySettings.fromString("http://" + PROXYVIA_URL_WITH_PORT);
    assertThat(proxySettings.host(), is(PROXYVIA_URL));
    assertThat(proxySettings.port(), is(PROXYVIA_PORT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowHttpsProtocol() {
    ProxySettings proxySettings = ProxySettings.fromString("https://" + PROXYVIA_URL_WITH_PORT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionIfUrlIsInvalid() {
    ProxySettings proxySettings = ProxySettings.fromString("ul:invalid:80");
  }
}
