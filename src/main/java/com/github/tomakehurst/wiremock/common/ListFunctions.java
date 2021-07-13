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

import java.util.ArrayList;
import java.util.List;

public final class ListFunctions {

  public static <A, B extends A> Pair<List<A>, List<B>> splitByType(A[] items, Class<B> subType) {
    List<A> as = new ArrayList<>();
    List<B> bs = new ArrayList<>();
    for (A a : items) {
      if (subType.isAssignableFrom(a.getClass())) {
        bs.add((B) a);
      } else {
        as.add(a);
      }
    }
    return new Pair<>(as, bs);
  }

  private ListFunctions() {
    throw new UnsupportedOperationException("Not instantiable");
  }
}
