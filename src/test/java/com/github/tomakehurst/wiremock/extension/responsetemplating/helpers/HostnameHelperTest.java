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
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Before;
import org.junit.Test;

public class HostnameHelperTest {

  private HostnameHelper helper;
  private String hostname;

  @Before
  public void init() throws UnknownHostException {
    helper = new HostnameHelper();
    hostname = InetAddress.getLocalHost().getHostName();

    LocalNotifier.set(new ConsoleNotifier(true));
  }

  @Test
  public void generatesHostname() throws Exception {
    ImmutableMap<String, Object> optionsHash = ImmutableMap.<String, Object>of();

    String output = render(optionsHash);
    assertThat(output, equalToIgnoringWhiteSpace(hostname));
  }

  private String render(ImmutableMap<String, Object> optionsHash) throws IOException {
    return helper
        .apply(null, new Options.Builder(null, null, null, null, null).setHash(optionsHash).build())
        .toString();
  }
}
