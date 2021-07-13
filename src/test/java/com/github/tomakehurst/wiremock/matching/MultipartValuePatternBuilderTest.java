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
 * Copyright (C) 2017 Arjan Duijzer
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
package com.github.tomakehurst.wiremock.matching;

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToXml;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class MultipartValuePatternBuilderTest {

  @Test
  public void testBuilderDefaultType() {
    MultipartValuePattern pattern = aMultipart("name").build();
    assertTrue(pattern.isMatchAny());
    assertFalse(pattern.isMatchAll());
  }

  @Test
  public void testBuilderAnyType() {
    MultipartValuePattern pattern =
        aMultipart("name").matchingType(MultipartValuePattern.MatchingType.ANY).build();

    assertTrue(pattern.isMatchAny());
    assertFalse(pattern.isMatchAll());
  }

  @Test
  public void testBuilderAllType() {
    MultipartValuePattern pattern =
        aMultipart("name").matchingType(MultipartValuePattern.MatchingType.ALL).build();

    assertTrue(pattern.isMatchAll());
    assertFalse(pattern.isMatchAny());
  }

  @Test
  public void testBuilderWithNameHeadersAndBody() {
    MultipartValuePattern pattern =
        aMultipart("name")
            .withHeader("X-Header", containing("something"))
            .withHeader("X-Other", absent())
            .withBody(equalToXml("<xml />"))
            .build();

    Map<String, List<MultiValuePattern>> headerPatterns = newLinkedHashMap();
    headerPatterns.put(
        "Content-Disposition", asList(MultiValuePattern.of(containing("name=\"name\""))));
    headerPatterns.put("X-Header", asList(MultiValuePattern.of(containing("something"))));
    headerPatterns.put("X-Other", asList(MultiValuePattern.of(absent())));
    //        assertThat(headerPatterns.entrySet(),
    // everyItem(isIn(pattern.getMultipartHeaders().entrySet())));

    List<ContentPattern<?>> bodyPatterns = Arrays.<ContentPattern<?>>asList(equalToXml("<xml />"));
    assertThat(bodyPatterns, everyItem(isIn(pattern.getBodyPatterns())));
  }

  @Test
  public void testBuilderWithNameNoHeadersAndNoBody() {
    MultipartValuePattern pattern = aMultipart().build();
    assertNull(pattern);
  }

  @Test
  public void testBuilderWithoutNameWithHeadersAndBody() {
    MultipartValuePattern pattern =
        aMultipart()
            .withHeader("X-Header", containing("something"))
            .withHeader("X-Other", absent())
            .withBody(equalToXml("<xml />"))
            .build();

    Map<String, List<MultiValuePattern>> headerPatterns = newLinkedHashMap();
    headerPatterns.put("X-Header", asList(MultiValuePattern.of(containing("something"))));
    headerPatterns.put("X-Other", asList(MultiValuePattern.of(absent())));
    //        assertThat(headerPatterns.entrySet(),
    // everyItem(isIn(pattern.getHeaders().entrySet())));

    List<ContentPattern<?>> bodyPatterns = Arrays.<ContentPattern<?>>asList(equalToXml("<xml />"));
    assertThat(bodyPatterns, everyItem(isIn(pattern.getBodyPatterns())));
  }

  @Test
  public void testBuilderWithNameAndOtherContentDispositionHeaderMatcher() {
    MultipartValuePattern pattern =
        aMultipart("name")
            .withHeader("Content-Disposition", containing("filename=\"something\""))
            .build();

    Map<String, List<MultiValuePattern>> headerPatterns = newLinkedHashMap();
    headerPatterns.put(
        "Content-Disposition",
        asList(
            MultiValuePattern.of(containing("name=\"name\"")),
            MultiValuePattern.of(containing("filename=\"something\""))));
    //        assertThat(headerPatterns.entrySet(),
    // everyItem(isIn(pattern.getMultipartHeaders().entrySet())));
  }
}
