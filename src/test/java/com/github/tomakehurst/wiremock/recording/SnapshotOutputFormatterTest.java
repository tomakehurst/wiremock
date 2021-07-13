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

import static com.github.tomakehurst.wiremock.recording.SnapshotOutputFormatter.FULL;
import static com.github.tomakehurst.wiremock.recording.SnapshotOutputFormatter.IDS;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SnapshotOutputFormatterTest {
  @Test
  public void fromStringDefault() {
    assertEquals(FULL, SnapshotOutputFormatter.fromString(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromStringWithInvalidFormat() {
    SnapshotOutputFormatter.fromString("invalid output format");
  }

  @Test
  public void fromStringWithFull() {
    assertEquals(FULL, SnapshotOutputFormatter.fromString("full"));
  }

  @Test
  public void fromStringWithIds() {
    assertEquals(IDS, SnapshotOutputFormatter.fromString("ids"));
  }
}
