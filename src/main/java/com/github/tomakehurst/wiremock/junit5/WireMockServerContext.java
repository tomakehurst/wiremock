package com.github.tomakehurst.wiremock.junit5;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * @author patouche - 19/03/2021
 */
public class WireMockServerContext {

    private final WireMockServer server;

    private final Wiremock definition;

    public WireMockServerContext(final WireMockServer server, final Wiremock definition) {
        this.server = server;
        this.definition = definition;
    }

    public WireMockServer getServer() {
        return server;
    }

    public Wiremock getDefinition() {
        return definition;
    }
}
