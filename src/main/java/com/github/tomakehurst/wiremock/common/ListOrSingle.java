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

import static java.util.Arrays.asList;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonSerialize(using = ListOrSingleSerialiser.class)
@JsonDeserialize(using = ListOrStringDeserialiser.class)
public class ListOrSingle<T> extends ArrayList<T> {

  public ListOrSingle(Collection<? extends T> c) {
    super(c);
  }

  public ListOrSingle(T... items) {
    this(asList(items));
  }

  @Override
  public String toString() {
    return size() > 0 ? get(0).toString() : "";
  }

  public static <T> ListOrSingle<T> of(T... items) {
    return new ListOrSingle<>(items);
  }

  public static <T> ListOrSingle<T> of(List<T> items) {
    return new ListOrSingle<>(items);
  }

  public T getFirst() {
    return get(0);
  }

  public T getLast() {
    return get(size() - 1);
  }

  @Override
  public T get(int index) {
    if (index < 0) {
      index = size() - 1 + index;
    }

    return super.get(index);
  }

  public boolean isSingle() {
    return size() == 1;
  }
}
