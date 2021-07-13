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

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.google.common.collect.FluentIterable.from;

import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class Urls {

  public static Map<String, QueryParameter> splitQueryFromUrl(String url) {
    String queryPart =
        url.contains("?") && !url.endsWith("?") ? url.substring(url.indexOf('?') + 1) : null;

    return splitQuery(queryPart);
  }

  public static Map<String, QueryParameter> splitQuery(URI uri) {
    if (uri == null) {
      return Collections.emptyMap();
    }

    return splitQuery(uri.getRawQuery());
  }

  public static Map<String, QueryParameter> splitQuery(String query) {
    if (query == null) {
      return Collections.emptyMap();
    }

    Iterable<String> pairs = Splitter.on('&').split(query);
    ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
    for (String queryElement : pairs) {
      int firstEqualsIndex = queryElement.indexOf('=');
      if (firstEqualsIndex == -1) {
        builder.putAll(decode(queryElement), "");
      } else {
        String key = decode(queryElement.substring(0, firstEqualsIndex));
        String value = decode(queryElement.substring(firstEqualsIndex + 1));
        builder.putAll(key, value);
      }
    }

    return Maps.transformEntries(
        builder.build().asMap(),
        new Maps.EntryTransformer<String, Collection<String>, QueryParameter>() {
          public QueryParameter transformEntry(String key, Collection<String> values) {
            return new QueryParameter(key, ImmutableList.copyOf(values));
          }
        });
  }

  public static String getPath(String url) {
    return url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
  }

  public static String urlToPathParts(URI uri) {
    Iterable<String> uriPathNodes = Splitter.on("/").omitEmptyStrings().split(uri.getPath());
    int nodeCount = Iterables.size(uriPathNodes);

    return nodeCount > 0 ? Joiner.on("-").join(from(uriPathNodes)) : "";
  }

  public static String decode(String encoded) {
    try {
      return URLDecoder.decode(encoded, "utf-8");
    } catch (UnsupportedEncodingException e) {
      return throwUnchecked(e, String.class);
    }
  }

  public static URL safelyCreateURL(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      return throwUnchecked(e, URL.class);
    }
  }
}
