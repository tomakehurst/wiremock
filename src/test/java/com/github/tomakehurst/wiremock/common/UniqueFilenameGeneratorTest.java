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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.UUID;
import org.junit.Test;

public class UniqueFilenameGeneratorTest {

  @Test
  public void generatesValidNameWhenRequestHasUrlWithTwoPathNodes() {
    String fileName = UniqueFilenameGenerator.generate("/some/path", "body", "random123");

    assertThat(fileName, is("body-some-path-random123.json"));
  }

  @Test
  public void generatesValidNameWhenRequestHasUrlWithOnePathNode() {
    String fileName = UniqueFilenameGenerator.generate("/thing", "body", "random123");

    assertThat(fileName, is("body-thing-random123.json"));
  }

  @Test
  public void generatesValidNameWhenRequestHasRootPath() {
    String fileName = UniqueFilenameGenerator.generate("/", "body", "random123");

    assertThat(fileName, is("body-(root)-random123.json"));
  }

  @Test
  public void truncatesToApproximately150CharactersWhenUrlVeryLong() {
    String prefix = "someprefix";
    String extension = "abc";
    String id = UUID.randomUUID().toString();

    String fileName =
        UniqueFilenameGenerator.generate(
            "/one/two/three/four/five/six/seven/eight/nine/ten/one/two/three/four/five/six/seven/eight/nine/ten/one/two/three/four/five/six/seven/eight/nine/ten/one/two/three/four/five/six/seven/eight/nine/ten/one/two/three/four/five/six/seven/eight/nine/ten/one/two/three/four/five/six/seven/eight/nine/ten",
            prefix,
            id,
            extension);

    System.out.println(fileName);

    int expectedLength = 150 + extension.length() + 1 + id.length() + 1 + prefix.length() + 1;
    assertThat(fileName.length(), is(expectedLength));
  }
}
