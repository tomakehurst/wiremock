package com.github.tomakehurst.wiremock.junit5;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtensionTest.SampleTestInstance1.SampleTestInstance2;
import com.github.tomakehurst.wiremock.junit5.WireMockExtensionTest.SampleTestInstance1.SampleTestInstance2.SampleTestInstance3;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author patouche - 20/03/2021
 */
@ExtendWith(MockitoExtension.class)
public class WireMockExtensionTest {

    static class SampleTestInstance1 {
        @Wiremock
        private WireMockServer server1;

        class SampleTestInstance2 {

            @Wiremock
            private WireMockServer server2;

            class SampleTestInstance3 {
                @Wiremock
                private WireMockServer server3;

            }

            public SampleTestInstance3 instance3() {
                return new SampleTestInstance3();
            }
        }

        public SampleTestInstance2 instance2() {
            return new SampleTestInstance2();
        }

    }

    private static class FakeTestInstances implements TestInstances {

        private final List<Object> instances;

        public FakeTestInstances(final Object... instances) {
            this.instances = Arrays.asList(instances);
        }

        @Override
        public Object getInnermostInstance() {
            return null;
        }

        @Override
        public List<Object> getEnclosingInstances() {
            return null;
        }

        @Override
        public List<Object> getAllInstances() {
            return this.instances;
        }

        @Override
        public <T> Optional<T> findInstance(Class<T> requiredType) {
            return Optional.empty();
        }
    }

    private static class FakeStore implements ExtensionContext.Store {
        private final Map<Object, Object> objects = new HashMap<>();

        @Override
        public Object get(Object key) {
            return objects.get(key);
        }

        @Override
        public <V> V get(Object key, Class<V> requiredType) {
            return requiredType.cast(get(key));
        }

        @Override
        public <K, V> Object getOrComputeIfAbsent(K key, Function<K, V> defaultCreator) {
            if (!objects.containsKey(key)) {
                put(key, defaultCreator.apply(key));
            }
            return get(key);
        }

        @Override
        public <K, V> V getOrComputeIfAbsent(K key, Function<K, V> defaultCreator, Class<V> requiredType) {
            return requiredType.cast(getOrComputeIfAbsent(key, defaultCreator));
        }

        @Override
        public void put(Object key, Object value) {
            objects.put(key, value);
        }

        @Override
        public Object remove(Object key) {
            return objects.remove(key);
        }

        @Override
        public <V> V remove(Object key, Class<V> requiredType) {
            return requiredType.cast(remove(key));
        }
    }

    static Wiremock getAnnotation(Class<?> clazz, String fieldName) {
        final Field field = FieldUtils.getDeclaredField(clazz, fieldName, true);
        return field.getAnnotation(Wiremock.class);
    }

    @Nested
    class SupportsParameterTest {

        @Mock
        private ParameterContext parameterContext;

        @Mock
        private ExtensionContext extensionContext;

        @Test
        void should_return_false_when_parameter_is_not_annotated_with_Wiremock() {
            // GIVEN
            final WireMockExtension extension = new WireMockExtension();
            when(parameterContext.isAnnotated(Wiremock.class)).thenReturn(false);

            // WHEN
            final boolean result = extension.supportsParameter(parameterContext, extensionContext);

            // THEN
            Assertions.assertFalse(result);
        }

        @Test
        void should_return_true_when_parameter_is_annotated_with_Wiremock() {
            // GIVEN
            final WireMockExtension extension = new WireMockExtension();
            when(parameterContext.isAnnotated(Wiremock.class)).thenReturn(true);

            // WHEN
            final boolean result = extension.supportsParameter(parameterContext, extensionContext);

            // THEN
            Assertions.assertTrue(result);
        }
    }

    @Nested
    class ResolveParameterTest {

        @Mock
        private ParameterContext parameterContext;
        @Mock
        private ExtensionContext extensionContext;

        @Test
        void should_create_server_for_field() {
            // GIVEN
            final WireMockExtension extension = new WireMockExtension();
            final Wiremock server1 = getAnnotation(SampleTestInstance1.class, "server1");
            final FakeStore store = new FakeStore();

            when(parameterContext.findAnnotation(Wiremock.class)).thenReturn(Optional.of(server1));
            when(extensionContext.getStore(any())).thenReturn(store);

            // WHEN
            final Object result = extension.resolveParameter(parameterContext, extensionContext);

            // THEN
            Assertions.assertNotNull(result);
            Assertions.assertTrue(result instanceof WireMockServer);
            final WireMockServer resultServer = (WireMockServer) result;
            Assertions.assertTrue(resultServer.isRunning());
            Assertions.assertTrue(store.objects.containsKey("servers"));
            Assertions.assertEquals(1, store.get("servers", Collection.class).size());
            resultServer.shutdown();
        }
    }

    @Nested
    class BeforeEachTest {

        @Mock
        private ExtensionContext extensionContext;

        @Test
        void should_inject_all_fields_annotated_with_Wiremock_annotation() throws Exception {
            // GIVEN
            final WireMockExtension extension = new WireMockExtension();

            final SampleTestInstance1 instance1 = new SampleTestInstance1();
            final SampleTestInstance2 instance2 = instance1.instance2();
            final SampleTestInstance3 instance3 = instance2.instance3();
            FakeTestInstances testInstances = new FakeTestInstances(instance1, instance2, instance3);
            final FakeStore store = new FakeStore();

            when(extensionContext.getRequiredTestInstances()).thenReturn(testInstances);
            when(extensionContext.getStore(any())).thenReturn(store);

            // WHEN
            extension.beforeEach(extensionContext);

            // THEN
            Assertions.assertNotNull(instance1.server1);
            Assertions.assertNotNull(instance2.server2);
            Assertions.assertNotNull(instance3.server3);
            Assertions.assertTrue(instance1.server1.isRunning());
            Assertions.assertTrue(instance2.server2.isRunning());
            Assertions.assertTrue(instance3.server3.isRunning());
            Assertions.assertEquals(1, store.objects.size());
            Assertions.assertTrue(store.objects.containsKey("servers"));
            Assertions.assertEquals(3, store.get("servers", Collection.class).size());
            instance1.server1.shutdown();
            instance2.server2.shutdown();
            instance3.server3.shutdown();
        }

    }

}
