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

import static java.util.Arrays.copyOf;
import static java.util.Arrays.fill;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;

public class Secret implements AutoCloseable {

  private static final char[] EMPTY_VALUE = new char[0];
  private volatile char[] value;

  public Secret(char[] value) {
    requireNonNull(value, "Secret value may not be null");

    this.value = copyOf(value, value.length);
  }

  public Secret(String value) {
    this(null == value ? null : value.toCharArray());
  }

  public char[] getValue() {
    return Arrays.copyOf(value, value.length);
  }

  @Override
  public void close() {
    if (EMPTY_VALUE == value) return;

    char[] tempValue = value;
    value = EMPTY_VALUE;

    fill(tempValue, (char) 0x00);
  }

  public boolean compareTo(String password) {
    if (password == null) {
      return false;
    }

    return Arrays.equals(password.toCharArray(), value);
  }
}
