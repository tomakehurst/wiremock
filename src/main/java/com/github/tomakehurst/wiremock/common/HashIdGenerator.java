/*
 * Copyright (C) 2011 Thomas Akehurst
 * Copyright (C) 2020 Mark Salisbury
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

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.CaseInsensitiveKey;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.core.Options.FileIdMethod.*;

public class HashIdGenerator implements IdGenerator {

    private final Options.FileIdMethod method;
    private final Set<CaseInsensitiveKey> hashHeadersToIgnore;

    public static class HashRequestResponseId extends RequestResponseId {
        public final HashDetails hashDetails;
        private HashRequestResponseId(String id, HashDetails details) {
            super(id);
            this.hashDetails = details;
        }
    }

    public static class HashDetails {
        public Map<String, Object> request = null;
        public Map<String, Object> response = null;
    };

    public HashIdGenerator(Options.FileIdMethod method) {
        this(method, ImmutableSet.of());
    }

    public HashIdGenerator(Options.FileIdMethod method, Set<String> hashHeadersToIgnore) {
        if (method != REQUEST_HASH && method != RESPONSE_HASH && method != REQUEST_RESPONSE_HASH)
            throw new IllegalArgumentException("FileIdMethod is not supported: " + method);
        this.method = method;
        this.hashHeadersToIgnore = hashHeadersToIgnore.stream().map(key -> new CaseInsensitiveKey(key)).collect(Collectors.toSet());
    }

    @Override
    public HashRequestResponseId generate(Request request, Response response, byte[] bodyBytes) {
        // Explicitly pull properties from the request/response instead of relying on toString()
        // to be stable and implemented consistently between different implementations of the
        // interface.
        HashDetails hashDetails = new HashDetails();

        if (method == REQUEST_HASH || method == REQUEST_RESPONSE_HASH) {
            hashDetails.request = new TreeMap<>();

            // GET, POST, etc.
            hashDetails.request.put("method", request.getMethod().getName());

            // /some/api/endpoint?query=blah&k=etc
            hashDetails.request.put("url", request.getUrl());

            hashDetails.request.put("headers", request.getHeaders().excluding(hashHeadersToIgnore));

            // ALL the cookies in the request
            String[] cookies = request.getCookies().keySet().toArray(new String[0]);
            Arrays.sort(cookies);
            hashDetails.request.put("cookies", cookies);

            byte[] requestBody = request.getBody();
            hashDetails.request.put("bodyHash", requestBody != null ? Arrays.hashCode(requestBody) : null);
            // it's OK to truncate as we're including a hash of the actual data
            hashDetails.request.put("body", truncateStringIfNecessary(request.getBodyAsString(), 500));

            // If this is a multi-part (form upload) request, add each part to the buffer
            Collection<Request.Part> parts = request.getParts();
            if (parts != null) {
                List<Map<String, Object>> multiParts = new ArrayList<>();
                for (Request.Part part : parts) {
                    Map<String, Object> partDetails = new TreeMap<>();
                    partDetails.put("name", part.getName());
                    partDetails.put("headers", part.getHeaders());
                    byte[] partBytes = (part.getBody() != null && part.getBody().asBytes() != null) ? part.getBody().asBytes() : null;
                    partDetails.put("bodyHash", partBytes != null ? Arrays.hashCode(partBytes) : null);
                    multiParts.add(partDetails);
                }
                hashDetails.request.put("multiparts", multiParts);
            }
        }
        if (method == RESPONSE_HASH || method == REQUEST_RESPONSE_HASH) {
            hashDetails.response = new TreeMap<>();

            // 200, 404, 500, etc.
            hashDetails.response.put("status", response.getStatus());
            if (response.getStatusMessage() != null)
                hashDetails.response.put("message", response.getStatusMessage());

            hashDetails.response.put("headers", response.getHeaders().excluding(hashHeadersToIgnore));

            hashDetails.response.put("bodyHash", bodyBytes != null ? Arrays.hashCode(bodyBytes) : null);
            // it's OK to truncate as we're including a hash of the actual data
            hashDetails.response.put("body", truncateStringIfNecessary(response.getBodyAsString(), 500));
        }

        String json = Json.write(hashDetails);
        // Convert to an "unsigned" int to ensure there are no negative values
        // output.
        String hashCode = String.format("%08X", (long)json.hashCode() - Integer.MIN_VALUE);
        return new HashRequestResponseId(hashCode, hashDetails);
    }

    private String truncateStringIfNecessary(String bodyAsString, int maxCharacters) {
        if (bodyAsString == null)
            return null;
        if (bodyAsString.length() <= maxCharacters)
            return bodyAsString;
        int truncated = bodyAsString.length() - maxCharacters;
        return bodyAsString.substring(0, maxCharacters) + "... (" + truncated + " characters have been truncated)";
    }


}
