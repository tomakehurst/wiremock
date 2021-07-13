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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import org.junit.Test;

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
    PathParams pathParams = new PathParams().add("id", "123").add("subId", "456");

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
}
