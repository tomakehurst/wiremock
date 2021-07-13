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
package com.github.tomakehurst.wiremock.http.ssl;

import static java.util.Objects.requireNonNull;

import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.SNIHostName;

public class DynamicKeyStore {

  private final X509KeyStore keyStore;
  private final CertificateAuthority existingCertificateAuthority;

  public DynamicKeyStore(X509KeyStore keyStore) {
    this.keyStore = requireNonNull(keyStore);
    this.existingCertificateAuthority =
        requireNonNull(
            keyStore.getCertificateAuthority(),
            "Keystore does not contain a certificate that can act as a certificate authority");
  }

  PrivateKey getPrivateKey(String alias) {
    return keyStore.getPrivateKey(alias);
  }

  X509Certificate[] getCertificateChain(String alias) {
    return keyStore.getCertificateChain(alias);
  }

  /**
   * @param keyType non null, guaranteed to be valid
   * @param requestedServerName non null
   */
  void generateCertificateIfNecessary(String keyType, SNIHostName requestedServerName)
      throws CertificateGenerationUnsupportedException, KeyStoreException {
    if (getPrivateKey(requestedServerName.getAsciiName()) == null) {
      generateCertificate(keyType, requestedServerName);
    }
  }

  /**
   * @param keyType non null, guaranteed to be valid
   * @param requestedServerName non null
   */
  private void generateCertificate(String keyType, SNIHostName requestedServerName)
      throws CertificateGenerationUnsupportedException, KeyStoreException {
    CertChainAndKey newCertChainAndKey =
        existingCertificateAuthority.generateCertificate(keyType, requestedServerName);
    keyStore.setKeyEntry(requestedServerName.getAsciiName(), newCertChainAndKey);
  }
}
