package com.github.tomakehurst.wiremock.recording;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.google.common.base.Function;
import java.util.Map;
import java.util.Set;

public class CaptureAllHeadersRequestTransformer implements Function<Request, RequestPatternBuilder> {
    private final RequestBodyPatternFactory bodyPatternFactory;
    private final Map<String, CaptureHeadersSpec> headersBlacklist;

    public CaptureAllHeadersRequestTransformer(Map<String, CaptureHeadersSpec> headersBlacklist
        , RequestBodyPatternFactory bodyPatternFactory) {
        this.headersBlacklist = headersBlacklist;
        this.bodyPatternFactory = bodyPatternFactory;
    }

    @Override
    public RequestPatternBuilder apply(Request request) {
        RequestPatternBuilder builder =
            new RequestPatternBuilder(request.getMethod(), WireMock.urlEqualTo(request.getUrl()));

        Set<String> headersToCapture = getHeadersToCapture(request.getAllHeaderKeys());

        for (String header : headersToCapture) {
            builder.withHeader(header, new EqualToPattern(request.getHeader(header), true));
        }

        byte[] body = request.getBody();
        if (bodyPatternFactory != null && body != null && body.length > 0) {
            builder.withRequestBody(bodyPatternFactory.forRequest(request));
        }
        return builder;
    }

    private Set<String> getHeadersToCapture(Set<String> requestHeaders) {
        if (headersBlacklist != null) {
            for (Map.Entry<String, CaptureHeadersSpec> headerBlackListEntry : headersBlacklist.entrySet()) {
                String blacklistedHeaderName = headerBlackListEntry.getKey();

                if (headerBlackListEntry.getValue().getCaseInsensitive()) {
                    blacklistedHeaderName = blacklistedHeaderName.toLowerCase();
                    String toRemove = null;
                    for (String header : requestHeaders) {
                        if (header.toLowerCase().equals(blacklistedHeaderName)) {
                            toRemove = header;
                            break;
                        }
                    }
                    if (toRemove != null) {
                        requestHeaders.remove(toRemove);
                    }
                } else {
                    requestHeaders.remove(blacklistedHeaderName);
                }
            }
        }

        return requestHeaders;
    }
}
