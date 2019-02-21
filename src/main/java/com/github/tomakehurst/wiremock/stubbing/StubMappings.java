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
package com.github.tomakehurst.wiremock.stubbing;


import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.google.common.base.Optional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StubMappings {

	ServeEvent serveFor(Request request);
	void addMapping(StubMapping mapping);
	void removeMapping(StubMapping mapping);
	void editMapping(StubMapping stubMapping);
	void reset();
	void resetScenarios();
	void resetScenario(String scenarioName);

    List<StubMapping> getAll();
	Optional<StubMapping> get(UUID id);

	List<Scenario> getAllScenarios();

    List<StubMapping> findByMetadata(StringValuePattern pattern);
}
