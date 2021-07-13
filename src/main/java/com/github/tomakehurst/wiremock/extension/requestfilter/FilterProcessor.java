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
package com.github.tomakehurst.wiremock.extension.requestfilter;

import com.github.tomakehurst.wiremock.http.Request;
import java.util.List;

public class FilterProcessor {

  public static RequestFilterAction processFilters(
      Request request,
      List<? extends RequestFilter> requestFilters,
      RequestFilterAction lastAction) {
    if (requestFilters.isEmpty()) {
      return lastAction;
    }

    RequestFilterAction action = requestFilters.get(0).filter(request);

    if (action instanceof ContinueAction) {
      Request newRequest = ((ContinueAction) action).getRequest();
      return processFilters(newRequest, requestFilters.subList(1, requestFilters.size()), action);
    }

    return action;
  }
}
