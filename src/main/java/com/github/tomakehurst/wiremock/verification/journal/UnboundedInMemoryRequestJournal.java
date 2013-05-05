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

package com.github.tomakehurst.wiremock.verification.journal;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.size;

public class UnboundedInMemoryRequestJournal implements RequestJournal {

    private final ConcurrentLinkedQueue<LoggedRequest> requests = new ConcurrentLinkedQueue<LoggedRequest>();

    @Override
    public int countRequestsMatching(RequestPattern requestPattern) {
        return size(filter(requests, matchedBy(requestPattern)));
    }

    @Override
    public List<LoggedRequest> getRequestsMatching(RequestPattern requestPattern) {
        return ImmutableList.copyOf(filter(requests, matchedBy(requestPattern)));
    }

    private Predicate<Request> matchedBy(final RequestPattern requestPattern) {
        return new Predicate<Request>() {
            public boolean apply(Request input) {
                return requestPattern.isMatchedBy(input);
            }
        };
    }

    @Override
    public void requestReceived(Request request, Response response) {
        requests.add(LoggedRequest.createFrom(request));
    }

    @Override
    public void reset() {
        requests.clear();
    }
}
