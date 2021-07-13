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
package com.github.tomakehurst.wiremock.jetty94;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.http.ssl.ApacheHttpHostNameMatcher;
import com.github.tomakehurst.wiremock.http.ssl.CertificateGeneratingX509ExtendedKeyManager;
import com.github.tomakehurst.wiremock.http.ssl.DynamicKeyStore;
import com.github.tomakehurst.wiremock.http.ssl.X509KeyStore;
import java.security.KeyStore;
import javax.net.ssl.KeyManager;
import javax.net.ssl.X509ExtendedKeyManager;
import org.eclipse.jetty.util.ssl.SslContextFactory;

class CertificateGeneratingSslContextFactory extends SslContextFactory.Server {

  private final X509KeyStore x509KeyStore;
  private final Notifier notifier;

  CertificateGeneratingSslContextFactory(X509KeyStore x509KeyStore, Notifier notifier) {
    this.x509KeyStore = requireNonNull(x509KeyStore);
    this.notifier = requireNonNull(notifier);
  }

  @Override
  protected KeyManager[] getKeyManagers(KeyStore keyStore) throws Exception {
    KeyManager[] managers = super.getKeyManagers(keyStore);
    return stream(managers)
        .map(
            manager -> {
              if (manager instanceof X509ExtendedKeyManager) {
                return new CertificateGeneratingX509ExtendedKeyManager(
                    (X509ExtendedKeyManager) manager,
                    new DynamicKeyStore(x509KeyStore),
                    new ApacheHttpHostNameMatcher(),
                    notifier);
              } else {
                return manager;
              }
            })
        .toArray(KeyManager[]::new);
  }
}
