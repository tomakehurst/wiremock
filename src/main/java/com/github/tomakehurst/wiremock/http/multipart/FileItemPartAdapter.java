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
package com.github.tomakehurst.wiremock.http.multipart;

import com.github.tomakehurst.wiremock.http.Body;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import java.util.Iterator;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;

public class FileItemPartAdapter implements Request.Part {

  private final FileItem fileItem;

  public FileItemPartAdapter(FileItem fileItem) {
    this.fileItem = fileItem;
  }

  @Override
  public String getName() {
    return fileItem.getFieldName();
  }

  @Override
  public HttpHeader getHeader(String name) {
    Iterator<String> headerValues = fileItem.getHeaders().getHeaders(name);
    return new HttpHeader(name, Iterators.toArray(headerValues, String.class));
  }

  @Override
  public HttpHeaders getHeaders() {
    FileItemHeaders headers = fileItem.getHeaders();
    Iterator<String> i = headers.getHeaderNames();
    ImmutableList.Builder<HttpHeader> builder = ImmutableList.builder();
    while (i.hasNext()) {
      String name = i.next();
      builder.add(getHeader(name));
    }

    return new HttpHeaders(builder.build());
  }

  @Override
  public Body getBody() {
    return new Body(fileItem.get());
  }

  public static final Function<FileItem, Request.Part> TO_PARTS =
      new Function<FileItem, Request.Part>() {
        @Override
        public Request.Part apply(FileItem fileItem) {
          return new FileItemPartAdapter(fileItem);
        }
      };
}
