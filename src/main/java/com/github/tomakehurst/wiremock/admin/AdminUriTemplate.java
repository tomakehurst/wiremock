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
package com.github.tomakehurst.wiremock.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.google.common.base.Function;
import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

public class AdminUriTemplate {
    public static final Pattern SPECIAL_SYMBOL_REGEX = Pattern.compile("(?:\\{(?<variable>[^}]+)\\})|(?<wildcard>\\*\\*)");

    private final String templateString;
    private final Parser parser;
    private final Renderer renderer;

    public AdminUriTemplate(String templateString) {
        this.templateString = templateString;

        Matcher matcher = SPECIAL_SYMBOL_REGEX.matcher(templateString);
        ParserBuilder parserBuilder = new ParserBuilder();
        RendererBuilder rendererBuilder = new RendererBuilder();
        int last = 0;
        while (matcher.find()) {
            String text = templateString.substring(last, matcher.start());
            parserBuilder.addStatic(text);
            rendererBuilder.addStatic(text);

            String variable = matcher.group("variable");
            if (variable != null) {
                parserBuilder.addVariable(variable);
                rendererBuilder.addVariable(variable);
            }

            String wildcard = matcher.group("wildcard");
            if (wildcard != null) {
                parserBuilder.addWildcard();
                rendererBuilder.addWildcard();
            }

            last = matcher.end();
        }
        String text = templateString.substring(last);
        parserBuilder.addStatic(text);
        rendererBuilder.addStatic(text);

        parser = parserBuilder.build();
        renderer = rendererBuilder.build();
    }

    public boolean matches(String url) {
        return parser.matches(url);
    }

    public PathParams parse(String url) {
        return parser.parse(url);
    }

    public String render(PathParams pathParams) {
        return renderer.render(pathParams);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminUriTemplate that = (AdminUriTemplate) o;
        return Objects.equal(templateString, that.templateString);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(templateString);
    }
}

class Parser {
    private final Pattern templatePattern;
    private final List<String> templateVariables;

    Parser(Pattern templatePattern, List<String> templateVariables) {
        this.templatePattern = templatePattern;
        this.templateVariables = templateVariables;
    }

    boolean matches(String url) {
        Matcher matcher = templatePattern.matcher(url);
        return matcher.matches();
    }

    PathParams parse(String url) {
        Matcher matcher = templatePattern.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(format("'%s' is not a matching URL", url));
        }

        PathParams pathParams = new PathParams();
        for (int i=0; i < templateVariables.size(); i++) {
            pathParams.put(templateVariables.get(i), matcher.group(i+1));
        }

        return pathParams;
    }
}

class ParserBuilder {
    private final StringBuilder templatePattern = new StringBuilder().append("^");
    private final List<String> templateVariables = new ArrayList<>();
    private int wildcardCount = 0;

    void addStatic(String text) {
        templatePattern.append(Pattern.quote(text));
    }

    void addVariable(String variable) {
        templatePattern.append("([^/]+)");
        templateVariables.add(variable);
    }

    void addWildcard() {
        templatePattern.append("(.*?)");
        templateVariables.add("" + wildcardCount++);
    }

    Parser build() {
        return new Parser(Pattern.compile(templatePattern.append("$").toString()), Collections.unmodifiableList(templateVariables));
    }
}

class Renderer {
    private final List<Function<PathParams, String>> tasks;

    Renderer(List<Function<PathParams, String>> tasks) {
        this.tasks = tasks;
    }

    String render(PathParams pathParams) {
        StringBuilder rendering = new StringBuilder();

        for (Function<PathParams, String> task : tasks) {
            rendering.append(task.apply(pathParams));
        }

        return rendering.toString();
    }
}

class RendererBuilder {
    private final List<Function<PathParams, String>> tasks = new ArrayList<>();
    private int wildcardCount = 0;

    void addStatic(final String text) {
        class Static implements Function<PathParams, String> {
            @Override
            public String apply(PathParams input) {
                return text;
            }
        }
        tasks.add(new Static());
    }

    void addVariable(final String variable) {
        class Variable implements Function<PathParams, String> {
            @Override
            public String apply(PathParams input) {
                String value = input.get(variable);
                if (value == null) {
                    throw new IllegalArgumentException(format("Path parameter %s was not bound", variable));
                }
                return value;
            }
        }
        tasks.add(new Variable());
    }

    void addWildcard() {
        final String wildcardIndex = "" + wildcardCount++;
        class Wildcard implements Function<PathParams, String> {
            @Override
            public String apply(PathParams input) {
                String value = input.get(wildcardIndex);
                if (value == null) {
                    throw new IllegalArgumentException(format("Wildcard was not bound"));
                }
                return value;
            }
        }
        tasks.add(new Wildcard());
    }

    Renderer build() {
        return new Renderer(Collections.unmodifiableList(tasks));
    }
}
