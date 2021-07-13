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

import com.google.common.io.Resources;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.Objects;

public class ReadOnlyFileOrClasspathKeyStoreSource extends KeyStoreSource {

  protected final String path;

  public ReadOnlyFileOrClasspathKeyStoreSource(
      String path, String keyStoreType, char[] keyStorePassword) {
    super(keyStoreType, keyStorePassword);
    this.path = path;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected InputStream createInputStream() {
    try {
      if (exists()) {
        return new FileInputStream(path);
      } else {
        try {
          URL pathUrl = new URL(path);
          return pathUrl.openStream();
        } catch (MalformedURLException ignored) {
          return Resources.getResource(path).openStream();
        }
      }
    } catch (IOException e) {
      return throwUnchecked(e, InputStream.class);
    }
  }

  @Override
  public boolean exists() {
    return new File(path).isFile();
  }

  @Override
  public void save(KeyStore keyStore) {}

  public String getPath() {
    return path;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    ReadOnlyFileOrClasspathKeyStoreSource that = (ReadOnlyFileOrClasspathKeyStoreSource) o;
    return path.equals(that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), path);
  }
}
