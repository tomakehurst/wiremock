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

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;

import com.github.jknack.handlebars.Options;
import java.net.InetAddress;

public class HostnameHelper extends HandlebarsHelper<Object> {

  private static String HOSTNAME;

  static {
    try {
      HOSTNAME = InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      notifier()
          .error(
              "Failed to look up localhost. {{hostname}} Handlebars helper will return localhost.",
              e);
      HOSTNAME = "localhost";
    }
  }

  @Override
  public Object apply(Object context, Options options) {
    return HOSTNAME;
  }
}
