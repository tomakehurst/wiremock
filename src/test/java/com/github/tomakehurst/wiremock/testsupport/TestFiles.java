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
package com.github.tomakehurst.wiremock.testsupport;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.commons.lang3.SystemUtils;

public class TestFiles {

  public static final String TRUST_STORE_PASSWORD = "mytruststorepassword";
  public static final String TRUST_STORE_NAME = getTrustStoreRelativeName();
  public static final String TRUST_STORE_PATH = filePath(TRUST_STORE_NAME);
  public static final String KEY_STORE_PATH = filePath("test-keystore");
  public static final String KEY_STORE_WITH_CA_PATH = filePath("test-keystore-with-ca");

  private static String getTrustStoreRelativeName() {
    return System.getProperty("java.specification.version").equals("1.7")
        ? "test-truststore.jks"
        : "test-truststore.pkcs12";
  }

  public static String defaultTestFilesRoot() {
    return filePath("test-file-root");
  }

  public static String file(String path) {
    try {
      String text = Resources.toString(Resources.getResource(path), Charsets.UTF_8);
      if (SystemUtils.IS_OS_WINDOWS) {
        text = text.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
      }

      return text;
    } catch (IOException e) {
      return throwUnchecked(e, String.class);
    }
  }

  public static String filePath(String path) {
    try {
      return new File(Resources.getResource(path).toURI()).getAbsolutePath();
    } catch (URISyntaxException e) {
      return throwUnchecked(e, String.class);
    }
  }

  public static String sampleWarRootDir() {
    return new File("sample-war").exists() ? "sample-war" : "../sample-war";
  }
}
