/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.caconfig;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Helps setting up a mock environment for Context-Aware Configuration.
 */
@ProviderType
public final class MockContextAwareConfig {

    private MockContextAwareConfig() {
        // static methods only
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Configuration annotation.
     * @param context Sling context
     * @param classNames Java class names
     */
    public static void registerAnnotationClasses(@NotNull SlingContextImpl context, @NotNull String @NotNull ... classNames) {
        ConfigurationMetadataUtil.registerAnnotationClasses(context.bundleContext(), classNames);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Configuration annotation.
     * @param context Sling context
     * @param classes Java classes
     */
    public static void registerAnnotationClasses(@NotNull SlingContextImpl context, @NotNull Class @NotNull ... classes) {
        ConfigurationMetadataUtil.registerAnnotationClasses(context.bundleContext(), classes);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Configuration annotation.
     * @param context Sling context
     * @param packageNames Java package names
     */
    public static void registerAnnotationPackages(@NotNull SlingContextImpl context, @NotNull String @NotNull ... packageNames) {
        Collection<Class> classes = ConfigurationMetadataUtil.getConfigurationClassesForPackages(StringUtils.join(packageNames, ","));
        registerAnnotationClasses(context, classes.toArray(new Class[classes.size()]));
    }

    /**
     * Writes configuration parameters using the primary configured persistence
     * provider.
     * @param context Sling context
     * @param contextPath Context path
     * @param configClass Configuration class
     * @param values Configuration values
     */
    public static void writeConfiguration(@NotNull SlingContextImpl context, @NotNull String contextPath, @NotNull Class<?> configClass,
            @NotNull Map<String, Object> values) {
        writeConfiguration(context, contextPath, getConfigurationName(configClass), values);
    }

    /**
     * Writes configuration parameters using the primary configured persistence
     * provider.
     * @param context Sling context
     * @param contextPath Context path
     * @param configName Config name
     * @param values Configuration values
     */
    public static void writeConfiguration(@NotNull SlingContextImpl context, @NotNull String contextPath, @NotNull String configName,
            @NotNull Map<String, Object> values) {
        ConfigurationPersistHelper helper = new ConfigurationPersistHelper(context, contextPath);
        helper.writeConfiguration(configName, values);
    }

    /**
     * Writes configuration parameters using the primary configured persistence
     * provider.
     * @param context Sling context
     * @param contextPath Context path
     * @param configClass Configuration class
     * @param values Configuration values
     */
    public static void writeConfiguration(@NotNull SlingContextImpl context, @NotNull String contextPath, Class<?> configClass, @NotNull Object @NotNull ... values) {
        writeConfiguration(context, contextPath, getConfigurationName(configClass), values);
    }

    /**
     * Writes configuration parameters using the primary configured persistence
     * provider.
     * @param context Sling context
     * @param contextPath Context path
     * @param configName Config name
     * @param values Configuration values
     */
    public static void writeConfiguration(@NotNull SlingContextImpl context, @NotNull String contextPath, @NotNull String configName, @NotNull Object @NotNull ... values) {
        writeConfiguration(context, contextPath, configName, MapUtil.toMap(values));
    }

    /**
     * Writes a collection of configuration parameters using the primary
     * configured persistence provider.
     * @param context Sling context
     * @param contextPath Context path
     * @param configClass Configuration class
     * @param values Configuration values
     */
    public static void writeConfigurationCollection(@NotNull SlingContextImpl context, @NotNull String contextPath,  @NotNull Class<?> configClass,
            @NotNull Collection<@NotNull Map<String, Object>> values) {
        writeConfigurationCollection(context, contextPath, getConfigurationName(configClass), values);
    }

    /**
     * Writes a collection of configuration parameters using the primary
     * configured persistence provider.
     * @param context Sling context
     * @param contextPath Context path
     * @param configName Config name
     * @param values Configuration values
     */
    public static void writeConfigurationCollection(@NotNull SlingContextImpl context, @NotNull String contextPath, @NotNull String configName,
            @NotNull Collection<@NotNull Map<String, Object>> values) {
        ConfigurationPersistHelper helper = new ConfigurationPersistHelper(context, contextPath);
        helper.writeConfigurationCollection(configName, values);
    }

    @SuppressWarnings("null")
    private static @NotNull String getConfigurationName(Class<?> configClass) {
        Configuration annotation = configClass.getAnnotation(Configuration.class);
        if (annotation != null && StringUtils.isNotBlank(annotation.name())) {
            return annotation.name();
        }
        return configClass.getName();
    }

}
