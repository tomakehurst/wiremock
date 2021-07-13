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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static com.github.tomakehurst.wiremock.testsupport.NoFileSource.noFileSource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.testsupport.WireMatchers;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;

public class HandlebarsCurrentDateHelperTest {

  private HandlebarsCurrentDateHelper helper;
  private ResponseTemplateTransformer transformer;

  @Before
  public void init() {
    helper = new HandlebarsCurrentDateHelper();
    transformer = new ResponseTemplateTransformer(true);

    LocalNotifier.set(new ConsoleNotifier(true));
  }

  @Test
  public void rendersNowDateTime() throws Exception {
    ImmutableMap<String, Object> optionsHash = ImmutableMap.<String, Object>of();

    Object output = render(optionsHash);

    assertThat(output, instanceOf(RenderableDate.class));
    assertThat(output.toString(), WireMatchers.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9:]+Z$"));
  }

  @Test
  public void rendersNowDateTimeWithCustomFormat() throws Exception {
    ImmutableMap<String, Object> optionsHash =
        ImmutableMap.<String, Object>of("format", "yyyy/mm/dd");

    Object output = render(optionsHash);

    assertThat(output, instanceOf(RenderableDate.class));
    assertThat(output.toString(), WireMatchers.matches("^[0-9]{4}/[0-9]{2}/[0-9]{2}$"));
  }

  @Test
  public void rendersPassedDateTimeWithDayOffset() throws Exception {
    String format = "yyyy-MM-dd";
    SimpleDateFormat df = new SimpleDateFormat(format);
    ImmutableMap<String, Object> optionsHash =
        ImmutableMap.<String, Object>of("format", format, "offset", "5 days");

    Object output = render(df.parse("2018-04-16"), optionsHash);

    assertThat(output.toString(), is("2018-04-21"));
  }

  @Test
  public void rendersNowWithDayOffset() throws Exception {
    ImmutableMap<String, Object> optionsHash =
        ImmutableMap.<String, Object>of("offset", "6 months");

    Object output = render(optionsHash);

    System.out.println(output);
  }

  @Test
  public void rendersNowAsUnixEpochInMilliseconds() throws Exception {
    ImmutableMap<String, Object> optionsHash = ImmutableMap.<String, Object>of("format", "epoch");

    Date date = new Date();
    Object output = render(date, optionsHash);

    assertThat(output.toString(), is(String.valueOf(date.getTime())));
  }

  @Test
  public void rendersNowAsUnixEpochInSeconds() throws Exception {
    ImmutableMap<String, Object> optionsHash = ImmutableMap.<String, Object>of("format", "unix");

    Date date = new Date();
    Object output = render(date, optionsHash);

    assertThat(output.toString(), is(String.valueOf(date.getTime() / 1000L)));
  }

  @Test
  public void adjustsISO8601ToSpecfiedTimezone() throws Exception {
    ImmutableMap<String, Object> optionsHash =
        ImmutableMap.<String, Object>of(
            "offset", "3 days",
            "timezone", "Australia/Sydney");

    Date inputDate = new ISO8601DateFormat().parse("2014-10-09T06:06:01Z");
    Object output = render(inputDate, optionsHash);

    assertThat(output.toString(), is("2014-10-12T17:06:01+11:00"));
  }

  @Test
  public void adjustsCustomFormatToSpecfiedTimezone() throws Exception {
    ImmutableMap<String, Object> optionsHash =
        ImmutableMap.<String, Object>of(
            "offset", "3 days",
            "timezone", "Australia/Sydney",
            "format", "yyyy-MM-dd HH:mm:ssZ");

    Date inputDate = new ISO8601DateFormat().parse("2014-10-09T06:06:01Z");
    Object output = render(inputDate, optionsHash);

    assertThat(output.toString(), is("2014-10-12 17:06:01+1100"));
  }

  @Test
  public void helperIsIncludedInTemplateTransformerWithNowTagName() {
    final ResponseDefinition responseDefinition =
        this.transformer.transform(
            mockRequest().url("/random-value"),
            aResponse().withBody("{{now offset='6 days'}}").build(),
            noFileSource(),
            Parameters.empty());

    String body = responseDefinition.getBody().trim();
    assertThat(body, WireMatchers.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9:]+Z$"));
  }

  @Test
  public void helperIsIncludedInTemplateTransformerWithDateTagName() {
    final ResponseDefinition responseDefinition =
        this.transformer.transform(
            mockRequest().url("/random-value"),
            aResponse().withBody("{{date offset='6 days'}}").build(),
            noFileSource(),
            Parameters.empty());

    String body = responseDefinition.getBody().trim();
    assertThat(body, WireMatchers.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9:]+Z$"));
  }

  @Test
  public void acceptsDateParameterwithDateTagName() {
    final ResponseDefinition responseDefinition =
        this.transformer.transform(
            mockRequest().url("/parsed-date"),
            aResponse()
                .withBody("{{date (parseDate '2018-05-05T10:11:12Z') offset='-1 days'}}")
                .build(),
            noFileSource(),
            Parameters.empty());

    String body = responseDefinition.getBody().trim();
    assertThat(body, is("2018-05-04T10:11:12Z"));
  }

  private Object render(ImmutableMap<String, Object> optionsHash) throws IOException {
    return render(null, optionsHash);
  }

  private Object render(Date context, ImmutableMap<String, Object> optionsHash) throws IOException {
    return helper.apply(
        context, new Options.Builder(null, null, null, null, null).setHash(optionsHash).build());
  }
}
