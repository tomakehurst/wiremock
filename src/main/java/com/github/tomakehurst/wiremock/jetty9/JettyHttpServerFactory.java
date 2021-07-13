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
package com.github.tomakehurst.wiremock.jetty9;

import com.github.tomakehurst.wiremock.common.Exceptions;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.AdminRequestHandler;
import com.github.tomakehurst.wiremock.http.HttpServer;
import com.github.tomakehurst.wiremock.http.HttpServerFactory;
import com.github.tomakehurst.wiremock.http.StubRequestHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class JettyHttpServerFactory implements HttpServerFactory {

  private static final Constructor<? extends JettyHttpServer> SERVER_CONSTRUCTOR =
      getServerConstructor();

  @SuppressWarnings("unchecked")
  private static Constructor<? extends JettyHttpServer> getServerConstructor() {
    try {
      Class<? extends JettyHttpServer> serverClass =
          (Class<? extends JettyHttpServer>)
              Class.forName("com.github.tomakehurst.wiremock.jetty94.Jetty94HttpServer");
      return safelyGetConstructor(
          serverClass, Options.class, AdminRequestHandler.class, StubRequestHandler.class);
    } catch (ClassNotFoundException e) {
      try {
        Class<? extends JettyHttpServer> serverClass =
            (Class<? extends JettyHttpServer>)
                Class.forName("com.github.tomakehurst.wiremock.jetty92.Jetty92HttpServer");
        return safelyGetConstructor(
            serverClass, Options.class, AdminRequestHandler.class, StubRequestHandler.class);
      } catch (ClassNotFoundException cnfe) {
        return safelyGetConstructor(
            JettyHttpServer.class,
            Options.class,
            AdminRequestHandler.class,
            StubRequestHandler.class);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> Constructor<T> safelyGetConstructor(
      Class<T> clazz, Class<?>... parameterTypes) {
    try {
      return clazz.getConstructor(parameterTypes);
    } catch (NoSuchMethodException e) {
      return Exceptions.throwUnchecked(e, Constructor.class);
    }
  }

  @Override
  public HttpServer buildHttpServer(
      Options options,
      AdminRequestHandler adminRequestHandler,
      StubRequestHandler stubRequestHandler) {
    try {
      return SERVER_CONSTRUCTOR.newInstance(options, adminRequestHandler, stubRequestHandler);
    } catch (InstantiationException | IllegalAccessException e) {
      return Exceptions.throwUnchecked(e, HttpServer.class);
    } catch (InvocationTargetException e) {
      return Exceptions.throwUnchecked(e.getCause(), null);
    }
  }
}
