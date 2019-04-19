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

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ResponseDefinitionBuilderTest {

    @Test
    public void withTransformerParameterShouldNotChangeOriginalTransformerParametersValue() {
        ResponseDefinition originalResponseDefinition = ResponseDefinitionBuilder
            .responseDefinition()
            .withTransformerParameter("name", "original")
            .build();

        ResponseDefinition transformedResponseDefinition = ResponseDefinitionBuilder
            .like(originalResponseDefinition)
            .but()
                .withTransformerParameter("name", "changed")
            .build();

        assertThat(originalResponseDefinition.getTransformerParameters().getString("name"), is("original"));
        assertThat(transformedResponseDefinition.getTransformerParameters().getString("name"), is("changed"));
    }

    @Test
    public void likeShouldCreateCompleteResponseDefinitionCopy() throws Exception {
        ResponseDefinition originalResponseDefinition = ResponseDefinitionBuilder.responseDefinition()
                .withStatus(200)
                .withStatusMessage("OK")
                .withBody("some body")
                .withBase64Body(Base64.encodeBase64String("some body".getBytes(Charsets.UTF_8)))
                .withBodyFile("some_body.json")
                .withHeader("some header", "some value")
                .withFixedDelay(100)
                .withUniformRandomDelay(1, 2)
                .withChunkedDribbleDelay(1, 1000)
                .withFault(Fault.EMPTY_RESPONSE)
                .withTransformers("some transformer")
                .withTransformerParameter("some param", "some value")
                .build();

        ResponseDefinition copiedResponseDefinition = ResponseDefinitionBuilder.like(originalResponseDefinition).build();

        assertThat(copiedResponseDefinition, is(originalResponseDefinition));
    }

    @Test
    public void withCookieShouldBuildAResponseSettingTheGivenCookie() {
        ResponseDefinition responseDefinition = ResponseDefinitionBuilder.responseDefinition()
                .withCookie("cookiename", "cookieval")
                .build();

        assertThat(responseDefinition.getHeaders().all(), hasItem(
                new HttpHeader("Set-Cookie", "cookiename=cookieval")));
    }
}
