package com.github.tomakehurst.wiremock.junit5;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Wire mock customizer.
 *
 * @author patouche - 13/03/2021
 */
public interface ServerCustomizer {

    /**
     * Customization of a {@link WireMockServer} to improve re-usability.
     *
     * @param server the wire mock server to customize.
     */
    void customize(WireMockServer server);

}
