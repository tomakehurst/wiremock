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
package com.github.tomakehurst.wiremock.admin;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import java.text.ParseException;
import java.util.Date;

public class Conversions {

  public static Integer toInt(QueryParameter parameter) {
    return parameter.isPresent() ? Integer.valueOf(parameter.firstValue()) : null;
  }

  public static Date toDate(QueryParameter parameter) {
    try {
      return parameter.isPresent() ? new ISO8601DateFormat().parse(parameter.firstValue()) : null;
    } catch (ParseException e) {
      throw new IllegalArgumentException(parameter.firstValue() + " is not a valid ISO8601 date");
    }
  }
}
