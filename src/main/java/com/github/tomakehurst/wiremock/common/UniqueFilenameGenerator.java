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

import com.github.tomakehurst.wiremock.http.Request;
import com.google.common.base.Joiner;
import okhttp3.HttpUrl;

import java.net.URI;
import java.util.LinkedList;

import static com.google.common.base.Splitter.on;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.size;
import static java.lang.Math.min;

public class UniqueFilenameGenerator {

    public static String generate(Request request, String prefix, String id) {
        return generate(request, prefix, id, "json");
    }

    public static String generate(Request request, String prefix, String id, String extension) {
        LinkedList<String> uriPathNodes = new LinkedList<>(HttpUrl.parse(request.getAbsoluteUrl()).pathSegments());
        uriPathNodes.remove("");
        int nodeCount = size(uriPathNodes);

        String pathPart = nodeCount > 0 ?
                sanitise(
                    Joiner.on("-")
                    .join(from(uriPathNodes)
                        .skip(nodeCount - min(nodeCount, 2))
                    )
                ):
                "(root)";


        return new StringBuilder(prefix)
                .append("-")
                .append(pathPart)
                .append("-")
                .append(id)
                .append(".")
                .append(extension)
                .toString();
    }

    private static String sanitise(String input) {
        return input.replaceAll("[,~:/?#\\[\\]@!\\$&'()*+;=]", "_");
    }
}
