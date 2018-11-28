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
import com.github.tomakehurst.wiremock.common.ClientError;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.jsonResponse;
import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForEmptyJson;
import static com.google.common.collect.Iterables.find;

public class SetScenarioStateTask implements AdminTask {

    @Override
    public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
        ScenarioState scenarioState = Json.read(request.getBodyAsString(), ScenarioState.class);
        Scenario scenario = find(admin.getAllScenarios().getScenarios(), Scenario.withName(scenarioState.getScenarioName());

        try {
            admin.setScenarioState(scenario, scenarioState.getState());
            return okForEmptyJson().build();
        } catch (ClientError e) {
            return jsonResponse(e.getErrors(), 404);
        }
    }
}