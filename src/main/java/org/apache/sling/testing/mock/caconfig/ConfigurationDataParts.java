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
import java.util.TreeMap;

/**
 * Splits a list of key/value pairs which may contain nested configuration and nested configuration lists in it's parts.
 */
class ConfigurationDataParts {

    private final Map<String, Object> values = new TreeMap<>();
    private final Map<String, Map<String, Object>> maps = new TreeMap<>();
    private final Map<String, Collection<Map<String, Object>>> collections = new TreeMap<>();

    @SuppressWarnings("unchecked")
    ConfigurationDataParts(Map<String, Object> input) {
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                maps.put(key, (Map) value);
            } else if (value instanceof Collection) {
                collections.put(key, (Collection) value);
            } else {
                values.put(key, value);
            }
        }
    }

    Map<String, Object> getValues() {
        return values;
    }

    Map<String, Map<String, Object>> getMaps() {
        return maps;
    }

    Map<String, Collection<Map<String, Object>>> getCollections() {
        return collections;
    }
}
