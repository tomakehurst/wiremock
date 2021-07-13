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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.google.common.base.Preconditions;
import java.net.MalformedURLException;
import java.net.URL;

public class ProxySettings {

  public static final ProxySettings NO_PROXY = new ProxySettings(null, 0);
  public static final int DEFAULT_PORT = 80;

  private final String host;
  private final int port;

  private String username;
  private String password;

  public ProxySettings(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public static ProxySettings fromString(String config) {
    try {
      URL proxyUrl;
      try {
        proxyUrl = new URL(config);
      } catch (MalformedURLException e) {
        config = "http://" + config;
        proxyUrl = new URL(config);
      }
      if (!"http".equals(proxyUrl.getProtocol())) {
        throw new IllegalArgumentException(
            "Proxy via does not support any other protocol than http");
      }
      Preconditions.checkArgument(
          !proxyUrl.getHost().isEmpty(), "Host part of proxy must be specified");
      ProxySettings proxySettings =
          new ProxySettings(
              proxyUrl.getHost(), proxyUrl.getPort() == -1 ? DEFAULT_PORT : proxyUrl.getPort());
      if (!isEmpty(proxyUrl.getUserInfo())) {
        String[] userInfoArray = proxyUrl.getUserInfo().split(":");
        proxySettings.setUsername(userInfoArray[0]);
        if (userInfoArray.length > 1) {
          proxySettings.setPassword(userInfoArray[1]);
        }
      }
      return proxySettings;
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(
          String.format("Proxy via Url %s was not recognized", config), e);
    }
  }

  public String host() {
    return host;
  }

  public int port() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    if (this == NO_PROXY) {
      return "(no proxy)";
    }

    return String.format(
        "%s:%s%s", host(), port(), (!isEmpty(this.username) ? " (with credentials)" : ""));
  }
}
