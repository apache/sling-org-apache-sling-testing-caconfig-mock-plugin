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
package org.apache.sling.testing.mock.caconfig.persistence;

import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.spi.ConfigurationCollectionPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceException;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.junit.Assert.assertNotNull;

/**
 * This is a variant of {@link org.apache.sling.caconfig.impl.def.DefaultConfigurationPersistenceStrategy}
 * which reads and stores data from a sub-resources named "jcr:content".
 *
 * Difference to {@link CustomConfigurationPersistenceStrategy}:
 * - For configuration collections jcr:content is added for each item, not for the parent
 * - For nested configuration jcr:content is not duplicated in the path
 */
public class CustomConfigurationPersistenceStrategy3 implements ConfigurationPersistenceStrategy2 {

    private static final String DEFAULT_RESOURCE_TYPE = JcrConstants.NT_UNSTRUCTURED;
    private static final String CHILD_NODE_NAME = JcrConstants.JCR_CONTENT;
    private static final Pattern JCR_CONTENT_PATTERN =
            Pattern.compile("(.*/)?" + Pattern.quote(CHILD_NODE_NAME) + "(/.*)?");

    @Override
    public Resource getResource(@NotNull Resource resource) {
        assertNotNull(resource);
        if (containsJcrContent(resource.getPath())) {
            return resource;
        }
        return resource.getChild(CHILD_NODE_NAME);
    }

    @Override
    public Resource getCollectionParentResource(@NotNull Resource resource) {
        assertNotNull(resource);
        return resource;
    }

    @Override
    public Resource getCollectionItemResource(@NotNull Resource resource) {
        return getResource(resource);
    }

    @Override
    public String getResourcePath(@NotNull String resourcePath) {
        assertNotNull(resourcePath);
        if (containsJcrContent(resourcePath)) {
            return resourcePath;
        }
        return resourcePath + "/" + CHILD_NODE_NAME;
    }

    @Override
    public String getCollectionParentResourcePath(@NotNull String resourcePath) {
        assertNotNull(resourcePath);
        return resourcePath;
    }

    @Override
    public String getCollectionItemResourcePath(@NotNull String resourcePath) {
        return getResourcePath(resourcePath);
    }

    @Override
    public String getConfigName(@NotNull String configName, @Nullable String relatedConfigPath) {
        assertNotNull(configName);
        if (containsJcrContent(configName)) {
            return configName;
        }
        return configName + "/" + CHILD_NODE_NAME;
    }

    @Override
    public String getCollectionParentConfigName(@NotNull String configName, @Nullable String relatedConfigPath) {
        assertNotNull(configName);
        return configName;
    }

    @Override
    public String getCollectionItemConfigName(@NotNull String configName, @Nullable String relatedConfigPath) {
        return getConfigName(configName, relatedConfigPath);
    }

    @Override
    public boolean persistConfiguration(
            @NotNull ResourceResolver resourceResolver,
            @NotNull String configResourcePath,
            @NotNull ConfigurationPersistData data) {
        getOrCreateResource(resourceResolver, getResourcePath(configResourcePath), data.getProperties());
        commit(resourceResolver);
        return true;
    }

    @Override
    public boolean persistConfigurationCollection(
            @NotNull ResourceResolver resourceResolver,
            @NotNull String configResourceCollectionParentPath,
            @NotNull ConfigurationCollectionPersistData data) {
        String parentPath = getCollectionParentResourcePath(configResourceCollectionParentPath);
        Resource configResourceParent = getOrCreateResource(resourceResolver, parentPath, ValueMap.EMPTY);

        // delete existing children and create new ones
        deleteChildren(configResourceParent);
        for (ConfigurationPersistData item : data.getItems()) {
            String path =
                    getCollectionItemResourcePath(configResourceParent.getPath() + "/" + item.getCollectionItemName());
            getOrCreateResource(resourceResolver, path, item.getProperties());
        }

        // if resource collection parent properties are given replace them as well
        if (data.getProperties() != null) {
            Resource propsResource =
                    getOrCreateResource(resourceResolver, parentPath + "/colPropsResource", ValueMap.EMPTY);
            replaceProperties(propsResource, data.getProperties());
        }

        commit(resourceResolver);
        return true;
    }

    @Override
    public boolean deleteConfiguration(@NotNull ResourceResolver resourceResolver, @NotNull String configResourcePath) {
        Resource resource = resourceResolver.getResource(configResourcePath);
        if (resource != null) {
            try {
                resourceResolver.delete(resource);
            } catch (PersistenceException ex) {
                throw new ConfigurationPersistenceException(
                        "Unable to delete configuration at " + configResourcePath, ex);
            }
        }
        commit(resourceResolver);
        return true;
    }

    private Resource getOrCreateResource(
            ResourceResolver resourceResolver, String path, Map<String, Object> properties) {
        try {
            Resource resource = ResourceUtil.getOrCreateResource(
                    resourceResolver, path, DEFAULT_RESOURCE_TYPE, DEFAULT_RESOURCE_TYPE, false);
            replaceProperties(resource, properties);
            return resource;
        } catch (PersistenceException ex) {
            throw new ConfigurationPersistenceException("Unable to persist configuration to " + path, ex);
        }
    }

    private void deleteChildren(Resource resource) {
        ResourceResolver resourceResolver = resource.getResourceResolver();
        try {
            for (Resource child : resource.getChildren()) {
                resourceResolver.delete(child);
            }
        } catch (PersistenceException ex) {
            throw new ConfigurationPersistenceException("Unable to remove children from " + resource.getPath(), ex);
        }
    }

    @SuppressWarnings("null")
    private void replaceProperties(Resource resource, Map<String, Object> properties) {
        ModifiableValueMap modValueMap = resource.adaptTo(ModifiableValueMap.class);
        // remove all existing properties that do not have jcr: namespace
        for (String propertyName : new HashSet<>(modValueMap.keySet())) {
            if (StringUtils.startsWith(propertyName, "jcr:")) {
                continue;
            }
            modValueMap.remove(propertyName);
        }
        modValueMap.putAll(properties);
    }

    private void commit(ResourceResolver resourceResolver) {
        try {
            resourceResolver.commit();
        } catch (PersistenceException ex) {
            throw new ConfigurationPersistenceException("Unable to save configuration: " + ex.getMessage(), ex);
        }
    }

    static boolean containsJcrContent(String path) {
        return JCR_CONTENT_PATTERN.matcher(path).matches();
    }
}
