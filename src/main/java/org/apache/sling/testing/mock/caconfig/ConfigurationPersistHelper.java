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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.management.multiplexer.ConfigurationPersistenceStrategyMultiplexer;
import org.apache.sling.caconfig.spi.ConfigurationCollectionPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistData;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Writes context-aware configuration contains in maps and nested maps via {@link ConfigurationManager} to repository.
 */
class ConfigurationPersistHelper {

    private final ConfigurationManager configManager;
    private final ConfigurationPersistenceStrategyMultiplexer configurationPersistenceStrategy;
    private final Resource contextResource;

    /**
     * @param context Sling context
     * @param contextPath Context path
     */
    ConfigurationPersistHelper(@NotNull SlingContextImpl context, @NotNull String contextPath) {
        configManager = context.getService(ConfigurationManager.class);
        configurationPersistenceStrategy = context.getService(ConfigurationPersistenceStrategyMultiplexer.class);
        contextResource = context.resourceResolver().getResource(contextPath);
        if (contextResource == null) {
            throw new IllegalArgumentException("No resource found at" + contextPath);
        }
    }

    /**
     * Writes configuration parameters using the primary configured persistence
     * provider.
     * @param configName Config name
     * @param values Configuration values
     */
    void writeConfiguration(@NotNull String configName, @NotNull Map<String, Object> values) {
        // write properties of main configuration
        ConfigurationDataParts parts = new ConfigurationDataParts(values);
        configManager.persistConfiguration(contextResource, configName, new ConfigurationPersistData(parts.getValues()));

        // write nested configuration and nested configuration collections
        for (Map.Entry<String,Map<String,Object>> nestedMap : parts.getMaps().entrySet()) {
            writeConfiguration(getNestedConfigName(configName, nestedMap.getKey()), nestedMap.getValue());
        }
        for (Map.Entry<String,Collection<Map<String,Object>>> nestedCollection : parts.getCollections().entrySet()) {
            writeConfigurationCollection(getNestedConfigName(configName, nestedCollection.getKey()), nestedCollection.getValue());
        }
    }

    /**
     * Writes a collection of configuration parameters using the primary configured persistence provider.
     * @param configName Config name
     * @param values Configuration values
     */
    void writeConfigurationCollection(@NotNull String configName, @NotNull Collection<@NotNull Map<String, Object>> values) {
        // split each collection item map in it's parts
        Map<String, ConfigurationDataParts> partsCollection = new LinkedHashMap<>();
        int index = 0;
        for (Map<String, Object> map : values) {
            partsCollection.put("item" + (index++), new ConfigurationDataParts(map));
        }

        // write properties of main configuration collection
        List<ConfigurationPersistData> items = partsCollection.entrySet().stream()
                .map(entry -> new ConfigurationPersistData(entry.getValue().getValues()).collectionItemName(entry.getKey()))
                .collect(Collectors.toList());
        configManager.persistConfigurationCollection(contextResource, configName,
                new ConfigurationCollectionPersistData(items));

        // write nested configuration and nested configuration collections
        for (Map.Entry<String, ConfigurationDataParts> entry : partsCollection.entrySet()) {
            String itemName = entry.getKey();
            ConfigurationDataParts parts = entry.getValue();
            for (Map.Entry<String,Map<String,Object>> nestedMap : parts.getMaps().entrySet()) {
                writeConfiguration(getNestedCollectionItemConfigName(configName, itemName, nestedMap.getKey()), nestedMap.getValue());
            }
            for (Map.Entry<String,Collection<Map<String,Object>>> nestedCollection : parts.getCollections().entrySet()) {
                writeConfigurationCollection(getNestedCollectionItemConfigName(configName, itemName, nestedCollection.getKey()), nestedCollection.getValue());
            }
        }
    }

    private String getConfigName(@NotNull String configName) {
        return StringUtils.defaultString(configurationPersistenceStrategy.getConfigName(configName, null), configName);
    }

    private String getCollectionParentConfigName(@NotNull String configName) {
        return StringUtils.defaultString(configurationPersistenceStrategy.getCollectionParentConfigName(configName, null), configName);
    }

    private String getCollectionItemConfigName(@NotNull String configName) {
        return StringUtils.defaultString(configurationPersistenceStrategy.getCollectionItemConfigName(configName, null), configName);
    }

    private String getNestedConfigName(@NotNull String configName, @NotNull String key) {
        return getConfigName(configName) + "/" + key;
    }

    private String getNestedCollectionItemConfigName(@NotNull String configName, @NotNull String itemName, @NotNull String key) {
        return getCollectionItemConfigName(getCollectionParentConfigName(configName) + "/" + itemName) + "/" + key;
    }

}
