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
package com.github.tomakehurst.wiremock.crypto;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class InMemoryKeyStore {

  public enum KeyStoreType {
    JKS("jks");

    private final String type;

    KeyStoreType(String type) {
      this.type = type;
    }
  }

  private final Secret password;
  private final KeyStore keyStore;

  public InMemoryKeyStore(KeyStoreType type, Secret password) {
    this.password = requireNonNull(password, "password");
    this.keyStore = initialise(requireNonNull(type, "type"));
  }

  private KeyStore initialise(KeyStoreType type) {
    try {
      KeyStore keyStore = KeyStore.getInstance(type.type);
      keyStore.load(null, password.getValue());
      return keyStore;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      return throwUnchecked(e, null);
    }
  }

  public void addPrivateKey(String alias, KeyPair keyPair, Certificate... certs)
      throws KeyStoreException {
    keyStore.setKeyEntry(alias, keyPair.getPrivate(), password.getValue(), certs);
  }

  public void addCertificate(String alias, Certificate cert) throws KeyStoreException {
    keyStore.setCertificateEntry(alias, cert);
  }

  public void saveAs(File file) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(file)) {
      try {
        keyStore.store(fos, password.getValue());
      } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
        throwUnchecked(e);
      }
    }
  }

  public KeyStore getKeyStore() {
    return keyStore;
  }
}
