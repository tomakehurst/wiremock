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

import java.security.cert.X509Certificate;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLException;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;

public class ApacheHttpHostNameMatcher implements HostNameMatcher {
  @Override
  public Boolean matches(X509Certificate x509Certificate, SNIHostName sniHostName) {
    try {
      new DefaultHostnameVerifier().verify(sniHostName.getAsciiName(), x509Certificate);
      return true;
    } catch (SSLException e) {
      return false;
    }
  }
}
