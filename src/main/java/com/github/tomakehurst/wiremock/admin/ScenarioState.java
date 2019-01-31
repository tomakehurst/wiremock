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

import com.github.tomakehurst.wiremock.common.Json;

public class ScenarioState {

    private String scenarioName;
    private String state;

    public ScenarioState(String scenarioName, String state) {
        this.scenarioName = scenarioName;
        this.state = state;
    }

    public ScenarioState() {
        //Concession to Jackson
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public String getState() {
        return state;
    }

    public static ScenarioState buildFrom(String mappingSpecJson) {
        return Json.read(mappingSpecJson, ScenarioState.class);
    }
}
