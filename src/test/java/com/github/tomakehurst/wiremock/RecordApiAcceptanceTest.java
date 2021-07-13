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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.common.Metadata.metadata;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.testsupport.TestHttpHeader.withHeader;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.equalToJson;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.findMappingWithUrl;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Iterables.find;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.testsupport.GlobalStubMappingTransformer;
import com.github.tomakehurst.wiremock.testsupport.NonGlobalStubMappingTransformer;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import com.google.common.base.Predicate;
import java.util.UUID;
import org.apache.http.entity.StringEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class RecordApiAcceptanceTest extends AcceptanceTestBase {

  private WireMockServer proxyingService;
  private WireMockTestClient proxyingTestClient;
  private String proxyTargetUrl;

  private void proxyServerStart(WireMockConfiguration config) {
    proxyingService = new WireMockServer(config.dynamicPort());
    proxyingService.start();
    proxyTargetUrl = "http://localhost:" + wireMockServer.port();
    proxyingService.stubFor(
        proxyAllTo(proxyTargetUrl).withMetadata(metadata().attr("proxy", true)));

    proxyingTestClient = new WireMockTestClient(proxyingService.port());
    wireMockServer.stubFor(any(anyUrl()).willReturn(ok()));
  }

  private void proxyServerStartWithEmptyFileRoot() {
    proxyServerStart(wireMockConfig().withRootDirectory("src/test/resources/empty"));
  }

  @Before
  public void clearTargetServerMappings() {
    wireMockServer.resetMappings();
  }

  @After
  public void proxyServerShutdown() {
    // delete any persisted stub mappings to ensure test isolation
    proxyingService.resetMappings();
    proxyingService.stop();
  }

  private static final String DEFAULT_SNAPSHOT_RESPONSE =
      "{                                                           \n"
          + "    \"mappings\": [                                         \n"
          + "        {                                                   \n"
          + "            \"request\" : {                                 \n"
          + "                \"url\" : \"/foo/bar\",                     \n"
          + "                \"method\" : \"GET\"                        \n"
          + "            },                                              \n"
          + "            \"response\" : {                                \n"
          + "                \"status\" : 200                            \n"
          + "            }                                               \n"
          + "        },                                                  \n"
          + "        {                                                   \n"
          + "            \"request\" : {                                 \n"
          + "                \"url\" : \"/foo/bar/baz\",                 \n"
          + "                \"method\" : \"GET\"                        \n"
          + "            },                                              \n"
          + "            \"response\" : {                                \n"
          + "                \"status\" : 200                            \n"
          + "            }                                               \n"
          + "        },                                                  \n"
          + "    ]                                                       \n"
          + "}                                                             ";

  @Test
  public void returnsRequestsWithDefaultOptions() throws Exception {
    proxyServerStart(wireMockConfig().withRootDirectory(setupTempFileRoot().getAbsolutePath()));
    proxyingTestClient.get("/foo/bar", withHeader("A", "B"));
    proxyingTestClient.get("/foo/bar/baz", withHeader("A", "B"));

    assertThat(
        proxyingTestClient.snapshot(""),
        equalToJson(DEFAULT_SNAPSHOT_RESPONSE, JSONCompareMode.STRICT_ORDER));

    // Should have persisted both stub mappings. The 3 is to account for the proxy mapping
    assertEquals(3, proxyingService.getStubMappings().size());
  }

  private static final String FILTER_BY_REQUEST_PATTERN_SNAPSHOT_REQUEST =
      "{                                                 \n"
          + "    \"outputFormat\": \"full\",                   \n"
          + "    \"persist\": \"false\",                       \n"
          + "    \"filters\": {                                \n"
          + "        \"urlPattern\": \"/foo.*\",               \n"
          + "        \"headers\": {                            \n"
          + "            \"A\": { \"equalTo\": \"B\" }         \n"
          + "        }                                         \n"
          + "    }                                             \n"
          + "}                                                   ";

  private static final String FILTER_BY_REQUEST_PATTERN_SNAPSHOT_RESPONSE =
      "{                                                           \n"
          + "    \"mappings\": [                                         \n"
          + "        {                                                   \n"
          + "            \"request\" : {                                 \n"
          + "                \"url\" : \"/foo/bar\",                     \n"
          + "                \"method\" : \"GET\"                        \n"
          + "            },                                              \n"
          + "            \"response\" : {                                \n"
          + "                \"status\" : 200                            \n"
          + "            }                                               \n"
          + "        },                                                  \n"
          + "        {                                                   \n"
          + "            \"request\" : {                                 \n"
          + "                \"url\" : \"/foo/bar/baz\",                 \n"
          + "                \"method\" : \"GET\"                        \n"
          + "            },                                              \n"
          + "            \"response\" : {                                \n"
          + "                \"status\" : 200                            \n"
          + "            }                                               \n"
          + "        }                                                   \n"
          + "    ]                                                       \n"
          + "}                                                             ";

  @Test
  public void returnsFilteredRequestsWithJustRequestPatternsAndFullOutputFormat() throws Exception {
    proxyServerStartWithEmptyFileRoot();

    // Matches both
    proxyingTestClient.get("/foo/bar", withHeader("A", "B"));
    // Fails header match
    proxyingTestClient.get("/foo");
    // Fails URL match
    proxyingTestClient.get("/bar", withHeader("A", "B"));
    // Fails header match
    proxyingTestClient.get("/foo/", withHeader("A", "C"));
    // Matches both
    proxyingTestClient.get("/foo/bar/baz", withHeader("A", "B"));

    assertThat(
        proxyingTestClient.snapshot(FILTER_BY_REQUEST_PATTERN_SNAPSHOT_REQUEST),
        equalToJson(FILTER_BY_REQUEST_PATTERN_SNAPSHOT_RESPONSE, JSONCompareMode.STRICT_ORDER));
  }

  private static final String FILTER_BY_REQUEST_PATTERN_AND_IDS_SNAPSHOT_REQUEST_TEMPLATE =
      "{                                                     \n"
          + "    \"outputFormat\": \"full\",                       \n"
          + "    \"persist\": \"false\",                           \n"
          + "    \"filters\": {                                    \n"
          + "        \"ids\": [ \"%s\", \"%s\" ],                  \n"
          + "        \"urlPattern\": \"/foo.*\"                    \n"
          + "    }                                                 \n"
          + "}                                                       ";

  private static final String FILTER_BY_REQUEST_PATTERN_AND_IDS_SNAPSHOT_RESPONSE =
      "{                                                       \n"
          + "    \"mappings\": [                                     \n"
          + "        {                                               \n"
          + "            \"request\" : {                             \n"
          + "                \"url\" : \"/foo/bar\",                 \n"
          + "                \"method\" : \"GET\"                    \n"
          + "            },                                          \n"
          + "            \"response\" : {                            \n"
          + "                \"status\" : 200                        \n"
          + "            }                                           \n"
          + "        }                                               \n"
          + "    ]                                                   \n"
          + "}                                                         ";

  @Test
  public void returnsFilteredRequestsWithRequestPatternAndIdsWithFullOutputFormat() {
    proxyServerStartWithEmptyFileRoot();

    // Matches both
    proxyingTestClient.get("/foo/bar");
    // Fails URL match
    proxyingTestClient.get("/bar");
    // Fails ID match
    proxyingTestClient.get("/foo");

    UUID fooBarId = findServeEventWithRequestUrl("/foo/bar").getId();
    UUID barId = findServeEventWithRequestUrl("/bar").getId();

    String request =
        String.format(FILTER_BY_REQUEST_PATTERN_AND_IDS_SNAPSHOT_REQUEST_TEMPLATE, fooBarId, barId);

    assertThat(
        proxyingTestClient.snapshot(request),
        equalToJson(
            FILTER_BY_REQUEST_PATTERN_AND_IDS_SNAPSHOT_RESPONSE, JSONCompareMode.STRICT_ORDER));
  }

  private static final String FILTER_BY_WITH_NON_PROXIED_TRUE_SNAPSHOT_REQUEST =
      "{                                                 \n"
          + "    \"persist\": false,                           \n"
          + "    \"filters\": {                                \n"
          + "        \"allowNonProxied\": true                 \n"
          + "    }                                             \n"
          + "}                                                   ";

  private static final String FILTER_BY_WITH_NON_PROXIED_TRUE_SNAPSHOT_RESPONSE =
      "{                                                       \n"
          + "    \"mappings\": [                                     \n"
          + "        {                                               \n"
          + "            \"request\" : {                             \n"
          + "                \"url\" : \"/record-anyway\",           \n"
          + "                \"method\" : \"GET\"                    \n"
          + "            },                                          \n"
          + "            \"response\" : {                            \n"
          + "                \"status\" : 404                        \n"
          + "            }                                           \n"
          + "        }                                               \n"
          + "    ]                                                   \n"
          + "}                                                         ";

  @Test
  public void returnsStubsFromNonProxiedRequestsWhenRequested() {
    proxyServerStartWithEmptyFileRoot();
    proxyingService.removeStubsByMetadata(matchingJsonPath("$.proxy"));

    proxyingTestClient.get("/record-anyway");

    assertThat(
        proxyingTestClient.snapshot(FILTER_BY_WITH_NON_PROXIED_TRUE_SNAPSHOT_REQUEST),
        equalToJson(FILTER_BY_WITH_NON_PROXIED_TRUE_SNAPSHOT_RESPONSE, JSONCompareMode.LENIENT));
  }

  private ServeEvent findServeEventWithRequestUrl(final String url) {
    return find(
        proxyingService.getAllServeEvents(),
        new Predicate<ServeEvent>() {
          @Override
          public boolean apply(ServeEvent input) {
            return url.equals(input.getRequest().getUrl());
          }
        });
  }

  private static final String CAPTURE_HEADERS_SNAPSHOT_REQUEST =
      "{                                      \n"
          + "    \"outputFormat\": \"full\",        \n"
          + "    \"persist\": \"false\",            \n"
          + "    \"captureHeaders\": {              \n"
          + "        \"Accept\": {                  \n"
          + "            \"caseInsensitive\": true  \n"
          + "        },                             \n"
          + "        \"X-Another\": {}              \n"
          + "    }                                  \n"
          + "}                                        ";

  private static final String CAPTURE_HEADERS_SNAPSHOT_RESPONSE =
      "{                                                           \n"
          + "    \"mappings\": [                                         \n"
          + "        {                                                   \n"
          + "            \"request\" : {                                 \n"
          + "                \"url\" : \"/foo/bar\",                     \n"
          + "                \"method\" : \"PUT\",                       \n"
          + "                \"headers\": {                              \n"
          + "                    \"Accept\": {                           \n"
          + "                        \"equalTo\": \"text/plain\",        \n"
          + "                        \"caseInsensitive\": true           \n"
          + "                    },                                      \n"
          + "                    \"X-Another\": {                        \n"
          + "                        \"equalTo\": \"blah\"               \n"
          + "                    }                                       \n"
          + "                }                                           \n"
          + "            },                                              \n"
          + "            \"response\" : {                                \n"
          + "                \"status\" : 200                            \n"
          + "            }                                               \n"
          + "        }                                                   \n"
          + "    ]                                                       \n"
          + "}                                                             ";

  @Test
  public void returnsStubMappingWithCapturedHeaders() {
    proxyServerStartWithEmptyFileRoot();

    proxyingTestClient.put(
        "/foo/bar",
        withHeader("Ignored", "whatever"),
        withHeader("Accept", "text/plain"),
        withHeader("X-Another", "blah"));

    String actual = proxyingTestClient.snapshot(CAPTURE_HEADERS_SNAPSHOT_REQUEST);
    assertThat(
        actual, equalToJson(CAPTURE_HEADERS_SNAPSHOT_RESPONSE, JSONCompareMode.STRICT_ORDER));
  }

  private static final String REPEATS_AS_SCENARIOS_SNAPSHOT_REQUEST =
      "{                                                 \n"
          + "    \"outputFormat\": \"full\",                   \n"
          + "    \"persist\": \"false\",                       \n"
          + "    \"repeatsAsScenarios\": \"true\"              \n"
          + "}                                                   ";

  private static final String REPEATS_AS_SCENARIOS_SNAPSHOT_RESPONSE =
      "{                                                                 \n"
          + "    \"mappings\": [                                               \n"
          + "        {                                                         \n"
          + "            \"scenarioName\" : \"scenario-1-bar-baz\",            \n"
          + "            \"requiredScenarioState\" : \"Started\",              \n"
          + "            \"newScenarioState\" : \"scenario-1-bar-baz-2\",      \n"
          + "            \"request\" : {                                       \n"
          + "                \"url\" : \"/bar/baz\",                           \n"
          + "                \"method\" : \"GET\"                              \n"
          + "            }                                                     \n"
          + "        },                                                        \n"
          + "        {                                                         \n"
          + "            \"request\" : {                                       \n"
          + "                \"url\" : \"/foo\",                               \n"
          + "                \"method\" : \"GET\"                              \n"
          + "            }                                                     \n"
          + "        },                                                        \n"
          + "        {                                                         \n"
          + "            \"scenarioName\" : \"scenario-1-bar-baz\",            \n"
          + "            \"requiredScenarioState\" : \"scenario-1-bar-baz-2\", \n"
          + "            \"request\" : {                                       \n"
          + "                \"url\" : \"/bar/baz\",                           \n"
          + "                \"method\" : \"GET\"                              \n"
          + "            }                                                     \n"
          + "        }                                                         \n"
          + "    ]                                                             \n"
          + "}                                                                   ";

  @Test
  public void returnsStubMappingsWithScenariosForRepeatedRequests() {
    proxyServerStartWithEmptyFileRoot();

    proxyingTestClient.get("/bar/baz");
    proxyingTestClient.get("/foo");
    proxyingTestClient.get("/bar/baz");

    assertThat(
        proxyingTestClient.snapshot(REPEATS_AS_SCENARIOS_SNAPSHOT_REQUEST),
        equalToJson(REPEATS_AS_SCENARIOS_SNAPSHOT_RESPONSE, JSONCompareMode.STRICT_ORDER));
  }

  private static final String GLOBAL_TRANSFORMED_STUB_MAPPING_REQUEST =
      "{                                  \n"
          + "    \"outputFormat\": \"full\",    \n"
          + "    \"persist\": \"false\"         \n"
          + "}                                    ";

  private static final String GLOBAL_TRANSFORMED_STUB_MAPPING_RESPONSE =
      "{                                                           \n"
          + "    \"mappings\": [                                         \n"
          + "        {                                                   \n"
          + "            \"request\" : {                                 \n"
          + "                \"url\" : \"/?transformed=global\",         \n"
          + "                \"method\" : \"GET\"                        \n"
          + "            },                                              \n"
          + "            \"response\" : {                                \n"
          + "                \"status\" : 200                            \n"
          + "            }                                               \n"
          + "        },                                                  \n"
          + "        {                                                   \n"
          + "            \"request\" : {                                 \n"
          + "                \"url\" : \"/foo?transformed=global\",      \n"
          + "                \"method\" : \"GET\"                        \n"
          + "            },                                              \n"
          + "            \"response\" : {                                \n"
          + "                \"status\" : 200                            \n"
          + "            }                                               \n"
          + "        },                                                  \n"
          + "    ]                                                       \n"
          + "}                                                             ";

  @Test
  public void returnsTransformedStubMappingWithGlobalTransformer() {
    proxyServerStart(
        wireMockConfig()
            .withRootDirectory("src/test/resources/empty")
            .extensions(
                GlobalStubMappingTransformer.class,
                NonGlobalStubMappingTransformer.class // should ignore this one
                ));

    proxyingTestClient.get("/");
    proxyingTestClient.get("/foo");

    assertThat(
        proxyingTestClient.snapshot(GLOBAL_TRANSFORMED_STUB_MAPPING_REQUEST),
        equalToJson(GLOBAL_TRANSFORMED_STUB_MAPPING_RESPONSE, JSONCompareMode.STRICT_ORDER));
  }

  private static final String NONGLOBAL_TRANSFORMED_STUB_MAPPING_REQUEST =
      "{                                    \n"
          + "    \"outputFormat\": \"full\",      \n"
          + "    \"persist\": \"false\",          \n"
          + "    \"transformers\": [              \n"
          + "       \"nonglobal-transformer\"     \n"
          + "    ]                                \n"
          + "}                                      ";

  private static final String NONGLOBAL_TRANSFORMED_STUB_MAPPING_RESPONSE =
      "{                                                           \n"
          + "    \"mappings\": [                                         \n"
          + "        {                                                   \n"
          + "            \"request\" : {                                 \n"
          + "                \"url\" : \"/?transformed=nonglobal\",      \n"
          + "                \"method\" : \"GET\",                       \n"
          + "                \"headers\": {                              \n"
          + "                    \"Accept\": {                           \n"
          + "                        \"equalTo\": \"B\"                  \n"
          + "                    }                                       \n"
          + "                }                                           \n"
          + "            },                                              \n"
          + "            \"response\" : {                                \n"
          + "                \"status\" : 200                            \n"
          + "            }                                               \n"
          + "        },                                                  \n"
          + "        {                                                   \n"
          + "            \"request\" : {                                 \n"
          + "                \"url\" : \"/foo?transformed=nonglobal\",   \n"
          + "                \"method\" : \"GET\",                       \n"
          + "                \"headers\": {                              \n"
          + "                    \"Accept\": {                           \n"
          + "                        \"equalTo\": \"B\"                  \n"
          + "                    }                                       \n"
          + "                }                                           \n"
          + "            },                                              \n"
          + "            \"response\" : {                                \n"
          + "                \"status\" : 200                            \n"
          + "            }                                               \n"
          + "        }                                                   \n"
          + "    ]                                                       \n"
          + "}                                                             ";

  @Test
  public void returnsTransformedStubMappingWithNonGlobalTransformer() {
    proxyServerStart(
        wireMockConfig()
            .withRootDirectory("src/test/resources/empty")
            .extensions(NonGlobalStubMappingTransformer.class));

    proxyingTestClient.get("/");
    proxyingTestClient.get("/foo");

    assertThat(
        proxyingTestClient.snapshot(NONGLOBAL_TRANSFORMED_STUB_MAPPING_REQUEST),
        equalToJson(NONGLOBAL_TRANSFORMED_STUB_MAPPING_RESPONSE, JSONCompareMode.STRICT_ORDER));
  }

  private static final String RECORD_WITH_CAPTURE_HEADERS_SNAPSHOT_REQUEST_TEMPLATE =
      "{                                      \n"
          + "    \"targetBaseUrl\": \"%s\",         \n"
          + "    \"outputFormat\": \"full\",        \n"
          + "    \"persist\": \"false\",            \n"
          + "    \"captureHeaders\": {              \n"
          + "        \"Accept\": {                  \n"
          + "            \"caseInsensitive\": true  \n"
          + "        },                             \n"
          + "        \"X-Another\": {}              \n"
          + "    }                                  \n"
          + "}                                        ";

  private static final String RECORD_WITH_CAPTURE_HEADERS_RECORD_RESPONSE =
      "{                                                           \n"
          + "    \"mappings\": [                                         \n"
          + "        {                                                   \n"
          + "            \"request\" : {                                 \n"
          + "                \"url\" : \"/foo/bar\",                     \n"
          + "                \"method\" : \"PUT\",                       \n"
          + "                \"headers\": {                              \n"
          + "                    \"Accept\": {                           \n"
          + "                        \"equalTo\": \"text/plain\",        \n"
          + "                        \"caseInsensitive\": true           \n"
          + "                    },                                      \n"
          + "                    \"X-Another\": {                        \n"
          + "                        \"equalTo\": \"blah\"               \n"
          + "                    }                                       \n"
          + "                }                                           \n"
          + "            },                                              \n"
          + "            \"response\" : {                                \n"
          + "                \"status\" : 200                            \n"
          + "            }                                               \n"
          + "        },                                                  \n"
          + "        {                                                   \n"
          + "            \"request\" : {                                 \n"
          + "                \"url\" : \"/foo/bar\",                     \n"
          + "                \"method\" : \"PUT\",                       \n"
          + "                \"headers\": {                              \n"
          + "                    \"Accept\": {                           \n"
          + "                        \"equalTo\": \"text/plain\",        \n"
          + "                        \"caseInsensitive\": true           \n"
          + "                    },                                      \n"
          + "                    \"X-Another\": {                        \n"
          + "                        \"equalTo\": \"blah\"               \n"
          + "                    }                                       \n"
          + "                }                                           \n"
          + "            },                                              \n"
          + "            \"response\" : {                                \n"
          + "                \"status\" : 200                            \n"
          + "            }                                               \n"
          + "        }                                                   \n"
          + "    ]                                                       \n"
          + "}                                                             ";

  @Test
  public void startsAndStopsRecording() {
    proxyServerStartWithEmptyFileRoot();

    String requestJson =
        String.format(RECORD_WITH_CAPTURE_HEADERS_SNAPSHOT_REQUEST_TEMPLATE, proxyTargetUrl);
    proxyingTestClient.postJson("/__admin/recordings/start", requestJson);

    proxyingTestClient.put(
        "/foo/bar",
        withHeader("Ignored", "whatever"),
        withHeader("Accept", "text/plain"),
        withHeader("X-Another", "blah"));
    proxyingTestClient.put(
        "/foo/bar", withHeader("Accept", "text/plain"), withHeader("X-Another", "blah"));

    WireMockResponse response =
        proxyingTestClient.post("/__admin/recordings/stop", new StringEntity("", UTF_8));
    assertThat(
        response.content(),
        equalToJson(RECORD_WITH_CAPTURE_HEADERS_RECORD_RESPONSE, JSONCompareMode.STRICT_ORDER));

    StubMapping createdMapping = findMappingWithUrl(proxyingService.getStubMappings(), "/foo/bar");
    assertThat(createdMapping.getScenarioName(), notNullValue());
  }

  private static final String NOT_RECORDING_ERROR =
      "{                                                          \n"
          + "    \"errors\": [                                          \n"
          + "        {                                                  \n"
          + "            \"code\": 30,                                  \n"
          + "            \"title\": \"Not currently recording.\"        \n"
          + "        }                                                  \n"
          + "    ]                                                      \n"
          + "}";

  @Test
  public void returnsErrorWhenAttemptingToStopRecordingWhenNotStarted() {
    proxyServerStartWithEmptyFileRoot();

    WireMockResponse response =
        proxyingTestClient.postWithBody("/__admin/recordings/stop", "", "text/plain", "utf-8");

    assertThat(response.content(), equalToJson(NOT_RECORDING_ERROR));
  }
}
