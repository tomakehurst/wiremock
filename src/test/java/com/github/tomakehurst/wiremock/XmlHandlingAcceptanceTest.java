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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class XmlHandlingAcceptanceTest {

  @Rule
  public WireMockRule wm =
      new WireMockRule(options().dynamicPort().extensions(new ResponseTemplateTransformer(false)));

  @Rule
  public WireMockRule externalDtdServer =
      new WireMockRule(options().dynamicPort().notifier(new ConsoleNotifier(true)));

  WireMockTestClient client;

  @Before
  public void init() {
    client = new WireMockTestClient(wm.port());

    externalDtdServer.stubFor(
        get("/dodgy.dtd")
            .willReturn(
                ok("<!ELEMENT shiftydata (#PCDATA)>")
                    .withHeader("Content-Type", "application/xml-dtd")));
  }

  @Test
  public void doesNotDownloadExternalDtdDocumentsWhenMatchingOnEqualToXml() {
    String xml =
        "<?xml version=\"1.0\"?>\n"
            + "<!DOCTYPE things [\n"
            + "<!ENTITY % sp SYSTEM \"http://localhost:"
            + externalDtdServer.port()
            + "/dodgy.dtd\">\n"
            + "%sp;\n"
            + "]>\n"
            + "\n"
            + "<things><shiftydata>123</shiftydata></things>";

    wm.stubFor(post("/xml-match").withRequestBody(equalToXml(xml)).willReturn(ok()));

    assertThat(client.postXml("/xml-match", xml).statusCode(), is(200));

    externalDtdServer.verify(0, getRequestedFor(anyUrl()));
  }

  @Test
  public void doesNotDownloadExternalDtdDocumentsWhenMatchingXPath() {
    String xml =
        "<?xml version=\"1.0\"?>\n"
            + "<!DOCTYPE things [\n"
            + "<!ENTITY % sp SYSTEM \"http://localhost:"
            + externalDtdServer.port()
            + "/dodgy.dtd\">\n"
            + "%sp;\n"
            + "]>\n"
            + "\n"
            + "<things><shiftydata>123</shiftydata></things>";

    wm.stubFor(
        post("/xpath-match").withRequestBody(matchingXPath("//shiftydata")).willReturn(ok()));

    assertThat(client.postXml("/xpath-match", xml).statusCode(), is(200));

    externalDtdServer.verify(0, getRequestedFor(anyUrl()));
  }

  @Test
  public void doesNotDownloadExternalDtdDocumentsWhenEvaluatingXPathInTemplate() {
    String xml =
        "<?xml version=\"1.0\"?>\n"
            + "<!DOCTYPE things [\n"
            + "<!ENTITY % sp SYSTEM \"http://localhost:"
            + externalDtdServer.port()
            + "/dodgy.dtd\">\n"
            + "%sp;\n"
            + "]>\n"
            + "\n"
            + "<things><shiftydata>123</shiftydata></things>";

    wm.stubFor(
        post("/xpath-template")
            .willReturn(
                ok("{{xPath request.body '//shiftydata/text()'}}")
                    .withTransformers(ResponseTemplateTransformer.NAME)));

    assertThat(client.postXml("/xpath-template", xml).statusCode(), is(200));

    externalDtdServer.verify(0, getRequestedFor(anyUrl()));
  }

  @Test
  public void doesNotAttemptToValidateXmlAgainstDtdWhenMatchingOnEqualToXml() {
    String xml =
        "<?xml version=\"1.0\"?>\n"
            + "<!DOCTYPE things [\n"
            + "<!ENTITY % sp SYSTEM \"http://localhost:"
            + externalDtdServer.port()
            + "/dodgy.dtd\">\n"
            + "%sp;\n"
            + "]>\n"
            + "\n"
            + "<badly-formed-things/>";

    wm.stubFor(post("/bad-xml-match").withRequestBody(equalToXml(xml)).willReturn(ok()));

    assertThat(client.postXml("/bad-xml-match", xml).statusCode(), is(200));
  }

  @Test
  public void doesNotAttemptToValidateXmlAgainstDtdWhenMatchingOnXPath() {
    String xml =
        "<?xml version=\"1.0\"?>\n"
            + "<!DOCTYPE things [\n"
            + "<!ENTITY % sp SYSTEM \"http://localhost:"
            + externalDtdServer.port()
            + "/dodgy.dtd\">\n"
            + "%sp;\n"
            + "]>\n"
            + "\n"
            + "<badly-formed-things/>";

    wm.stubFor(
        post("/bad-xpath-match")
            .withRequestBody(matchingXPath("/badly-formed-things"))
            .willReturn(ok()));

    assertThat(client.postXml("/bad-xpath-match", xml).statusCode(), is(200));
  }
}
