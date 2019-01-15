/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.core;

import com.github.tomakehurst.wiremock.extension.Extension;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.google.common.base.Optional;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.net.MalformedURLException;
import java.util.Map;

public class WireMockConfigurationTest {

    @Test
    public void testJettyStopTimeout() {
        Long expectedStopTimeout = 500L;
        WireMockConfiguration wireMockConfiguration = WireMockConfiguration.wireMockConfig().jettyStopTimeout(expectedStopTimeout);
        Optional<Long> jettyStopTimeout = wireMockConfiguration.jettySettings().getStopTimeout();

        assertThat(jettyStopTimeout.isPresent(), is(true));
        assertThat(jettyStopTimeout.get(), is(expectedStopTimeout));
    }

    @Test
    public void testJettyStopTimeoutNotSet() {
        WireMockConfiguration wireMockConfiguration = WireMockConfiguration.wireMockConfig();
        Optional<Long> jettyStopTimeout = wireMockConfiguration.jettySettings().getStopTimeout();
        assertThat(jettyStopTimeout.isPresent(), is(false));
    }

    @Test
    public void shouldUseQueuedThreadPoolByDefault() {
        int maxThreads = 20;
        WireMockConfiguration wireMockConfiguration = WireMockConfiguration.wireMockConfig().containerThreads(maxThreads);

        QueuedThreadPool threadPool = (QueuedThreadPool) wireMockConfiguration.threadPoolFactory().buildThreadPool(wireMockConfiguration);

        assertThat(threadPool.getMaxThreads(), is(maxThreads));
    }
    
    @Test
    public void testReloadFileExtensions() throws MalformedURLException {
    	WireMockConfiguration wireMockConfiguration = WireMockConfiguration.wireMockConfig().usingFilesUnderDirectory("src/test/resources/extensionsTestFolder");
    	wireMockConfiguration.reloadFileExtensions();
    	
    	
    	Map<String, Extension> extensions = wireMockConfiguration.extensionsOfType(Extension.class);
    	assertThat("Two extensions must be loaded from json definition", extensions.keySet().size(), is(2));
    	
    	ResponseTemplateTransformer responseTemplateTransformer = (ResponseTemplateTransformer) extensions.get("response-template");
    	assertThat("ResponseTemplateTransformer extension must be loaded", responseTemplateTransformer, is(notNullValue()));
    	assertThat("Json defined test helper 1 should be loaded", responseTemplateTransformer.getHandlebars().helper("helpertest1"), is(notNullValue()));
    	assertThat("Json defined test helper 2 should be loaded", responseTemplateTransformer.getHandlebars().helper("helpertest2"), is(notNullValue()));
    }
}