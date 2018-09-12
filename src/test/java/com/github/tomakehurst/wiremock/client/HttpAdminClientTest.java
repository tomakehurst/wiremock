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
/*
 * Copyright (C) 2017 Thomas Akehurst
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

import com.github.tomakehurst.wiremock.common.AdminException;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class HttpAdminClientTest {

    @Test
    public void returnsOptionsWhenCallingGetOptions() {
        HttpAdminClient client = new HttpAdminClient("localhost", 8080);
        assertThat(client.getOptions().portNumber(), is(8080));
        assertThat(client.getOptions().bindAddress(), is("localhost"));
    }

    @Test(expected = AdminException.class)
    public void rejectsStubMappingWithTransformationInstances() {
        HttpAdminClient client = new HttpAdminClient("", 0);
        client.addStubMapping(
                WireMock.stubFor(any(anyUrl())
                        .willReturn(aResponse().withTransformers(new CustomResponseDefinitionTransformer())))
        );
    }

    private static class CustomResponseDefinitionTransformer extends ResponseDefinitionTransformer {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
            return null;
        }
    }
}
