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
package com.github.tomakehurst.wiremock.client;

import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.UUID;

public interface MappingBuilder {

    MappingBuilder atPriority(Integer priority);
    MappingBuilder withHeader(String key, StringValuePattern headerPattern);
    MappingBuilder withQueryParam(String key, StringValuePattern queryParamPattern);
    MappingBuilder withRequestBody(StringValuePattern bodyPattern);
    ScenarioMappingBuilder inScenario(String scenarioName);
    MappingBuilder withId(UUID id);
    MappingBuilder withBasicAuth(String username, String password);
    MappingBuilder withCookie(String name, StringValuePattern cookieValuePattern);

    MappingBuilder willReturn(ResponseDefinitionBuilder responseDefBuilder);

    StubMapping build();
}
