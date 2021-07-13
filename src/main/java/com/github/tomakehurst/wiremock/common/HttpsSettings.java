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

import com.github.tomakehurst.wiremock.common.ssl.KeyStoreSettings;
import com.google.common.io.Resources;

public class HttpsSettings {

  private final int port;
  private final String keyStorePath;
  private final String keyStorePassword;
  private final String keyManagerPassword;
  private final String keyStoreType;
  private final String trustStorePath;
  private final String trustStorePassword;
  private final String trustStoreType;
  private final boolean needClientAuth;

  public HttpsSettings(
      int port,
      String keyStorePath,
      String keyStorePassword,
      String keyManagerPassword,
      String keyStoreType,
      String trustStorePath,
      String trustStorePassword,
      String trustStoreType,
      boolean needClientAuth) {
    this.port = port;
    this.keyStorePath = keyStorePath;
    this.keyStorePassword = keyStorePassword;
    this.keyManagerPassword = keyManagerPassword;
    this.keyStoreType = keyStoreType;
    this.trustStorePath = trustStorePath;
    this.trustStorePassword = trustStorePassword;
    this.trustStoreType = trustStoreType;
    this.needClientAuth = needClientAuth;
  }

  public int port() {
    return port;
  }

  public String keyStorePath() {
    return keyStorePath;
  }

  public String keyStorePassword() {
    return keyStorePassword;
  }

  public String keyManagerPassword() {
    return keyManagerPassword;
  }

  public String keyStoreType() {
    return keyStoreType;
  }

  public boolean enabled() {
    return port > -1;
  }

  public String trustStorePath() {
    return trustStorePath;
  }

  public String trustStorePassword() {
    return trustStorePassword;
  }

  public String trustStoreType() {
    return trustStoreType;
  }

  public boolean needClientAuth() {
    return needClientAuth;
  }

  public boolean hasTrustStore() {
    return trustStorePath != null;
  }

  public KeyStoreSettings trustStore() {
    return trustStorePath != null
        ? new KeyStoreSettings(trustStorePath, trustStorePassword, trustStoreType)
        : KeyStoreSettings.NO_STORE;
  }

  public KeyStoreSettings keyStore() {
    return new KeyStoreSettings(keyStorePath, keyStorePassword, keyStoreType);
  }

  @Override
  public String toString() {
    return "HttpsSettings{"
        + "port="
        + port
        + ", keyStorePath='"
        + keyStorePath
        + '\''
        + ", keyStoreType='"
        + keyStoreType
        + '\''
        + ", trustStorePath='"
        + trustStorePath
        + '\''
        + ", trustStoreType='"
        + trustStoreType
        + '\''
        + ", needClientAuth="
        + needClientAuth
        + '}';
  }

  public static class Builder {

    private int port;
    private String keyStorePath = Resources.getResource("keystore").toString();
    private String keyStorePassword = "password";
    private String keyManagerPassword = "password";
    private String keyStoreType = "JKS";
    private String trustStorePath = null;
    private String trustStorePassword = "password";
    private String trustStoreType = "JKS";
    private boolean needClientAuth = false;

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder keyStorePath(String keyStorePath) {
      this.keyStorePath = keyStorePath;
      return this;
    }

    public Builder keyStorePassword(String keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
      return this;
    }

    public Builder keyManagerPassword(String keyStorePassword) {
      this.keyManagerPassword = keyStorePassword;
      return this;
    }

    public Builder keyStoreType(String keyStoreType) {
      this.keyStoreType = keyStoreType;
      return this;
    }

    public Builder trustStorePath(String trustStorePath) {
      this.trustStorePath = trustStorePath;
      return this;
    }

    public Builder trustStorePassword(String trustStorePassword) {
      this.trustStorePassword = trustStorePassword;
      return this;
    }

    public Builder trustStoreType(String trustStoreType) {
      this.trustStoreType = trustStoreType;
      return this;
    }

    public Builder needClientAuth(boolean needClientAuth) {
      this.needClientAuth = needClientAuth;
      return this;
    }

    public HttpsSettings build() {
      return new HttpsSettings(
          port,
          keyStorePath,
          keyStorePassword,
          keyManagerPassword,
          keyStoreType,
          trustStorePath,
          trustStorePassword,
          trustStoreType,
          needClientAuth);
    }
  }
}
