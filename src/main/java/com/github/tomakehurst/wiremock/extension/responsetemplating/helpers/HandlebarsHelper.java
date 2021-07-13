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

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.RenderCache;

/**
 * This abstract class is the base for all defined Handlebars helper in wiremock. It basically
 * allows simpler error handling.
 *
 * @param <T> Type used as context for the Handlebars helper.
 */
public abstract class HandlebarsHelper<T> implements Helper<T> {

  public static final String ERROR_PREFIX = "[ERROR: ";
  public static final String ERROR_SUFFIX = "]";

  /**
   * Handle invalid helper data without exception details or because none was thrown.
   *
   * @param message message to log and return
   * @return a message which will be used as content
   */
  protected String handleError(final String message) {
    notifier().error(formatMessage(message));
    return formatMessage(message);
  }

  /**
   * Handle invalid helper data with exception details in the log message.
   *
   * @param message message to log and return
   * @param cause which occurred during application of the helper
   * @return a message which will be used as content
   */
  protected String handleError(final String message, final Throwable cause) {
    notifier().error(formatMessage(message), cause);
    return formatMessage(message);
  }

  /**
   * Handle invalid helper data with exception details in the log message. Also additional
   * information regarding the issue is written in the logs.
   *
   * @param message message to log and return
   * @param logExclusive additional information just for the log
   * @param cause which occured during application of the helper
   * @return a message which will be used as content
   */
  protected String handleError(
      final String message, final String logExclusive, final Throwable cause) {
    notifier().error(ERROR_PREFIX + message + " - " + logExclusive, cause);
    return formatMessage(message);
  }

  private String formatMessage(String message) {
    return ERROR_PREFIX + message + ERROR_SUFFIX;
  }

  protected static RenderCache getRenderCache(Options options) {
    return options.get("renderCache", new RenderCache());
  }
}
