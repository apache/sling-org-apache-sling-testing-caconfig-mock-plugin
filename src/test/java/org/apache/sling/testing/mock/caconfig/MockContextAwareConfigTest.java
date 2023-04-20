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

import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.testing.mock.caconfig.example.ListConfig;
import org.apache.sling.testing.mock.caconfig.example.NestedConfig;
import org.apache.sling.testing.mock.caconfig.example.NestedConfigSub;
import org.apache.sling.testing.mock.caconfig.example.NestedConfigSub2;
import org.apache.sling.testing.mock.caconfig.example.NestedListConfig;
import org.apache.sling.testing.mock.caconfig.example.SimpleConfig;
import org.apache.sling.testing.mock.sling.builder.ImmutableValueMap;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MockContextAwareConfigTest {

    @Rule
    public SlingContext context = new SlingContextBuilder()
            .plugin(CACONFIG)
            .build();

    @Before
    public void setUp() {
        MockContextAwareConfig.registerAnnotationPackages(context, "org.apache.sling.testing.mock.caconfig.example");

        context.create().resource("/content/region/site", "sling:configRef", "/conf/region/site");

        context.currentResource(context.create().resource("/content/region/site/en"));
    }

    @Test
    public void testSingletonConfig() {
        MockContextAwareConfig.writeConfiguration(context, "/content/region/site", SimpleConfig.class,
                "stringParam", "value1");

        SimpleConfig config = getConfig(SimpleConfig.class);
        assertEquals("value1", config.stringParam());
        assertEquals(5, config.intParam());
    }

    @Test
    public void testCollectionConfig() {
        MockContextAwareConfig.writeConfigurationCollection(context, "/content/region/site", ListConfig.class, ImmutableList.of(
                ImmutableMap.<String,Object>of("stringParam", "value1"),
                ImmutableMap.<String,Object>of("stringParam", "value2")));

        Collection<ListConfig> config = getConfigCollection(ListConfig.class);
        assertEquals(2, config.size());
        Iterator<ListConfig> items = config.iterator();

        ListConfig item1 = items.next();
        assertEquals("value1", item1.stringParam());
        assertEquals(5, item1.intParam());

        ListConfig item2 = items.next();
        assertEquals("value2", item2.stringParam());
        assertEquals(5, item2.intParam());
    }

    @Test
    public void testNestedSingletonConfig() {
        MockContextAwareConfig.writeConfiguration(context, "/content/region/site", NestedConfig.class,
                "stringParam", "value1",
                "sub", ImmutableList.of(
                        ImmutableValueMap.of("subStringParam", "v1", "intParam", 5, "stringArrayParam", new String[] {"v1a","v1b"}),
                        ImmutableValueMap.of("subStringParam", "v2")),
                "sub2", ImmutableValueMap.of(
                        "sub2StringParam", "v3",
                        "sub", ImmutableValueMap.of("subStringParam", "v4"),
                        "subList", ImmutableList.of(ImmutableValueMap.of("subStringParam", "v5a"),ImmutableValueMap.of("subStringParam", "v5b"))),
                "sub2List", ImmutableList.of(
                        ImmutableValueMap.of("sub2StringParam", "v6")));

        NestedConfig config = getConfig(NestedConfig.class);
        assertEquals("value1", config.stringParam());

        NestedConfigSub[] sub = config.sub();
        assertEquals(2, sub.length);
        assertEquals("v1", sub[0].subStringParam());
        assertEquals(5, sub[0].intParam());
        assertArrayEquals(new String[] {"v1a","v1b"}, sub[0].stringArrayParam());
        assertEquals("v2", sub[1].subStringParam());

        NestedConfigSub2 sub2 = config.sub2();
        assertEquals("v3", sub2.sub2StringParam());
        assertEquals("v4", sub2.sub().subStringParam());
        NestedConfigSub[] sub2_sublist = sub2.subList();
        assertEquals(2, sub2_sublist.length);
        assertEquals("v5a", sub2_sublist[0].subStringParam());
        assertEquals("v5b", sub2_sublist[1].subStringParam());

        NestedConfigSub2[] sub2list = config.sub2List();
        assertEquals(1, sub2list.length);
        assertEquals("v6", sub2list[0].sub2StringParam());
    }

    @Test
    public void testNestedCollectionConfigConfig() {
        MockContextAwareConfig.writeConfigurationCollection(context, "/content/region/site", NestedListConfig.class, ImmutableList.of(
                ImmutableMap.<String,Object>of("stringParam", "value1",
                        "sub", ImmutableList.of(
                                ImmutableValueMap.of("subStringParam", "v1", "intParam", 5, "stringArrayParam", new String[] {"v1a","v1b"}),
                                ImmutableValueMap.of("subStringParam", "v2")),
                        "sub2", ImmutableValueMap.of(
                                "sub2StringParam", "v3",
                                "sub", ImmutableValueMap.of("subStringParam", "v4"),
                                "subList", ImmutableList.of(ImmutableValueMap.of("subStringParam", "v5a"),ImmutableValueMap.of("subStringParam", "v5b"))),
                        "sub2List", ImmutableList.of(
                                ImmutableValueMap.of("sub2StringParam", "v6"))),
                ImmutableMap.<String,Object>of("stringParam", "value2")));

        Collection<NestedListConfig> config = getConfigCollection(NestedListConfig.class);
        assertEquals(2, config.size());
        Iterator<NestedListConfig> items = config.iterator();

        NestedListConfig item1 = items.next();
        assertEquals("value1", item1.stringParam());

        NestedConfigSub[] sub = item1.sub();
        assertEquals(2, sub.length);
        assertEquals("v1", sub[0].subStringParam());
        assertEquals(5, sub[0].intParam());
        assertArrayEquals(new String[] {"v1a","v1b"}, sub[0].stringArrayParam());
        assertEquals("v2", sub[1].subStringParam());

        NestedConfigSub2 sub2 = item1.sub2();
        assertEquals("v3", sub2.sub2StringParam());
        assertEquals("v4", sub2.sub().subStringParam());
        NestedConfigSub[] sub2_sublist = sub2.subList();
        assertEquals(2, sub2_sublist.length);
        assertEquals("v5a", sub2_sublist[0].subStringParam());
        assertEquals("v5b", sub2_sublist[1].subStringParam());

        NestedConfigSub2[] sub2list = item1.sub2List();
        assertEquals(1, sub2list.length);
        assertEquals("v6", sub2list[0].sub2StringParam());

        NestedListConfig item2 = items.next();
        assertEquals("value2", item2.stringParam());
    }

    @SuppressWarnings("null")
    private <T> @NotNull T getConfig(@NotNull Class<T> configClass) {
        Resource resource = context.request().getResource();
        T result = resource.adaptTo(ConfigurationBuilder.class).as(configClass);
        assertNotNull(result);
        return result;
    }

    @SuppressWarnings("null")
    private <T> @NotNull Collection<@Nullable T> getConfigCollection(@NotNull Class<T> configClass) {
        Resource resource = context.request().getResource();
        Collection<@Nullable T> result = resource.adaptTo(ConfigurationBuilder.class).asCollection(configClass);
        assertNotNull(result);
        return result;
    }

}
