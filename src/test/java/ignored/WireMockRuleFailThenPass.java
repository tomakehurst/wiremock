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
package ignored;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WireMockRuleFailThenPass {

  // Generates a failure to illustrate a Rule bug whereby a failed test would cause BindExceptions
  // on subsequent (otherwise passing) tests
  @Test
  public void fail() {
    assertTrue(false);
  }

  @Test
  public void succeed() {
    assertTrue(true);
  }
}
