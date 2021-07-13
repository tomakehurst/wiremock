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
package com.github.tomakehurst.wiremock.recording;

import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Test;

public class SnapshotStubMappingPostProcessorTest {
  private static final List<StubMapping> TEST_STUB_MAPPINGS =
      ImmutableList.of(
          WireMock.get("/foo").build(), WireMock.get("/bar").build(), WireMock.get("/foo").build());

  @Test
  public void processFiltersRepeatedRequestsWhenNotRecordingScenarios() {
    final List<StubMapping> actual =
        new SnapshotStubMappingPostProcessor(false, noopTransformerRunner(), null, null)
            .process(TEST_STUB_MAPPINGS);

    assertThat(actual, hasSize(2));
    assertThat(actual.get(0).getRequest().getUrl(), equalTo("/foo"));
    assertThat(actual.get(1).getRequest().getUrl(), equalTo("/bar"));
  }

  @Test
  public void processRunsTransformers() {
    SnapshotStubMappingTransformerRunner transformerRunner =
        new SnapshotStubMappingTransformerRunner(null) {
          @Override
          public StubMapping apply(StubMapping stubMapping) {
            // Return StubMapping with "/transformed" at the end of the original URL
            String url = stubMapping.getRequest().getUrl();
            return new StubMapping(
                newRequestPattern().withUrl(url + "/transformed").build(), ResponseDefinition.ok());
          }
        };

    final List<StubMapping> actual =
        new SnapshotStubMappingPostProcessor(false, transformerRunner, null, null)
            .process(TEST_STUB_MAPPINGS);

    assertThat(actual, hasSize(2));
    assertThat(actual.get(0).getRequest().getUrl(), equalTo("/foo/transformed"));
    assertThat(actual.get(1).getRequest().getUrl(), equalTo("/bar/transformed"));
  }

  @Test
  public void processExtractsBodiesWhenMatched() {
    final ResponseDefinitionBodyMatcher bodyMatcher =
        new ResponseDefinitionBodyMatcher(0, 0) {
          @Override
          public MatchResult match(ResponseDefinition responseDefinition) {
            // Only match the second stub mapping
            return responseDefinition == TEST_STUB_MAPPINGS.get(1).getResponse()
                ? MatchResult.exactMatch()
                : MatchResult.noMatch();
          }
        };

    final SnapshotStubMappingBodyExtractor bodyExtractor =
        new SnapshotStubMappingBodyExtractor(null) {
          @Override
          public void extractInPlace(StubMapping stubMapping) {
            stubMapping.setRequest(newRequestPattern().withUrl("/extracted").build());
          }
        };

    final List<StubMapping> actual =
        new SnapshotStubMappingPostProcessor(
                false, noopTransformerRunner(), bodyMatcher, bodyExtractor)
            .process(TEST_STUB_MAPPINGS);

    assertThat(actual, hasSize(2));
    // Should've only modified second stub mapping
    assertThat(actual.get(0).getRequest().getUrl(), is("/foo"));
    assertThat(actual.get(1).getRequest().getUrl(), is("/extracted"));
  }

  private static SnapshotStubMappingTransformerRunner noopTransformerRunner() {
    return new SnapshotStubMappingTransformerRunner(null) {
      @Override
      public StubMapping apply(StubMapping stubMapping) {
        return stubMapping;
      }
    };
  }
}
