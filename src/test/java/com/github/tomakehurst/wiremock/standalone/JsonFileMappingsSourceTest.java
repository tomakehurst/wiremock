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
package com.github.tomakehurst.wiremock.standalone;

import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.stubbing.StubMappingsWithTransformersAndCustomMatchers;
import com.github.tomakehurst.wiremock.stubbing.StubMappings;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JsonFileMappingsSourceTest {

	@Test
	public void loadsMappingsViaClasspathFileSource() {
		ClasspathFileSource fileSource = new ClasspathFileSource("jar-filesource");
		JsonFileMappingsSource source = new JsonFileMappingsSource(fileSource);
		StubMappings stubMappings = new StubMappingsWithTransformersAndCustomMatchers();

		source.loadMappingsInto(stubMappings);

		assertThat(stubMappings.getAll().get(0).getRequest().getUrl(), is("/test"));
	}
}