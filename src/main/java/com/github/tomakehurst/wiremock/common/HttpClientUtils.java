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

import static com.google.common.base.Charsets.UTF_8;

import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

public class HttpClientUtils {

  public static String getEntityAsStringAndCloseStream(HttpResponse httpResponse) {
    HttpEntity entity = httpResponse.getEntity();
    if (entity != null) {
      try {
        String content = EntityUtils.toString(entity, UTF_8.name());
        entity.getContent().close();
        return content;
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }

    return null;
  }

  public static byte[] getEntityAsByteArrayAndCloseStream(HttpResponse httpResponse) {
    HttpEntity entity = httpResponse.getEntity();
    if (entity != null) {
      try {
        byte[] content = EntityUtils.toByteArray(entity);
        entity.getContent().close();
        return content;
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }

    return null;
  }
}
