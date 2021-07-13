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

import java.io.File;
import javax.servlet.ServletContext;

public class ServletContextFileSource extends AbstractFileSource {

  private final String rootPath;
  private final ServletContext servletContext;

  public ServletContextFileSource(ServletContext servletContext, String rootPath) {
    super(getRootFile(servletContext, rootPath));
    this.rootPath = rootPath;
    this.servletContext = servletContext;
  }

  private static File getRootFile(ServletContext servletContext, String rootPath) {
    String containerRootPath = servletContext.getRealPath(rootPath);
    servletContext.log("rootPath: " + rootPath);
    return new File(containerRootPath);
  }

  @Override
  public FileSource child(String subDirectoryName) {
    return new ServletContextFileSource(servletContext, rootPath + '/' + subDirectoryName);
  }

  @Override
  protected boolean readOnly() {
    return true;
  }
}
