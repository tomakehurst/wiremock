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
package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class TruncateDateTimeHelperTest extends HandlebarsHelperTestBase {

  private TruncateDateTimeHelper helper;

  @Before
  public void init() {
    helper = new TruncateDateTimeHelper();
  }

  @Test
  public void truncatesDateObject() throws IOException {
    Date date = Date.from(ZonedDateTime.parse("2020-03-27T11:22:33Z").toInstant());

    Object output = renderHelperValue(helper, date, "last day of month");

    assertThat(output, Matchers.instanceOf(Date.class));

    Date truncated = (Date) output;
    assertThat(truncated.toInstant(), is(Instant.parse("2020-03-31T00:00:00Z")));
  }
}
