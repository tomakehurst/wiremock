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
package com.github.tomakehurst.wiremock.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class TrailingSlashFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    String path = getRequestPathFrom(httpServletRequest);

    StatusAndRedirectExposingHttpServletResponse wrappedResponse =
        new StatusAndRedirectExposingHttpServletResponse(
            (HttpServletResponse) response, path, httpServletRequest);
    chain.doFilter(request, wrappedResponse);
  }

  private static class StatusAndRedirectExposingHttpServletResponse
      extends HttpServletResponseWrapper {

    private String path;
    private HttpServletRequest request;

    public StatusAndRedirectExposingHttpServletResponse(
        HttpServletResponse response, String path, HttpServletRequest request) {
      super(response);
      this.path = path;
      this.request = request;
    }

    @Override
    public void sendRedirect(String location) throws IOException {
      if (location.contains(path)) {
        RequestDispatcher dispatcher =
            request.getRequestDispatcher(getPathPartFromLocation(location));
        try {
          dispatcher.forward(request, this);
        } catch (ServletException se) {
          throw new IOException(se);
        }
      }
    }

    private String getPathPartFromLocation(String location) throws IOException {
      URL url = new URL(location);
      return url.getPath();
    }
  }

  private String getRequestPathFrom(HttpServletRequest httpServletRequest) throws ServletException {
    try {
      String fullPath =
          new URI(URLEncoder.encode(httpServletRequest.getRequestURI(), "utf-8")).getPath();
      String pathWithoutContext = fullPath.substring(httpServletRequest.getContextPath().length());
      return URLDecoder.decode(pathWithoutContext, "utf-8");
    } catch (URISyntaxException e) {
      throw new ServletException(e);
    } catch (UnsupportedEncodingException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void destroy() {}
}
