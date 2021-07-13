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
package com.github.tomakehurst.wiremock.common;

import com.google.common.io.BaseEncoding;

public class GuavaBase64Encoder implements Base64Encoder {

  @Override
  public String encode(byte[] content) {
    return encode(content, true);
  }

  @Override
  public String encode(byte[] content, boolean padding) {
    BaseEncoding encoder = BaseEncoding.base64();
    if (!padding) {
      encoder = encoder.omitPadding();
    }
    return encoder.encode(content);
  }

  @Override
  public byte[] decode(String base64) {
    return BaseEncoding.base64().decode(base64);
  }
}
