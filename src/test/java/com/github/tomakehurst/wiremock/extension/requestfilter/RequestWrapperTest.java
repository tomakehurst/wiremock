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
package com.github.tomakehurst.wiremock.extension.requestfilter;

import static com.github.tomakehurst.wiremock.common.Encoding.encodeBase64;
import static com.github.tomakehurst.wiremock.matching.MockMultipart.mockPart;
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.matching.MockRequest;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import java.util.List;
import org.junit.Test;

public class RequestWrapperTest {

  @Test
  public void changesTheRequestMethod() {
    MockRequest request = mockRequest().method(RequestMethod.GET);

    Request wrappedRequest = RequestWrapper.create().setMethod(RequestMethod.POST).wrap(request);

    assertThat(wrappedRequest.getMethod(), is(RequestMethod.POST));
  }

  @Test
  public void transformsTheUrl() {
    MockRequest request = mockRequest().url("/original-path?one=1&two=2");

    Request wrappedRequest =
        RequestWrapper.create()
            .transformAbsoluteUrl(
                new FieldTransformer<String>() {
                  @Override
                  public String transform(String existingUrl) {
                    return existingUrl
                        .replace("my.domain", "wiremock.org")
                        .replace("/original-path", "/new-path");
                  }
                })
            .wrap(request);

    assertThat(wrappedRequest.getUrl(), is("/new-path?one=1&two=2"));
    assertThat(wrappedRequest.getAbsoluteUrl(), is("http://wiremock.org/new-path?one=1&two=2"));
  }

  @Test
  public void transformsAUrlWithNoPath() {
    MockRequest request = mockRequest().url("");

    Request wrappedRequest =
        RequestWrapper.create()
            .transformAbsoluteUrl(
                new FieldTransformer<String>() {
                  @Override
                  public String transform(String existingUrl) {
                    return existingUrl.replace("my.domain", "wiremock.org");
                  }
                })
            .wrap(request);

    assertThat(wrappedRequest.getUrl(), is(""));
    assertThat(wrappedRequest.getAbsoluteUrl(), is("http://wiremock.org"));
  }

  @Test
  public void addsSpecifiedHeaders() {
    MockRequest request = mockRequest().header("One", "1");

    Request wrappedRequest = RequestWrapper.create().addHeader("Two", "2").wrap(request);

    assertThat(wrappedRequest.getHeaders().size(), is(2));
    assertThat(wrappedRequest.getHeader("Two"), is("2"));
  }

  @Test
  public void removesSpecifiedHeaders() {
    MockRequest request = mockRequest().header("One", "1");

    Request wrappedRequest = RequestWrapper.create().removeHeader("One").wrap(request);

    assertThat(wrappedRequest.getHeaders().size(), is(0));
  }

  @Test
  public void transformsSpecifiedHeaders() {
    MockRequest request = mockRequest().header("One", "1").header("Two", "2", "3");

    Request wrappedRequest =
        RequestWrapper.create()
            .transformHeader(
                "One",
                new FieldTransformer<List<String>>() {
                  @Override
                  public List<String> transform(List<String> headerValues) {
                    return FluentIterable.from(headerValues)
                        .transform(
                            new Function<String, String>() {
                              @Override
                              public String apply(String headerValue) {
                                return headerValue + "1";
                              }
                            })
                        .toList();
                  }
                })
            .transformHeader(
                "two",
                new FieldTransformer<List<String>>() {
                  @Override
                  public List<String> transform(List<String> headerValues) {
                    return FluentIterable.from(headerValues)
                        .transform(
                            new Function<String, String>() {
                              @Override
                              public String apply(String headerValue) {
                                return headerValue + "2";
                              }
                            })
                        .toList();
                  }
                })
            .wrap(request);

    assertThat(wrappedRequest.getHeader("One"), is("11"));
    HttpHeader headerTwo = wrappedRequest.header("Two");
    assertThat(headerTwo.values().get(0), is("22"));
    assertThat(headerTwo.values().get(1), is("32"));
  }

  @Test
  public void addsSpecifiedCookies() {
    MockRequest request = mockRequest().cookie("One", "1");

    Request wrappedRequest =
        RequestWrapper.create()
            .addCookie("Two", new Cookie("2"))
            .addCookie("Three", new Cookie("3"))
            .wrap(request);

    assertThat(wrappedRequest.getCookies().size(), is(3));
    assertThat(wrappedRequest.getCookies().get("Two").firstValue(), is("2"));
    assertThat(wrappedRequest.getCookies().get("Three").firstValue(), is("3"));
  }

  @Test
  public void removesSpecifiedCookies() {
    MockRequest request = mockRequest().cookie("One", "1").cookie("Two", "2").cookie("Three", "3");

    Request wrappedRequest = RequestWrapper.create().removeCookie("Two").wrap(request);

    assertThat(wrappedRequest.getCookies().size(), is(2));
    assertThat(wrappedRequest.getCookies().get("Two"), nullValue());
  }

  @Test
  public void transformsSpecifiedCookies() {
    MockRequest request = mockRequest().cookie("One", "1");

    Request wrappedRequest =
        RequestWrapper.create()
            .transformCookie(
                "One",
                new FieldTransformer<Cookie>() {
                  @Override
                  public Cookie transform(Cookie cookie) {
                    return new Cookie(cookie.firstValue() + "1");
                  }
                })
            .wrap(request);

    assertThat(wrappedRequest.getCookies().get("One").firstValue(), is("11"));
  }

  @Test
  public void transformsAStringBody() {
    MockRequest request = mockRequest().body("One");

    Request wrappedRequest =
        RequestWrapper.create()
            .transformBody(
                new FieldTransformer<Body>() {
                  @Override
                  public Body transform(Body existingBody) {
                    String newValue = existingBody.asString().replace("One", "Two");
                    return new Body(newValue);
                  }
                })
            .wrap(request);

    assertThat(wrappedRequest.getBodyAsString(), is("Two"));
    assertThat(wrappedRequest.getBodyAsBase64(), is(encodeBase64("Two".getBytes())));
  }

  @Test
  public void transformsABinaryBody() {
    final byte[] initialBytes = new byte[] {1, 2, 3};
    final byte[] finalBytes = new byte[] {4, 5, 6};

    MockRequest request = mockRequest().body(initialBytes);

    Request wrappedRequest =
        RequestWrapper.create()
            .transformBody(
                new FieldTransformer<Body>() {
                  @Override
                  public Body transform(Body existingBody) {
                    return new Body(finalBytes);
                  }
                })
            .wrap(request);

    assertThat(wrappedRequest.getBody(), is(finalBytes));
    assertThat(wrappedRequest.getBodyAsBase64(), is(encodeBase64(finalBytes)));
  }

  @Test
  public void transformsMultiparts() {
    MockRequest request =
        mockRequest().part(mockPart().name("one").body("1")).part(mockPart().name("two").body("2"));

    Request wrappedRequest =
        RequestWrapper.create()
            .transformParts(
                new FieldTransformer<Request.Part>() {
                  @Override
                  public Request.Part transform(Request.Part existingPart) {
                    return existingPart.getName().equals("one")
                        ? mockPart().name("one").body("1111")
                        : mockPart().name("two").body("2222");
                  }
                })
            .wrap(request);

    assertThat(wrappedRequest.getPart("one").getBody().asString(), is("1111"));
    assertThat(wrappedRequest.getPart("two").getBody().asString(), is("2222"));
    assertThat(wrappedRequest.getParts(), hasItem(mockPart().name("one").body("1111")));
  }
}
