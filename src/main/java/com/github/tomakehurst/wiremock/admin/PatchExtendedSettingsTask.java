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

import com.github.tomakehurst.wiremock.admin.model.ExtendedSettingsWrapper;
import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

public class PatchExtendedSettingsTask implements AdminTask {

  @Override
  public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
    ExtendedSettingsWrapper extendedSettingsWrapper =
        Json.read(request.getBodyAsString(), ExtendedSettingsWrapper.class);
    Parameters newExtended = extendedSettingsWrapper.getExtended();

    GlobalSettings existingSettings = admin.getGlobalSettings().getSettings();
    Parameters existingExtended = existingSettings.getExtended();

    Parameters extended = existingExtended.merge(newExtended);

    GlobalSettings newGlobalSettings = existingSettings.copy().extended(extended).build();

    admin.updateGlobalSettings(newGlobalSettings);

    return ResponseDefinition.okEmptyJson();
  }
}
