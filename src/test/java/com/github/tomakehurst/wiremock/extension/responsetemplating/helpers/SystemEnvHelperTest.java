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
package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SystemEnvHelperTest {

    private SystemEnvHelper helper;
    private ResponseTemplateTransformer transformer;

    @Before
    public void init() throws UnknownHostException {
        helper = new SystemEnvHelper();
        transformer = new ResponseTemplateTransformer(true);

        LocalNotifier.set(new ConsoleNotifier(true));
    }

    @Test
    public void generatesHostname() throws Exception {
        ImmutableMap<String, Object> optionsHash = ImmutableMap.<String, Object>of(
                "name", "PATH"
        );

        String output = render(optionsHash);
        assertNotNull(output);
        assertTrue(output.length() > 0);
    }

    private String render(ImmutableMap<String, Object> optionsHash) throws IOException {
        return helper.apply(null,
                new Options.Builder(null, null, null, null, null)
                        .setHash(optionsHash).build()
        ).toString();
    }

}
