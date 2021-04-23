package com.github.tomakehurst.wiremock.junit5;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;

/**
 * @author patouche - 29/03/2021
 */
public interface ConfigurationFactory {

    /**
     * Create {@link WireMockConfiguration} for the wiremock server.
     *
     * @param wiremock the wiremock annotation
     * @return the wiremock configuration.
     */
    WireMockConfiguration createOptions(Wiremock wiremock);

    /**
     * Default wiremock factory.
     */
    class DefaultConfigurationFactory implements ConfigurationFactory {

        @Override
        public WireMockConfiguration createOptions(Wiremock wiremock) {
            final int port = wiremock.port() > 0 ? wiremock.port() : Options.DYNAMIC_PORT;
            return WireMockConfiguration.options()
                    .port(port)
                    .notifier(new Slf4jNotifier(wiremock.verbose()))
                    .extensions(new ResponseTemplateTransformer(true));
        }
    }
}
