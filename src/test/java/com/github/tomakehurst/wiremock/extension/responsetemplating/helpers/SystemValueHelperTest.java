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

import static org.junit.Assert.*;

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.extension.responsetemplating.SystemKeyAuthoriser;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class SystemValueHelperTest {

  private SystemValueHelper helper;

  @Before
  public void init() {
    helper = new SystemValueHelper(new SystemKeyAuthoriser(ImmutableSet.of(".*")));
    LocalNotifier.set(new ConsoleNotifier(true));
  }

  @Test
  public void getExistingEnvironmentVariableShouldNotNull() throws Exception {
    ImmutableMap<String, Object> optionsHash =
        ImmutableMap.<String, Object>of(
            "key", "PATH",
            "type", "ENVIRONMENT");

    String output = render(optionsHash);
    assertNotNull(output);
    assertTrue(output.length() > 0);
  }

  @Test
  public void getNonExistingEnvironmentVariableShouldNull() throws Exception {
    ImmutableMap<String, Object> optionsHash =
        ImmutableMap.<String, Object>of(
            "key", "NON_EXISTING_VAR",
            "type", "ENVIRONMENT");

    String output = render(optionsHash);
    assertNull(output);
  }

  @Test
  public void getForbiddenEnvironmentVariableShouldReturnError() throws Exception {
    helper = new SystemValueHelper(new SystemKeyAuthoriser(ImmutableSet.of("JAVA*")));

    ImmutableMap<String, Object> optionsHash =
        ImmutableMap.<String, Object>of(
            "key", "TEST_VAR",
            "type", "ENVIRONMENT");
    String value = render(optionsHash);
    assertEquals("[ERROR: Access to TEST_VAR is denied]", value);
  }

  @Test
  public void getEmptyKeyShouldReturnError() throws Exception {
    ImmutableMap<String, Object> optionsHash =
        ImmutableMap.<String, Object>of(
            "key", "",
            "type", "PROPERTY");
    String value = render(optionsHash);
    assertEquals("[ERROR: The key cannot be empty]", value);
  }

  @Test
  public void getAllowedPropertyShouldSuccess() throws Exception {
    helper = new SystemValueHelper(new SystemKeyAuthoriser(ImmutableSet.of("test.*")));
    System.setProperty("test.key", "aaa");
    assertEquals("aaa", System.getProperty("test.key"));
    ImmutableMap<String, Object> optionsHash =
        ImmutableMap.<String, Object>of(
            "key", "test.key",
            "type", "PROPERTY");
    String value = render(optionsHash);
    assertEquals("aaa", value);
  }

  @Test
  public void getForbiddenPropertyShouldReturnError() throws Exception {
    helper = new SystemValueHelper(new SystemKeyAuthoriser(ImmutableSet.of("JAVA.*")));
    System.setProperty("test.key", "aaa");
    ImmutableMap<String, Object> optionsHash =
        ImmutableMap.<String, Object>of(
            "key", "test.key",
            "type", "PROPERTY");
    String value = render(optionsHash);
    assertEquals("[ERROR: Access to test.key is denied]", value);
  }

  @Test
  public void getNonExistingSystemPropertyShouldNull() throws Exception {
    ImmutableMap<String, Object> optionsHash =
        ImmutableMap.<String, Object>of(
            "key", "not.existing.prop",
            "type", "PROPERTY");
    String output = render(optionsHash);
    assertNull(output);
  }

  private String render(ImmutableMap<String, Object> optionsHash) throws IOException {
    return helper.apply(
        null, new Options.Builder(null, null, null, null, null).setHash(optionsHash).build());
  }
}
