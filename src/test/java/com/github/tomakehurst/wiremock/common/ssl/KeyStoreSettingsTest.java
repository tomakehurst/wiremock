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
package com.github.tomakehurst.wiremock.common.ssl;

import static com.github.tomakehurst.wiremock.testsupport.TestFiles.*;
import static org.junit.Assert.assertNotNull;

import java.security.KeyStore;
import org.junit.Test;

public class KeyStoreSettingsTest {

  @Test
  public void loadsTrustStoreFromClasspath() {
    KeyStoreSettings trustStoreSettings =
        new KeyStoreSettings(TRUST_STORE_NAME, TRUST_STORE_PASSWORD, "jks");

    KeyStore keyStore = trustStoreSettings.loadStore();
    assertNotNull(keyStore);
  }

  @Test
  public void loadsTrustStoreFromFilesystem() {
    KeyStoreSettings trustStoreSettings =
        new KeyStoreSettings(TRUST_STORE_PATH, TRUST_STORE_PASSWORD, "jks");

    KeyStore keyStore = trustStoreSettings.loadStore();
    assertNotNull(keyStore);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsWhenTrustStoreNotFound() {
    KeyStoreSettings trustStoreSettings = new KeyStoreSettings("test-unknownstore", "", "jks");
    trustStoreSettings.loadStore();
  }
}
