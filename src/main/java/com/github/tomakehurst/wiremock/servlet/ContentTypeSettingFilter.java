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
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ContentTypeSettingFilter implements Filter {

  private ServletContext context;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    context = filterConfig.getServletContext();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (response instanceof HttpServletResponse) {
      String filePath = ((HttpServletRequest) request).getRequestURI();
      String contentType = context.getMimeType(filePath);
      if (contentType == null) {
        contentType = "application/json";
      }
      ((HttpServletResponse) response).setContentType(contentType);
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {}
}
