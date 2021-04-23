package com.github.tomakehurst.wiremock.common;

public class RequestResponseId {
    private final String id;
    public RequestResponseId(String id) {
        this.id = id;
    }

    public String value() { return id; }
}
