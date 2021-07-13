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

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;

import java.lang.reflect.Method;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

public class JettyUtils {

  private static final Map<Class<?>, Method> URI_METHOD_BY_CLASS_CACHE = new HashMap<>();
  private static final boolean IS_JETTY;

  static {
    // do the check only once per classloader / execution
    IS_JETTY = isClassExist("org.eclipse.jetty.server.Request");
  }

  private JettyUtils() {
    // Hide public constructor
  }

  public static boolean isJetty() {
    return IS_JETTY;
  }

  private static boolean isClassExist(String type) {
    try {
      ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
      ClassLoader loader = contextCL == null ? JettyUtils.class.getClassLoader() : contextCL;
      Class.forName(type, false, loader);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static Response unwrapResponse(HttpServletResponse httpServletResponse) {
    if (httpServletResponse instanceof HttpServletResponseWrapper) {
      ServletResponse unwrapped = ((HttpServletResponseWrapper) httpServletResponse).getResponse();
      return (Response) unwrapped;
    }

    return (Response) httpServletResponse;
  }

  public static Socket getTlsSocket(Response response) {
    HttpChannel httpChannel = response.getHttpOutput().getHttpChannel();
    SslConnection.DecryptedEndPoint sslEndpoint =
        (SslConnection.DecryptedEndPoint) httpChannel.getEndPoint();
    Object endpoint = sslEndpoint.getSslConnection().getEndPoint();
    try {
      return (Socket) endpoint.getClass().getMethod("getSocket").invoke(endpoint);
    } catch (Exception e) {
      return throwUnchecked(e, Socket.class);
    }
  }

  public static boolean uriIsAbsolute(Request request) {
    HttpURI uri = getHttpUri(request);
    return uri.getScheme() != null;
  }

  private static HttpURI getHttpUri(Request request) {
    try {
      return (HttpURI) getURIMethodFromClass(request.getClass()).invoke(request);
    } catch (Exception e) {
      throw new IllegalArgumentException(request + " does not have a getUri or getHttpURI method");
    }
  }

  private static Method getURIMethodFromClass(Class<?> requestClass) throws NoSuchMethodException {
    if (URI_METHOD_BY_CLASS_CACHE.containsKey(requestClass)) {
      return URI_METHOD_BY_CLASS_CACHE.get(requestClass);
    }
    Method method;
    try {
      method = requestClass.getDeclaredMethod("getUri");
    } catch (NoSuchMethodException ignored) {
      method = requestClass.getDeclaredMethod("getHttpURI");
    }
    URI_METHOD_BY_CLASS_CACHE.put(requestClass, method);
    return method;
  }
}
