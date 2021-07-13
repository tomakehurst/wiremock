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
package com.github.tomakehurst.wiremock.common;

import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.matches;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class VeryShortIdGeneratorTest {

  @Test
  public void IdsGeneratedContainOnlyLegalCharsAndAreRightLength() {
    final IdGenerator generator = new VeryShortIdGenerator();

    for (int i = 0; i < 1000; i++) {
      final String id = generator.generate();
      assertThat(id, matches("[A-Za-z0-9]{5}"));
    }
  }
}
