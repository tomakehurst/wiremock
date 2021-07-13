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

import static com.github.tomakehurst.wiremock.common.ArrayFunctions.prepend;
import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Date;
import javax.net.ssl.SNIHostName;
import sun.security.x509.*;

@SuppressWarnings("sunapi")
public class CertificateAuthority {

  private final X509Certificate[] certificateChain;
  private final PrivateKey key;

  public CertificateAuthority(X509Certificate[] certificateChain, PrivateKey key) {
    this.certificateChain = requireNonNull(certificateChain);
    if (certificateChain.length == 0) {
      throw new IllegalArgumentException("Chain must have entries");
    }
    this.key = requireNonNull(key);
  }

  public static CertificateAuthority generateCertificateAuthority()
      throws CertificateGenerationUnsupportedException {
    try {
      KeyPair pair = generateKeyPair("RSA");
      String sigAlg = "SHA256WithRSA";
      X509CertInfo info =
          makeX509CertInfo(
              sigAlg,
              "WireMock Local Self Signed Root Certificate",
              Period.ofYears(10),
              pair.getPublic(),
              certificateAuthorityExtensions(pair.getPublic()));

      X509CertImpl certificate = selfSign(info, pair.getPrivate(), sigAlg);

      return new CertificateAuthority(new X509Certificate[] {certificate}, pair.getPrivate());
    } catch (NoSuchAlgorithmException
        | NoSuchProviderException
        | InvalidKeyException
        | CertificateException
        | SignatureException
        | NoSuchMethodError
        | VerifyError
        | NoClassDefFoundError
        | IOException e) {
      throw new CertificateGenerationUnsupportedException(
          "Your runtime does not support generating certificates at runtime", e);
    }
  }

  private static X509CertImpl selfSign(X509CertInfo info, PrivateKey privateKey, String sigAlg)
      throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
          NoSuchProviderException, SignatureException {
    X509CertImpl certificate = new X509CertImpl(info);
    certificate.sign(privateKey, sigAlg);
    return certificate;
  }

  private static CertificateExtensions certificateAuthorityExtensions(PublicKey publicKey) {
    try {
      KeyIdentifier keyId = new KeyIdentifier(publicKey);
      byte[] keyIdBytes = keyId.getIdentifier();
      CertificateExtensions extensions = new CertificateExtensions();
      extensions.set(
          AuthorityKeyIdentifierExtension.NAME,
          new AuthorityKeyIdentifierExtension(keyId, null, null));

      extensions.set(
          BasicConstraintsExtension.NAME, new BasicConstraintsExtension(true, Integer.MAX_VALUE));

      KeyUsageExtension keyUsage = new KeyUsageExtension(new boolean[7]);
      keyUsage.set(KeyUsageExtension.KEY_CERTSIGN, true);
      keyUsage.set(KeyUsageExtension.CRL_SIGN, true);
      extensions.set(KeyUsageExtension.NAME, keyUsage);

      extensions.set(
          SubjectKeyIdentifierExtension.NAME, new SubjectKeyIdentifierExtension(keyIdBytes));

      return extensions;
    } catch (IOException e) {
      return throwUnchecked(e, null);
    }
  }

  public X509Certificate[] certificateChain() {
    return certificateChain;
  }

  public PrivateKey key() {
    return key;
  }

  CertChainAndKey generateCertificate(String keyType, SNIHostName hostName)
      throws CertificateGenerationUnsupportedException {
    try {
      KeyPair pair = generateKeyPair(keyType);
      String sigAlg = "SHA256With" + keyType;
      X509CertInfo info =
          makeX509CertInfo(
              sigAlg,
              hostName.getAsciiName(),
              Period.ofYears(1),
              pair.getPublic(),
              subjectAlternativeName(hostName));

      X509CertImpl certificate = sign(info);

      X509Certificate[] fullChain = prepend(certificate, certificateChain);
      return new CertChainAndKey(fullChain, pair.getPrivate());
    } catch (NoSuchAlgorithmException
        | NoSuchProviderException
        | InvalidKeyException
        | CertificateException
        | SignatureException
        | NoSuchMethodError
        | VerifyError
        | NoClassDefFoundError
        | IOException e) {
      throw new CertificateGenerationUnsupportedException(
          "Your runtime does not support generating certificates at runtime", e);
    }
  }

  private X509CertImpl sign(X509CertInfo info)
      throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException,
          NoSuchProviderException, SignatureException {
    X509Certificate issuerCertificate = certificateChain[0];
    info.set(X509CertInfo.ISSUER, issuerCertificate.getSubjectDN());

    X509CertImpl certificate = new X509CertImpl(info);
    certificate.sign(key, issuerCertificate.getSigAlgName());
    return certificate;
  }

  private static KeyPair generateKeyPair(String keyType) throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyType);
    keyGen.initialize(2048, new SecureRandom());
    return keyGen.generateKeyPair();
  }

  private static X509CertInfo makeX509CertInfo(
      String sigAlg,
      String subjectName,
      Period validity,
      PublicKey publicKey,
      CertificateExtensions certificateExtensions)
      throws IOException, CertificateException, NoSuchAlgorithmException {
    ZonedDateTime start = ZonedDateTime.now();
    ZonedDateTime end = start.plus(validity);

    X500Name myname = new X500Name("CN=" + subjectName);
    X509CertInfo info = new X509CertInfo();
    // Add all mandatory attributes
    info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
    info.set(
        X509CertInfo.SERIAL_NUMBER,
        new CertificateSerialNumber(new java.util.Random().nextInt() & 0x7fffffff));
    info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(AlgorithmId.get(sigAlg)));
    info.set(X509CertInfo.SUBJECT, myname);
    info.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
    info.set(
        X509CertInfo.VALIDITY,
        new CertificateValidity(Date.from(start.toInstant()), Date.from(end.toInstant())));
    info.set(X509CertInfo.ISSUER, myname);
    info.set(X509CertInfo.EXTENSIONS, certificateExtensions);
    return info;
  }

  private static CertificateExtensions subjectAlternativeName(SNIHostName hostName) {
    GeneralName name = new GeneralName(dnsName(hostName));
    GeneralNames names = new GeneralNames();
    names.add(name);
    try {
      CertificateExtensions extensions = new CertificateExtensions();
      extensions.set(
          SubjectAlternativeNameExtension.NAME, new SubjectAlternativeNameExtension(names));
      return extensions;
    } catch (IOException e) {
      // it's an in memory op, should be impossible...
      return throwUnchecked(e, null);
    }
  }

  private static DNSName dnsName(SNIHostName name) {
    try {
      return new DNSName(name.getAsciiName());
    } catch (IOException e) {
      // DNSName throws IOException for a parse error (which isn't an IO problem...)
      // An SNIHostName should be guaranteed not to have a parse issue
      return throwUnchecked(e, null);
    }
  }
}
