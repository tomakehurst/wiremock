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
package com.github.tomakehurst.wiremock.extension;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import java.util.Map;

public class ExtensionLoader {

  @SuppressWarnings("unchecked")
  public static <T> Map<String, T> loadExtension(String... classNames) {
    return (Map<String, T>)
        asMap(from(asList(classNames)).transform(toClasses()).transform(toExtensions()));
  }

  public static Map<String, Extension> load(String... classNames) {
    return loadExtension(classNames);
  }

  public static Map<String, Extension> asMap(Iterable<Extension> extensions) {
    return Maps.uniqueIndex(
        extensions,
        new Function<Extension, String>() {
          public String apply(Extension extension) {
            return extension.getName();
          }
        });
  }

  public static Map<String, Extension> load(Class<? extends Extension>... classes) {
    return asMap(from(asList(classes)).transform(toExtensions()));
  }

  private static Function<Class<? extends Extension>, Extension> toExtensions() {
    return new Function<Class<? extends Extension>, Extension>() {
      @SuppressWarnings("unchecked")
      public Extension apply(Class<? extends Extension> extensionClass) {
        try {
          return extensionClass.newInstance();
        } catch (Exception e) {
          return throwUnchecked(e, Extension.class);
        }
      }
    };
  }

  private static Function<String, Class<? extends Extension>> toClasses() {
    return new Function<String, Class<? extends Extension>>() {
      @SuppressWarnings("unchecked")
      public Class<? extends Extension> apply(String className) {
        try {
          return (Class<? extends Extension>) Class.forName(className);
        } catch (ClassNotFoundException e) {
          return throwUnchecked(e, Class.class);
        }
      }
    };
  }

  public static <T extends Extension> Predicate<Map.Entry<String, Extension>> valueAssignableFrom(
      final Class<T> extensionType) {
    return new Predicate<Map.Entry<String, Extension>>() {
      public boolean apply(Map.Entry<String, Extension> input) {
        return extensionType.isAssignableFrom(input.getValue().getClass());
      }
    };
  }
}
