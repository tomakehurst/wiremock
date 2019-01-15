package com.github.tomakehurst.wiremock.extension.plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.Helper;
import com.github.tomakehurst.wiremock.extension.Extension;

public class PluginLoader {

    private static final String CANT_INSTANTIATE_HELPER_ERROR = "Can't instantiate helper %s of type %s";
    private static final String CANT_INSTANTIATE_EXTENSION_ERROR = "Can't instantiate extension %s";
    private static final String CANT_INSTANTIATE_PARAM_ERROR = "Can't instantiate param of type %s with value %s";

    private static final String CANT_FIND_PARAM_CONSTRUCTOR_ERROR = "Can't find constructor for param type %s";
    private static final String CANT_FIND_EXTENSION_CONSTRUCTOR_ERROR = "Can't find apropiate constructor for extension type %s";

    public static final String HELPER_REF = "@helpers";

    private PluginLoader() {

    }

    public static List<Extension> initExtensionsInstances(ExtensionFile extensionFile,
            List<URLClassLoader> jarClassLoaders) {
        // Load all helpers in new object HashMap<String, Helper>
        Map<String, Helper<?>> helpersInstances = initHelpers(extensionFile, jarClassLoaders);
        // Load all extensions and instantiate them, modify param if value = @helpers
        return initExtensions(helpersInstances, extensionFile, jarClassLoaders);
    }

    private static List<Extension> initExtensions(Map<String, Helper<?>> helpersInstances, ExtensionFile extensionFile,
            List<URLClassLoader> jarClassLoaders) {
        List<Extension> extensions = new ArrayList<>();
        for (ExtensionDefinition extensionDefinition : extensionFile.getExtensionList()) {
            extensions.add(initExtension(extensionDefinition, helpersInstances, jarClassLoaders));
        }
        return extensions;
    }

    private static Extension initExtension(ExtensionDefinition extensionDefinition,
            Map<String, Helper<?>> helpersInstances, List<URLClassLoader> jarClassLoaders) {
        Class<Extension> extensionClass = (Class<Extension>) getClassFromAnyLoader(jarClassLoaders,
                extensionDefinition.getExtensionClassname());

        List<Object> initargs = new ArrayList<>();
        for (ArgumentDefinition argumentDefinition : extensionDefinition.getArguments()) {
            if (StringUtils.equalsIgnoreCase(HELPER_REF, argumentDefinition.getValue())) {
                initargs.add(helpersInstances);
            } else {
                initargs.add(instantiateParam(argumentDefinition, jarClassLoaders));
            }
        }

        // Instantiate extension with constructor and params
        try {
            if (initargs.isEmpty()) {
                return extensionClass.newInstance();
            } else {
                Constructor<Extension> extensionConstructor = extractConstructor(extensionClass,
                        extensionDefinition.getArguments(), jarClassLoaders);
                Object[] argArray = new Object[initargs.size()];
                return extensionConstructor.newInstance(initargs.toArray(argArray));
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException
                | IllegalArgumentException e) {
            throw new PluginInitializationException(
                    String.format(CANT_INSTANTIATE_EXTENSION_ERROR, extensionDefinition.getExtensionClassname()), e);
        }

    }

    private static Constructor<Extension> extractConstructor(Class<Extension> extensionClass,
            List<ArgumentDefinition> arguments, List<URLClassLoader> jarClassLoaders) {

        Class<?>[] parameterTypes = new Class[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            parameterTypes[i] = getClassFromAnyLoader(jarClassLoaders, arguments.get(i).getType());
        }
        try {
            return extensionClass.getConstructor(parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new PluginInitializationException(
                    String.format(CANT_FIND_EXTENSION_CONSTRUCTOR_ERROR, extensionClass.getName()), e);
        }
    }

    private static Object instantiateParam(ArgumentDefinition definition, List<URLClassLoader> jarClassLoaders) {
        Class paramClass = getClassFromAnyLoader(jarClassLoaders, definition.getType());
        if (paramClass.isPrimitive()) {
            paramClass = ClassUtils.primitiveToWrapper(paramClass);
        }
        Constructor<Object> constructor;
        try {
            constructor = paramClass.getConstructor(String.class);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new PluginInitializationException(
                    String.format(CANT_FIND_PARAM_CONSTRUCTOR_ERROR, definition.getType()), e);
        }
        try {
            return constructor.newInstance(definition.getValue());
        } catch (Exception ex) {
            throw new PluginInitializationException(
                    String.format(CANT_INSTANTIATE_PARAM_ERROR, definition.getType(), definition.getValue()), ex);
        }

    }

    private static Map<String, Helper<?>> initHelpers(ExtensionFile extensionFile,
            List<URLClassLoader> jarClassLoaders) {
        Map<String, Helper<?>> instantiatedHelpers = new HashMap<>();
        for (HelperDefinition helperDefinition : extensionFile.getHelpers()) {
            Class<Helper<?>> helperClass = (Class<Helper<?>>) getClassFromAnyLoader(jarClassLoaders,
                    helperDefinition.getHelperClass());
            try {
                instantiatedHelpers.put(helperDefinition.getHelperName(), helperClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new PluginInitializationException(String.format(CANT_INSTANTIATE_HELPER_ERROR,
                        helperDefinition.getHelperName(), helperDefinition.getClass()), e);
            }
        }
        return instantiatedHelpers;
    }

    private static Class<?> getClassFromAnyLoader(List<URLClassLoader> jarClassLoaders, String className) {
        try {
            return ClassUtils.getClass(className);
        } catch (ClassNotFoundException e) {
            for (URLClassLoader urlClassLoader : jarClassLoaders) {
                try {
                    return ClassUtils.getClass(urlClassLoader, className, true);
                } catch (ClassNotFoundException e1) {
                    // Nothing to do, just iterate over next classLoader
                }
            }
        }
        throw new PluginInitializationException("Plugin loader can't found class " + className);
    }
}
