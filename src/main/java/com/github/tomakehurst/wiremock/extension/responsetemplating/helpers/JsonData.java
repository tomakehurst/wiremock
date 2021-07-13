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

import com.github.tomakehurst.wiremock.common.Json;
import java.util.*;

public abstract class JsonData<T> {

  protected abstract String toJsonString();

  @Override
  public String toString() {
    return toJsonString();
  }

  @SuppressWarnings("unchecked")
  static Object create(Object data) {
    if (data instanceof Map) {
      return new MapJsonData((Map<String, Object>) data);
    }

    if (data instanceof List) {
      return new ListJsonData((List<Object>) data);
    }

    return data;
  }

  protected final T data;

  public JsonData(T data) {
    this.data = data;
  }

  public static class MapJsonData extends JsonData<Map<String, Object>>
      implements Map<String, Object> {

    @Override
    protected String toJsonString() {
      return Json.write(data);
    }

    public MapJsonData(Map<String, Object> data) {
      super(data);
    }

    @Override
    public int size() {
      return data.size();
    }

    @Override
    public boolean isEmpty() {
      return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
      return data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return data.containsValue(value);
    }

    @Override
    public Object get(Object key) {
      return data.get(key);
    }

    @Override
    public Object remove(Object key) {
      return data.remove(key);
    }

    @Override
    public void clear() {
      data.clear();
    }

    @Override
    public Set<String> keySet() {
      return data.keySet();
    }

    @Override
    public Collection<Object> values() {
      return data.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      return data.entrySet();
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
      data.putAll(m);
    }

    @Override
    public Object put(String key, Object value) {
      return data.put(key, value);
    }
  }

  public static class ListJsonData extends JsonData<List<Object>> implements List<Object> {

    public ListJsonData(List<Object> data) {
      super(data);
    }

    public int size() {
      return data.size();
    }

    public boolean isEmpty() {
      return data.isEmpty();
    }

    public boolean contains(Object o) {
      return data.contains(o);
    }

    public Iterator<Object> iterator() {
      return data.iterator();
    }

    public Object[] toArray() {
      return data.toArray();
    }

    public <T> T[] toArray(T[] a) {
      return data.toArray(a);
    }

    public boolean add(Object o) {
      return data.add(o);
    }

    public boolean remove(Object o) {
      return data.remove(o);
    }

    public boolean containsAll(Collection<?> c) {
      return data.containsAll(c);
    }

    public boolean addAll(Collection<?> c) {
      return data.addAll(c);
    }

    public boolean addAll(int index, Collection<?> c) {
      return data.addAll(index, c);
    }

    public boolean removeAll(Collection<?> c) {
      return data.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
      return data.retainAll(c);
    }

    public void clear() {
      data.clear();
    }

    @Override
    public boolean equals(Object o) {
      return data.equals(o);
    }

    @Override
    public int hashCode() {
      return data.hashCode();
    }

    public Object get(int index) {
      return data.get(index);
    }

    public Object set(int index, Object element) {
      return data.set(index, element);
    }

    public void add(int index, Object element) {
      data.add(index, element);
    }

    public Object remove(int index) {
      return data.remove(index);
    }

    public int indexOf(Object o) {
      return data.indexOf(o);
    }

    public int lastIndexOf(Object o) {
      return data.lastIndexOf(o);
    }

    public ListIterator<Object> listIterator() {
      return data.listIterator();
    }

    public ListIterator<Object> listIterator(int index) {
      return data.listIterator(index);
    }

    public List<Object> subList(int fromIndex, int toIndex) {
      return data.subList(fromIndex, toIndex);
    }

    @Override
    protected String toJsonString() {
      return String.valueOf(data);
    }
  }
}
