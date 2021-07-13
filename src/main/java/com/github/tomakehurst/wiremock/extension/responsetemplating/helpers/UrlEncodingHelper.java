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
package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class UrlEncodingHelper implements Helper<Object> {

  @Override
  public Object apply(Object context, Options options) throws IOException {
    Object encodingObj = options.hash.get("encoding");
    String encoding = encodingObj != null ? encodingObj.toString() : "utf-8";
    if (Boolean.TRUE.equals(options.hash.get("decode"))) {
      return decode(context.toString(), encoding);
    }

    return encode(context.toString(), encoding);
  }

  private String encode(String value, String encoding) throws IOException {
    try {
      return URLEncoder.encode(value, encoding);
    } catch (UnsupportedEncodingException e) {
      throw new IOException(e);
    }
  }

  private String decode(String value, String encoding) throws IOException {
    try {
      return URLDecoder.decode(value, encoding);
    } catch (UnsupportedEncodingException e) {
      throw new IOException(e);
    }
  }
}
