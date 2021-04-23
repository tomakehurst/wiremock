package com.github.tomakehurst.wiremock.junit5;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit5.ConfigurationFactory.DefaultConfigurationFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author patouche - 30/03/2021
 */
@DisplayName("DefaultConfigurationFactory#createOptions")
class DefaultConfigurationFactoryTest {

    private static final DefaultConfigurationFactory FACTORY = new DefaultConfigurationFactory();

    @Wiremock
    private WireMockServer annotationDefault;

    private static Wiremock getAnnotation(final String fieldName) {
        return WireMockExtensionTest.getAnnotation(DefaultConfigurationFactoryTest.class, fieldName);
    }

    @Test
    void should_create_a_new_default_configuration() {
        // GIVEN
        Wiremock annotation = getAnnotation("annotationDefault");

        // WHEN
        final WireMockConfiguration options = FACTORY.createOptions(annotation);

        // THEN
        Assertions.assertEquals(0, options.portNumber());
        Assertions.assertTrue(options.notifier() instanceof Slf4jNotifier);
        Assertions.assertEquals(1, options.extensionsOfType(ResponseTemplateTransformer.class).size());
    }
}
