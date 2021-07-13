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

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

@SuppressWarnings("sunapi")
public class X509CertificateSpecification implements CertificateSpecification {

  private final X509CertificateVersion version;
  private final X500Name subject;
  private final X500Name issuer;
  // java.time is JDK8 only
  private final Date notBefore;
  private final Date notAfter;

  public X509CertificateSpecification(
      X509CertificateVersion version, String subject, String issuer, Date notBefore, Date notAfter)
      throws IOException {
    this.version = requireNonNull(version);
    this.subject = new X500Name(requireNonNull(subject));
    this.issuer = new X500Name(requireNonNull(issuer));
    this.notBefore = requireNonNull(notBefore);
    this.notAfter = requireNonNull(notAfter);
  }

  @Override
  public X509Certificate certificateFor(KeyPair keyPair)
      throws CertificateException, InvalidKeyException, SignatureException {
    try {
      SecureRandom random = new SecureRandom();

      X509CertInfo info = new X509CertInfo();
      info.set(X509CertInfo.VERSION, version.getVersion());

      // On Java <= 1.7 it has to be a `CertificateSubjectName`
      // On Java >= 1.8 it has to be an `X500Name`
      try {
        info.set(X509CertInfo.SUBJECT, subject);
      } catch (CertificateException ignore) {
        info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(subject));
      }

      // On Java <= 1.7 it has to be a `CertificateIssuerName`
      // On Java >= 1.8 it has to be an `X500Name`
      try {
        info.set(X509CertInfo.ISSUER, issuer);
      } catch (CertificateException ignore) {
        info.set(X509CertInfo.ISSUER, new CertificateIssuerName(issuer));
      }

      info.set(X509CertInfo.VALIDITY, new CertificateValidity(notBefore, notAfter));

      info.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
      info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger(64, random)));
      info.set(
          X509CertInfo.ALGORITHM_ID,
          new CertificateAlgorithmId(new AlgorithmId(AlgorithmId.sha1WithRSAEncryption_oid)));

      // Sign the cert to identify the algorithm that's used.
      X509CertImpl cert = new X509CertImpl(info);
      cert.sign(keyPair.getPrivate(), "SHA256withRSA");

      // Update the algorithm and sign again.
      info.set(
          CertificateAlgorithmId.NAME + '.' + CertificateAlgorithmId.ALGORITHM,
          cert.get(X509CertImpl.SIG_ALG));
      cert = new X509CertImpl(info);
      cert.sign(keyPair.getPrivate(), "SHA256withRSA");
      cert.verify(keyPair.getPublic());

      return cert;
    } catch (IOException | NoSuchAlgorithmException | NoSuchProviderException e) {
      return throwUnchecked(e, null);
    }
  }
}
