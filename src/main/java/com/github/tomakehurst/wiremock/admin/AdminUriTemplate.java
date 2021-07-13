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

import static com.google.common.base.Preconditions.checkArgument;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.google.common.base.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminUriTemplate {

  public static final Pattern PATH_VARIABLE_REGEX = Pattern.compile("^\\{(.*)\\}$");

  private final String templateString;
  private final String[] templateParts;

  public AdminUriTemplate(String templateString) {
    this.templateString = templateString;
    templateParts = templateString.split("/");
  }

  public boolean matches(String url) {
    String[] urlParts = url.split("/");

    if (templateParts.length != urlParts.length) {
      return false;
    }

    for (int i = 0; i < templateParts.length; i++) {
      boolean isVariable = isVariable(templateParts[i]);
      boolean areEqual = templateParts[i].equals(urlParts[i]);

      if (!isVariable && !areEqual) {
        return false;
      }
    }

    return true;
  }

  public PathParams parse(String url) {
    PathParams pathParams = new PathParams();
    String[] urlParts = url.split("/");

    if (templateParts.length != urlParts.length) {
      throw new IllegalArgumentException(url + " does not match " + templateString);
    }

    for (int i = 0; i < templateParts.length; i++) {
      Matcher matcher = PATH_VARIABLE_REGEX.matcher(templateParts[i]);
      boolean areEqual = templateParts[i].equals(urlParts[i]);

      checkArgument(areEqual || matcher.matches(), url + " does not match " + templateString);

      if (matcher.matches()) {
        String variableName = getVariableName(templateParts[i]);
        pathParams.put(variableName, urlParts[i]);
      }
    }

    return pathParams;
  }

  public String render(PathParams pathParams) {
    StringBuilder sb = new StringBuilder();
    for (String templatePart : templateParts) {
      sb.append('/');
      if (isVariable(templatePart)) {
        String variableName = getVariableName(templatePart);
        String variableValue = pathParams.get(variableName);
        if (variableValue == null) {
          throw new IllegalArgumentException("Path parameter " + variableName + " was not bound");
        }
        sb.append(variableValue);
      } else {
        sb.append(templatePart);
      }
    }

    sb.deleteCharAt(0);

    return sb.toString();
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

  private static String getVariableName(String templatePart) {
    return templatePart.substring(1, templatePart.length() - 1);
  }

  private static boolean isVariable(String templatePart) {
    return PATH_VARIABLE_REGEX.matcher(templatePart).matches();
  }
}
