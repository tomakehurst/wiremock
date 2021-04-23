package com.github.tomakehurst.wiremock.junit5;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wire Mock extension for JUnit 5.
 * <p/>
 * Example using constructor injection :
 * <pre class='code'><code class='java'>
 *  &#064;ExtendWith(WireMockExtension.class)
 *  class SampleTests {
 *
 *      private final WireMockServer server;
 *
 *      SampleTests(&#064;Wiremock(port = 8081) WireMockServer server) {
 *          this.server = server;
 *      }
 *
 *      &#064;Test
 *      void shouldSucceed() {
 *          testClient.get("http://localhost:8081/endpoints"))
 *      }
 *
 *  }
 * </code></pre>
 * <p/>
 * Example using field injection :
 * <pre class='code'><code class='java'>
 *  &#064;ExtendWith(WireMockExtension.class)
 *  class SampleTests {
 *
 *      static SampleCustomizer implements WireMockCustomizer {
 *
 *          public void customize(WireMockServer server) {
 *              server.stubFor(...)
 *          }
 *
 *      }
 *
 *      &#064;Wiremock(customizers = { SampleCustomizer.class })
 *      private WireMockServer server;
 *
 *      &#064;Test
 *      void shouldSucceed() {
 *          testClient.get(format("http://localhost:%d/endpoints", server.port()))
 *      }
 *
 *  }
 * </code></pre>
 *
 * <p>
 * Alternative to:
 * <ul>
 *     <li><a href='https://github.com/lanwen/wiremock-junit5'>wiremock-junit5</a></li>
 *     <li><a href='https://github.com/JensPiegsa/wiremock-extension'>wiremock-extension</a></li>
 *     <li><a href='https://github.com/phxql/wiremock-junit5'>wiremock-junit5</a></li>
 * </ul>
 * <p>
 *
 * @author patouche - 11/03/2021
 */
public class WireMockExtension implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

    /** Wiremock namespace. */
    private static final Namespace WIREMOCK = Namespace.create(WireMockExtension.class);

    /** Servers key store value. */
    private static final String SERVERS = "servers";

    /** {@inheritDoc} */
    @Override
    public boolean supportsParameter(
            final ParameterContext parameterContext,
            final ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.isAnnotated(Wiremock.class);
    }

    /**
     * Create a wiremock server and register it into the junit context.
     *
     * @param context  the extension context
     * @param wiremock the wiremock annotation
     * @return a new instance of a {@link WireMockServer}
     */
    protected static WireMockServer createServer(final ExtensionContext context, final Wiremock wiremock) {
        final ConfigurationFactory factory = ReflectionSupport.newInstance(wiremock.factory());
        final WireMockConfiguration options = factory.createOptions(wiremock);
        final WireMockServer server = new WireMockServer(options);
        server.start();
        Arrays.stream(wiremock.customizers()).map(ReflectionSupport::newInstance).forEach(c -> c.customize(server));
        registeredContextServers(context).add(new WireMockServerContext(server, wiremock));
        return server;
    }

    /** {@inheritDoc} */
    @Override
    public Object resolveParameter(
            final ParameterContext parameterContext,
            final ExtensionContext extensionContext) throws ParameterResolutionException {
        final Wiremock wiremock = parameterContext.findAnnotation(Wiremock.class)
                .orElseThrow(() ->
                        new ParameterResolutionException(
                                "Cannot find annotation @" + Wiremock.class.getSimpleName()
                                        + " on parameter " + parameterContext.getParameter().getName()
                        )
                );
        return createServer(extensionContext, wiremock);
    }

    /**
     * Inject field on a test instance.
     * <p/>
     * We don't use {@link org.junit.jupiter.api.extension.TestInstancePostProcessor} which has been design for fields
     * injection as we want to reset all this fields between all test (which will create a new server)
     *
     * @param testInstance the test instance
     * @param context      the extension context of the running test
     * @throws Exception
     */
    private static void injectFields(final Object testInstance, final ExtensionContext context) throws Exception {
        final List<Field> fields = AnnotationSupport.findAnnotatedFields(testInstance.getClass(), Wiremock.class);
        for (final Field field : fields) {
            final WireMockServer server = createServer(context, field.getAnnotation(Wiremock.class));
            FieldUtils.writeField(field, testInstance, server, true);
        }
    }

    /**
     * Create server using field injection. All fields marked with an annotation {@link Wiremock} will be create a new
     * {@link WireMockServer} which will be injected into the current field.
     * <p/>
     * All fields are retrieve from the store and initialized here because this callback is executed before an
     * individual test and any user-defined setup method.
     * <p/>
     * This allow to have field injection in parent class of the current class under test.
     */
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        final TestInstances testInstances = context.getRequiredTestInstances();
        for (Object testInstance : testInstances.getAllInstances()) {
            injectFields(testInstance, context);
        }
    }

    /**
     * Cleanup all servers previously created and registered in the current {@link ExtensionContext} store.
     * <br>
     * This callback will be invoked after an individual test and any user-defined teardown method have been executed.
     */
    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        final Set<WireMockServerContext> serverContexts = registeredContextServers(context);
        serverContexts.stream().map(WireMockServerContext::getServer).forEach(s -> {
            s.resetRequests();
            s.resetToDefaultMappings();
            s.stop();
        });
    }

    /**
     * Retrieve the current {@link Set} of {@link WireMockServer} registred in the current extension context.
     * <br>
     * If the {@link #SERVERS} store for the {@link #WIREMOCK} store doesn't exist, it will be initialize the first time
     * we will try to retrieve it.
     *
     * @param context the current extension context.
     * @return the {@link Set} of {@link WireMockServer} registered in the current context.
     */
    @SuppressWarnings("unchecked")
    private static Set<WireMockServerContext> registeredContextServers(ExtensionContext context) {
        return context.getStore(WIREMOCK).getOrComputeIfAbsent(SERVERS, (k) -> new HashSet<>(), Set.class);
    }

}
