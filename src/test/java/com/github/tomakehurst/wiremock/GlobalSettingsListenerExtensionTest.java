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
package com.github.tomakehurst.wiremock;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import com.github.tomakehurst.wiremock.common.ClientError;
import com.github.tomakehurst.wiremock.common.Errors;
import com.github.tomakehurst.wiremock.common.JsonException;
import com.github.tomakehurst.wiremock.extension.GlobalSettingsListener;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class GlobalSettingsListenerExtensionTest {

  public static class Listening {

    TestGlobalSettingsListener listener = new TestGlobalSettingsListener();

    @Rule public WireMockRule wm = new WireMockRule(options().dynamicPort().extensions(listener));

    @Before
    public void init() {
      listener.events.clear();
    }

    @Test
    public void callsListenerMethodWithBeforeAndAfterStatesWhenSettingsUpdated() {
      wm.updateGlobalSettings(GlobalSettings.builder().fixedDelay(100).build());

      listener.events.clear();

      wm.updateGlobalSettings(GlobalSettings.builder().fixedDelay(200).build());

      assertThat(listener.events.size(), is(2));
      assertThat(listener.events.get(0), is("beforeGlobalSettingsUpdated, old: 100, new: 200"));
      assertThat(listener.events.get(1), is("afterGlobalSettingsUpdated, old: 100, new: 200"));
    }

    public static class TestGlobalSettingsListener implements GlobalSettingsListener {

      public List<String> events = new ArrayList<>();

      @Override
      public String getName() {
        return "test-settings-listener";
      }

      @Override
      public void beforeGlobalSettingsUpdated(
          GlobalSettings oldSettings, GlobalSettings newSettings) {
        events.add(
            "beforeGlobalSettingsUpdated, old: "
                + oldSettings.getFixedDelay()
                + ", new: "
                + newSettings.getFixedDelay());
      }

      @Override
      public void afterGlobalSettingsUpdated(
          GlobalSettings oldSettings, GlobalSettings newSettings) {
        events.add(
            "afterGlobalSettingsUpdated, old: "
                + oldSettings.getFixedDelay()
                + ", new: "
                + newSettings.getFixedDelay());
      }
    }
  }

  public static class Vetoing {

    @Rule
    public WireMockRule wm =
        new WireMockRule(
            options().dynamicPort().extensions(new VetoingTestGlobalSettingsListener()));

    @Test
    public void settingsUpdateCanBeVetoedByThrowningAnException() {
      try {
        wm.updateGlobalSettings(GlobalSettings.builder().fixedDelay(100).build());
        fail();
      } catch (ClientError e) {
        assertThat(e.getErrors().first().getTitle(), is("missing required element"));
        assertThat(wm.getGlobalSettings().getSettings().getFixedDelay(), nullValue());
      }
    }

    public static class VetoingTestGlobalSettingsListener implements GlobalSettingsListener {

      @Override
      public String getName() {
        return "vetoing-settings-listener";
      }

      @Override
      public void beforeGlobalSettingsUpdated(
          GlobalSettings oldSettings, GlobalSettings newSettings) {
        throw JsonException.fromErrors(Errors.single(123, "/one/two", "missing required element"));
      }

      @Override
      public void afterGlobalSettingsUpdated(
          GlobalSettings oldSettings, GlobalSettings newSettings) {}
    }
  }
}
