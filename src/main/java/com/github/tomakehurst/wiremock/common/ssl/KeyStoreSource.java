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

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;

import com.github.tomakehurst.wiremock.common.Source;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Objects;

public abstract class KeyStoreSource implements Source<KeyStore> {

  protected final String keyStoreType;
  protected final char[] keyStorePassword;

  protected KeyStoreSource(String keyStoreType, char[] keyStorePassword) {
    this.keyStoreType = keyStoreType;
    this.keyStorePassword = keyStorePassword;
  }

  public KeyStore load() {
    InputStream instream = null;
    try {
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      instream = createInputStream();
      trustStore.load(instream, keyStorePassword);
      return trustStore;
    } catch (Exception e) {
      return throwUnchecked(e, KeyStore.class);
    } finally {
      if (instream != null) {
        try {
          instream.close();
        } catch (IOException ioe) {
          throwUnchecked(ioe);
        }
      }
    }
  }

  protected abstract InputStream createInputStream();

  public String getKeyStoreType() {
    return keyStoreType;
  }

  public String getKeyStorePassword() {
    return new String(keyStorePassword);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    KeyStoreSource that = (KeyStoreSource) o;
    return keyStoreType.equals(that.keyStoreType)
        && Arrays.equals(keyStorePassword, that.keyStorePassword);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(keyStoreType);
    result = 31 * result + Arrays.hashCode(keyStorePassword);
    return result;
  }
}
