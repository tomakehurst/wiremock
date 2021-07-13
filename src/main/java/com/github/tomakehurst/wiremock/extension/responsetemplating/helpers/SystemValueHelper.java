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

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.SystemKeyAuthoriser;
import java.security.AccessControlException;
import org.apache.commons.lang3.StringUtils;

public class SystemValueHelper extends HandlebarsHelper<Object> {

  private final SystemKeyAuthoriser systemKeyAuthoriser;

  public SystemValueHelper(SystemKeyAuthoriser systemKeyAuthoriser) {
    this.systemKeyAuthoriser = systemKeyAuthoriser;
  }

  @Override
  public String apply(Object context, Options options) {
    String key = options.hash("key", "");
    String type = options.hash("type", "ENVIRONMENT");
    if (StringUtils.isEmpty(key)) {
      return this.handleError("The key cannot be empty");
    }
    if (!systemKeyAuthoriser.isPermitted(key)) {
      return this.handleError("Access to " + key + " is denied");
    }

    String rawValue = "";

    try {
      switch (type) {
        case "ENVIRONMENT":
          rawValue = getSystemEnvironment(key);
          break;
        case "PROPERTY":
          rawValue = getSystemProperties(key);
          break;
      }
      return rawValue;

    } catch (AccessControlException e) {
      return this.handleError("Access to " + key + " is denied");
    }
  }

  private String getSystemEnvironment(final String key) {
    return System.getenv(key);
  }

  private String getSystemProperties(final String key) {
    return System.getProperty(key);
  }
}
