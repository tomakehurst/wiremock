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

import java.io.IOException;
import sun.security.x509.CertificateVersion;

@SuppressWarnings("sunapi")
public enum X509CertificateVersion {
  V1(CertificateVersion.V1),
  V2(CertificateVersion.V2),
  V3(CertificateVersion.V3);

  private final CertificateVersion version;

  X509CertificateVersion(int version) {
    this.version = getVersion(version);
  }

  private static CertificateVersion getVersion(int version) {
    try {
      return new CertificateVersion(version);
    } catch (IOException e) {
      return throwUnchecked(e, null);
    }
  }

  CertificateVersion getVersion() {
    return version;
  }
}
