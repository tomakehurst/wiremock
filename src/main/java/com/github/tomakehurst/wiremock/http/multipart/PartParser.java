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

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.http.multipart.FileItemPartAdapter.TO_PARTS;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

public class PartParser {

  @SuppressWarnings("unchecked")
  public static Collection<Request.Part> parseFrom(Request request) {
    FileItemFactory fileItemFactory =
        new DiskFileItemFactory(Integer.MAX_VALUE, new File(System.getProperty("java.io.tmpdir")));

    HttpHeaders headers = request.getHeaders();
    ByteArrayUploadContext uploadContext =
        new ByteArrayUploadContext(
            request.getBody(),
            headerValueOrNull("Content-Encoding", headers),
            headers.getContentTypeHeader().firstValue());

    FileUpload upload = new FileUpload(fileItemFactory);

    try {
      List<FileItem> items = upload.parseRequest(uploadContext);
      return Lists.transform(items, TO_PARTS);
    } catch (FileUploadException e) {
      return throwUnchecked(e, Collection.class);
    }
  }

  private static String headerValueOrNull(String key, HttpHeaders httpHeaders) {
    HttpHeader header = httpHeaders.getHeader(key);
    return header.isPresent() ? header.firstValue() : null;
  }

  public static class ByteArrayUploadContext implements UploadContext {

    private final byte[] content;
    private final String encoding;
    private final String contentType;

    public ByteArrayUploadContext(byte[] content, String encoding, String contentType) {
      this.content = content;
      this.encoding = encoding;
      this.contentType = contentType;
    }

    @Override
    public long contentLength() {
      return content.length;
    }

    @Override
    public String getCharacterEncoding() {
      return encoding;
    }

    @Override
    public String getContentType() {
      return contentType;
    }

    @Override
    public int getContentLength() {
      return content.length;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(content);
    }
  }
}
