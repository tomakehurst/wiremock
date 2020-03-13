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
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AdminUriTemplateTest {

    @Test
    public void extractsSinglePathParameter() {
        AdminUriTemplate template = new AdminUriTemplate("/things/{id}");

        PathParams pathParams = template.parse("/things/11-22-33");

        assertThat(pathParams.get("id"), is("11-22-33"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentExceptionIfAttemptingParsingOnNonMatchingUrl() {
        AdminUriTemplate template = new AdminUriTemplate("/things/{id}");
        template.parse("/things/stuff/11-22-33");
    }

    @Test
    public void matchesWhenUrlIsEquivalentToTemplate() {
        AdminUriTemplate template = new AdminUriTemplate("/things/{id}/otherthings/{subId}");

        assertThat(template.matches("/things/11-22-33/otherthings/12378"), is(true));
    }

    @Test
    public void nonMatchWhenUrlIsShorterThanTemplate() {
        AdminUriTemplate template = new AdminUriTemplate("/things/{id}/otherthings/{subId}");

        assertThat(template.matches("/things/11-22-33/otherthings"), is(false));
    }

    @Test
    public void nonMatchWhenUrlPartIsMismatch() {
        AdminUriTemplate template = new AdminUriTemplate("/things/{id}/otherthings/{subId}");

        assertThat(template.matches("/things/11-22-33/other-stuff/1234"), is(false));
    }

    @Test
    public void rendersWithParameters() {
        AdminUriTemplate template = new AdminUriTemplate("/things/{id}/otherthings/{subId}");
        PathParams pathParams = new PathParams()
            .add("id", "123")
            .add("subId", "456");

        String path = template.render(pathParams);

        assertThat(path, is("/things/123/otherthings/456"));
    }

    @Test
    public void rendersWithoutParameters() {
        AdminUriTemplate template = new AdminUriTemplate("/things/stuff");

        String path = template.render(PathParams.empty());

        assertThat(path, is("/things/stuff"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsErrorWhenNotAllParametersAreBound() {
        AdminUriTemplate template = new AdminUriTemplate("/things/{id}/otherthings/{subId}");
        template.render(new PathParams().add("id", "123"));
    }

    @Test
    public void parseWithWildcardAndOneDepthPath() {
        AdminUriTemplate template = new AdminUriTemplate("/things/**");

        PathParams pathParams = template.parse("/things/stuff");

        assertThat(pathParams.get("0"), is("stuff"));
    }

    @Test
    public void parseWithWildcardAndTwoDepthPath() {
        AdminUriTemplate template = new AdminUriTemplate("/things/**");

        PathParams pathParams = template.parse("/things/foo/bar");

        assertThat(pathParams.get("0"), is("foo/bar"));
    }

    @Test
    public void parseWithVariableAndWildcardAndTwoDepthPath() {
        AdminUriTemplate template = new AdminUriTemplate("/things/{id}/**");

        PathParams pathParams = template.parse("/things/foo/bar");

        assertThat(pathParams.get("id"), is("foo"));
        assertThat(pathParams.get("0"), is("bar"));
    }

    @Test
    public void renderWithWildcardAndOneDepth() {
        AdminUriTemplate template = new AdminUriTemplate("/things/**");
        PathParams pathParams = new PathParams()
                                        .add("0", "stuff");

        String path = template.render(pathParams);

        assertThat(path, is("/things/stuff"));
    }

    @Test
    public void renderWithWildcardAndTwoDepth() {
        AdminUriTemplate template = new AdminUriTemplate("/things/**");
        PathParams pathParams = new PathParams()
                                        .add("0", "foo/bar");

        String path = template.render(pathParams);

        assertThat(path, is("/things/foo/bar"));
    }

    @Test
    public void renderWithVariableAndWildcardAndTwoDepthPath() {
        AdminUriTemplate template = new AdminUriTemplate("/things/{id}/**");
        PathParams pathParams = new PathParams()
                                        .add("id", "foo")
                                        .add("0", "bar");

        String path = template.render(pathParams);

        assertThat(path, is("/things/foo/bar"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsErrorWhenNotWildcardParameterIsNotBound() {
        AdminUriTemplate template = new AdminUriTemplate("/things/{id}/**");
        template.render(new PathParams().add("id", "123"));
    }

    @Test
    public void checkHashAndEquality() {
        List<String> templates = asList("/things", "/things/", "/things/**", "/things/{id}", "/things/{name}",
                                        "/things/**/", "/things/{id}/", "/things/{name}/");

        Set<AdminUriTemplate> uriTemplateSet = new LinkedHashSet<>();
        for (String template : templates) {
            AdminUriTemplate uriTemplate = new AdminUriTemplate(template);
            if (!uriTemplateSet.add(uriTemplate)) {
                fail(format("Can't add '%s' to '%s'", template, uriTemplateSet));
            }
        }
    }



    @Test
    public void checkEquality() {
        List<String> templates = asList("/things", "/things/", "/things/**", "/things/{id}", "/things/{name}",
                "/things/**/", "/things/{id}/", "/things/{name}/");

        List<AdminUriTemplate> uriTemplates = new ArrayList<>();
        for (String template : templates) {
            AdminUriTemplate uriTemplate = new AdminUriTemplate(template);
            if (uriTemplates.contains(uriTemplate)) {
                fail(format("Can't add '%s' to '%s'", template, uriTemplates));
            }
            uriTemplates.add(uriTemplate);
        }
    }
}
