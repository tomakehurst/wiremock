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
package com.github.tomakehurst.wiremock.matching;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class MemoizingMatchResult extends MatchResult {

  private final Supplier<Double> memoizedDistance =
      Suppliers.memoize(
          new Supplier<Double>() {
            @Override
            public Double get() {
              return target.getDistance();
            }
          });

  private final Supplier<Boolean> memoizedExactMatch =
      Suppliers.memoize(
          new Supplier<Boolean>() {
            @Override
            public Boolean get() {
              return target.isExactMatch();
            }
          });

  private final MatchResult target;

  public MemoizingMatchResult(MatchResult target) {
    this.target = target;
  }

  @Override
  public boolean isExactMatch() {
    return memoizedExactMatch.get();
  }

  @Override
  public double getDistance() {
    return memoizedDistance.get();
  }
}
