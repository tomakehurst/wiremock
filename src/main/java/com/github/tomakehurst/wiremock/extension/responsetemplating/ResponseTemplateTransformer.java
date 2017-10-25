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
package com.github.tomakehurst.wiremock.extension.responsetemplating;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.TextFile;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.WiremockHelpers;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;

public class ResponseTemplateTransformer extends ResponseDefinitionTransformer {

    private final boolean global;

    private final Handlebars handlebars;

    public ResponseTemplateTransformer(boolean global) {
        this(global, Collections.<String, Helper>emptyMap());
    }

    public ResponseTemplateTransformer(boolean global, String helperName, Helper helper) {
        this(global, ImmutableMap.of(helperName, helper));
    }

    public ResponseTemplateTransformer(boolean global, Map<String, Helper> helpers) {
        this.global = global;
        handlebars = new Handlebars();

        for (StringHelpers helper: StringHelpers.values()) {
            handlebars.registerHelper(helper.name(), helper);
        }

        //Add all available wiremock helpers
        for(WiremockHelpers helper: WiremockHelpers.values()){
            handlebars.registerHelper(helper.name(), helper);
        }

        for (Map.Entry<String, Helper> entry: helpers.entrySet()) {
            handlebars.registerHelper(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean applyGlobally() {
        return global;
    }

    @Override
    public String getName() {
        return "response-template";
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
        ResponseDefinitionBuilder newResponseDefBuilder = ResponseDefinitionBuilder.like(responseDefinition);
        final ImmutableMap<String, TemplateModel> model = getTemplateModel(request, parameters);

        if (responseDefinition.specifiesBodyContent()) {
            Template bodyTemplate = uncheckedCompileTemplate(responseDefinition.getBody());
            applyTemplatedResponseBody(newResponseDefBuilder, model, bodyTemplate);
        } else if (responseDefinition.specifiesBodyFile()) {
            TextFile file = files.getTextFileNamed(responseDefinition.getBodyFileName());
            Template bodyTemplate = uncheckedCompileTemplate(file.readContentsAsString());
            applyTemplatedResponseBody(newResponseDefBuilder, model, bodyTemplate);
        }

        if (responseDefinition.getHeaders() != null) {
            Iterable<HttpHeader> newResponseHeaders = Iterables.transform(responseDefinition.getHeaders().all(), new Function<HttpHeader, HttpHeader>() {
                @Override
                public HttpHeader apply(HttpHeader input) {
                    List<String> newValues = Lists.transform(input.values(), new Function<String, String>() {
                        @Override
                        public String apply(String input) {
                            Template template = uncheckedCompileTemplate(input);
                            return uncheckedApplyTemplate(template, model);
                        }
                    });

                    return new HttpHeader(input.key(), newValues);
                }
            });
            newResponseDefBuilder.withHeaders(new HttpHeaders(newResponseHeaders));
        }

        if (responseDefinition.getProxyBaseUrl() != null) {
            Template proxyBaseUrlTemplate = uncheckedCompileTemplate(responseDefinition.getProxyBaseUrl());
            String newProxyBaseUrl = uncheckedApplyTemplate(proxyBaseUrlTemplate, model);
            newResponseDefBuilder.proxiedFrom(newProxyBaseUrl);
        }

        return newResponseDefBuilder.build();
    }

    private ImmutableMap<String, TemplateModel> getTemplateModel(Request request, Parameters parameters) {
        final ImmutableMap.Builder<String, TemplateModel> modelBuilder = ImmutableMap.builder();

        modelBuilder.put("request", RequestTemplateModel.from(request));
        templateModelHook(modelBuilder, parameters);

        return modelBuilder.build();
    }

    protected void templateModelHook(ImmutableMap.Builder<String, TemplateModel> modelBuilder, Parameters parameters) {

    }

    private void applyTemplatedResponseBody(ResponseDefinitionBuilder newResponseDefBuilder, ImmutableMap<String, TemplateModel> model, Template bodyTemplate) {
        String newBody = uncheckedApplyTemplate(bodyTemplate, model);
        newResponseDefBuilder.withBody(newBody);
    }

    private String uncheckedApplyTemplate(Template template, ImmutableMap<String, TemplateModel> context) {
        try {
            return template.apply(context);
        } catch (IOException e) {
            return throwUnchecked(e, String.class);
        }
    }

    private Template uncheckedCompileTemplate(String content) {
        try {
            return handlebars.compileInline(content);
        } catch (IOException e) {
            return throwUnchecked(e, Template.class);
        }
    }
}
