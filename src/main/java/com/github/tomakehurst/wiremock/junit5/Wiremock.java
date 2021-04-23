package com.github.tomakehurst.wiremock.junit5;

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.junit5.ConfigurationFactory.DefaultConfigurationFactory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for JUnit 5 injection.
 *
 * @author patouche - 11/03/2021
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface Wiremock {

    /**
     * Wiremock server port. By default, this value is specified to {@link Options#DYNAMIC_PORT} values which mean than
     * dynamic port will be enable.
     *
     * @return the port number.
     */
    int port() default Options.DYNAMIC_PORT;

    /**
     * Enable verbose mode.
     *
     * @return true if the verbose mode should be enable. false otherwise
     */
    boolean verbose() default false;

    /**
     * In there have been unmatched requests during the test, the {@link WireMockExtension} will throw a {@link
     * com.github.tomakehurst.wiremock.client.VerificationException} after the test.
     *
     * @return true if the server should fail on unmatched request. false otherwise.
     */
    boolean failOnUnmatchedRequests() default true;

    /**
     * The factory for creating server options. The default {@link ConfigurationFactory} use will support the parameters
     * provided in the {@link Wiremock} annotation.
     *
     * @return the factory class
     */
    Class<? extends ConfigurationFactory> factory() default DefaultConfigurationFactory.class;

    /**
     * Customizers for {@link com.github.tomakehurst.wiremock.WireMockServer}.
     * <p/>
     * TODO: Should probably be mode into annotation
     *
     * @return an array of classes to customize the {@link com.github.tomakehurst.wiremock.WireMockServer}
     */
    Class<? extends ServerCustomizer>[] customizers() default {};

    /**
     * Not implemented.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
    @interface Customize {
        Class<? extends ServerCustomizer>[] value() default {};
    }

}
